// Copyright (C) 2015 Tuma Solutions, LLC
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

import static net.sourceforge.processdash.ui.lib.BoxUtils.hbox;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
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

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.metal.MetalIconFactory;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.CheckboxList;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;

import teamdash.wbs.IconFactory;
import teamdash.wbs.TeamTimePanel;

public class SubteamBalancingMenu extends JMenu implements
        SubteamDataModel.Listener {

    /** The list of team members on this project. */
    private TeamMemberList teamList;

    /** The list of team members on this project. */
    private TeamTimePanel teamTimePanel;

    /** A user interface for selecting a group of individuals */
    private PersonSelector personSelector;

    /** Icons for indicating which subteam is selected/inactive */
    private Icon selectedIcon, unselectedIcon;

    private static Resources resources = Resources
            .getDashBundle("WBSEditor.Subteam");


    public SubteamBalancingMenu(TeamMemberList teamList,
            TeamTimePanel teamTimePanel) {
        super(resources.getString("Balance_Menu"));
        this.teamList = teamList;
        this.teamTimePanel = teamTimePanel;
        this.personSelector = new PersonSelector();

        selectedIcon = UIManager.getIcon("RadioButtonMenuItem.checkIcon");
        if (selectedIcon == null)
            selectedIcon = MetalIconFactory.getRadioButtonMenuItemIcon();
        unselectedIcon = IconFactory.getEmptyIcon(selectedIcon.getIconWidth(),
            selectedIcon.getIconHeight());

        add(new BalanceEntireTeam());
        add(new SelectIndividuals());

        teamList.getSubteamModel().addSubteamDataModelListener(this);
        rebuildSubteamMenus();
    }

    public void subteamDataModelChanged(SubteamDataModel.Event e) {
        rebuildSubteamMenus();
    }

    private void rebuildSubteamMenus() {
        // remove any existing subteam menus
        for (int i = getMenuComponentCount(); i-- > 0;) {
            if (getMenuComponent(i) instanceof BalanceNamedSubteam)
                remove(i);
        }

        // recreate the subteam menus from scratch and add them to our menu.
        int pos = 1;
        for (String oneName : teamList.getSubteamModel().getSubteamNames())
            add(new BalanceNamedSubteam(oneName), pos++);
    }

    private void applyNamedSubteam(String subteamName) {
        Set<Integer> subteamFilter = teamList.getSubteamModel()
                .getSubteamFilter(subteamName);
        if (subteamFilter == null)
            subteamName = null;

        teamTimePanel.applySubteamFilter(subteamFilter, subteamName);
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
            return !teamTimePanel.isSubteamFiltered();
        }

        public void actionPerformed(ActionEvent e) {
            teamTimePanel.applySubteamFilter(null, null);
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

        private Dimension scrollPaneSize;

        private JCheckBox saveSubteam;

        private JTextField subteamName;

        public SelectIndividuals() {
            super(resources.getString("Select_Individuals.Menu"));
            scrollPaneSize = new Dimension(150, 150);

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
            editSubteam();
        }

        private void editSubteam() {
            personSelector.loadStateFromTeamTimePanel();
            personSelector.clearSelection();
            JScrollPane sp = new JScrollPane(personSelector);
            sp.setPreferredSize(scrollPaneSize);
            personSelector.scrollRectToVisible(new Rectangle(0, 0));

            subteamName.setText(teamTimePanel.getSubteamName());
            saveSubteam.setSelected(false);

            String title = resources.getString("Select_Individuals.Title");
            Object[] message = new Object[] {
                    resources.getString("Select_Individuals.Header"), sp,
                    hbox(6, personSelector.toggleAll), " ",
                    hbox(6, saveSubteam), hbox(40, subteamName),
                    new JOptionPaneTweaker.MakeResizable() };

            int userChoice = JOptionPane.showConfirmDialog(
                SwingUtilities.getWindowAncestor(teamTimePanel), message,
                title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            scrollPaneSize = sp.getSize();
            if (userChoice != JOptionPane.OK_OPTION)
                return;

            Set<Integer> subteamFilter = personSelector.getFilter();
            if (subteamFilter == null) {
                teamTimePanel.applySubteamFilter(null, null);

            } else if (!subteamFilter.isEmpty()) {
                String name = getSaveSubteamName();
                if (!subteamFilter.equals(teamTimePanel.getSubteamFilter())
                        || name != null)
                    teamTimePanel.applySubteamFilter(subteamFilter, name);
                if (name != null)
                    teamList.getSubteamModel().saveSubteam(name, subteamFilter);
            }
        }

        public void subteamNameChanged() {
            boolean hasName = subteamName.getText().trim().length() > 0;
            saveSubteam.setSelected(hasName);
        }

        private String getSaveSubteamName() {
            if (saveSubteam.isSelected() == false)
                return null;
            String result = subteamName.getText().trim();
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
            reloadTeamMembers();

            Set<Integer> filter = teamTimePanel.getSubteamFilter();
            if (filter == null) {
                toggleAll.setSelected(true);
                setAllChecked(true);
            } else {
                toggleAll.setSelected(false);
                for (int i = teamMembers.size(); i-- > 0;)
                    setChecked(i, filter.contains(teamMembers.get(i).getId()));
            }
        }

        private void reloadTeamMembers() {
            teamMembers = teamList.getTeamMembers();
            String[] names = new String[teamMembers.size()];
            for (int i = names.length; i-- > 0;)
                names[i] = teamMembers.get(i).getName();
            setItems(names);
        }

        public Set<Integer> getFilter() {
            Set<Integer> filter = new HashSet<Integer>();
            for (int i = teamMembers.size(); i-- > 0;) {
                if (getChecked(i))
                    filter.add(teamMembers.get(i).getId());
            }
            if (filter.size() == teamMembers.size())
                return null;
            else
                return filter;
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == toggleAll)
                setAllChecked(toggleAll.isSelected());
        }

    }

}
