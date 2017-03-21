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
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import net.sourceforge.processdash.ui.lib.AbstractPixelAwareRecolorableIcon;
import net.sourceforge.processdash.ui.lib.PaintUtils;

public class TrashCanIcon extends AbstractPixelAwareRecolorableIcon {

    private Color fill, edge;

    public TrashCanIcon(Color fill) {
        this.fill = fill;
        this.edge = Color.black;
    }


    @Override
    protected void paintIcon(Graphics2D g2, int width, int height,
            float scale) {

        // calculate the gradient for shading
        Color shadow = PaintUtils.mixColors(fill, edge, 0.3f);
        GradientPaint gradient = new GradientPaint(0, 0, fill, width, 0,
                shadow);

        // draw the can
        float s2 = 2 * scale;
        int hPad = (int) s2;
        int ww = width - 2 * hPad - 2;
        int top = (int) (2 * scale + 1);
        RoundRectangle2D can = new RoundRectangle2D.Float(hPad, top, //
                ww, height - top - 1, s2, s2);
        g2.setPaint(gradient);
        g2.fill(can);
        g2.setColor(edge);
        g2.draw(can);

        // draw lines on the can
        int lineSpacing = Math.min(5, Math.max(2, ww / 5));
        int lineIndent = Math.max(2, (lineSpacing + ww % lineSpacing) / 2);
        for (int x = hPad + lineIndent; x < can.getMaxX() - 1; x += lineSpacing)
            g2.drawLine(x, top, x, height - 3);

        // draw the handle
        hPad = (int) (5 * scale);
        ww = width - 2 * hPad - 2;
        g2.setStroke(new BasicStroke(1));
        g2.setColor(edge);
        g2.draw(new RoundRectangle2D.Float(hPad, 0, ww, 3 * scale, s2, s2));

        // draw the lid
        hPad = (int) scale;
        ww = width - 2 * hPad - 2;
        int lh = (int) (2 * scale + 0.5);
        Area lid = new Area();
        lid.add(new Area(new RoundRectangle2D.Float(hPad, 2 * hPad, //
                ww, lh * 2, s2, s2)));
        lid.intersect(new Area(new Rectangle2D.Float(hPad, 2 * hPad, ww, lh)));
        g2.setPaint(gradient);
        g2.fill(lid);
        g2.setPaint(edge);
        g2.draw(lid);
    }

}
