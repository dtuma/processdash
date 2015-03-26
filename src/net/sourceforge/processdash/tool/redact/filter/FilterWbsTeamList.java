// Copyright (C) 2012-2015 Tuma Solutions, LLC
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
import net.sourceforge.processdash.tool.redact.EnabledFor;
import net.sourceforge.processdash.tool.redact.PersonMapper;

@EnabledFor(RedactFilterIDs.PEOPLE)
public class FilterWbsTeamList extends AbstractLineBasedFilter {

    public FilterWbsTeamList() {
        setFilenamePatterns("/team(2)?.xml$");
    }

    public String getString(String line) {
        if (line != null && line.contains("<teamMember ")) {
            line = replaceXmlAttr(line, "name", PersonMapper.HASH_PERSON_NAME);
            line = replaceXmlAttr(line, "initials", PersonMapper.HASH_INITIALS);
            line = replaceXmlAttr(line, "subteams", PersonMapper.HASH_SUBTEAMS);
            line = discardXmlAttr(line, "serverIdentityData");
        }

        return line;
    }

}
