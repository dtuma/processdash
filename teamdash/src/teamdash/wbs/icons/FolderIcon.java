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
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;

import net.sourceforge.processdash.ui.lib.AbstractRecolorableIcon;

public class FolderIcon extends AbstractRecolorableIcon {

    private Color fill, shadow, edge;

    private Shape back, front;

    public FolderIcon(Color fill) {
        this.fill = fill;
        this.shadow = shadow(fill);
        this.edge = Color.black;
        this.width = 19;

        Area a = new Area();
        a.add(new Area(new RoundRectangle2D.Float(1, 1, 6, 4, 3, 3)));
        a.add(new Area(new Rectangle(1, 3, 12, 9)));
        this.back = a;

        this.front = shape(1, 12, 5, 5, 17, 5, 13, 12);
    }

    @Override
    protected void paintIcon(Graphics2D g2, Shape clip, float scale) {
        // paint the back of the folder
        g2.setColor(shadow);
        g2.fill(back);
        g2.setStroke(new BasicStroke(1 / scale));
        g2.setColor(edge);
        g2.draw(back);

        // paint the front of the folder
        g2.setPaint(new GradientPaint(12, 7, fill, 13, 12, shadow));
        g2.fill(front);
        g2.setColor(edge);
        g2.draw(front);
    }

}
