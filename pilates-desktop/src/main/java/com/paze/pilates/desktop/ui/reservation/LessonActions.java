package com.paze.pilates.desktop.ui.reservation;

import com.paze.pilates.desktop.ui.shared.NotificationBar;
import com.paze.pilates.dto.AppUserDto;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.LessonDto;
import com.paze.pilates.dto.Permission;
import com.paze.pilates.service.AuthService;
import com.paze.pilates.service.LessonService;
import java.awt.Component;
import java.awt.Frame;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.MessageSource;

/**
 * Bir ders üzerinde yapılabilen işlemler (katılımcılar, öğrenci ekleme, tamamlama, geri alma, ders açma).
 * Takvim ve ana ekrandaki bugünkü dersler bileşeni aynı işlemleri buradan kullanır.
 * Görünürlük kuralları yalnızca sunumdur; asıl yetki kontrolü serviste.
 */
@org.springframework.stereotype.Component
public class LessonActions {

    private final LessonService lessonService;
    private final AuthService authService;
    private final NotificationBar notificationBar;
    private final ObjectProvider<LessonFormDialog> lessonDialogProvider;
    private final ObjectProvider<LessonParticipantsDialog> participantsDialogProvider;
    private final ObjectProvider<LessonStudentAddDialog> studentAddDialogProvider;
    private final ObjectProvider<LessonCompleteDialog> completeDialogProvider;
    private final MessageSource messages;

    public LessonActions(LessonService lessonService,
                         AuthService authService,
                         NotificationBar notificationBar,
                         ObjectProvider<LessonFormDialog> lessonDialogProvider,
                         ObjectProvider<LessonParticipantsDialog> participantsDialogProvider,
                         ObjectProvider<LessonStudentAddDialog> studentAddDialogProvider,
                         ObjectProvider<LessonCompleteDialog> completeDialogProvider,
                         MessageSource messages) {
        this.lessonService = lessonService;
        this.authService = authService;
        this.notificationBar = notificationBar;
        this.lessonDialogProvider = lessonDialogProvider;
        this.participantsDialogProvider = participantsDialogProvider;
        this.studentAddDialogProvider = studentAddDialogProvider;
        this.completeDialogProvider = completeDialogProvider;
        this.messages = messages;
    }

    // ── Görünürlük ────────────────────────────────────────────────────────────

    public boolean canOpenLessons() {
        return authService.getCurrentUser().map(AppUserDto::canOpenLessons).orElse(false);
    }

    public boolean canManage(LessonDto lesson) {
        return authService.getCurrentUser().map(u -> u.canManageLessonOf(lesson.instructorId())).orElse(false);
    }

    public boolean canAddStudent(LessonDto lesson) {
        return lesson != null && canManage(lesson) && !lesson.completed();
    }

    public boolean canComplete(LessonDto lesson) {
        return lesson != null && canManage(lesson) && !lesson.completed();
    }

    public boolean canUndoCompletion(LessonDto lesson) {
        return lesson != null && lesson.completed()
                && authService.getCurrentUser().map(u -> u.has(Permission.LESSON_MANAGE)).orElse(false);
    }

    // ── İşlemler: değişiklik olursa onChanged çalışır ──────────────────────────

    public void showParticipants(Component parent, LessonDto lesson, Runnable onChanged) {
        if (participantsDialogProvider.getObject().showDialog(frameOf(parent), lesson)) {
            onChanged.run();
        }
    }

    public void addStudent(Component parent, LessonDto lesson, Runnable onChanged) {
        if (studentAddDialogProvider.getObject().showDialog(frameOf(parent), lesson) != null) {
            notificationBar.showSuccess(text("calendar.student.added"));
            onChanged.run();
        }
    }

    public void complete(Component parent, LessonDto lesson, Runnable onChanged) {
        if (completeDialogProvider.getObject().showDialog(frameOf(parent), lesson) != null) {
            notificationBar.showSuccess(text("calendar.lesson.completed"));
            onChanged.run();
        }
    }

    public void undoCompletion(Component parent, LessonDto lesson, Runnable onChanged) {
        int answer = JOptionPane.showConfirmDialog(parent, text("calendar.undo_completion.confirm"),
                text("calendar.menu.undo_completion"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        new SwingWorker<LessonDto, Void>() {
            @Override protected LessonDto doInBackground() {
                return lessonService.undoCompletion(lesson.id());
            }
            @Override protected void done() {
                try {
                    get();
                    notificationBar.showSuccess(text("calendar.lesson.completion_undone"));
                    onChanged.run();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    notificationBar.showError(ex.getCause() instanceof BusinessException be
                            ? text(be.getMessage()) : text("error.unexpected"));
                }
            }
        }.execute();
    }

    /** Ders açma formunu açar; açılan dersi döner, iptal edilirse null. */
    public LessonDto openLesson(Component parent, LocalDate date, LocalTime start) {
        LessonDto created = lessonDialogProvider.getObject().showDialog(frameOf(parent), date, start);
        if (created != null) {
            notificationBar.showSuccess(text("calendar.lesson.created"));
        }
        return created;
    }

    /** Derse ait, kullanıcının yapabildiği işlemlerin menü öğeleri (sağ tık menüsü için). */
    public List<JMenuItem> menuItems(Component parent, LessonDto lesson, Runnable onChanged) {
        List<JMenuItem> items = new ArrayList<>();
        items.add(item("calendar.menu.show_participants", () -> showParticipants(parent, lesson, onChanged)));
        if (canAddStudent(lesson)) {
            items.add(item("calendar.menu.add_student", () -> addStudent(parent, lesson, onChanged)));
        }
        if (canComplete(lesson)) {
            items.add(item("calendar.menu.complete_lesson", () -> complete(parent, lesson, onChanged)));
        }
        if (canUndoCompletion(lesson)) {
            items.add(item("calendar.menu.undo_completion", () -> undoCompletion(parent, lesson, onChanged)));
        }
        return items;
    }

    private JMenuItem item(String key, Runnable action) {
        JMenuItem item = new JMenuItem(text(key));
        item.addActionListener(e -> action.run());
        return item;
    }

    private static Frame frameOf(Component c) {
        return c instanceof Frame f ? f : (Frame) SwingUtilities.getWindowAncestor(c);
    }

    private String text(String key) {
        return messages.getMessage(key, null, key, Locale.getDefault());
    }
}
