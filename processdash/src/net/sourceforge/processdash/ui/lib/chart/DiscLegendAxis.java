// Copyright (C) 2008 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.lib.chart;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnitSource;
import org.jfree.data.Range;
import org.jfree.ui.RectangleEdge;

/** An axis used by the disc chart to show which value correspond to
 *   a certain disc size */
public class DiscLegendAxis extends NumberAxis {

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

        // the font used to display tick marks requires labels to be placed
        // at least this far apart.
        double tickLabelHeight = estimateMaximumTickLabelHeight(g2);

        // our tick marks get closer together as the values get larger. So
        // if we have a collision, it would be at the very top end of the
        // axis. We want to calculate a tick unit that will avoid that
        // collision. So first, we will find the pixel position of a
        // hypothetical tick mark drawn for the largest domain value.
        Range r = getRange();
        double maxValue = r.getUpperBound();
        double maxY = valueToJava2D(maxValue, dataArea, edge);

        // So far, we've identified a hypothetical tick mark placed at the
        // highest known domain value. If we drew another tick mark just
        // underneath it (one tick label height away), what domain value would
        // that hypothetical second-to-largest tick mark represent?
        double nextY = maxY + tickLabelHeight;
        double nextValue = java2DToValue(nextY, dataArea, edge);

        // calculate the numerical difference between these two domain values.
        // Then pick the tick unit that is larger, to avoid collisions.
        double amountBetweenTicks = maxValue - nextValue;
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
