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

package net.sourceforge.processdash.tool.perm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Role implements Comparable<Role> {

    String id;

    private String name;

    private boolean inactive;

    private List<Permission> permissions;

    public Role(String id, String name, boolean inactive,
            List<Permission> permissions) {
        this.id = id;
        this.name = name;
        this.inactive = inactive;
        this.permissions = Collections
                .unmodifiableList(new ArrayList(permissions));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isInactive() {
        return inactive;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int compareTo(Role that) {
        if (this == that)
            return 0;
        else if (PermissionsManager.STANDARD_ROLE_ID.equals(this.id))
            return -1;
        else if (PermissionsManager.STANDARD_ROLE_ID.equals(that.id))
            return +1;
        else
            return String.CASE_INSENSITIVE_ORDER.compare(this.name, that.name);
    }

}
