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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.Icon;

public class ColumnSelectorIcon implements Icon {

    private Color edge, fill;

    public ColumnSelectorIcon(boolean enabled) {
        this.edge = enabled ? Color.black : Color.darkGray;
        this.fill = enabled ? Color.white : Color.lightGray;
    }

    @Override
    public int getIconWidth() {
        return 9;
    }

    @Override
    public int getIconHeight() {
        return 9;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        // remove any scaling from the graphics context, so we can render with
        // pixel precision
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(x, y);
        double scale = g2.getTransform().getScaleX();
        g2.scale(1 / scale, 1 / scale);

        // calculate the size of the icon and various features, in pixels
        int pixelSize = (int) (9 * scale);
        if ((pixelSize & 1) > 0)
            pixelSize--;
        int mid = pixelSize / 2;
        int low = (int) (0.5 + pixelSize / 4f);
        int lineWidth = (int) (0.4 + pixelSize / 9f);

        // draw the icon
        g2.setPaint(fill);
        g2.fillRect(0, 0, pixelSize, pixelSize);
        g2.setPaint(edge);
        g2.setStroke(new BasicStroke(lineWidth));
        g2.drawRect(0, 0, pixelSize, pixelSize);
        g2.drawLine(mid, 0, mid, pixelSize);
        g2.drawLine(0, low, pixelSize, low);
    }

}
