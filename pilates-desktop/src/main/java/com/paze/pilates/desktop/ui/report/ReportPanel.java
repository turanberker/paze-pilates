package com.paze.pilates.desktop.ui.report;

import java.util.Locale;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Component
public class ReportPanel extends JPanel {

    public ReportPanel(MessageSource messages) {
        add(new JLabel(messages.getMessage("placeholder.under_construction", null, Locale.getDefault()),
            SwingConstants.CENTER));
    }
}
