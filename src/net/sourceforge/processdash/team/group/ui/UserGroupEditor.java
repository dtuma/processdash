// Copyright (C) 2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.group.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.team.group.UserGroup;
import net.sourceforge.processdash.team.group.UserGroupManager;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.util.StringUtils;

public class UserGroupEditor {

    private AbstractAction copyAction, renameAction, deleteAction;

    private DefaultListModel groups;

    private JList groupList;

    private UserGroup everyone, currentlyEditing;

    private GroupMembershipSelector memberList;

    private JPanel userInterface;

    private Set<UserGroup> groupsToSave, groupsToDelete;

    private static final Resources resources = Resources
            .getDashBundle("ProcessDashboard.Groups");


    public UserGroupEditor() {
        // keep track of editing state
        currentlyEditing = null;
        groupsToSave = new HashSet<UserGroup>();
        groupsToDelete = new HashSet<UserGroup>();
        everyone = UserGroupManager.getInstance().getEveryone();

        // build a user interface and display it to the user
        int userChoice = JOptionPane.showConfirmDialog(null, createUI(),
            resources.getString("Edit_Window_Title"),
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        // if the user clicked OK, save their changes
        if (userChoice == JOptionPane.OK_OPTION)
            saveChanges();

        // store the preferred size of the window for next use
        InternalSettings.set(SIZE_PREF, userInterface.getWidth() + ","
                + userInterface.getHeight());
    }

    private void saveChanges() {
        saveMembershipChanges();
        for (UserGroup g : groupsToDelete) {
            if (g.getId() != null)
                UserGroupManager.getInstance().deleteGroup(g);
        }
        for (UserGroup g : groupsToSave) {
            UserGroupManager.getInstance().saveGroup(g);
        }
    }

    private Component createUI() {
        // create the button panel
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        buttonPanel.add(new JButton(new AddAction()));
        buttonPanel.add(new JButton(copyAction = new CopyAction()));
        buttonPanel.add(new JButton(renameAction = new RenameAction()));
        buttonPanel.add(new JButton(deleteAction = new DeleteAction()));

        // read the known groups, and add them to a list
        List<UserGroup> allGroups = new ArrayList<UserGroup>();
        allGroups.addAll(UserGroupManager.getInstance().getGroups().values());
        Collections.sort(allGroups);
        groups = new DefaultListModel();
        for (UserGroup g : allGroups)
            groups.addElement(g);
        groupList = new JList(groups);
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupList.addListSelectionListener(new SelectionHandler());
        JScrollPane gsp = new JScrollPane(groupList);
        gsp.setPreferredSize(new Dimension(200, 300));

        // create an object for editing the members in the selected group
        memberList = new GroupMembershipSelector();
        JScrollPane msp = new JScrollPane(memberList);
        msp.setPreferredSize(new Dimension(200, 300));

        // create titles to display on the dialog
        JLabel groupTitle = new JLabel(resources.getString("User_Groups"));
        JLabel memberTitle = new JLabel(
                resources.getString("Selected_User_Group"));
        Font f = groupTitle.getFont();
        f = f.deriveFont(f.getSize2D() * 1.5f);
        groupTitle.setFont(f);
        memberTitle.setFont(f);
        Border b = BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black);
        groupTitle.setBorder(b);
        memberTitle.setBorder(b);

        // arrange the components onto a panel
        GridBagLayout layout = new GridBagLayout();
        userInterface = new JPanel(layout);
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        layout.addLayoutComponent(groupTitle, c);
        userInterface.add(groupTitle);

        c.gridx = 2;
        Component comp = new JOptionPaneTweaker.MakeResizable();
        layout.addLayoutComponent(comp, c);
        userInterface.add(comp);

        c.gridx = 1;
        c.insets = new Insets(0, 10, 0, 0);
        layout.addLayoutComponent(memberTitle, c);
        userInterface.add(memberTitle);

        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(10, 20, 0, 0);
        layout.addLayoutComponent(buttonPanel, c);
        userInterface.add(buttonPanel);

        c.gridy = 2;
        c.weightx = c.weighty = 1.0;
        layout.addLayoutComponent(gsp, c);
        userInterface.add(gsp);

        c.gridx = c.gridy = 1;
        c.insets = new Insets(10, 30, 0, 0);
        c.gridheight = 2;
        layout.addLayoutComponent(msp, c);
        userInterface.add(msp);

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

    private void saveMembershipChanges() {
        if (currentlyEditing != null && memberList.isDirty()) {
            UserGroup updated = new UserGroup(
                    currentlyEditing.getDisplayName(),
                    currentlyEditing.getId(), //
                    currentlyEditing.isCustom(),
                    memberList.getSelectedMembers());
            groupsToSave.remove(currentlyEditing);
            groupsToSave.add(updated);
            memberList.clearDirty();

            int pos = groups.indexOf(currentlyEditing);
            currentlyEditing = updated;
            groups.set(pos, updated);
        }
    }

    private Object[] promptForName(String resKey, String resArg,
            String defaultName, boolean isCustom, boolean showCustom) {
        String title = resources.getString(resKey + "_Title");
        String prompt;
        if (resArg == null)
            prompt = resources.getString(resKey + "_Prompt");
        else
            prompt = resources.format(resKey + "_Prompt_FMT", resArg);

        JTextField nameField = new JTextField(defaultName);

        Object[] typePanel = null;
        JRadioButton sharedOption, customOption = null;
        if (showCustom) {
            String typePrompt = resources.getString("Type_Prompt");
            Border indent = BorderFactory.createEmptyBorder(0, 20, 0, 0);
            sharedOption = new JRadioButton(resources.getString("Type_Shared"));
            sharedOption.setBorder(indent);
            customOption = new JRadioButton(resources.getString("Type_Custom"));
            customOption.setBorder(indent);

            ButtonGroup bg = new ButtonGroup();
            bg.add(sharedOption);
            bg.add(customOption);
            (isCustom ? customOption : sharedOption).setSelected(true);

            typePanel = new Object[] { " ", typePrompt, sharedOption,
                    customOption };
        }

        Object message = new Object[] { prompt, nameField, typePanel,
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

            // when renaming, if they did not alter the value, abort.
            if (defaultName != null && name.equals(defaultName))
                return null;

            // make a note of whether this is a custom group.
            boolean custom = showCustom ? customOption.isSelected() : isCustom;

            // if they entered a duplicate name, display an error
            for (int i = groups.size(); i-- > 0;) {
                UserGroup g = (UserGroup) groups.get(i);
                if (g.isCustom() == custom && g.getDisplayName().equals(name)) {
                    JOptionPane.showMessageDialog(userInterface,
                        resources.format("Name_Duplicate_FMT", name),
                        resources.getString("Name_Error_Title"),
                        JOptionPane.ERROR_MESSAGE);
                    continue PROMPT;
                }
            }

            // all seems OK. Return the values the user selected.
            return new Object[] { name, custom };
        }
    }

    private void addGroupToListAndSelect(UserGroup add) {
        for (int i = 0; i < groups.size(); i++) {
            UserGroup g = (UserGroup) groups.get(i);
            if (add.compareTo(g) < 0) {
                groups.add(i, add);
                groupList.setSelectedValue(add, true);
                return;
            }
        }
        groups.addElement(add);
        groupList.setSelectedValue(add, true);
    }


    private class AddAction extends AbstractAction {

        public AddAction() {
            super(resources.getString("Add"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            saveMembershipChanges();
            Object[] userEntry = promptForName("Add", null, null, false, true);
            if (userEntry == null)
                return;

            String name = (String) userEntry[0];
            Boolean custom = (Boolean) userEntry[1];
            UserGroup newGroup = new UserGroup(name, null, custom,
                    Collections.EMPTY_SET);
            groupsToSave.add(newGroup);
            addGroupToListAndSelect(newGroup);
        }
    }


    private class CopyAction extends AbstractAction {

        public CopyAction() {
            super(resources.getString("Copy"));
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentlyEditing == null)
                return;

            saveMembershipChanges();
            Object[] userEntry = promptForName("Copy",
                currentlyEditing.getDisplayName(), null,
                currentlyEditing.isCustom(), true);
            if (userEntry == null)
                return;

            String name = (String) userEntry[0];
            Boolean custom = (Boolean) userEntry[1];
            UserGroup newGroup = new UserGroup(name, null, custom,
                    currentlyEditing.getMembers());
            groupsToSave.add(newGroup);
            addGroupToListAndSelect(newGroup);
        }
    }


    private class RenameAction extends AbstractAction {

        public RenameAction() {
            super(resources.getString("Rename"));
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentlyEditing == null)
                return;

            saveMembershipChanges();
            Object[] userEntry = promptForName("Rename",
                currentlyEditing.getDisplayName(),
                currentlyEditing.getDisplayName(), //
                currentlyEditing.isCustom(), false);
            if (userEntry == null)
                return;

            String name = (String) userEntry[0];
            UserGroup renamed = new UserGroup(name, currentlyEditing.getId(),
                    currentlyEditing.isCustom(), currentlyEditing.getMembers());
            groupsToSave.remove(currentlyEditing);
            groupsToSave.add(renamed);
            groups.removeElement(currentlyEditing);
            addGroupToListAndSelect(renamed);
        }
    }


    private class DeleteAction extends AbstractAction {

        public DeleteAction() {
            super(resources.getString("Delete"));
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentlyEditing == null)
                return;

            String title = resources.getString("Delete_Title");
            String prompt = resources.format("Delete_Prompt_FMT",
                currentlyEditing.getDisplayName());
            int userChoice = JOptionPane.showConfirmDialog(userInterface,
                prompt, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

            if (userChoice == JOptionPane.YES_OPTION) {
                UserGroup delete = currentlyEditing;
                memberList.clearDirty();
                groupList.clearSelection();
                groups.removeElement(delete);
                groupsToSave.remove(delete);
                groupsToDelete.add(delete);
            }
        }
    }


    private class SelectionHandler implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            saveMembershipChanges();

            UserGroup selected = (UserGroup) groupList.getSelectedValue();
            boolean hasSelection = (selected != null);
            copyAction.setEnabled(hasSelection);
            renameAction.setEnabled(hasSelection);
            deleteAction.setEnabled(hasSelection);

            if (selected != currentlyEditing) {
                currentlyEditing = selected;
                if (selected == null)
                    memberList.clearList();
                else
                    memberList.setData(everyone, selected);
            }
        }

    }

    private static final String SIZE_PREF = "userPref.userGroupEditor.dimensions";

}
