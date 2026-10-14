package com.paze.pilates.desktop.ui.instructor;

import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.InstructorDto;
import com.paze.pilates.service.InstructorService;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
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

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class InstructorFormDialog extends JDialog {

    private static final Logger log = LoggerFactory.getLogger(InstructorFormDialog.class);

    private final InstructorService instructorService;
    private final MessageSource messages;

    private JTextField firstNameField;
    private JTextField lastNameField;
    private JLabel errorLabel;
    private JButton saveBtn;

    private InstructorDto saved;
    private InstructorDto editing;

    public InstructorFormDialog(InstructorService instructorService, MessageSource messages) {
        this.instructorService = instructorService;
        this.messages = messages;
    }

    private void init(String titleKey, Frame owner) {
        setTitle(text(titleKey));
        setModal(true);
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 8, 24));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridx = 0;

        c.gridy = 0;
        c.insets = new Insets(0, 0, 4, 0);
        form.add(new JLabel(text("instructor.form.label.first_name")), c);

        firstNameField = new JTextField(20);
        c.gridy = 1;
        c.insets = new Insets(0, 0, 14, 0);
        form.add(firstNameField, c);

        c.gridy = 2;
        c.insets = new Insets(0, 0, 4, 0);
        form.add(new JLabel(text("instructor.form.label.last_name")), c);

        lastNameField = new JTextField(20);
        c.gridy = 3;
        c.insets = new Insets(0, 0, 14, 0);
        form.add(lastNameField, c);

        errorLabel = new JLabel(" ");
        errorLabel.setForeground(java.awt.Color.decode("#DC2626"));
        c.gridy = 4;
        c.insets = new Insets(0, 0, 0, 0);
        form.add(errorLabel, c);

        add(form, BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
        getRootPane().setDefaultButton(saveBtn);

        pack();
        setMinimumSize(new Dimension(320, getHeight()));
        setLocationRelativeTo(owner);
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));

        JButton cancelBtn = new JButton(text("instructor.form.button.cancel"));
        cancelBtn.addActionListener(e -> dispose());

        saveBtn = new JButton(text("instructor.form.button.save"));
        saveBtn.putClientProperty("JButton.buttonType", "default");
        saveBtn.addActionListener(e -> onSave());

        panel.add(cancelBtn);
        panel.add(saveBtn);
        return panel;
    }

    private void onSave() {
        Long id = (editing != null) ? editing.id() : null;
        InstructorDto dto = new InstructorDto(id, firstNameField.getText(), lastNameField.getText());

        saveBtn.setEnabled(false);
        errorLabel.setText(" ");
        new SwingWorker<InstructorDto, Void>() {
            @Override protected InstructorDto doInBackground() {
                return instructorService.saveInstructor(dto);
            }
            @Override protected void done() {
                saveBtn.setEnabled(true);
                try {
                    saved = get();
                    dispose();
                } catch (ExecutionException ex) {
                    if (ex.getCause() instanceof BusinessException be) {
                        errorLabel.setText(text(be.getMessage()));
                    } else {
                        log.error("Hoca kaydedilemedi", ex.getCause());
                        errorLabel.setText(text("error.unexpected"));
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /** Yeni hoca formu açar; kaydedilen DTO'yu döner, iptal edilirse null. */
    public InstructorDto showForNew(Frame owner) {
        editing = null;
        init("instructor.form.title.add", owner);
        setVisible(true);
        return saved;
    }

    /** Mevcut hoca için düzenleme formu açar; kaydedilen DTO'yu döner, iptal edilirse null. */
    public InstructorDto showForEdit(Frame owner, InstructorDto instructor) {
        editing = instructor;
        init("instructor.form.title.edit", owner);
        firstNameField.setText(instructor.firstName());
        lastNameField.setText(instructor.lastName());
        setVisible(true);
        return saved;
    }

    private String text(String key) {
        return messages.getMessage(key, null, key, Locale.getDefault());
    }
}
