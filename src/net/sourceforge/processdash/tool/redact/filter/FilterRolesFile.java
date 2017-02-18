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

import static net.sourceforge.processdash.tool.perm.PermissionsManager.STANDARD_ROLE_ID;

import net.sourceforge.processdash.tool.redact.EnabledFor;
import net.sourceforge.processdash.tool.redact.RedactFilterIDs;
import net.sourceforge.processdash.tool.redact.RedactFilterUtils;
import net.sourceforge.processdash.util.StringMapper;

@EnabledFor(RedactFilterIDs.USERS)
public class FilterRolesFile extends AbstractLineBasedFilter {

    public FilterRolesFile() {
        setFilenamePatterns("^roles.dat$",
            "externalresources/.*/settings.xml$");
    }

    @Override
    public String getString(String line) {

        // is this an opening tag for a role definition?
        if (line.contains("<role ")) {

            // if this isn't the "standard user" role, hash the role name.
            if (!STANDARD_ROLE_ID.equals(getXmlAttr(line, "id")))
                line = replaceXmlAttr(line, "name", HASH_ROLE_NAME);

            // in the WBS settings.xml file, hash the list of assigned users
            line = replaceXmlAttr(line, "users", HASH_USERNAME_LIST);
        }

        return line;
    }

    private static final StringMapper HASH_ROLE_NAME = RedactFilterUtils
            .hashPrefixMapper("Role ");

    private static final StringMapper HASH_USERNAME_LIST = RedactFilterUtils
            .hashListMapper(FilterUsersFile.HASH_USERNAME);

}
