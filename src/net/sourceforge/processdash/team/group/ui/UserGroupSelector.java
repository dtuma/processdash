// Copyright (C) 2016-2017 Tuma Solutions, LLC
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.team.group.UserFilter;
import net.sourceforge.processdash.team.group.UserGroup;
import net.sourceforge.processdash.team.group.UserGroupManager;
import net.sourceforge.processdash.team.group.UserGroupMember;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.lib.AbstractTreeTableModel;
import net.sourceforge.processdash.ui.lib.JFilterableTreeComponent;
import net.sourceforge.processdash.ui.lib.JOptionPaneActionHandler;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.ui.lib.JTreeTable;
import net.sourceforge.processdash.ui.lib.TreeTableModel;

public class UserGroupSelector {

    private UserGroup everyoneOption;

    private String groupHeader, indivHeader, loadingLabel, noneFoundLabel;

    private FilterChoices filterChoices;

    private JFilterableTreeComponent selector;

    private UserFilter selectedItem;

    private static final Resources resources = Resources
            .getDashBundle("ProcessDashboard.Groups");


    public UserGroupSelector(Component parent, String resKey, boolean showIndivs) {
        everyoneOption = UserGroup.EVERYONE;
        groupHeader = resources.getString("Groups");
        indivHeader = showIndivs ? resources.getString("Individuals") : null;
        loadingLabel = resources.getString("Loading");
        noneFoundLabel = resources.getString("None_Found");

        // create a component for selecting a group filter
        filterChoices = new FilterChoices();
        selector = new JFilterableTreeComponent(filterChoices,
                resources.getString("Find") + " ", false);
        selector.getTreeTable().setTableHeader(null);
        new ChoiceRenderer(selector);
        new JOptionPaneActionHandler().install(selector);

        // load the preferred size of the window
        try {
            String[] size = Settings.getVal(SIZE_PREF).split(",");
            Dimension d = new Dimension(Integer.parseInt(size[0]),
                    Integer.parseInt(size[1]));
            selector.setPreferredSize(d);
        } catch (Exception e) {
            selector.setPreferredSize(new Dimension(350, 350));
        }

        // display a dialog with the group filter selector
        String title = resources.getString("Filter_To_Group");
        Object content = new Object[] { resources.getString(resKey), selector,
                new JOptionPaneTweaker.MakeResizable(),
                new JOptionPaneTweaker.GrabFocus(selector.getFilterTextField()) };
        int userChoice = JOptionPane.showConfirmDialog(parent, content, title,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        // if the user made a selection, save it
        if (userChoice == JOptionPane.OK_OPTION) {
            Object selection = selector.getSelectedLeaf();
            if (selection instanceof UserFilter)
                selectedItem = (UserFilter) selection;
        }

        // store the preferred size of the window for next use
        InternalSettings.set(SIZE_PREF,
            selector.getWidth() + "," + selector.getHeight());
    }

    /**
     * @return the object the user selected with this component
     */
    public UserFilter getSelectedItem() {
        return selectedItem;
    }



    /**
     * Tree model that holds the various options the user can choose from for
     * filtering
     */
    private class FilterChoices extends AbstractTreeTableModel {

        private static final String ROOT_OBJECT = "ROOT";

        private List<UserGroup> groups;

        private List<UserGroupMember> people;

        public FilterChoices() {
            super(ROOT_OBJECT);

            // retrieve the list of groups from the user group manager
            groups = loadGroups();

            // the list of people cannot be loaded until all projects have
            // been loaded. On a large team dashboard with many projects, this
            // could take time. If this is the first time this window has
            // opened, run the task on a background thread.
            if (indivHeader == null)
                people = null;
            else if (peopleHaveBeenLoadedBefore)
                people = loadPeople();
            else
                new PeopleLoader(this).execute();
        }

        public int getColumnCount() {
            return 1;
        }

        public Class getColumnClass(int column) {
            return TreeTableModel.class;
        }

        public String getColumnName(int column) {
            return " ";
        }

        public boolean isCellEditable(Object node, int column) {
            return false;
        }

        public Object getValueAt(Object node, int column) {
            return node;
        }

        @Override
        public Object getChild(Object parent, int index) {
            if (parent == ROOT_OBJECT) {
                switch (index) {
                case 0:
                    return everyoneOption;
                case 1:
                    return groupHeader;
                case 2:
                    return indivHeader;
                default:
                    return null;
                }

            } else if (parent == groupHeader) {
                return groups.isEmpty() ? noneFoundLabel : groups.get(index);

            } else if (parent == indivHeader) {
                return people == null ? loadingLabel
                        : (people.isEmpty() ? noneFoundLabel //
                                : people.get(index));

            } else
                return null;
        }

        @Override
        public int getChildCount(Object parent) {
            if (parent == ROOT_OBJECT)
                return indivHeader == null ? 2 : 3;
            else if (parent == groupHeader)
                return Math.max(1, groups.size());
            else if (parent == indivHeader)
                return people == null ? 1 : Math.max(1, people.size());
            else
                return 0;
        }

        private List<UserGroup> loadGroups() {
            List<UserGroup> groups = new ArrayList<UserGroup>(UserGroupManager
                    .getInstance().getGroups().values());
            Collections.sort(groups);
            return groups;
        }

        private void setGroups(List<UserGroup> groups) {
            this.groups = groups;
            fireTreeStructureChanged(this, new Object[] { ROOT_OBJECT,
                    groupHeader }, null, null);
        }

        private List<UserGroupMember> loadPeople() {
            List<UserGroupMember> people = new ArrayList<UserGroupMember>(
                    UserGroupManager.getInstance().getAllKnownPeople());
            Collections.sort(people);
            return people;
        }

        private void setPeople(List<UserGroupMember> people) {
            peopleHaveBeenLoadedBefore = true;
            this.people = people;
            fireTreeStructureChanged(this, new Object[] { ROOT_OBJECT,
                    indivHeader }, null, null);
        }

    }


    /**
     * Class to load the list of people asynchronously
     */
    private class PeopleLoader extends
            SwingWorker<List<UserGroupMember>, Object> {
        private FilterChoices filterChoices;

        PeopleLoader(FilterChoices filterChoices) {
            this.filterChoices = filterChoices;
        }

        @Override
        protected List<UserGroupMember> doInBackground() throws Exception {
            return filterChoices.loadPeople();
        }

        @Override
        protected void done() {
            final Object currentSelection = selector.getSelectedLeaf();

            try {
                filterChoices.setPeople(get());
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (currentSelection != null)
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        selector.setSelectedNode(currentSelection);
                    }
                });
        }

    }


    /**
     * This renderer performs double-duty: it knows how to render the tree
     * cells, and it also knows how to render those within the table.
     */
    private class ChoiceRenderer extends DefaultTreeCellRenderer implements
            TableCellRenderer {

        private Icon everyoneIcon, groupIcon, personIcon, loadingIcon;

        private Border plain, line;

        private Font bold, regular, italic;

        private TableCellRenderer tableCellRenderer;

        private JLabel editLabel;

        private JPanel panel;

        public ChoiceRenderer(JFilterableTreeComponent selector) {
            everyoneIcon = DashboardIconFactory.getEveryoneIcon();
            groupIcon = DashboardIconFactory.getGroupIcon();
            personIcon = DashboardIconFactory.getIndividualIcon();
            loadingIcon = DashboardIconFactory.getHourglassIcon();
            plain = BorderFactory.createEmptyBorder(1, 0, 0, 0);
            line = BorderFactory.createMatteBorder(1, 0, 0, 0, Color.lightGray);

            JTreeTable treeTable = selector.getTreeTable();
            treeTable.setRowHeight(personIcon.getIconHeight() + 3
                    + treeTable.getRowMargin());
            tableCellRenderer = treeTable.getCellRenderer(0, 0);
            treeTable.getColumnModel().getColumn(0).setCellRenderer(this);
            treeTable.getTree().setCellRenderer(this);

            regular = treeTable.getFont();
            bold = regular.deriveFont(Font.BOLD);
            italic = regular.deriveFont(Font.ITALIC);

            editLabel = new JLabel("<html><a href='#'>"
                    + resources.getHTML("Create_Edit_Link")
                    + "</a>&nbsp;&nbsp;&nbsp;</html>");
            editLabel.setFont(italic);

            panel = new JPanel(new BorderLayout());
            panel.add(Box.createHorizontalGlue(), BorderLayout.CENTER);
            panel.add(editLabel, BorderLayout.EAST);
            panel.setOpaque(true);

            MouseHandler h = new MouseHandler(treeTable,
                    editLabel.getPreferredSize().width);
            treeTable.addMouseMotionListener(h);
            treeTable.addMouseListener(h);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row,
                boolean hasFocus) {
            // call the default logic to initialize appearance
            super.getTreeCellRendererComponent(tree, value, sel, expanded,
                leaf, row, hasFocus);

            // configure special icons for individuals and groups
            if (value instanceof UserGroup)
                setIcon(UserGroup.isEveryone((UserGroup) value) //
                        ? everyoneIcon : groupIcon);
            else if (value instanceof UserGroupMember)
                setIcon(personIcon);
            else if (value == loadingLabel)
                setIcon(loadingIcon);
            else if (value == noneFoundLabel)
                setIcon(null);

            // draw a thin line above group headings
            setBorder(leaf ? plain : line);

            // highlight the selected leaf with a bold font
            if (value == noneFoundLabel)
                setFont(italic);
            else
                setFont((leaf && sel) ? bold : regular);

            return this;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            // call parent logic to get the renderer for the cell. Then place
            // that renderer on the left edge of our panel
            panel.add(tableCellRenderer.getTableCellRendererComponent(table,
                value, isSelected, hasFocus, row, column), BorderLayout.WEST);

            // set the background color of the panel to indicate selection
            if (isSelected)
                panel.setBackground(table.getSelectionBackground());
            else
                panel.setBackground(table.getBackground());

            // display a thin line above group headings
            boolean isFolder = value == groupHeader || value == indivHeader;
            panel.setBorder(isFolder ? line : plain);

            // display the "create/edit" label on the group header
            editLabel.setVisible(value == groupHeader);

            return panel;
        }

    }


    /**
     * Display a hyperlink-style cursor over the "create/edit" label, and
     * respond to clicks on it.
     */
    private class MouseHandler extends MouseMotionAdapter implements
            MouseListener {

        private JTable table;

        private int linkWidth;

        private boolean armed;

        MouseHandler(JTable table, int linkWidth) {
            this.table = table;
            this.linkWidth = linkWidth;
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            armed = isOverEditLink(e);
            if (armed)
                table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            else
                table.setCursor(null);
        }

        private boolean isOverEditLink(MouseEvent e) {
            int row = table.rowAtPoint(e.getPoint());
            if (row == -1)
                return false;

            if (groupHeader != table.getValueAt(row, 0))
                return false;

            Rectangle r = table.getCellRect(row, 0, false);
            return (e.getX() > r.width - linkWidth);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (armed) {
                // display a "wait" icon, in case it takes a while for the
                // UserGroupEditor window to appear.
                table.setCursor(null);
                selector.setCursor(Cursor
                        .getPredefinedCursor(Cursor.WAIT_CURSOR));

                // open the user group editor.
                new UserGroupEditor(selector);

                // reload the newly edited groups.
                selector.setCursor(null);
                filterChoices.setGroups(filterChoices.loadGroups());
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {}

        @Override
        public void mouseReleased(MouseEvent e) {}

        @Override
        public void mouseEntered(MouseEvent e) {}

        @Override
        public void mouseExited(MouseEvent e) {}
    }

    private static boolean peopleHaveBeenLoadedBefore = false;

    private static final String SIZE_PREF = "userPref.userGroupSelector.dimensions";

}
