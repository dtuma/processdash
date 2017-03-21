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

import net.sourceforge.processdash.ui.lib.AbstractPixelAwareRecolorableIcon;

public class CollapseAllIcon extends AbstractPixelAwareRecolorableIcon {

    private Color fill, edge;

    public CollapseAllIcon(Color fill) {
        this.fill = fill;
        this.edge = Color.black;
    }

    @Override
    protected void paintIcon(Graphics2D g2, int width, int height,
            float scale) {
        // compute the geometry for the icon
        int minusWidth = (int) (0.5 + 9 * width / 16f);
        int minusHeight = (int) (0.5 + minusWidth / 3f);
        int top = (height - 2 * minusHeight) / 3;
        int top2 = height - top - minusHeight - 1;
        int left = (int) (width / 15);
        int left2 = width - left - minusWidth - 1;

        // fill the shapes
        g2.setColor(fill);
        g2.fillRect(left, top, minusWidth, minusHeight);
        g2.fillRect(left2, top2, minusWidth, minusHeight);

        // draw the edges
        g2.setColor(edge);
        g2.setStroke(new BasicStroke(1));
        g2.drawRect(left, top, minusWidth, minusHeight);
        g2.drawRect(left2, top2, minusWidth, minusHeight);
    }

}
