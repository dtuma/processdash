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

import net.sourceforge.processdash.ui.lib.AbstractRecolorableIcon;

public class BlockArrowIcon extends AbstractRecolorableIcon {

    public static final int UP = 0, DOWN = 1, LEFT = 2, RIGHT = 3;

    private Color fill, edge;

    private Shape shape;

    public BlockArrowIcon(Color fill, int direction) {
        this.fill = fill;
        this.edge = Color.black;
        boolean flip = (direction & 1) > 0;
        boolean transpose = (direction & 2) > 0;
        this.shape = shape(transpose, flip, 8, 0, 15, 7, 11, 7, 11, 15, 4, 15,
            4, 7, 0, 7, 7, 0);
    }

    private Shape shape(boolean transpose, boolean flip, float... coords) {
        for (int i = 0; i < coords.length; i += 2) {
            if (flip)
                coords[i + 1] = 15 - coords[i + 1];
            if (transpose) {
                float t = coords[i];
                coords[i] = coords[i + 1];
                coords[i + 1] = t;
            }
        }
        return super.shape(coords);
    }

    @Override
    protected void paintIcon(Graphics2D g2, Shape clip, float scale) {
        // fill the shape
        g2.setColor(fill);
        g2.fill(shape);

        // draw edge
        g2.setStroke(new BasicStroke(0.7f / scale));
        g2.setColor(edge);
        g2.draw(shape);
    }

}
