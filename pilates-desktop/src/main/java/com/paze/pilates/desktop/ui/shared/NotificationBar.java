package com.paze.pilates.desktop.ui.shared;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import org.springframework.stereotype.Component;

@Component
public class NotificationBar extends JPanel {

    private final JLabel messageLabel;
    private Timer hideTimer;

    public NotificationBar() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 8));
        setVisible(false);

        messageLabel = new JLabel("", SwingConstants.LEFT);
        messageLabel.setFont(messageLabel.getFont().deriveFont(Font.PLAIN, 13f));
        add(messageLabel, BorderLayout.CENTER);

        JButton closeBtn = new JButton("✕");
        closeBtn.putClientProperty("JButton.buttonType", "toolBarButton");
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> setVisible(false));
        add(closeBtn, BorderLayout.EAST);
    }

    public void showSuccess(String message) {
        show(message, new Color(220, 252, 231), new Color(21, 128, 61));
    }

    public void showError(String message) {
        show(message, new Color(254, 226, 226), new Color(153, 27, 27));
    }

    private void show(String message, Color bg, Color fg) {
        messageLabel.setText(message);
        setBackground(bg);
        messageLabel.setForeground(fg);
        setVisible(true);
        revalidate();

        if (hideTimer != null) {
            hideTimer.stop();
        }
        hideTimer = new Timer(4000, e -> setVisible(false));
        hideTimer.setRepeats(false);
        hideTimer.start();
    }
}
