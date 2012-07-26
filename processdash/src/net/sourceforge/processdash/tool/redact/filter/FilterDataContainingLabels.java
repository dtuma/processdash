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

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.tool.redact.EnabledFor;
import net.sourceforge.processdash.tool.redact.LabelMapper;
import net.sourceforge.processdash.tool.redact.RedactFilterIDs;

@EnabledFor(RedactFilterIDs.LABELS)
public class FilterDataContainingLabels extends AbstractDataStringFilter {

    private LabelMapper labelMapper;

    @EnabledFor("^Synchronized_Task_Labels$")
    public String scrambleTaskLabels(String value) {
        List<String> labelData = StringData.create(value).asList().asList();
        ListData newVal = new ListData();
        for (String elem : labelData) {
            if (elem.startsWith(LABEL_PREFIX)) {
                String label = elem.substring(LABEL_PREFIX.length());
                elem = LABEL_PREFIX + labelMapper.getString(label);
            }
            newVal.add(elem);
        }
        return newVal.saveString().substring(1);
    }

    private static final String LABEL_PREFIX = "label:";

}
