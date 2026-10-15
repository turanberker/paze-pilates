package com.paze.pilates.desktop.ui.finance;

import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.ExpenseDto;
import com.paze.pilates.service.ExpenseService;
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
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ExpenseFormDialog extends JDialog {

    private final ExpenseService expenseService;
    private final MessageSource messages;

    private JSpinner   dateSpinner;
    private JTextField amountField;
    private JTextField descriptionField;
    private JLabel     errorLabel;

    private ExpenseDto saved;
    private ExpenseDto editing;

    public ExpenseFormDialog(ExpenseService expenseService, MessageSource messages) {
        this.expenseService = expenseService;
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
        form.add(new JLabel(text("expense.form.label.date") + " *"), lbl);
        dateSpinner = new JSpinner(new SpinnerDateModel());
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd.MM.yyyy"));
        fld.gridy = row++;
        form.add(dateSpinner, fld);

        lbl.gridy = row++;
        form.add(new JLabel(text("expense.form.label.amount") + " *"), lbl);
        amountField = new JTextField(25);
        fld.gridy = row++;
        form.add(amountField, fld);

        lbl.gridy = row++;
        form.add(new JLabel(text("expense.form.label.description") + " *"), lbl);
        descriptionField = new JTextField(25);
        fld.gridy = row++;
        form.add(descriptionField, fld);

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

        JButton cancelBtn = new JButton(text("expense.form.button.cancel"));
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = new JButton(text("expense.form.button.save"));
        saveBtn.putClientProperty("JButton.buttonType", "default");
        saveBtn.addActionListener(e -> onSave());

        panel.add(cancelBtn);
        panel.add(saveBtn);
        return panel;
    }

    private void onSave() {
        Date d = (Date) dateSpinner.getValue();
        ExpenseDto dto = new ExpenseDto(
                editing != null ? editing.id() : null,
                d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                parseAmount(amountField.getText()),
                descriptionField.getText().trim());

        try {
            saved = expenseService.save(dto);
            dispose();
        } catch (BusinessException ex) {
            errorLabel.setText(text(ex.getMessage()));
        } catch (Exception ex) {
            errorLabel.setText(text("error.unexpected"));
        }
    }

    /** "1.250,50" veya "1250.50" biçimini kabul eder; geçersizse null döner. */
    private BigDecimal parseAmount(String raw) {
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

    /** Yeni gider formu; tarih varsayılan olarak verilen gündür. Kaydedilen DTO'yu döner, iptal edilirse null. */
    public ExpenseDto showForNew(Frame owner, LocalDate defaultDate) {
        editing = null;
        init("expense.form.title.add", owner);
        dateSpinner.setValue(toDate(defaultDate));
        setVisible(true);
        return saved;
    }

    /** Mevcut gider için düzenleme formu; kaydedilen DTO'yu döner, iptal edilirse null. */
    public ExpenseDto showForEdit(Frame owner, ExpenseDto expense) {
        editing = expense;
        init("expense.form.title.edit", owner);
        dateSpinner.setValue(toDate(expense.expenseDate()));
        amountField.setText(expense.amount().toPlainString());
        descriptionField.setText(expense.description());
        setVisible(true);
        return saved;
    }

    private static Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private String text(String key) {
        return messages.getMessage(key, null, key, Locale.getDefault());
    }
}
