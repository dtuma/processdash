// Copyright (C) 2007-2013 Tuma Solutions, LLC
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.EventHandler;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.log.time.TimeLoggingModel;

public class MouseHandler extends MouseAdapter {

    private TimeLoggingModel timeLoggingModel;

    private ActionListener showWindowAction;

    private ActionListener playPauseAction;

    private ActionListener changeTaskAction;

    private ActionListener popupMessageClickAction;

    private IconImageHandler imageHandler;

    private ActionEventHandler actionEventHandler;

    private Timer singleClickTimer;

    private boolean doubleClickToShowWindow;


    public MouseHandler(ProcessDashboard pdash, TrayIcon icon,
            MenuHandler menuHandler, IconImageHandler imageHandler) {
        this.timeLoggingModel = pdash.getTimeLoggingModel();
        this.showWindowAction = menuHandler.getShowWindowAction();
        this.playPauseAction = menuHandler.getPlayPauseAction();
        this.changeTaskAction = menuHandler.getChangeTaskAction();
        this.imageHandler = imageHandler;
        this.actionEventHandler = new ActionEventHandler();

        ActionListener al = EventHandler.create(ActionListener.class, this,
            "singleClickTimerReached");
        this.singleClickTimer = new Timer(500, al);
        this.singleClickTimer.setRepeats(false);
        this.doubleClickToShowWindow = Settings.getBool(
            "systemTray.doubleClickShowsToolbar", true);

        icon.addMouseListener(this);
        icon.addActionListener(actionEventHandler);

        // in the future, we may introduce more sophisticated support, allowing
        // different actions to be triggered based on the content of the
        // popup message that the user clicked on.  For now though, keep
        // things simple and show the dashboard window in response to clicks
        // on the popup message.
        this.popupMessageClickAction = this.showWindowAction;
    }


    @Override
    public void mouseClicked(MouseEvent e) {
        // let the action event handler know that we just saw a click on the
        // tray icon itself.
        actionEventHandler.sawClickOnIcon(e);

        // we are only interested in left-clicks.  Ignore all others.
        if (e.getButton() != MouseEvent.BUTTON1)
            return;

        if (e.getClickCount() > 1) {
            // handle double click events
            singleClickTimer.stop();
            imageHandler.update();
            if (doubleClickToShowWindow && e.getClickCount() == 2)
                showWindowAction.actionPerformed(null);

        } else if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) {
            // Ctrl-click should open the change task dialog
            if (changeTaskAction != null)
                changeTaskAction.actionPerformed(null);

        } else if (timeLoggingModel.isLoggingAllowed()) {
            // a single left-click was detected
            if (doubleClickToShowWindow) {
                // if we're handling double-clicks, we must set the timer to
                // see if a double click arrives later.  Show the user some
                // immediate visual feedback so they know that their click
                // registered.
                imageHandler.showArmedIcon();
                singleClickTimer.restart();
            } else {
                // if we aren't handling double-clicks, we can just respond
                // to the single click immediately.
                playPauseAction.actionPerformed(null);
            }
        }
    }

    public void singleClickTimerReached() {
        playPauseAction.actionPerformed(null);
        imageHandler.update();
    }

    /**
     * This class helps to overcome cross-platform inconsistencies in the
     * handling of tray icon action events.
     * 
     * When you register an ActionListener on a TrayIcon, that listener gets
     * called at various times:
     * 
     * <ol>
     * <li>When the user double-clicks the icon on Windows operating system</li>
     * <li>When the user single-clicks the icon on Linux/Gnome</li>
     * <li>When the user clicks on a popup message displayed by the icon (using
     *     the TrayIcon.displayMessage method)</li>
     * </ol>
     * 
     * We have a desire to respond to the scenario #3.  But as soon as we
     * register an action listener, it will get called in the other cases too.
     * Unfortunately, scenario #2 will interfere with our desire to start/stop
     * the dashboard timer.
     * 
     * So this class acts as an action listener, and possibly forwards action
     * events on to the popupMessageClickAction - but only if no clicks have
     * been seen recently on the icon itself.
     *
     * @author Tuma
     */
    private class ActionEventHandler implements ActionListener, Runnable {

        private ActionEvent eventReceived;

        private long lastIconClickTime;

        public void actionPerformed(ActionEvent e) {
            eventReceived = e;
            SwingUtilities.invokeLater(this);
        }

        public void sawClickOnIcon(MouseEvent e) {
            lastIconClickTime = System.currentTimeMillis();
        }

        public void run() {
            long now = System.currentTimeMillis();
            long lastIconClickAge = now - lastIconClickTime;
            if (lastIconClickAge > 1000
                    && eventReceived != null
                    && popupMessageClickAction != null)
                popupMessageClickAction.actionPerformed(eventReceived);

            eventReceived = null;
        }
    }

}
