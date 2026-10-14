package com.paze.pilates.desktop.ui.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class DashboardPanel extends JPanel {

    private final MessageSource messages;

    public DashboardPanel(MessageSource messages) {
        this.messages = messages;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildStatCards(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 28, 0));

        JLabel title = new JLabel(text("dashboard.welcome"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));

        JLabel subtitle = new JLabel(text("dashboard.subtitle"));
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 14f));
        subtitle.setForeground(new Color(100, 116, 139));

        JPanel textPanel = new JPanel(new BorderLayout(0, 6));
        textPanel.setOpaque(false);
        textPanel.add(title, BorderLayout.NORTH);
        textPanel.add(subtitle, BorderLayout.CENTER);

        panel.add(textPanel, BorderLayout.WEST);
        return panel;
    }

    private JPanel buildStatCards() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        JLabel sectionTitle = new JLabel(text("dashboard.today"));
        sectionTitle.setFont(sectionTitle.getFont().deriveFont(Font.BOLD, 14f));
        sectionTitle.setForeground(new Color(71, 85, 105));
        sectionTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        wrapper.add(sectionTitle, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 0));
        cards.setOpaque(false);
        cards.add(buildCard(text("dashboard.stat.students"),    "--", new Color(219, 234, 254), new Color(37, 99, 235)));
        cards.add(buildCard(text("dashboard.stat.reservations"),"--", new Color(220, 252, 231), new Color(22, 163, 74)));
        cards.add(buildCard(text("dashboard.stat.revenue"),     "--", new Color(254, 243, 199), new Color(202, 138, 4)));
        cards.add(buildCard(text("dashboard.stat.instructors"), "--", new Color(243, 232, 255), new Color(147, 51, 234)));

        JPanel cardsWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cardsWrapper.setOpaque(false);
        cardsWrapper.add(cards);

        wrapper.add(cards, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildCard(String title, String value, Color bg, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(bg);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accent, 1, true),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        card.setPreferredSize(new Dimension(160, 110));

        JLabel valueLabel = new JLabel(value, SwingConstants.LEFT);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 32f));
        valueLabel.setForeground(accent);

        JLabel titleLabel = new JLabel(title, SwingConstants.LEFT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, 12f));
        titleLabel.setForeground(new Color(71, 85, 105));

        card.add(valueLabel, BorderLayout.CENTER);
        card.add(titleLabel, BorderLayout.SOUTH);
        return card;
    }

    private String text(String key) {
        return messages.getMessage(key, null, Locale.getDefault());
    }
}
