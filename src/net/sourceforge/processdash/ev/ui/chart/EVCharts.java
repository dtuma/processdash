// Copyright (C) 2008-2018 Tuma Solutions, LLC
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

import java.util.Map;


import org.jfree.data.xy.XYDataset;

public class EVCharts {

    /** A widget that displays a cumulative earned value chart */
    public static class Value extends AbstractEVTimeSeriesChart{

        public static final String ID = "pdash.ev.cumValueChart";

        @Override
        protected XYDataset createDataset(Map env, Map params) {
            return getSchedule(env).getValueChartData();
        }

    }

    /** A widget that displays a cumulative direct time chart */
    public static class DirectTime extends AbstractEVTimeSeriesChart {

        public static final String ID = "pdash.ev.cumDirectTimeChart";

        @Override
        protected XYDataset createDataset(Map env, Map params) {
            return getSchedule(env).getTimeChartData();
        }

    }

    /** A widget that displays a combined chart */
    public static class Combined extends AbstractEVTimeSeriesChart {

        public static final String ID = "pdash.ev.cumCombinedChart";

        @Override
        protected XYDataset createDataset(Map env, Map params) {
            return getSchedule(env).getCombinedChartData();
        }

    }

}
