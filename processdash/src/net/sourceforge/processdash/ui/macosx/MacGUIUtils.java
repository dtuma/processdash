// Copyright (C) 2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.macosx;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

public class MacGUIUtils {

    private static final boolean IS_MAC_OS_X =
        "Mac OS X".equalsIgnoreCase(System.getProperty("os.name"));

    public static boolean isMacOSX() {
        return IS_MAC_OS_X;
    }

    public static void tweakLookAndFeel() {
        if (!isMacOSX())
            return;

        UIManager.put("Table.gridColor", Color.gray);
    }

    public static void setDirty(JFrame frame, boolean isDirty) {
        setDirty(frame.getRootPane(), isDirty);
    }

    public static void setDirty(JDialog dialog, boolean isDirty) {
        setDirty(dialog.getRootPane(), isDirty);
    }

    private static void setDirty(JComponent comp, boolean isDirty) {
        if (comp != null)
            comp.putClientProperty("windowModified", Boolean.valueOf(isDirty));
    }

    public static void tweakTable(JTable table) {
        if (isMacOSX()) {
            table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_META,
                KeyEvent.META_DOWN_MASK), "doNothing");
            table.getActionMap().put("doNothing", new AbstractAction() {
                public void actionPerformed(ActionEvent e) {}});
        }
    }
}
