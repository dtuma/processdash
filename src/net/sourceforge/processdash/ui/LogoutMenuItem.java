// Copyright (C) 2025 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.bridge.client.BridgedWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.impl.HttpAuthenticator;

public class LogoutMenuItem extends JMenuItem
        implements ActionListener, Runnable {

    private ProcessDashboard parent;

    private static Resources resources = ConfigureButton.resources;

    public LogoutMenuItem(ProcessDashboard parent) {
        super(resources.getString("Logout.Title"));
        setToolTipText(resources.getString("Logout.Tooltip"));

        if (parent.getWorkingDirectory() instanceof BridgedWorkingDirectory) {
            this.parent = parent;
            addActionListener(this);
        } else {
            setVisible(false);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String title = resources.getString("Logout.Title");
        String message = resources.getString("Logout.Confirm");
        int userChoice = JOptionPane.showConfirmDialog(parent, message, title,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (userChoice == JOptionPane.OK_OPTION)
            parent.exitProgram(this);
    }

    @Override
    public void run() {
        String url = parent.getWorkingDirectory().getDescription();
        HttpAuthenticator.logout(url);
    }

}
