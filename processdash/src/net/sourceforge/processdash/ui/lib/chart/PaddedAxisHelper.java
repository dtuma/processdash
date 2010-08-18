// Copyright (C) 2010 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib.chart;

import java.awt.geom.Rectangle2D;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.ui.RectangleEdge;

public class PaddedAxisHelper {

    protected ValueAxis axis;

    protected double upperPad;

    protected double lowerPad;

    public PaddedAxisHelper(ValueAxis axis) {
        this.axis = axis;
    }

    public double getUpperPad() {
        return upperPad;
    }

    public void setUpperPad(double upperPad) {
        this.upperPad = upperPad;
    }

    public double getLowerPad() {
        return lowerPad;
    }

    public void setLowerPad(double lowerPad) {
        this.lowerPad = lowerPad;
    }

    public Rectangle2D removePadding(Rectangle2D area, RectangleEdge edge) {
        boolean inverted = axis.isInverted();
        if (RectangleEdge.isTopOrBottom(edge)) {
            double xPad = inverted ? upperPad : lowerPad;
            double width = area.getWidth() - upperPad - lowerPad;
            return new Rectangle2D.Double(area.getX() + xPad, area.getY(),
                    width, area.getHeight());
        } else {
            double yPad = inverted ? lowerPad : upperPad;
            double height = area.getHeight() - upperPad - lowerPad;
            return new Rectangle2D.Double(area.getX(), area.getY() + yPad,
                    area.getWidth(), height);
        }
    }

}
