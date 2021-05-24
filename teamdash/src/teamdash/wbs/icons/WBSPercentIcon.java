// Copyright (C) 2021 Tuma Solutions, LLC
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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import net.sourceforge.processdash.ui.lib.AbstractRecolorableIcon;

public class WBSPercentIcon extends AbstractRecolorableIcon {

    private Color line;

    private Font font;

    public WBSPercentIcon() {
        this.line = Color.black;
        this.font = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    }

    @Override
    protected void paintIcon(Graphics2D g2, Shape clip, float scale) {
        g2.setColor(line);
        g2.setFont(font);

        Rectangle2D b = g2.getFontMetrics().getStringBounds("%", g2);
        double left = (getIconWidth() - b.getWidth()) / 2;
        g2.drawString("%", (float) left, 14);
    }

}
