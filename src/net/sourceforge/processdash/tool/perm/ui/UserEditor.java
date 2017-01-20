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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.EventHandler;
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
import net.sourceforge.processdash.tool.perm.PermissionsManager;
import net.sourceforge.processdash.tool.perm.User;
import net.sourceforge.processdash.ui.lib.BoxUtils;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.ui.lib.TableUtils;
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

        if (editable)
            toolbar.addItems(5, new JButton(new DeleteAction()));

        return toolbar;
    }

    public interface FilterHandler extends ActionListener, DocumentListener {
    }

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


    private abstract class UserAction extends AbstractAction
            implements ListSelectionListener {

        UserAction(String resKey) {
            super(resources.getString(resKey));
            table.getSelectionModel().addListSelectionListener(this);
            valueChanged(null);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            setEnabled(shouldBeEnabled(getSelectedModelRows()));
        }

        protected int[] getSelectedModelRows() {
            int[] selectedRows = table.getSelectedRows();
            for (int i = selectedRows.length; i-- > 0;)
                selectedRows[i] = table.convertRowIndexToModel(selectedRows[i]);
            return selectedRows;
        }

        protected abstract boolean shouldBeEnabled(int[] rows);

        protected Component getParent() {
            return SwingUtilities.getWindowAncestor(showInactiveCheckbox);
        }

    }


    private class DeleteAction extends UserAction {

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
                JOptionPane.showMessageDialog(getParent(),
                    resources.getStrings("Delete.Not_Allowed_Message"),
                    resources.getString("Delete.Not_Allowed_Title"),
                    JOptionPane.ERROR_MESSAGE);

            } else if (JOptionPane.showConfirmDialog(getParent(),
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
                for (int i = rows.length; i-- > 0;)
                    model.deleteUser(rows[i]);
            }
        }

    }

}
