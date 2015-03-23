// Copyright (C) 2002-2015 Tuma Solutions, LLC
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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class BufferedIcon extends ImageIcon {

    public BufferedIcon() {}

    public BufferedIcon(Icon delegate) {
        renderIcon(delegate);
    }

    protected void renderIcon(int width, int height) {
        renderIcon(null, width, height);
    }

    protected void renderIcon(Icon delegate) {
        renderIcon(delegate, -1, -1);
    }

    private void renderIcon(Icon delegate, int width, int height) {
        if (delegate != null) {
            width = delegate.getIconWidth();
            height = delegate.getIconHeight();
        }
        Image image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        Graphics imageG = image.getGraphics();
        if (imageG instanceof Graphics2D) {
            Graphics2D g2 = (Graphics2D) imageG;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        }
        if (delegate != null) {
            delegate.paintIcon(null, imageG, 0, 0);
        } else {
            doPaint(imageG);
        }
        setImage(image);
    }

    protected void doPaint(Graphics g) {}

    public void applyFilter(RGBImageFilter filter) {
        ImageProducer prod = new FilteredImageSource(getImage().getSource(),
                filter);
        setImage(Toolkit.getDefaultToolkit().createImage(prod));
    }

}
