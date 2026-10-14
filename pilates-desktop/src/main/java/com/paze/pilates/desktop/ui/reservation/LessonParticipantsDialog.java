package com.paze.pilates.desktop.ui.reservation;

import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.LessonDto;
import com.paze.pilates.dto.LessonParticipantDto;
import com.paze.pilates.dto.Permission;
import com.paze.pilates.service.AuthService;
import com.paze.pilates.service.LessonService;
import java.awt.BorderLayout;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;

/** Seçilen dersin katılımcılarını listeler. */
@org.springframework.stereotype.Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class LessonParticipantsDialog extends JDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final LessonService lessonService;
    private final AuthService authService;
    private final MessageSource messages;

    private ParticipantTableModel tableModel;
    private JTable table;
    private JButton removeBtn;
    private boolean changed;
    private JLabel statusLabel;

    public LessonParticipantsDialog(LessonService lessonService, AuthService authService, MessageSource messages) {
        this.lessonService = lessonService;
        this.authService = authService;
        this.messages = messages;
    }

    /** Formu açar; katılımcı çıkarıldıysa true döner. */
    public boolean showDialog(Frame owner, LessonDto lesson) {
        setTitle(text("lesson.participants.title"));
        setModal(true);
        setLayout(new BorderLayout(0, 8));

        JLabel header = new JLabel(messages.getMessage("lesson.participants.header",
                new Object[] {lesson.instructorName(), lesson.roomName(), DATE_FMT.format(lesson.date()),
                        TIME_FMT.format(lesson.startTime()), TIME_FMT.format(lesson.endTime())},
                Locale.getDefault()));
        header.setBorder(BorderFactory.createEmptyBorder(16, 20, 0, 20));
        add(header, BorderLayout.NORTH);

        tableModel = new ParticipantTableModel();
        table = new JTable(tableModel);
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> removeBtn.setEnabled(table.getSelectedRow() >= 0));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        add(scroll, BorderLayout.CENTER);

        statusLabel = new JLabel(" ");
        removeBtn = new JButton(text("lesson.participants.button.remove"));
        removeBtn.setEnabled(false);
        removeBtn.addActionListener(e -> onRemove(lesson));
        JButton closeBtn = new JButton(text("lesson.participants.button.close"));
        closeBtn.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 20, 12, 20));
        bottom.add(statusLabel, BorderLayout.WEST);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        if (authService.getCurrentUser().map(u -> u.has(Permission.LESSON_MANAGE)).orElse(false)) {
            buttons.add(removeBtn);
        }
        buttons.add(closeBtn);
        bottom.add(buttons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(520, 360));
        pack();
        setLocationRelativeTo(owner);
        load(lesson);
        setVisible(true);
        return changed;
    }

    private void onRemove(LessonDto lesson) {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        LessonParticipantDto participant = tableModel.getRow(row);
        int answer = JOptionPane.showConfirmDialog(this,
                messages.getMessage("lesson.participants.remove.confirm",
                        new Object[] {participant.studentName()}, Locale.getDefault()),
                text("lesson.participants.remove.title"), JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        removeBtn.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                lessonService.removeParticipant(participant.id());
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    changed = true;
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    statusLabel.setText(ex.getCause() instanceof BusinessException be
                            ? text(be.getMessage()) : text("error.unexpected"));
                    return;
                }
                load(lesson);
            }
        }.execute();
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
                    statusLabel.setText(rows.isEmpty()
                            ? text("lesson.participants.empty")
                            : messages.getMessage("lesson.participants.count",
                                    new Object[] {rows.size(), lesson.capacity()}, Locale.getDefault()));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    statusLabel.setText(text("error.unexpected"));
                }
            }
        }.execute();
    }

    private String text(String key) {
        return messages.getMessage(key, null, key, Locale.getDefault());
    }

    private final class ParticipantTableModel extends AbstractTableModel {

        private List<LessonParticipantDto> rows = new ArrayList<>();

        LessonParticipantDto getRow(int row) {
            return rows.get(row);
        }

        void setRows(List<LessonParticipantDto> rows) {
            this.rows = new ArrayList<>(rows);
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }

        @Override public int getColumnCount() { return 2; }

        @Override public String getColumnName(int col) {
            return text(col == 0 ? "lesson.participants.col.name" : "lesson.participants.col.phone");
        }

        @Override public Object getValueAt(int row, int col) {
            LessonParticipantDto p = rows.get(row);
            return col == 0 ? p.studentName() : p.phone();
        }
    }
}
