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

package net.sourceforge.processdash.ui.icons;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;

import net.sourceforge.processdash.ui.lib.AbstractPixelAwareRecolorableIcon;

public class ExternalLinkIcon extends AbstractPixelAwareRecolorableIcon {

    private Color color;

    public ExternalLinkIcon() {
        this.color = Color.darkGray;
        this.width = this.height = 10;
        this.antialias = false;
    }

    @Override
    protected void paintIcon(Graphics2D g2, int width, int height,
            float scale) {
        // calculate geometry for the box and arrow
        int boxSize = (int) (0.5 + width * 0.8);
        int boxHalf = (boxSize + 1) / 2;
        int arrowBoxSize = width - boxHalf;
        Shape arrowBox = new Rectangle(boxHalf, 0, arrowBoxSize, arrowBoxSize);
        Area nonArrow = new Area(new Rectangle(0, 0, width, height));
        nonArrow.subtract(new Area(arrowBox));

        // draw the box
        g2.setColor(color);
        g2.setClip(nonArrow);
        int lw = width / 7;
        for (int i = lw; i-- > 0;) {
            int bw = boxSize - 1 - 2 * i;
            g2.drawRect(i, height - boxSize + i, bw, bw);
        }

        // draw the shaft of the arrow
        g2.setClip(arrowBox);
        lw = width / 8;
        for (int i = 1 + lw * 2; i-- > 0;) {
            int d = i - lw;
            g2.drawLine(boxHalf - d - 1, height - boxHalf, width - d - 1, 0);
        }

        // draw the arrow head
        for (int i = boxHalf; i-- > 0;) {
            int x = width - 1 - i;
            g2.drawLine(x, 0, x + boxHalf - 1, boxHalf - 1);
        }
    }

}
