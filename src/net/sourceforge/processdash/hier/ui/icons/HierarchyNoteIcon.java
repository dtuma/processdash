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
import java.awt.Component;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.lib.AbstractPixelAwareRecolorableIcon;
import net.sourceforge.processdash.ui.lib.PaintUtils;

public class HierarchyNoteIcon extends AbstractPixelAwareRecolorableIcon {
    
    public static final HierarchyNoteIcon 
        WHITE = new HierarchyNoteIcon(Color.white),
        YELLOW = new HierarchyNoteIcon(new Color(255, 184, 0)),
        RED = new HierarchyNoteIcon(Color.red);

    private Color fill, edge, text;

    public HierarchyNoteIcon(Color fill) {
        this.fill = fill;
        this.edge = PaintUtils.mixColors(fill, Color.black, 0.5);
        this.text = Color.gray;
        this.width = DashboardIconFactory.getStandardIconSize()
                - 2 * DashboardIconFactory.getStandardIconPad();
        this.height = (int) (0.5 + width * 11 / 15f);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.setColor(c.getBackground());
        g.fillRect(x, y, width, height);
        super.paintIcon(c, g, x, y);
    }

    @Override
    protected void paintIcon(Graphics2D g2, int width, int height,
            float scale) {
        // fill the background area and trace its outline
        Shape shape = new RoundRectangle2D.Float(0, 0, width - 1, height - 1,
                5 * scale, 5 * scale);
        g2.setPaint(new GradientPaint(0, 0, fill, width, height, Color.white));
        g2.fill(shape);
        g2.setStroke(new BasicStroke(1));
        g2.setColor(edge);
        g2.draw(shape);

        // draw text in the center
        int x = (int) (2 * Math.min(scale, 2));
        int y = (int) (3 * scale);
        int lineSpacing = (int) (2.3 * scale);
        g2.setClip(new RoundRectangle2D.Float(x, x, width - 2 * x,
                height - 2 * x, 2 * scale, 2 * scale));
        g2.setFont(new Font("Dialog", Font.BOLD, 10).deriveFont(1.9f * scale));
        g2.setColor(text);
        String s = TEXT;
        while (y <= height - 2) {
            g2.drawString(s, x, y);
            s = s.substring(10);
            y += lineSpacing;
        }
    }

    private static final String TEXT = " Lorem ipsum dolor sit amet, "
            + "consectetur  adipiscing elit, sed do eiusmod tempor incididunt";

}
