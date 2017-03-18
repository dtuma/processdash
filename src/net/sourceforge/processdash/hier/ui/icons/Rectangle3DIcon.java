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

package net.sourceforge.processdash.hier.ui.icons;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;

import net.sourceforge.processdash.ui.lib.AbstractRecolorableIcon;

public class Rectangle3DIcon extends AbstractRecolorableIcon {

    protected Color fill, highlight, shadow, edge;

    private Shape shape, topLeft, bottomRight;

    private static final Stroke BEVEL = new BasicStroke(3);

    public Rectangle3DIcon(int iconWidth, int iconHeight, Color fill, int top,
            int left, int bottom, int right) {
        this.width = iconWidth;
        this.height = iconHeight;
        this.fill = fill;
        this.highlight = highlight(fill);
        this.shadow = shadow(fill);
        this.edge = Color.black;

        this.shape = new Rectangle(left, top, iconWidth - left - right - 1,
                iconHeight - top - bottom - 1);

        int rightEdge = iconWidth - right - 1;
        int bottomEdge = iconHeight - bottom - 1;

        topLeft = shape(left, top, rightEdge, top, rightEdge - 4, top + 4, //
            left + 4, bottomEdge - 4, left, bottomEdge);
        bottomRight = shape(rightEdge, bottomEdge, left, bottomEdge, //
            left + 4, bottomEdge - 4, rightEdge - 4, top + 4, rightEdge, top);
    }

    @Override
    protected void paintIcon(Graphics2D g2, Shape clip, float scale) {
        // fill the shape
        g2.setColor(fill);
        g2.fill(shape);

        // draw highlights
        g2.setStroke(BEVEL);
        g2.setColor(highlight);
        clip(g2, clip, topLeft);
        g2.draw(shape);

        // draw shadows
        g2.setColor(shadow);
        clip(g2, clip, bottomRight);
        g2.draw(shape);

        // draw edge
        g2.setStroke(new BasicStroke(Math.min(scale, 1) / scale));
        g2.setColor(edge);
        g2.setClip(clip);
        g2.draw(shape);
    }

}
