// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.macosx;

import javax.swing.SwingUtilities;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.util.Initializable;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationEvent;
import com.apple.eawt.ApplicationListener;

public class DashboardMacOSXHelper implements Initializable<ProcessDashboard>,
        ApplicationListener {

    ProcessDashboard pdash;

    public DashboardMacOSXHelper() {
        if (!MacGUIUtils.isMacOSX())
            throw new IllegalArgumentException("Not Mac OS X");
    }

    public void initialize(ProcessDashboard pdash) {
        this.pdash = pdash;
        Application.getApplication().addApplicationListener(this);
    }

    public void dispose() {
        Application.getApplication().removeApplicationListener(this);
    }

    public void handleAbout(ApplicationEvent e) {
        e.setHandled(true);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                pdash.showAboutDialog();
            }});
    }

    public void handleQuit(ApplicationEvent e) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                pdash.exitProgram();
            }});
    }

    public void handleOpenApplication(ApplicationEvent e) {}
    public void handleOpenFile(ApplicationEvent e) {}
    public void handlePreferences(ApplicationEvent e) {}
    public void handlePrintFile(ApplicationEvent e) {}
    public void handleReOpenApplication(ApplicationEvent e) {}

}
