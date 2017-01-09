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

public class User {

    private String name;

    private String username;

    private boolean inactive;

    private List<String> roleIDs;

    public User(String name, String username, boolean inactive,
            List<String> roleIDs) {
        this.name = name;
        this.username = username;
        this.inactive = inactive;
        this.roleIDs = Collections.unmodifiableList(new ArrayList(roleIDs));
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getUsernameLC() {
        return username.toLowerCase();
    }

    public boolean isInactive() {
        return inactive;
    }

    public List<String> getRoleIDs() {
        return roleIDs;
    }

    public List<Role> getRoles() {
        List<Role> result = new ArrayList<Role>(roleIDs.size());
        for (String id : roleIDs) {
            Role r = PermissionsManager.getInstance().getRoleByID(id);
            if (r != null)
                result.add(r);
        }
        return result;
    }

}
