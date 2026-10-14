package com.paze.pilates.desktop.ui.main;

import com.paze.pilates.dto.AppUserDto;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Component
public class MainMenuBar extends JMenuBar {

    private final MessageSource messages;
    private Consumer<String> navCallback;
    private Runnable exitCallback;
    private Runnable changePasswordCallback;

    public MainMenuBar(MessageSource messages) {
        this.messages = messages;
    }

    /** Menüyü kullanıcının görebildiği hedeflerle kurar; boş kalan menü eklenmez. */
    public void build(AppUserDto user) {
        removeAll();

        JMenu fileMenu = new JMenu(text("menu.file"));
        addNav(fileMenu, user, "menu.file.dashboard", NavTarget.DASHBOARD);
        if (!user.staticAdmin()) {
            JMenuItem changePassword = new JMenuItem(text("menu.file.change_password"));
            changePassword.addActionListener(e -> {
                if (changePasswordCallback != null) {
                    changePasswordCallback.run();
                }
            });
            fileMenu.add(changePassword);
        }
        fileMenu.addSeparator();
        JMenuItem exitItem = new JMenuItem(text("menu.file.exit"));
        exitItem.addActionListener(e -> {
            if (exitCallback != null) {
                exitCallback.run();
            }
        });
        fileMenu.add(exitItem);
        add(fileMenu);

        JMenu definitionsMenu = new JMenu(text("menu.definitions"));
        addNav(definitionsMenu, user, "menu.definitions.studio", NavTarget.STUDIO);
        addNav(definitionsMenu, user, "menu.definitions.instructor", NavTarget.INSTRUCTOR);
        addNav(definitionsMenu, user, "menu.definitions.users", NavTarget.USERS);
        addIfNotEmpty(definitionsMenu);

        JMenu studentMenu = new JMenu(text("menu.student"));
        addNav(studentMenu, user, "menu.student.list", NavTarget.STUDENT);
        addNav(studentMenu, user, "menu.student.packages", NavTarget.STUDENT_PACKAGE);
        addIfNotEmpty(studentMenu);

        JMenu planningMenu = new JMenu(text("menu.planning"));
        addNav(planningMenu, user, "menu.planning.calendar", NavTarget.CALENDAR);
        addIfNotEmpty(planningMenu);

        JMenu financeMenu = new JMenu(text("menu.finance"));
        addNav(financeMenu, user, "menu.finance.packages", NavTarget.PACKAGE);
        addNav(financeMenu, user, "menu.finance.payments", NavTarget.FINANCE);
        addIfNotEmpty(financeMenu);

        JMenu reportMenu = new JMenu(text("menu.report"));
        addNav(reportMenu, user, "menu.report.overview", NavTarget.REPORT);
        addIfNotEmpty(reportMenu);

        add(Box.createHorizontalGlue());
        add(new JLabel(messages.getMessage("menu.user_info",
                new Object[] {user.fullName(), text("role." + user.role().name())}, Locale.getDefault())));
        add(Box.createHorizontalStrut(12));
        revalidate();
        repaint();
    }

    private void addNav(JMenu menu, AppUserDto user, String labelKey, NavTarget target) {
        if (!target.isAllowedFor(user)) {
            return;
        }
        JMenuItem item = new JMenuItem(text(labelKey));
        item.addActionListener(e -> {
            if (navCallback != null) {
                navCallback.accept(target.key());
            }
        });
        menu.add(item);
    }

    private void addIfNotEmpty(JMenu menu) {
        if (menu.getItemCount() > 0) {
            add(menu);
        }
    }

    public void setNavCallback(Consumer<String> callback) {
        this.navCallback = callback;
    }

    public void setExitCallback(Runnable callback) {
        this.exitCallback = callback;
    }

    public void setChangePasswordCallback(Runnable callback) {
        this.changePasswordCallback = callback;
    }

    private String text(String key) {
        return messages.getMessage(key, null, key, Locale.getDefault());
    }
}
