package com.paze.pilates.desktop.ui.student;

import com.paze.pilates.desktop.ui.shared.NotificationBar;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.RemainingSessionFilter;
import com.paze.pilates.dto.StudentPackageDto;
import com.paze.pilates.service.StudentPackageService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.MessageSource;

/** Öğrencilere atanan paketlerin listesi; paket atama ve ödeme alma buradan yapılır. */
@org.springframework.context.annotation.Scope("prototype")
@org.springframework.stereotype.Component
public class StudentPackageListPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Color FINISHED_BG = new Color(254, 202, 202);
    private static final Color LOW_BG = new Color(254, 240, 138);
    private static final int LOW_THRESHOLD = 2;

    private final StudentPackageService studentPackageService;
    private final NotificationBar notificationBar;
    private final ObjectProvider<StudentPackageAssignDialog> assignDialogProvider;
    private final ObjectProvider<CompletePaymentDialog> paymentDialogProvider;
    private final MessageSource messages;

    private StudentPackageTableModel tableModel;
    private JTable table;
    private JTextField nameField;
    private JComboBox<RemainingSessionFilter> filterCombo;
    private JButton paymentBtn;
    private JButton cancelBtn;

    public StudentPackageListPanel(
            StudentPackageService studentPackageService,
            NotificationBar notificationBar,
            ObjectProvider<StudentPackageAssignDialog> assignDialogProvider,
            ObjectProvider<CompletePaymentDialog> paymentDialogProvider,
            MessageSource messages) {
        this.studentPackageService = studentPackageService;
        this.notificationBar = notificationBar;
        this.assignDialogProvider = assignDialogProvider;
        this.paymentDialogProvider = paymentDialogProvider;
        this.messages = messages;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        add(buildHeaderBar(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildActionBar(), BorderLayout.SOUTH);

        reload();
    }

    private JPanel buildHeaderBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);

        JLabel title = new JLabel(text("student_package.section.title"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        bar.add(title, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        nameField = new JTextField(18);
        nameField.putClientProperty("JTextField.placeholderText", text("student_package.search.placeholder"));
        nameField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                reload();
            }
        });

        filterCombo = new JComboBox<>(RemainingSessionFilter.values());
        filterCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RemainingSessionFilter f) {
                    setText(text("student_package.filter." + f.name().toLowerCase(Locale.ROOT)));
                }
                return this;
            }
        });
        filterCombo.addActionListener(e -> reload());

        JButton assignBtn = new JButton(text("student_package.button.assign"));
        assignBtn.putClientProperty("JButton.buttonType", "default");
        assignBtn.addActionListener(e -> onAssign());

        right.add(nameField);
        right.add(filterCombo);
        right.add(assignBtn);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JScrollPane buildTable() {
        tableModel = new StudentPackageTableModel();
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(32);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class, new RemainingRowRenderer());

        table.getColumnModel().getColumn(0).setPreferredWidth(180); // Öğrenci
        table.getColumnModel().getColumn(1).setPreferredWidth(160); // Paket
        table.getColumnModel().getColumn(2).setPreferredWidth(90);  // Toplam
        table.getColumnModel().getColumn(3).setPreferredWidth(90);  // Kalan
        table.getColumnModel().getColumn(4).setPreferredWidth(110); // Ödeme durumu
        table.getColumnModel().getColumn(5).setPreferredWidth(110); // Ödeme tarihi
        table.getColumnModel().getColumn(6).setPreferredWidth(110); // Atama tarihi

        table.getSelectionModel().addListSelectionListener(
                e -> { if (!e.getValueIsAdjusting()) updateButtons(); });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(0, 400));
        return scroll;
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bar.setOpaque(false);

        paymentBtn = new JButton(text("student_package.button.complete_payment"));
        paymentBtn.setEnabled(false);
        paymentBtn.addActionListener(e -> onCompletePayment());
        bar.add(paymentBtn);

        cancelBtn = new JButton(text("student_package.button.cancel"));
        cancelBtn.setEnabled(false);
        cancelBtn.addActionListener(e -> onCancel());
        bar.add(cancelBtn);
        return bar;
    }

    /** Ödemesi alınmış satırda ödeme alma ve iptal kapalıdır. */
    private void updateButtons() {
        int row = table.getSelectedRow();
        boolean unpaid = row >= 0 && !tableModel.getRow(row).paid();
        paymentBtn.setEnabled(unpaid);
        cancelBtn.setEnabled(unpaid);
    }

    private void onCancel() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int confirm = JOptionPane.showConfirmDialog(
                this,
                text("student_package.cancel.confirm"),
                text("student_package.button.cancel"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        Long id = tableModel.getRow(row).id();
        cancelBtn.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                studentPackageService.cancel(id);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    reload();
                    notificationBar.showSuccess(text("student_package.cancel.success"));
                } catch (ExecutionException ex) {
                    notificationBar.showError(ex.getCause() instanceof BusinessException be
                            ? text(be.getMessage()) : text("error.unexpected"));
                    updateButtons();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void onAssign() {
        StudentPackageDto result = assignDialogProvider.getObject().showDialog(parentFrame());
        if (result != null) {
            reload();
            notificationBar.showSuccess(result.usedSessions() > 0
                    ? messages.getMessage("student_package.assign.success_with_debt",
                            new Object[] {result.usedSessions()}, Locale.getDefault())
                    : text("student_package.assign.success"));
        }
    }

    private void onCompletePayment() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        StudentPackageDto result = paymentDialogProvider.getObject()
                .showDialog(parentFrame(), tableModel.getRow(row));
        if (result != null) {
            reload();
            notificationBar.showSuccess(text("student_package.payment.success"));
        }
    }

    /** Her ekrana gelişte güncel veriyi görmek için dışarıdan da çağrılabilir. */
    public void reload() {
        String name = nameField.getText();
        RemainingSessionFilter filter = (RemainingSessionFilter) filterCombo.getSelectedItem();
        new SwingWorker<List<StudentPackageDto>, Void>() {
            @Override protected List<StudentPackageDto> doInBackground() {
                return studentPackageService.search(name, filter);
            }
            @Override protected void done() {
                try {
                    tableModel.setRows(get());
                    updateButtons();
                } catch (InterruptedException | ExecutionException ex) {
                    notificationBar.showError(text("error.unexpected"));
                }
            }
        }.execute();
    }

    private Frame parentFrame() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }

    private String text(String key) {
        return messages.getMessage(key, null, key, Locale.getDefault());
    }

    // ── Renklendirme: kalan 0 → kırmızı, 1-2 → sarı ───────────────────────────

    private class RemainingRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                int remaining = tableModel.getRow(tbl.convertRowIndexToModel(row)).remainingSessions();
                if (remaining == 0) {
                    setBackground(FINISHED_BG);
                } else if (remaining <= LOW_THRESHOLD) {
                    setBackground(LOW_BG);
                } else {
                    setBackground(tbl.getBackground());
                }
            }
            return this;
        }
    }

    // ── Table model ───────────────────────────────────────────────────────────

    private class StudentPackageTableModel extends AbstractTableModel {

        private final String[] columns = {
            text("student_package.col.student"),
            text("student_package.col.package"),
            text("student_package.col.total"),
            text("student_package.col.remaining"),
            text("student_package.col.payment_status"),
            text("student_package.col.payment_date"),
            text("student_package.col.assigned_date")
        };
        private final List<StudentPackageDto> rows = new ArrayList<>();

        @Override public int getRowCount()    { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int col) { return columns[col]; }

        @Override
        public Object getValueAt(int row, int col) {
            StudentPackageDto sp = rows.get(row);
            return switch (col) {
                case 0 -> sp.studentName();
                case 1 -> sp.packageName();
                case 2 -> sp.totalSessions();
                case 3 -> sp.remainingSessions();
                case 4 -> text(sp.paid() ? "student_package.status.paid" : "student_package.status.unpaid");
                case 5 -> sp.paymentDate() == null ? "" : sp.paymentDate().format(DATE_FMT);
                case 6 -> sp.assignedDate().format(DATE_FMT);
                default -> null;
            };
        }

        void setRows(List<StudentPackageDto> list) {
            rows.clear();
            rows.addAll(list);
            fireTableDataChanged();
        }

        StudentPackageDto getRow(int row) { return rows.get(row); }
    }
}
