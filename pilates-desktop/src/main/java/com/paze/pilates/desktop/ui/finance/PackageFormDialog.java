package com.paze.pilates.desktop.ui.finance;

import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.SessionPackageDto;
import com.paze.pilates.service.SessionPackageService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PackageFormDialog extends JDialog {

    private final SessionPackageService packageService;
    private final MessageSource messages;

    private JTextField nameField;
    private JTextField priceField;
    private JSpinner   sessionSpinner;
    private JSpinner   startDateSpinner;
    private JCheckBox  activeCheck;
    private JLabel     errorLabel;

    private SessionPackageDto saved;
    private SessionPackageDto editing;

    public PackageFormDialog(SessionPackageService packageService, MessageSource messages) {
        this.packageService = packageService;
        this.messages = messages;
    }

    private void init(String titleKey, Frame owner) {
        setTitle(text(titleKey));
        setModal(true);
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 8, 24));

        GridBagConstraints lbl = new GridBagConstraints();
        lbl.fill = GridBagConstraints.HORIZONTAL;
        lbl.weightx = 1.0;
        lbl.gridx = 0;
        lbl.insets = new Insets(0, 0, 4, 0);

        GridBagConstraints fld = new GridBagConstraints();
        fld.fill = GridBagConstraints.HORIZONTAL;
        fld.weightx = 1.0;
        fld.gridx = 0;
        fld.insets = new Insets(0, 0, 12, 0);

        int row = 0;

        lbl.gridy = row++;
        form.add(new JLabel(text("package.form.label.name") + " *"), lbl);
        nameField = new JTextField(25);
        fld.gridy = row++;
        form.add(nameField, fld);

        lbl.gridy = row++;
        form.add(new JLabel(text("package.form.label.sessions") + " *"), lbl);
        sessionSpinner = new JSpinner(new SpinnerNumberModel(8, 1, 999, 1));
        fld.gridy = row++;
        form.add(sessionSpinner, fld);

        lbl.gridy = row++;
        form.add(new JLabel(text("package.form.label.price") + " *"), lbl);
        priceField = new JTextField(25);
        fld.gridy = row++;
        form.add(priceField, fld);

        lbl.gridy = row++;
        form.add(new JLabel(text("package.form.label.startdate") + " *"), lbl);
        startDateSpinner = new JSpinner(new SpinnerDateModel());
        startDateSpinner.setEditor(new JSpinner.DateEditor(startDateSpinner, "dd.MM.yyyy"));
        fld.gridy = row++;
        form.add(startDateSpinner, fld);

        activeCheck = new JCheckBox(text("package.form.label.active"), true);
        fld.gridy = row++;
        form.add(activeCheck, fld);

        errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.decode("#DC2626"));
        GridBagConstraints errFld = new GridBagConstraints();
        errFld.fill = GridBagConstraints.HORIZONTAL;
        errFld.weightx = 1.0;
        errFld.gridx = 0;
        errFld.gridy = row;
        form.add(errorLabel, errFld);

        add(form, BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(380, getHeight()));
        setLocationRelativeTo(owner);
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));

        JButton cancelBtn = new JButton(text("package.form.button.cancel"));
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = new JButton(text("package.form.button.save"));
        saveBtn.putClientProperty("JButton.buttonType", "default");
        saveBtn.addActionListener(e -> onSave());

        panel.add(cancelBtn);
        panel.add(saveBtn);
        return panel;
    }

    private void onSave() {
        BigDecimal price = parsePrice(priceField.getText());
        Date d = (Date) startDateSpinner.getValue();
        LocalDate startDate = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        SessionPackageDto dto = new SessionPackageDto(
                editing != null ? editing.id() : null,
                nameField.getText().trim(),
                price,
                (Integer) sessionSpinner.getValue(),
                startDate,
                activeCheck.isSelected());

        try {
            saved = packageService.save(dto);
            dispose();
        } catch (BusinessException ex) {
            errorLabel.setText(text(ex.getMessage()));
        } catch (Exception ex) {
            errorLabel.setText(ex.getMessage());
        }
    }

    /** "1.250,50" veya "1250.50" biçimini kabul eder; geçersizse null döner. */
    private BigDecimal parsePrice(String raw) {
        String s = raw.trim().replace(" ", "");
        if (s.isEmpty()) return null;
        if (s.contains(",")) {
            s = s.replace(".", "").replace(',', '.');
        }
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Yeni paket formu; kaydedilen DTO'yu döner, iptal edilirse null. */
    public SessionPackageDto showForNew(Frame owner) {
        editing = null;
        init("package.form.title.add", owner);
        setVisible(true);
        return saved;
    }

    /** Mevcut paket için düzenleme formu; kaydedilen DTO'yu döner, iptal edilirse null. */
    public SessionPackageDto showForEdit(Frame owner, SessionPackageDto pkg) {
        editing = pkg;
        init("package.form.title.edit", owner);
        nameField.setText(pkg.name());
        priceField.setText(pkg.price().toPlainString());
        sessionSpinner.setValue(pkg.sessionCount());
        startDateSpinner.setValue(
                Date.from(pkg.startDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        activeCheck.setSelected(pkg.active());
        setVisible(true);
        return saved;
    }

    private String text(String key) {
        return messages.getMessage(key, null, key, Locale.getDefault());
    }
}
