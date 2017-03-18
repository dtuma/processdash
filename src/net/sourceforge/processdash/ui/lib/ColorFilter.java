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

import java.awt.Color;
import java.awt.image.RGBImageFilter;

import javax.swing.GrayFilter;

public abstract class ColorFilter extends RGBImageFilter {

    public ColorFilter() {
        canFilterIndexColorModel = true;
    }

    public Color recolor(Color c) {
        int rgb = c.getRGB();
        int newRgb = filterRGB(0, 0, rgb);
        return new Color(newRgb, true);
    }

    public static final ColorFilter Red = new ColorFilter() {

        @Override
        public int filterRGB(int x, int y, int rgb) {
            // Use NTSC conversion formula.
            int gray = (int) ((0.30 * ((rgb >> 16) & 0xff) + 0.59
                    * ((rgb >> 8) & 0xff) + 0.11 * (rgb & 0xff)) / 3);

            if (gray < 0)
                gray = 0;
            if (gray > 255)
                gray = 255;
            return (rgb & 0xffff0000) | (gray << 8) | (gray << 0);
        }

    };

    public static final ColorFilter Phantom = new ColorFilter() {

        @Override
        public int filterRGB(int x, int y, int rgb) {
            int alpha = rgb & 0xff000000;
            int red = filt((rgb >> 16) & 0xff);
            int green = filt((rgb >> 8) & 0xff);
            int blue = filt((rgb >> 0) & 0xff);

            return alpha | (red << 16) | (green << 8) | blue;
        }

        public int filt(int component) {
            return (component + 0xff) / 2;
        }

    };

    public static final ColorFilter Disabled = new ColorFilter() {
        GrayFilter grayFilter = new GrayFilter(true, 50);

        public int filterRGB(int x, int y, int rgb) {
            return grayFilter.filterRGB(x, y, rgb);
        }

    };

}
