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
import java.awt.Rectangle;
import java.awt.Shape;

import net.sourceforge.processdash.ui.lib.AbstractRecolorableIcon;

public class ProbeTaskIcon extends AbstractRecolorableIcon {

    private Color fill, edge, line, points;

    private Shape shape;

    public ProbeTaskIcon() {
        fill = Color.white;
        edge = Color.black;
        line = Color.decode("#6b24b3");
        points = Color.green.darker();
        shape = new Rectangle(2, 2, 11, 11);
    }

    @Override
    protected void paintIcon(Graphics2D g2, Shape clip, float scale) {
        int pad = 2, size = 11;
        g2.setColor(fill);
        g2.fill(shape);

        g2.clip(shape);
        g2.setColor(line);
        g2.drawLine(pad, pad + size - 2, pad + size, pad);

        g2.setColor(points);
        g2.fillRect(pad + 4, pad + 2, 2, 2);
        g2.fillRect(pad + 4, pad + 8, 2, 2);
        g2.fillRect(pad + 9, pad + 4, 2, 2);

        g2.setClip(clip);
        g2.setStroke(new BasicStroke(1f / scale));
        g2.setColor(edge);
        g2.drawRect(pad, pad, size, size);
    }

}
