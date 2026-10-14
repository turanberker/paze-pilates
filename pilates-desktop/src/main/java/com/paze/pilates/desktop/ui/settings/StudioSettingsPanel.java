package com.paze.pilates.desktop.ui.settings;

import com.paze.pilates.desktop.ui.shared.LogoPicker;
import com.paze.pilates.desktop.ui.shared.NotificationBar;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.StudioSettingsDto;
import com.paze.pilates.service.StudioService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Ayarlar > Stüdyo Ayarları: tek stüdyo kaydının adı ve header logosu. */
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Component
public class StudioSettingsPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(StudioSettingsPanel.class);

    private final StudioService studioService;
    private final NotificationBar notificationBar;
    private final MessageSource messages;

    private JTextField nameField;
    private LogoPicker logoPicker;
    private JButton saveBtn;
    private Runnable onSaved;

    public StudioSettingsPanel(StudioService studioService, NotificationBar notificationBar, MessageSource messages) {
        this.studioService = studioService;
        this.notificationBar = notificationBar;
        this.messages = messages;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 20));
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JLabel title = new JLabel(text("settings.studio.title"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;

        nameField = new JTextField(30);
        logoPicker = new LogoPicker(messages, notificationBar::showError);
        addRow(form, c, 0, "studio.label.name", nameField);
        addRow(form, c, 1, "settings.studio.label.logo", logoPicker);

        saveBtn = new JButton(text("studio.button.save"));
        saveBtn.putClientProperty("JButton.buttonType", "default");
        saveBtn.addActionListener(e -> onSave());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.add(saveBtn);
        c.gridy = 4;
        c.insets = new Insets(8, 0, 0, 0);
        form.add(buttons, c);

        // Form sol üstte, doğal boyutunda kalsın.
        JPanel row = new JPanel(new BorderLayout());
        row.add(form, BorderLayout.WEST);
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(row, BorderLayout.NORTH);
        add(holder, BorderLayout.CENTER);
    }

    private void addRow(JPanel form, GridBagConstraints c, int row, String labelKey, JComponent field) {
        c.gridy = row * 2;
        c.insets = new Insets(0, 0, 4, 0);
        form.add(new JLabel(text(labelKey)), c);
        c.gridy = row * 2 + 1;
        c.insets = new Insets(0, 0, 14, 0);
        form.add(field, c);
    }

    /** Kayıt başarılı olduğunda çalışır (ör. header logosunu yenilemek için). */
    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    /** Ekran her açıldığında güncel ayarları yükler. */
    public void reload() {
        new SwingWorker<StudioSettingsDto, Void>() {
            @Override protected StudioSettingsDto doInBackground() {
                return studioService.getSettings();
            }
            @Override protected void done() {
                try {
                    StudioSettingsDto settings = get();
                    nameField.setText(settings.name());
                    logoPicker.setLogo(settings.logo());
                } catch (ExecutionException ex) {
                    showError(ex);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void onSave() {
        StudioSettingsDto settings = new StudioSettingsDto(nameField.getText(), logoPicker.getLogo());
        saveBtn.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                studioService.updateSettings(settings);
                return null;
            }
            @Override protected void done() {
                saveBtn.setEnabled(true);
                try {
                    get();
                    nameField.setText(settings.name().trim());
                    notificationBar.showSuccess(text("studio.save.success"));
                    if (onSaved != null) {
                        onSaved.run();
                    }
                } catch (ExecutionException ex) {
                    showError(ex);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void showError(ExecutionException ex) {
        if (ex.getCause() instanceof BusinessException be) {
            notificationBar.showError(messages.getMessage(be.getMessage(), be.getArgs(), be.getMessage(),
                    Locale.getDefault()));
        } else {
            log.error("Stüdyo ayarları işlenemedi", ex.getCause());
            notificationBar.showError(text("error.unexpected"));
        }
    }

    private String text(String key) {
        return messages.getMessage(key, null, key, Locale.getDefault());
    }
}
