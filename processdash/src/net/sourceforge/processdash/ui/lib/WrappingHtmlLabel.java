// Copyright (C) 2005-2011 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.lib;

import java.awt.Dimension;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;

public class WrappingHtmlLabel extends JEditorPane implements Runnable {

    private int lastWidth = -1;

    public WrappingHtmlLabel(String text) {
        super("text/html", text);
        setEditable(false);
        setBackground(null);
    }

    public Dimension getMinimumSize() {
        Dimension d = getPreferredSize();
        d.width = 0;
        return d;
    }

    public Dimension getMaximumSize() {
        Dimension d = getPreferredSize();
        d.width = Integer.MAX_VALUE;
        return d;
    }

    public void run() {
        invalidate();
    }

    public void setSize(Dimension d) {
        super.setSize(d);
        checkWidth(d.width);
    }

    public void setSize(int width, int height) {
        super.setSize(width, height);
        checkWidth(width);
    }

    private void checkWidth(int width) {
        if (width != lastWidth) {
            lastWidth = width;
            SwingUtilities.invokeLater(this);
        }
    }
}
