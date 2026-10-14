package com.paze.pilates.desktop.ui.reservation;

import com.paze.pilates.dto.AttendanceDto;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.LessonDto;
import com.paze.pilates.dto.LessonParticipantDto;
import com.paze.pilates.service.LessonService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;

/**
 * Dersi tamamlama: her katılımcı için yoklama alınır. Gelenin seansı düşülür;
 * gelmeyenin seansının düşülüp düşülmeyeceğine hoca karar verir.
 */
@org.springframework.stereotype.Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class LessonCompleteDialog extends JDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final LessonService lessonService;
    private final MessageSource messages;

    private AttendanceTableModel tableModel;
    private JButton completeBtn;
    private JLabel errorLabel;
    private LessonDto completed;

    public LessonCompleteDialog(LessonService lessonService, MessageSource messages) {
        this.lessonService = lessonService;
        this.messages = messages;
    }

    /** Formu açar; tamamlanan dersi döner, iptal edilirse null. */
    public LessonDto showDialog(Frame owner, LessonDto lesson) {
        setTitle(text("lesson.complete.title"));
        setModal(true);
        setLayout(new BorderLayout(0, 8));

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setBorder(BorderFactory.createEmptyBorder(16, 20, 0, 20));
        header.add(new JLabel(messages.getMessage("lesson.participants.header",
                new Object[] {lesson.instructorName(), lesson.roomName(), DATE_FMT.format(lesson.date()),
                        TIME_FMT.format(lesson.startTime()), TIME_FMT.format(lesson.endTime())},
                Locale.getDefault())), BorderLayout.NORTH);
        header.add(new JLabel(text("lesson.complete.hint")), BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        tableModel = new AttendanceTableModel();
        JTable table = new JTable(tableModel);
        table.setRowHeight(26);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(220);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        add(scroll, BorderLayout.CENTER);

        errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.decode("#DC2626"));
        completeBtn = new JButton(text("lesson.complete.button.complete"));
        completeBtn.putClientProperty("JButton.buttonType", "default");
        completeBtn.setEnabled(false);
        completeBtn.addActionListener(e -> onComplete(lesson));
        JButton cancelBtn = new JButton(text("lesson.complete.button.cancel"));
        cancelBtn.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 20, 12, 20));
        bottom.add(errorLabel, BorderLayout.WEST);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(cancelBtn);
        buttons.add(completeBtn);
        bottom.add(buttons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(600, 380));
        pack();
        setLocationRelativeTo(owner);
        load(lesson);
        setVisible(true);
        return completed;
    }

    private void load(LessonDto lesson) {
        new SwingWorker<List<LessonParticipantDto>, Void>() {
            @Override protected List<LessonParticipantDto> doInBackground() {
                return lessonService.getParticipants(lesson.id());
            }
            @Override protected void done() {
                try {
                    List<LessonParticipantDto> rows = get();
                    tableModel.setRows(rows);
                    completeBtn.setEnabled(true);
                    if (rows.isEmpty()) {
                        errorLabel.setText(text("lesson.complete.no_participants"));
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    errorLabel.setText(text("error.unexpected"));
                }
            }
        }.execute();
    }

    private void onComplete(LessonDto lesson) {
        List<AttendanceDto> attendance = tableModel.attendance();
        completeBtn.setEnabled(false);
        errorLabel.setText(" ");
        new SwingWorker<LessonDto, Void>() {
            @Override protected LessonDto doInBackground() {
                return lessonService.completeLesson(lesson.id(), attendance);
            }
            @Override protected void done() {
                try {
                    completed = get();
                    dispose();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    errorLabel.setText(ex.getCause() instanceof BusinessException be
                            ? text(be.getMessage()) : text("error.unexpected"));
                    completeBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private String text(String key) {
        return messages.getMessage(key, null, key, Locale.getDefault());
    }

    private static final class Row {
        final LessonParticipantDto participant;
        boolean attended = true;
        boolean chargeNoShow;

        Row(LessonParticipantDto participant) {
            this.participant = participant;
        }
    }

    /** Sütunlar: öğrenci, geldi, (gelmediyse) seansı düş. Varsayılan: herkes geldi. */
    private final class AttendanceTableModel extends AbstractTableModel {

        private final List<Row> rows = new ArrayList<>();

        void setRows(List<LessonParticipantDto> participants) {
            rows.clear();
            participants.forEach(p -> rows.add(new Row(p)));
            fireTableDataChanged();
        }

        List<AttendanceDto> attendance() {
            return rows.stream()
                    .map(r -> new AttendanceDto(r.participant.id(), r.attended, !r.attended && r.chargeNoShow))
                    .toList();
        }

        @Override public int getRowCount() { return rows.size(); }

        @Override public int getColumnCount() { return 3; }

        @Override public String getColumnName(int col) {
            return text(switch (col) {
                case 0 -> "lesson.complete.col.name";
                case 1 -> "lesson.complete.col.attended";
                default -> "lesson.complete.col.charge_no_show";
            });
        }

        @Override public Class<?> getColumnClass(int col) {
            return col == 0 ? String.class : Boolean.class;
        }

        @Override public boolean isCellEditable(int row, int col) {
            return col == 1 || (col == 2 && !rows.get(row).attended);
        }

        @Override public Object getValueAt(int row, int col) {
            Row r = rows.get(row);
            return switch (col) {
                case 0 -> r.participant.studentName();
                case 1 -> r.attended;
                default -> r.attended || r.chargeNoShow;
            };
        }

        @Override public void setValueAt(Object value, int row, int col) {
            Row r = rows.get(row);
            if (col == 1) {
                r.attended = Boolean.TRUE.equals(value);
            } else if (col == 2) {
                r.chargeNoShow = Boolean.TRUE.equals(value);
            }
            fireTableRowsUpdated(row, row);
        }
    }
}
