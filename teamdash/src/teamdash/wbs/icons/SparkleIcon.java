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
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;

import net.sourceforge.processdash.ui.lib.AbstractRecolorableIcon;

public class SparkleIcon extends AbstractRecolorableIcon {

    private Color white, yellow, orange;

    private Shape shape;

    public SparkleIcon(boolean centered, float size) {
        this.white = Color.white;
        this.yellow = Color.yellow;
        this.orange = new Color(255, 152, 8);

        Path2D p = new Path2D.Float();
        for (int i = 0; i < 5; i++) {
            p.moveTo(0, 0);
            double deg = 2 * Math.PI * i / 5;
            p.lineTo(-size * Math.sin(deg), -size * Math.cos(deg));
        }
        Rectangle b = p.getBounds();
        this.width = b.width;
        this.height = b.height;

        if (!centered) {
            AffineTransform t = new AffineTransform();
            t.translate(-b.getMinX(), -b.getMinY());
            p.transform(t);
        }
        this.shape = p;
    }

    @Override
    protected void paintIcon(Graphics2D g2, Shape clip, float scale) {
        g2.setStroke(new BasicStroke(4));
        g2.setColor(orange);
        g2.draw(shape);

        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(yellow);
        g2.draw(shape);

        g2.setStroke(new BasicStroke(0.5f));
        g2.setColor(white);
        g2.draw(shape);
    }

}
