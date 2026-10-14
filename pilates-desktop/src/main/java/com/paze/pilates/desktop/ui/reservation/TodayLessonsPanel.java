package com.paze.pilates.desktop.ui.reservation;

import com.paze.pilates.desktop.ui.shared.NotificationBar;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.LessonDto;
import com.paze.pilates.service.LessonService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;

/**
 * Bugünkü dersler bileşeni: yönetici tüm dersleri, hoca yalnızca kendi derslerini görür.
 * Takvimdeki ders işlemleri ({@link LessonActions}) buradan da yapılır. Herhangi bir panele eklenebilir.
 */
@org.springframework.stereotype.Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class TodayLessonsPanel extends JPanel {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Color COMPLETED_BG = new Color(187, 247, 208);
    private static final int SLOT_MINUTES = 30;

    private final LessonService lessonService;
    private final LessonActions actions;
    private final NotificationBar notificationBar;
    private final MessageSource messages;

    private LessonTableModel tableModel;
    private JTable table;
    private JLabel titleLabel;
    private JLabel emptyLabel;
    private JButton participantsBtn;
    private JButton addStudentBtn;
    private JButton completeBtn;
    private JButton undoBtn;

    public TodayLessonsPanel(LessonService lessonService, LessonActions actions,
                             NotificationBar notificationBar, MessageSource messages) {
        this.lessonService = lessonService;
        this.actions = actions;
        this.notificationBar = notificationBar;
        this.messages = messages;
        initComponents();
        reload();
    }

    private void initComponents() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        titleLabel = new JLabel(text("today_lessons.title"));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        header.add(titleLabel, BorderLayout.WEST);
        JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerButtons.setOpaque(false);
        if (actions.canOpenLessons()) {
            JButton openBtn = new JButton(text("today_lessons.button.open_lesson"));
            openBtn.addActionListener(e -> onOpenLesson());
            headerButtons.add(openBtn);
        }
        JButton refreshBtn = new JButton(text("today_lessons.button.refresh"));
        refreshBtn.addActionListener(e -> reload());
        headerButtons.add(refreshBtn);
        header.add(headerButtons, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        tableModel = new LessonTableModel();
        table = new JTable(tableModel);
        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class, new CompletedRowRenderer());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtons();
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                LessonDto lesson = selected();
                if (e.getClickCount() == 2 && lesson != null) {
                    actions.showParticipants(TodayLessonsPanel.this, lesson, TodayLessonsPanel.this::reload);
                }
            }
            @Override public void mousePressed(MouseEvent e) {
                maybeShowMenu(e);
            }
            @Override public void mouseReleased(MouseEvent e) {
                maybeShowMenu(e);
            }
        });

        emptyLabel = new JLabel(" ");
        JPanel center = new JPanel(new BorderLayout(0, 4));
        center.setOpaque(false);
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        center.add(emptyLabel, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        participantsBtn = button("calendar.menu.show_participants",
                () -> actions.showParticipants(this, selected(), this::reload));
        addStudentBtn = button("calendar.menu.add_student",
                () -> actions.addStudent(this, selected(), this::reload));
        completeBtn = button("calendar.menu.complete_lesson",
                () -> actions.complete(this, selected(), this::reload));
        undoBtn = button("calendar.menu.undo_completion",
                () -> actions.undoCompletion(this, selected(), this::reload));
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bar.setOpaque(false);
        bar.add(participantsBtn);
        bar.add(addStudentBtn);
        bar.add(completeBtn);
        bar.add(undoBtn);
        add(bar, BorderLayout.SOUTH);
        updateButtons();
    }

    /** Günün derslerini yeniden yükler; ekrana her gelişte çağrılabilir. */
    public void reload() {
        LocalDate today = LocalDate.now();
        titleLabel.setText(messages.getMessage("today_lessons.title_with_date",
                new Object[] {DATE_FMT.format(today)}, Locale.getDefault()));
        new SwingWorker<List<LessonDto>, Void>() {
            @Override protected List<LessonDto> doInBackground() {
                return lessonService.findMyLessonsOn(today);
            }
            @Override protected void done() {
                try {
                    List<LessonDto> lessons = get();
                    tableModel.setRows(lessons);
                    emptyLabel.setText(lessons.isEmpty() ? text("today_lessons.empty") : " ");
                    updateButtons();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    notificationBar.showError(ex.getCause() instanceof BusinessException be
                            ? text(be.getMessage()) : text("error.unexpected"));
                }
            }
        }.execute();
    }

    private void onOpenLesson() {
        LocalTime now = LocalTime.now();
        int minute = now.getMinute() < SLOT_MINUTES ? SLOT_MINUTES : 0;
        LocalTime start = now.withMinute(minute).withSecond(0).withNano(0).plusHours(minute == 0 ? 1 : 0);
        LocalDate date = start.isBefore(now) ? LocalDate.now().plusDays(1) : LocalDate.now();
        if (actions.openLesson(this, date, start) != null) {
            reload();
        }
    }

    private void maybeShowMenu(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        int row = table.rowAtPoint(e.getPoint());
        if (row < 0) {
            return;
        }
        table.setRowSelectionInterval(row, row);
        JPopupMenu menu = new JPopupMenu();
        actions.menuItems(this, tableModel.getRow(row), this::reload).forEach(menu::add);
        menu.show(table, e.getX(), e.getY());
    }

    private void updateButtons() {
        LessonDto lesson = selected();
        participantsBtn.setEnabled(lesson != null);
        addStudentBtn.setVisible(lesson == null || actions.canManage(lesson));
        addStudentBtn.setEnabled(actions.canAddStudent(lesson));
        completeBtn.setVisible(lesson == null || actions.canManage(lesson));
        completeBtn.setEnabled(actions.canComplete(lesson));
        undoBtn.setVisible(actions.canUndoCompletion(lesson));
    }

    private LessonDto selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : tableModel.getRow(table.convertRowIndexToModel(row));
    }

    private JButton button(String key, Runnable action) {
        JButton b = new JButton(text(key));
        b.addActionListener(e -> {
            if (selected() != null) {
                action.run();
            }
        });
        return b;
    }

    private String text(String key) {
        return messages.getMessage(key, null, key, Locale.getDefault());
    }

    private final class CompletedRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                       boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, selected, focus, row, col);
            if (!selected) {
                setBackground(tableModel.getRow(t.convertRowIndexToModel(row)).completed()
                        ? COMPLETED_BG : t.getBackground());
            }
            return this;
        }
    }

    private final class LessonTableModel extends AbstractTableModel {

        private final String[] columns = {
            text("today_lessons.col.time"),
            text("today_lessons.col.instructor"),
            text("today_lessons.col.room"),
            text("today_lessons.col.participants"),
            text("today_lessons.col.status")
        };
        private final List<LessonDto> rows = new ArrayList<>();

        void setRows(List<LessonDto> list) {
            rows.clear();
            rows.addAll(list);
            fireTableDataChanged();
        }

        LessonDto getRow(int row) { return rows.get(row); }

        @Override public int getRowCount() { return rows.size(); }

        @Override public int getColumnCount() { return columns.length; }

        @Override public String getColumnName(int col) { return columns[col]; }

        @Override public Object getValueAt(int row, int col) {
            LessonDto l = rows.get(row);
            return switch (col) {
                case 0 -> TIME_FMT.format(l.startTime()) + " - " + TIME_FMT.format(l.endTime());
                case 1 -> l.instructorName();
                case 2 -> l.roomName();
                case 3 -> l.participantCount() + " / " + l.capacity();
                default -> text(l.completed() ? "today_lessons.status.completed" : "today_lessons.status.open");
            };
        }
    }
}
