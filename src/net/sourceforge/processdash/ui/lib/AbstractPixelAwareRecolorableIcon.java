// Copyright (C) 2017-2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public abstract class AbstractPixelAwareRecolorableIcon
        extends AbstractRecolorableIcon {

    protected Image buffered;

    protected int bufferedWidth = -1, bufferedHeight = -1;

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(x, y);
        if (zoom != null && zoom.getZoomLevel() != 1.0)
            g2.scale(zoom.getZoomLevel(), zoom.getZoomLevel());

        AffineTransform t = g2.getTransform();
        int pixelWidth = (int) (this.width * t.getScaleX());
        int pixelHeight = (int) (this.height * t.getScaleY());

        if (pixelWidth != bufferedWidth || pixelHeight != bufferedHeight) {
            buffered = new BufferedImage(pixelWidth, pixelHeight,
                    BufferedImage.TYPE_INT_ARGB);

            Graphics2D gb = (Graphics2D) buffered.getGraphics();
            if (antialias)
                gb.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            if (strokePure)
                gb.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);

            paintIcon(gb, pixelWidth, pixelHeight, (float) t.getScaleX());

            bufferedWidth = pixelWidth;
            bufferedHeight = pixelHeight;
        }

        g2.scale(1 / t.getScaleX(), 1 / t.getScaleY());
        g2.drawImage(buffered, 0, 0, null);
    }

    abstract protected void paintIcon(Graphics2D g2, int width, int height,
            float scale);

    @Override
    protected AbstractRecolorableIcon clone() {
        AbstractRecolorableIcon result = super.clone();
        ((AbstractPixelAwareRecolorableIcon) result).bufferedWidth = -1;
        return result;
    }

}
