// Copyright (C) 2002-2017 Tuma Solutions, LLC
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

package teamdash.wbs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;

import net.sourceforge.processdash.i18n.Resources;

import teamdash.TreeListSelector;

public class WBSColumnSelectorDialog extends JDialog {

    private TableColumnModel tableColumnModel;
    private TreeListSelector columnSelector;
    private JLabel dialogLabel;

    private static final Resources resources = Resources
            .getDashBundle("WBSEditor.CustomTabs.Change_Columns");

    public WBSColumnSelectorDialog(JFrame parent, String title, Map availableTabs) {
        super(parent, title, true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        //build content
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        dialogLabel = new JLabel();
        contentPanel.add(dialogLabel, BorderLayout.NORTH);
        columnSelector = new TreeListSelector(buildTreeModel(availableTabs),
                resources.getString("Available_Columns"),
                resources.getString("Selected_Columns"));
        columnSelector.setBorder(new EmptyBorder(10, 2, 2, 2));
        DefaultTreeCellRenderer treeCellRenderer = new DefaultTreeCellRenderer();
        treeCellRenderer.setOpenIcon(null);
        treeCellRenderer.setClosedIcon(null);
        treeCellRenderer.setLeafIcon(null);
        columnSelector.setTreeCellRenderer(treeCellRenderer);
        columnSelector.setTranslator(new TreeNodeColumnTranslator());
        contentPanel.add(columnSelector, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        FlowLayout buttonLayout = new FlowLayout(FlowLayout.RIGHT);
        buttonPanel.setLayout(buttonLayout);
        JButton setColumnButton = new JButton(resources.getString("Set_Columns"));
        setColumnButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                updateTabColumns(columnSelector.getSelectedList());
                dispose();
            }
        });
        buttonPanel.add(setColumnButton);
        JButton cancelButton = new JButton(resources.getString("Cancel"));
        cancelButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        buttonPanel.add(cancelButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        getContentPane().add(contentPanel);
        setSize(500, 400);
    }

    public void setDialogMessage(String tabName) {
        dialogLabel.setText(resources.format("Prompt_FMT", tabName));
    }

    public void setTableColumnModel(TableColumnModel tableColumnModel) {
        this.tableColumnModel = tableColumnModel;
        List tabColumnList = new ArrayList();
        for (Enumeration e = tableColumnModel.getColumns(); e.hasMoreElements();)
            tabColumnList.add(e.nextElement());
        columnSelector.setSelectedList(tabColumnList);
    }

    /**
     * Construct the TreeNode structure for the available columns
     * @param availableTabs
     * @return TreeNode
     */
    private TreeNode buildTreeModel(Map availableTabs) {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(
                resources.getString("Tabs_Root"));
        for (Iterator iter = availableTabs.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry)iter.next();
            DefaultMutableTreeNode tabNode = new DefaultMutableTreeNode(entry.getKey());
            TableColumnModel tableColumnModel = (TableColumnModel) entry.getValue();
            for (Enumeration e = tableColumnModel.getColumns(); e.hasMoreElements();) {
                DefaultMutableTreeNode columnNode = new DefaultMutableTreeNode(e.nextElement());
                tabNode.add(columnNode);
            }
            rootNode.add(tabNode);
        }

        return rootNode;
    }

    /**
     * Modify the tableColumnModel by removing or adding columns selected by the user.
     * @param selectedColumns
     */
    private void updateTabColumns(List selectedColumns) {
        // If the column in the old list doesn't exist in the new list it needs to be removed
        // The remaining columns are new columns to be added
        List removeColumns = new ArrayList();
        for (Enumeration e = tableColumnModel.getColumns(); e.hasMoreElements();) {
            Object currentColumn = e.nextElement();
            if (!selectedColumns.remove(currentColumn))
                removeColumns.add(currentColumn);
        }

        for (Iterator iter = removeColumns.iterator(); iter.hasNext(); )
            tableColumnModel.removeColumn((TableColumn) iter.next());

        for (Iterator iter = selectedColumns.iterator(); iter.hasNext(); )
            tableColumnModel.addColumn(new DataTableColumn((DataTableColumn) iter.next()));
    }

    private class TreeNodeColumnTranslator implements TreeListSelector.Translator {

        public Object translate(Object o) {
            if (o instanceof DefaultMutableTreeNode)
                return ((DefaultMutableTreeNode)o).getUserObject();

            return o;
        }

    }
}
