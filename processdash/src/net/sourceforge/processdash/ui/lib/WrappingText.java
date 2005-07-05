// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2005 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.lib;

import java.awt.Dimension;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class WrappingText extends JTextArea implements Runnable {

    private int lastWidth = -1;

    public WrappingText(String text) {
        super(text);
        setEditable(false);
        setLineWrap(true);
        setWrapStyleWord(true);
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
