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

import static net.sourceforge.processdash.tool.perm.PermissionsManager.EDIT_USERS_PERM;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.tool.perm.PermissionsChangeEvent;
import net.sourceforge.processdash.tool.perm.PermissionsChangeListener;
import net.sourceforge.processdash.tool.perm.PermissionsManager;

public class ShowUserEditorAction extends AbstractAction
        implements PermissionsChangeListener {

    private Component parent;

    private boolean editable;


    public ShowUserEditorAction(Component parent) {
        this.parent = parent;
        setEditability();
        PermissionsManager.getInstance().addPermissionsChangeListener(this);
    }

    private void setEditability() {
        this.editable = Settings.isReadWrite() && PermissionsManager
                .getInstance().hasPermission(EDIT_USERS_PERM);
        putValue(Action.NAME, UserEditor.resources
                .getString(editable ? "Edit_Users" : "View_Users"));
    }

    @Override
    public void permissionsChanged(PermissionsChangeEvent event) {
        setEditability();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        setEditability();
        do {
            new UserEditor(parent, editable);
        } while (PermissionChangeApprover.needsRevisit(parent, editable));
    }

}
