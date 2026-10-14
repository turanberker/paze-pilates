package com.paze.pilates.desktop.ui.student;

import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.StudentDto;
import com.paze.pilates.service.StudentService;
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
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class StudentFormDialog extends JDialog {

    private final StudentService studentService;
    private final MessageSource messages;

    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField phoneField;
    private JTextField emailField;
    private JCheckBox  birthDateCheck;
    private JSpinner   birthDateSpinner;
    private JTextArea  notesArea;
    private JLabel     errorLabel;

    private StudentDto saved;
    private StudentDto editing;

    public StudentFormDialog(StudentService studentService, MessageSource messages) {
        this.studentService = studentService;
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

        // Ad (zorunlu)
        lbl.gridy = row++;
        form.add(requiredLabel("student.form.label.firstname"), lbl);
        firstNameField = new JTextField(25);
        fld.gridy = row++;
        form.add(firstNameField, fld);

        // Soyad (zorunlu)
        lbl.gridy = row++;
        form.add(requiredLabel("student.form.label.lastname"), lbl);
        lastNameField = new JTextField(25);
        fld.gridy = row++;
        form.add(lastNameField, fld);

        // Telefon (zorunlu)
        lbl.gridy = row++;
        form.add(requiredLabel("student.form.label.phone"), lbl);
        phoneField = new JTextField(25);
        fld.gridy = row++;
        form.add(phoneField, fld);

        // E-posta (isteğe bağlı)
        lbl.gridy = row++;
        form.add(new JLabel(text("student.form.label.email")), lbl);
        emailField = new JTextField(25);
        fld.gridy = row++;
        form.add(emailField, fld);

        // Doğum tarihi (isteğe bağlı)
        lbl.gridy = row++;
        form.add(new JLabel(text("student.form.label.birthdate")), lbl);
        JPanel birthPanel = buildBirthDatePanel();
        fld.gridy = row++;
        form.add(birthPanel, fld);

        // Notlar (isteğe bağlı)
        lbl.gridy = row++;
        form.add(new JLabel(text("student.form.label.notes")), lbl);
        notesArea = new JTextArea(4, 25);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(notesArea);
        GridBagConstraints notesFld = new GridBagConstraints();
        notesFld.fill = GridBagConstraints.BOTH;
        notesFld.weightx = 1.0;
        notesFld.weighty = 1.0;
        notesFld.gridx = 0;
        notesFld.insets = new Insets(0, 0, 12, 0);
        notesFld.gridy = row++;
        form.add(notesScroll, notesFld);

        // Hata etiketi
        errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.decode("#DC2626"));
        GridBagConstraints errFld = new GridBagConstraints();
        errFld.fill = GridBagConstraints.HORIZONTAL;
        errFld.weightx = 1.0;
        errFld.gridx = 0;
        errFld.gridy = row;
        errFld.insets = new Insets(0, 0, 0, 0);
        form.add(errorLabel, errFld);

        add(form, BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(380, getHeight()));
        setLocationRelativeTo(owner);
    }

    private JPanel buildBirthDatePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);

        birthDateCheck = new JCheckBox(text("student.form.birthdate.known"));
        SpinnerDateModel model = new SpinnerDateModel();
        birthDateSpinner = new JSpinner(model);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(birthDateSpinner, "dd.MM.yyyy");
        birthDateSpinner.setEditor(editor);
        birthDateSpinner.setPreferredSize(new Dimension(120, birthDateSpinner.getPreferredSize().height));
        birthDateSpinner.setEnabled(false);

        birthDateCheck.addActionListener(e ->
                birthDateSpinner.setEnabled(birthDateCheck.isSelected()));

        panel.add(birthDateCheck);
        panel.add(birthDateSpinner);
        return panel;
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));

        JButton cancelBtn = new JButton(text("student.form.button.cancel"));
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = new JButton(text("student.form.button.save"));
        saveBtn.putClientProperty("JButton.buttonType", "default");
        saveBtn.addActionListener(e -> onSave());

        panel.add(cancelBtn);
        panel.add(saveBtn);
        return panel;
    }

    private void onSave() {
        String firstName = firstNameField.getText().trim();
        String lastName  = lastNameField.getText().trim();
        String phone     = phoneField.getText().trim();
        String email     = emailField.getText().trim();
        String notes     = notesArea.getText().trim();

        LocalDate birthDate = null;
        if (birthDateCheck.isSelected()) {
            Date d = (Date) birthDateSpinner.getValue();
            birthDate = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }

        Long id = (editing != null) ? editing.id() : null;
        StudentDto dto = new StudentDto(
                id, firstName, lastName,
                phone.isEmpty() ? null : phone,
                email.isEmpty() ? null : email,
                birthDate,
                notes.isEmpty() ? null : notes,
                true
        );

        try {
            saved = studentService.save(dto);
            dispose();
        } catch (BusinessException ex) {
            errorLabel.setText(text(ex.getMessage()));
        } catch (Exception ex) {
            errorLabel.setText(ex.getMessage());
        }
    }

    /** Yeni öğrenci formu; kaydedilen DTO'yu döner, iptal edilirse null. */
    public StudentDto showForNew(Frame owner) {
        editing = null;
        init("student.form.title.add", owner);
        setVisible(true);
        return saved;
    }

    /** Mevcut öğrenci için düzenleme formu; kaydedilen DTO'yu döner, iptal edilirse null. */
    public StudentDto showForEdit(Frame owner, StudentDto student) {
        editing = student;
        init("student.form.title.edit", owner);
        firstNameField.setText(student.firstName());
        lastNameField.setText(student.lastName());
        phoneField.setText(student.phone() != null ? student.phone() : "");
        emailField.setText(student.email() != null ? student.email() : "");
        notesArea.setText(student.notes() != null ? student.notes() : "");
        if (student.birthDate() != null) {
            birthDateCheck.setSelected(true);
            birthDateSpinner.setEnabled(true);
            Date d = Date.from(student.birthDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
            birthDateSpinner.setValue(d);
        }
        setVisible(true);
        return saved;
    }

    private JLabel requiredLabel(String key) {
        JLabel lbl = new JLabel(text(key) + " *");
        return lbl;
    }

    private String text(String key) {
        return messages.getMessage(key, null, key, Locale.getDefault());
    }
}
