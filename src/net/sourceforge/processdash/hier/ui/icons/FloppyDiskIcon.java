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
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import net.sourceforge.processdash.ui.lib.AbstractRecolorableIcon;

public class FloppyDiskIcon extends AbstractRecolorableIcon {

    private Color highlight, gray, white, edge;

    private Shape area, label;

    public FloppyDiskIcon(Color highlight) {
        this.highlight = highlight;
        this.edge = Color.black;
        this.gray = Color.gray;
        this.white = Color.white;

        this.area = shape(1, 1, 13, 1, 14, 2, 14, 14, 1, 14);
        this.label = new RoundRectangle2D.Float(3, 8, 9, 9, 3, 3);
    }

    @Override
    protected void paintIcon(Graphics2D g2, Shape clip, float scale) {
        // fill the background
        g2.setColor(highlight);
        g2.fill(area);

        // paint the shutter
        g2.setColor(gray);
        g2.fill(new Rectangle2D.Float(5, 1, 6, 5 + 1 / scale));
        g2.setColor(white);
        g2.fill(new Rectangle2D.Float(8, 2 + 1 / scale, 2, 3));

        // paint the label
        g2.clip(area);
        g2.setColor(white);
        g2.fill(label);
        g2.setStroke(new BasicStroke(1 / scale));
        g2.setColor(gray);
        g2.draw(label);

        // draw text on the label
        g2.setColor(highlight);
        int t = 1;
        for (float f = 10; f < 13; f += 2 / scale) {
            g2.draw(new Line2D.Float(5, f, 10 - t, f));
            t = (t + 2) % 3;
        }

        // paint the outer edge
        g2.setClip(clip);
        g2.setColor(edge);
        g2.draw(area);
    }

}
