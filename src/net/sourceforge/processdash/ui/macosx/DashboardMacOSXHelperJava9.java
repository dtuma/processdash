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

package net.sourceforge.processdash.ui.macosx;

import java.awt.Desktop;
import java.beans.EventHandler;
import java.lang.reflect.Method;

import javax.swing.SwingUtilities;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.util.Initializable;

/**
 * This object registers the ProcessDashboard object to handle
 * operating-system-initiated events on Mac OS X, when running under Java 9.
 *
 * Java 9 no longer includes the com.apple.eawt classes, instead providing
 * support through the java.awt.Desktop class. This object uses the latter
 * support; but makes all calls reflectively so this class can be compiled with
 * a Java 8 JDK.
 */
public class DashboardMacOSXHelperJava9
        implements Initializable<ProcessDashboard> {

    ProcessDashboard pdash;

    public DashboardMacOSXHelperJava9() {
        if (!MacGUIUtils.isMacOSX())
            throw new IllegalArgumentException("Not Mac OS X");
        try {
            Class.forName("java.awt.desktop.QuitStrategy");
        } catch (ClassNotFoundException cnfe) {
            throw new IllegalArgumentException("Not Java 9");
        }
    }

    @Override
    public void initialize(ProcessDashboard pdash) {
        try {
            doInit(pdash);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception t) {
            throw new RuntimeException(t);
        }
    }

    private void doInit(ProcessDashboard pdash) throws Exception {
        // retrieve the enum constant for QuitStrategy.CLOSE_ALL_WINDOWS
        Class quitStrategyClz = Class.forName("java.awt.desktop.QuitStrategy");
        Method m = quitStrategyClz.getMethod("valueOf", String.class);
        Object closeAllWindows = m.invoke(null, "CLOSE_ALL_WINDOWS");

        // invoke Desktop.getDesktop().setQuitStrategy(CLOSE_ALL_WINDOWS)
        m = Desktop.class.getMethod("setQuitStrategy", quitStrategyClz);
        m.invoke(Desktop.getDesktop(), closeAllWindows);

        // retrieve the AboutHandler interface, and create an event handler
        Class aboutHandlerClz = Class.forName("java.awt.desktop.AboutHandler");
        Object aboutHandler = EventHandler.create(aboutHandlerClz, this,
            "handleAbout");

        // register with Desktop.getDesktop().setAboutHandler()
        m = Desktop.class.getMethod("setAboutHandler", aboutHandlerClz);
        m.invoke(Desktop.getDesktop(), aboutHandler);

        // after successful initialization, save the ProcessDashboard object
        this.pdash = pdash;
    }

    public void handleAbout() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                pdash.showAboutDialog();
            }
        });
    }

    @Override
    public void dispose() {}

}
