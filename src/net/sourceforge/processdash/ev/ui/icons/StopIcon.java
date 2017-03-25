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

package net.sourceforge.processdash.ev.ui.icons;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;

import net.sourceforge.processdash.ui.lib.AbstractPixelAwareRecolorableIcon;

public class StopIcon extends AbstractPixelAwareRecolorableIcon {

    private Color fill, edge;

    public StopIcon(int size) {
        this.fill = new Color(234, 0, 0);
        this.edge = Color.black;
        this.antialias = false;
        this.width = this.height = size;
    }

    @Override
    protected void paintIcon(Graphics2D g2, int width, int height,
            float scale) {
        // calculate the pixel geometry of the octagon
        int angle = (int) (width / (Math.sqrt(2) * (1 + 2 / Math.sqrt(2))));
        int side = width - 1 - 2 * angle;
        int a = 0, b = angle, c = side + angle, d = side + 2 * angle;
        Shape s = shape(b, a, c, a, d, b, d, c, c, d, b, d, a, c, a, b);

        // fill the shape
        g2.setColor(fill);
        g2.fill(s);

        // draw the outer edge
        g2.setStroke(new BasicStroke(1));
        g2.setColor(edge);
        g2.draw(s);
    }

}
