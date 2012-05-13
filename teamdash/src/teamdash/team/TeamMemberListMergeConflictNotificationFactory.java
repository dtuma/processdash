// Copyright (C) 2012 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;

import teamdash.merge.MergeWarning;
import teamdash.merge.ui.MergeConflictNotification;
import teamdash.merge.ui.MergeConflictNotification.ModelType;

public class TeamMemberListMergeConflictNotificationFactory {

    public static List<MergeConflictNotification> createAll(
            TeamMemberListMerger merger) {
        List<MergeConflictNotification> result = new ArrayList();
        for (MergeWarning<Integer> warning : merger.getMergeWarnings()) {
            MergeConflictNotification notification = create(merger, warning);
            if (notification != null)
                result.add(notification);
        }
        return result;
    }

    public static MergeConflictNotification create(TeamMemberListMerger merger,
            MergeWarning<Integer> mw) {
        MergeConflictNotification result = new MergeConflictNotification(
                ModelType.TeamList, mw);
        result.putNodeAttributes(
            merger.main.findTeamMemberByID(mw.getMainNodeID()),
            merger.incoming.findTeamMemberByID(mw.getIncomingNodeID()));
        result.addUserOption(MergeConflictNotification.DISMISS, null);
        try {
            result.formatDescription();
            return result;
        } catch (MissingResourceException mre) {
            System.err.println("Unexpected merge conflict key for "
                    + "team member list: " + mre.getKey());
            return null;
        }
    }

}
