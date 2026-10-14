package com.paze.pilates.desktop;

import com.paze.pilates.desktop.ui.auth.ChangePasswordDialog;
import com.paze.pilates.desktop.ui.finance.PackageListPanel;
import com.paze.pilates.desktop.ui.finance.PaymentPanel;
import com.paze.pilates.desktop.ui.instructor.InstructorListPanel;
import com.paze.pilates.desktop.ui.main.DashboardPanel;
import com.paze.pilates.desktop.ui.main.MainMenuBar;
import com.paze.pilates.desktop.ui.main.NavTarget;
import com.paze.pilates.desktop.ui.report.ReportPanel;
import com.paze.pilates.desktop.ui.reservation.CalendarPanel;
import com.paze.pilates.desktop.ui.settings.StudioSettingsPanel;
import com.paze.pilates.desktop.ui.shared.LogoImages;
import com.paze.pilates.desktop.ui.shared.NotificationBar;
import com.paze.pilates.desktop.ui.student.StudentListPanel;
import com.paze.pilates.desktop.ui.student.StudentPackageListPanel;
import com.paze.pilates.desktop.ui.studio.StudioPanel;
import com.paze.pilates.desktop.ui.user.UserListPanel;
import com.paze.pilates.dto.AppUserDto;
import com.paze.pilates.service.AuthService;
import com.paze.pilates.service.StudioService;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Component
public class MainFrame extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(MainFrame.class);
    private static final int HEADER_LOGO_HEIGHT = 24;
    private static final int HEADER_LOGO_MAX_WIDTH = 120;

    private final ConfigurableApplicationContext context;
    private final MessageSource messages;
    private final AuthService authService;
    private final StudioService studioService;
    private final MainMenuBar menuBar;
    private final NotificationBar notificationBar;
    private final ObjectProvider<ChangePasswordDialog> changePasswordDialogProvider;

    private final ObjectProvider<DashboardPanel> dashboardPanel;
    private final ObjectProvider<StudioPanel> studioPanel;
    private final ObjectProvider<InstructorListPanel> instructorPanel;
    private final ObjectProvider<UserListPanel> userPanel;
    private final ObjectProvider<StudentListPanel> studentPanel;
    private final ObjectProvider<StudentPackageListPanel> studentPackagePanel;
    private final ObjectProvider<CalendarPanel> calendarPanel;
    private final ObjectProvider<PackageListPanel> packagePanel;
    private final ObjectProvider<PaymentPanel> paymentPanel;
    private final ObjectProvider<ReportPanel> reportPanel;
    private final ObjectProvider<StudioSettingsPanel> studioSettingsPanel;

    private final CardLayout cardLayout = new CardLayout();
    private JPanel contentCards;
    private StudentPackageListPanel loadedStudentPackagePanel;
    private DashboardPanel loadedDashboardPanel;
    private StudioSettingsPanel loadedStudioSettingsPanel;
    private Runnable logoutCallback;
    private boolean loggingOut;

    /**
     * Paneller girişten sonra, yalnızca kullanıcının yetkisi olanlar oluşturulur
     * (panel kurucuları servis çağırdığı için yetkisiz panel hata üretirdi).
     */
    public MainFrame(
            ConfigurableApplicationContext context,
            MessageSource messages,
            AuthService authService,
            StudioService studioService,
            MainMenuBar menuBar,
            NotificationBar notificationBar,
            ObjectProvider<ChangePasswordDialog> changePasswordDialogProvider,
            ObjectProvider<DashboardPanel> dashboardPanel,
            ObjectProvider<StudioPanel> studioPanel,
            ObjectProvider<InstructorListPanel> instructorPanel,
            ObjectProvider<UserListPanel> userPanel,
            ObjectProvider<StudentListPanel> studentPanel,
            ObjectProvider<StudentPackageListPanel> studentPackagePanel,
            ObjectProvider<CalendarPanel> calendarPanel,
            ObjectProvider<PackageListPanel> packagePanel,
            ObjectProvider<PaymentPanel> paymentPanel,
            ObjectProvider<ReportPanel> reportPanel,
            ObjectProvider<StudioSettingsPanel> studioSettingsPanel) {

        this.context = context;
        this.messages = messages;
        this.authService = authService;
        this.studioService = studioService;
        this.menuBar = menuBar;
        this.notificationBar = notificationBar;
        this.changePasswordDialogProvider = changePasswordDialogProvider;
        this.dashboardPanel = dashboardPanel;
        this.studioPanel = studioPanel;
        this.instructorPanel = instructorPanel;
        this.userPanel = userPanel;
        this.studentPanel = studentPanel;
        this.studentPackagePanel = studentPackagePanel;
        this.calendarPanel = calendarPanel;
        this.packagePanel = packagePanel;
        this.paymentPanel = paymentPanel;
        this.reportPanel = reportPanel;
        this.studioSettingsPanel = studioSettingsPanel;

        initComponents();
    }

    private void initComponents() {
        AppUserDto user = authService.getCurrentUser().orElseThrow();

        setTitle(text("app.title"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1024, 700));
        setLayout(new BorderLayout());

        contentCards = new JPanel(cardLayout);
        if (NavTarget.DASHBOARD.isAllowedFor(user)) {
            loadedDashboardPanel = dashboardPanel.getObject();
            contentCards.add(loadedDashboardPanel, NavTarget.DASHBOARD.key());
        }
        addCard(user, NavTarget.STUDIO, studioPanel);
        addCard(user, NavTarget.INSTRUCTOR, instructorPanel);
        addCard(user, NavTarget.USERS, userPanel);
        addCard(user, NavTarget.STUDENT, studentPanel);
        if (NavTarget.STUDENT_PACKAGE.isAllowedFor(user)) {
            loadedStudentPackagePanel = studentPackagePanel.getObject();
            contentCards.add(loadedStudentPackagePanel, NavTarget.STUDENT_PACKAGE.key());
        }
        addCard(user, NavTarget.CALENDAR, calendarPanel);
        addCard(user, NavTarget.PACKAGE, packagePanel);
        addCard(user, NavTarget.FINANCE, paymentPanel);
        addCard(user, NavTarget.REPORT, reportPanel);
        if (NavTarget.STUDIO_SETTINGS.isAllowedFor(user)) {
            loadedStudioSettingsPanel = studioSettingsPanel.getObject();
            loadedStudioSettingsPanel.setOnSaved(this::loadLogo);
            contentCards.add(loadedStudioSettingsPanel, NavTarget.STUDIO_SETTINGS.key());
        }

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(notificationBar);
        content.add(contentCards);

        menuBar.setNavCallback(this::showPanel);
        menuBar.setExitCallback(this::logout);
        menuBar.setChangePasswordCallback(this::onChangePassword);
        menuBar.build(user);
        setJMenuBar(menuBar);

        add(content, BorderLayout.CENTER);
        loadLogo();

        showPanel(NavTarget.DASHBOARD.isAllowedFor(user) ? NavTarget.DASHBOARD.key() : NavTarget.CALENDAR.key());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (loggingOut) {
                    return;
                }
                context.close();
                System.exit(0);
            }
        });
    }

    /** Stüdyo logosunu arka planda yükler; logo yoksa header ve pencere ikonu varsayılan görünümde kalır. */
    private void loadLogo() {
        new SwingWorker<BufferedImage, Void>() {
            @Override protected BufferedImage doInBackground() {
                return studioService.getLogo().map(LogoImages::decode).orElse(null);
            }
            @Override protected void done() {
                try {
                    BufferedImage logo = get();
                    menuBar.setLogo(logo == null ? null
                            : LogoImages.fit(logo, HEADER_LOGO_MAX_WIDTH, HEADER_LOGO_HEIGHT));
                    setIconImage(logo);
                } catch (ExecutionException ex) {
                    log.warn("Stüdyo logosu yüklenemedi", ex.getCause());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /** Oturumu kapatır, pencereyi kapatır ve (verilmişse) giriş ekranına dönülmesini sağlar. */
    private void logout() {
        loggingOut = true;
        authService.logout();
        dispose();
        if (logoutCallback != null) {
            logoutCallback.run();
        }
    }

    /** Çıkış yapıldığında çalışır; uygulama kapanmaz, çağıran giriş ekranını yeniden gösterir. */
    public void setLogoutCallback(Runnable callback) {
        this.logoutCallback = callback;
    }

    private void addCard(AppUserDto user, NavTarget target, ObjectProvider<? extends JPanel> provider) {
        if (target.isAllowedFor(user)) {
            contentCards.add(provider.getObject(), target.key());
        }
    }

    private void onChangePassword() {
        if (changePasswordDialogProvider.getObject().showDialog(this, false)) {
            notificationBar.showSuccess(text("password.change.success"));
        }
    }

    public void showPanel(String name) {
        if (NavTarget.DASHBOARD.key().equals(name) && loadedDashboardPanel != null) {
            loadedDashboardPanel.reload();
        }
        if (NavTarget.STUDENT_PACKAGE.key().equals(name) && loadedStudentPackagePanel != null) {
            loadedStudentPackagePanel.reload();
        }
        if (NavTarget.STUDIO_SETTINGS.key().equals(name) && loadedStudioSettingsPanel != null) {
            loadedStudioSettingsPanel.reload();
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
