// Copyright (C) 2015-2020 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.team;

import static net.sourceforge.processdash.ui.lib.BoxUtils.GLUE;
import static net.sourceforge.processdash.ui.lib.BoxUtils.hbox;
import static net.sourceforge.processdash.ui.lib.BoxUtils.vbox;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.EventHandler;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.metal.MetalIconFactory;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.BoxUtils;
import net.sourceforge.processdash.ui.lib.CheckboxList;
import net.sourceforge.processdash.ui.lib.GuiPrefs;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;

import teamdash.wbs.IconFactory;
import teamdash.wbs.TeamTimePanel;

public class SubteamBalancingMenu extends JMenu implements
        SubteamDataModel.Listener {

    /** The list of team members on this project. */
    private TeamMemberList teamList;

    /** The list of team members on this project. */
    private TeamTimePanel teamTimePanel;

    /** An object to be notified when we save changes */
    private ChangeListener dirtyListener;

    /** The menu that that displays the team time panel */
    private JMenuItem showTimePanelMenu;

    /** A user interface for selecting a group of individuals */
    private PersonSelector personSelector;

    /** A scroll pane showing the person selector */
    private JScrollPane personSelectorPane;

    /** A model for tracking the name of the active subteam */
    private ComboBoxModel activeSubteamName;

    /** Icons for indicating which subteam is selected/inactive */
    private Icon selectedIcon, unselectedIcon;

    private static Resources resources = Resources
            .getDashBundle("WBSEditor.Subteam");


    public SubteamBalancingMenu(TeamMemberList teamList,
            TeamTimePanel teamTimePanel, ChangeListener dirtyListener,
            JMenuItem showTimePanelMenu, GuiPrefs prefs) {
        super(resources.getString("Balance_Menu"));
        this.teamList = teamList;
        this.teamTimePanel = teamTimePanel;
        this.dirtyListener = dirtyListener;
        this.showTimePanelMenu = showTimePanelMenu;
        this.personSelector = new PersonSelector();
        this.personSelectorPane = new JScrollPane(personSelector);
        this.personSelectorPane.setPreferredSize(new Dimension(150, 150));

        // restore the active subteam from the last editing session
        activeSubteamName = new DefaultComboBoxModel();
        prefs.load("teamTimePanel.activeSubteam", activeSubteamName);
        String previousSubteam = (String) activeSubteamName.getSelectedItem();
        if (previousSubteam != null)
            applyNamedSubteamImpl(previousSubteam);

        // retrieve icons used for indicating the selected subteam
        selectedIcon = UIManager.getIcon("RadioButtonMenuItem.checkIcon");
        if (selectedIcon == null)
            selectedIcon = MetalIconFactory.getRadioButtonMenuItemIcon();
        unselectedIcon = IconFactory.getEmptyIcon(selectedIcon.getIconWidth(),
            selectedIcon.getIconHeight());

        // create menus for creating and activating subteams
        add(new BalanceEntireTeam());
        add(new SelectIndividuals());
        addSeparator();
        add(new ManageSubteamsAction());

        getSubteamModel().addSubteamDataModelListener(this);
        rebuildSubteamMenus();

        teamList.addTableModelListener(EventHandler.create( //
            TableModelListener.class, this, "updateVisibility"));
        updateVisibility();
    }

    public void updateVisibility() {
        setVisible(teamList.getRowCount() > 1);
    }

    public void subteamDataModelChanged(SubteamDataModel.Event e) {
        // recreate the subteam menus if needed
        rebuildSubteamMenus();

        // check to see if the definition of the currently active subteam
        // has changed. If so, tell the team time panel to reapply the filter.
        String currentName = teamTimePanel.getSubteamName();
        if (currentName != null) {
            Set<Integer> currentFilter = teamTimePanel.getSubteamFilter();
            Set<Integer> officialFilter = getSubteamModel().getSubteamFilter(
                currentName);
            if (officialFilter == null)
                // the active subteam was deleted. Reapply the existing filter
                // with a null name, so it will be labeled as "Subteam."
                teamTimePanel.applySubteamFilter(currentFilter, null);
            else if (!officialFilter.equals(currentFilter))
                // the active subteam filter has been edited. Reapply so the
                // team time panel shows the new subteam definition.
                teamTimePanel.applySubteamFilter(officialFilter, currentName);
        }
    }

    private void rebuildSubteamMenus() {
        // remove any existing subteam menus
        for (int i = getMenuComponentCount(); i-- > 0;) {
            if (getMenuComponent(i) instanceof BalanceNamedSubteam)
                remove(i);
        }

        // recreate the subteam menus from scratch and add them to our menu.
        int pos = 1;
        for (String oneName : getSubteamModel().getSubteamNames())
            add(new BalanceNamedSubteam(oneName), pos++);
    }

    private void applyNamedSubteam(String subteamName) {
        applyNamedSubteamImpl(subteamName);
        showTimePanel();
    }

    private void applyNamedSubteamImpl(String subteamName) {
        Set<Integer> subteamFilter = getSubteamModel().getSubteamFilter(
            subteamName);
        if (subteamFilter == null)
            subteamName = null;

        teamTimePanel.applySubteamFilter(subteamFilter, subteamName);
    }

    private SubteamDataModel getSubteamModel() {
        return teamList.getSubteamModel();
    }

    private Frame getParentWindow() {
        return (Frame) SwingUtilities.getWindowAncestor(teamTimePanel);
    }

    private void showTimePanel() {
        showTimePanelMenu.setSelected(true);
    }



    private abstract class BalanceOptionMenu extends JMenuItem implements
            ActionListener, ChangeListener {

        protected BalanceOptionMenu(String name) {
            super(name);
            addActionListener(this);
            teamTimePanel.addSubteamFilterListener(this);
            stateChanged(null);
        }

        public void stateChanged(ChangeEvent e) {
            if (e == null || e.getSource() == teamTimePanel) {
                boolean isActive = isActiveSubteamMenu();
                setSelected(isActive);
                setIcon(isActive ? selectedIcon : unselectedIcon);
            }
        }

        protected abstract boolean isActiveSubteamMenu();

        public abstract void actionPerformed(ActionEvent e);

    }


    private class BalanceEntireTeam extends BalanceOptionMenu {

        BalanceEntireTeam() {
            super(resources.getString("Entire_Team"));
            setFont(getFont().deriveFont(Font.BOLD + Font.ITALIC));
        }

        protected boolean isActiveSubteamMenu() {
            activeSubteamName.setSelectedItem(teamTimePanel.getSubteamName());
            return !teamTimePanel.isSubteamFiltered();
        }

        public void actionPerformed(ActionEvent e) {
            applyNamedSubteam(null);
        }

    }


    private class BalanceNamedSubteam extends BalanceOptionMenu {

        BalanceNamedSubteam(String subteamName) {
            super(subteamName);
        }

        protected boolean isActiveSubteamMenu() {
            return getText().equalsIgnoreCase(teamTimePanel.getSubteamName());
        }

        public void actionPerformed(ActionEvent e) {
            applyNamedSubteam(getText());
        }

    }


    public class SelectIndividuals extends BalanceOptionMenu {

        private JCheckBox saveSubteam;

        private JTextField subteamName;

        public SelectIndividuals() {
            super(resources.getString("Select_Individuals.Menu"));

            saveSubteam = new JCheckBox(
                    resources.getString("Select_Individuals.Save_Subteam"));
            saveSubteam.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (saveSubteam.isSelected())
                        subteamName.requestFocusInWindow();
                }
            });

            subteamName = new JTextField();
            subteamName.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    subteamName.selectAll();
                }
            });
            subteamName.getDocument().addDocumentListener(
                EventHandler.create(DocumentListener.class, this,
                    "subteamNameChanged"));
        }

        protected boolean isActiveSubteamMenu() {
            return teamTimePanel.isSubteamFiltered()
                    && teamTimePanel.getSubteamName() == null;
        }

        public void actionPerformed(ActionEvent e) {
            personSelector.loadStateFromTeamTimePanel();
            subteamName.setText(teamTimePanel.getSubteamName());
            saveSubteam.setSelected(false);

            String title = resources.getString("Select_Individuals.Title");
            Object[] message = new Object[] {
                    resources.getString("Select_Individuals.Header"),
                    personSelectorPane,
                    hbox(6, personSelector.toggleAll), " ",
                    hbox(6, saveSubteam), hbox(40, subteamName),
                    new JOptionPaneTweaker.MakeResizable() };

            int userChoice = JOptionPane.showConfirmDialog(getParentWindow(),
                message, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
            personSelectorPane.setPreferredSize(personSelectorPane.getSize());
            if (userChoice != JOptionPane.OK_OPTION)
                return;

            Set<Integer> subteamFilter = personSelector.getFilter(true);
            if (subteamFilter == null) {
                teamTimePanel.applySubteamFilter(null, null);

            } else if (!subteamFilter.isEmpty()) {
                String name = getSaveSubteamName();
                if (!subteamFilter.equals(teamTimePanel.getSubteamFilter())
                        || name != null)
                    teamTimePanel.applySubteamFilter(subteamFilter, name);
                if (name != null) {
                    getSubteamModel().saveSubteam(name, subteamFilter);
                    dirtyListener.stateChanged(new ChangeEvent(this));
                }
            }
            showTimePanel();
        }

        public void subteamNameChanged() {
            boolean hasName = subteamName.getText().trim().length() > 0;
            saveSubteam.setSelected(hasName);
        }

        private String getSaveSubteamName() {
            if (saveSubteam.isSelected() == false)
                return null;
            String result = subteamName.getText().trim().replace('\t', ' ');
            return (result.length() > 0 ? result : null);
        }

    }


    private class PersonSelector extends CheckboxList implements ActionListener {

        private List<TeamMember> teamMembers;

        protected JCheckBox toggleAll;

        public PersonSelector() {
            super(new Object[0]);
            toggleAll = new JCheckBox(
                    resources.getString("Select_Individuals.Select_All"), true);
            toggleAll.addActionListener(this);
        }

        public void loadStateFromTeamTimePanel() {
            loadStateFromFilter(teamTimePanel.getSubteamFilter());
        }

        public void loadStateFromSubteam(String name) {
            loadStateFromFilter(getSubteamModel().getSubteamFilter(name));
        }

        protected void loadStateFromFilter(Set<Integer> filter) {
            reloadTeamMembers();
            int scrollToRow = 0;
            if (filter == null) {
                toggleAll.setSelected(true);
                setAllChecked(true);
            } else {
                toggleAll.setSelected(false);
                setAllChecked(false);
                for (int i = teamMembers.size(); i-- > 0;) {
                    if (filter.contains(teamMembers.get(i).getId())) {
                        setChecked(i, true);
                        scrollToRow = i - 1;
                    }
                }
            }
            personSelectorPane.getVerticalScrollBar().setValue(
                getCellRect(Math.max(0, scrollToRow), 0, true).y);
        }

        private void reloadTeamMembers() {
            teamMembers = teamList.getTeamMembers();
            String[] names = new String[teamMembers.size()];
            for (int i = names.length; i-- > 0;)
                names[i] = teamMembers.get(i).getName();
            setItems(names);
        }

        public Set<Integer> getFilter(boolean rejectWholeTeam) {
            Set<Integer> filter = new HashSet<Integer>();
            for (int i = teamMembers.size(); i-- > 0;) {
                if (getChecked(i))
                    filter.add(teamMembers.get(i).getId());
            }
            if (rejectWholeTeam && filter.size() == teamMembers.size())
                return null;
            else
                return filter;
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == toggleAll)
                setAllChecked(toggleAll.isSelected());
        }

    }


    private class ManageSubteamsAction extends AbstractAction {

        ManagerDialog dialog;

        private ManageSubteamsAction() {
            super(resources.getString("Manage.Menu"));
        }

        public void actionPerformed(ActionEvent e) {
            if (dialog == null) {
                dialog = new ManagerDialog();
            } else {
                dialog.setVisible(true);
                dialog.toFront();
            }
        }

    }


    public class ManagerDialog extends JDialog implements
            SubteamDataModel.Listener, ListSelectionListener {

        private DefaultListModel model;

        private JList subteamList;

        private JButton newButton, editButton, deleteButton, balanceButton,
                closeButton;

        private JTextField subteamName;

        private boolean savingChange;

        private ManagerDialog() {
            super(getParentWindow(), resources.getString("Manage.Title"), false);

            model = new DefaultListModel();
            subteamList = new JList(model);
            subteamList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            subteamList.addListSelectionListener(this);
            subteamList.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2)
                        balanceSubteam();
                }
            });
            rebuildListOfSubteams();
            getSubteamModel().addSubteamDataModelListener(this);

            JScrollPane sp = new JScrollPane(subteamList);
            sp.setPreferredSize(new Dimension(150, 150));

            newButton = makeButton("New", "newSubteam");
            editButton = makeButton("Edit", "editSubteam");
            deleteButton = makeButton("Delete", "deleteSubteam");
            balanceButton = makeButton("Balance", "balanceSubteam");
            closeButton = makeButton("Close", "closeDialog");
            valueChanged(null);

            subteamName = new JTextField();

            BoxUtils buttons = vbox(newButton, GLUE, editButton, GLUE,
                deleteButton, GLUE, balanceButton);
            BoxUtils content = vbox(
                hbox(resources.getString("Manage.Header"), GLUE),
                hbox(sp, 10, buttons), 10, hbox(GLUE, closeButton, GLUE));
            content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            getContentPane().add(content);
            pack();
            setLocationRelativeTo(getParentWindow());
            setVisible(true);
        }

        private JButton makeButton(String resKey, String method) {
            String text = resources.getString("Manage." + resKey + ".Button");
            JButton result = new JButton(text);
            result.addActionListener(EventHandler.create(ActionListener.class,
                this, method));
            Dimension d = result.getPreferredSize();
            d.width = 200;
            result.setMaximumSize(d);
            return result;
        }

        // handle changes to the selected item in the list
        public void valueChanged(ListSelectionEvent e) {
            boolean hasSelection = (subteamList.getSelectedIndex() != -1);
            editButton.setEnabled(hasSelection);
            deleteButton.setEnabled(hasSelection);
            balanceButton.setEnabled(hasSelection);
        }

        // handle external changes to the subteam model
        public void subteamDataModelChanged(SubteamDataModel.Event e) {
            if (!savingChange)
                rebuildListOfSubteams();
        }

        private void saveWithoutRebuildingSubteamList(String subteamName,
                Set<Integer> subteamFilter) {
            try {
                savingChange = true;
                getSubteamModel().saveSubteam(subteamName, subteamFilter);
                dirtyListener.stateChanged(new ChangeEvent(this));
            } finally {
                savingChange = false;
            }
        }

        private void rebuildListOfSubteams() {
            String currentItem = (String) subteamList.getSelectedValue();
            model.clear();
            for (String name : getSubteamModel().getSubteamNames())
                model.addElement(name);
            subteamList.setSelectedValue(currentItem, true);
        }

        public void newSubteam() {
            Set<Integer> filter = showEditDialog("Manage.New.Title", null);
            if (filter != null) {
                String name = getSaveName();
                saveWithoutRebuildingSubteamList(name, filter);
                applyNamedSubteam(name);
                rebuildListOfSubteams();
                subteamList.setSelectedValue(name, true);
            }
        }

        public void editSubteam() {
            String origName = (String) subteamList.getSelectedValue();
            if (origName == null)
                return;

            Set<Integer> filter = showEditDialog("Manage.Edit.Title", origName);
            if (filter != null) {
                String newName = getSaveName();
                saveWithoutRebuildingSubteamList(newName, filter);
                applyNamedSubteam(newName);
                if (!origName.equalsIgnoreCase(newName))
                    saveWithoutRebuildingSubteamList(origName, null);
                rebuildListOfSubteams();
                subteamList.setSelectedValue(newName, true);
            }
        }

        private Set<Integer> showEditDialog(String titleKey, String name) {
            personSelector.loadStateFromSubteam(name);
            subteamName.setText(name);

            String title = resources.getString(titleKey);
            Object message = new Object[] {
                    hbox(resources.getString("Manage.Name"), 5, subteamName),
                    Box.createVerticalStrut(5), personSelectorPane,
                    hbox(6, personSelector.toggleAll),
                    new JOptionPaneTweaker.MakeResizable() };

            while (true) {
                // display the dialog to the user
                int userChoice = JOptionPane.showConfirmDialog(this, message,
                    title, JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
                personSelectorPane.setPreferredSize(personSelectorPane
                        .getSize());

                // if they didn't press OK, abort the operation
                if (userChoice != JOptionPane.OK_OPTION)
                    return null;

                // display an error if they didn't enter a subteam name
                String saveName = getSaveName();
                if (saveName.length() == 0) {
                    JOptionPane.showMessageDialog(this,
                        resources.getString("Manage.Name_Empty.Message"),
                        resources.getString("Manage.Name_Empty.Title"),
                        JOptionPane.ERROR_MESSAGE);
                    continue;
                }

                // display an error if they didn't select any people
                Set<Integer> filter = personSelector.getFilter(false);
                if (filter.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                        resources.getString("Manage.Subteam_Empty.Message"),
                        resources.getString("Manage.Subteam_Empty.Title"),
                        JOptionPane.ERROR_MESSAGE);
                    continue;
                }

                // ask for confirmation before overwriting another subteam
                if (!saveName.equalsIgnoreCase(name)
                        && getSubteamModel().getSubteamFilter(saveName) != null) {
                    String overwriteTitle = resources
                            .getString("Manage.Overwrite.Title");
                    String[] overwriteMessage = resources.formatStrings(
                        "Manage.Overwrite.Message_FMT", saveName);
                    userChoice = JOptionPane.showConfirmDialog(this,
                        overwriteMessage, overwriteTitle,
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (userChoice != JOptionPane.YES_OPTION)
                        continue;
                }

                return filter;
            }
        }

        private String getSaveName() {
            return subteamName.getText().trim().replace('\t', ' ');
        }

        public void deleteSubteam() {
            String selectedSubteam = (String) subteamList.getSelectedValue();
            if (selectedSubteam == null)
                return;

            // ask for confirmation before deleting the subteam
            String title = resources.getString("Manage.Delete.Title");
            String[] message = resources.formatStrings(
                "Manage.Delete.Message_FMT", selectedSubteam);
            int userChoice = JOptionPane.showConfirmDialog(this, message,
                title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

            if (userChoice == JOptionPane.YES_OPTION) {
                model.removeElement(selectedSubteam);
                saveWithoutRebuildingSubteamList(selectedSubteam, null);
            }
        }

        public void balanceSubteam() {
            String selectedSubteam = (String) subteamList.getSelectedValue();
            if (selectedSubteam != null)
                applyNamedSubteam(selectedSubteam);
        }

        public void closeDialog() {
            setVisible(false);
        }
    }

}
