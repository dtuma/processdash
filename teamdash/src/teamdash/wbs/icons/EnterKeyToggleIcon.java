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

package teamdash.wbs.icons;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;

import net.sourceforge.processdash.ui.lib.AbstractRecolorableIcon;

import teamdash.wbs.IconFactory;

public class EnterKeyToggleIcon extends AbstractRecolorableIcon {

    private Color top, left, shadow, edge, fill, arrow;
    
    private Stroke bevel;

    private SparkleIcon sparkle;

    private Shape shape;

    private Shape[] edges;

    private Color[] edgeColors;

    public EnterKeyToggleIcon(boolean on) {
        this.left = highlight(Color.lightGray);
        this.top = Color.lightGray;
        this.shadow = Color.gray;
        this.edge = Color.darkGray;
        this.fill = Color.white;
        this.arrow = shadow(top);
        this.bevel = new BasicStroke(3);

        this.sparkle = new SparkleIcon(true, 2f);
        if (on == false) {
            sparkle = (SparkleIcon) IconFactory.getModifiedIcon(sparkle,
                IconFactory.DISABLED_ICON);
        }

        float[] coords = new float[] { 0, 8, 0, 15, 15, 15, 15, 5, 8, 5, 8, 8 };
        this.shape = shape(coords);
        this.edges = new Shape[6];
        this.edgeColors = new Color[6];
        edges[0] = edge(coords,  0, DR, UR);  edgeColors[0] = left;
        edges[1] = edge(coords,  2, UR, UL);  edgeColors[1] = shadow;
        edges[2] = edge(coords,  4, UL, DL);  edgeColors[2] = shadow;
        edges[3] = edge(coords,  6, DL, DR);  edgeColors[3] = top;
        edges[4] = edge(coords,  8, DR, DR);  edgeColors[4] = left;
        edges[5] = edge(coords, 10, DR, DR);  edgeColors[5] = top;
    }

    private static final int UR = 3, UL = 2, DR = 1, DL = 0;

    private Shape edge(float[] coords, int pos, int dirA, int dirB) {
        float[] edge = new float[8];
        System.arraycopy(coords, pos, edge, 0, 2);
        addBevelDelta(edge, 0, 2, dirA);
        pos = (pos + 2) % coords.length;
        System.arraycopy(coords, pos, edge, 6, 2);
        addBevelDelta(edge, 6, 4, dirB);
        return shape(edge);
    }

    private void addBevelDelta(float[] edge, int pos, int destPos, int dir) {
        float x = edge[pos], y = edge[pos + 1];
        x += ((dir & 1) > 0 ? 3 : -3);
        y += ((dir & 2) > 0 ? -3 : 3);
        edge[destPos] = x;
        edge[destPos + 1] = y;
    }


    @Override
    protected void paintIcon(Graphics2D g2, Shape clip, float scale) {
        // fill the enter key
        g2.setColor(fill);
        g2.fill(shape);

        // draw the beveled edges on each side of the key
        g2.setStroke(bevel);
        for (int i = edges.length; i-- > 0;) {
            g2.setClip(edges[i]);
            g2.setColor(edgeColors[i]);
            g2.draw(shape);
        }
        g2.setClip(clip);

        // draw a thin border around the outside
        g2.setStroke(new BasicStroke(1 / scale));
        g2.setColor(scale > 0 ? edge : shadow);
        g2.draw(shape);

        // create a pixel-precise graphics context to draw the arrow
        Graphics2D g3 = (Graphics2D) g2.create();
        g3.scale(1 / scale, 1 / scale);

        // draw the shaft of the arrow
        int r = (int) (11.5 * scale + 0.4);
        int l = (int) (4 * scale);
        g3.setColor(arrow);
        g3.setStroke(new BasicStroke(1));
        g3.drawLine(r, (int) (8 * scale), r, r);
        g3.drawLine(l, r, r, r);

        // draw the arrowhead
        int dy = (int) (scale + 0.3);
        int dx = (dy == 1 ? 1 : 2 * dy);
        Shape arrowhead = shape(l, r, l + dx, r - dy, l + dx, r + dy);
        g3.fill(arrowhead);
        g3.draw(arrowhead);

        // paint the sparkle
        g2.translate(4, 4);
        sparkle.paintIcon(g2, clip, scale);
    }

}
