// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.macosx;

import javax.swing.SwingUtilities;

import net.sourceforge.processdash.tool.quicklauncher.QuickLauncher;
import net.sourceforge.processdash.util.Initializable;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationEvent;
import com.apple.eawt.ApplicationListener;

public class LauncherMacOSXHelper implements Initializable<QuickLauncher>,
        ApplicationListener {

    QuickLauncher launcher;

    public LauncherMacOSXHelper() {
        String osName = System.getProperty("os.name");
        if (!"Mac OS X".equalsIgnoreCase(osName))
            throw new IllegalArgumentException("Not Mac OS X");
    }

    public void initialize(QuickLauncher launcher) {
        this.launcher = launcher;
        Application.getApplication().addApplicationListener(this);
    }

    public void dispose() {
        Application.getApplication().removeApplicationListener(this);
    }

    public void handleOpenFile(ApplicationEvent e) {
        final String filename = e.getFilename();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                launcher.launchFilename(filename);
            }});
    }

    public void handleQuit(ApplicationEvent e) {
        launcher.quit();
    }

    public void handleAbout(ApplicationEvent e) {}
    public void handleOpenApplication(ApplicationEvent e) {}
    public void handlePreferences(ApplicationEvent e) {}
    public void handlePrintFile(ApplicationEvent e) {}
    public void handleReOpenApplication(ApplicationEvent e) {}

}
