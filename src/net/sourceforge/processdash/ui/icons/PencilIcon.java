// Copyright (C) 2020 Tuma Solutions, LLC
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;

import net.sourceforge.processdash.ui.lib.AbstractPixelAwareRecolorableIcon;

public class PencilIcon extends AbstractPixelAwareRecolorableIcon {

    private Color edge, point;

    public PencilIcon(Color edge) {
        this.edge = edge;
        this.point = new Color(edge.getRed(), edge.getGreen(), edge.getBlue(), 64);
    }

    @Override
    protected void paintIcon(Graphics2D g2, int width, int height,
            float scale) {

        // calculate points in the geometry for the shape of the pencil
        int gap = 0, w = width / 3, m = width / 10;
        int top = gap, left = gap, right = height - 1, bottom = right;
        int ax = left + m, ay = bottom - w;
        int bx = left + w, by = bottom - m;
        int diag = ay - top;
        int cx = bx + diag, cy = by - diag;
        int dx = ax + diag, dy = ay - diag;
        int ee = width / 8;
        int ex = cx - ee, ey = cy + ee;
        int fx = dx - ee, fy = dy + ee;
        int px = left, py = bottom;

        // create shapes for various elements
        Shape bodyShape = shape(ax, ay, bx, by, cx, cy, dx, dy);
        Shape pointShape = shape(px, py, ax, ay, bx, by);
        Shape eraserShape = shape(ex, ey, fx, fy, dx, dy, cx, cy);


        // fill in the point of the pencil
        Shape clip = g2.getClip();
        clip(g2, clip, pointShape);
        g2.setColor(point);
        g2.fill(pointShape);

        // draw outlines for the point, body, and eraser
        g2.setClip(clip);
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(edge);
        g2.draw(bodyShape);
        g2.setStroke(new BasicStroke(0.5f));
        g2.draw(pointShape);
        g2.draw(eraserShape);
    }

}
