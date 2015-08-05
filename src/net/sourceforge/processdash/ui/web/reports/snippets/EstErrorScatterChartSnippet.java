// Copyright (C) 2015 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.reports.snippets;

import java.io.IOException;

import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.snippet.SnippetEnvironment;

public class EstErrorScatterChartSnippet extends AbstractChartSnippet {

    @Override
    protected void writeContents() throws IOException {
        StringBuffer args = new StringBuffer();

        Resources res = (Resources) env.get(SnippetEnvironment.RESOURCES);
        appendParam(args, "title", res.getString("Title"));
        copyParam(args, "TargetPercent", "pct");

        buildData(res);
        appendParam(args, "useData", DATA_NAME);

        writeSmallChart("estErrorScatter", args.toString(), SMALL_ARGS);
    }

    private void buildData(Resources res) throws IOException {

        // create a result set to hold the data
        ResultSet data = new ResultSet(2, 6);
        data.setColName(0, res.getString("Component"));
        data.setColName(1, res.getString("Plan_Size"));
        data.setColName(2, res.getString("Actual_Size"));
        data.setColName(3, res.getString("Size_Est_Error"));
        data.setColName(4, res.getString("Plan_Time"));
        data.setColName(5, res.getString("Actual_Time"));
        data.setColName(6, res.getString("Time_Est_Error"));
        data.setFormat(3, "100%");
        data.setFormat(6, "100%");

        // load dummy data into the result set
        data.setRowName(1, "Component 1");
        data.setData(1, 1, new DoubleData(1));
        data.setData(1, 2, new DoubleData(1.5));
        data.setData(1, 3, new DoubleData(0.5));
        data.setData(1, 4, StringData.create("1:00"));
        data.setData(1, 5, StringData.create("1:30"));
        data.setData(1, 6, new DoubleData(0.5));

        data.setRowName(2, "Component 2");
        data.setData(2, 1, new DoubleData(2));
        data.setData(2, 2, new DoubleData(1));
        data.setData(2, 3, new DoubleData(-0.5));
        data.setData(2, 4, StringData.create("2:00"));
        data.setData(2, 5, StringData.create("1:00"));
        data.setData(2, 6, new DoubleData(-0.5));

        // store the result set into the repository
        ListData l = new ListData();
        l.add(data);
        getDataContext().putValue(DATA_NAME, l);
    }

    private static final String DATA_NAME = "EstErrorScatterChart///Data";

    private static final String SMALL_ARGS = "&hideTickLabels";

}
