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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

import net.sourceforge.processdash.ui.lib.AbstractRecolorableIcon;
import net.sourceforge.processdash.ui.lib.RecolorableIcon;

public class CopyNodeIcon extends AbstractRecolorableIcon {

    private RecolorableIcon delegate;

    private float scale;

    private float padX, padY, deltaX, deltaY;

    /**
     * Create an icon representing a copy operation
     * 
     * @param delegate
     *            an icon representing the type of item that will be copied
     * @param scale
     *            a number less that 1.0 representing the percentage at which
     *            the miniaturized icons should be drawn
     * @param padX
     *            the number of pixels of left padding in this icon
     * @param padY
     *            the number of pixels to pad from the top
     * @param deltaX
     *            the horizontal distance between the first and second miniature
     *            icons
     * @param deltaY
     *            the vertical distance between the first and second miniature
     *            icons
     */
    public CopyNodeIcon(RecolorableIcon delegate, float scale, float padX,
            float padY, float deltaX, float deltaY) {
        this.delegate = delegate;
        this.scale = scale;
        this.padX = padX;
        this.padY = padY;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.width = (int) (2 * padX + deltaX + delegate.getIconWidth() * scale);
        this.height = (int) (2 * padY + deltaY + delegate.getIconHeight() * scale);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(x + padX, y + padY);
        g2.scale(scale, scale);
        delegate.paintIcon(c, g2, 0, 0);
        g2.translate(deltaX / scale, deltaY / scale);
        delegate.paintIcon(c, g2, 0, 0);
    }

}
