// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

import com.jrefinery.chart.*;
import java.awt.Color;
import java.awt.BasicStroke;
import pspdash.data.DateData;

public class xy extends pspdash.CGIChartBase {

    private static Color[] NO_LINE = new Color[] { new Color(0, 0, 0, 0) };
    private static Color[] DEFAULT_COLS = new Color[] {
        Color.red, Color.blue, Color.green, Color.yellow, Color.cyan,
        Color.magenta, Color.orange, Color.pink, Color.lightGray };
    private static BasicStroke bs = new BasicStroke();
    private static BasicStroke[] DEFAULT_STROKE = new BasicStroke[] {
        bs, bs, bs, bs, bs, bs, bs, bs, bs };

    /** Create a  line chart. */
    public JFreeChart createChart() {
        JFreeChart chart;
        if ((data.numRows() > 0 && data.numCols() == 0 &&
             data.getData(1,1) instanceof DateData) ||
            parameters.get("xDate") != null)
            chart = JFreeChart.createTimeSeriesChart(data.xyDataSource());
        else
            chart = JFreeChart.createXYChart(data.xyDataSource());

        if (!chromeless) {
            String label = data.getColName(1);
            chart.getPlot().getAxis(Plot.HORIZONTAL_AXIS).setLabel(label);

            label = getSetting("yLabel");
            if (label == null && data.numCols() == 2)
                label = data.getColName(2);
            if (label == null) label = getSetting("units");
            if (label == null) label = "Value";
            chart.getPlot().getAxis(Plot.VERTICAL_AXIS).setLabel(label);
        }

        if (data.numCols() == 2)
            chart.setLegend(null);

        return chart;
    }

    public void massageParameters() {
        //parameters.put("order", parameters.get("d1"));
    }

}
