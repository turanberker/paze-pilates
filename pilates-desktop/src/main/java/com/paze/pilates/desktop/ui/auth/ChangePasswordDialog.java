package com.paze.pilates.desktop.ui.auth;

import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.service.AuthService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Kendi şifresini değiştirme; geçici şifreyle girişte zorunlu modda iptal edilemez (çıkışa döner). */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ChangePasswordDialog extends JDialog {

    private static final Logger log = LoggerFactory.getLogger(ChangePasswordDialog.class);

    private final AuthService authService;
    private final MessageSource messages;

    private JPasswordField currentField;
    private JPasswordField newField;
    private JPasswordField confirmField;
    private JLabel errorLabel;
    private JButton saveBtn;

    private boolean changed;

    public ChangePasswordDialog(AuthService authService, MessageSource messages) {
        this.authService = authService;
        this.messages = messages;
    }

    /**
     * @param forced true ise geçici şifre değiştirilmeden devam edilemez; iptal "çıkış" anlamına gelir
     * @return şifre değiştiyse true
     */
    public boolean showDialog(Frame owner, boolean forced) {
        setTitle(text(forced ? "password.change.title.forced" : "password.change.title"));
        setModal(true);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 8, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridx = 0;
        int row = 0;

        if (forced) {
            JLabel info = new JLabel("<html><body style='width:260px'>" + text("password.change.forced_info"));
            c.gridy = row++;
            c.insets = new Insets(0, 0, 14, 0);
            form.add(info, c);
        }
        currentField = addPasswordRow(form, c, row++, "password.change.current");
        newField = addPasswordRow(form, c, row++, "password.change.new");
        confirmField = addPasswordRow(form, c, row++, "password.change.confirm");

        errorLabel = new JLabel(" ");
        errorLabel.setForeground(new Color(220, 38, 38));
        c.gridy = row * 2;
        c.insets = new Insets(0, 0, 0, 0);
        form.add(errorLabel, c);
        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        JButton cancelBtn = new JButton(text(forced ? "password.change.button.exit" : "password.change.button.cancel"));
        cancelBtn.addActionListener(e -> dispose());
        saveBtn = new JButton(text("password.change.button.save"));
        saveBtn.putClientProperty("JButton.buttonType", "default");
        saveBtn.addActionListener(e -> onSave());
        buttons.add(cancelBtn);
        buttons.add(saveBtn);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(saveBtn);

        pack();
        setMinimumSize(new Dimension(340, getHeight()));
        setLocationRelativeTo(owner);
        setVisible(true);
        return changed;
    }

    private JPasswordField addPasswordRow(JPanel form, GridBagConstraints c, int row, String labelKey) {
        c.gridy = row * 2 + 1;
        c.insets = new Insets(0, 0, 4, 0);
        form.add(new JLabel(text(labelKey)), c);
        JPasswordField field = new JPasswordField(20);
        c.gridy = row * 2 + 2;
        c.insets = new Insets(0, 0, 12, 0);
        form.add(field, c);
        return field;
    }

    private void onSave() {
        String current = new String(currentField.getPassword());
        String next = new String(newField.getPassword());
        String confirm = new String(confirmField.getPassword());
        saveBtn.setEnabled(false);
        errorLabel.setText(" ");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                authService.changeOwnPassword(current, next, confirm);
                return null;
            }
            @Override protected void done() {
                saveBtn.setEnabled(true);
                try {
                    get();
                    changed = true;
                    dispose();
                } catch (ExecutionException ex) {
                    if (ex.getCause() instanceof BusinessException be) {
                        errorLabel.setText(text(be));
                    } else {
                        log.error("Şifre değiştirilemedi", ex.getCause());
                        errorLabel.setText(text("error.unexpected"));
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private String text(BusinessException be) {
        return messages.getMessage(be.getMessage(), be.getArgs(), be.getMessage(), Locale.getDefault());
    }

    private String text(String key) {
        return messages.getMessage(key, null, key, Locale.getDefault());
    }
}
