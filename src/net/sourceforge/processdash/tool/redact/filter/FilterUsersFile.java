// Copyright (C) 2017 Tuma Solutions, LLC
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

import static net.sourceforge.processdash.tool.perm.PermissionsManager.CATCH_ALL_USER_ID;

import net.sourceforge.processdash.tool.perm.WhoAmI;
import net.sourceforge.processdash.tool.redact.EnabledFor;
import net.sourceforge.processdash.tool.redact.PersonMapper;
import net.sourceforge.processdash.tool.redact.RedactFilterIDs;
import net.sourceforge.processdash.util.StringMapper;

@EnabledFor(RedactFilterIDs.USERS)
public class FilterUsersFile extends AbstractLineBasedFilter {

    public FilterUsersFile() {
        setFilenamePatterns("^users.dat$");
    }

    @Override
    public String getString(String line) {
        if (!line.contains("<user "))
            return line;

        String username = getXmlAttr(line, "id");
        if (CATCH_ALL_USER_ID.equals(username))
            return line;

        line = replaceXmlAttr(line, "name", PersonMapper.HASH_PERSON_NAME);
        line = replaceXmlAttr(line, "id", HASH_USERNAME);
        return line;
    }

    public static final StringMapper HASH_USERNAME = new StringMapper() {
        public String getString(String username) {
            return WhoAmI.hashUsername(username);
        }
    };

}
