// Copyright (C) 2010 Tuma Solutions, LLC
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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public class TeamMemberClipSelection implements Transferable {

    private static class LocalTeamMemberListDataFlavor extends DataFlavor {
        public LocalTeamMemberListDataFlavor() {
            // by subclassing DataFlavor, we help it to determine the
            // appropriate classloader that should be used to load the
            // TeamMemberClipData class, named in the mime type below.
            super(DataFlavor.javaJVMLocalObjectMimeType + "; class="
                    + TeamMemberClipData.class.getName(),
                    "Team Member List Data");
        }
    }

    public static final DataFlavor LOCAL_TEAM_LIST_FLAVOR =
            new LocalTeamMemberListDataFlavor();

    public static final DataFlavor REMOTE_TEAM_LIST_FLAVOR = new DataFlavor(
            TeamMemberClipData.class, "Team Member List Data");

    private static final DataFlavor[] FLAVORS = { LOCAL_TEAM_LIST_FLAVOR,
            REMOTE_TEAM_LIST_FLAVOR };

    private TeamMemberClipData clipData;

    public TeamMemberClipSelection(List<TeamMember> teamMembers) {
        this.clipData = new TeamMemberClipData(teamMembers);
    }

    public DataFlavor[] getTransferDataFlavors() {
        return (DataFlavor[]) FLAVORS.clone();
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return isSupportedDataFlavor(flavor);
    }

    public static boolean isSupportedDataFlavor(DataFlavor flavor) {
        for (int i = 0; i < FLAVORS.length; i++) {
            if (flavor.equals(FLAVORS[i]))
                return true;
        }
        return false;
    }

    public static boolean hasSupportedDataFlavor(DataFlavor[] flavors) {
        for (DataFlavor f : flavors) {
            if (isSupportedDataFlavor(f))
                return true;
        }
        return false;
    }

    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {
        if (isSupportedDataFlavor(flavor))
            return clipData;
        else
            throw new UnsupportedFlavorException(flavor);
    }


    /**
     * Extract the list of team members from a transferrable, if possible
     */
    public static List<TeamMember> getTeamMembersFromTransferrable(
            Transferable t, Date zeroDay) {
        // first, try to retrieve a local java object (the exact one we placed
        // on the clipboard).
        try {
            Object d = t.getTransferData(LOCAL_TEAM_LIST_FLAVOR);
            List result = ((TeamMemberClipData) d).getTeamMembers(zeroDay);
            if (result != null)
                return result;
        } catch (Exception e) {
        }

        // if that failed, try to retrieve a TeamMemberClipData object placed
        // on the clipboard by some other WBS Editor.
        try {
            Object d = t.getTransferData(REMOTE_TEAM_LIST_FLAVOR);
            List result = ((TeamMemberClipData) d).getTeamMembers(zeroDay);
            if (result != null)
                return result;
        } catch (Exception e) {
        }

        // if all else fails, return null to indicate failure.
        return null;
    }

}
