// Copyright (C) 2017 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

import java.awt.Color;
import java.awt.Graphics2D;

import net.sourceforge.processdash.ui.lib.AbstractPixelAwareRecolorableIcon;

public class ExpansionToggleIcon extends AbstractPixelAwareRecolorableIcon {

    private Color color;

    private boolean expand;

    public ExpansionToggleIcon(boolean expand) {
        this.width = this.height = 9;
        this.color = Color.black;
        this.expand = expand;
    }

    @Override
    protected void paintIcon(Graphics2D g2, int width, int height,
            float scale) {
        // Make the pixel size odd if it isn't already, to enable our lines to
        // be centered.
        int pixelSize = width;
        if ((pixelSize & 1) == 0)
            pixelSize--;
        int mid = pixelSize / 2;
        int pad = pixelSize / 4;
        int end = pixelSize - pad - 1;

        g2.setColor(color);
        g2.drawRect(0, 0, pixelSize - 1, pixelSize - 1);
        g2.drawLine(pad, mid, end, mid);
        if (expand)
            g2.drawLine(mid, pad, mid, end);
    }

}
