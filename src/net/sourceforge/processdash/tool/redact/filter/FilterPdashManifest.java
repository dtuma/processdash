// Copyright (C) 2012-2025 Tuma Solutions, LLC
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

import net.sourceforge.processdash.tool.export.impl.ArchiveMetricsXmlConstants;
import net.sourceforge.processdash.tool.redact.RedactFilterIDs;
import net.sourceforge.processdash.tool.redact.EnabledFor;
import net.sourceforge.processdash.tool.redact.PersonMapper;
import net.sourceforge.processdash.tool.redact.RedactFilterUtils;

public class FilterPdashManifest extends AbstractLineBasedFilter implements
        ArchiveMetricsXmlConstants {

    @EnabledFor(RedactFilterIDs.PEOPLE)
    private boolean filterPeople;

    public FilterPdashManifest() {
        setFilenamePatterns("!manifest.xml$");
    }

    public String getString(String str) {
        String tag = getXmlTagName(str);

        if (EXPORTED_TAG.equals(tag) && filterPeople) {
            str = replaceXmlAttr(str, OWNER_ATTR, PersonMapper.HASH_PERSON_NAME);
            str = replaceXmlAttr(str, USERNAME_ATTR,
                RedactFilterUtils.HASH_STRING);

        } else if (FROM_DATASET_TAG.equals(tag) || FROM_WBS_TAG.equals(tag)) {
            str = replaceXmlAttr(str, FROM_DATASET_LOCATION_ATTR,
                RedactFilterUtils.HASH_STRING);
        }

        return str;
    }

}
