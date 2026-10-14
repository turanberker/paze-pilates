package com.paze.pilates.desktop.ui.shared;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/** Stüdyo logosunu çözümleyip ekranda gösterilecek boyuta küçültür. */
public final class LogoImages {

    private LogoImages() {
    }

    /** PNG/JPEG içeriğini görsele çevirir; okunamazsa null döner. */
    public static BufferedImage decode(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        try {
            return ImageIO.read(new ByteArrayInputStream(data));
        } catch (IOException ex) {
            return null;
        }
    }

    /** En-boy oranını koruyarak verilen kutuya sığdırır; görsel zaten küçükse büyütmez. */
    public static ImageIcon fit(BufferedImage image, int maxWidth, int maxHeight) {
        double scale = Math.min(1.0, Math.min(
                (double) maxWidth / image.getWidth(), (double) maxHeight / image.getHeight()));
        int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
        return new ImageIcon(image.getScaledInstance(width, height, Image.SCALE_SMOOTH));
    }
}
