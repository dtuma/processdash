// Copyright (C) 2014 Tuma Solutions, LLC
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

package teamdash.wbs.columns;

import java.awt.Component;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import teamdash.team.TeamMember;
import teamdash.team.TeamMemberList;
import teamdash.wbs.AssignedToComboBox;
import teamdash.wbs.AssignedToDocument;
import teamdash.wbs.AutocompletingDataTableCellEditor;
import teamdash.wbs.CustomEditedColumn;
import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class WorkflowResourcesColumn extends AbstractDataColumn implements
        CustomRenderedColumn, CustomEditedColumn {

    public static final String COLUMN_ID = "Performed By";

    private static final String ATTR_NAME = "Workflow Performed By";

    private DataTableModel dataModel;

    private WBSModel wbsModel;

    private TeamMemberList teamList;

    public WorkflowResourcesColumn(DataTableModel dataModel, TeamMemberList team) {
        this.dataModel = dataModel;
        this.wbsModel = dataModel.getWBSModel();
        this.teamList = team;
        this.columnID = COLUMN_ID;
        this.columnName = resources.getString("Workflow_Performed_By.Name");
        this.preferredWidth = 200;
        setConflictAttributeName(ATTR_NAME);
    }

    public boolean isCellEditable(WBSNode node) {
        return node.getIndentLevel() > 1 && wbsModel.isLeaf(node);
    }

    public Object getValueAt(WBSNode node) {
        return (isCellEditable(node) ? node.getAttribute(ATTR_NAME) : null);
    }

    public void setValueAt(Object aValue, WBSNode node) {
        String text = (String) aValue;
        if (text == null || text.trim().length() == 0) {
            node.removeAttribute(ATTR_NAME);
            return;
        }

        StringBuilder result = new StringBuilder();
        Matcher m = WORD_PAT.matcher(text);
        Set initials = getTeamInitials();
        Set wordsSeen = new HashSet();
        while (m.find()) {
            String word = m.group();
            if (wordsSeen.add(word)) {
                result.append(AssignedToDocument.SEPARATOR_SPACE);
                if (initials.contains(word))
                    result.append(word);
                else
                    result.append(ROLE_BEG).append(word).append(ROLE_END);
            }
        }
        text = (result.length() == 0 ? null : result.substring(2));
        node.setAttribute(ATTR_NAME, text);

        // update the num people column if applicable
        int numPeople = wordsSeen.size();
        if (numPeople > WorkflowNumPeopleColumn.getNumPeopleAt(node)) {
            int col = dataModel.findColumn(WorkflowNumPeopleColumn.COLUMN_ID);
            dataModel.setValueAt(Integer.toString(numPeople), node, col);
        }
    }

    private Set<String> getTeamInitials() {
        Set<String> result = new LinkedHashSet<String>();
        for (TeamMember m : teamList.getTeamMembers())
            result.add(m.getInitials());
        return result;
    }

    private Set<String> getRoleNamesInUse(Set<String> initials) {
        Set<String> result = new TreeSet<String>();
        for (WBSNode node : wbsModel.getDescendants(wbsModel.getRoot())) {
            String oneValue = (String) getValueAt(node);
            if (oneValue != null) {
                Matcher m = WORD_PAT.matcher(oneValue);
                while (m.find()) {
                    String word = m.group();
                    if (!initials.contains(word))
                        result.add(word);
                }
            }
        }
        return result;
    }

    public TableCellRenderer getCellRenderer() {
        return new WorkflowTableCellRenderer();
    }

    public TableCellEditor getCellEditor() {
        return performedByEditor;
    }


    private static class ComboBoxRenderer extends DefaultListCellRenderer {
        private JSeparator separator = new JSeparator();

        private int numRoles;

        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            if (value == null)
                return separator;
            else if (index < numRoles)
                value = ROLE_BEG + value + ROLE_END;
            return super.getListCellRendererComponent(list, value, index,
                isSelected, cellHasFocus);
        }
    }

    private class PerformedByEditor extends AutocompletingDataTableCellEditor {
        private AssignedToComboBox comboBox;

        private ComboBoxRenderer comboBoxRenderer;

        public PerformedByEditor() {
            super(new AssignedToComboBox(false));
            comboBox = (AssignedToComboBox) getComboBox();
            comboBox.setNumbersAllowed(false);
            comboBox.setRenderer(comboBoxRenderer = new ComboBoxRenderer());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int column) {
            // call super() so the editor setup timer will be restarted
            super.getTableCellEditorComponent(table, null, isSelected, row,
                column);

            // initialize the combo box contents and return it
            comboBox.setFullText(value == null ? "" : value + " ");
            comboBox.setInitialsList(getCompletionOptions());
            return comboBox;
        }

        private List<String> getCompletionOptions() {
            Set<String> teamInitials = getTeamInitials();
            Set<String> roles = getRoleNamesInUse(teamInitials);

            List<String> result = new LinkedList();
            result.addAll(roles);
            result.add(resources.getString("Workflow_Performed_By.New_Role"));
            comboBoxRenderer.numRoles = result.size();
            result.add(null);
            result.addAll(teamInitials);
            return result;
        }

        @Override
        public Object getCellEditorValue() {
            return comboBox.getFullText();
        }
    }

    private PerformedByEditor performedByEditor = new PerformedByEditor();


    private static final String ROLE_BEG = "\u00AB";

    private static final String ROLE_END = "\u00BB";

    private static final Pattern WORD_PAT = Pattern.compile("\\p{L}+");

}
