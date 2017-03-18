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

import static java.awt.RenderingHints.KEY_STROKE_CONTROL;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Line2D;

import net.sourceforge.processdash.ui.lib.AbstractRecolorableIcon;

public class ProxyTableIcon extends AbstractRecolorableIcon {

    private Color header, background, grid, edge;

    public ProxyTableIcon() {
        this.header = new Color(76, 93, 134);
        this.background = Color.white;
        this.grid = Color.lightGray;
        this.edge = Color.black;
    }

    @Override
    protected void paintIcon(Graphics2D g2, Shape clip, float scale) {
        // fill the grid background
        int gridWidth = 14, gridHeight = 10;
        Graphics2D gg = (Graphics2D) g2.create(1, 4, gridWidth, gridHeight);
        gg.setColor(background);
        gg.fillRect(0, 0, gridWidth, gridHeight);

        // draw the grid
        gg.setRenderingHint(KEY_STROKE_CONTROL,
            RenderingHints.VALUE_STROKE_PURE);
        gg.setStroke(new BasicStroke(0.7f / scale));
        gg.setColor(grid);
        float delta = gridHeight / (scale > 1.3 ? 4f : 3f);
        for (float y = delta; y < gridHeight; y += delta)
            gg.draw(new Line2D.Float(0, y, gridWidth, y));
        delta = gridWidth / (scale > 1.3 ? 5f : 4f);
        for (float x = delta; x < gridWidth; x += delta)
            gg.draw(new Line2D.Float(x, 0, x, gridHeight));

        // paint the table header
        g2.setColor(header);
        g2.fillRect(1, 1, 14, 3);

        // paint the edge
        g2.setStroke(new BasicStroke(1 / scale));
        g2.setColor(edge);
        g2.drawRect(1, 1, 14, 13);
    }

}
