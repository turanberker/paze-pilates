package com.paze.pilates;

import com.formdev.flatlaf.FlatLightLaf;
import com.paze.pilates.desktop.MainFrame;
import com.paze.pilates.desktop.ui.auth.ChangePasswordDialog;
import com.paze.pilates.desktop.ui.auth.LoginDialog;
import com.paze.pilates.desktop.ui.auth.SetupDialog;
import com.paze.pilates.dto.AppUserDto;
import com.paze.pilates.service.AuthService;
import com.paze.pilates.service.SetupService;
import javax.swing.SwingUtilities;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class PazePilatesApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(PazePilatesApplication.class)
                .headless(false)
                .run(args);

        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            startSession(context);
        });
    }

    /** Giriş (veya ilk kurulum) → ana pencere. Çıkış yapılınca aynı akış yeniden başlar, uygulama kapanmaz. */
    private static void startSession(ConfigurableApplicationContext context) {
        boolean setupRequired = context.getBean(SetupService.class).isSetupRequired();
        if (authenticate(context, setupRequired)) {
            MainFrame frame = context.getBean(MainFrame.class);
            frame.setLogoutCallback(() -> startSession(context));
            frame.showWindow();
        } else {
            context.close();
            System.exit(0);
        }
    }

    /**
     * Kullanıcı yoksa kurulum ekranı, varsa giriş ekranı. Kurulumdan "sistem girişi" seçilirse
     * giriş ekranı açılır. Geçici şifreyle girenler şifre değiştirmeden ana pencereye geçemez.
     */
    private static boolean authenticate(ConfigurableApplicationContext context, boolean setupRequired) {
        boolean loggedIn;
        if (setupRequired) {
            SetupDialog.Result result = context.getBean(SetupDialog.class).showAndWait();
            loggedIn = switch (result) {
                case COMPLETED -> true;
                case SYSTEM_LOGIN -> context.getBean(LoginDialog.class).showAndWait();
                case CANCELLED -> false;
            };
        } else {
            loggedIn = context.getBean(LoginDialog.class).showAndWait();
        }
        if (!loggedIn) {
            return false;
        }
        AppUserDto user = context.getBean(AuthService.class).getCurrentUser().orElseThrow();
        return !user.mustChangePassword()
                || context.getBean(ChangePasswordDialog.class).showDialog(null, true);
    }
}
