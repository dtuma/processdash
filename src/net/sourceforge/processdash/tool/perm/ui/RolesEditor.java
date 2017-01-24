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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
import net.sourceforge.processdash.tool.perm.PermissionsManager;
import net.sourceforge.processdash.tool.perm.Role;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.util.StringUtils;

public class RolesEditor {

    private AbstractAction copyRoleAction, renameRoleAction, deleteRoleAction;

    private DefaultListModel<Role> roles;

    private JList<Role> rolesList;

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
        roleButtons.add(new JButton(copyRoleAction = new CopyRoleAction()));
        roleButtons.add(new JButton(renameRoleAction = new RenameRoleAction()));
        roleButtons.add(new JButton(deleteRoleAction = new DeleteRoleAction()));

        // create the permissions button panel
        JPanel permButtons = new JPanel(new GridLayout(1, 3, 5, 5));
        permButtons.add(new JButton("Add")); // FIXME
        permButtons.add(new JButton("Edit")); // FIXME
        permButtons.add(new JButton("Delete")); // FIXME

        // read the known roles, and add them to a list
        roles = new DefaultListModel();
        for (Role r : PermissionsManager.getInstance().getAllRoles())
            roles.addElement(r);
        rolesList = new JList(roles);
        rolesList.setVisibleRowCount(15);
        rolesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rolesList.addListSelectionListener(new RoleSelectionHandler());
        JScrollPane rsp = new JScrollPane(rolesList);
        rsp.setPreferredSize(new Dimension(200, 300)); // FIXME

        // create an object for editing the permissions in the selected role
        permissionList = new PermissionList();
        JScrollPane psp = new JScrollPane(permissionList);
        psp.setPreferredSize(new Dimension(200, 300)); // FIXME

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
        Component comp = new JOptionPaneTweaker.MakeResizable();
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
                Role r = roles.get(i);
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
            Role r = roles.get(i);
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
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentlyEditing == null)
                return;

            String title = resources.getString("Delete_Title");
            String prompt = resources.format("Delete_Prompt_FMT",
                currentlyEditing.getName());
            int userChoice = JOptionPane.showConfirmDialog(userInterface,
                prompt, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

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

            Role selected = rolesList.getSelectedValue();
            boolean hasSelection = (selected != null);
            copyRoleAction.setEnabled(hasSelection);
            renameRoleAction.setEnabled(hasSelection);
            deleteRoleAction.setEnabled(hasSelection);

            if (selected != currentlyEditing) {
                currentlyEditing = selected;
                if (selected == null)
                    permissionList.clearList();
                else
                    permissionList.setContents(selected.getPermissions());
            }
        }

    }

    private static final String SIZE_PREF = "userPref.rolesEditor.dimensions";

}
