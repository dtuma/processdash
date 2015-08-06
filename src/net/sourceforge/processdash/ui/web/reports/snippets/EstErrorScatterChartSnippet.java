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

import static net.sourceforge.processdash.api.PDashQuery.FilterMode.CURRENT;
import static net.sourceforge.processdash.util.FormatUtil.formatTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.processdash.api.PDashQuery;
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
        copyParam(args, "CutoffPercent", "cut");

        buildData(res);
        appendParam(args, "useData", DATA_NAME);

        writeSmallChart("estErrorScatter", args.toString(), SMALL_ARGS);
    }

    private void buildData(Resources res) throws IOException {
        // query data from the database
        String[] hql = getTinyWebServer().getRequestAsString(
            "/dash/snippets/estErrorScatter.hql").split(";\\s*");
        PDashQuery query = getPdash().getQuery();
        List enactmentKeys = query.query(hql[0], getProjectKeys(), CURRENT);
        List<Object[]> taskStatus = query.query(hql[1], enactmentKeys, CURRENT);
        List<Object[]> sizeData = query.query(hql[2], enactmentKeys, CURRENT);

        // create a result set to hold the data
        ResultSet data = new ResultSet(taskStatus.size(), 7);
        data.setColName(0, res.getString("Component"));
        data.setColName(1, res.getString("Size_Units"));
        data.setColName(2, res.getString("Plan_Size"));
        data.setColName(3, res.getString("Actual_Size"));
        data.setColName(4, res.getString("Size_Est_Error"));
        data.setColName(5, res.getString("Plan_Time"));
        data.setColName(6, res.getString("Actual_Time"));
        data.setColName(7, res.getString("Time_Est_Error"));
        data.setFormat(4, "100%");
        data.setFormat(7, "100%");

        // load time data into the result set
        for (int i = 0; i < taskStatus.size(); i++) {
            Object[] oneTask = taskStatus.get(i);
            int row = i + 1;
            data.setRowName(row, (String) oneTask[1]);
            double planTime = ((Number) oneTask[2]).doubleValue();
            double actualTime = ((Number) oneTask[3]).doubleValue();
            data.setData(row, 5, StringData.create(formatTime(planTime)));
            data.setData(row, 6, StringData.create(formatTime(actualTime)));
            if (planTime > 0) {
                double timeErr = (actualTime - planTime) / planTime;
                data.setData(row, 7, new DoubleData(timeErr));
            }
        }

        // load size data into the result set
        for (Object[] oneSize : sizeData) {
            int row = getRow(taskStatus, oneSize[0]);
            if (row != -1) {
                String units = (String) oneSize[1];
                StringData currentUnits = (StringData) data.getData(row, 1);
                if (currentUnits == null)
                    data.setData(row, 1, StringData.create(units));
                else if (!units.equals(currentUnits.format()))
                    continue;

                int col = "Plan".equals(oneSize[2]) ? 2 : 3;
                double size = ((Number) oneSize[3]).doubleValue();
                data.setData(row, col, new DoubleData(size));
            }
        }

        // go back and calculate size estimating errors
        boolean requireSize = parameters.containsKey("RequireSize");
        for (int i = data.numRows(); i > 0; i--) {
            DoubleData plan = (DoubleData) data.getData(i, 2);
            DoubleData actual = (DoubleData) data.getData(i, 3);
            if (hasValue(plan) && hasValue(actual)) {
                double sizeError = (actual.getDouble() - plan.getDouble())
                        / plan.getDouble();
                data.setData(i, 4, new DoubleData(sizeError));
            } else if (requireSize) {
                data.removeRow(i);
            }
        }

        // store the result set into the repository
        ListData l = new ListData();
        l.add(data);
        getDataContext().putValue(DATA_NAME, l);
    }

    private List<Integer> getProjectKeys() {
        List<Integer> result = new ArrayList<Integer>();
        ListData l = ListData.asListData(getDataContext().getSimpleValue(
            "DB_Project_Keys"));
        if (l != null) {
            for (int i = l.size(); i-- > 0;)
                result.add(((DoubleData) l.get(i)).getInteger());
        }
        return result;
    }

    private int getRow(List<Object[]> taskStatus, Object enactmentKey) {
        for (int i = taskStatus.size(); i-- > 0;)
            if (taskStatus.get(i)[0].equals(enactmentKey))
                return i + 1;
        return -1;
    }

    private boolean hasValue(DoubleData d) {
        return d != null && d.getDouble() > 0;
    }

    private static final String DATA_NAME = "EstErrorScatterChart///Data";

    private static final String SMALL_ARGS = "&hideTickLabels";

}
