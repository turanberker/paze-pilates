package com.paze.pilates.desktop.ui.user;

import com.paze.pilates.desktop.ui.shared.NotificationBar;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.UserDto;
import com.paze.pilates.service.UserService;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Tanımlamalar > Kullanıcılar: hoca için kullanıcı aç, şifre sıfırla, rol değiştir, pasife al. */
@Lazy
@Component
public class UserListPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(UserListPanel.class);

    private final UserService userService;
    private final NotificationBar notificationBar;
    private final ObjectProvider<UserFormDialog> dialogProvider;
    private final MessageSource messages;

    private UserTableModel tableModel;
    private JTable table;
    private JButton resetBtn;
    private JButton roleBtn;
    private JButton toggleBtn;

    public UserListPanel(
            UserService userService,
            NotificationBar notificationBar,
            ObjectProvider<UserFormDialog> dialogProvider,
            MessageSource messages) {
        this.userService = userService;
        this.notificationBar = notificationBar;
        this.dialogProvider = dialogProvider;
        this.messages = messages;
        initComponents();
        loadUsers();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel(text("user.section.title"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        header.add(title, BorderLayout.WEST);
        JButton addBtn = new JButton(text("user.button.add"));
        addBtn.putClientProperty("JButton.buttonType", "default");
        addBtn.addActionListener(e -> onAdd());
        JPanel addWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        addWrap.setOpaque(false);
        addWrap.add(addBtn);
        header.add(addWrap, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        tableModel = new UserTableModel();
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(32);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtons();
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(0, 300));
        add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actions.setOpaque(false);
        resetBtn = new JButton(text("user.button.reset_password"));
        resetBtn.addActionListener(e -> onResetPassword());
        roleBtn = new JButton(text("user.button.change_role"));
        roleBtn.addActionListener(e -> onChangeRole());
        toggleBtn = new JButton(text("user.button.deactivate"));
        toggleBtn.addActionListener(e -> onToggleActive());
        actions.add(resetBtn);
        actions.add(roleBtn);
        actions.add(toggleBtn);
        add(actions, BorderLayout.SOUTH);
        updateButtons();
    }

    private void updateButtons() {
        int row = table.getSelectedRow();
        boolean selected = row >= 0;
        resetBtn.setEnabled(selected);
        roleBtn.setEnabled(selected);
        toggleBtn.setEnabled(selected);
        if (selected) {
            toggleBtn.setText(text(tableModel.get(row).active() ? "user.button.deactivate" : "user.button.activate"));
        }
    }

    private void onAdd() {
        UserDto created = dialogProvider.getObject().showDialog(parentFrame(), UserFormDialog.Mode.CREATE, null);
        if (created != null) {
            tableModel.upsert(created);
            notificationBar.showSuccess(text("user.create.success"));
        }
    }

    private void onResetPassword() {
        UserDto user = selected();
        if (user == null) {
            return;
        }
        UserDto result = dialogProvider.getObject()
                .showDialog(parentFrame(), UserFormDialog.Mode.RESET_PASSWORD, user);
        if (result != null) {
            tableModel.upsert(result);
            notificationBar.showSuccess(text("user.reset_password.success"));
        }
    }

    private void onChangeRole() {
        UserDto user = selected();
        if (user == null) {
            return;
        }
        UserDto result = dialogProvider.getObject()
                .showDialog(parentFrame(), UserFormDialog.Mode.CHANGE_ROLE, user);
        if (result != null) {
            tableModel.upsert(result);
            notificationBar.showSuccess(text("user.change_role.success"));
        }
    }

    private void onToggleActive() {
        UserDto user = selected();
        if (user == null) {
            return;
        }
        boolean activate = !user.active();
        runUserAction(() -> activate ? userService.activate(user.id()) : userService.deactivate(user.id()),
                activate ? "user.activate.success" : "user.deactivate.success");
    }

    private void runUserAction(Supplier<UserDto> action, String successKey) {
        new SwingWorker<UserDto, Void>() {
            @Override protected UserDto doInBackground() {
                return action.get();
            }
            @Override protected void done() {
                try {
                    tableModel.upsert(get());
                    notificationBar.showSuccess(text(successKey));
                } catch (ExecutionException ex) {
                    if (ex.getCause() instanceof BusinessException be) {
                        notificationBar.showError(text(be.getMessage()));
                    } else {
                        log.error("Kullanıcı işlemi başarısız", ex.getCause());
                        notificationBar.showError(text("error.unexpected"));
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void loadUsers() {
        new SwingWorker<List<UserDto>, Void>() {
            @Override protected List<UserDto> doInBackground() {
                return userService.findAll();
            }
            @Override protected void done() {
                try {
                    tableModel.setAll(get());
                } catch (InterruptedException | ExecutionException ex) {
                    log.error("Kullanıcılar yüklenemedi", ex);
                    notificationBar.showError(text("error.unexpected"));
                }
            }
        }.execute();
    }

    private UserDto selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : tableModel.get(row);
    }

    private Frame parentFrame() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }

    private String text(String key) {
        return messages.getMessage(key, null, key, Locale.getDefault());
    }

    private class UserTableModel extends AbstractTableModel {

        private final String[] columns = {
            text("user.col.username"), text("user.col.full_name"), text("user.col.role"),
            text("user.col.instructor"), text("user.col.status")
        };
        private final List<UserDto> rows = new ArrayList<>();

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int col) { return columns[col]; }

        @Override
        public Object getValueAt(int row, int col) {
            UserDto u = rows.get(row);
            return switch (col) {
                case 0 -> u.username();
                case 1 -> u.fullName();
                case 2 -> text("role." + u.role().name());
                case 3 -> u.instructorName() == null ? "" : u.instructorName();
                case 4 -> {
                    if (!u.active()) {
                        yield text("user.status.inactive");
                    }
                    yield text(u.mustChangePassword() ? "user.status.must_change_password" : "user.status.active");
                }
                default -> null;
            };
        }

        void setAll(List<UserDto> list) {
            rows.clear();
            rows.addAll(list);
            fireTableDataChanged();
        }

        /** Satır ID ile bulunur; yoksa sona eklenir. */
        void upsert(UserDto user) {
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).id().equals(user.id())) {
                    rows.set(i, user);
                    fireTableRowsUpdated(i, i);
                    updateButtons();
                    return;
                }
            }
            rows.add(user);
            fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
        }

        UserDto get(int row) { return rows.get(row); }
    }
}
