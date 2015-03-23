// Copyright (C) 2002-2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.reports;

import java.util.Vector;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.LinearRegression;

import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;



/** 
 * Add a line to an XYDataSource.
 */
public class XYDataSourceTrendLine extends AbstractXYDataset {

    /** Used to specify if the trend line should start from the X/Y axis, or
     * from the X-value of the first data point. */
    public static final boolean START_LINE_AT_ZERO = true;

    private static final Double ZERO = new Double(0);

    /** The left and right coordinates of the line */
    protected Double minX, minY, maxX, maxY;

    /** What should the line be called, when mentioned in the legend? */
    protected String lineName;

    /** The XYDataSource that we are adding the line to. */
    protected XYDataset source;



    public XYDataSourceTrendLine(XYDataset src, String lineName) {
        this.source = src;
        this.lineName = lineName;
        minX = minY = maxX = maxY = ZERO;
    }

    public void setLine(double minX, double minY,
                        double maxX, double maxY) {
        this.minX = new Double(minX);
        this.minY = new Double(minY);
        this.maxX = new Double(maxX);
        this.maxY = new Double(maxY);
    }

    public void setLineSlope(double intercept, double slope,
                             double minX, double maxX) {
        setLine(minX, intercept + slope * minX,
                maxX, intercept + slope * maxX);
    }

    protected static boolean badValue(double d) {
        return Double.isNaN(d) || Double.isInfinite(d);
    }

    // DataSource interface

    @Override
    public int getSeriesCount() {
        return (badValue(minY.doubleValue()) ? 0 : 1);
    }

    @Override
    public Comparable getSeriesKey(int seriesIndex) {
        return (seriesIndex == 0 ? lineName : null);
    }

    @Override
    public void addChangeListener(DatasetChangeListener listener) {
        source.addChangeListener(listener);
    }
    @Override
    public void removeChangeListener(DatasetChangeListener listener) {
        source.removeChangeListener(listener);
    }

    // XYDataSource interface

    public Number getX(int seriesIndex, int itemIndex) {
        if (seriesIndex == 0)
            return (itemIndex == 0 ? minX : maxX);
        else
            return null;
    }

    public Number getY(int seriesIndex, int itemIndex) {
        if (seriesIndex == 0)
            return (itemIndex == 0 ? minY : maxY);
        else
            return null;
    }

    public int getItemCount(int seriesIndex) {
        if (seriesIndex == 0)
            return 2;
        else
            return 0;
    }

    private static final String DEFAULT_NAME =
        Resources.getGlobalBundle().getString("Trend");

    private static class RegressionLine extends XYDataSourceTrendLine
            implements DatasetChangeListener{

        /** The series number of the underlying dataset for which we calculate the regression
            line */
        private int seriesNum;

        /** Used to indicate if the regression line should start for 0 or from the first
            data point. */
        public boolean startFromZero;

        public RegressionLine(XYDataset src, int seriesNum, boolean startFromZero) {
            super(src, DEFAULT_NAME);
            super.addChangeListener(this);
            this.seriesNum = seriesNum;
            this.startFromZero = startFromZero;
            calculateLine();
        }

        private void calculateLine() {
            Vector data = new Vector();
            double minX = Double.NaN;
            double maxX = Double.NaN;
            int i = 0;
            if (source.getSeriesCount() > seriesNum)
                i = source.getItemCount(seriesNum);
            while (i-- > 0) try {
                double[] pair = new double[2];
                pair[0] = source.getXValue(seriesNum, i);
                pair[1] = source.getYValue(seriesNum, i);
                if (Double.isNaN(pair[0]) || Double.isInfinite(pair[0]) ||
                    Double.isNaN(pair[1]) || Double.isInfinite(pair[1]))
                    continue;
                else {
                    data.add(pair);
                    if (!(minX < pair[0])) minX = pair[0];
                    if (!(maxX > pair[0])) maxX = pair[0];
                }
            } catch (NullPointerException e) {}
            if (minX == maxX) {
                if (maxX > 0) minX = 0; else maxX = 0;
            }
            resetLine(new LinearRegression(data), minX, maxX);
        }

        protected void resetLine(LinearRegression regress,
                                 double minX, double maxX) {
            if (startFromZero)
                minX = (regress.beta0 < 0 ? regress.getXIntercept() : 0);
            setLineSlope(regress.beta0, regress.beta1, minX, maxX);
        }

        public void datasetChanged(DatasetChangeEvent event) {
            calculateLine();
        }
    }


    public static XYDataset getRegressionLine(XYDataset src) {
        return getRegressionLine(src, 0);
    }
    public static XYDataset getRegressionLine(XYDataset src, int seriesNum) {
        return getRegressionLine(src, seriesNum, false);
    }
    public static XYDataset getRegressionLine(XYDataset src,
                                              int seriesNum,
                                              boolean startFromZero) {
        return new RegressionLine(src, seriesNum, startFromZero);
    }

    private static class AverageLine extends RegressionLine {
        public AverageLine(XYDataset src, int num, boolean startFromZero) {
            super(src, num, startFromZero);
        }

        @Override
        protected void resetLine(LinearRegression regress,
                                 double minX, double maxX) {
            if (startFromZero)
                minX = 0;
            setLineSlope(0, regress.y_avg / regress.x_avg, minX, maxX);
        }
    }


    public static XYDataset getAverageLine(XYDataset src) {
        return getAverageLine(src, 0);
    }
    public static XYDataset getAverageLine(XYDataset src, boolean startFromZero) {
        return getAverageLine(src, 0, startFromZero);
    }
    public static XYDataset getAverageLine(XYDataset src, int seriesNum) {
        return getAverageLine(src, seriesNum, false);
    }
    public static XYDataset getAverageLine(XYDataset src,
                                           int seriesNum,
                                           boolean startFromZero) {
        return new AverageLine(src, seriesNum, startFromZero);
    }

    @Override
    public DatasetGroup getGroup() {
        return source.getGroup();
    }

    @Override
    public void setGroup(DatasetGroup group) {
        source.setGroup(group);
    }

}
