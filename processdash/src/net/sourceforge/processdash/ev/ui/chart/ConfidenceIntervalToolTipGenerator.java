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
import java.text.MessageFormat;
import java.text.NumberFormat;

import net.sourceforge.processdash.i18n.Resources;

import org.jfree.data.xy.XYDataset;

public abstract class ConfidenceIntervalToolTipGenerator extends EVXYToolTipGenerator {
    private static Resources resources =
        Resources.getDashBundle("EV.Snippet.Confidence_Interval_Chart");

    protected static final String TOOLTIP_FORMAT = "{0} {1} {2} = {3}";

    /** The formatter used to format the percentage value */
    private NumberFormat percentageFormat = null;

    public ConfidenceIntervalToolTipGenerator(DateFormat xFormat,
                                              NumberFormat percentageFormat) {
        super(TOOLTIP_FORMAT, xFormat, getNumberFormat());
        this.percentageFormat = percentageFormat;
    }

    public ConfidenceIntervalToolTipGenerator(NumberFormat xFormat,
                                              NumberFormat percentageFormat) {
        super(TOOLTIP_FORMAT, xFormat, getNumberFormat());
        this.percentageFormat = percentageFormat;
    }

    @Override
    protected Object[] createItemArray(XYDataset dataset, int series, int item) {
        // The result array has this format :
        //  [0] : Series name
        //  [1] : X value
        //  [2] : Y value as percentage (between 0 and 100)
        //
        // What we want to return is a object array with this format :
        //  [0] : Series name
        //  [1] : Percentage value
        //  [2] : Either LPI or UPI
        //  [3] : X value
        Object[] itemArray = super.createItemArray(dataset, series, item);
        double yValue = new Double((String)itemArray[2]).doubleValue();

        double percentageValue = (2 * Math.abs(50 - yValue)) / 100;
        String percentageInterval = yValue < 50 ? resources.getString("LPI") :
                                                  resources.getString("UPI");

        Object[] result = new Object[4];
        result[0] = itemArray[0];
        result[1] = this.percentageFormat.format(percentageValue);
        result[2] = percentageInterval;
        result[3] = itemArray[1];

        return result;
    }

    @Override
    public String generateLabelString(XYDataset dataset, int series, int item) {
        String result = null;
        Object[] items = createItemArray(dataset, series, item);
        double yValue = dataset.getYValue(series, item);

        if (yValue == 50) {
            // If the Y value is 50%, we return
            //  "(Series Name) Most Likely Value = (X value)".
            //  items[0] contains the series name
            //  items[3] contains the formatted X value
            result = items[0] + " " +
                     resources.getString("Most_Likely_Value") + " = " + items[3];
        }
        else {
            result = MessageFormat.format(getFormatString(), items);
        }

        return result;
    }
}
