// Copyright (C) 2020 Tuma Solutions, LLC
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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.RGBImageFilter;
import java.io.ByteArrayInputStream;

import com.kitfox.svg.app.beans.SVGIcon;

import net.sourceforge.processdash.ui.lib.AbstractPixelAwareRecolorableIcon;
import net.sourceforge.processdash.ui.lib.RecolorableIcon;


public class ScalableSvgIcon extends AbstractPixelAwareRecolorableIcon {

    private SVGIcon svgIcon;

    private static int NAME_IDX = 1;

    public ScalableSvgIcon(int height, byte[] svgData) {
        this.svgIcon = new SVGIcon();
        svgIcon.setSvgURI(svgIcon.getSvgUniverse().loadSVG(
            new ByteArrayInputStream(svgData), "ScaledSvgIcon#" + NAME_IDX++));
        svgIcon.setAntiAlias(true);
        svgIcon.setScaleToFit(true);

        this.height = height;
        this.width = (int) (0.5 + (svgIcon.getIconWidth() * height //
                / svgIcon.getIconHeight()));
    }

    @Override
    protected void paintIcon(Graphics2D g2, int width, int height,
            float scale) {
        svgIcon.setPreferredSize(new Dimension(width, height));
        svgIcon.paintIcon(null, g2, 0, 0);
    }

    @Override
    public RecolorableIcon recolor(RGBImageFilter filter) {
        ScalableSvgIcon result = (ScalableSvgIcon) super.recolor(filter);
        result.addColorFilter(filter);
        return result;
    }

}
