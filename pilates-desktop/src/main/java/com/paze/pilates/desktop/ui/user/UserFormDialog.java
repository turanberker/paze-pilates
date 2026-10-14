package com.paze.pilates.desktop.ui.user;

import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.InstructorDto;
import com.paze.pilates.dto.UserDto;
import com.paze.pilates.dto.UserRole;
import com.paze.pilates.service.InstructorService;
import com.paze.pilates.service.UserService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;

/** Kullanıcı formu: yeni kullanıcı, rol değiştirme ve geçici şifre verme aynı alanları farklı modda kullanır. */
@org.springframework.stereotype.Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UserFormDialog extends JDialog {

    public enum Mode { CREATE, CHANGE_ROLE, RESET_PASSWORD }

    private static final Logger log = LoggerFactory.getLogger(UserFormDialog.class);

    private final UserService userService;
    private final InstructorService instructorService;
    private final MessageSource messages;

    private JTextField usernameField;
    private JComboBox<UserRole> roleCombo;
    private JComboBox<InstructorDto> instructorCombo;
    private JTextField fullNameField;
    private JTextField passwordField;
    private JLabel errorLabel;
    private JButton saveBtn;

    private JComponent[] instructorRow;
    private JComponent[] fullNameRow;

    private Mode mode;
    private UserDto editing;
    private UserDto saved;

    public UserFormDialog(UserService userService, InstructorService instructorService, MessageSource messages) {
        this.userService = userService;
        this.instructorService = instructorService;
        this.messages = messages;
    }

    /** Formu açar; kaydedilen kullanıcıyı döner, iptal edilirse null. */
    public UserDto showDialog(Frame owner, Mode mode, UserDto user) {
        this.mode = mode;
        this.editing = user;
        setTitle(text("user.form.title." + mode.name().toLowerCase(Locale.ROOT)));
        setModal(true);
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 8, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridx = 0;
        int row = 0;

        if (mode != Mode.CREATE) {
            JLabel who = new JLabel(user.fullName() + " (" + user.username() + ")");
            c.gridy = row++;
            c.insets = new Insets(0, 0, 14, 0);
            form.add(who, c);
        }
        if (mode == Mode.CREATE) {
            usernameField = new JTextField(22);
            addRow(form, c, row, "user.form.label.username", usernameField);
            row += 2;
        }
        if (mode != Mode.RESET_PASSWORD) {
            roleCombo = new JComboBox<>(UserRole.values());
            roleCombo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                              boolean selected, boolean focus) {
                    super.getListCellRendererComponent(list, value, index, selected, focus);
                    setText(value == null ? "" : text("role." + ((UserRole) value).name()));
                    return this;
                }
            });
            roleCombo.addActionListener(e -> updateRoleDependentRows());
            addRow(form, c, row, "user.form.label.role", roleCombo);
            row += 2;

            instructorCombo = new JComboBox<>();
            instructorCombo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                              boolean selected, boolean focus) {
                    super.getListCellRendererComponent(list, value, index, selected, focus);
                    InstructorDto i = (InstructorDto) value;
                    setText(i == null ? "" : i.firstName() + " " + i.lastName());
                    return this;
                }
            });
            instructorRow = addRow(form, c, row, "user.form.label.instructor", instructorCombo);
            row += 2;
        }
        if (mode == Mode.CREATE) {
            fullNameField = new JTextField(22);
            fullNameRow = addRow(form, c, row, "user.form.label.full_name", fullNameField);
            row += 2;
        }
        if (mode != Mode.CHANGE_ROLE) {
            passwordField = new JTextField(22);
            addRow(form, c, row, "user.form.label.temporary_password", passwordField);
            row += 2;
            JLabel hint = new JLabel("<html><body style='width:260px'>" + text("user.form.temporary_password_hint"));
            c.gridy = row++;
            c.insets = new Insets(0, 0, 8, 0);
            form.add(hint, c);
        }

        errorLabel = new JLabel(" ");
        errorLabel.setForeground(new Color(220, 38, 38));
        c.gridy = row;
        c.insets = new Insets(0, 0, 0, 0);
        form.add(errorLabel, c);
        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        JButton cancelBtn = new JButton(text("user.form.button.cancel"));
        cancelBtn.addActionListener(e -> dispose());
        saveBtn = new JButton(text("user.form.button.save"));
        saveBtn.putClientProperty("JButton.buttonType", "default");
        saveBtn.addActionListener(e -> onSave());
        buttons.add(cancelBtn);
        buttons.add(saveBtn);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(saveBtn);

        if (roleCombo != null) {
            roleCombo.setSelectedItem(mode == Mode.CHANGE_ROLE ? user.role() : UserRole.INSTRUCTOR);
            loadInstructors();
            updateRoleDependentRows();
        }
        pack();
        setMinimumSize(new Dimension(380, getHeight()));
        setLocationRelativeTo(owner);
        setVisible(true);
        return saved;
    }

    /** Etiket + alan ekler; satırı gizleyip göstermek için bileşenleri döner. */
    private JComponent[] addRow(JPanel form, GridBagConstraints c, int row, String labelKey, JComponent field) {
        JLabel label = new JLabel(text(labelKey));
        c.gridy = row;
        c.insets = new Insets(0, 0, 4, 0);
        form.add(label, c);
        c.gridy = row + 1;
        c.insets = new Insets(0, 0, 12, 0);
        form.add(field, c);
        return new JComponent[] {label, field};
    }

    private void updateRoleDependentRows() {
        boolean isInstructor = roleCombo.getSelectedItem() == UserRole.INSTRUCTOR;
        setVisible(instructorRow, isInstructor);
        if (fullNameRow != null) {
            setVisible(fullNameRow, !isInstructor);
        }
        pack();
    }

    private static void setVisible(JComponent[] row, boolean visible) {
        for (JComponent component : row) {
            component.setVisible(visible);
        }
    }

    private void loadInstructors() {
        new SwingWorker<List<InstructorDto>, Void>() {
            @Override protected List<InstructorDto> doInBackground() {
                return instructorService.getInstructors();
            }
            @Override protected void done() {
                try {
                    DefaultComboBoxModel<InstructorDto> model = new DefaultComboBoxModel<>();
                    model.addAll(get());
                    instructorCombo.setModel(model);
                    if (editing != null && editing.instructorId() != null) {
                        get().stream().filter(i -> i.id().equals(editing.instructorId())).findFirst()
                                .ifPresent(model::setSelectedItem);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    log.error("Hocalar yüklenemedi", ex);
                    errorLabel.setText(text("error.unexpected"));
                }
            }
        }.execute();
    }

    private void onSave() {
        UserRole role = roleCombo == null ? null : (UserRole) roleCombo.getSelectedItem();
        InstructorDto instructor = instructorCombo == null ? null : (InstructorDto) instructorCombo.getSelectedItem();
        Long instructorId = (role == UserRole.INSTRUCTOR && instructor != null) ? instructor.id() : null;
        String username = usernameField == null ? null : usernameField.getText();
        String fullName = fullNameField == null ? null : fullNameField.getText();
        String password = passwordField == null ? null : passwordField.getText();

        saveBtn.setEnabled(false);
        errorLabel.setText(" ");
        new SwingWorker<UserDto, Void>() {
            @Override protected UserDto doInBackground() {
                return switch (mode) {
                    case CREATE -> userService.createUser(username, fullName, role, instructorId, password);
                    case CHANGE_ROLE -> userService.changeRole(editing.id(), role, instructorId);
                    case RESET_PASSWORD -> userService.resetPassword(editing.id(), password);
                };
            }
            @Override protected void done() {
                saveBtn.setEnabled(true);
                try {
                    saved = get();
                    dispose();
                } catch (ExecutionException ex) {
                    if (ex.getCause() instanceof BusinessException be) {
                        errorLabel.setText(text(be.getMessage()));
                    } else {
                        log.error("Kullanıcı kaydedilemedi", ex.getCause());
                        errorLabel.setText(text("error.unexpected"));
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private String text(String key) {
        return messages.getMessage(key, null, key, Locale.getDefault());
    }
}
