package com.paze.pilates.desktop.ui.student;

import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.StudentPackageDto;
import com.paze.pilates.service.StudentPackageService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingWorker;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Ödemesi alınmamış paket ataması için ödeme alma formu. */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CompletePaymentDialog extends JDialog {

    private final StudentPackageService studentPackageService;
    private final MessageSource messages;

    private JSpinner paymentDateSpinner;
    private JButton saveBtn;
    private JLabel errorLabel;

    private StudentPackageDto target;
    private StudentPackageDto saved;

    public CompletePaymentDialog(StudentPackageService studentPackageService, MessageSource messages) {
        this.studentPackageService = studentPackageService;
        this.messages = messages;
    }

    private void init(Frame owner) {
        setTitle(text("student_package.payment.title"));
        setModal(true);
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 8, 24));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridx = 0;
        c.insets = new Insets(0, 0, 8, 0);

        int row = 0;
        c.gridy = row++;
        form.add(new JLabel(target.studentName() + " · " + target.packageName()), c);

        c.gridy = row++;
        form.add(new JLabel(text("student_package.form.label.payment_date") + " *"), c);
        paymentDateSpinner = new JSpinner(new SpinnerDateModel());
        paymentDateSpinner.setEditor(new JSpinner.DateEditor(paymentDateSpinner, "dd.MM.yyyy"));
        c.gridy = row++;
        form.add(paymentDateSpinner, c);

        errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.decode("#DC2626"));
        c.gridy = row;
        form.add(errorLabel, c);

        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        JButton cancelBtn = new JButton(text("student_package.form.button.cancel"));
        cancelBtn.addActionListener(e -> dispose());
        saveBtn = new JButton(text("student_package.payment.button.save"));
        saveBtn.putClientProperty("JButton.buttonType", "default");
        saveBtn.addActionListener(e -> onSave());
        buttons.add(cancelBtn);
        buttons.add(saveBtn);
        add(buttons, BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(360, getHeight()));
        setLocationRelativeTo(owner);
    }

    private void onSave() {
        Long id = target.id();
        LocalDate paymentDate = ((Date) paymentDateSpinner.getValue())
                .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        saveBtn.setEnabled(false);
        new SwingWorker<StudentPackageDto, Void>() {
            @Override protected StudentPackageDto doInBackground() {
                return studentPackageService.completePayment(id, paymentDate);
            }
            @Override protected void done() {
                try {
                    saved = get();
                    dispose();
                } catch (ExecutionException ex) {
                    errorLabel.setText(ex.getCause() instanceof BusinessException be
                            ? text(be.getMessage()) : text("error.unexpected"));
                    saveBtn.setEnabled(true);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /** Ödeme formunu açar; güncellenen kaydı döner, iptal edilirse null. */
    public StudentPackageDto showDialog(Frame owner, StudentPackageDto studentPackage) {
        target = studentPackage;
        init(owner);
        setVisible(true);
        return saved;
    }

    private String text(String key) {
        return messages.getMessage(key, null, key, Locale.getDefault());
    }
}
