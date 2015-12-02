// Copyright (C) 2015 Tuma Solutions, LLC
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

import java.util.LinkedList;
import java.util.List;

import net.sourceforge.processdash.tool.redact.EnabledFor;
import net.sourceforge.processdash.tool.redact.PersonMapper;
import net.sourceforge.processdash.tool.redact.RedactFilterIDs;
import net.sourceforge.processdash.tool.redact.RedactFilterUtils;
import net.sourceforge.processdash.util.StringUtils;

@EnabledFor({ RedactFilterIDs.WORKFLOWS, RedactFilterIDs.PEOPLE })
public class FilterWbsWorkflowRoles extends AbstractWbsAttrFilter {

    @EnabledFor(RedactFilterIDs.WORKFLOWS)
    private boolean filterWorkflows;

    @EnabledFor(RedactFilterIDs.PEOPLE)
    private boolean filterPeople;

    @Override
    public void filter(DataFileEntry e) {
        if (filterWorkflows && e.getKey().startsWith(ASSIGNMENT_PREFIX)) {
            String role = e.getKey().substring(ASSIGNMENT_PREFIX.length());
            String hashedRole = hashRole(role);
            e.setKey(ASSIGNMENT_PREFIX + hashedRole);
        }
        super.filter(e);
    }

    @EnabledFor({ "^" + ASSIGNMENT_PREFIX, //
            "^Workflow Roles List$", //
            "^Workflow Performed By$" })
    public String filterPersonList(String value) {
        List<String> tokens = new LinkedList<String>();
        for (String oneToken : value.split(",")) {
            oneToken = oneToken.trim();
            if (oneToken.length() > 0) {
                boolean isRole = oneToken.charAt(0) == ROLE_BEG;
                if (isRole && filterWorkflows)
                    oneToken = hashRole(oneToken);
                else if (!isRole && filterPeople)
                    oneToken = PersonMapper.hashInitials(oneToken);
            }
            tokens.add(oneToken);
        }
        return StringUtils.join(tokens, ", ");
    }

    private String hashRole(String roleToken) {
        String roleName = roleToken.substring(1, roleToken.length() - 1);
        String hashedRole = "role" + RedactFilterUtils.hash(roleName, true, 4);
        return ROLE_BEG + hashedRole + ROLE_END;
    }


    private static final char ROLE_BEG = '\u00AB';

    private static final char ROLE_END = '\u00BB';

    private static final String ASSIGNMENT_PREFIX = "Workflow Role Assignment ";

}
