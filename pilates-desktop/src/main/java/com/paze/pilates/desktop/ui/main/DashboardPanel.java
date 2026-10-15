package com.paze.pilates.desktop.ui.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import com.paze.pilates.desktop.ui.reservation.TodayLessonsPanel;
import com.paze.pilates.desktop.ui.shared.NotificationBar;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.DailySummaryDto;
import com.paze.pilates.dto.LessonDto;
import com.paze.pilates.dto.Permission;
import com.paze.pilates.service.AuthService;
import com.paze.pilates.service.DashboardService;
import com.paze.pilates.service.LessonService;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Component
public class DashboardPanel extends JPanel {

    private final MessageSource messages;
    private final AuthService authService;
    private final DashboardService dashboardService;
    private final LessonService lessonService;
    private final NotificationBar notificationBar;
    private final TodayLessonsPanel todayLessons;
    private final boolean admin;
    private final JLabel lessonsValue = valueLabel();
    private final JLabel lessonsHint = hintLabel();
    private final JLabel studentsValue = valueLabel();
    private final JLabel studentsHint = hintLabel();
    private final JLabel revenueValue = valueLabel();
    private final JLabel revenueHint = hintLabel();
    private final JLabel occupancyValue = valueLabel();
    private final JLabel occupancyHint = hintLabel();
    private final JLabel expiringValue = valueLabel();
    private final JLabel unpaidValue = valueLabel();
    private final JLabel unpaidHint = hintLabel();
    private final JLabel debtValue = valueLabel();
    private final JLabel pendingValue = valueLabel();
    private final NumberFormat moneyFmt = NumberFormat.getNumberInstance(Locale.getDefault());

