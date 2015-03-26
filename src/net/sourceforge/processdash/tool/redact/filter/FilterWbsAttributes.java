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

import java.util.List;

import net.sourceforge.processdash.tool.redact.RedactFilterData;
import net.sourceforge.processdash.tool.redact.RedactFilterUtils;

public class FilterWbsAttributes extends RedactFilterUtils implements
        LineBasedFilter {

    private RedactFilterData data;

    private List<DataFileEntryFilter> filters;

    private String currentFilename;

    public void afterPropertiesSet() {
        filters = RedactFilterUtils.getExtensions(data,
            "redact-wbs-attr-filter");
    }

    public boolean shouldFilter(String filename) {
        currentFilename = filename;

        // Note: we filter all of the WBS-style files, not just wbs.xml.
        // This is because people sometimes copy and paste nodes from one
        // model to another (for example, to create a workflow from a set of
        // existing WBS nodes).  This can cause models to contain unexpected
        // hidden attributes. So to be on the safe side, we apply attribute
        // filters to everything wbs-model-related.

        for (String suffix : WBS_MODEL_FILES)
            if (filename.endsWith(suffix))
                return true;
        return false;
    }

    private static final String[] WBS_MODEL_FILES = { "/wbs.xml",
            "/workflow.xml", "/milestones.xml" };

    public String getString(String str) {
        // only change <attr> tags
        String tag = getXmlTagName(str);
        if (!"attr".equals(tag))
            return str;

        String name = getXmlAttr(str, "name");
        String value = getXmlAttr(str, "value");

        // give our filters an opportunity to tweak the value
        DataFileEntry e = new DataFileEntry(currentFilename, name, value);
        for (DataFileEntryFilter f : filters) {
            f.filter(e);
            if (e.getKey() == null)
                return null;
        }

        // if any filter modified the value, make a replacement output line
        if (e.isModified()) {
            str = replaceXmlAttr(str, "name", e.getKey());
            str = replaceXmlAttr(str, "value", e.getValue());
        }

        return str;
    }

}
