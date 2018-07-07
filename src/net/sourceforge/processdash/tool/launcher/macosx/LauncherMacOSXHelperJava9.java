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

package net.sourceforge.processdash.tool.launcher.macosx;

import java.awt.Desktop;
import java.beans.EventHandler;
import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import net.sourceforge.processdash.ui.macosx.MacGUIUtils;

/**
 * This object registers the Launcher object to handle
 * operating-system-initiated events on Mac OS X, when running under Java 9.
 *
 * Java 9 no longer includes the com.apple.eawt classes, instead providing
 * support through the java.awt.Desktop class. This object uses the latter
 * support; but makes all calls reflectively so this class can be compiled with
 * a Java 8 JDK.
 */
public class LauncherMacOSXHelperJava9 implements Runnable {

    private Method getFilesMethod;

    public LauncherMacOSXHelperJava9() {
        if (!MacGUIUtils.isMacOSX())
            throw new IllegalArgumentException("Not Mac OS X");
        try {
            Class.forName("java.awt.desktop.OpenFilesHandler");
        } catch (ClassNotFoundException cnfe) {
            throw new IllegalArgumentException("Not Java 9");
        }
    }

    @Override
    public void run() {
        try {
            doInit();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception t) {
            throw new RuntimeException(t);
        }
    }

    private void doInit() throws Exception {
        // Look up methods we will need for event handling logic
        Class filesEventClz = Class.forName("java.awt.desktop.FilesEvent");
        getFilesMethod = filesEventClz.getMethod("getFiles");

        // retrieve the OpenFilesHandler interface, and create an event handler
        Class handlerClz = Class.forName("java.awt.desktop.OpenFilesHandler");
        Object openFilesHandler = EventHandler.create(handlerClz, this,
            "openFiles", "");

        // register with Desktop.getDesktop().setOpenFileHandler()
        Method m = Desktop.class.getMethod("setOpenFileHandler", handlerClz);
        m.invoke(Desktop.getDesktop(), openFilesHandler);
    }

    public void openFiles(Object event) {
        try {
            List<File> files = (List<File>) getFilesMethod.invoke(event);
            String[] filenames = new String[files.size()];
            for (int i = files.size(); i-- > 0;)
                filenames[i] = files.get(i).getPath();
            LauncherMacOSX.openFiles(filenames);
        } catch (Exception e) {
        }
    }

}
