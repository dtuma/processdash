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
import java.awt.Shape;
import java.awt.Stroke;

import net.sourceforge.processdash.ui.lib.AbstractRecolorableIcon;

public class TaskIcon extends AbstractRecolorableIcon {

    private Color fill, highlight, shadow, edge;

    private Shape shape, topLeft, bottomRight;

    private static final Stroke BEVEL = new BasicStroke(3);

    public TaskIcon(Color fill) {
        this.fill = fill;
        this.highlight = highlight(fill);
        this.shadow = shadow(fill);
        this.edge = Color.black;

        this.shape = shape(0, 14, 5, 1, 15, 1, 10, 14);
        this.topLeft = shape(5, 1, 15, 1, 7, 6.5f, 8, 8.5f, 0, 14);
        this.bottomRight = shape(10, 14, 0, 14, 8, 9, 7, 6, 15, 1);
    }

    @Override
    protected void paintIcon(Graphics2D g2, Shape clip, float scale) {
        // fill the shape
        g2.setColor(fill);
        g2.fill(shape);

        // draw highlights
        g2.setStroke(BEVEL);
        clip(g2, clip, topLeft);
        g2.setColor(highlight);
        g2.draw(shape);

        // draw shadows
        clip(g2, clip, bottomRight);
        g2.setColor(shadow);
        g2.draw(shape);

        // draw edge
        g2.setStroke(new BasicStroke(Math.min(scale, 1) / scale));
        g2.setClip(clip);
        g2.setColor(edge);
        g2.draw(shape);
    }

}