    public DashboardPanel(MessageSource messages, AuthService authService, DashboardService dashboardService,
                          LessonService lessonService, NotificationBar notificationBar,
                          ObjectProvider<TodayLessonsPanel> todayLessonsProvider) {
        this.messages = messages;
        this.authService = authService;
        this.dashboardService = dashboardService;
        this.lessonService = lessonService;
        this.notificationBar = notificationBar;
        this.admin = authService.getCurrentUser().map(u -> u.has(Permission.STUDIO_MANAGE)).orElse(false);
        moneyFmt.setMinimumFractionDigits(2);
        moneyFmt.setMaximumFractionDigits(2);
        this.todayLessons = todayLessonsProvider.getObject();
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

        add(buildHeader(), BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout(0, 28));
        body.setOpaque(false);
        // Yönetici tüm stüdyonun özetini, hoca yalnızca kendi bugünkü ders ve katılımcı sayısını görür.
        body.add(admin ? buildStatCards() : buildInstructorCards(), BorderLayout.NORTH);
        body.add(todayLessons, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    /** Ana ekrana her gelişte güncel dersleri yükler. */
    public void reload() {
        todayLessons.reload();
        if (admin) {
            reloadSummary();
        } else {
            reloadMyDay();
        }
    }

    private void reloadMyDay() {
        new SwingWorker<List<LessonDto>, Void>() {
            @Override protected List<LessonDto> doInBackground() {
                return lessonService.findMyLessonsOn(LocalDate.now());
            }
            @Override protected void done() {
                try {
                    List<LessonDto> lessons = get();
                    lessonsValue.setText(String.valueOf(lessons.size()));
                    lessonsHint.setText(format("dashboard.hint.completed",
                            lessons.stream().filter(LessonDto::completed).count()));
                    studentsValue.setText(String.valueOf(lessons.stream().mapToInt(LessonDto::participantCount).sum()));
                    studentsHint.setText(text("dashboard.hint.my_participants"));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    notificationBar.showError(ex.getCause() instanceof BusinessException be
                            ? text(be.getMessage()) : text("error.unexpected"));
                }
            }
        }.execute();
    }

    private void reloadSummary() {
        new SwingWorker<DailySummaryDto, Void>() {
            @Override protected DailySummaryDto doInBackground() {
                return dashboardService.getDailySummary(LocalDate.now());
            }
            @Override protected void done() {
                try {
                    show(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    notificationBar.showError(ex.getCause() instanceof BusinessException be
                            ? text(be.getMessage()) : text("error.unexpected"));
                }
            }
        }.execute();
    }

    private void show(DailySummaryDto s) {
        lessonsValue.setText(String.valueOf(s.lessonCount()));
        lessonsHint.setText(format("dashboard.hint.completed", s.completedLessonCount()));
        studentsValue.setText(String.valueOf(s.studentCount()));
        studentsHint.setText(format("dashboard.hint.reservations", s.reservationCount(), s.attendedCount()));
        revenueValue.setText(moneyFmt.format(s.collectedAmount()));
        revenueHint.setText(format("dashboard.hint.packages", s.collectedPackageCount()));
        occupancyValue.setText("%" + s.occupancyPercent());
        occupancyHint.setText(text("dashboard.hint.occupancy"));
        expiringValue.setText(String.valueOf(s.expiringPackageCount()));
        unpaidValue.setText(String.valueOf(s.unpaidPackageCount()));
        unpaidHint.setText(moneyFmt.format(s.unpaidAmount()));
        debtValue.setText(String.valueOf(s.debtStudentCount()));
        pendingValue.setText(String.valueOf(s.pendingLessonCount()));
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

    private JPanel buildInstructorCards() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        JLabel sectionTitle = new JLabel(text("dashboard.today"));
        sectionTitle.setFont(sectionTitle.getFont().deriveFont(Font.BOLD, 14f));
        sectionTitle.setForeground(new Color(71, 85, 105));
        sectionTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        wrapper.add(sectionTitle, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 0));
        cards.setOpaque(false);
        cards.add(buildCard(text("dashboard.stat.my_lessons"), lessonsValue, lessonsHint, new Color(219, 234, 254), new Color(37, 99, 235)));
        cards.add(buildCard(text("dashboard.stat.my_participants"), studentsValue, studentsHint, new Color(220, 252, 231), new Color(22, 163, 74)));
        wrapper.add(cards, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildStatCards() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        JLabel sectionTitle = new JLabel(text("dashboard.today"));
        sectionTitle.setFont(sectionTitle.getFont().deriveFont(Font.BOLD, 14f));
        sectionTitle.setForeground(new Color(71, 85, 105));
        sectionTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        wrapper.add(sectionTitle, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(2, 4, 16, 16));
        cards.setOpaque(false);
        cards.add(buildCard(text("dashboard.stat.lessons"), lessonsValue, lessonsHint, new Color(219, 234, 254), new Color(37, 99, 235)));
        cards.add(buildCard(text("dashboard.stat.students"), studentsValue, studentsHint, new Color(220, 252, 231), new Color(22, 163, 74)));
        cards.add(buildCard(text("dashboard.stat.revenue"), revenueValue, revenueHint, new Color(254, 243, 199), new Color(202, 138, 4)));
        cards.add(buildCard(text("dashboard.stat.occupancy"), occupancyValue, occupancyHint, new Color(243, 232, 255), new Color(147, 51, 234)));
        cards.add(buildCard(text("dashboard.stat.expiring"), expiringValue, hintLabel(text("dashboard.hint.expiring")), new Color(255, 237, 213), new Color(234, 88, 12)));
        cards.add(buildCard(text("dashboard.stat.unpaid"), unpaidValue, unpaidHint, new Color(254, 226, 226), new Color(220, 38, 38)));
        cards.add(buildCard(text("dashboard.stat.debt"), debtValue, hintLabel(text("dashboard.hint.debt")), new Color(254, 226, 226), new Color(220, 38, 38)));
        cards.add(buildCard(text("dashboard.stat.pending"), pendingValue, hintLabel(text("dashboard.hint.pending")), new Color(241, 245, 249), new Color(71, 85, 105)));

        wrapper.add(cards, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildCard(String title, JLabel valueLabel, JLabel hint, Color bg, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(bg);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accent, 1, true),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));
        card.setPreferredSize(new Dimension(160, 110));

        valueLabel.setForeground(accent);

        JLabel titleLabel = new JLabel(title, SwingConstants.LEFT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, 12f));
        titleLabel.setForeground(new Color(71, 85, 105));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(titleLabel, BorderLayout.NORTH);
        bottom.add(hint, BorderLayout.SOUTH);

        card.add(valueLabel, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private static JLabel valueLabel() {
        JLabel label = new JLabel("--", SwingConstants.LEFT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 30f));
        return label;
    }

    private static JLabel hintLabel() {
        return hintLabel(" ");
    }

    private static JLabel hintLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.LEFT);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
        label.setForeground(new Color(100, 116, 139));
        return label;
    }

    private String format(String key, Object... args) {
        return messages.getMessage(key, args, Locale.getDefault());
    }

    private String text(String key) {
        return messages.getMessage(key, null, Locale.getDefault());
    }
}
