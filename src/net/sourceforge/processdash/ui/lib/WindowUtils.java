// Copyright (C) 2016-2021 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;

import javax.swing.SwingUtilities;

public class WindowUtils {

    /**
     * Make a window visible and bring it to the front of other windows.
     * 
     * @param c
     *            a frame, a window, or a component within a window
     */
    public static void showWindowToFront(Component c) {
        Window w;
        if (c instanceof Window) {
            w = (Window) c;
        } else {
            w = SwingUtilities.getWindowAncestor(c);
            if (w == null)
                return;
        }

        if (w instanceof Frame) {
            Frame f = (Frame) w;
            int state = f.getExtendedState();
            if ((state & Frame.ICONIFIED) != 0)
                f.setExtendedState(state - Frame.ICONIFIED);
        }

        w.setVisible(true);
        w.toFront();
        w.setAlwaysOnTop(true);
        w.toFront();
        w.setAlwaysOnTop(false);
        w.toFront();
    }

    public static void setLocationRelativeTo(Window window, Object relativeTo,
            int dx, int dy) {
        Component relativeComponent = null;
        if (relativeTo instanceof Component)
            relativeComponent = (Component) relativeTo;
        setLocationRelativeTo(window, relativeComponent, dx, dy);
    }

    public static void setLocationRelativeTo(Window window,
            Component relativeTo, int dx, int dy) {
        if (relativeTo != null && relativeTo.isShowing()) {
            Rectangle s = relativeTo.getGraphicsConfiguration().getBounds();
            Point r = relativeTo.getLocationOnScreen();
            Rectangle w = window.getBounds();
            int x = Math.max(s.x, Math.min(r.x + dx, s.x + s.width - w.width));
            int y = Math.max(s.y, Math.min(r.y + dy, s.y + s.height - w.height));
            window.setLocation(x, y);
        } else {
            window.setLocationRelativeTo(null);
        }
    }

}
