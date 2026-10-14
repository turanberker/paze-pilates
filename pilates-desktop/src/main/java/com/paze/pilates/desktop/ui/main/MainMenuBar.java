package com.paze.pilates.desktop.ui.main;

import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class MainMenuBar extends JMenuBar {

    private final MessageSource messages;
    private Consumer<String> navCallback;
    private Runnable exitCallback;

    public MainMenuBar(MessageSource messages) {
        this.messages = messages;
        initComponents();
    }

    private void initComponents() {
        JMenu fileMenu = new JMenu(text("menu.file"));
        fileMenu.add(navItem("menu.file.dashboard", "dashboard"));
        fileMenu.addSeparator();
        JMenuItem exitItem = new JMenuItem(text("menu.file.exit"));
        exitItem.addActionListener(e -> {
            if (exitCallback != null) {
                exitCallback.run();
            }
        });
        fileMenu.add(exitItem);

        JMenu definitionsMenu = new JMenu(text("menu.definitions"));
        definitionsMenu.add(navItem("menu.definitions.studio", "studio"));
        definitionsMenu.add(navItem("menu.definitions.instructor", "instructor"));

        JMenu studentMenu = new JMenu(text("menu.student"));
        studentMenu.add(navItem("menu.student.list", "student"));
        studentMenu.add(navItem("menu.student.packages", "student_package"));

        JMenu planningMenu = new JMenu(text("menu.planning"));
        planningMenu.add(navItem("menu.planning.calendar", "calendar"));

        JMenu financeMenu = new JMenu(text("menu.finance"));
        financeMenu.add(navItem("menu.finance.packages", "package"));
        financeMenu.add(navItem("menu.finance.payments", "finance"));

        JMenu reportMenu = new JMenu(text("menu.report"));
        reportMenu.add(navItem("menu.report.overview", "report"));

        add(fileMenu);
        add(definitionsMenu);
        add(studentMenu);
        add(planningMenu);
        add(financeMenu);
        add(reportMenu);
    }

    private JMenuItem navItem(String labelKey, String panelKey) {
        JMenuItem item = new JMenuItem(text(labelKey));
        item.addActionListener(e -> {
            if (navCallback != null) {
                navCallback.accept(panelKey);
            }
        });
        return item;
    }

    public void setNavCallback(Consumer<String> callback) {
        this.navCallback = callback;
    }

    public void setExitCallback(Runnable callback) {
        this.exitCallback = callback;
    }

    private String text(String key) {
        return messages.getMessage(key, null, Locale.getDefault());
    }
}
