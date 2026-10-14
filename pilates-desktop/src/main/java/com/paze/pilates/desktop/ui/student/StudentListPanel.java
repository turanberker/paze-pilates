package com.paze.pilates.desktop.ui.student;

import com.paze.pilates.desktop.ui.shared.NotificationBar;
import com.paze.pilates.dto.StudentDto;
import com.paze.pilates.service.StudentService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class StudentListPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final StudentService studentService;
    private final NotificationBar notificationBar;
    private final ObjectProvider<StudentFormDialog> dialogProvider;
    private final MessageSource messages;

    private StudentTableModel tableModel;
    private JTable table;
    private JTextField searchField;
    private JButton editBtn;
    private JButton deactivateBtn;

    public StudentListPanel(
            StudentService studentService,
            NotificationBar notificationBar,
            ObjectProvider<StudentFormDialog> dialogProvider,
            MessageSource messages) {
        this.studentService = studentService;
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

        loadStudents(null);
    }

    private JPanel buildHeaderBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);

        JLabel title = new JLabel(text("student.section.title"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setForeground(new Color(30, 41, 59));
        bar.add(title, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setOpaque(false);

        searchField = new JTextField(20);
        searchField.putClientProperty("JTextField.placeholderText", text("student.search.placeholder"));
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                loadStudents(searchField.getText());
            }
        });

        JButton addBtn = new JButton(text("student.button.add"));
        addBtn.putClientProperty("JButton.buttonType", "default");
        addBtn.addActionListener(e -> onAddStudent());

        rightPanel.add(searchField);
        rightPanel.add(addBtn);
        bar.add(rightPanel, BorderLayout.EAST);
        return bar;
    }

    private JScrollPane buildTable() {
        tableModel = new StudentTableModel();
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(32);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);

        // Sütun genişlikleri
        table.getColumnModel().getColumn(0).setPreferredWidth(160); // Ad
        table.getColumnModel().getColumn(1).setPreferredWidth(160); // Soyad
        table.getColumnModel().getColumn(2).setPreferredWidth(120); // Telefon
        table.getColumnModel().getColumn(3).setPreferredWidth(180); // E-posta
        table.getColumnModel().getColumn(4).setPreferredWidth(100); // Doğum

        table.getSelectionModel().addListSelectionListener(
                e -> { if (!e.getValueIsAdjusting()) updateButtons(); });
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) onEditStudent();
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(0, 400));
        return scroll;
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bar.setOpaque(false);

        editBtn = new JButton(text("student.button.edit"));
        editBtn.setEnabled(false);
        editBtn.addActionListener(e -> onEditStudent());

        deactivateBtn = new JButton(text("student.button.deactivate"));
        deactivateBtn.setEnabled(false);
        deactivateBtn.addActionListener(e -> onDeactivateStudent());

        bar.add(editBtn);
        bar.add(deactivateBtn);
        return bar;
    }

    private void updateButtons() {
        boolean sel = table.getSelectedRow() >= 0;
        editBtn.setEnabled(sel);
        deactivateBtn.setEnabled(sel);
    }

    private void onAddStudent() {
        StudentDto result = dialogProvider.getObject().showForNew(parentFrame());
        if (result != null) {
            tableModel.addStudent(result);
            notificationBar.showSuccess(text("student.save.success"));
        }
    }

    private void onEditStudent() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        StudentDto result = dialogProvider.getObject()
                .showForEdit(parentFrame(), tableModel.getStudent(row));
        if (result != null) {
            tableModel.updateStudent(row, result);
            notificationBar.showSuccess(text("student.save.success"));
        }
    }

    private void onDeactivateStudent() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int confirm = JOptionPane.showConfirmDialog(
                this,
                text("student.deactivate.confirm"),
                text("student.button.deactivate"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            studentService.deactivate(tableModel.getStudent(row).id());
            tableModel.removeStudent(row);
            notificationBar.showSuccess(text("student.deactivate.success"));
        }
    }

    private void loadStudents(String term) {
        new SwingWorker<List<StudentDto>, Void>() {
            @Override protected List<StudentDto> doInBackground() {
                return studentService.search(term);
            }
            @Override protected void done() {
                try {
                    tableModel.setStudents(get());
                } catch (InterruptedException | ExecutionException ex) {
                    notificationBar.showError(ex.getMessage());
                }
            }
        }.execute();
    }

    private Frame parentFrame() {
        return (Frame) javax.swing.SwingUtilities.getWindowAncestor(this);
    }

    private String text(String key) {
        return messages.getMessage(key, null, key, Locale.getDefault());
    }

    // ── Table model ───────────────────────────────────────────────────────────

    private class StudentTableModel extends AbstractTableModel {

        private final String[] columns = {
            text("student.col.firstname"),
            text("student.col.lastname"),
            text("student.col.phone"),
            text("student.col.email"),
            text("student.col.birthdate")
        };
        private final List<StudentDto> rows = new ArrayList<>();

        @Override public int getRowCount()    { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int col) { return columns[col]; }

        @Override
        public Object getValueAt(int row, int col) {
            StudentDto s = rows.get(row);
            return switch (col) {
                case 0 -> s.firstName();
                case 1 -> s.lastName();
                case 2 -> s.phone() != null ? s.phone() : "";
                case 3 -> s.email() != null ? s.email() : "";
                case 4 -> s.birthDate() != null ? s.birthDate().format(DATE_FMT) : "";
                default -> null;
            };
        }

        void setStudents(List<StudentDto> list) {
            rows.clear();
            rows.addAll(list);
            fireTableDataChanged();
        }

        void addStudent(StudentDto s) {
            rows.add(s);
            fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
        }

        void updateStudent(int row, StudentDto s) {
            rows.set(row, s);
            fireTableRowsUpdated(row, row);
        }

        void removeStudent(int row) {
            rows.remove(row);
            fireTableRowsDeleted(row, row);
        }

        StudentDto getStudent(int row) { return rows.get(row); }
    }
}
