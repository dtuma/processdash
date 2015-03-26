// Copyright (C) 2002-2015 Tuma Solutions, LLC
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
import java.awt.Graphics;

import javax.swing.Icon;

public class ConcatenatedIcon implements Icon {

    private Icon[] icons;

    int width, height;

    public ConcatenatedIcon(Icon... icons) {
        this.icons = icons;
        this.width = this.height = 0;
        for (int i = 0; i < icons.length; i++) {
            this.width += icons[i].getIconWidth();
            this.height = Math.max(this.height, icons[i].getIconHeight());
        }
    }

    public int getIconHeight() {
        return height;
    }

    public int getIconWidth() {
        return width;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        for (int i = 0; i < icons.length; i++) {
            icons[i].paintIcon(c, g, x, y);
            x += icons[i].getIconWidth();
        }
    }

}
