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

import static net.sourceforge.processdash.tool.perm.PermissionsManager.CATCH_ALL_USER_ID;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import net.sourceforge.processdash.tool.perm.PermissionsManager;
import net.sourceforge.processdash.tool.perm.User;

public class UserTableModel extends AbstractTableModel {

    private boolean editable;

    private List<EditableUser> users;


    public UserTableModel(boolean editable) {
        this.editable = editable;

        List<User> allUsers = PermissionsManager.getInstance().getAllUsers();
        users = new ArrayList<EditableUser>(allUsers.size() + 5);
        for (User oneUser : allUsers)
            users.add(new EditableUser(oneUser));
    }

    public EditableUser getUser(int row) {
        return users.get(row);
    }

    public int findRowForUser(String username) {
        if (username == null)
            return -1;
        for (int i = 0; i < getRowCount(); i++) {
            if (username.equalsIgnoreCase(getUser(i).getUsername()))
                return i;
        }
        return -1;
    }

    public boolean isCatchAllUserRow(int row) {
        return CATCH_ALL_USER_ID.equals(getUser(row).getUsername());
    }

    public void addUser(EditableUser newUser) {
        if (editable && newUser != null) {
            int newRow = getRowCount();
            users.add(newUser);
            fireTableRowsInserted(newRow, newRow);
        }
    }

    public EditableUser deleteUser(int row) {
        if (editable && !isCatchAllUserRow(row)) {
            EditableUser deleted = users.remove(row);
            fireTableRowsDeleted(row, row);
            return deleted;
        }
        return null;
    }

    @Override
    public int getRowCount() {
        return users.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_KEYS.length;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == ACTIVE_COL)
            return Boolean.class;
        else
            return String.class;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        EditableUser user = users.get(rowIndex);
        switch (columnIndex) {

        case NAME_COL:
            return user.getName();

        case USERNAME_COL:
            return user.getUsername();

        case ACTIVE_COL:
            return user.getActive();

        case ROLES_COL:
            return user.getRoleNames();

        default:
            return null;
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (!editable)
            return;

        EditableUser user = users.get(rowIndex);
        switch (columnIndex) {

        case NAME_COL:
            if (!isCatchAllUserRow(rowIndex))
                user.setName((String) aValue);
            break;

        case USERNAME_COL:
            if (!isCatchAllUserRow(rowIndex) && !isCatchAllUsername(aValue))
                user.setUsername((String) aValue);
            break;

        case ACTIVE_COL:
            user.setActive((Boolean) aValue);
            break;

        case ROLES_COL:
            user.setRoleNames((String) aValue);
            break;

        }
    }

    private boolean isCatchAllUsername(Object value) {
        if (value instanceof String) {
            return ((String) value).trim().equals(CATCH_ALL_USER_ID);
        } else {
            return false;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (isCatchAllUserRow(rowIndex) && columnIndex < ACTIVE_COL)
            return false;

        return editable;
    }


    static final int NAME_COL = 0;

    static final int USERNAME_COL = 1;

    static final int ACTIVE_COL = 2;

    static final int ROLES_COL = 3;

    static final String[] COLUMN_KEYS = { "Name", "Username", "Active",
            "Roles" };

    private static final String[] COLUMN_NAMES = UserEditor.resources
            .getStrings("Columns.", COLUMN_KEYS, ".Name");

    static final int[] COLUMN_WIDTHS = UserEditor.resources.getInts("Columns.",
        COLUMN_KEYS, ".Width_");

    static final String[] COLUMN_TOOLTIPS = { null, null,
            UserEditor.resources.getString("Columns.Active.Tooltip"),
            UserEditor.resources.getString("Columns.Roles.Tooltip") };

}
