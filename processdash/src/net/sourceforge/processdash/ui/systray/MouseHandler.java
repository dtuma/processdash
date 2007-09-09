// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.systray;

import java.awt.TrayIcon;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.EventHandler;

import javax.swing.Timer;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.log.time.TimeLoggingModel;

public class MouseHandler extends MouseAdapter {

    private TimeLoggingModel timeLoggingModel;

    private ActionListener showWindowAction;

    private ActionListener playPauseAction;

    private ActionListener changeTaskAction;

    private IconImageHandler imageHandler;

    private Timer singleClickTimer;

    private boolean doubleClickToShowWindow;


    public MouseHandler(ProcessDashboard pdash, TrayIcon icon,
            MenuHandler menuHandler, IconImageHandler imageHandler) {
        this.timeLoggingModel = pdash.getTimeLoggingModel();
        this.showWindowAction = menuHandler.getShowWindowAction();
        this.playPauseAction = menuHandler.getPlayPauseAction();
        this.changeTaskAction = menuHandler.getChangeTaskAction();
        this.imageHandler = imageHandler;

        ActionListener al = EventHandler.create(ActionListener.class, this,
            "singleClickTimerReached");
        this.singleClickTimer = new Timer(500, al);
        this.singleClickTimer.setRepeats(false);
        this.doubleClickToShowWindow = Settings.getBool(
            "systemTray.doubleClickShowsToolbar", true);

        icon.addMouseListener(this);
    }


    @Override
    public void mouseClicked(MouseEvent e) {
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
}
