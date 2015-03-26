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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ev.ui.chart;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

/**
 * A renderer used to add a tooltip on a line connecting 2 items. The tooltip will be the
 *  one used for the first item.
 */
public class TooltipLineXYLineAndShapeRenderer extends XYLineAndShapeRenderer {

    /** The minimum width of the tooltip area */
    private static final float MINIMUM_TOOLTIP_AREA_WIDTH = 4;

    public TooltipLineXYLineAndShapeRenderer(boolean lines, boolean shapes) {
        super(lines, shapes);
    }

    @Override
    protected void drawPrimaryLine(XYItemRendererState state,
                                   Graphics2D g2,
                                   XYPlot plot,
                                   XYDataset dataset,
                                   int pass,
                                   int series,
                                   int item,
                                   ValueAxis domainAxis,
                                   ValueAxis rangeAxis,
                                   Rectangle2D dataArea) {
        super.drawPrimaryLine(state, g2, plot, dataset, pass, series, item, domainAxis,
                rangeAxis, dataArea);

        EntityCollection entities = state.getEntityCollection();
        if (entities != null && item > 0) {
            Shape tooltipArea = getTooltipArea(state.workingLine, series);
            addEntity(entities, tooltipArea, dataset, series, item, 0, 0);
        }

    }

    /**
     * Creates and returns a polygon that has the same shape as the line passed
     *  as a parameter, but ticker so it can be used as a mouse-over tooltip area
     */
    private Shape getTooltipArea(Line2D line, int series) {
        GeneralPath area = new GeneralPath();

        float areaWidth = getAreaWidth(series);

        area.moveTo((float)line.getX1(), (float) (line.getY1() + areaWidth/2));
        area.lineTo((float)line.getX2(), (float) (line.getY2() + areaWidth/2));
        area.lineTo((float)line.getX2(), (float) (line.getY2() - areaWidth/2));
        area.lineTo((float)line.getX1(), (float) (line.getY1() - areaWidth/2));
        area.closePath();

        return area;
    }

    /**
     *  This method tries to get the line stroke's thickness to determine how big we need
     *   to draw the area. If it fails, or if the stroke thickness is not big enough, the
     *   minimum width will be used.
     */
    private float getAreaWidth(int series) {
        float areaWidth = MINIMUM_TOOLTIP_AREA_WIDTH;

        Stroke seriesStroke = getSeriesStroke(series);

        if (seriesStroke instanceof BasicStroke) {
            float lineWidth = ((BasicStroke) seriesStroke).getLineWidth();

            if (lineWidth > areaWidth)
                areaWidth = lineWidth;
        }

        return areaWidth;
    }

}
