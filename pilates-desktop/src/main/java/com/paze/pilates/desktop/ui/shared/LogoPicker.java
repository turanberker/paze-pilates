package com.paze.pilates.desktop.ui.shared;

import com.paze.pilates.dto.StudioDto;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;

/**
 * Stüdyo logosu seçimi: önizleme, Seç / Kaldır düğmeleri. Kurulum ve stüdyo ayarları ekranlarında ortak kullanılır.
 * Seçilen dosyanın asıl doğrulaması serviste yapılır.
 */
public class LogoPicker extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(LogoPicker.class);
    private static final int PREVIEW_WIDTH = 120;
    private static final int PREVIEW_HEIGHT = 56;

    private final MessageSource messages;
    private final Consumer<String> errorHandler;
    private final JLabel preview;
    private final JButton removeBtn;
    private byte[] logo;

    /** @param errorHandler dosya okunamadığında kullanıcıya gösterilecek mesajı alır */
    public LogoPicker(MessageSource messages, Consumer<String> errorHandler) {
        super(new BorderLayout(10, 0));
        this.messages = messages;
        this.errorHandler = errorHandler;

        preview = new JLabel(text("studio.logo.none"), SwingConstants.CENTER);
        preview.setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
        preview.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")));

        JButton chooseBtn = new JButton(text("studio.logo.button.choose"));
        chooseBtn.addActionListener(e -> onChoose());
        removeBtn = new JButton(text("studio.logo.button.remove"));
        removeBtn.setEnabled(false);
        removeBtn.addActionListener(e -> setLogo(null));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.setOpaque(false);
        buttons.add(chooseBtn);
        buttons.add(removeBtn);
        JLabel hint = new JLabel(text("studio.logo.hint", StudioDto.LOGO_MAX_BYTES / 1024));
        hint.setForeground(UIManager.getColor("Label.disabledForeground"));
        hint.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));

        JPanel right = new JPanel(new BorderLayout(0, 4));
        right.setOpaque(false);
        right.add(buttons, BorderLayout.NORTH);
        right.add(hint, BorderLayout.CENTER);

        setOpaque(false);
        add(preview, BorderLayout.WEST);
        add(right, BorderLayout.CENTER);
    }

    /** Seçili logonun dosya içeriği; logo yoksa null. */
    public byte[] getLogo() {
        return logo;
    }

    /** Önizlemeyi verilen logoyla günceller; null veya okunamayan içerik "logo yok" gösterir. */
    public void setLogo(byte[] data) {
        BufferedImage image = LogoImages.decode(data);
        logo = image != null ? data : null;
        if (image == null) {
            preview.setIcon(null);
            preview.setText(text("studio.logo.none"));
        } else {
            preview.setText(null);
            preview.setIcon(LogoImages.fit(image, PREVIEW_WIDTH - 8, PREVIEW_HEIGHT - 8));
        }
        removeBtn.setEnabled(logo != null);
    }

    private void onChoose() {
        JFileChooser chooser = new JFileChooser();
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter(text("studio.logo.file_filter"), "png", "jpg", "jpeg"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        // Çok büyük bir dosyayı belleğe almamak için boyuta okumadan önce bakılır; asıl kontrol serviste.
        if (file.length() > StudioDto.LOGO_MAX_BYTES) {
            errorHandler.accept(text("studio.error.logo_too_large", StudioDto.LOGO_MAX_BYTES / 1024));
            return;
        }
        try {
            byte[] data = Files.readAllBytes(file.toPath());
            if (LogoImages.decode(data) == null) {
                errorHandler.accept(text("studio.error.logo_invalid_format"));
                return;
            }
            setLogo(data);
        } catch (IOException ex) {
            log.warn("Logo dosyası okunamadı: {}", file, ex);
            errorHandler.accept(text("studio.logo.error.read_failed"));
        }
    }

    private String text(String key, Object... args) {
        return messages.getMessage(key, args, key, Locale.getDefault());
    }
}
