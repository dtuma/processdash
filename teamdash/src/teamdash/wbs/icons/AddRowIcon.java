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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;

import net.sourceforge.processdash.ui.lib.AbstractRecolorableIcon;
import net.sourceforge.processdash.ui.lib.PaintUtils;

public class AddRowIcon extends AbstractRecolorableIcon {

    private Color white, green, edge;

    private Paint gradient;

    private SparkleIcon sparkle;

    public AddRowIcon() {
        this.white = Color.white;
        this.green = Color.green;
        this.edge = Color.black;
        finalizeColors();
        this.sparkle = new SparkleIcon(true, 3);
    }

    @Override
    protected void finalizeColors() {
        gradient = new GradientPaint(0, 4, green, //
                4, 9, PaintUtils.mixColors(green, Color.black, 0.5f));
    }

    @Override
    protected void paintIcon(Graphics2D g2, Shape clip, float scale) {
        g2.setColor(white);
        g2.fillRect(0, 0, 8, 13);

        g2.setStroke(new BasicStroke(1 / scale));
        g2.setColor(edge);
        g2.drawRect(0, 0, 8, 13);

        g2.setPaint(gradient);
        g2.fillRect(0, 4, 8, 5);
        g2.setColor(edge);
        g2.drawRect(0, 4, 8, 5);

        g2.translate(11, 11);
        sparkle.paintIcon(g2, clip, scale);
    }

}
