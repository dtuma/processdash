// Copyright (C) 2003-2008 Tuma Solutions, LLC
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


package net.sourceforge.processdash.ev.ci;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.LinkedList;

import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import cern.jet.random.engine.RandomEngine;

public abstract class AbstractConfidenceInterval implements
        XMLPersistableConfidenceInterval {

    protected double input;

    /** Some confidence intervals are sensitive to the value they are
     * correcting.  Clients should call setInput() with the input value
     * before calling other methods on the confidence interval.
     */
    public void setInput(double input) {
        this.input = input;
    }



    /** Data structure to hold a pair of numbers indicating a historical
     * value.
     */
    public static class DataPoint {

        // Note: it is conceivable that some confidence intervals
        // (such as those based on multiple regression techniques)
        // could have historical data points made up of more than two
        // numbers.  Since we aren't using any such techniques right
        // now, we'll keep things simple.
        public double x, y;

        public DataPoint(double x, double y) {
            this.x = x;  this.y = y;
        }

        public double plan()   { return x; }
        public double x()      { return x; }

        public double actual() { return y; }
        public double y()      { return y; }

        public String toString() { return "[" + x + ",  " + y + "]"; }
    }



    /** The list of historical data points used to calculate this confidence
     * interval.
     */
    protected LinkedList dataPointsList = null;
    protected DataPoint[] dataPoints = null;
    protected int numSamples;


    /** The number of historical data points used to calculate this
     * confidence interval.
     */
    protected final int getNumPoints() {
        return (dataPoints != null ? dataPoints.length : 0);
    }


    /** Convenience function to retrieve a particular historical data point.
     */
    protected final DataPoint getPoint(int i) {
        return dataPoints[i];
    }


    /** Add a historical data point
     */
    public void addDataPoint(double x, double y) {
        if (dataPointsList == null) {
            if (dataPoints == null || dataPoints.length == 0)
                dataPointsList = new LinkedList();
            else
                dataPointsList = new LinkedList(Arrays.asList(dataPoints));
        }

        dataPointsList.add(new DataPoint(x, y));
    }

    /** This method should be called after the last historical data point
     * has been added.
     */
    public void dataPointsComplete() {
        if (dataPointsList == null) {
            dataPoints = new DataPoint[0];
            numSamples = 0;
        } else {
            dataPoints = new DataPoint[dataPointsList.size()];
            dataPoints = (DataPoint[]) dataPointsList.toArray(dataPoints);
            numSamples = dataPoints.length;
        }
    }

    public void debugPrintData() {
        for (int i = 0;   i < numSamples;   i++)
            System.out.println("\t" + dataPoints[i]);
    }

    /** Obtain the given quantile of the distribution used to produce this
     * confidence interval, and translate the input value using that
     * number.
     *
     * Subclasses can override this one method to instantly provide
     * definitions for getLPI, getUPI, getRandomValue, and getPrediction.
     * Alternatively, they can choose to directly override those methods.
     */
    public double getQuantile(double percentage) {
        return Double.NaN;
    }

    public double getPrediction() {
        return getQuantile(0.5);
    }


    /** Return the lower end of the confidence interval for a
     * particular confidence percentage.
     */
    public double getLPI(double percentage) {
        return getQuantile((1 - percentage) / 2);
    }


    /** Return the upper end of the confidence interval for a
     * particular confidence percentage.
     */
    public double getUPI(double percentage) {
        return getQuantile((1 + percentage) / 2);
    }


    /** Return a random value from the distribution upon which
     * this confidence interval is based.
     */
    public double getRandomValue(RandomEngine u) {
        return getQuantile(u.nextDouble());
    }

    private static final String PACKAGE = "net.sourceforge.processdash.ev.ci.";

    public void saveToXML(String tagName, StringBuffer result) {
        String type = getClass().getName();
        if (type.startsWith(PACKAGE))
            type = type.substring(PACKAGE.length());

        result.append("<").append(tagName)
            .append(" type='").append(type).append("' ");
        saveXMLAttributes(result);
        result.append(" />");
    }
    protected void saveXMLAttributes(StringBuffer result) {}

    public static ConfidenceInterval readFromXML(NodeList e) {
        if (e == null || e.getLength() == 0) return null;
        if (e.item(0) instanceof Element)
            return readFromXML((Element) e.item(0));
        else
            return null;
    }

    public static ConfidenceInterval readFromXML(Element e) {
        if (e == null) return null;

        String type = e.getAttribute("type");
        if (!XMLUtils.hasValue(type)) return null;

        Class c = null;
        try {
            c = Class.forName(PACKAGE + type);
        } catch (ClassNotFoundException cnfe) {}
        if (c == null) try {
            c = Class.forName(type);
        } catch (ClassNotFoundException cnfe) {
            return null;
        }

        try {
            Class[] paramTypes = new Class[] { Element.class };
            Constructor cnstr = c.getConstructor(paramTypes);
            return (ConfidenceInterval) cnstr.newInstance(new Object[] { e });
        } catch (Exception ex) {
            return null;
        }
    }
}
