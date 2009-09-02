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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.ui.web.reports.BarChart;
import net.sourceforge.processdash.util.StringUtils;

public class ProbeItemSizeHistogram extends BarChart {

    private static final String PARTS_LIST = "New_Objects_Prefix_List";

    private static final String METHODS = "/Methods";

    private static final String REL_SIZE = "/Relative Size";

    private static final List<String> SIZE_NAMES = Collections
            .unmodifiableList(Arrays.asList("Very Small", "Small", "Medium",
                "Large", "Very Large", "BLANK???"));

    private static final int UNCATEGORIZED_POS = SIZE_NAMES.size() - 1;

    private static final String SNIPPET_HEADER = "<HTML><head>\n"
            + "<script type='text/javascript' src='/lib/overlib.js'></script>\n"
            + "</head><body><p>\n";

    @Override
    protected void writeContents() throws IOException {
        if (isHtmlMode())
            out.write(SNIPPET_HEADER);
        super.writeContents();
        if (isHtmlMode())
            out.print("</p></body></html>");
    }

    @Override
    protected void buildData() {
        int[] histogram = new int[SIZE_NAMES.size()];

        DataContext data = getDataContext();
        ListData partsAdditions = ListData.asListData(data
                .getSimpleValue(PARTS_LIST));
        if (partsAdditions != null) {
            for (int i = 0; i < partsAdditions.size(); i++) {
                String path = asString(partsAdditions.get(i));
                double itemCount = asDoubleData(data.getSimpleValue(path
                        + METHODS));
                String relSize = asString(data.getSimpleValue(path + REL_SIZE));
                if (!StringUtils.hasValue(relSize) && itemCount == 0)
                    continue;

                int relSizePos = SIZE_NAMES.indexOf(relSize);
                if (relSizePos == -1)
                    relSizePos = UNCATEGORIZED_POS;
                if (itemCount == 0)
                    itemCount = 1;

                histogram[relSizePos]++;
            }
        }

        // if no items were present in the "[BLANK]" category, don't show it.
        int len = histogram.length;
        if (histogram[len-1] == 0)
            len--;

        ResultSet result = new ResultSet(len, 1);
        result.setColName(0, "");
        result.setColName(1, "Total # Items");

        for (int i = 0; i < len; i++) {
            result.setRowName(i + 1, SIZE_NAMES.get(i));
            result.setData(i + 1, 1, new DoubleData(histogram[i]));
        }
        this.data = result;
    }

    private double asDoubleData(SimpleData d) {
        if (d instanceof DoubleData)
            return ((DoubleData) d).getDouble();
        else
            return 0;
    }

    private String asString(Object d) {
        if (d == null)
            return null;
        if (d instanceof SaveableData)
            d = ((SaveableData) d).getSimpleValue();
        if (d instanceof SimpleData)
            return ((SimpleData) d).format();
        else
            return d.toString();
    }

}
