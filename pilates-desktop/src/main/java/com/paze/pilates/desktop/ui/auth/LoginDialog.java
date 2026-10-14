package com.paze.pilates.desktop.ui.auth;

import com.paze.pilates.service.AuthService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class LoginDialog extends JDialog {

    private final AuthService authService;
    private final MessageSource messages;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel errorLabel;

    private boolean loginSucceeded = false;

    public LoginDialog(AuthService authService, MessageSource messages) {
        super((Frame) null, "", true);
        this.authService = authService;
        this.messages = messages;
        initComponents();
    }

    private void initComponents() {
        setTitle(text("app.title"));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(248, 250, 252));

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildForm(),   BorderLayout.CENTER);

        setContentPane(root);
        pack();
        setMinimumSize(new Dimension(380, getHeight()));
        setLocationRelativeTo(null);
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(new Color(30, 41, 59));
        panel.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        JLabel appName = new JLabel(text("app.title"), SwingConstants.CENTER);
        appName.setFont(appName.getFont().deriveFont(Font.BOLD, 18f));
        appName.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel(text("login.subtitle"), SwingConstants.CENTER);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 12f));
        subtitle.setForeground(new Color(148, 163, 184));

        panel.add(appName,  BorderLayout.CENTER);
        panel.add(subtitle, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(0, 0, 6, 0);
        c.gridx = 0;

        c.gridy = 0;
        panel.add(fieldLabel(text("login.username")), c);

        usernameField = new JTextField();
        usernameField.setPreferredSize(new Dimension(300, 34));
        c.gridy = 1;
        c.insets = new Insets(0, 0, 16, 0);
        panel.add(usernameField, c);

        c.insets = new Insets(0, 0, 6, 0);
        c.gridy = 2;
        panel.add(fieldLabel(text("login.password")), c);

        passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(300, 34));
        c.gridy = 3;
        c.insets = new Insets(0, 0, 20, 0);
        panel.add(passwordField, c);

        loginButton = new JButton(text("login.button"));
        loginButton.setPreferredSize(new Dimension(300, 38));
        loginButton.setBackground(new Color(37, 99, 235));
        loginButton.setForeground(Color.WHITE);
        loginButton.setOpaque(true);
        loginButton.setFocusPainted(false);
        loginButton.setFont(loginButton.getFont().deriveFont(Font.BOLD, 13f));
        c.gridy = 4;
        c.insets = new Insets(0, 0, 10, 0);
        panel.add(loginButton, c);

        errorLabel = new JLabel(" ");
        errorLabel.setForeground(new Color(220, 38, 38));
        errorLabel.setFont(errorLabel.getFont().deriveFont(Font.PLAIN, 12f));
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridy = 5;
        c.insets = new Insets(0, 0, 0, 0);
        panel.add(errorLabel, c);

        loginButton.addActionListener(this::onLogin);
        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) onLogin(null);
            }
        });
        usernameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) passwordField.requestFocus();
            }
        });

        return panel;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 12f));
        label.setForeground(new Color(71, 85, 105));
        return label;
    }

    private void onLogin(ActionEvent ignored) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText(text("login.error.empty"));
            return;
        }

        loginButton.setEnabled(false);
        errorLabel.setText(" ");

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return authService.authenticate(username, password);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        loginSucceeded = true;
                        dispose();
                    } else {
                        errorLabel.setText(text("login.error"));
                        passwordField.setText("");
                        passwordField.requestFocus();
                    }
                } catch (Exception ex) {
                    errorLabel.setText(text("login.error"));
                } finally {
                    loginButton.setEnabled(true);
                }
            }
        }.execute();
    }

    /** Modal dialog'u gösterir; giriş başarılıysa {@code true} döner. */
    public boolean showAndWait() {
        setVisible(true);
        return loginSucceeded;
    }

    private String text(String key) {
        return messages.getMessage(key, null, Locale.getDefault());
    }
}
