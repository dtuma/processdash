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
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Path2D;

import net.sourceforge.processdash.ui.lib.AbstractRecolorableIcon;

public class DocumentIcon extends AbstractRecolorableIcon {

    private Color fill, page, edge;

    private Paint fillGradient;

    private Shape all, leaf;

    public DocumentIcon(Color fill) {
        float ax = 11, bx = 9, by = 2.5f, cy = 8;

        this.fill = fill;
        this.page = Color.white;
        this.edge = Color.black;
        finalizeColors();

        this.all = shape(2, 0, 2, 15, 14, 15, 14, cy, ax, 0);

        Path2D p = new Path2D.Float();
        p.moveTo(ax, 0);
        p.lineTo(bx, by);
        p.lineTo(14, cy);
        this.leaf = p;
    }

    @Override
    protected void finalizeColors() {
        this.fillGradient = new GradientPaint(0, 0, page, 16, 16, fill);
    }

    @Override
    protected void paintIcon(Graphics2D g2, Shape clip, float scale) {
        g2.setPaint(fillGradient);
        g2.fill(all);

        g2.setPaint(edge);
        g2.setStroke(new BasicStroke(0.6f / scale));
        g2.clip(all);
        g2.draw(leaf);

        g2.setStroke(new BasicStroke(1f / scale));
        g2.setClip(clip);
        g2.draw(all);
    }

}
