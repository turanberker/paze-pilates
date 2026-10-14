package com.paze.pilates.desktop.ui.student;

import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.SessionPackageDto;
import com.paze.pilates.dto.StudentDto;
import com.paze.pilates.dto.StudentPackageDto;
import com.paze.pilates.service.SessionPackageService;
import com.paze.pilates.service.StudentPackageService;
import com.paze.pilates.service.StudentService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingWorker;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;

/** Öğrenciye aktif paket atama formu. */
@org.springframework.stereotype.Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class StudentPackageAssignDialog extends JDialog {

    private final StudentPackageService studentPackageService;
    private final StudentService studentService;
    private final SessionPackageService packageService;
    private final MessageSource messages;

    private JComboBox<StudentDto> studentCombo;
    private JComboBox<SessionPackageDto> packageCombo;
    private JCheckBox paidCheck;
    private JSpinner paymentDateSpinner;
    private JButton saveBtn;
    private JLabel errorLabel;

    private StudentPackageDto saved;

    public StudentPackageAssignDialog(
            StudentPackageService studentPackageService,
            StudentService studentService,
            SessionPackageService packageService,
            MessageSource messages) {
        this.studentPackageService = studentPackageService;
        this.studentService = studentService;
        this.packageService = packageService;
        this.messages = messages;
    }

    private void init(Frame owner) {
        setTitle(text("student_package.form.title"));
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
        form.add(new JLabel(text("student_package.form.label.student") + " *"), lbl);
        studentCombo = new JComboBox<>();
        studentCombo.setRenderer(new LabelRenderer());
        fld.gridy = row++;
        form.add(studentCombo, fld);

        lbl.gridy = row++;
        form.add(new JLabel(text("student_package.form.label.package") + " *"), lbl);
        packageCombo = new JComboBox<>();
        packageCombo.setRenderer(new LabelRenderer());
        fld.gridy = row++;
        form.add(packageCombo, fld);

        paidCheck = new JCheckBox(text("student_package.form.label.paid"), false);
        paidCheck.addActionListener(e -> paymentDateSpinner.setEnabled(paidCheck.isSelected()));
        fld.gridy = row++;
        form.add(paidCheck, fld);

        lbl.gridy = row++;
        form.add(new JLabel(text("student_package.form.label.payment_date")), lbl);
        paymentDateSpinner = new JSpinner(new SpinnerDateModel());
        paymentDateSpinner.setEditor(new JSpinner.DateEditor(paymentDateSpinner, "dd.MM.yyyy"));
        paymentDateSpinner.setEnabled(false);
        fld.gridy = row++;
        form.add(paymentDateSpinner, fld);

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
        setMinimumSize(new Dimension(420, getHeight()));
        setLocationRelativeTo(owner);
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));

        JButton cancelBtn = new JButton(text("student_package.form.button.cancel"));
        cancelBtn.addActionListener(e -> dispose());

        saveBtn = new JButton(text("student_package.form.button.save"));
        saveBtn.putClientProperty("JButton.buttonType", "default");
        saveBtn.setEnabled(false);
        saveBtn.addActionListener(e -> onSave());

        panel.add(cancelBtn);
        panel.add(saveBtn);
        return panel;
    }

    /** Aktif öğrenci ve aktif paket listelerini arka planda yükler. */
    private void loadOptions() {
        new SwingWorker<Options, Void>() {
            @Override protected Options doInBackground() {
                return new Options(studentService.findAll(), packageService.findActive());
            }
            @Override protected void done() {
                try {
                    Options o = get();
                    o.students().forEach(studentCombo::addItem);
                    o.packages().forEach(packageCombo::addItem);
                    if (o.packages().isEmpty()) {
                        errorLabel.setText(text("student_package.form.no_active_package"));
                    }
                    saveBtn.setEnabled(true);
                } catch (InterruptedException | ExecutionException ex) {
                    errorLabel.setText(text("error.unexpected"));
                }
            }
        }.execute();
    }

    private void onSave() {
        StudentDto student = (StudentDto) studentCombo.getSelectedItem();
        SessionPackageDto pkg = (SessionPackageDto) packageCombo.getSelectedItem();
        Long studentId = student != null ? student.id() : null;
        Long packageId = pkg != null ? pkg.id() : null;
        boolean paid = paidCheck.isSelected();
        LocalDate paymentDate = paid ? toLocalDate((Date) paymentDateSpinner.getValue()) : null;

        saveBtn.setEnabled(false);
        new SwingWorker<StudentPackageDto, Void>() {
            @Override protected StudentPackageDto doInBackground() {
                return studentPackageService.assign(studentId, packageId, paid, paymentDate);
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

    /** Formu açar; atanan kaydı döner, iptal edilirse null. */
    public StudentPackageDto showDialog(Frame owner) {
        init(owner);
        loadOptions();
        setVisible(true);
        return saved;
    }

    private static LocalDate toLocalDate(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private String text(String key) {
        return messages.getMessage(key, null, key, Locale.getDefault());
    }

    private record Options(List<StudentDto> students, List<SessionPackageDto> packages) {
    }

    /** Combo içinde öğrenci için "Ad Soyad", paket için "Ad (N seans)" gösterir. */
    private class LabelRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof StudentDto s) {
                setText(s.firstName() + " " + s.lastName());
            } else if (value instanceof SessionPackageDto p) {
                setText(messages.getMessage("student_package.form.package_option",
                        new Object[] {p.name(), p.sessionCount()}, Locale.getDefault()));
            }
            return this;
        }
    }
}
