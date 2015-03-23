// Copyright (C) 2004-2007 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.security;

import java.security.AccessControlException;
import java.security.AccessController;
import java.security.BasicPermission;


public class DashboardPermission extends BasicPermission {

    public DashboardPermission(String name) {
        super(name);
    }

    public DashboardPermission(String name, String actions) {
        super(name, actions);
    }

    public void checkPermission() throws AccessControlException {
        if (CHECKING_ENABLED)
            AccessController.checkPermission(this);
    }


    private static boolean CHECKING_ENABLED = false;

    static void enableChecking() {
        CHECKING_ENABLED = true;
    }

}
