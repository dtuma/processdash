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
import java.awt.Shape;

import net.sourceforge.processdash.ui.lib.AbstractRecolorableIcon;
import net.sourceforge.processdash.ui.lib.PaintUtils;

public class FilterIcon extends AbstractRecolorableIcon {

    private Color fill, edge, glow;

    private GradientPaint gradient;

    private Shape shape;

    public FilterIcon(Color fill, Color glow) {
        this.fill = fill;
        this.edge = Color.black;
        if (glow != null)
            this.glow = PaintUtils.makeTransparent(glow, 90);
        finalizeColors();
        this.shape = shape(1, 1, 1, 2, 7, 8, 7, 14, //
            10, 14, 10, 8, 16, 2, 16, 1);
        this.width = 18;
    }

    @Override
    protected void finalizeColors() {
        Color shadow = PaintUtils.mixColors(fill, Color.black, 0.3f);
        this.gradient = new GradientPaint(width / 2, 0, fill, width, 0, shadow);
    }

    @Override
    protected void paintIcon(Graphics2D g2, Shape clip, float scale) {
        // paint the glow, if applicable
        if (glow != null) {
            g2.setPaint(glow);
            float w = 5;
            for (int i = 6; i-- > 1;) {
                g2.setStroke(new BasicStroke(w, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_ROUND));
                g2.draw(shape);
                w *= 0.6f;
            }
        }

        // fill and outline the shape
        g2.setPaint(gradient);
        g2.fill(shape);
        g2.setStroke(new BasicStroke(1 / scale));
        g2.setPaint(edge);
        g2.draw(shape);
    }

}
