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

package net.sourceforge.processdash.ui.lib.chart;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.FormatUtil;

import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.XYDataset;

public class XYControlLimitTooltipGenerator implements XYToolTipGenerator {
    private final Resources resources =
        Resources.getDashBundle("Analysis.Snippet.Charts.Control_limit");

    public String generateToolTip(XYDataset dataset, int series, int item) {
        double value = dataset.getYValue(series, item);
        String seriesName = XYControlLimitDataset.SERIES_RES_KEYS[series];

        return resources.getString(seriesName + ".Full_Name")
               + " = "
               + FormatUtil.getOneFractionDigitNumberFormat().format(value);
    }
}
