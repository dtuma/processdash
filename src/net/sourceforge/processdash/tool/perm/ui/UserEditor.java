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
import java.awt.Dimension;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.perm.PermissionsManager;
import net.sourceforge.processdash.tool.perm.User;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.ui.lib.TableUtils;

public class UserEditor {

    private UserTableModel model;

    private Set<User> usersToDelete;

    private JTable table;

    static final Resources resources = Resources
            .getDashBundle("Permissions.UserEditor");


    public UserEditor(Component parent, boolean editable) {
        // ask the permissions manager to sync with external user changes
        PermissionsManager.getInstance().updateExternalUsers();

        // create data structures, and build the user interface
        this.model = new UserTableModel(editable);
        this.usersToDelete = new HashSet<User>();
        Object content = buildUI(editable);

        // display the UI in dialog window
        String title = resources
                .getString(editable ? "Edit_Users" : "View_Users");
        int optionType = editable ? JOptionPane.OK_CANCEL_OPTION
                : JOptionPane.DEFAULT_OPTION;
        int userChoice = JOptionPane.showConfirmDialog(parent, content, title,
            optionType, JOptionPane.PLAIN_MESSAGE);

        // if the user pressed OK, save changes
        if (editable && userChoice == JOptionPane.OK_OPTION)
            saveChanges();
    }


    private Object buildUI(boolean editable) {
        makeTable();
        return new Object[] { new JScrollPane(table),
                new JOptionPaneTweaker.MakeResizable() };
    }


    private void makeTable() {
        this.table = new JTable(model);

        int preferredWidth = TableUtils.configureTable(table,
            UserTableModel.COLUMN_WIDTHS, UserTableModel.COLUMN_TOOLTIPS);
        int preferredHeight = 15 * table.getRowHeight();
        table.setPreferredScrollableViewportSize(
            new Dimension(preferredWidth, preferredHeight));
    }


    private void saveChanges() {
        Set<User> usersToSave = new HashSet();
        for (int i = model.getRowCount(); i-- > 0;) {
            EditableUser oneUser = model.getUser(i);
            if (oneUser.isUsernameChange())
                usersToDelete.add(oneUser.getOriginalUser());
            if (oneUser.isModified())
                usersToSave.add(oneUser.getNewUser());
        }
        PermissionsManager.getInstance().alterUsers(usersToSave, usersToDelete);
    }

}
