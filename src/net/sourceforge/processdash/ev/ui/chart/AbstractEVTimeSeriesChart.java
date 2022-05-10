// Copyright (C) 2008-2022 Tuma Solutions, LLC
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

import java.text.DateFormat;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

import net.sourceforge.processdash.ev.EVDateFormatOverride;
import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ui.lib.chart.XYDatasetFilter;
import net.sourceforge.processdash.util.FormatUtil;


public abstract class AbstractEVTimeSeriesChart extends AbstractEVXYChart {

    @Override
    protected JFreeChart getXYChartObject(XYDataset data) {
        return maybeChangeDateTickUnits(ChartFactory.createTimeSeriesChart(null,
            null, null, data, true, true, false));
    }

    @Override
    protected XYItemRenderer createRenderer(JFreeChart chart) {
        return createRangeXYItemRenderer();
    }

    @Override
    protected XYDataset getAdjustedData(XYDataset data) {
        return new XYDatasetFilter(data);
    }

    @Override
    protected ChartPanel getChartPanel(JFreeChart chart, XYDataset data) {
        return new EVHiddenOrShownSeriesXYChartPanel(chart, (XYDatasetFilter) data);
    }

    @Override
    protected XYToolTipGenerator getTooltipGenerator() {
        return new EVTimeSeriesTooltipGenerator();
    }

    /**
     * Temporary method used by EVReport to build a chart
     */
    public static JFreeChart createEVReportChart(XYDataset data) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart
                    (null, null, null, data, true, true, false);

        RangeXYItemRenderer renderer = new RangeXYItemRenderer();
        renderer.putAllSeriesPaints(SERIES_PAINTS);
        renderer.putAllSeriesStrokes(SERIES_STROKES);
        renderer.setLegendItemLabelGenerator(new SeriesNameGenerator());
        renderer.setBaseToolTipGenerator(new EVTimeSeriesTooltipGenerator());

        chart.getXYPlot().setRenderer(renderer);
        return maybeChangeDateTickUnits(chart);
    }

    public static class EVTimeSeriesTooltipGenerator extends EVXYToolTipGenerator {

        public EVTimeSeriesTooltipGenerator() {
            super(EVSchedule.getDateFormatter(),
                  FormatUtil.getOneFractionDigitNumberFormat());
        }

    }

    private static JFreeChart maybeChangeDateTickUnits(JFreeChart chart) {
        try {
            maybeChangeAxisDateTickUnits(
                (DateAxis) chart.getXYPlot().getDomainAxis());
        } catch (Exception e) {
        }

        return chart;
    }

    public static void maybeChangeAxisDateTickUnits(DateAxis axis) {
        DateFormat formatOverride = EVDateFormatOverride.getDateFormatOverride();
        if (formatOverride == null)
            return;

        // if EVDemoConfig is overriding the date format, we should adopt
        // the same format for any ticks that previously contained a year
        try {
            TickUnits oldTicks = (TickUnits) axis.getStandardTickUnits();
            TickUnits newTicks = new TickUnits();
            for (int i = 0; i < oldTicks.size(); i++) {
                DateTickUnit oneTick = (DateTickUnit) oldTicks.get(i);
                DateTickUnitType oneTickUnitType = oneTick.getUnitType();
                if (oneTickUnitType.equals(DateTickUnitType.MONTH)) {
                    // apply the format override for all "month" tick units
                    DateTickUnit newTick = new DateTickUnit(oneTickUnitType,
                            oneTick.getMultiple(), oneTick.getRollUnitType(),
                            oneTick.getRollMultiple(), formatOverride);
                    newTicks.add(newTick);

                } else if (oneTickUnitType.equals(DateTickUnitType.YEAR)) {
                    // don't add any tick units for year marks and higher

                } else {
                    // add other tick units as-is
                    newTicks.add(oneTick);
                }
            }

            axis.setStandardTickUnits(newTicks);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
