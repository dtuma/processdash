// Copyright (C) 2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.psp;

import java.io.IOException;
import java.text.NumberFormat;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.util.TimeNumberFormat;

public class ProbeConsistencyCheck extends PspForEngBase {

    @Override
    protected void writeContents() throws IOException {
        DataContext data = getDataContext();

        out.println(HEADER);
        maybePrintOutputMismatch(data, "PROBE_Size_Output_Mismatch",
            "Estimated Added & Modified Size", SIZE_FMT, "Size_Mismatch_FMT");
        maybePrintOutputMismatch(data, "PROBE_Time_Output_Mismatch",
            "Estimated Time", TIME_FMT, "Time_Mismatch_FMT");
        out.println(FOOTER);
    }

    private void maybePrintOutputMismatch(DataContext data, String testElem,
            String elem, NumberFormat fmt, String resKey) throws IOException {
        if (getBoolean(data, testElem) == false)
            return;

        String probeMethod = getString(data, elem + "/Probe Method");
        double probeValue = getNumber(data, "PROBE_Last_Run_Value/" + elem);
        double userValue = getNumber(data, elem);
        if (Double.isNaN(probeValue) || Double.isNaN(userValue))
            return;

        String probeText = fmt.format(probeValue);
        String userText = fmt.format(userValue);
        String message = res().format(resKey, probeMethod, probeText, userText);
        printStudentDataError(message);
    }

    private boolean getBoolean(DataContext data, String dataName) {
        SimpleData sd = data.getSimpleValue(dataName);
        return (sd != null && sd.test());
    }

    private String getString(DataContext data, String dataName) {
        SimpleData sd = data.getSimpleValue(dataName);
        return (sd == null ? null : sd.format());
    }

    private double getNumber(DataContext data, String dataName) {
        SimpleData sd = data.getSimpleValue(dataName);
        if (sd instanceof NumberData)
            return ((NumberData) sd).getDouble();
        else
            return Double.NaN;
    }

    private static final NumberFormat TIME_FMT = new TimeNumberFormat();
    private static final NumberFormat SIZE_FMT = NumberFormat.getNumberInstance();
    static {
        SIZE_FMT.setMaximumFractionDigits(1);
    }


}
