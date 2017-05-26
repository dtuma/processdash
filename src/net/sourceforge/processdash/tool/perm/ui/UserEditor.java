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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.EventHandler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.team.ui.PersonLookupData;
import net.sourceforge.processdash.team.ui.PersonLookupDialog;
import net.sourceforge.processdash.tool.perm.Permission;
import net.sourceforge.processdash.tool.perm.PermissionsManager;
import net.sourceforge.processdash.tool.perm.Role;
import net.sourceforge.processdash.tool.perm.User;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.lib.BoxUtils;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.ui.lib.TableUtils;
import net.sourceforge.processdash.ui.lib.autocomplete.AssignedToComboBox;
import net.sourceforge.processdash.ui.lib.autocomplete.AutocompletingDataTableCellEditor;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class UserEditor {

    private UserTableModel model;

    private Set<User> usersToDelete;

    private JTextField searchField;

    private JCheckBox showInactiveCheckbox;

    private JTable table;

    private TableRowSorter rowSorter;

    private boolean duplicateUsernameDialogVisible;

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
        Component table = makeTable();
        Component toolbar = makeToolbar(editable);
        updateFilter();

        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.add(toolbar, BorderLayout.NORTH);
        p.add(table, BorderLayout.CENTER);
        return p;
    }

    private Component makeTable() {
        table = new JTable(model);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        rowSorter = new TableRowSorter(model);
        rowSorter.setSortable(UserTableModel.ROLES_COL, false);
        table.setRowSorter(rowSorter);

        table.getColumnModel().getColumn(UserTableModel.USERNAME_COL)
                .setCellEditor(new UsernameCellEditor());
        table.getColumnModel().getColumn(UserTableModel.ROLES_COL)
                .setCellEditor(new RolesCellEditor());
        table.getColumnModel().getColumn(UserTableModel.ROLES_COL)
                .setHeaderRenderer(new RolesHeaderRenderer());
        table.getTableHeader().addMouseListener(new RolesColumnClickHandler());

        int preferredWidth = TableUtils.configureTable(table,
            UserTableModel.COLUMN_WIDTHS, UserTableModel.COLUMN_TOOLTIPS);
        int preferredHeight = 15 * table.getRowHeight();
        table.setPreferredScrollableViewportSize(
            new Dimension(preferredWidth, preferredHeight));

        // when talking to a legacy PDES, the PDES determines which users are
        // active, not us; so don't display the "Active" column in the table.
        if (isLegacyPdesMode()) {
            table.getColumnModel().removeColumn(
                table.getColumnModel().getColumn(UserTableModel.ACTIVE_COL));
        }

        return new JScrollPane(table);
    }

    private Component makeToolbar(boolean editable) {
        FilterHandler filterer = EventHandler.create(FilterHandler.class, this,
            "updateFilter");
        BoxUtils toolbar = BoxUtils.hbox();

        searchField = new JTextField(10);
        searchField.getDocument().addDocumentListener(filterer);
        toolbar.addItems(resources.getString("Find") + ":", 3, searchField, 20);

        showInactiveCheckbox = new JCheckBox(
                resources.getString("Show_Inactive"));
        showInactiveCheckbox.addActionListener(filterer);
        if (isLegacyPdesMode()) {
            showInactiveCheckbox.setSelected(true);
        } else {
            toolbar.addItem(showInactiveCheckbox);
        }
        toolbar.addItems(100, BoxUtils.GLUE);

        if (editable) {
            AbstractAction add = (PersonLookupDialog.isLookupServerConfigured() //
                    ? new AddPdesUserAction() : new AddPlainUserAction());
            toolbar.addItems(5, new JButton(add));
        }

        toolbar.addItems(5, new JButton(new ViewAction()));

        if (editable)
            toolbar.addItems(5, new JButton(new DeleteAction()));

        toolbar.addItem(new JOptionPaneTweaker.MakeResizable());
        toolbar.addItem(new JOptionPaneTweaker.DisableKeys());

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
        selectNewUserRow(numRows);
    }


    /**
     * Select the table row for given user, and possibly start editing their
     * name/username if it is missing.
     */
    private void selectNewUserRow(final int visibleRow) {
        // make certain we have a valid row to display
        if (visibleRow == -1)
            return;

        // identify the most useful cell to begin editing.
        final int modelCol = getBestColumnToEdit(visibleRow);
        final int col = table.convertColumnIndexToView(modelCol);

        // select the row and column we just identified
        table.setRowSelectionInterval(visibleRow, visibleRow);
        table.getColumnModel().getSelectionModel().setLeadSelectionIndex(col);
        table.scrollRectToVisible(table.getCellRect(visibleRow, col, true));

        // make certain the table gets focus. And if this user doesn't have a
        // name/username yet, start editing that cell.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                table.requestFocusInWindow();
                if (modelCol <= UserTableModel.USERNAME_COL)
                    table.editCellAt(visibleRow, col);
            }
        });
    }

    private int getBestColumnToEdit(int visibleRow) {
        int modelRow = table.convertRowIndexToModel(visibleRow);
        EditableUser user = model.getUser(modelRow);
        if (!StringUtils.hasValue(user.getName()))
            return UserTableModel.NAME_COL;
        else if (!StringUtils.hasValue(user.getUsername()))
            return UserTableModel.USERNAME_COL;
        else if (!user.getActive() && !isLegacyPdesMode())
            return UserTableModel.ACTIVE_COL;
        else
            return UserTableModel.ROLES_COL;
    }


    private boolean isLegacyPdesMode() {
        return PermissionsManager.getInstance().isLegacyPdesMode();
    }


    /**
     * @return the dialog window containing our UI components
     */
    protected Window getDialogParent() {
        return SwingUtilities.getWindowAncestor(table);
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
            // the username is the unique ID we use to identify users. So if
            // an individual's username was changed, this is equivalent to
            // deleting the old user and adding a new one with a different
            // username. Oblige by adding the old user to the deletion list.
            if (oneUser.isUsernameChange())
                usersToDelete.add(oneUser.getOriginalUser());

            // if this row of the table was modified, and it has a nonempty
            // username, save it. If the username is empty, we skip the row
            // as either (1) an aborted addition, or (2) the deletion of an
            // old user by clearing out the values on their row.
            if (oneUser.isModified()) {
                if (StringUtils.hasValue(oneUser.getUsername()))
                    usersToSave.add(oneUser.getNewUser());
            }
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
            if (duplicateUsernameDialogVisible)
                return;

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
            if (duplicateUsernameDialogVisible)
                return;

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
            selectNewUserRow(tableRow);

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
     * An action to view the permission of the selected user.
     */
    private class ViewAction extends EnablementAction {

        public ViewAction() {
            super("View");
        }

        @Override
        protected boolean shouldBeEnabled(int[] rows) {
            return rows.length == 1;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] rows = getSelectedModelRows();
            if (rows.length != 1)
                return;

            // retrieve the permissions of the selected user, and build a
            // component to display them
            final User user = model.getUser(rows[0]).getNewUser();
            final PermissionList list = new PermissionList();
            list.setContents(new ArrayList<Permission>(PermissionsManager
                    .getInstance().getPermissionsForUser(user, false)));
            JScrollPane sp = new JScrollPane(list);
            sp.setPreferredSize(new Dimension(300, 200));

            // create a checkbox for toggling between explicit and full list
            final JCheckBox cb = new JCheckBox(
                    resources.getString("View_Permissions.Show_Children"));
            cb.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    boolean deep = cb.isSelected();
                    list.setContents(new ArrayList(PermissionsManager
                            .getInstance().getPermissionsForUser(user, deep)));
                }
            });
            Object footer = BoxUtils.hbox(BoxUtils.GLUE, cb, BoxUtils.GLUE);

            // build the contents to display in the dialog
            String title = resources.getString("View_Permissions.Title");
            String header = resources.format("View_Permissions.Header_FMT",
                user.getName());
            Object[] message = { header, sp, footer,
                    new JOptionPaneTweaker.MakeResizable() };

            // show the list of user permissions
            JOptionPane.showMessageDialog(getDialogParent(), message, title,
                JOptionPane.PLAIN_MESSAGE);
        }

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
            if (duplicateUsernameDialogVisible)
                return;

            int[] rows = getSelectedModelRows();
            if (rows.length == 0) {
                // nothing to do
                return;

            } else if (rows.length == 1 && model.isCatchAllUserRow(rows[0])) {
                // the user is trying to delete the "catch all" user. display a
                // message explaining that this is not allowed.
                Object msg = resources.getString("Delete.Not_Allowed_Message");
                if (!PermissionsManager.getInstance().isLegacyPdesMode())
                    msg = new Object[] { msg, " ", resources
                            .getStrings("Delete.Not_Allowed_Message_2") };
                JOptionPane.showMessageDialog(getDialogParent(), msg,
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


    /**
     * A table cell editor that guards against the entry of duplicate usernames.
     */
    private class UsernameCellEditor extends DefaultCellEditor {

        private String otherUsername;

        private UsernameCellEditor() {
            super(new JTextField());
        }

        @Override
        public boolean stopCellEditing() {
            // see if this username is a duplicate. If not, return normally
            int otherRow = getIndexOfDuplicateUsernameRow();
            if (otherRow == -1)
                return super.stopCellEditing();

            // the user has entered a duplicate username. Show them an error
            // message and ask what they want to do
            String title = resources.getString("Duplicate_Username.Title");
            String[] message = resources.formatStrings(
                "Duplicate_Username.Message_FMT", otherUsername);
            duplicateUsernameDialogVisible = true;
            int userChoice = JOptionPane.showConfirmDialog(getDialogParent(),
                message, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.ERROR_MESSAGE);
            duplicateUsernameDialogVisible = false;

            // if the user presses "OK", return false to let them keep editing.
            if (userChoice == JOptionPane.OK_OPTION)
                return false;

            // if the user presses "Cancel", cancel this editing session and
            // select the row for the duplicate user.
            cancelCellEditing();
            addUserToFilter(otherUsername);
            selectNewUserRow(table.convertRowIndexToView(otherRow));
            return true;
        }

        private int getIndexOfDuplicateUsernameRow() {
            // find the index of the row that is currently being edited
            int editingRow = table.getEditingRow();
            if (editingRow == -1)
                return -1;
            editingRow = table.convertRowIndexToModel(editingRow);

            // read the proposed username that has been entered into this row
            String newUsername = ((JTextField) getComponent()).getText().trim();
            if (newUsername.isEmpty())
                return -1;

            // check to see if any other user already has this username. If
            // so, return the model index of that user's row.
            for (int row = 0; row < model.getRowCount(); row++) {
                if (row != editingRow) {
                    otherUsername = model.getUser(row).getUsername();
                    if (newUsername.equalsIgnoreCase(otherUsername))
                        return row;
                }
            }

            // no other user has this username.
            return -1;
        }

    }


    /**
     * Open the role definition report if the user clicks on the header for the
     * Roles column
     */
    private class RolesColumnClickHandler extends MouseAdapter {

        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e)) {
                int col = table.getTableHeader().columnAtPoint(e.getPoint());
                col = table.convertColumnIndexToModel(col);
                if (col == UserTableModel.ROLES_COL)
                    Browser.launch("/dash/rolesReport");
            }
        }

    }


    /**
     * Renderer to display the header for the Roles column
     */
    private class RolesHeaderRenderer implements TableCellRenderer {

        private TableCellRenderer delegate;

        private String tooltip;

        private Icon icon;

        RolesHeaderRenderer() {
            delegate = table.getTableHeader().getDefaultRenderer();
            String tip = HTMLUtils.escapeEntities(
                UserTableModel.COLUMN_TOOLTIPS[UserTableModel.ROLES_COL]);
            tooltip = "<html><div style='width:200px'>" + tip + "</div></html>";
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            Component result = delegate.getTableCellRendererComponent(table,
                value, isSelected, hasFocus, row, column);

            if (result instanceof JLabel) {
                if (icon == null) {
                    int height = result.getFontMetrics(result.getFont())
                            .getAscent() - 1;
                    icon = DashboardIconFactory.getHelpIcon(height);
                }

                JLabel l = (JLabel) result;
                l.setHorizontalTextPosition(SwingConstants.LEFT);
                l.setIcon(icon);
                l.setIconTextGap(10);
                l.setToolTipText(tooltip);
            }

            return result;
        }

    }


    /**
     * A table cell editor that provides autocompletion for role names.
     */
    private class RolesCellEditor extends AutocompletingDataTableCellEditor {

        private AssignedToComboBox comboBox;

        RolesCellEditor() {
            super(new AssignedToComboBox(true));
            comboBox = (AssignedToComboBox) getComboBox();
            comboBox.setWordPattern(ROLE_NAME_PAT);
            comboBox.setSeparatorChar(',');
            comboBox.setInitialsList(getRoleNames());
        }

        private List<String> getRoleNames() {
            List<String> roleNames = new ArrayList<String>();
            for (Role r : PermissionsManager.getInstance().getAllRoles()) {
                if (!r.isInactive())
                    roleNames.add(r.getName());
            }
            Collections.sort(roleNames, String.CASE_INSENSITIVE_ORDER);
            return roleNames;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            // call super() so the editor setup timer will be restarted
            super.getTableCellEditorComponent(table, null, isSelected, row,
                column);

            // initialize the combo box contents and return it
            comboBox.setFullText((String) value);
            return comboBox;
        }

        @Override
        public Object getCellEditorValue() {
            return comboBox.getFullText();
        }

    }

    private static final Pattern ROLE_NAME_PAT = Pattern.compile("(\\w[^,]*)");

}
