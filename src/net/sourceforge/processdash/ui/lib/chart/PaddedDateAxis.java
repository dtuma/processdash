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

import org.jfree.chart.axis.DateAxis;
import org.jfree.ui.RectangleEdge;

public class PaddedDateAxis extends DateAxis {

    private PaddedAxisHelper helper;

    public PaddedDateAxis() {
        this.helper = new PaddedAxisHelper(this);
    }

    public double getLowerPad() {
        return helper.getLowerPad();
    }

    public void setLowerPad(double lowerPad) {
        helper.setLowerPad(lowerPad);
    }

    public double getUpperPad() {
        return helper.getUpperPad();
    }

    public void setUpperPad(double upperPad) {
        helper.setUpperPad(upperPad);
    }

    @Override
    public double java2DToValue(double java2dValue, Rectangle2D area,
            RectangleEdge edge) {
        Rectangle2D noPad = helper.removePadding(area, edge);
        return super.java2DToValue(java2dValue, noPad, edge);
    }

    @Override
    public double valueToJava2D(double value, Rectangle2D area,
            RectangleEdge edge) {
        Rectangle2D noPad = helper.removePadding(area, edge);
        return super.valueToJava2D(value, noPad, edge);
    }

}
