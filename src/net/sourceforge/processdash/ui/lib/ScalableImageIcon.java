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

package net.sourceforge.processdash.ui.lib;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.RGBImageFilter;

import javax.swing.GrayFilter;
import javax.swing.ImageIcon;

/**
 * This Icon manages one or more images of different sizes, and automatically
 * scales its output to achieve a good appearance for screens with different DPI
 * settings.
 * 
 * Java 9 includes new support for HiDPI monitors. On a HiDPI monitor, Swing
 * components are scaled up to match the size of native windows. Unfortunately,
 * ImageIcons are scaled in a naiive manner, resulting in very poor, blocky
 * appearance if the scaling factor is anything other than 100%.
 * 
 * This class accepts a variety of images of different sizes, including at least
 * one very-high-resolution image (which should be the first image provided to
 * the constructor). When it comes time to draw the icon, it will detect the
 * scaling factor in effect and choose the best image to display, automatically
 * resizing the image if necessary.
 */
public class ScalableImageIcon implements RecolorableIcon {

    private int width, height;

    private ImageIcon[] delegates;

    private ImageFilter[] filters;

    private int lastWidth, lastHeight;

    private ImageIcon scaledImage;

    /**
     * Create a new auto-scaling image icon
     * 
     * @param height
     *            the target height of this icon, in Java2D "user space." If
     *            graphic scaling is at 100%, the icon will be this many pixels
     *            tall. (The target width of this icon will be calculated
     *            automatically, based on the aspect ratio of the first image
     *            given.)
     * @param images
     *            one or more images of different sizes. Ideally, the first icon
     *            should be a high-resolution image that is larger than you
     *            expect the icon ever to appear. Other icons of different sizes
     *            can optionally follow in any order. When drawing, one of the
     *            icons will be selected if it is exactly the right size;
     *            otherwise the smallest icon that is larger than the target
     *            size will be scaled down to the appropriate size and drawn.
     */
    public ScalableImageIcon(int height, ImageIcon... images) {
        this.height = height;
        this.width = height * images[0].getIconWidth()
                / images[0].getIconHeight();
        this.delegates = images;
        this.filters = null;
        this.lastWidth = this.lastHeight = -1;
    }

    /**
     * Convenience constructor when a number of images are in the classpath, in
     * the same package as a particular class.
     * 
     * @param height
     *            the target height of this icon, in "user space."
     * @param base
     *            a class from the package where the icon files are stored
     * @param iconNames
     *            the names of the icon files, in the package above
     */
    public ScalableImageIcon(int height, Class base, String... iconNames) {
        this(height, loadIcons(base, iconNames));
    }

    private static ImageIcon[] loadIcons(Class base, String[] iconNames) {
        ImageIcon[] result = new ImageIcon[iconNames.length];
        for (int i = iconNames.length; i-- > 0;)
            result[i] = new ImageIcon(base.getResource(iconNames[i]));
        return result;
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g;
        AffineTransform t = g2.getTransform();
        g2.translate(x, y);

        int desiredWidth = (int) (t.getScaleX() * width);
        int desiredHeight = (int) (t.getScaleY() * height);

        if (lastWidth != desiredWidth || lastHeight != desiredHeight) {
            scaledImage = getScaledIcon(desiredWidth, desiredHeight);
            lastWidth = desiredWidth;
            lastHeight = desiredHeight;
        }

        g2.scale(1 / t.getScaleX(), 1 / t.getScaleY());
        scaledImage.paintIcon(c, g2, 0, 0);

        g2.setTransform(t);
    }

    private ImageIcon getScaledIcon(int desiredWidth, int desiredHeight) {
        // Look for the best icon from our list of delegates
        ImageIcon best = delegates[0];
        int bestHeight = best.getIconHeight();
        for (ImageIcon i : delegates) {
            int oneHeight = i.getIconHeight();
            if (i.getIconWidth() == desiredWidth && oneHeight == desiredHeight) {
                // we found an icon that is exactly the right size!
                if (filters == null)
                    // if no filter is in place, return the icon unchanged.
                    return i;
                else
                    // create a filtered version of the unscaled icon.
                    return new ImageIcon(runFilters(i));

            } else if (oneHeight >= desiredHeight && oneHeight < bestHeight) {
                // keep track of the smallest icon we find that is larger
                // than the desired size.
                best = i;
                bestHeight = oneHeight;
            }
        }

        // use the best icon we found. apply any filter if needed, then resize.
        Image img = runFilters(best);
        return new ImageIcon(img.getScaledInstance(desiredWidth, desiredHeight,
            Image.SCALE_SMOOTH));
    }

    private Image runFilters(ImageIcon i) {
        Image img = i.getImage();
        if (filters != null) {
            for (ImageFilter filter : filters)
                img = Toolkit.getDefaultToolkit().createImage(
                    new FilteredImageSource(img.getSource(), filter));
        }
        return img;
    }

    @Override
    public ScalableImageIcon recolor(RGBImageFilter filter) {
        ScalableImageIcon result = new ScalableImageIcon(height, delegates);
        int filtLen = (filters == null ? 0 : filters.length);
        result.filters = new ImageFilter[filtLen + 1];
        if (filtLen > 0)
            System.arraycopy(this.filters, 0, result.filters, 0, filtLen);
        result.filters[filtLen] = filter;
        return result;
    }

    public ScalableImageIcon getDisabledIcon() {
        return recolor(new GrayFilter(true, 50));
    }

}
