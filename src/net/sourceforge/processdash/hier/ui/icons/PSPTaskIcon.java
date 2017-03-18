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

public class PSPTaskIcon extends AbstractRecolorableIcon {

    private Color fill, highlight, glare, shadow, edge;

    private Shape shape;

    private Shape[] edges;

    private static final Stroke BEVEL = new BasicStroke(3);

    public PSPTaskIcon(Color fill) {
        this.fill = fill;
        this.highlight = highlight(fill);
        this.shadow = shadow(fill);
        this.glare = Color.white;
        this.edge = Color.black;
        this.strokePure = true;

        // coordinates for the corners of the pentagon shape
        float[] coords = new float[] { 7.5f, 0.5f, 14.33f, 6.1f, 11.72f, 14, //
                3.28f, 14, 0.67f, 6.1f };
        this.shape = shape(coords);

        this.edges = new Shape[5];
        for (int i = 0; i < 5; i++)
            edges[i] = edge(coords, i * 2);
    }

    private Shape edge(float[] coords, int pos) {
        float[] edgeCoords = new float[6];
        // first vertex of pentagon
        System.arraycopy(coords, pos, edgeCoords, 0, 2);
        // center point of pentagon
        edgeCoords[2] = 7.5f;
        edgeCoords[3] = 8.19f;
        // next vertex of pentagon
        pos = (pos + 2) % coords.length;
        System.arraycopy(coords, pos, edgeCoords, 4, 2);
        return shape(edgeCoords);
    }

    @Override
    protected void paintIcon(Graphics2D g2, Shape clip, float scale) {
        // fill the shape
        g2.setColor(fill);
        g2.fill(shape);

        // draw shadows (bottom and bottom-right edges)
        g2.setStroke(BEVEL);
        g2.setColor(shadow);
        clip(g2, clip, edges[1]);
        g2.draw(shape);
        clip(g2, clip, edges[2]);
        g2.draw(shape);

        // draw highlights (top-right and bottom-left edges)
        g2.setColor(highlight);
        clip(g2, clip, edges[0]);
        g2.draw(shape);
        clip(g2, clip, edges[3]);
        g2.draw(shape);

        // draw glare (top-left edge)
        g2.setColor(glare);
        clip(g2, clip, edges[4]);
        g2.draw(shape);

        // draw outline
        g2.setStroke(new BasicStroke(1f / scale));
        g2.setColor(edge);
        g2.setClip(clip);
        g2.draw(shape);
    }

}
