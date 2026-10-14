package com.paze.pilates.desktop.ui.reservation;

import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.LessonDto;
import com.paze.pilates.dto.LessonParticipantDto;
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
import org.springframework.beans.factory.ObjectProvider;
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
    private final ObjectProvider<LessonCompleteDialog> completeDialogProvider;
    private final MessageSource messages;

    private ParticipantTableModel tableModel;
    private JTable table;
    private JButton removeBtn;
    private JButton completeBtn;
    private boolean changed;
    private boolean completed;
    private JLabel statusLabel;

    public LessonParticipantsDialog(LessonService lessonService, AuthService authService,
                                    ObjectProvider<LessonCompleteDialog> completeDialogProvider,
                                    MessageSource messages) {
        this.lessonService = lessonService;
        this.authService = authService;
        this.completeDialogProvider = completeDialogProvider;
        this.messages = messages;
    }

    /** Formu açar; katılımcı çıkarıldıysa veya ders tamamlandıysa true döner. */
    public boolean showDialog(Frame owner, LessonDto lesson) {
        completed = lesson.completed();
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
        table.getSelectionModel().addListSelectionListener(
                e -> removeBtn.setEnabled(!completed && table.getSelectedRow() >= 0));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        add(scroll, BorderLayout.CENTER);

        statusLabel = new JLabel(" ");
        removeBtn = new JButton(text("lesson.participants.button.remove"));
        removeBtn.setEnabled(false);
        removeBtn.addActionListener(e -> onRemove(lesson));
        completeBtn = new JButton(text("lesson.participants.button.complete"));
        completeBtn.setEnabled(!completed);
        completeBtn.addActionListener(e -> onComplete(owner, lesson));
        JButton closeBtn = new JButton(text("lesson.participants.button.close"));
        closeBtn.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 20, 12, 20));
        bottom.add(statusLabel, BorderLayout.WEST);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        if (authService.getCurrentUser().map(u -> u.canManageLessonOf(lesson.instructorId())).orElse(false)) {
            buttons.add(removeBtn);
            buttons.add(completeBtn);
        }
        buttons.add(closeBtn);
        bottom.add(buttons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(720, 380));
        pack();
        setLocationRelativeTo(owner);
        load(lesson);
        setVisible(true);
        return changed;
    }

    private void onComplete(Frame owner, LessonDto lesson) {
        LessonDto result = completeDialogProvider.getObject().showDialog(owner, lesson);
        if (result != null) {
            completed = true;
            changed = true;
            completeBtn.setEnabled(false);
            removeBtn.setEnabled(false);
            load(lesson);
        }
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
                    String status = rows.isEmpty()
                            ? text("lesson.participants.empty")
                            : messages.getMessage("lesson.participants.count",
                                    new Object[] {rows.size(), lesson.capacity()}, Locale.getDefault());
                    statusLabel.setText(completed ? status + " · " + text("lesson.participants.completed") : status);
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

        @Override public int getColumnCount() { return 4; }

        @Override public String getColumnName(int col) {
            return text(switch (col) {
                case 0 -> "lesson.participants.col.name";
                case 1 -> "lesson.participants.col.phone";
                case 2 -> "lesson.participants.col.attendance";
                default -> "lesson.participants.col.session";
            });
        }

        @Override public Object getValueAt(int row, int col) {
            LessonParticipantDto p = rows.get(row);
            return switch (col) {
                case 0 -> p.studentName();
                case 1 -> p.phone();
                case 2 -> p.attended() == null ? ""
                        : text(p.attended() ? "lesson.participants.attended" : "lesson.participants.absent");
                default -> sessionText(p);
            };
        }

        /** Seans düşüldüyse paket adı, paketi yoksa borç, düşülmediyse bilgi. */
        private String sessionText(LessonParticipantDto p) {
            if (p.attended() == null) {
                return "";
            }
            if (!p.charged()) {
                return text("lesson.participants.session.not_charged");
            }
            return p.inDebt() ? text("lesson.participants.session.debt") : p.packageName();
        }
    }
}
