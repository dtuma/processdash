// Copyright (C) 2017 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.systray;

import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.Timer;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.UserNotificationManager;
import net.sourceforge.processdash.ui.UserNotificationManager.Notification;

public class UserNotificationHandler
        implements TableModelListener, ActionListener {

    private static final Resources resources = Resources
            .getDashBundle("ProcessDashboard.SysTray.Notification");


    private MessageHandler message;

    private boolean showDoubleClick;

    private Timer showMessageTimer;

    private UserNotificationManager mgr;

    public UserNotificationHandler(MessageHandler messageHandler) {
        message = messageHandler;
        showDoubleClick = "windows".equals(InternalSettings.getOSPrefix());

        showMessageTimer = new Timer(5000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                displayNotificationMessage();
            }
        });
        showMessageTimer.setRepeats(false);

        mgr = UserNotificationManager.getInstance();
        mgr.addNotificationListener(this);
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        if (e.getType() == TableModelEvent.UPDATE && isEnabled())
            showMessageTimer.restart();
    }

    private boolean isEnabled() {
        return Settings.getBool("userPref.systemTray.showNotifications", true);
    }

    private void displayNotificationMessage() {
        // get the list of pending notifications.
        List<Notification> notifications = mgr.getNotifications();
        if (notifications.isEmpty() || mgr.isNotificationsWindowShowing())
            return;

        // if only one notification is pending, use its message in our popup.
        // Otherwise, display a message about multiple items needing attention.
        String text;
        if (notifications.size() == 1)
            text = notifications.get(0).getMessage();
        else
            text = resources.getString("Multiple");

        // on windows, the user must double-click on the popup for the action
        // to be triggered. Display a suffix advising the user of this.
        if (showDoubleClick)
            text = text + "\n" + resources.getString("Double_Click");

        // display the message, and request that clicks be sent to our
        // actionPerformed() method.
        message.displayMessage(null, text, TrayIcon.MessageType.NONE, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // the user has clicked on our popup message. If the message was about
        // a single notification, handle it. Otherwise, display the window with
        // the list of pending notifications.
        List<Notification> notifications = mgr.getNotifications();
        if (notifications.size() == 1) {
            notifications.get(0).handle();
        } else {
            mgr.maybeShowNotifications();
        }
    }

}
