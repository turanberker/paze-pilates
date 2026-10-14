package com.paze.pilates.desktop.ui.finance;

import com.paze.pilates.desktop.ui.shared.NotificationBar;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.SessionPackageDto;
import com.paze.pilates.service.SessionPackageService;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
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
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class PackageListPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final SessionPackageService packageService;
    private final NotificationBar notificationBar;
    private final ObjectProvider<PackageFormDialog> dialogProvider;
    private final MessageSource messages;

    private PackageTableModel tableModel;
    private JTable table;
    private JButton editBtn;
    private JButton toggleActiveBtn;

    public PackageListPanel(
            SessionPackageService packageService,
            NotificationBar notificationBar,
            ObjectProvider<PackageFormDialog> dialogProvider,
            MessageSource messages) {
        this.packageService = packageService;
        this.notificationBar = notificationBar;
        this.dialogProvider = dialogProvider;
        this.messages = messages;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        add(buildHeaderBar(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildActionBar(), BorderLayout.SOUTH);

        loadPackages();
    }

    private JPanel buildHeaderBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);

        JLabel title = new JLabel(text("package.section.title"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        bar.add(title, BorderLayout.WEST);

        JButton addBtn = new JButton(text("package.button.add"));
        addBtn.putClientProperty("JButton.buttonType", "default");
        addBtn.addActionListener(e -> onAdd());
        bar.add(addBtn, BorderLayout.EAST);
        return bar;
    }

    private JScrollPane buildTable() {
        tableModel = new PackageTableModel();
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(32);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);

        table.getColumnModel().getColumn(0).setPreferredWidth(220); // Ad
        table.getColumnModel().getColumn(1).setPreferredWidth(110); // Seans
        table.getColumnModel().getColumn(2).setPreferredWidth(130); // Ücret
        table.getColumnModel().getColumn(3).setPreferredWidth(120); // Başlangıç
        table.getColumnModel().getColumn(4).setPreferredWidth(80);  // Durum

        table.getSelectionModel().addListSelectionListener(
                e -> { if (!e.getValueIsAdjusting()) updateButtons(); });
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) onEdit();
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(0, 400));
        return scroll;
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bar.setOpaque(false);

        editBtn = new JButton(text("package.button.edit"));
        editBtn.setEnabled(false);
        editBtn.addActionListener(e -> onEdit());
        bar.add(editBtn);

        toggleActiveBtn = new JButton(text("package.button.deactivate"));
        toggleActiveBtn.setEnabled(false);
        toggleActiveBtn.addActionListener(e -> onToggleActive());
        bar.add(toggleActiveBtn);
        return bar;
    }

    private void updateButtons() {
        int row = table.getSelectedRow();
        editBtn.setEnabled(row >= 0);
        toggleActiveBtn.setEnabled(row >= 0);
        if (row >= 0) {
            toggleActiveBtn.setText(text(tableModel.getPackage(row).active()
                    ? "package.button.deactivate" : "package.button.activate"));
        }
    }

    /** Aktiflik değişimi genel save yerine amaca özel servis metoduyla yapılır. */
    private void onToggleActive() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        SessionPackageDto pkg = tableModel.getPackage(row);
        toggleActiveBtn.setEnabled(false);
        new SwingWorker<SessionPackageDto, Void>() {
            @Override protected SessionPackageDto doInBackground() {
                return pkg.active() ? packageService.deactivate(pkg.id()) : packageService.activate(pkg.id());
            }
            @Override protected void done() {
                try {
                    SessionPackageDto result = get();
                    loadPackages();
                    notificationBar.showSuccess(text(result.active()
                            ? "package.activate.success" : "package.deactivate.success"));
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

    private void onAdd() {
        SessionPackageDto result = dialogProvider.getObject().showForNew(parentFrame());
        if (result != null) {
            loadPackages();
            notificationBar.showSuccess(text("package.save.success"));
        }
    }

    private void onEdit() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        SessionPackageDto result = dialogProvider.getObject()
                .showForEdit(parentFrame(), tableModel.getPackage(row));
        if (result != null) {
            loadPackages();
            notificationBar.showSuccess(text("package.save.success"));
        }
    }

    private void loadPackages() {
        new SwingWorker<List<SessionPackageDto>, Void>() {
            @Override protected List<SessionPackageDto> doInBackground() {
                return packageService.findAll();
            }
            @Override protected void done() {
                try {
                    tableModel.setPackages(get());
                    updateButtons();
                } catch (InterruptedException | ExecutionException ex) {
                    notificationBar.showError(ex.getMessage());
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

    // ── Table model ───────────────────────────────────────────────────────────

    private class PackageTableModel extends AbstractTableModel {

        private final String[] columns = {
            text("package.col.name"),
            text("package.col.sessions"),
            text("package.col.price"),
            text("package.col.startdate"),
            text("package.col.status")
        };
        private final NumberFormat priceFmt = NumberFormat.getNumberInstance(Locale.getDefault());
        private final List<SessionPackageDto> rows = new ArrayList<>();

        PackageTableModel() {
            priceFmt.setMinimumFractionDigits(2);
            priceFmt.setMaximumFractionDigits(2);
        }

        @Override public int getRowCount()    { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int col) { return columns[col]; }

        @Override
        public Object getValueAt(int row, int col) {
            SessionPackageDto p = rows.get(row);
            return switch (col) {
                case 0 -> p.name();
                case 1 -> p.sessionCount();
                case 2 -> priceFmt.format(p.price());
                case 3 -> p.startDate().format(DATE_FMT);
                case 4 -> text(p.active() ? "package.status.active" : "package.status.passive");
                default -> null;
            };
        }

        void setPackages(List<SessionPackageDto> list) {
            rows.clear();
            rows.addAll(list);
            fireTableDataChanged();
        }

        SessionPackageDto getPackage(int row) { return rows.get(row); }
    }
}
