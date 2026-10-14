package com.paze.pilates.desktop.ui.student;

import java.util.Locale;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class StudentListPanel extends JPanel {

    public StudentListPanel(MessageSource messages) {
        add(new JLabel(messages.getMessage("placeholder.under_construction", null, Locale.getDefault()),
            SwingConstants.CENTER));
    }
}
