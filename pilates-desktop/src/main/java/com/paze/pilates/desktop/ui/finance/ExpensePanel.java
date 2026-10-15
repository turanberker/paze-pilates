package com.paze.pilates.desktop.ui.finance;

import com.paze.pilates.desktop.ui.shared.NotificationBar;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.ExpenseDto;
import com.paze.pilates.dto.MonthlyFinanceSummaryDto;
import com.paze.pilates.service.ExpenseService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Seçilen ayın gelir/gider/net özeti ve gider listesi (ekle, düzenle, sil). */
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Component
public class ExpensePanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(ExpensePanel.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final ExpenseService expenseService;
    private final NotificationBar notificationBar;
    private final ObjectProvider<ExpenseFormDialog> dialogProvider;
    private final MessageSource messages;
    private final NumberFormat moneyFmt = NumberFormat.getNumberInstance(Locale.getDefault());

    private YearMonth month = YearMonth.now();
    private JLabel monthLabel;
    private final JLabel incomeValue = valueLabel();
    private final JLabel incomeHint = hintLabel();
    private final JLabel expenseValue = valueLabel();
    private final JLabel expenseHint = hintLabel();
    private final JLabel netValue = valueLabel();
    private final JLabel netHint = hintLabel();

    private ExpenseTableModel tableModel;
    private JTable table;
    private JButton editBtn;
    private JButton deleteBtn;

    public ExpensePanel(
            ExpenseService expenseService,
            NotificationBar notificationBar,
            ObjectProvider<ExpenseFormDialog> dialogProvider,
            MessageSource messages) {
        this.expenseService = expenseService;
        this.notificationBar = notificationBar;
        this.dialogProvider = dialogProvider;
        this.messages = messages;
        moneyFmt.setMinimumFractionDigits(2);
        moneyFmt.setMaximumFractionDigits(2);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel top = new JPanel(new BorderLayout(0, 16));
        top.setOpaque(false);
        top.add(buildHeaderBar(), BorderLayout.NORTH);
        top.add(buildSummaryCards(), BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildActionBar(), BorderLayout.SOUTH);

        reload();
    }

    private JPanel buildHeaderBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);

        JLabel title = new JLabel(text("expense.section.title"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        bar.add(title, BorderLayout.WEST);

        JPanel monthNav = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        monthNav.setOpaque(false);
        JButton prevBtn = new JButton(text("expense.month.previous"));
        prevBtn.addActionListener(e -> changeMonth(month.minusMonths(1)));
        monthLabel = new JLabel(" ", SwingConstants.CENTER);
        monthLabel.setFont(monthLabel.getFont().deriveFont(Font.BOLD, 14f));
        monthLabel.setPreferredSize(new Dimension(140, monthLabel.getPreferredSize().height));
        JButton nextBtn = new JButton(text("expense.month.next"));
        nextBtn.addActionListener(e -> changeMonth(month.plusMonths(1)));
        JButton currentBtn = new JButton(text("expense.month.current"));
        currentBtn.addActionListener(e -> changeMonth(YearMonth.now()));
        monthNav.add(prevBtn);
        monthNav.add(monthLabel);
        monthNav.add(nextBtn);
        monthNav.add(currentBtn);
        bar.add(monthNav, BorderLayout.CENTER);

        JButton addBtn = new JButton(text("expense.button.add"));
        addBtn.putClientProperty("JButton.buttonType", "default");
        addBtn.addActionListener(e -> onAdd());
        bar.add(addBtn, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildSummaryCards() {
        JPanel cards = new JPanel(new GridLayout(1, 3, 16, 0));
        cards.setOpaque(false);
        cards.add(buildCard(text("expense.summary.income"), incomeValue, incomeHint,
                new Color(220, 252, 231), new Color(22, 163, 74)));
        cards.add(buildCard(text("expense.summary.expense"), expenseValue, expenseHint,
                new Color(254, 226, 226), new Color(220, 38, 38)));
        cards.add(buildCard(text("expense.summary.net"), netValue, netHint,
                new Color(219, 234, 254), new Color(37, 99, 235)));
        return cards;
    }

    private JPanel buildCard(String title, JLabel valueLabel, JLabel hint, Color bg, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(bg);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 1, true),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)));
        card.setPreferredSize(new Dimension(160, 100));
        valueLabel.setForeground(accent);

        JLabel titleLabel = new JLabel(title, SwingConstants.LEFT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, 12f));
        titleLabel.setForeground(new Color(71, 85, 105));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(titleLabel, BorderLayout.NORTH);
        bottom.add(hint, BorderLayout.SOUTH);

        card.add(valueLabel, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private JScrollPane buildTable() {
        tableModel = new ExpenseTableModel();
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(32);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);

        table.getColumnModel().getColumn(0).setPreferredWidth(120); // Tarih
        table.getColumnModel().getColumn(1).setPreferredWidth(420); // Açıklama
        table.getColumnModel().getColumn(2).setPreferredWidth(140); // Tutar
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(2).setCellRenderer(right);

        table.getSelectionModel().addListSelectionListener(
                e -> { if (!e.getValueIsAdjusting()) updateButtons(); });
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) onEdit();
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(0, 320));
        return scroll;
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bar.setOpaque(false);

        editBtn = new JButton(text("expense.button.edit"));
        editBtn.setEnabled(false);
        editBtn.addActionListener(e -> onEdit());
        bar.add(editBtn);

        deleteBtn = new JButton(text("expense.button.delete"));
        deleteBtn.setEnabled(false);
        deleteBtn.addActionListener(e -> onDelete());
        bar.add(deleteBtn);
        return bar;
    }

    private void updateButtons() {
        boolean selected = table.getSelectedRow() >= 0;
        editBtn.setEnabled(selected);
        deleteBtn.setEnabled(selected);
    }

    private void changeMonth(YearMonth newMonth) {
        month = newMonth;
        reload();
    }

    private void onAdd() {
        // Seçili ay içinde bulunulan ay ise bugün, değilse o ayın ilk günü önerilir.
        LocalDate defaultDate = month.equals(YearMonth.now()) ? LocalDate.now() : month.atDay(1);
        ExpenseDto result = dialogProvider.getObject().showForNew(parentFrame(), defaultDate);
        if (result != null) {
            reload();
            notificationBar.showSuccess(text("expense.save.success"));
        }
    }

    private void onEdit() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        ExpenseDto result = dialogProvider.getObject().showForEdit(parentFrame(), tableModel.getExpense(row));
        if (result != null) {
            reload();
            notificationBar.showSuccess(text("expense.save.success"));
        }
    }

    private void onDelete() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int confirm = JOptionPane.showConfirmDialog(
                this,
                text("expense.delete.confirm"),
                text("expense.button.delete"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        Long id = tableModel.getExpense(row).id();
        deleteBtn.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                expenseService.delete(id);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    reload();
                    notificationBar.showSuccess(text("expense.delete.success"));
                } catch (ExecutionException ex) {
                    showError(ex);
                    updateButtons();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /** Seçili ayın özetini ve giderlerini arka planda yeniden yükler. */
    public void reload() {
        YearMonth m = month;
        monthLabel.setText(messages.getMessage("expense.month.label",
                new Object[] {text("month." + m.getMonthValue()), String.valueOf(m.getYear())}, Locale.getDefault()));
        new SwingWorker<MonthData, Void>() {
            @Override protected MonthData doInBackground() {
                return new MonthData(expenseService.getMonthlySummary(m), expenseService.findByMonth(m));
            }
            @Override protected void done() {
                try {
                    MonthData data = get();
                    if (!m.equals(month)) return; // kullanıcı bu arada ay değiştirdi
                    showSummary(data.summary());
                    tableModel.setExpenses(data.expenses());
                    updateButtons();
                } catch (ExecutionException ex) {
                    showError(ex);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void showSummary(MonthlyFinanceSummaryDto s) {
        incomeValue.setText(moneyFmt.format(s.income()));
        incomeHint.setText(format("expense.summary.income_hint", s.incomePackageCount()));
        expenseValue.setText(moneyFmt.format(s.expense()));
        expenseHint.setText(format("expense.summary.expense_hint", s.expenseCount()));
        netValue.setText(moneyFmt.format(s.net()));
        netHint.setText(text(s.net().signum() < 0 ? "expense.summary.net_loss" : "expense.summary.net_profit"));
    }

    private void showError(ExecutionException ex) {
        if (ex.getCause() instanceof BusinessException be) {
            notificationBar.showError(text(be.getMessage()));
        } else {
            log.error("Gider ekranı işlemi başarısız", ex.getCause());
            notificationBar.showError(text("error.unexpected"));
        }
    }

    private Frame parentFrame() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }

    private static JLabel valueLabel() {
        JLabel label = new JLabel("--", SwingConstants.LEFT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 26f));
        return label;
    }

    private static JLabel hintLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
        label.setForeground(new Color(100, 116, 139));
        return label;
    }

    private String format(String key, Object... args) {
        return messages.getMessage(key, args, key, Locale.getDefault());
    }

    private String text(String key) {
        return messages.getMessage(key, null, key, Locale.getDefault());
    }

    private record MonthData(MonthlyFinanceSummaryDto summary, List<ExpenseDto> expenses) {
    }

    // ── Table model ───────────────────────────────────────────────────────────

    private class ExpenseTableModel extends AbstractTableModel {

        private final String[] columns = {
            text("expense.col.date"),
            text("expense.col.description"),
            text("expense.col.amount")
        };
        private final List<ExpenseDto> rows = new ArrayList<>();

        @Override public int getRowCount()    { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int col) { return columns[col]; }

        @Override
        public Object getValueAt(int row, int col) {
            ExpenseDto e = rows.get(row);
            return switch (col) {
                case 0 -> e.expenseDate().format(DATE_FMT);
                case 1 -> e.description();
                case 2 -> moneyFmt.format(e.amount());
                default -> null;
            };
        }

        void setExpenses(List<ExpenseDto> list) {
            rows.clear();
            rows.addAll(list);
            fireTableDataChanged();
        }

        ExpenseDto getExpense(int row) { return rows.get(row); }
    }
}
