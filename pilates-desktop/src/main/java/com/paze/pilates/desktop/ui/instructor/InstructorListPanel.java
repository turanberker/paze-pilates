package com.paze.pilates.desktop.ui.instructor;

import com.paze.pilates.desktop.ui.shared.NotificationBar;
import com.paze.pilates.dto.InstructorDto;
import com.paze.pilates.service.InstructorService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Component
public class InstructorListPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(InstructorListPanel.class);

    private final InstructorService instructorService;
    private final NotificationBar notificationBar;
    private final ObjectProvider<InstructorFormDialog> dialogProvider;
    private final MessageSource messages;

    private InstructorTableModel tableModel;
    private JTable table;
    private JButton editBtn;
    private JButton deleteBtn;

    public InstructorListPanel(
            InstructorService instructorService,
            NotificationBar notificationBar,
            ObjectProvider<InstructorFormDialog> dialogProvider,
            MessageSource messages) {
        this.instructorService = instructorService;
        this.notificationBar = notificationBar;
        this.dialogProvider = dialogProvider;
        this.messages = messages;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        add(buildHeaderBar(), BorderLayout.NORTH);
        add(buildTable(),     BorderLayout.CENTER);
        add(buildActionBar(), BorderLayout.SOUTH);

        loadInstructors();
    }

    private JPanel buildHeaderBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);

        JLabel title = new JLabel(text("instructor.section.title"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setForeground(new Color(30, 41, 59));
        bar.add(title, BorderLayout.WEST);

        JButton addBtn = new JButton(text("instructor.button.add"));
        addBtn.putClientProperty("JButton.buttonType", "default");
        addBtn.addActionListener(e -> onAdd());
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        wrap.setOpaque(false);
        wrap.add(addBtn);
        bar.add(wrap, BorderLayout.EAST);

        return bar;
    }

    private JScrollPane buildTable() {
        tableModel = new InstructorTableModel();
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(32);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);

        table.getSelectionModel().addListSelectionListener(
                e -> { if (!e.getValueIsAdjusting()) updateButtons(); });
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) onEdit();
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(0, 300));
        return scroll;
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bar.setOpaque(false);

        editBtn = new JButton(text("instructor.button.edit"));
        editBtn.setEnabled(false);
        editBtn.addActionListener(e -> onEdit());

        deleteBtn = new JButton(text("instructor.button.delete"));
        deleteBtn.setEnabled(false);
        deleteBtn.addActionListener(e -> onDelete());

        bar.add(editBtn);
        bar.add(deleteBtn);
        return bar;
    }

    private void updateButtons() {
        boolean sel = table.getSelectedRow() >= 0;
        editBtn.setEnabled(sel);
        deleteBtn.setEnabled(sel);
    }

    private void onAdd() {
        InstructorDto result = dialogProvider.getObject().showForNew(parentFrame());
        if (result != null) {
            tableModel.add(result);
            notificationBar.showSuccess(text("instructor.save.success"));
        }
    }

    private void onEdit() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        InstructorDto result = dialogProvider.getObject().showForEdit(parentFrame(), tableModel.get(row));
        if (result != null) {
            tableModel.update(row, result);
            notificationBar.showSuccess(text("instructor.save.success"));
        }
    }

    private void onDelete() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        InstructorDto instructor = tableModel.get(row);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                text("instructor.delete.confirm"),
                text("instructor.button.delete"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                instructorService.deleteInstructor(instructor.id());
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    tableModel.remove(instructor);
                    notificationBar.showSuccess(text("instructor.delete.success"));
                } catch (InterruptedException | ExecutionException ex) {
                    log.error("Hoca silinemedi", ex);
                    notificationBar.showError(text("error.unexpected"));
                }
            }
        }.execute();
    }

    private void loadInstructors() {
        new SwingWorker<List<InstructorDto>, Void>() {
            @Override protected List<InstructorDto> doInBackground() {
                return instructorService.getInstructors();
            }
            @Override protected void done() {
                try {
                    tableModel.setAll(get());
                } catch (InterruptedException | ExecutionException ex) {
                    log.error("Hocalar yüklenemedi", ex);
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

    // ── Table model ───────────────────────────────────────────────────────

    private class InstructorTableModel extends AbstractTableModel {

        private final String[] columns = {
            text("instructor.col.first_name"),
            text("instructor.col.last_name")
        };
        private final List<InstructorDto> rows = new ArrayList<>();

        @Override public int getRowCount()    { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int col) { return columns[col]; }

        @Override
        public Object getValueAt(int row, int col) {
            InstructorDto i = rows.get(row);
            return switch (col) {
                case 0 -> i.firstName();
                case 1 -> i.lastName();
                default -> null;
            };
        }

        void setAll(List<InstructorDto> list) {
            rows.clear();
            rows.addAll(list);
            fireTableDataChanged();
        }

        void add(InstructorDto instructor) {
            rows.add(instructor);
            fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
        }

        void update(int row, InstructorDto instructor) {
            rows.set(row, instructor);
            fireTableRowsUpdated(row, row);
        }

        /** Silme arka planda biterken tablo değişmiş olabilir; satır ID ile bulunur. */
        void remove(InstructorDto instructor) {
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).id().equals(instructor.id())) {
                    rows.remove(i);
                    fireTableRowsDeleted(i, i);
                    return;
                }
            }
        }

        InstructorDto get(int row) { return rows.get(row); }
    }
}
