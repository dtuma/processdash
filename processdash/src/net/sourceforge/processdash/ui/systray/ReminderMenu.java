// Copyright (C) 2007-2008 Tuma Solutions, LLC
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

import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.beans.PropertyChangeListener;

import javax.swing.JOptionPane;

import net.sourceforge.processdash.i18n.Resources;

/**
 * The menu that controls the behavior of the dashboard tray icon reminder
 * 
 * @author Ali El Gamal<alielgamal@gmail.com>
 * 
 */
public class ReminderMenu extends Menu {

    private static Resources resources = Resources
            .getDashBundle("ProcessDashboard.SysTray.Menu.Reminder");

    public ReminderMenu(Reminder reminder) {
        setLabel(resources.getString("Reminder"));
        add(new EnableDisableReminderMenuItem(reminder));
        add(new ReminderTimoutMenuItem(reminder));
    }

    public class EnableDisableReminderMenuItem extends MenuItem {
        Reminder reminder;

        public EnableDisableReminderMenuItem(Reminder reminder) {
            this.reminder = reminder;

            PropertyChangeListener pcl = EventHandler.create(
                    PropertyChangeListener.class, this, "update");
            reminder.addPropertyChangeListener(pcl);

            ActionListener al = EventHandler.create(ActionListener.class, this,
                    "action");
            addActionListener(al);

            update();
        }

        public void update() {
            String display;
            if (reminder.isDisabled()) {
                display = resources.getString("Enable_Reminder");
            } else {
                display = resources.getString("Disable_Reminder");
            }

            setLabel(display);
        }

        public void action() {
            boolean disable = !reminder.isDisabled();
            reminder.setDisabled(disable);
        }
    }

    public class ReminderTimoutMenuItem extends MenuItem {
        Reminder reminder;

        public ReminderTimoutMenuItem(Reminder reminder) {
            this.reminder = reminder;

            ActionListener ac = EventHandler.create(ActionListener.class, this,
                    "action");
            this.addActionListener(ac);

            setLabel(resources.getString("Timeout"));
        }

        public void action() {
            String display = resources.getString("Change_Timeout");
            String errorDisplay = resources.getString("Invalid_Timeout");
            while (true) {
                String timeoutStr = JOptionPane.showInputDialog(display,
                        reminder.getTimeout() + "");
                display = errorDisplay;
                if (timeoutStr == null) {
                    return;
                }
                int timeout = 0;
                try {
                    timeout = Integer.parseInt(timeoutStr);
                } catch (Exception e) {

                }
                if (timeout > 0) {
                    reminder.setTimeout(timeout);
                    return;
                }
            }
        }

    }
}
