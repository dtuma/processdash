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

import static net.sourceforge.processdash.tool.perm.PermissionsManager.STANDARD_ROLE_ID;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.perm.Permission;
import net.sourceforge.processdash.tool.perm.PermissionEditor;
import net.sourceforge.processdash.tool.perm.PermissionSpec;
import net.sourceforge.processdash.tool.perm.PermissionsManager;
import net.sourceforge.processdash.tool.perm.Role;
import net.sourceforge.processdash.tool.perm.User;
import net.sourceforge.processdash.ui.lib.BoxUtils;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.util.StringUtils;

public class RolesEditor {

    private AbstractAction copyRoleAction, renameRoleAction, deleteRoleAction,
            addPermAction, editPermAction, deletePermAction, revertPermAction;

    private DefaultListModel roles;

    private JList rolesList;

    private boolean editable;

    private Role currentlyEditing;

    private Set<Role> rolesToSave, rolesToDelete;

    private PermissionList permissionList;

    private JPanel userInterface;

    static final Resources resources = Resources
            .getDashBundle("Permissions.RolesEditor");


    public RolesEditor(Component parent, boolean allowEdits) {
        // keep track of editing state
        editable = allowEdits;
        currentlyEditing = null;
        rolesToSave = new HashSet<Role>();
        rolesToDelete = new HashSet<Role>();

        // build a user interface and display it to the user
        String title = resources
                .getString(editable ? "Edit_Roles" : "View_Roles");
        int optionType = editable ? JOptionPane.OK_CANCEL_OPTION
                : JOptionPane.DEFAULT_OPTION;
        int userChoice = JOptionPane.showConfirmDialog(parent, createUI(),
            title, optionType, JOptionPane.PLAIN_MESSAGE);

        // if the user clicked OK, save their changes
        if (editable && userChoice == JOptionPane.OK_OPTION)
            saveChanges();

        // store the preferred size of the window for next use
        InternalSettings.set(SIZE_PREF,
            userInterface.getWidth() + "," + userInterface.getHeight());
    }

    private void saveChanges() {
        savePermissionChanges();
        PermissionsManager.getInstance().alterRoles(rolesToSave, rolesToDelete);
    }

    private Component createUI() {
        // create the roles button panel
        JPanel roleButtons = new JPanel(new GridLayout(2, 2, 5, 5));
        roleButtons.add(new JButton(new AddRoleAction()));
        roleButtons.add(new JButton(new CopyRoleAction()));
        roleButtons.add(new JButton(new RenameRoleAction()));
        roleButtons.add(new JButton(new DeleteRoleAction()));

        // create the permissions button panel
        JPanel permButtons = new JPanel(new GridLayout(1, 4, 5, 5));
        permButtons.add(new JButton(new AddPermissionAction()));
        permButtons.add(new JButton(new EditPermissionAction()));
        permButtons.add(new JButton(new DeletePermissionAction()));
        permButtons.add(new JButton(new RevertPermissionsAction()));

        // read the known roles, and add them to a list
        roles = new DefaultListModel();
        for (Role r : PermissionsManager.getInstance().getAllRoles())
            roles.addElement(r);
        rolesList = new JList(roles);
        rolesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rolesList.addListSelectionListener(new RoleSelectionHandler());
        JScrollPane rsp = new JScrollPane(rolesList);
        int prefHeight = Math.max(rolesList.getCellBounds(0, 0).height * 15 + 6,
            200);
        rsp.setPreferredSize(new Dimension(200, prefHeight));

        // create an object for editing the permissions in the selected role
        permissionList = new PermissionList();
        permissionList.setSelectionMode(
            ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        permissionList.getSelectionModel()
                .addListSelectionListener(new PermissionSelectionHandler());
        JScrollPane psp = new JScrollPane(permissionList);
        psp.setPreferredSize(new Dimension(450, prefHeight));

        // create titles to display on the dialog
        JLabel rolesTitle = new JLabel(resources.getString("Roles_Header"));
        JLabel permissionsTitle = new JLabel(
                resources.getString("Permissions_Header"));
        Font f = rolesTitle.getFont();
        f = f.deriveFont(f.getSize2D() * 1.5f);
        rolesTitle.setFont(f);
        permissionsTitle.setFont(f);
        Border b = BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black);
        rolesTitle.setBorder(b);
        permissionsTitle.setBorder(b);

        // arrange the components onto a panel
        GridBagLayout layout = new GridBagLayout();
        userInterface = new JPanel(layout);
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        layout.addLayoutComponent(rolesTitle, c);
        userInterface.add(rolesTitle);

        c.gridx = 2;
        Component comp = BoxUtils.hbox(new JOptionPaneTweaker.MakeResizable(),
            new JOptionPaneTweaker.DisableKeys());
        layout.addLayoutComponent(comp, c);
        userInterface.add(comp);

        c.gridx = 1;
        c.insets = new Insets(0, 10, 0, 0);
        layout.addLayoutComponent(permissionsTitle, c);
        userInterface.add(permissionsTitle);

        c.gridx = 0;
        c.gridy = 1;
        c.gridheight = 2;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(10, 20, 0, 0);
        if (editable) {
            layout.addLayoutComponent(roleButtons, c);
            userInterface.add(roleButtons);
        }

        c.gridy = 3;
        c.gridheight = 1;
        c.weightx = c.weighty = 1.0;
        layout.addLayoutComponent(rsp, c);
        userInterface.add(rsp);

        c.gridx = c.gridy = 1;
        c.weightx = c.weighty = 0;
        c.insets = new Insets(10, 30, 0, 0);
        if (editable) {
            layout.addLayoutComponent(permButtons, c);
            userInterface.add(permButtons);
        }

        c.gridy = 2;
        c.gridheight = 2;
        c.weightx = 3.0;
        c.weighty = 1.0;
        layout.addLayoutComponent(psp, c);
        userInterface.add(psp);

        // load the preferred size of the window
        try {
            String[] size = Settings.getVal(SIZE_PREF).split(",");
            Dimension d = new Dimension(Integer.parseInt(size[0]),
                    Integer.parseInt(size[1]));
            userInterface.setPreferredSize(d);
        } catch (Exception e) {
        }

        return userInterface;
    }

