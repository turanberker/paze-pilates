package com.paze.pilates.desktop.ui.reservation;

import java.util.Locale;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class CalendarPanel extends JPanel {

    public CalendarPanel(MessageSource messages) {
        add(new JLabel(messages.getMessage("placeholder.under_construction", null, Locale.getDefault()),
            SwingConstants.CENTER));
    }
}
