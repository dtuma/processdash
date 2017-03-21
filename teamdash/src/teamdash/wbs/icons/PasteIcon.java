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

public class PasteIcon extends AbstractRecolorableIcon {

    private RecolorableIcon clipboard;

    private RecolorableIcon delegate;

    private float scale;

    private int dx, dy;

    public PasteIcon(RecolorableIcon delegate, float scale, int dx, int dy) {
        this.clipboard = new WBSImageIcon("paste.png");
        this.delegate = delegate;
        this.scale = scale;
        this.dx = dx;
        this.dy = dy;
        this.width = dx + (int) (0.5 + delegate.getIconWidth() * scale);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        clipboard.paintIcon(c, g, x, y);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(x + dx, y + dy);
        g2.scale(scale, scale);
        delegate.paintIcon(c, g2, 0, 0);
    }

}
