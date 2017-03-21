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

import static java.awt.RenderingHints.KEY_ANTIALIASING;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;

import net.sourceforge.processdash.ui.lib.AbstractRecolorableIcon;
import net.sourceforge.processdash.ui.lib.RecolorableIcon;

public class SortDatesIcon extends AbstractRecolorableIcon {

    private RecolorableIcon calendar;

    private Color gray, black;

    private Paint gradient;

    private Shape arrowhead;

    public SortDatesIcon() {
        this.calendar = new WBSImageIcon("calendar-32.png", "calendar-16.png");
        this.gray = new Color(0, 0, 0, 10);
        this.black = Color.black;
        finalizeColors();
        this.arrowhead = shape(19, 10, 23, 10, 21, 14);
        this.width = 24;
    }

    @Override
    protected void finalizeColors() {
        gradient = new GradientPaint(0, 1, gray, 0, 12, black);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        // draw the calendar
        calendar.paintIcon(c, g, x, y);

        // draw the arrow
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(x, y);

        // draw the shaft of the arrow
        g2.setStroke(new BasicStroke(1));
        g2.setPaint(gradient);
        g2.drawLine(21, 1, 21, 11);

        // draw the arrow head
        g2.setColor(black);
        g2.draw(arrowhead);
        g2.fill(arrowhead);
    }

}
