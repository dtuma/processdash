// Copyright (C) 2012-2023 Tuma Solutions, LLC
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

import java.util.Date;

import net.sourceforge.processdash.tool.redact.EnabledFor;
import net.sourceforge.processdash.tool.redact.RedactFilterIDs;
import net.sourceforge.processdash.tool.redact.RedactFilterUtils;

@EnabledFor(RedactFilterIDs.NOTES)
public class FilterDataContainingNotes extends AbstractDataStringFilter {

    @EnabledFor({ "^Team_Note", "/Team_Note" })
    public String deleteHierarchyNotes(String note) {
        return null;
    }

    @EnabledFor("/Description$")
    public String scrambleSizeDescriptions(String descr) {
        // this will scramble descriptions on each row of:
        //    * the PSP Size Estimating Template
        //    * the Team Project Size Inventory Form
        return RedactFilterUtils.hash(descr);
    }

    @EnabledFor("/Diff_Annotation_Data$")
    public String deleteSizeEstDiffAnnotationData(String data) {
        return null;
    }

    @EnabledFor({ "Task-Schedule/.*/Snapshot/", "^Snapshot/" })
    public String fixBaselineDescription(String data) {
        int tagEnd = data.indexOf('>') + 1;
        String tag = data.substring(0, tagEnd);

        try {
            String date = RedactFilterUtils.getXmlAttr(tag, "when");
            Date ts = new Date(Long.parseLong(date.substring(1)));
            String newName = "Baseline saved " + ts;
            tag = RedactFilterUtils.replaceXmlAttr(tag, "name", newName);
        } catch (Exception e) {
        }
        tag = RedactFilterUtils.discardXmlAttr(tag, "comment");

        return tag + data.substring(tagEnd);
    }

}
