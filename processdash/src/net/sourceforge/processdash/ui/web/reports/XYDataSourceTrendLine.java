// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.ui.web.reports;

import java.util.Vector;

import net.sourceforge.processdash.util.LinearRegression;

import org.jfree.data.DatasetChangeListener;
import org.jfree.data.DatasetGroup;
import org.jfree.data.XYDataset;



/** Add a line to an XYDataSource.
 *
 * Note: Change notification is not yet supported.
 */
public class XYDataSourceTrendLine implements XYDataset {

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

    public int getSeriesCount() {
        return (badValue(minY.doubleValue()) ? 0 : 1);
    }

    public String getSeriesName(int seriesIndex) {
        return (seriesIndex == 0 ? lineName : null);
    }

    public void addChangeListener(DatasetChangeListener listener) {
        source.addChangeListener(listener);
    }
    public void removeChangeListener(DatasetChangeListener listener) {
        source.removeChangeListener(listener);
    }

    // XYDataSource interface

    public Number getXValue(int seriesIndex, int itemIndex) {
        if (seriesIndex == 0)
            return (itemIndex == 0 ? minX : maxX);
        else
            return null;
    }

    public Number getYValue(int seriesIndex, int itemIndex) {
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

    private static class RegressionLine extends XYDataSourceTrendLine {
        public RegressionLine(XYDataset src, int seriesNum) {
            super(src, "Trend");

            Vector data = new Vector();
            double minX = Double.NaN, maxX = Double.NaN;
            int i = src.getItemCount(seriesNum);
            while (i-- > 0) try {
                double[] pair = new double[2];
                pair[0] = src.getXValue(seriesNum, i).doubleValue();
                pair[1] = src.getYValue(seriesNum, i).doubleValue();
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
            setLineSlope(regress.beta0, regress.beta1, minX, maxX);
        }
    }


    public static XYDataset getRegressionLine(XYDataset src) {
        return getRegressionLine(src, 0);
    }
    public static XYDataset getRegressionLine(XYDataset src, int seriesNum) {
        return new RegressionLine(src, seriesNum);
    }

    private static class AverageLine extends RegressionLine {
        public AverageLine(XYDataset src, int num) { super(src, num); }
        protected void resetLine(LinearRegression regress,
                                 double minX, double maxX) {
            setLineSlope(0, regress.y_avg / regress.x_avg, minX, maxX);
        }
    }


    public static XYDataset getAverageLine(XYDataset src) {
        return getAverageLine(src, 0);
    }
    public static XYDataset getAverageLine(XYDataset src, int seriesNum) {
        return new AverageLine(src, seriesNum);
    }

    public DatasetGroup getGroup() {
        return source.getGroup();
    }

    public void setGroup(DatasetGroup group) {
        source.setGroup(group);
    }

}
