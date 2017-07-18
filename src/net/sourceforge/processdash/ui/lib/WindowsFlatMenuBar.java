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

package net.sourceforge.processdash.ui.lib;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JMenuBar;
import javax.swing.UIManager;

/**
 * Specialization of JMenuBar that alters the background color on the Windows
 * look and feel to match other controls. (Other look and feels receive stock
 * behavior.)
 */
public class WindowsFlatMenuBar extends JMenuBar {

    private Color background;

    public WindowsFlatMenuBar() {
        if (WindowsGUIUtils.isWindowsLAF())
            background = UIManager.getColor("control");
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (background != null) {
            g.setColor(background);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

}
