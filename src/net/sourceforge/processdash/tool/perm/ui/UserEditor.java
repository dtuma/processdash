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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.team.ui.PersonLookupData;
import net.sourceforge.processdash.team.ui.PersonLookupDialog;
import net.sourceforge.processdash.tool.perm.PermissionsManager;
import net.sourceforge.processdash.tool.perm.User;
import net.sourceforge.processdash.ui.lib.BoxUtils;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.ui.lib.TableUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class UserEditor {

    private UserTableModel model;

    private Set<User> usersToDelete;

    private JTextField searchField;

    private JCheckBox showInactiveCheckbox;

    private JTable table;

    private TableRowSorter rowSorter;

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
        Object table = makeTable();
        Object toolbar = makeToolbar(editable);
        updateFilter();

        return BoxUtils.vbox(toolbar, 5, table,
            new JOptionPaneTweaker.MakeResizable());
    }

    private Object makeTable() {
        table = new JTable(model);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        rowSorter = new TableRowSorter(model);
        table.setRowSorter(rowSorter);

        int preferredWidth = TableUtils.configureTable(table,
            UserTableModel.COLUMN_WIDTHS, UserTableModel.COLUMN_TOOLTIPS);
        int preferredHeight = 15 * table.getRowHeight();
        table.setPreferredScrollableViewportSize(
            new Dimension(preferredWidth, preferredHeight));

        return new JScrollPane(table);
    }

    private Object makeToolbar(boolean editable) {
        FilterHandler filterer = EventHandler.create(FilterHandler.class, this,
            "updateFilter");
        BoxUtils toolbar = BoxUtils.hbox();

        searchField = new JTextField(10);
        searchField.getDocument().addDocumentListener(filterer);
        toolbar.addItems(resources.getString("Find") + ":", 3, searchField, 20);

        showInactiveCheckbox = new JCheckBox(
                resources.getString("Show_Inactive"));
        showInactiveCheckbox.addActionListener(filterer);
        toolbar.addItems(showInactiveCheckbox, 100, BoxUtils.GLUE);

        if (editable) {
            AbstractAction add = (PersonLookupDialog.isLookupServerConfigured() //
                    ? new AddPdesUserAction() : new AddPlainUserAction());
            toolbar.addItems(5, new JButton(add));
        }

        if (editable)
            toolbar.addItems(5, new JButton(new DeleteAction()));

        return toolbar;
    }

    public interface FilterHandler extends ActionListener, DocumentListener {
    }

    /**
     * Update the table row filter to reflect the values of the search text
     * field and the "show inactive users" checkbox.
     */
    public void updateFilter() {
        rowSorter.setRowFilter(calcFilter());
    }

    private RowFilter<UserTableModel, Object> calcFilter() {
        RowFilter<UserTableModel, Object> textFilter = null;
        String searchText = searchField.getText().trim();
        if (StringUtils.hasValue(searchText))
            textFilter = RowFilter.regexFilter("(?i)" + searchText,
                UserTableModel.NAME_COL, UserTableModel.USERNAME_COL,
                UserTableModel.ROLES_COL);

        RowFilter<UserTableModel, Object> activeFilter = null;
        if (showInactiveCheckbox.isSelected() == false)
            activeFilter = RowFilter.regexFilter("^true$",
                UserTableModel.ACTIVE_COL);

        if (textFilter == null)
            return activeFilter;
        else if (activeFilter == null)
            return textFilter;
        else
            return RowFilter.andFilter(Arrays.asList(textFilter, activeFilter));
    }


    /**
     * If a filter is in effect, tweak it to ensure that the given user will be
     * displayed.
     */
    private void addUserToFilter(String username) {
        // if there isn't a current row filter, there is nothing to do
        RowFilter<UserTableModel, Object> currentFilter = rowSorter
                .getRowFilter();
        if (currentFilter == null)
            return;

        // create a filter that specifically chooses the given individual.
        RowFilter<UserTableModel, Object> userFilter = RowFilter
                .regexFilter("(?i)" + username, UserTableModel.USERNAME_COL);

        // install a new "or filter" that includes this user in the results.
        rowSorter.setRowFilter(RowFilter.orFilter(Arrays.asList( //
            currentFilter, userFilter)));
    }


    /**
     * Add a new user to the end of the table
     */
    private void addNewUser(String username, String name) {
        // clear the user search if one is in effect.
        searchField.setText("");

        // create a new user object, and set its name/username/active flag
        EditableUser newUser = new EditableUser(null);
        newUser.setUsername(username);
        newUser.setName(name);
        newUser.setActive(true);

        // copy the roles from the catch-all user
        int catchAllRow = model.findRowForUser(CATCH_ALL_USER_ID);
        if (catchAllRow != -1) {
            EditableUser catchAllUser = model.getUser(catchAllRow);
            newUser.setRoleIDs(catchAllUser.getRoleIDs());
        }

        // add the user, then scroll to and select the new row
        final int numRows = table.getRowCount();
        model.addUser(newUser);
        selectNewUserRow(numRows, name);
    }


    /**
     * Select the table row for given user, and possibly start editing their
     * name if it is missing.
     */
    private void selectNewUserRow(final int visibleRow, String name) {
        // if the user is nameless, we should place the editing cursor on the
        // name column. Otherwise, we should place it on the roles column.
        int modelCol = (!StringUtils.hasValue(name) ? UserTableModel.NAME_COL
                : UserTableModel.ROLES_COL);
        final int col = table.convertColumnIndexToView(modelCol);

        table.setRowSelectionInterval(visibleRow, visibleRow);
        table.getColumnModel().getSelectionModel().setLeadSelectionIndex(col);
        table.scrollRectToVisible(table.getCellRect(visibleRow, col, true));

        // if this user doesn't have a name yet, start editing that cell.
        if (!StringUtils.hasValue(name)) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    table.requestFocusInWindow();
                    table.editCellAt(visibleRow, col);
                }
            });
        }
    }


    /**
     * @return the dialog window containing our UI components
     */
    protected Window getDialogParent() {
        return SwingUtilities.getWindowAncestor(showInactiveCheckbox);
    }


    /**
     * @return the currently selected rows, in model coordinates
     */
    protected int[] getSelectedModelRows() {
        int[] rows = table.getSelectedRows();
        for (int i = rows.length; i-- > 0;)
            rows[i] = table.convertRowIndexToModel(rows[i]);
        return rows;
    }


    /**
     * Save any additions, deletions, and edits that were made in this window.
     */
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



    /**
     * An action to add a new, empty user row (when the PDES is not in use).
     */
    private class AddPlainUserAction extends AbstractAction {

        public AddPlainUserAction() {
            super(resources.getString("Add"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            addNewUser("", "");
        }

    }


    /**
     * An action to look up users from the PDES, and add them.
     */
    private class AddPdesUserAction extends AbstractAction
            implements PersonLookupData {

        private String name, username;

        AddPdesUserAction() {
            super(resources.getString("Add"));
        }

        public String getName() {
            return null;
        }

        public String getServerIdentityInfo() {
            // provide initialization parameters for the person lookup logic
            return "allowEmptySave=t&saveText="
                    + HTMLUtils.urlEncode(resources.getString("Add"));
        }

        public void setName(String name) {
            this.name = name;
            if (name != null)
                name = name.trim();
        }

        public void setServerIdentityInfo(String query) {
            username = (String) HTMLUtils.parseQuery(query).get("username");
            if (username != null)
                username = username.trim();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // reset state, then display a PDES user lookup dialog
            name = username = null;
            try {
                new PersonLookupDialog(getDialogParent(), this);
            } catch (IOException ioe) {
                // if the server is unreachable, fall back to the plain logic
                // and just add an empty row for the user to type into.
                addNewUser("", "");
                return;
            }

            // save the data received from the user lookup dialog
            if (!StringUtils.hasValue(username))
                // the user pressed cancel. do nothing
                return;

            else if (updateExistingUser(username, name))
                // the user entered a username that's already in the table
                return;

            else
                // add a new user to the end of the table
                addNewUser(username, name);
        }

        private boolean updateExistingUser(String username, String name) {
            // find the user with this username in the table model.
            int existingRow = model.findRowForUser(username);
            if (existingRow == -1)
                return false;

            // update the user's data based on the new values
            EditableUser user = model.getUser(existingRow);
            if (!CATCH_ALL_USER_ID.equals(username)) {
                if (StringUtils.hasValue(name))
                    user.setName(name);
                user.setUsername(username);
            }
            user.setActive(true);

            // find the visible table row that is displaying the user
            int tableRow = table.convertRowIndexToView(existingRow);
            if (tableRow == -1) {
                // if the user isn't visible, install a new row filter that
                // will cause them to be.
                addUserToFilter(username);
                tableRow = table.convertRowIndexToView(existingRow);
            }

            // highlight the table row for the user we just updated
            if (tableRow != -1)
                selectNewUserRow(tableRow, user.getName());

            return true;
        }

    }


    /**
     * An action that should be enabled/disabled based on the active selection.
     */
    private abstract class EnablementAction extends AbstractAction
            implements ListSelectionListener {

        EnablementAction(String resKey) {
            super(resources.getString(resKey));
            table.getSelectionModel().addListSelectionListener(this);
            valueChanged(null);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            setEnabled(shouldBeEnabled(getSelectedModelRows()));
        }

        protected abstract boolean shouldBeEnabled(int[] rows);

    }


    /**
     * An action to delete the selected users.
     */
    private class DeleteAction extends EnablementAction {

        DeleteAction() {
            super("Delete");
        }

        @Override
        protected boolean shouldBeEnabled(int[] rows) {
            return rows.length > 0;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] rows = getSelectedModelRows();
            if (rows.length == 0) {
                // nothing to do
                return;

            } else if (rows.length == 1 && model.isCatchAllUserRow(rows[0])) {
                // the user is trying to delete the "catch all" user. display a
                // message explaining that this is not allowed.
                JOptionPane.showMessageDialog(getDialogParent(),
                    resources.getStrings("Delete.Not_Allowed_Message"),
                    resources.getString("Delete.Not_Allowed_Title"),
                    JOptionPane.ERROR_MESSAGE);

            } else if (JOptionPane.showConfirmDialog(getDialogParent(),
                resources.getString("Delete.Confirm_Message"),
                resources.getString("Delete.Confirm_Title"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
                // we asked for confirmation, and the user said no. do nothing.

            } else {
                // delete the rows requested. Delete starting with the highest
                // numbered model row, so lower indexes will still be valid.
                // Note that the model will automatically refuse to delete the
                // catch-all user row, so we don't need to check for that here.
                Arrays.sort(rows);
                for (int i = rows.length; i-- > 0;) {
                    EditableUser u = model.deleteUser(rows[i]);
                    if (u != null && u.getOriginalUser() != null)
                        usersToDelete.add(u.getOriginalUser());
                }
            }
        }

    }

}
