package com.paze.pilates.desktop.ui.report;

import java.util.Locale;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class ReportPanel extends JPanel {

    public ReportPanel(MessageSource messages) {
        add(new JLabel(messages.getMessage("placeholder.under_construction", null, Locale.getDefault()),
            SwingConstants.CENTER));
    }
}
