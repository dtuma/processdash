// Copyright (C) 2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.lib.chart;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnitSource;
import org.jfree.ui.RectangleEdge;

/** An axis used by the disc chart to show which value correspond to
 *   a certain disc size */
public class DiscLegendAxis extends NumberAxis {

    /**  The ticks marks' position for the legend axis are relative to the disc area.
     *    By definition, the space between the ticks will shrink exponentially
     *    (discSarea = discRadius * discRadius). That means that we have to come up
     *    with a different way of calculating the space between tick marks. If we
     *    multiply 1/dataArea by 11000, the ticks marks are properly spaced and do not
     *    overlap. */
    private static final double DATA_AREA_MULTIPLIER = 11000.0;

    /** The DiscItemDistributor that is used to fetch information on
         the disc, for instance, the scale at which they are drawn.*/
    private AbstractDiscItemDistributor discItemDistributor = null;

    public DiscLegendAxis(TickUnitSource legendTickUnits) {
        super();
        setStandardTickUnits(legendTickUnits);
    }

    public void setDiscItemDistributor(AbstractDiscItemDistributor discItemDistributor) {
        this.discItemDistributor = discItemDistributor;
    }

    /**
     * Converts a coordinate in Java2D space on the legend axis to the corresponding
     *  disc area.
     */
    @Override
    public double java2DToValue(double java2DValue,
                                Rectangle2D area,
                                RectangleEdge edge) {

        if (discItemDistributor == null) {
            // This should not happen since prior to using a legend axis, a DiscPlot
            //  should set a DiscItemDistributor.
            return 0.0;
        }

        double scaledDiameter = area.getMaxY() - java2DValue;
        double discDiameter = scaledDiameter / discItemDistributor.getScale();
        double discRadius = discDiameter / 2;
        double value = Math.PI * discRadius * discRadius;

        return value;
    }

    /**
     * Converts a disc area to a coordinate in Java2D space on the legend axis.
     */
    @Override
    public double valueToJava2D(double value,
                                Rectangle2D area,
                                RectangleEdge edge) {

        if (discItemDistributor == null) {
            // This should not happen since prior to using a legend axis, a DiscPlot
            //  should set a DiscItemDistributor.
            return 0.0;
        }

        double discRadius = Math.sqrt(Math.abs(value) / Math.PI);
        double discDiameter = 2 * discRadius;
        double scaledDiameter = discDiameter * discItemDistributor.getScale();
        double java2DValue = area.getMaxY() - scaledDiameter;

        return java2DValue;
    }

    @Override
    protected void selectVerticalAutoTickUnit(Graphics2D g2,
                                              Rectangle2D dataArea,
                                              RectangleEdge edge) {
        // This division seems to generate a good looking result
        double amountBetweenTicks = 1 / dataArea.getHeight() * DATA_AREA_MULTIPLIER;
        NumberTickUnit unit = (NumberTickUnit) getStandardTickUnits().
                                getCeilingTickUnit(amountBetweenTicks);

        setTickUnit(unit, false, false);
    }

    /**
     * The superclass' drawAxisLine() method draws the axis line for the entire
     *  dataArea's length. Since we want the line to end at the height of the maximum
     *  shown tick, we create a new Rectangle2D with the appropriate dimensions.
     */
    @Override
    protected void drawAxisLine(Graphics2D g2,
                                double cursor,
                                Rectangle2D dataArea,
                                RectangleEdge edge) {

        // In the original flow of events, the refreshing of the ticks is made after
        //  drawing the axis line. Since this implementation needs the ticks to be
        //  set properly, we force a tick refresh.
        AxisState state = new AxisState(cursor);
        refreshTicks(g2, state, dataArea, edge);

        double amountBetweenTicks = getTickUnit().getSize();
        int tickCount = calculateVisibleTickCount() - 1;
        double lowestTickValue = calculateLowestVisibleTickValue();

        double minY = valueToJava2D(lowestTickValue + tickCount * amountBetweenTicks,
                                    dataArea,
                                    edge);
        double maxY = valueToJava2D(lowestTickValue, dataArea, edge);
        double height = maxY - minY;

        Rectangle2D axisLineArea = new Rectangle2D.Double(dataArea.getX(),
                                                          minY,
                                                          dataArea.getWidth(),
                                                          height);

        super.drawAxisLine(g2, cursor, axisLineArea, edge);
    }

}
