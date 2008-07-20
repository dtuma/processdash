// Copyright (C) 2002-2003 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.probe.wizard;

import java.util.Iterator;
import java.util.Vector;

import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.util.HTMLUtils;


public class AveragingMethod extends ProbeMethod {

    protected int inputColumn;
    protected int xColumn;
    protected int yColumn;


    public AveragingMethod(ProbeData data, String letter,
                           MethodPurpose purpose, int xColumn) {
        super(data, letter, purpose);

        this.xColumn = xColumn;
        this.yColumn = purpose.getYColumn();

        this.inputColumn = purpose.mapInputColumn(xColumn);
        this.inputValue = data.getCurrentValue(inputColumn);
    }


    public void calc() {
        Vector dataPoints = histData.getXYDataPoints(xColumn, yColumn);

        double x_sum = 0;
        double y_sum = 0;

        for (Iterator i = dataPoints.iterator(); i.hasNext();) {
            double[] dataPoint = (double[]) i.next();
            x_sum += dataPoint[0];
            y_sum += dataPoint[1];
        }

        this.beta0 = 0;
        this.beta1 = y_sum / x_sum;
        this.outputValue = this.inputValue * this.beta1;

        rateBeta1();
        if (rating > 0)
            observations.add
                (resources.format("Method.Beta1_OK_FMT",
                                  methodPurpose.formatBeta1(beta1)));
    }


    protected void rateBeta1() {
        double expectedBeta1 = methodPurpose.getExpectedBeta1();
        double ratio = beta1 / expectedBeta1;

        if (badDouble(ratio)) {
            errorMessages.add(resources.getString("Method.Not_Enough_Data"));
            rating = CANNOT_CALCULATE;

        } else if (ratio > 2 || ratio < 0.5) {
            rating = SERIOUS_PROBLEM;
            String resKey =
                "Method." + methodPurpose.getKey() + ".Beta1_Problem_FMT";
            errorMessages.add
                (resources.format(resKey, methodPurpose.formatBeta1(beta1),
                                  methodPurpose.formatBeta1(expectedBeta1)));

        } else {
            rating += 1.0 - 2 * Math.abs(Math.log(ratio) / Math.log(2));
        }
    }


    protected String buildChartURL() {
        if (getRating() > CANNOT_CALCULATE) {
            StringBuffer url = new StringBuffer();
            url.append("../xy.class?for=%5b"+ProbeData.PROBE_LIST_NAME+"%5d");
            addParam(url, "title", getMethodName());
            addChartParam(url, 1, xColumn);
            addChartParam(url, 2, methodPurpose.getYColumn());
            addTrendParam(url);

            return url.toString();
        } else
            return null;
    }
    private void addChartParam(StringBuffer url, int dNum, int col) {
        String elemExpr = histData.getDataName(col, false);
        addParam(url, "d"+dNum, elemExpr);
        String header = histData.getResultSet().getColName(col);
        addParam(url, "h"+dNum, header);
        String elemName = histData.getDataName(col, true);
        addParam(url, "where", "[" + elemName + "] > 0");
    }


    protected void addParam(StringBuffer url, String name, String value) {
        url.append("&").append(HTMLUtils.urlEncode(name))
            .append("=").append(HTMLUtils.urlEncode(value));
    }

    protected void addTrendParam(StringBuffer url) {
        url.append("&trend=average");
    }

    protected String getTutorialLink() {
        ResultSet rs = histData.getResultSet();
        return Tutorial.getAverageLink
            (getPurposeLabel(), getMethodLetter(),
             rs.getColName(xColumn),
             rs.getColName(methodPurpose.getYColumn()),
             rs.getColName(inputColumn),
             rs.getColName(methodPurpose.getTargetColumn()));
    }

}
