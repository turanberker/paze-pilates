package com.paze.pilates.desktop.ui.reservation;

import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.InstructorDto;
import com.paze.pilates.dto.LessonDto;
import com.paze.pilates.dto.RoomDto;
import com.paze.pilates.service.LessonService;
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
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
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
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingWorker;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;

/** Takvimden seçilen gün/saat için ders açma formu. */
@org.springframework.stereotype.Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class LessonFormDialog extends JDialog {

    /** Başlangıç saati seçildiğinde bitiş için önerilen süre (dakika). */
    private static final int DEFAULT_DURATION_MINUTES = 60;

    private final LessonService lessonService;
    private final MessageSource messages;

    private JComboBox<InstructorDto> instructorCombo;
    private JComboBox<RoomDto> roomCombo;
    private JSpinner dateSpinner;
    private JSpinner startSpinner;
    private JSpinner endSpinner;
    private JButton saveBtn;
    private JLabel errorLabel;

    private LessonDto saved;

    public LessonFormDialog(
            LessonService lessonService,
            MessageSource messages) {
        this.lessonService = lessonService;
        this.messages = messages;
    }

    private void init(Frame owner, LocalDate date, LocalTime start) {
        setTitle(text("lesson.form.title"));
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
        form.add(new JLabel(text("lesson.form.label.instructor") + " *"), lbl);
        instructorCombo = new JComboBox<>();
        instructorCombo.setRenderer(new LabelRenderer());
        fld.gridy = row++;
        form.add(instructorCombo, fld);

        lbl.gridy = row++;
        form.add(new JLabel(text("lesson.form.label.room") + " *"), lbl);
        roomCombo = new JComboBox<>();
        roomCombo.setRenderer(new LabelRenderer());
        fld.gridy = row++;
        form.add(roomCombo, fld);

        lbl.gridy = row++;
        form.add(new JLabel(text("lesson.form.label.date") + " *"), lbl);
        dateSpinner = new JSpinner(new SpinnerDateModel(toDate(date, LocalTime.MIDNIGHT), null, null,
                java.util.Calendar.DAY_OF_MONTH));
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd.MM.yyyy"));
        fld.gridy = row++;
        form.add(dateSpinner, fld);

        lbl.gridy = row++;
        form.add(new JLabel(text("lesson.form.label.start") + " *"), lbl);
        startSpinner = timeSpinner(start);
        fld.gridy = row++;
        form.add(startSpinner, fld);

        lbl.gridy = row++;
        form.add(new JLabel(text("lesson.form.label.end") + " *"), lbl);
        endSpinner = timeSpinner(start.plusMinutes(DEFAULT_DURATION_MINUTES));
        fld.gridy = row++;
        form.add(endSpinner, fld);

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

    private static JSpinner timeSpinner(LocalTime time) {
        JSpinner spinner = new JSpinner(new SpinnerDateModel(toDate(LocalDate.now(), time), null, null,
                java.util.Calendar.MINUTE));
        spinner.setEditor(new JSpinner.DateEditor(spinner, "HH:mm"));
        return spinner;
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));

        JButton cancelBtn = new JButton(text("lesson.form.button.cancel"));
        cancelBtn.addActionListener(e -> dispose());

        saveBtn = new JButton(text("lesson.form.button.save"));
        saveBtn.putClientProperty("JButton.buttonType", "default");
        saveBtn.setEnabled(false);
        saveBtn.addActionListener(e -> onSave());

        panel.add(cancelBtn);
        panel.add(saveBtn);
        return panel;
    }

    /** Aktif hoca ve oda listelerini arka planda yükler. */
    private void loadOptions() {
        new SwingWorker<Options, Void>() {
            @Override protected Options doInBackground() {
                return new Options(lessonService.findInstructorsForLessons(), lessonService.findRoomsForLessons());
            }
            @Override protected void done() {
                try {
                    Options o = get();
                    o.instructors().forEach(instructorCombo::addItem);
                    o.rooms().forEach(roomCombo::addItem);
                    if (o.instructors().isEmpty()) {
                        errorLabel.setText(text("lesson.form.no_instructor"));
                    } else if (o.rooms().isEmpty()) {
                        errorLabel.setText(text("lesson.form.no_room"));
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

    private void onSave() {
        InstructorDto instructor = (InstructorDto) instructorCombo.getSelectedItem();
        RoomDto room = (RoomDto) roomCombo.getSelectedItem();
        Long instructorId = instructor != null ? instructor.id() : null;
        Long roomId = room != null ? room.id() : null;
        LocalDate date = toLocalDate((Date) dateSpinner.getValue());
        LocalTime start = toLocalTime((Date) startSpinner.getValue());
        LocalTime end = toLocalTime((Date) endSpinner.getValue());

        saveBtn.setEnabled(false);
        errorLabel.setText(" ");
        new SwingWorker<LessonDto, Void>() {
            @Override protected LessonDto doInBackground() {
                return lessonService.openLesson(instructorId, roomId, date, start, end);
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

    /** Formu açar; açılan dersi döner, iptal edilirse null. */
    public LessonDto showDialog(Frame owner, LocalDate date, LocalTime start) {
        init(owner, date, start);
        loadOptions();
        setVisible(true);
        return saved;
    }

    private static Date toDate(LocalDate date, LocalTime time) {
        return Date.from(date.atTime(time).atZone(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate toLocalDate(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static LocalTime toLocalTime(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0);
    }

    private String text(String key) {
        return messages.getMessage(key, null, key, Locale.getDefault());
    }

    private record Options(List<InstructorDto> instructors, List<RoomDto> rooms) {
    }

    private static class LabelRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof InstructorDto i) {
                setText(i.firstName() + " " + i.lastName());
            } else if (value instanceof RoomDto r) {
                setText(r.name());
            }
            return this;
        }
    }
}
