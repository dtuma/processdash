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

package net.sourceforge.processdash.ui;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import org.json.simple.JSONObject;

import com.sun.jna.Native;
import com.sun.jna.Pointer;

import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;

public class WindowTracker {

    private static Window lastWindowOpened;

    /**
     * Run a task on the AWT event thread to display a new window, and return
     * the window that it opened.
     *
     * @param task
     *            a task that will open a window, and that will make a call to
     *            {@link #windowOpened(Window)}
     * @return the window that was opened, or null if no window was opened
     */
    public synchronized static Window openWindow(final Runnable task) {
        lastWindowOpened = null;

        // create a wrapper task to run on the AWT event thread.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // run the delegate task to open the window.
                task.run();
                // if the delegate task was ill-behaved and neglected to call
                // windowOpened, invoke that method ourselves to ensure our
                // caller does not wait forever.
                windowOpened(lastWindowOpened);
            }
        });

        // wait for the task to run and to call windowOpened()
        try {
            WindowTracker.class.wait(1000);
        } catch (InterruptedException ie) {
        }

        // return a handle to the window that was passed to windowOpened()
        return lastWindowOpened;
    }


    /**
     * Register the fact that a window was opened.
     */
    public synchronized static void windowOpened(Window w) {
        lastWindowOpened = w;
        WindowTracker.class.notifyAll();
    }


    /**
     * @return an object that can be added to a JOptionPane, and that will call
     *         windowOpened when the JOptionPane is displayed
     */
    public static Component dlg() {
        return new JOptionPaneTweaker() {
            public void doTweak(JDialog dialog) {
                windowOpened(dialog);
            }
        };
    }


    /**
     * Return a JSON document describing a window that was opened
     */
    public static String getWindowOpenedJson(Object w) {
        JSONObject window = new JSONObject();

        // store the window title
        if (w instanceof String)
            window.put("title", w);
        else if (w instanceof Frame)
            window.put("title", ((Frame) w).getTitle());
        else if (w instanceof Dialog)
            window.put("title", ((Dialog) w).getTitle());

        if (w instanceof Window) {
            if (System.getProperty("os.name").contains("Windows")) {
                // store the Windows HWND pointer, if available
                try {
                    Pointer windowPointer = Native.getWindowPointer((Window) w);
                    if (windowPointer != null)
                        window.put("id", Pointer.nativeValue(windowPointer));
                } catch (Throwable t) {
                }
            } else {
                // store the X11 XID, if available
                try {
                    long windowID = Native.getWindowID((Window) w);
                    if (windowID != 0)
                        window.put("id", windowID);
                } catch (Throwable t) {
                }
            }
        }

        // if we didn't recognize the parameter type, abort
        if (window.isEmpty())
            return null;

        // build an object and return the JSON
        JSONObject result = new JSONObject();
        result.put("window", window);
        result.put("stat", "ok");
        return result.toString();
    }

}
