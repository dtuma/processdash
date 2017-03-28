// Copyright (C) 2007-2017 Tuma Solutions, LLC
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

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
        Graphics2D g2 = (Graphics2D) g.create();
        float scale = (float) g2.getTransform().getScaleX();
        g2.setStroke(new BasicStroke(1 / scale));
        paintFrame(g2, x + 12, y);
        paintFrame(g2, x + 16, y + 3);

        g2.setColor(boxColor);
        g2.drawRect(x + 0, y + 1, 8, 8);

        if (isChecked()) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(checkColor);
            g2.setStroke(new BasicStroke(scale > 1.3 ? 1.5f : 1));
            Path2D p = new Path2D.Float();
            p.moveTo(x + 2, y + 5);
            p.lineTo(x + 4, y + 7);
            p.lineTo(x + 6, y + 3);
            g2.draw(p);
        }
    }

    private void paintFrame(Graphics g, int x, int y) {
        g.setColor(windowColor);
        g.fillRect(x, y, 8, 7);
        g.setColor(titleBarColor);
        g.fillRect(x, y, 8, 2);
        g.setColor(frameColor);
        g.drawRect(x, y, 8, 7);
    }

}
