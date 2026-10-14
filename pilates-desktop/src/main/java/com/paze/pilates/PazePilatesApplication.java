package com.paze.pilates;

import com.formdev.flatlaf.FlatLightLaf;
import com.paze.pilates.desktop.MainFrame;
import com.paze.pilates.desktop.ui.auth.LoginDialog;
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

            LoginDialog loginDialog = context.getBean(LoginDialog.class);
            boolean loggedIn = loginDialog.showAndWait();

            if (loggedIn) {
                context.getBean(MainFrame.class).showWindow();
            } else {
                context.close();
                System.exit(0);
            }
        });
    }
}
