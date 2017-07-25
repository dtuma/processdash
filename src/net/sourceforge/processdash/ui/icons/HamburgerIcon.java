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

package net.sourceforge.processdash.ui.icons;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;

import javax.swing.JMenu;

import net.sourceforge.processdash.ui.lib.AbstractPixelAwareRecolorableIcon;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;

public class HamburgerIcon extends AbstractPixelAwareRecolorableIcon {

    private Color fill, altFill;

    public HamburgerIcon() {
        width = 18;
        height = 19;
        fill = Color.DARK_GRAY;
        if (MacGUIUtils.isMacOSX())
            altFill = new Color(0xf0f0ff);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Color newFill = useAltFill(c) ? altFill : Color.DARK_GRAY;
        if (newFill != fill) {
            bufferedWidth = -1;
            fill = newFill;
        }
        super.paintIcon(c, g, x, y);
    }

    private boolean useAltFill(Component c) {
        return altFill != null && c instanceof JMenu
                && (((JMenu) c).isSelected() || ((JMenu) c).isArmed());
    }

    @Override
    protected void paintIcon(Graphics2D g2, int width, int height,
            float scale) {
        // compute geometry for the lines in the icon
        int dim = (height + 4) / 7;
        int lineWidth = dim + 1 - (dim & 1);
        float halfLine = lineWidth / 2f;
        int spacing = (height - 3 * lineWidth + 1) / 4;
        int top = (height - 2 * spacing - 3 * lineWidth) / 2;

        Stroke s = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND);
        g2.setStroke(s);
        g2.setColor(fill);
        g2.scale(1.5, 1);
        for (int i = 3; i-- > 0;) {
            float y = i * (lineWidth + spacing) + halfLine + top;
            g2.draw(new Line2D.Float(halfLine, y, //
                    (width * 2) / 3f - halfLine, y));
        }
    }

}
