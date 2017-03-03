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

package teamdash.team;

import net.sourceforge.processdash.team.group.UserFilter;
import net.sourceforge.processdash.team.group.UserGroup;

public class TeamMemberFilter {

    private TeamMemberList team;

    private UserFilter filter;


    public TeamMemberFilter(TeamMemberList team, UserFilter filter) {
        this.team = team;
        this.filter = filter;
    }

    public boolean include(TeamMember member) {
        if (member == null)
            return false;

        // if the object we were given is included in our team list, use it
        // to check for inclusion
        for (TeamMember m : team.getTeamMembers()) {
            if (m == member)
                return includeImpl(m);
        }

        // if we were given a team member object that isn't in our team list,
        // check for inclusion based on its initials
        return include(member.getInitials());
    }

    public boolean include(int teamMemberId) {
        return includeImpl(team.findTeamMemberByID(teamMemberId));
    }

    public boolean include(String teamMemberInitials) {
        return includeImpl(team.findTeamMember(teamMemberInitials));
    }

    private boolean includeImpl(TeamMember member) {
        if (member == null)
            return false;
        else if (UserGroup.isEveryone(filter))
            return true;
        else
            return filter.getDatasetIDs().contains(member.getDatasetID());
    }

}
