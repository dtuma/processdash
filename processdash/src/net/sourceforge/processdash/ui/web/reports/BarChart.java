// PSP Dashboard - Data Automation Tool for PSP-like processes
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.web.reports;


import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;

import pspdash.CGIChartBase;


public class BarChart extends CGIChartBase {

    /** Create a bar chart. */
    public JFreeChart createChart() {
        //if (data.numCols() == 1) data = data.transpose();
        JFreeChart chart;
        boolean vertical = true; // default
        String direction = getParameter("dir");
        if ((direction != null && direction.toLowerCase().startsWith("hor"))
            || parameters.get("horizontal") != null)
            vertical = false;
        chart = ChartFactory.createBarChart3D
            (null, null, null, data.catDataSource(),
             (vertical ? PlotOrientation.VERTICAL : PlotOrientation.HORIZONTAL),
             true, false, false);

        setupCategoryChart(chart);

        return chart;
    }

}