    private void savePermissionChanges() {
        if (currentlyEditing != null && permissionList.isDirty()) {
            Role updated = new Role(currentlyEditing.getId(),
                    currentlyEditing.getName(), //
                    currentlyEditing.isInactive(),
                    permissionList.getContents());

            rolesToSave.remove(currentlyEditing);
            rolesToSave.add(updated);
            permissionList.clearDirty();

            int pos = roles.indexOf(currentlyEditing);
            currentlyEditing = updated;
            roles.set(pos, updated);
        }
    }

    private String promptForName(String resKey, String resArg,
            String defaultName) {
        String title = resources.getString(resKey + "_Title");
        String prompt;
        if (resArg == null)
            prompt = resources.getString(resKey + "_Prompt");
        else
            prompt = resources.format(resKey + "_Prompt_FMT", resArg);

        JTextField nameField = new JTextField(defaultName);
        Object message = new Object[] { prompt, nameField,
                new JOptionPaneTweaker.GrabFocus(nameField) };

        PROMPT: while (true) {
            // prompt the user for the new name
            nameField.selectAll();
            int userChoice = JOptionPane.showConfirmDialog(userInterface,
                message, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
            if (userChoice != JOptionPane.OK_OPTION)
                return null;

            // if they did not enter a name, display an error
            String name = nameField.getText().trim();
            if (!StringUtils.hasValue(name)) {
                JOptionPane.showMessageDialog(userInterface,
                    resources.getString("Name_Missing"),
                    resources.getString("Name_Error_Title"),
                    JOptionPane.ERROR_MESSAGE);
                continue PROMPT;
            }

            // if the name included a comma, display an error
            if (name.indexOf(',') != -1) {
                JOptionPane.showMessageDialog(userInterface,
                    resources.getString("No_Comma"),
                    resources.getString("Name_Error_Title"),
                    JOptionPane.ERROR_MESSAGE);
                continue PROMPT;
            }

            // when renaming, if they did not alter the value, abort.
            if (defaultName != null && name.equalsIgnoreCase(defaultName))
                return (name.equals(defaultName) ? null : name);

            // if they entered a duplicate name, display an error
            for (int i = roles.size(); i-- > 0;) {
                Role r = (Role) roles.get(i);
                if (r.getName().equalsIgnoreCase(name)) {
                    JOptionPane.showMessageDialog(userInterface,
                        resources.format("Name_Duplicate_FMT", name),
                        resources.getString("Name_Error_Title"),
                        JOptionPane.ERROR_MESSAGE);
                    continue PROMPT;
                }
            }

            // all seems OK. Return the name the user selected.
            return name;
        }
    }

    private void addRoleToListAndSelect(Role add) {
        for (int i = 0; i < roles.size(); i++) {
            Role r = (Role) roles.get(i);
            if (add.compareTo(r) < 0) {
                roles.add(i, add);
                rolesList.setSelectedValue(add, true);
                return;
            }
        }
        roles.addElement(add);
        rolesList.setSelectedValue(add, true);
    }


    private class AddRoleAction extends AbstractAction {

        public AddRoleAction() {
            super(resources.getString("Add"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            savePermissionChanges();
            String name = promptForName("Add", null, null);
            if (name == null)
                return;

            Role newRole = new Role(null, name, false, Collections.EMPTY_LIST);
            rolesToSave.add(newRole);
            addRoleToListAndSelect(newRole);
        }
    }


    private class CopyRoleAction extends AbstractAction {

        public CopyRoleAction() {
            super(resources.getString("Copy"));
            setEnabled(false);
            copyRoleAction = this;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentlyEditing == null)
                return;

            savePermissionChanges();
            String name = promptForName("Copy", currentlyEditing.getName(),
                null);
            if (name == null)
                return;

            Role newRole = new Role(null, name, false,
                    currentlyEditing.getPermissions());
            rolesToSave.add(newRole);
            addRoleToListAndSelect(newRole);
        }
    }


    private class RenameRoleAction extends AbstractAction {

        public RenameRoleAction() {
            super(resources.getString("Rename"));
            setEnabled(false);
            renameRoleAction = this;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentlyEditing == null)
                return;

            savePermissionChanges();
            String name = promptForName("Rename", currentlyEditing.getName(),
                currentlyEditing.getName());
            if (name == null)
                return;

            Role renamed = new Role(currentlyEditing.getId(), name,
                    currentlyEditing.isInactive(),
                    currentlyEditing.getPermissions());
            rolesToSave.remove(currentlyEditing);
            rolesToSave.add(renamed);
            roles.removeElement(currentlyEditing);
            addRoleToListAndSelect(renamed);
        }
    }


    private class DeleteRoleAction extends AbstractAction {

        public DeleteRoleAction() {
            super(resources.getString("Delete"));
            setEnabled(false);
            deleteRoleAction = this;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentlyEditing == null)
                return;
            if (STANDARD_ROLE_ID.equals(currentlyEditing.getId()))
                return;

            // identify the users who are assigned to this role
            Vector<User> affectedUsers = new Vector();
            for (User u : PermissionsManager.getInstance().getAllUsers()) {
                if (u.getRoleIDs().contains(currentlyEditing.getId()))
                    affectedUsers.addElement(u);
            }

            // create the objects to display in the confirmation dialog
            String title = resources.getString("Delete_Title");
            String prompt = resources.format("Delete_Prompt_FMT",
                currentlyEditing.getName());
            Object message;
            int iconType;
            if (affectedUsers.isEmpty()) {
                message = prompt;
                iconType = JOptionPane.QUESTION_MESSAGE;
            } else {
                JPanel p = new JPanel(new BorderLayout(20, 10));
                Object[] header = resources.getStrings("Delete_Warning_Header");
                p.add(BoxUtils.vbox(header), BorderLayout.NORTH);
                p.add(new JOptionPaneTweaker.MakeResizable(),
                    BorderLayout.WEST);
                JList list = new JList(affectedUsers);
                list.setVisibleRowCount(Math.min(affectedUsers.size(), 10));
                p.add(new JScrollPane(list), BorderLayout.CENTER);
                p.add(new JLabel(prompt), BorderLayout.SOUTH);
                message = p;
                iconType = JOptionPane.ERROR_MESSAGE;
            }

            // ask the user for confirmation
            int userChoice = JOptionPane.showConfirmDialog(userInterface,
                message, title, JOptionPane.YES_NO_OPTION, iconType);

            if (userChoice == JOptionPane.YES_OPTION) {
                Role delete = currentlyEditing;
                permissionList.clearDirty();
                rolesList.clearSelection();
                roles.removeElement(delete);
                rolesToSave.remove(delete);
                if (delete.getId() != null)
                    rolesToDelete.add(delete);
            }
        }
    }


