package com.paze.pilates.desktop.ui.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class MainMenuPanel extends JPanel {

    private static final Color SIDEBAR_BG   = new Color(30, 41, 59);
    private static final Color HOVER_BG     = new Color(51, 65, 85);
    private static final Color ACTIVE_BG    = new Color(37, 99, 235);
    private static final Color SIDEBAR_TEXT = new Color(203, 213, 225);
    private static final Color ACTIVE_TEXT  = Color.WHITE;
    private static final Color DIM_TEXT     = new Color(148, 163, 184);

    private final MessageSource messages;
    private final Map<String, JLabel> navItems = new LinkedHashMap<>();
    private Consumer<String> navCallback;
    private String activeKey = "dashboard";

    public MainMenuPanel(MessageSource messages) {
        this.messages = messages;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(210, 0));
        setBackground(SIDEBAR_BG);

        add(buildLogoPanel(), BorderLayout.NORTH);
        add(buildNavPanel(),  BorderLayout.CENTER);
        add(buildFooter(),    BorderLayout.SOUTH);
    }

    private JPanel buildLogoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(15, 23, 42));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 16, 18, 16));

        JLabel logo = new JLabel(text("app.title"), SwingConstants.CENTER);
        logo.setFont(logo.getFont().deriveFont(Font.BOLD, 13f));
        logo.setForeground(ACTIVE_TEXT);
        panel.add(logo, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildNavPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(SIDEBAR_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

        addNavItem(panel, "dashboard",  text("nav.dashboard"));
        addNavItem(panel, "studio",     text("nav.studio"));
        addNavItem(panel, "instructor", text("nav.instructor"));
        addNavItem(panel, "student",    text("nav.student"));
        addNavItem(panel, "calendar",   text("nav.calendar"));
        addNavItem(panel, "finance",    text("nav.finance"));
        addNavItem(panel, "report",     text("nav.report"));
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private void addNavItem(JPanel container, String key, String label) {
        JLabel item = new JLabel("  " + label);
        item.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        item.setPreferredSize(new Dimension(210, 44));
        item.setFont(item.getFont().deriveFont(13f));
        item.setForeground(SIDEBAR_TEXT);
        item.setOpaque(true);
        item.setBackground(SIDEBAR_BG);
        item.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (navCallback != null) {
                    navCallback.accept(key);
                }
                setActive(key);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!key.equals(activeKey)) {
                    item.setBackground(HOVER_BG);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!key.equals(activeKey)) {
                    item.setBackground(SIDEBAR_BG);
                }
            }
        });

        navItems.put(key, item);
        container.add(item);
    }

    private JLabel buildFooter() {
        JLabel version = new JLabel(text("app.version"), SwingConstants.CENTER);
        version.setFont(version.getFont().deriveFont(10f));
        version.setForeground(DIM_TEXT);
        version.setBorder(BorderFactory.createEmptyBorder(8, 8, 14, 8));
        version.setOpaque(true);
        version.setBackground(SIDEBAR_BG);
        return version;
    }

    public void setActive(String key) {
        this.activeKey = key;
        navItems.forEach((k, item) -> {
            boolean selected = k.equals(key);
            item.setBackground(selected ? ACTIVE_BG : SIDEBAR_BG);
            item.setForeground(selected ? ACTIVE_TEXT : SIDEBAR_TEXT);
        });
    }

    public void setNavCallback(Consumer<String> callback) {
        this.navCallback = callback;
    }

    private String text(String key) {
        return messages.getMessage(key, null, Locale.getDefault());
    }
}
