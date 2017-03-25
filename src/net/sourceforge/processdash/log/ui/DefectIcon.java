// Copyright (C) 2015-2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.lib.AbstractPixelAwareRecolorableIcon;

public class DefectIcon extends AbstractPixelAwareRecolorableIcon {

    public static DefectIcon enabled() {
        return new DefectIcon(DashboardIconFactory.getStandardIconSize(),
                Color.DARK_GRAY, new Color(0, 98, 49), new Color(0, 49, 156));
    }

    public static DefectIcon disabled() {
        return new DefectIcon(DashboardIconFactory.getStandardIconSize(),
                new Color(170, 170, 170), new Color(200, 200, 200),
                new Color(170, 170, 170));
    }


    private Color outline, fill, contrast;

    public DefectIcon(int height, Color outline, Color fill, Color contrast) {
        this.height = height;
        this.width = (int) Math.round(height * 1.58);
        this.outline = outline;
        this.fill = fill;
        this.contrast = contrast;
    }

    @Override
    protected void paintIcon(Graphics2D g2, int width, int height,
            float scale) {

        // calculate geometry and shapes of the icon
        int w = width;
        int h = height;
        int pad = height / 5;
        int pad2 = 2 * pad;
        int yMid = h / 2;

        Shape torso = new Ellipse2D.Double(pad + 3, pad, w - pad - 4,
                h - pad2 - 1);
        Shape head = new Ellipse2D.Double(pad - 1, yMid - pad, pad2, pad2);

        Area outsideTorso = new Area(new Rectangle2D.Double(0, 0, w, h));
        outsideTorso.subtract(new Area(torso));
        Area outsideHead = new Area(new Rectangle2D.Double(0, 0, w, h));
        outsideHead.subtract(new Area(head));
        Area body = new Area(torso);
        body.add(new Area(head));
        Area outsideBody = new Area(outsideTorso);
        outsideBody.subtract(new Area(head));

        g2.setStroke(new BasicStroke(scale));
        g2.setClip(outsideBody);
        g2.setColor(outline);
        // antennae
        g2.drawArc(-1 - pad, yMid - pad - 1, pad2 + 2, pad2 + 2, -90, 180);
        // front legs
        g2.drawArc(pad2 + 1, 0, h - 1, h - 1, 90, 180);
        // middle legs
        g2.drawArc(w / 2 + 1, 0, h - 1, h - 1, 90, 180);
        // back legs
        g2.drawArc(w - h / 2 + pad, 2, h - 6, h - 5, 90, 180);

        if (fill != null) {
            g2.setClip(null);
            g2.setColor(fill);
            g2.fill(body.createTransformedArea(
                AffineTransform.getTranslateInstance(0.5, 0.5)));

            // zigzags
            g2.setClip(torso);
            g2.setColor(contrast);
            g2.setStroke(new BasicStroke(pad * 0.7f));
            for (int num = 0, i = w - pad2; num++ < 3; i -= pad2) {
                Path2D.Double stripe = new Path2D.Double();
                int hhh = (h - pad2) / 4;
                stripe.moveTo(i + hhh, yMid - 2 * hhh);
                stripe.lineTo(i, yMid - hhh);
                stripe.lineTo(i + hhh, yMid);
                stripe.lineTo(i, yMid + hhh);
                stripe.lineTo(i + hhh, yMid + 2 * hhh);
                g2.draw(stripe);
            }
        }

        g2.setColor(outline);
        g2.setStroke(new BasicStroke(1));
        g2.setClip(outsideHead);
        g2.draw(torso);
        g2.setClip(outsideTorso);
        g2.draw(head);
    }

}
