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
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.MessageSource;

/** Haftalık takvim; boş saate çift tıklayarak ders açılır. */
@org.springframework.stereotype.Component
public class CalendarPanel extends JPanel {

    static final LocalTime DAY_START = LocalTime.of(7, 0);
    static final LocalTime DAY_END = LocalTime.of(22, 0);
    static final int SLOT_MINUTES = 30;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Color LESSON_BG = new Color(191, 219, 254);

    private final LessonService lessonService;
    private final NotificationBar notificationBar;
    private final ObjectProvider<LessonFormDialog> lessonDialogProvider;
    private final MessageSource messages;

    private LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
    private WeekTableModel tableModel;
    private JTable table;
    private JLabel weekLabel;

    public CalendarPanel(
            LessonService lessonService,
            NotificationBar notificationBar,
            ObjectProvider<LessonFormDialog> lessonDialogProvider,
            MessageSource messages) {
        this.lessonService = lessonService;
        this.notificationBar = notificationBar;
        this.lessonDialogProvider = lessonDialogProvider;
        this.messages = messages;
        initComponents();
        loadWeek();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel top = new JPanel(new BorderLayout());
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton prev = new JButton(text("calendar.button.previous"));
        JButton today = new JButton(text("calendar.button.today"));
        JButton next = new JButton(text("calendar.button.next"));
        prev.addActionListener(e -> moveWeek(weekStart.minusWeeks(1)));
        today.addActionListener(e -> moveWeek(LocalDate.now().with(DayOfWeek.MONDAY)));
        next.addActionListener(e -> moveWeek(weekStart.plusWeeks(1)));
        weekLabel = new JLabel();
        weekLabel.setFont(weekLabel.getFont().deriveFont(Font.BOLD, 15f));
        nav.add(prev);
        nav.add(today);
        nav.add(next);
        nav.add(weekLabel);
        top.add(nav, BorderLayout.WEST);
        JLabel hint = new JLabel(text("calendar.hint"));
        hint.setHorizontalAlignment(SwingConstants.RIGHT);
        top.add(hint, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        tableModel = new WeekTableModel();
        table = new JTable(tableModel);
        table.setRowHeight(26);
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, new SlotRenderer());
        table.getColumnModel().getColumn(0).setMaxWidth(70);
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    onSlotActivated(table.rowAtPoint(e.getPoint()), table.columnAtPoint(e.getPoint()));
                }
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void moveWeek(LocalDate newWeekStart) {
        weekStart = newWeekStart;
        loadWeek();
    }

    private void loadWeek() {
        LocalDate from = weekStart;
        weekLabel.setText(messages.getMessage("calendar.week_title",
                new Object[] {DATE_FMT.format(from), DATE_FMT.format(from.plusDays(6))}, Locale.getDefault()));
        tableModel.setWeek(from, List.of());
        new SwingWorker<List<LessonDto>, Void>() {
            @Override protected List<LessonDto> doInBackground() {
                return lessonService.findBetween(from, from.plusDays(6));
            }
            @Override protected void done() {
                try {
                    if (from.equals(weekStart)) {
                        tableModel.setWeek(from, get());
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    notificationBar.showError(ex.getCause() instanceof BusinessException be
                            ? text(be.getMessage()) : text("error.unexpected"));
                }
            }
        }.execute();
    }

    private void onSlotActivated(int row, int col) {
        if (row < 0 || col < 1) {
            return;
        }
        LocalDate date = weekStart.plusDays(col - 1L);
        LocalTime start = WeekTableModel.slotStart(row);
        LessonDto created = lessonDialogProvider.getObject()
                .showDialog((Frame) SwingUtilities.getWindowAncestor(this), date, start);
        if (created != null) {
            notificationBar.showSuccess(text("calendar.lesson.created"));
            if (!created.date().isBefore(weekStart) && !created.date().isAfter(weekStart.plusDays(6))) {
                loadWeek();
            } else {
                moveWeek(created.date().with(DayOfWeek.MONDAY));
            }
        }
    }

    private String text(String key) {
        return messages.getMessage(key, null, key, Locale.getDefault());
    }

    /** Satırlar 30 dakikalık dilimler, sütunlar saat + haftanın 7 günü. */
    private final class WeekTableModel extends AbstractTableModel {

        private LocalDate start = weekStart;
        private List<LessonDto> lessons = new ArrayList<>();

        void setWeek(LocalDate weekStart, List<LessonDto> lessons) {
            this.start = weekStart;
            this.lessons = new ArrayList<>(lessons);
            fireTableStructureChanged();
            if (table != null) {
                table.getColumnModel().getColumn(0).setMaxWidth(70);
            }
        }

        static LocalTime slotStart(int row) {
            return DAY_START.plusMinutes((long) row * SLOT_MINUTES);
        }

        @Override public int getRowCount() {
            return (int) (java.time.Duration.between(DAY_START, DAY_END).toMinutes() / SLOT_MINUTES);
        }

        @Override public int getColumnCount() {
            return 8;
        }

        @Override public String getColumnName(int col) {
            if (col == 0) {
                return text("calendar.col.time");
            }
            LocalDate d = start.plusDays(col - 1L);
            return d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault()) + " " + d.getDayOfMonth();
        }

        /** Hücre: o dilimle kesişen derslerin etiketi (hücre başında "hoca · oda"). */
        @Override public Object getValueAt(int row, int col) {
            LocalTime slot = slotStart(row);
            if (col == 0) {
                return TIME_FMT.format(slot);
            }
            LocalDate date = start.plusDays(col - 1L);
            LocalTime slotEnd = slot.plusMinutes(SLOT_MINUTES);
            List<String> labels = new ArrayList<>();
            for (LessonDto l : lessons) {
                if (l.date().equals(date) && l.startTime().isBefore(slotEnd) && l.endTime().isAfter(slot)) {
                    boolean startsHere = !l.startTime().isBefore(slot);
                    labels.add(startsHere || row == 0
                            ? messages.getMessage("calendar.lesson.cell",
                                    new Object[] {l.instructorName(), l.roomName()}, Locale.getDefault())
                            : "");
                }
            }
            return labels.isEmpty() ? null : String.join(" | ", labels);
        }

        @Override public boolean isCellEditable(int row, int col) {
            return false;
        }
    }

    private static final class SlotRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                       boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, selected, focus, row, col);
            if (col == 0) {
                setForeground(t.getForeground());
                setBackground(t.getTableHeader().getBackground());
            } else if (value != null) {
                setBackground(LESSON_BG);
                setForeground(Color.BLACK);
            } else {
                setBackground(t.getBackground());
            }
            return this;
        }
    }
}
