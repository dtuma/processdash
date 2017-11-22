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

import net.sourceforge.processdash.DashController;

public class MessageHandler implements ActionListener {

    private TrayIcon trayIcon;

    private ActionListener messageClickAction;

    public MessageHandler(TrayIcon trayIcon) {
        this.trayIcon = trayIcon;
    }

    public void displayMessage(String caption, String text,
            TrayIcon.MessageType type, ActionListener action) {
        this.messageClickAction = action;
        if (SystemTrayIconJDK6Impl.isVisible(trayIcon))
            trayIcon.displayMessage(caption, text, type);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (messageClickAction != null)
            messageClickAction.actionPerformed(e);
        else
            DashController.raiseWindow();
    }

}
