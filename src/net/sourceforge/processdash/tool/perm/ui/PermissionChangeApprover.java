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

package net.sourceforge.processdash.tool.perm.ui;

import java.awt.Component;
import java.util.Set;

import javax.swing.JOptionPane;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.perm.PermissionsManager;
import net.sourceforge.processdash.tool.quicklauncher.CompressedInstanceLauncher;

public class PermissionChangeApprover {

    private static final Resources resources = Resources
            .getDashBundle("Permissions.Warning");


    public static boolean needsRevisit(Component parent, boolean editable) {
        // if editing is not allowed, no revisits are necessary
        if (!editable)
            return false;

        // don't bother users if they are altering a data backup. They could be
        // prepping it for delivery to an untrusted external source
        if (CompressedInstanceLauncher.isRunningFromCompressedData())
            return false;

        // determine whether the user has lost important capabilities
        Set<String> lostPermissions = PermissionsManager.getInstance()
                .checkPermissionLoss();
        int change = 0;
        if (lostPermissions.contains(PermissionsManager.EDIT_ROLES_PERM))
            change += LOST_ROLES;
        if (lostPermissions.contains(PermissionsManager.EDIT_USERS_PERM))
            change += LOST_USERS;
        if (change == 0)
            return false;

        // if the user will lose important permissions, ask for confirmation
        String title = resources.getString("Permission_Loss.Title");
        Object[] message = { resources.getStrings(RES_KEYS[change]), " ",
                resources.getStrings("Permission_Loss.Footer") };
        int userChoice = JOptionPane.showConfirmDialog(parent, message, title,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        return userChoice != JOptionPane.OK_OPTION;
    }

    private static final int LOST_USERS = 1;

    private static final int LOST_ROLES = 2;

    private static final String[] RES_KEYS = { null, "Permission_Loss.User",
            "Permission_Loss.Role", "Permission_Loss.User_Role" };

}
