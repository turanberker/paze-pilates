package com.paze.pilates.desktop.ui.auth;

import com.paze.pilates.desktop.ui.shared.LogoPicker;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.SetupRequestDto;
import com.paze.pilates.service.AuthService;
import com.paze.pilates.service.SetupService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Veritabanında hiç kullanıcı yokken giriş yerine açılan ilk kurulum ekranı. */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SetupDialog extends JDialog {

    public enum Result { COMPLETED, SYSTEM_LOGIN, CANCELLED }

    private static final Logger log = LoggerFactory.getLogger(SetupDialog.class);

    private final SetupService setupService;
    private final AuthService authService;
    private final MessageSource messages;

    private JTextField studioNameField;
    private JTextField fullNameField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmField;
    private JLabel errorLabel;
    private JButton saveBtn;
    private LogoPicker logoPicker;

    private Result result = Result.CANCELLED;

    public SetupDialog(SetupService setupService, AuthService authService, MessageSource messages) {
        super((java.awt.Frame) null, "", true);
        this.setupService = setupService;
        this.authService = authService;
        this.messages = messages;
        initComponents();
    }

    private void initComponents() {
        setTitle(text("setup.title"));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 28, 8, 28));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridx = 0;

        JLabel info = new JLabel("<html><body style='width:300px'>" + text("setup.info"));
        c.gridy = 0;
        c.insets = new Insets(0, 0, 16, 0);
        form.add(info, c);

        errorLabel = new JLabel(" ");
        errorLabel.setForeground(new Color(220, 38, 38));
        studioNameField = new JTextField(24);
        fullNameField = new JTextField(24);
        usernameField = new JTextField(24);
        passwordField = new JPasswordField(24);
        confirmField = new JPasswordField(24);
        addRow(form, c, 1, "setup.label.studio_name", studioNameField);
        logoPicker = new LogoPicker(messages, errorLabel::setText);
        addRow(form, c, 2, "setup.label.logo", logoPicker);
        addRow(form, c, 3, "setup.label.full_name", fullNameField);
        addRow(form, c, 4, "setup.label.username", usernameField);
        addRow(form, c, 5, "setup.label.password", passwordField);
        addRow(form, c, 6, "setup.label.password_confirm", confirmField);

        c.gridy = 14;
        c.insets = new Insets(0, 0, 0, 0);
        form.add(errorLabel, c);
        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        if (setupService.isStaticAdminAvailable()) {
            JButton systemLoginBtn = new JButton(text("setup.button.system_login"));
            systemLoginBtn.addActionListener(e -> {
                result = Result.SYSTEM_LOGIN;
                dispose();
            });
            buttons.add(systemLoginBtn);
        }
        saveBtn = new JButton(text("setup.button.save"));
        saveBtn.putClientProperty("JButton.buttonType", "default");
        saveBtn.addActionListener(e -> onSave());
        buttons.add(saveBtn);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(saveBtn);

        pack();
        setMinimumSize(new Dimension(400, getHeight()));
        setLocationRelativeTo(null);
    }

    private void addRow(JPanel form, GridBagConstraints c, int row, String labelKey, JComponent field) {
        c.gridy = row * 2;
        c.insets = new Insets(0, 0, 4, 0);
        form.add(new JLabel(text(labelKey)), c);
        c.gridy = row * 2 + 1;
        c.insets = new Insets(0, 0, 12, 0);
        form.add(field, c);
    }

    private void onSave() {
        SetupRequestDto request = new SetupRequestDto(
                studioNameField.getText(), fullNameField.getText(), usernameField.getText(),
                new String(passwordField.getPassword()), new String(confirmField.getPassword()), logoPicker.getLogo());
        saveBtn.setEnabled(false);
        errorLabel.setText(" ");
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                setupService.completeSetup(request);
                return authService.authenticate(request.username().trim(), request.password());
            }
            @Override protected void done() {
                saveBtn.setEnabled(true);
                try {
                    if (get()) {
                        result = Result.COMPLETED;
                        dispose();
                    } else {
                        errorLabel.setText(text("login.error"));
                    }
                } catch (ExecutionException ex) {
                    if (ex.getCause() instanceof BusinessException be) {
                        errorLabel.setText(text(be));
                    } else {
                        log.error("Kurulum tamamlanamadı", ex.getCause());
                        errorLabel.setText(text("error.unexpected"));
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /** Modal açar; kurulum tamamlanıp giriş yapıldıysa COMPLETED, sistem girişi seçildiyse SYSTEM_LOGIN döner. */
    public Result showAndWait() {
        setVisible(true);
        return result;
    }

    private String text(BusinessException be) {
        return messages.getMessage(be.getMessage(), be.getArgs(), be.getMessage(), Locale.getDefault());
    }

    private String text(String key) {
        return messages.getMessage(key, null, key, Locale.getDefault());
    }
}
