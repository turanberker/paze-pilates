package com.paze.pilates.desktop.ui.studio;

import com.paze.pilates.dto.RoomDto;
import com.paze.pilates.service.StudioService;
import com.paze.pilates.desktop.ui.shared.NotificationBar;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class StudioPanel extends JPanel {

    private final StudioService studioService;
    private final NotificationBar notificationBar;
    private final ObjectProvider<RoomFormDialog> dialogProvider;
    private final MessageSource messages;

    private RoomTableModel roomTableModel;
    private JTable roomTable;
    private JButton editRoomBtn;
    private JButton deleteRoomBtn;

    public StudioPanel(
            StudioService studioService,
            NotificationBar notificationBar,
            ObjectProvider<RoomFormDialog> dialogProvider,
            MessageSource messages) {
        this.studioService = studioService;
        this.notificationBar = notificationBar;
        this.dialogProvider = dialogProvider;
        this.messages = messages;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        add(buildHeaderBar(), BorderLayout.NORTH);
        add(buildTable(),     BorderLayout.CENTER);
        add(buildActionBar(), BorderLayout.SOUTH);

        loadRooms();
    }

    private JPanel buildHeaderBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);

        JLabel title = new JLabel(text("studio.section.rooms"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setForeground(new Color(30, 41, 59));
        bar.add(title, BorderLayout.WEST);

        JButton addBtn = new JButton(text("studio.room.button.add"));
        addBtn.putClientProperty("JButton.buttonType", "default");
        addBtn.addActionListener(e -> onAddRoom());
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        wrap.setOpaque(false);
        wrap.add(addBtn);
        bar.add(wrap, BorderLayout.EAST);

        return bar;
    }

    private JScrollPane buildTable() {
        roomTableModel = new RoomTableModel();
        roomTable = new JTable(roomTableModel);
        roomTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomTable.setRowHeight(32);
        roomTable.getTableHeader().setReorderingAllowed(false);
        roomTable.setFillsViewportHeight(true);
        roomTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        roomTable.getColumnModel().getColumn(1).setMaxWidth(140);

        roomTable.getSelectionModel().addListSelectionListener(
                e -> { if (!e.getValueIsAdjusting()) updateButtons(); });
        roomTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) onEditRoom();
            }
        });

        JScrollPane scroll = new JScrollPane(roomTable);
        scroll.setPreferredSize(new Dimension(0, 300));
        return scroll;
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bar.setOpaque(false);

        editRoomBtn = new JButton(text("studio.room.button.edit"));
        editRoomBtn.setEnabled(false);
        editRoomBtn.addActionListener(e -> onEditRoom());

        deleteRoomBtn = new JButton(text("studio.room.button.delete"));
        deleteRoomBtn.setEnabled(false);
        deleteRoomBtn.addActionListener(e -> onDeleteRoom());

        bar.add(editRoomBtn);
        bar.add(deleteRoomBtn);
        return bar;
    }

    private void updateButtons() {
        boolean sel = roomTable.getSelectedRow() >= 0;
        editRoomBtn.setEnabled(sel);
        deleteRoomBtn.setEnabled(sel);
    }

    private void onAddRoom() {
        RoomDto result = dialogProvider.getObject().showForNew(parentFrame());
        if (result != null) {
            roomTableModel.addRoom(result);
            notificationBar.showSuccess(text("studio.room.save.success"));
        }
    }

    private void onEditRoom() {
        int row = roomTable.getSelectedRow();
        if (row < 0) return;
        RoomDto result = dialogProvider.getObject().showForEdit(parentFrame(), roomTableModel.getRoom(row));
        if (result != null) {
            roomTableModel.updateRoom(row, result);
            notificationBar.showSuccess(text("studio.room.save.success"));
        }
    }

    private void onDeleteRoom() {
        int row = roomTable.getSelectedRow();
        if (row < 0) return;
        int confirm = JOptionPane.showConfirmDialog(
                this,
                text("studio.room.delete.confirm"),
                text("studio.room.button.delete"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            studioService.deleteRoom(roomTableModel.getRoom(row).id());
            roomTableModel.removeRoom(row);
            notificationBar.showSuccess(text("studio.room.delete.success"));
        }
    }

    private void loadRooms() {
        new SwingWorker<List<RoomDto>, Void>() {
            @Override protected List<RoomDto> doInBackground() {
                return studioService.getRooms();
            }
            @Override protected void done() {
                try {
                    roomTableModel.setRooms(get());
                } catch (InterruptedException | ExecutionException ex) {
                    notificationBar.showError(ex.getMessage());
                }
            }
        }.execute();
    }

    private Frame parentFrame() {
        return (Frame) javax.swing.SwingUtilities.getWindowAncestor(this);
    }

    private String text(String key) {
        return messages.getMessage(key, null, key, Locale.getDefault());
    }

    // ── Table model ───────────────────────────────────────────────────────

    private class RoomTableModel extends AbstractTableModel {

        private final String[] columns = {
            text("studio.room.col.name"),
            text("studio.room.col.capacity")
        };
        private final List<RoomDto> rows = new ArrayList<>();

        @Override public int getRowCount()    { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int col) { return columns[col]; }

        @Override
        public Object getValueAt(int row, int col) {
            RoomDto r = rows.get(row);
            return switch (col) {
                case 0 -> r.name();
                case 1 -> r.capacity();
                default -> null;
            };
        }

        void setRooms(List<RoomDto> list) {
            rows.clear();
            rows.addAll(list);
            fireTableDataChanged();
        }

        void addRoom(RoomDto room) {
            rows.add(room);
            fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
        }

        void updateRoom(int row, RoomDto room) {
            rows.set(row, room);
            fireTableRowsUpdated(row, row);
        }

        void removeRoom(int row) {
            rows.remove(row);
            fireTableRowsDeleted(row, row);
        }

        RoomDto getRoom(int row) { return rows.get(row); }
    }
}
