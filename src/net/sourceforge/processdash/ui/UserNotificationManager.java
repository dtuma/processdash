// Copyright (C) 2007-2017 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.MenuSelectionManager;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.process.ui.TriggerURI;
import net.sourceforge.processdash.ui.lib.WrappedTextTableCellRenderer;

public class UserNotificationManager {

    private static final UserNotificationManager INSTANCE = new UserNotificationManager();

    public static UserNotificationManager getInstance() {
        return INSTANCE;
    }



    private NotificationList notifications;

    private NotificationsWindow notificationsWindow = null;

    private Timer toFrontTimer;

    private long deferUntil = 0;

    private Window parentWindow;

    private static final long DEFERRAL_PERIOD = 30 * 60 * 1000; // 30 minutes

    private static final Resources resources = Resources
            .getDashBundle("ProcessDashboard.Notifier");


    private UserNotificationManager() {
        notifications = new NotificationList();

        toFrontTimer = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (notificationsWindow != null)
                    notificationsWindow.showWindow();
            }});
        toFrontTimer.setRepeats(false);

        MenuSelectionManager.defaultManager().addChangeListener( //
            new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    maybeShowNotifications(parentWindow);
                }
            });
    }

    public void addNotification(String id, String message, String uri) {
        notifications.add(new Notification(id, message, uri));
        deferUntil = 0;
        maybeShowNotifications(parentWindow);
    }

    public void removeNotification(String id) {
        if (id != null)
            notifications.removeNotification(id);
    }

    /** @since 2.4.1 */
    public List<Notification> getNotifications() {
        return new ArrayList(notifications.notifications);
    }

    /** @since 2.4.1 */
    public void addNotificationListener(TableModelListener l) {
        notifications.addTableModelListener(l);
    }

    /** @since 2.4.1 */
    public void removeNotificationListener(TableModelListener l) {
        notifications.removeTableModelListener(l);
    }

    public void maybeShowNotifications(Window w) {
        parentWindow = w;
        if (w == null || !w.isShowing() || !w.isActive())
            // don't show notifications if the parent window is not active.
            return;

        if (MenuSelectionManager.defaultManager().getSelectedPath().length > 0)
            // don't display the notification window if the user is currently
            // interacting with a menu, since the change of focus would cause
            // their menu context to be lost.
            return;

        if (notifications.isEmpty())
            // there are no notifications to show.
            return;

        if (notificationsWindow != null && notificationsWindow.isShowing())
            // the notifications window is already being displayed to the user.
            // nothing needs to be done.
            return;

        if (System.currentTimeMillis() < deferUntil)
            // the user dismissed the notifications window, and a sufficient
            // period of time has not yet elapsed. Wait until later.
            return;

        if (notificationsWindow == null) {
            // create the notifications window.
            notificationsWindow = new NotificationsWindow();
            notificationsWindow.pack();
        }

        // display the notifications window.
        notificationsWindow.setLocationRelativeTo(w);
        toFrontTimer.restart();
    }

    public void maybeHideNotifications() {
        if (notificationsWindow != null && notificationsWindow.isShowing())
            notificationsWindow.handleDefer();
    }

    public class Notification {
        String id;

        String message;

        String uri;

        private Notification(String id, String message, String uri) {
            if (id == null || message == null)
                throw new NullPointerException("id/message cannot be null");

            this.id = id;
            this.message = message;
            this.uri = uri;
        }

        public String getId() {
            return id;
        }

        public String getMessage() {
            return message;
        }

        public String getUri() {
            return uri;
        }

        public void handle() {
            removeNotification(id);
            if (uri != null)
                TriggerURI.handle(uri);
        }

        public boolean equals(Object obj) {
            if (obj instanceof Notification) {
                Notification that = (Notification) obj;
                return this.id.equals(that.id);
            }
            return false;
        }

        public int hashCode() {
            return id.hashCode();
        }

        public String toString() {
            return message;
        }
    }

    private class NotificationList extends AbstractTableModel {

        private List notifications = Collections
                .synchronizedList(new ArrayList());

        public void add(Notification notification) {
            if (!notifications.contains(notification)) {
                int numRows = notifications.size();
                notifications.add(notification);
                fireTableRowsInserted(numRows, numRows);
            }
        }

        public void removeNotification(String id) {
            for (int row = 0;  row < notifications.size();  row++) {
                Notification n = (Notification) notifications.get(row);
                if (id.equals(n.id)) {
                    notifications.remove(row);
                    fireTableRowsDeleted(row, row);
                    break;
                }
            }
        }

        public void ignore(int row) {
            notifications.remove(row);
            fireTableRowsDeleted(row, row);
        }

        public void handle(int row) {
            Notification n = (Notification) notifications.remove(row);
            fireTableRowsDeleted(row, row);
            n.handle();
        }

        public int getColumnCount() {
            return 2;
        }

        public int getRowCount() {
            return notifications.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0)
                return BULLET;
            else
                return notifications.get(rowIndex);
        }

        public boolean isEmpty() {
            return notifications.isEmpty();
        }

    }

    private static final String BULLET = "\u2022";

    private class NotificationsWindow extends JFrame implements MouseListener,
            ListSelectionListener {
        JTable table;

        JButton ignoreButton, okButton, deferButton;

        public NotificationsWindow() {
            super(resources.getString("Title"));
            JPanel panel = new JPanel(new BorderLayout(5, 5));
            panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            DashboardIconFactory.setWindowIcon(this);
            panel.add(new JLabel(resources.getString("Prompt")),
                    BorderLayout.NORTH);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            table = new JTable(notifications);
            DefaultTableCellRenderer r = new DefaultTableCellRenderer();
            r.setVerticalAlignment(JLabel.TOP);
            table.getColumnModel().getColumn(0).setCellRenderer(r);
            table.getColumnModel().getColumn(0).setMaxWidth(15);
            table.getColumnModel().getColumn(1).setCellRenderer(
                    new WrappedTextTableCellRenderer(table));
            table.setTableHeader(null);
            table.setShowGrid(false);
            table.setIntercellSpacing(new Dimension(0, 8));
            table.addMouseListener(this);
            table.getSelectionModel().addListSelectionListener(this);
            table.setPreferredScrollableViewportSize(new Dimension(300, 200));

            notifications.addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    hideIfEmpty();
                }});

            JScrollPane sp = new JScrollPane(table,
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            panel.add(sp);

            ignoreButton = new JButton(resources.getString("Ignore"));
            ignoreButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    handleIgnore();
                }});

            deferButton = new JButton(resources.getString("Defer"));
            deferButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    handleDefer();
                }});

            okButton = new JButton(resources.getString("OK"));
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    handleOK();
                }});
            setButtonEnablement();

            Box buttons = Box.createHorizontalBox();
            buttons.add(ignoreButton);
            buttons.add(Box.createHorizontalStrut(5));
            buttons.add(deferButton);

            buttons.add(Box.createHorizontalGlue());
            buttons.add(okButton);
            panel.add(buttons, BorderLayout.SOUTH);

            getContentPane().add(panel);
        }

        public void showWindow() {
            if (!notifications.isEmpty() && table.getSelectedRowCount() == 0)
                table.setRowSelectionInterval(0, 0);
            setVisible(true);
            toFront();
        }

        public void handleIgnore() {
            int[] selRows = table.getSelectedRows();
            Arrays.sort(selRows);
            for (int i = selRows.length; i-- > 0;)
                notifications.ignore(selRows[i]);

            hideIfEmpty();
        }

        public void handleOK() {
            int row = table.getSelectedRow();
            if (row != -1)
                notifications.handle(row);

            hideIfEmpty();
        }

        public void handleDefer() {
            dispose();
        }

        public void dispose() {
            super.dispose();
            deferUntil = System.currentTimeMillis() + DEFERRAL_PERIOD;
        }

        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2 && e.getPoint() != null) {
                int row = table.rowAtPoint(e.getPoint());
                if (row != -1) {
                    notifications.handle(row);
                    hideIfEmpty();
                }
            }
        }

        private void hideIfEmpty() {
            if (notifications.isEmpty())
                setVisible(false);
        }

        private void setButtonEnablement() {
            boolean enableButtons = (table.getSelectedRowCount() > 0);
            ignoreButton.setEnabled(enableButtons);
            okButton.setEnabled(enableButtons);
        }

        public void mouseEntered(MouseEvent e) {}

        public void mouseExited(MouseEvent e) {}

        public void mousePressed(MouseEvent e) {}

        public void mouseReleased(MouseEvent e) {}

        public void valueChanged(ListSelectionEvent e) {
            setButtonEnablement();
        }
    }
}
