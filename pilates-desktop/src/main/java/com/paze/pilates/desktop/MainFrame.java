package com.paze.pilates.desktop;

import com.paze.pilates.desktop.ui.finance.PackageListPanel;
import com.paze.pilates.desktop.ui.finance.PaymentPanel;
import com.paze.pilates.desktop.ui.instructor.InstructorListPanel;
import com.paze.pilates.desktop.ui.main.DashboardPanel;
import com.paze.pilates.desktop.ui.main.MainMenuBar;
import com.paze.pilates.desktop.ui.report.ReportPanel;
import com.paze.pilates.desktop.ui.reservation.CalendarPanel;
import com.paze.pilates.desktop.ui.shared.NotificationBar;
import com.paze.pilates.desktop.ui.student.StudentListPanel;
import com.paze.pilates.desktop.ui.student.StudentPackageListPanel;
import com.paze.pilates.desktop.ui.studio.StudioPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class MainFrame extends JFrame {

    private final ConfigurableApplicationContext context;
    private final MessageSource messages;
    private final MainMenuBar menuBar;
    private final NotificationBar notificationBar;

    private final DashboardPanel dashboardPanel;
    private final StudioPanel studioPanel;
    private final InstructorListPanel instructorPanel;
    private final StudentListPanel studentPanel;
    private final StudentPackageListPanel studentPackagePanel;
    private final CalendarPanel calendarPanel;
    private final PackageListPanel packagePanel;
    private final PaymentPanel paymentPanel;
    private final ReportPanel reportPanel;

    private final CardLayout cardLayout = new CardLayout();
    private JPanel contentCards;

    public MainFrame(
            ConfigurableApplicationContext context,
            MessageSource messages,
            MainMenuBar menuBar,
            NotificationBar notificationBar,
            DashboardPanel dashboardPanel,
            StudioPanel studioPanel,
            InstructorListPanel instructorPanel,
            StudentListPanel studentPanel,
            StudentPackageListPanel studentPackagePanel,
            CalendarPanel calendarPanel,
            PackageListPanel packagePanel,
            PaymentPanel paymentPanel,
            ReportPanel reportPanel) {

        this.context = context;
        this.messages = messages;
        this.menuBar = menuBar;
        this.notificationBar = notificationBar;
        this.dashboardPanel = dashboardPanel;
        this.studioPanel = studioPanel;
        this.instructorPanel = instructorPanel;
        this.studentPanel = studentPanel;
        this.studentPackagePanel = studentPackagePanel;
        this.calendarPanel = calendarPanel;
        this.packagePanel = packagePanel;
        this.paymentPanel = paymentPanel;
        this.reportPanel = reportPanel;

        initComponents();
    }

    private void initComponents() {
        setTitle(text("app.title"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1024, 700));
        setLayout(new BorderLayout());

        contentCards = new JPanel(cardLayout);
        contentCards.add(dashboardPanel, "dashboard");
        contentCards.add(studioPanel,    "studio");
        contentCards.add(instructorPanel,"instructor");
        contentCards.add(studentPanel,   "student");
        contentCards.add(studentPackagePanel, "student_package");
        contentCards.add(calendarPanel,  "calendar");
        contentCards.add(packagePanel,   "package");
        contentCards.add(paymentPanel,   "finance");
        contentCards.add(reportPanel,    "report");

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(notificationBar);
        content.add(contentCards);

        menuBar.setNavCallback(this::showPanel);
        menuBar.setExitCallback(this::dispose);
        setJMenuBar(menuBar);

        add(content, BorderLayout.CENTER);

        showPanel("dashboard");

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                context.close();
                System.exit(0);
            }
        });
    }

    public void showPanel(String name) {
        if ("student_package".equals(name)) {
            studentPackagePanel.reload();
        }
        cardLayout.show(contentCards, name);
    }

    public void showSuccess(String message) {
        notificationBar.showSuccess(message);
    }

    public void showError(String message) {
        notificationBar.showError(message);
    }

    public void showWindow() {
        pack();
        setSize(1280, 800);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private String text(String key) {
        return messages.getMessage(key, null, Locale.getDefault());
    }
}
