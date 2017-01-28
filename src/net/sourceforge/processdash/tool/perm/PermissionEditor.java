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

import java.awt.Component;
import java.util.Map;

public interface PermissionEditor<T extends Permission> {

    /**
     * Interact with the user to configure the parameters for a permission.
     * 
     * @param p
     *            an existing permission
     * @param parent
     *            the parent component which dialogs should be centered over
     * @param isAdd
     *            true if we are adding a new permission, false if we are
     *            editing an existing permission
     * @return <tt>null</tt> if the user canceled the edit; otherwise, a set of
     *         parameters that should be used to create a modified permission
     *         reflecting the user's edits
     */
    public Map<String, String> editPermission(T p, Component parent,
            boolean isAdd);

}
