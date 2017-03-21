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

import net.sourceforge.processdash.ui.lib.AbstractPixelAwareRecolorableIcon;

public class ExpandAllIcon extends AbstractPixelAwareRecolorableIcon {

    private Color fill, edge;

    public ExpandAllIcon(Color fill) {
        this.width = 18;
        this.fill = fill;
        this.edge = Color.black;
    }

    @Override
    protected void paintIcon(Graphics2D g2, int width, int height,
            float scale) {
        g2.setStroke(new BasicStroke(1));

        // calculate the size of the icon and various features
        int plusSize = width / 2;
        int leg = (int) (0.7 + plusSize / 3f);
        int body = plusSize - 2 * leg;

        // calculate the shape of a single plus sign
        int a = 0, b = leg, c = leg + body, d = plusSize;
        Shape shape = shape(b, a, c, a, c, b, d, b, d, c, c, c, c, d, b, d, //
            b, c, a, c, a, b, b, b);

        // draw the top-left plus sign
        g2.setColor(fill);
        g2.fill(shape);
        g2.setColor(edge);
        g2.draw(shape);

        // draw the bottom-right plus sign
        g2.translate(plusSize - 1, c);
        g2.setColor(fill);
        g2.fill(shape);
        g2.setColor(edge);
        g2.draw(shape);
    }

}
