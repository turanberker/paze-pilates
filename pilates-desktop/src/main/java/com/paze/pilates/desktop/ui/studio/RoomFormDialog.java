package com.paze.pilates.desktop.ui.studio;

import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.RoomDto;
import com.paze.pilates.service.StudioService;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RoomFormDialog extends JDialog {

    private final StudioService studioService;
    private final MessageSource messages;

    private JTextField nameField;
    private JSpinner capacitySpinner;
    private JLabel errorLabel;

    private RoomDto saved;
    private RoomDto editing;

    public RoomFormDialog(StudioService studioService, MessageSource messages) {
        this.studioService = studioService;
        this.messages = messages;
    }

    private void init(String titleKey, java.awt.Frame owner) {
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
        form.add(new JLabel(text("room.form.label.name")), c);

        nameField = new JTextField(20);
        c.gridy = 1;
        c.insets = new Insets(0, 0, 14, 0);
        form.add(nameField, c);

        c.gridy = 2;
        c.insets = new Insets(0, 0, 4, 0);
        form.add(new JLabel(text("room.form.label.capacity")), c);

        capacitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        ((JSpinner.DefaultEditor) capacitySpinner.getEditor()).getTextField().setColumns(6);
        c.gridy = 3;
        c.insets = new Insets(0, 0, 14, 0);
        form.add(capacitySpinner, c);

        errorLabel = new JLabel(" ");
        errorLabel.setForeground(java.awt.Color.decode("#DC2626"));
        c.gridy = 4;
        c.insets = new Insets(0, 0, 0, 0);
        form.add(errorLabel, c);

        add(form, BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(320, getHeight()));
        setLocationRelativeTo(owner);
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));

        JButton cancelBtn = new JButton(text("room.form.button.cancel"));
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = new JButton(text("room.form.button.save"));
        saveBtn.putClientProperty("JButton.buttonType", "default");
        saveBtn.addActionListener(e -> onSave());

        panel.add(cancelBtn);
        panel.add(saveBtn);
        return panel;
    }

    private void onSave() {
        String name = nameField.getText().trim();
        int capacity = (int) capacitySpinner.getValue();
        Long id = (editing != null) ? editing.id() : null;

        try {
            saved = studioService.saveRoom(new RoomDto(id, name, capacity));
            dispose();
        } catch (BusinessException ex) {
            errorLabel.setText(text(ex.getMessage()));
        } catch (Exception ex) {
            errorLabel.setText(ex.getMessage());
        }
    }

    /** Yeni oda formu açar; kaydedilen DTO'yu döner, iptal edilirse null. */
    public RoomDto showForNew(Frame owner) {
        editing = null;
        init("room.form.title.add", owner);
        setVisible(true);
        return saved;
    }

    /** Mevcut oda için düzenleme formu açar; kaydedilen DTO'yu döner, iptal edilirse null. */
    public RoomDto showForEdit(Frame owner, RoomDto room) {
        editing = room;
        init("room.form.title.edit", owner);
        nameField.setText(room.name());
        capacitySpinner.setValue(room.capacity());
        setVisible(true);
        return saved;
    }

    private String text(String key) {
        return messages.getMessage(key, null, key, Locale.getDefault());
    }
}
