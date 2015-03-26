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

package net.sourceforge.processdash.ui.lib;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

public class PaddedIcon implements Icon {

    private Icon delegate;
    private int top, left, bottom, right;

    public PaddedIcon(Icon delegate, int top, int left, int bottom, int right) {
        this.delegate = delegate;
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
    }

    public int getIconHeight() {
        return delegate.getIconHeight() + top + bottom;
    }

    public int getIconWidth() {
        return delegate.getIconWidth() + left + right;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        delegate.paintIcon(c, g, x+left, y+top);
    }

}