    private class RoleSelectionHandler implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            savePermissionChanges();

            Role selected = (Role) rolesList.getSelectedValue();
            boolean hasSelection = (selected != null);
            boolean isNotStandard = hasSelection //
                    && !STANDARD_ROLE_ID.equals(selected.getId());
            copyRoleAction.setEnabled(hasSelection);
            renameRoleAction.setEnabled(isNotStandard);
            deleteRoleAction.setEnabled(isNotStandard);
            addPermAction.setEnabled(hasSelection);
            revertPermAction.setEnabled(false);

            if (selected != currentlyEditing) {
                currentlyEditing = selected;
                if (selected == null)
                    permissionList.clearList();
                else
                    permissionList.setContents(selected.getPermissions());
            }
        }

    }


    private class AddPermissionAction extends AbstractAction {

        private PermissionChooser chooser;

        public AddPermissionAction() {
            super(resources.getString("Add"));
            setEnabled(false);
            addPermAction = this;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (chooser == null)
                chooser = new PermissionChooser();

            List<PermissionSpec> specsToAdd = chooser
                    .promptForPermissions(userInterface);
            if (specsToAdd.isEmpty())
                return;

            permissionList.clearSelection();
            for (PermissionSpec oneSpec : specsToAdd) {
                // create a default instance of the requested permission
                Permission p = oneSpec.createPermission(false, null);

                // edit parameters for the permission, if applicable
                PermissionEditor editor = (p == null ? null : p.getEditor());
                if (editor != null) {
                    Map<String, String> params = editor.editPermission(p,
                        userInterface, true);
                    if (params == null)
                        p = null;
                    else
                        p = oneSpec.createPermission(false, params);
                }

                // add the resulting permission to the list
                if (p != null) {
                    int row = permissionList.addPermission(p);
                    permissionList.addRowSelectionInterval(row, row);
                }
            }

            if (permissionList.isDirty())
                revertPermAction.setEnabled(true);
        }

    }


    private class EditPermissionAction extends AbstractAction {

        public EditPermissionAction() {
            super(resources.getString("Edit"));
            setEnabled(false);
            editPermAction = this;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // find which permission row is selected. abort if not exactly one
            int[] rows = permissionList.getSelectedRows();
            if (rows.length != 1)
                return;

            // get the permission for the selected row
            Permission perm = permissionList.getPermission(rows[0]);
            if (perm == null)
                return;

            // if the given permission is not editable, abort
            PermissionEditor editor = perm.getEditor();
            if (editor == null)
                return;

            // edit the parameters for the selected permission. abort if the
            // user cancels the edit operation
            Map<String, String> modifiedParams = editor.editPermission(perm,
                userInterface, false);
            if (modifiedParams == null)
                return;

            // create and store a new permission with the given parameters
            Permission modified = perm.getSpec()
                    .createPermission(perm.isInactive(), modifiedParams);
            if (modified != null && !modified.equals(perm)) {
                permissionList.alterPermission(rows[0], modified);
                revertPermAction.setEnabled(true);
            }
        }

    }


    private class DeletePermissionAction extends AbstractAction {

        public DeletePermissionAction() {
            super(resources.getString("Delete"));
            setEnabled(false);
            deletePermAction = this;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] selectedRows = permissionList.getSelectedRows();
            Arrays.sort(selectedRows);
            for (int i = selectedRows.length; i-- > 0;)
                permissionList.deletePermission(selectedRows[i]);
            permissionList.clearSelection();

            if (permissionList.isDirty())
                revertPermAction.setEnabled(true);
        }

    }


    private class RevertPermissionsAction extends AbstractAction {

        public RevertPermissionsAction() {
            super(resources.getString("Revert"));
            setEnabled(false);
            revertPermAction = this;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentlyEditing == null)
                return;

            // find the original/saved version of this role.
            Role revert = PermissionsManager.getInstance()
                    .getRoleByID(currentlyEditing.getId());

            // if this role could not be found, it has been newly added in
            // this invocation of the roles editor. Reset it to an empty state
            if (revert == null) {
                revert = new Role(null, currentlyEditing.getName(),
                        currentlyEditing.isInactive(), Collections.EMPTY_LIST);
                rolesToSave.add(revert);
            }

            rolesToSave.remove(currentlyEditing);
            permissionList.setContents(revert.getPermissions());

            int pos = roles.indexOf(currentlyEditing);
            currentlyEditing = revert;
            roles.set(pos, revert);

            setEnabled(false);
        }

    }


    private class PermissionSelectionHandler implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            int[] rows = {};
            if (!permissionList.isEmpty())
                rows = permissionList.getSelectedRows();

            editPermAction.setEnabled(rows.length == 1 && permissionList
                    .getPermission(rows[0]).getEditor() != null);
            deletePermAction.setEnabled(rows.length > 0);
        }

    }


    private static final String SIZE_PREF = "userPref.rolesEditor.dimensions";

}
