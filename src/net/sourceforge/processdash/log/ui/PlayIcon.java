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

package net.sourceforge.processdash.log.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;

import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.lib.AbstractPixelAwareRecolorableIcon;
import net.sourceforge.processdash.ui.lib.PaintUtils;

public class PlayIcon extends AbstractPixelAwareRecolorableIcon {

    public static PlayIcon black() {
        return new PlayIcon(Color.black, null);
    }

    public static PlayIcon glowing() {
        return new PlayIcon(Color.black, Color.green);
    }

    public static PlayIcon disabled() {
        return new PlayIcon(new Color(100, 100, 100), null);
    }


    private int pad;

    private Color fill, highlight, shadow, innerGlow;

    public PlayIcon(Color fill, Color glow) {
        this(DashboardIconFactory.getStandardIconSize(),
                DashboardIconFactory.getStandardIconPad(), //
                fill, glow);
    }

    public PlayIcon(int size, int pad, Color fill, Color glow) {
        this.width = this.height = size;
        this.pad = pad;
        this.fill = fill;
        this.highlight = Color.white;
        this.shadow = Color.gray;
        if (glow != null)
            this.innerGlow = PaintUtils.makeTransparent(glow, 80);
    }

    @Override
    protected void paintIcon(Graphics2D g2, int width, int height,
            float scale) {
        int pad = (int) (this.pad * width / (double) this.width);
        int left = pad;
        int right = width - pad - 1;
        int top = pad;
        int bottom = height - pad - 1;
        int mid = height / 2;

        // fill the background area
        Shape triangle = shape(left, top, left, bottom, right, mid);
        g2.setColor(fill);
        g2.setClip(triangle);
        g2.fill(triangle);

        // paint the glow, if applicable
        if (innerGlow != null) {
            g2.setColor(innerGlow);
            int cX = (left * 2 + right) / 3;
            for (int i = 8; i-- > 0;) {
                int r = (int) (i * mid / 10f);
                g2.fillOval(cX - r, mid - r, r * 2 + 1, r * 2 + 1);
            }
        }

        // prepare to draw the beveled inset edges on the triangle
        float bevelWidth = 2 + (float) Math.pow(width - 17, 0.8) / 3f;
        g2.setStroke(new BasicStroke(bevelWidth));
        int bevX = (left + left + right) / 3;

        // draw the shadow for the top and left edges
        g2.setColor(shadow);
        g2.draw(triangle);

        // draw the highlight on the bottom edge
        g2.setColor(highlight);
        g2.setClip(shape(left - 1, bottom + 1, bevX, mid, right + 1, mid));
        g2.draw(triangle);
    }

}
