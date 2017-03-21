// Copyright (C) 2017 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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
import java.awt.geom.Path2D;

import net.sourceforge.processdash.ui.lib.AbstractRecolorableIcon;

public class UndoIcon extends AbstractRecolorableIcon {

    private Color fill, shadow, edge;

    private Shape back, front;

    private boolean flip;

    public UndoIcon(Color fill, boolean flip) {
        this.fill = fill;
        this.shadow = shadow(fill);
        this.edge = Color.black;
        this.flip = flip;

        Path2D p = new Path2D.Float();
        p.moveTo(x(4), 0);
        p.quadTo(x(14.7f), 0.5, x(15), 3);
        p.lineTo(x(15), 9);
        p.quadTo(x(14.7f), 4.5, x(4), 3);
        p.closePath();
        back = p;

        p = new Path2D.Float();
        p.moveTo(x(15), 3);
        p.quadTo(x(14.7f), 7.5f, x(6), 8);
        p.lineTo(x(6), 5);
        p.lineTo(x(0), 10);
        p.lineTo(x(6), 15);
        p.lineTo(x(6), 12);
        p.quadTo(x(14.7f), 11.5, x(15), 9);
        p.closePath();
        front = p;
    }

    private float x(float x) {
        return (flip ? 15 - x : x);
    }

    @Override
    protected void paintIcon(Graphics2D g2, Shape clip, float scale) {
        g2.setStroke(new BasicStroke(0.7f / scale));

        // draw the back portion
        g2.setColor(shadow);
        g2.fill(back);
        g2.setColor(edge);
        g2.draw(back);

        // draw the front portion
        g2.setColor(fill);
        g2.fill(front);
        g2.setColor(edge);
        g2.draw(front);
    }

}
