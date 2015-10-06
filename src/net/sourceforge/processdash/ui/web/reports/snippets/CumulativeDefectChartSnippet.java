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
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import net.sourceforge.processdash.api.PDashQuery;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.DateUtils;

public class CumulativeDefectChartSnippet extends AbstractChartSnippet {

    private static final Resources resources = Resources
            .getDashBundle("Defects.CumDefects");

    @Override
    protected void writeContents() throws IOException {
        StringBuffer args = new StringBuffer();

        appendParam(args, "title", resources.getString("Snippet_Name"));
        appendParam(args, "yLabel", resources.getString("Y_Label"));

        buildData();

        appendParam(args, "useData", DATA_NAME);
        appendParam(args, "skipRowHdr", "true");

        writeSmallChart("xy", args.toString(), SMALL_ARGS);
    }

    private void buildData() throws IOException {
        // query data from the database
        String hql = getTinyWebServer().getRequestAsString(
            "/dash/snippets/cumDefects.hql");
        PDashQuery query = getPdash().getQuery();
        List<Object[]> defectCounts = query.query(hql);

        // Calc the number of rows we expect in our result set: one per day
        int numRows;
        if (defectCounts.isEmpty()) {
            numRows = 0;
        } else if (defectCounts.size() == 1) {
            numRows = 2;
        } else {
            Date start = (Date) defectCounts.get(0)[0];
            Date end = (Date) defectCounts.get(defectCounts.size() - 1)[0];
            long len = end.getTime() - start.getTime() + DAY_DELTA;
            numRows = (int) (len / DateUtils.DAYS) + 1;
        }

        // create a result set to hold the data
        ResultSet data = new ResultSet(numRows, 3);
        data.setColName(0, resources.getString("Date"));
        data.setColName(1, resources.getString("Date"));
        data.setColName(2, resources.getString("Count"));
        data.setColName(3, resources.getString("Cumulative"));

        // load defect data into the result set
        int row = 0;
        int cum = 0;
        Calendar lastDate = Calendar.getInstance();
        for (int i = 0; i < defectCounts.size(); i++) {
            Object[] oneCount = defectCounts.get(i);
            Date d = (Date) oneCount[0];
            int num = ((Number) oneCount[1]).intValue();

            if (i == 0) {
                // add a "zero" point preceding the first row
                lastDate.setTime(d);
                lastDate.add(Calendar.DATE, -1);
                addRow(data, ++row, lastDate.getTime(), 0, 0);

            } else {
                // add extra rows for days when no defects were removed
                long datePadCutoff = d.getTime() - DAY_DELTA;
                while (datePadCutoff > lastDate.getTimeInMillis()) {
                    lastDate.add(Calendar.DATE, 1);
                    addRow(data, ++row, lastDate.getTime(), 0, cum);
                }
            }

            // now add a row for the current data point
            cum += num;
            lastDate.setTime(d);
            addRow(data, ++row, d, num, cum);
        }

        // store the result set into the repository
        ListData l = new ListData();
        l.add(data);
        getDataContext().putValue(DATA_NAME, l);
    }

    protected void addRow(ResultSet data, int row, Date d, int num, int cum) {
        DateData date = new DateData(d, true);
        date.setFormatAsDateOnly(true);
        data.setRowName(row, date.format());
        data.setData(row, 1, date);
        data.setData(row, 2, new DoubleData(num));
        data.setData(row, 3, new DoubleData(cum));
    }

    private static final String DATA_NAME = "CumDefectChart///Data";

    private static final String SMALL_ARGS = "&hideTickLabels&hideLegend";

    // a constant that is slightly longer than a day, to accomodate days that
    // are longer due to a daylight savings change
    private static final long DAY_DELTA = 26 * DateUtils.HOURS;

}
