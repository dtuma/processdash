// Copyright (C) 2007-2011 Tuma Solutions, LLC
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
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

public class MultiWindowCheckboxIcon implements Icon {

    private Color checkColor = Color.black;
    private Color boxColor = Color.gray;
    private Color frameColor = Color.black;
    private Color titleBarColor = Color.blue.darker();
    private Color windowColor = Color.white;

    private boolean checked;

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public int getIconHeight() {
        return 11;
    }

    public int getIconWidth() {
        return 25;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.setColor(boxColor);
        g.drawRect(x + 0, y + 1, 8, 8);
        paintFrame(g, x + 12, y);
        paintFrame(g, x + 16, y + 3);

        if (isChecked()) {
            g.setColor(checkColor);
            for (int i = -1; i < 4; i++)
                g.drawLine(x + 3 + i, y + 6 - Math.abs(i), x + 3 + i, y + 7
                        - Math.abs(i));
        }
    }

    private void paintFrame(Graphics g, int x, int y) {
        g.setColor(frameColor);
        g.drawRect(x, y, 8, 7);
        g.setColor(windowColor);
        g.fillRect(x + 1, y + 1, 7, 6);
        g.setColor(titleBarColor);
        g.drawLine(x + 1, y + 1, x + 7, y + 1);
    }

}
