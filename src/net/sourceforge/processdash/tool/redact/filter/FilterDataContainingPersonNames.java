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

import net.sourceforge.processdash.tool.redact.RedactFilterIDs;
import net.sourceforge.processdash.tool.redact.RedactFilterUtils;
import net.sourceforge.processdash.tool.redact.EnabledFor;
import net.sourceforge.processdash.tool.redact.PersonMapper;

@EnabledFor(RedactFilterIDs.PEOPLE)
public class FilterDataContainingPersonNames extends AbstractDataStringFilter {

    @EnabledFor({ "^Team_Note", "/Team_Note" })
    public String filterNoteAuthor(String noteXml) {
        return RedactFilterUtils.replaceXmlAttr(noteXml, "author",
            PersonMapper.HASH_PERSON_NAME);
    }

    @EnabledFor("^Owner$")
    public String filterDatasetOwner(String owner) {
        return PersonMapper.hashPersonName(owner);
    }

    @EnabledFor("^Indiv_Initials$")
    public String scrambleTeamProjectInitials(String initials) {
        return PersonMapper.hashInitials(initials);
    }

    @EnabledFor("^Client_Unique_ID$")
    public String scrambleTeamProjectClientIDs(String clientID) {
        return PersonMapper.hashClientNodeID(clientID);
    }

    @EnabledFor("^Task-Schedule/.*/Task Lists$")
    public String filterNamesInEvRollupSpec(String rollupSpec) {
        StringBuilder result = new StringBuilder();
        int pos = 0;
        while (true) {
            int beg = rollupSpec.indexOf('(', pos);
            if (beg == -1)
                break;
            int end = rollupSpec.indexOf(')', beg);
            if (end == -1)
                break;

            result.append(rollupSpec.substring(pos, ++beg));
            result.append(PersonMapper.hashPersonName( //
                    rollupSpec.substring(beg, end)));
            pos = end;
        }
        result.append(rollupSpec.substring(pos));
        return result.toString();
    }

}
