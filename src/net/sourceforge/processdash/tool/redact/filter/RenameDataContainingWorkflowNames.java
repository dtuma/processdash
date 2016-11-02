// Copyright (C) 2016 Tuma Solutions, LLC
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

import net.sourceforge.processdash.tool.redact.EnabledFor;
import net.sourceforge.processdash.tool.redact.HierarchyNameMapper;
import net.sourceforge.processdash.tool.redact.RedactFilterIDs;

@EnabledFor(RedactFilterIDs.WORKFLOWS)
public class RenameDataContainingWorkflowNames implements DataFileEntryFilter {

    private HierarchyNameMapper nameMapper;

    public void filter(DataFileEntry e) {
        String key = e.getKey();
        if (!key.startsWith(DATANAME_PREFIX))
            return;

        int end = key.indexOf('/', DATANAME_PREFIX.length());
        if (end == -1)
            return;

        String name = key.substring(DATANAME_PREFIX.length(), end);
        if (name.startsWith("WF:"))
            return;

        e.setKey(DATANAME_PREFIX + nameMapper.getString(name)
                + key.substring(end));
    }

    private static final String DATANAME_PREFIX = "Workflow_Prefs/";

}
