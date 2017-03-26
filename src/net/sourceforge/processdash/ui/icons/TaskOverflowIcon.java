// Copyright (C) 2014-2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.icons;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import net.sourceforge.processdash.ui.lib.AbstractPixelAwareRecolorableIcon;

public class TaskOverflowIcon extends AbstractPixelAwareRecolorableIcon {

    private Color color;

    public TaskOverflowIcon() {
        this.color = Color.black;
        this.height = 6;
        this.width = 9;
        this.antialias = false;
    }

    @Override
    protected void paintIcon(Graphics2D g2, int width, int height,
            float scale) {
        int hh = width / 4;
        float lw = hh;
        int top = (height - 2 * hh) / 2;

        g2.setStroke(new BasicStroke(1));
        g2.setColor(color);

        for (int left = 0; left < lw * 3; left++) {
            g2.drawLine(left, top + hh, left + hh, top);
            g2.drawLine(left, top + hh, left + hh, top + hh * 2);
            if (left == lw - 1)
                left += lw;
        }
    }

}
