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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.CheckboxList;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;

import teamdash.wbs.TeamTimePanel;

public class SubteamBalancingMenu extends JMenu {

    /** The list of team members on this project. */
    private TeamMemberList teamList;

    /** The list of team members on this project. */
    private TeamTimePanel teamTimePanel;

    /** A user interface for selecting a group of individuals */
    private PersonSelector personSelector;

    private static Resources resources = Resources
            .getDashBundle("WBSEditor.Subteam");


    public SubteamBalancingMenu(TeamMemberList teamList,
            TeamTimePanel teamTimePanel) {
        super(resources.getString("Balance_Menu"));
        this.teamList = teamList;
        this.teamTimePanel = teamTimePanel;
        this.personSelector = new PersonSelector();
        add(new BalanceEntireTeam());
        add(new SelectIndividuals());
    }


    private class BalanceEntireTeam extends AbstractAction {

        BalanceEntireTeam() {
            super(resources.getString("Entire_Team"));
        }

        public void actionPerformed(ActionEvent e) {
            teamTimePanel.applySubteamFilter(null, null);
        }

    }


    private class SelectIndividuals extends AbstractAction {

        private Dimension scrollPaneSize;

        public SelectIndividuals() {
            super(resources.getString("Select_Individuals.Menu"));
            scrollPaneSize = new Dimension(150, 150);
        }

        public void actionPerformed(ActionEvent e) {
            editSubteam();
        }

        private void editSubteam() {
            personSelector.loadStateFromTeamTimePanel();
            personSelector.clearSelection();
            JScrollPane sp = new JScrollPane(personSelector);
            sp.setPreferredSize(scrollPaneSize);
            sp.scrollRectToVisible(new Rectangle(0, 0));

            String title = resources.getString("Select_Individuals.Title");
            Object[] message = new Object[] {
                    resources.getString("Select_Individuals.Header"), sp,
                    personSelector.toggleAll,
                    new JOptionPaneTweaker.MakeResizable() };

            int userChoice = JOptionPane.showConfirmDialog(
                SwingUtilities.getWindowAncestor(teamTimePanel), message,
                title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            scrollPaneSize = sp.getSize();
            if (userChoice != JOptionPane.OK_OPTION)
                return;

            Set<Integer> subteamFilter = personSelector.getFilter();
            if (subteamFilter == null)
                teamTimePanel.applySubteamFilter(null, null);
            else if (!subteamFilter.isEmpty())
                teamTimePanel.applySubteamFilter(subteamFilter,
                    resources.getString("Subteam"));
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
