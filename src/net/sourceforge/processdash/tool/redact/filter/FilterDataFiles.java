// Copyright (C) 2012-2016 Tuma Solutions, LLC
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

import static net.sourceforge.processdash.data.repository.DataRepository.EQUALS_SIGN_REPL;

import java.util.List;

import net.sourceforge.processdash.tool.redact.RedactFilterData;
import net.sourceforge.processdash.tool.redact.RedactFilterIDs;
import net.sourceforge.processdash.tool.redact.RedactFilterUtils;
import net.sourceforge.processdash.tool.redact.EnabledFor;

public class FilterDataFiles implements LineBasedFilter {

    private RedactFilterData data;

    @EnabledFor(RedactFilterIDs.TASK_NAMES)
    private boolean removeTaskNames;

    private List<DataFileEntryFilter> filters;

    private String currentFilename;

    public void afterPropertiesSet() {
        filters = RedactFilterUtils.getExtensions(data,
            "redact-datafile-filter");
    }

    public boolean shouldFilter(String filename) {
        currentFilename = filename;
        return (filename.endsWith(".dat")
                && !"groups.dat".equalsIgnoreCase(filename)
                && !"datasetID.dat".equalsIgnoreCase(filename));
    }

    public String getString(String str) {
        if (str == null)
            return null;

        // don't change include directives
        if (str.startsWith("#"))
            return str;

        // commentsgenerally contain path names, discard if necessary
        if (str.startsWith("="))
            return removeTaskNames ? null : str;

        // find the equals sign on the line
        int eqPos = str.indexOf('=');
        if (eqPos == -1)
            return str;

        // parse out the key and value, and whether the val is read only
        String key = str.substring(0, eqPos).replace(EQUALS_SIGN_REPL, '=');

        boolean readOnly = false;
        if (eqPos + 1 < str.length() && str.charAt(eqPos + 1) == '=') {
            readOnly = true;
            eqPos++;
        }

        String value = str.substring(eqPos + 1);

        // give our filters an opportunity to tweak the value
        DataFileEntry e = new DataFileEntry(currentFilename, key, value);
        for (DataFileEntryFilter f : filters) {
            f.filter(e);
            if (e.getKey() == null)
                return null;
        }

        // if any filter modified the value, make a replacement output line
        if (e.isModified()) {
            StringBuilder newStr = new StringBuilder();
            newStr.append(e.getKey().replace('=', EQUALS_SIGN_REPL));
            newStr.append(readOnly ? "==" : "=");
            newStr.append(e.getValue());
            str = newStr.toString();
        }

        return str;
    }

}
