package com.paze.pilates.desktop.ui.reservation;

import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.LessonDto;
import com.paze.pilates.dto.LessonParticipantDto;
import com.paze.pilates.dto.StudentDto;
import com.paze.pilates.service.LessonService;
import com.paze.pilates.service.StudentService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;

/** Seçilen derse öğrenci ekleme formu. */
@org.springframework.stereotype.Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class LessonStudentAddDialog extends JDialog {

    private final LessonService lessonService;
    private final StudentService studentService;
    private final MessageSource messages;

    private JComboBox<StudentDto> studentCombo;
    private JButton saveBtn;
    private JLabel errorLabel;

    private LessonParticipantDto saved;

    public LessonStudentAddDialog(LessonService lessonService, StudentService studentService,
                                  MessageSource messages) {
        this.lessonService = lessonService;
        this.studentService = studentService;
        this.messages = messages;
    }

    /** Formu açar; eklenen katılımcıyı döner, iptal edilirse null. */
    public LessonParticipantDto showDialog(Frame owner, LessonDto lesson) {
        setTitle(text("lesson.add_student.title"));
        setModal(true);
        setResizable(false);
        setLayout(new BorderLayout(0, 8));

        JPanel form = new JPanel(new BorderLayout(0, 4));
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 4, 24));
        form.add(new JLabel(text("lesson.add_student.label.student") + " *"), BorderLayout.NORTH);
        studentCombo = new JComboBox<>();
        studentCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof StudentDto s) {
                    setText(s.firstName() + " " + s.lastName());
                }
                return this;
            }
        });
        form.add(studentCombo, BorderLayout.CENTER);
        errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.decode("#DC2626"));
        form.add(errorLabel, BorderLayout.SOUTH);
        add(form, BorderLayout.CENTER);

        JButton cancelBtn = new JButton(text("lesson.add_student.button.cancel"));
        cancelBtn.addActionListener(e -> dispose());
        saveBtn = new JButton(text("lesson.add_student.button.save"));
        saveBtn.putClientProperty("JButton.buttonType", "default");
        saveBtn.setEnabled(false);
        saveBtn.addActionListener(e -> onSave(lesson));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        buttons.add(cancelBtn);
        buttons.add(saveBtn);
        add(buttons, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
        loadStudents();
        setVisible(true);
        return saved;
    }

    private void loadStudents() {
        new SwingWorker<List<StudentDto>, Void>() {
            @Override protected List<StudentDto> doInBackground() {
                return studentService.findAll();
            }
            @Override protected void done() {
                try {
                    List<StudentDto> students = get();
                    students.forEach(studentCombo::addItem);
                    if (students.isEmpty()) {
                        errorLabel.setText(text("lesson.add_student.no_student"));
                    } else {
                        saveBtn.setEnabled(true);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    errorLabel.setText(text("error.unexpected"));
                }
            }
        }.execute();
    }

    private void onSave(LessonDto lesson) {
        StudentDto student = (StudentDto) studentCombo.getSelectedItem();
        Long studentId = student != null ? student.id() : null;
        saveBtn.setEnabled(false);
        errorLabel.setText(" ");
        new SwingWorker<LessonParticipantDto, Void>() {
            @Override protected LessonParticipantDto doInBackground() {
                return lessonService.addParticipant(lesson.id(), studentId);
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

    private String text(String key) {
        return messages.getMessage(key, null, key, Locale.getDefault());
    }
}
