// Copyright (C) 2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.redact.filter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.tool.redact.RedactFilterIDs;
import net.sourceforge.processdash.tool.redact.EnabledFor;

@EnabledFor({ RedactFilterIDs.TASK_NAMES, RedactFilterIDs.NOTES })
public class FilterTaskListMetadata extends AbstractDataStringFilter {

    @EnabledFor(RedactFilterIDs.TASK_NAMES)
    private boolean stripTaskLists;

    @EnabledFor(RedactFilterIDs.NOTES)
    private boolean stripComments;

    @EnabledFor("/Task_List_Metadata$")
    public String filterMetadata(String metadata) {
        Properties p = new Properties();
        try {
            String val = StringData.unescapeString(metadata);
            p.load(new ByteArrayInputStream(val.getBytes("ISO-8859-1")));
        } catch (Exception e) {
            return null;
        }

        boolean madeChange = false;

        if (stripComments && p.remove("Schedule_Notes") != null)
            madeChange = true;

        if (stripTaskLists
                && p.remove("Forecast.Range.Historical_Data") != null)
            madeChange = true;

        if (madeChange == false)
            return metadata;

        if (p.isEmpty())
            return null;

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            p.store(out, "task list metadata");
            return StringData.escapeString(out.toString("ISO-8859-1"));
        } catch (IOException e) {
            return null;
        }
    }

}
