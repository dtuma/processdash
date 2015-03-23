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

import java.text.DateFormat;
import java.text.NumberFormat;

import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.data.xy.XYDataset;

public abstract class EVXYToolTipGenerator extends StandardXYToolTipGenerator {
    private static final String DEFAULT_TOOLTIP_FORMAT = "{0}: ({1}, {2})";


    public EVXYToolTipGenerator(DateFormat xFormat, NumberFormat yFormat) {
        super(DEFAULT_TOOLTIP_FORMAT, xFormat, yFormat);
    }

    public EVXYToolTipGenerator(NumberFormat xFormat, NumberFormat yFormat) {
        super(DEFAULT_TOOLTIP_FORMAT, xFormat, yFormat);
    }

    public EVXYToolTipGenerator(String tooltipFormat,
                                DateFormat xFormat,
                                NumberFormat yFormat) {
        super(tooltipFormat, xFormat, yFormat);
    }

    public EVXYToolTipGenerator(String tooltipFormat,
                                NumberFormat xFormat,
                                NumberFormat yFormat) {
        super(tooltipFormat, xFormat, yFormat);
    }

    @Override
    protected Object[] createItemArray(XYDataset dataset, int series,
            int item) {
        Object[] result = super.createItemArray(dataset, series, item);
        result[0] = AbstractEVChart.getNameForSeries(dataset, series);
        return result;
    }
}
