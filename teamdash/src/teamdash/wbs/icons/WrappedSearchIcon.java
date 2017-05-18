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
import java.awt.geom.RoundRectangle2D;

import net.sourceforge.processdash.ui.lib.AbstractRecolorableIcon;

public class WrappedSearchIcon extends AbstractRecolorableIcon {

    private Color line;

    private Shape arrow, loop;

    public WrappedSearchIcon() {
        this.line = Color.black;

        int s = 5, x = 9, y = 11;
        this.arrow = shape(x, y, x - s / 2, y - s, x + s / 2, y - s);
        this.loop = new RoundRectangle2D.Float(1, 1, 8, 14, 6, 10);
    }

    public void setColor(Color c) {
        line = c;
    }

    @Override
    protected void paintIcon(Graphics2D g2, Shape clip, float scale) {
        g2.setStroke(new BasicStroke(1 / scale));
        g2.setColor(line);
        g2.draw(arrow);
        g2.fill(arrow);
        g2.draw(loop);
    }

}
