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

package net.sourceforge.processdash.team.group;

public class UserGroupMember implements Comparable<UserGroupMember> {

    private String displayName;

    private String datasetID;

    public UserGroupMember(String displayName, String datasetID) {
        this.displayName = displayName;
        this.datasetID = datasetID;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDatasetID() {
        return datasetID;
    }

    @Override
    public int compareTo(UserGroupMember that) {
        return this.displayName.compareToIgnoreCase(that.displayName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof UserGroupMember) {
            UserGroupMember that = (UserGroupMember) obj;
            return this.datasetID.equals(that.datasetID);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return datasetID.hashCode();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

}
