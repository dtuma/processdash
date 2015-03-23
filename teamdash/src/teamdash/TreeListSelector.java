// Copyright (C) 2002-2010 Tuma Solutions, LLC
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

package teamdash;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.EventHandler;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class TreeListSelector extends JPanel implements TreeSelectionListener, ListSelectionListener {
    private static final String ADD_BUTTON_TOOLTIP = "Add to list";
    private static final String ADD_BUTTON_LABEL = ">";
    private static final String ADD_ALL_BUTTON_TOOLTIP = "Add all to list";
    private static final String ADD_ALL_BUTTON_LABEL = ">>";
    private static final String REMOVE_BUTTON_TOOLTIP = "Remove from list";
    private static final String REMOVE_BUTTON_LABEL = "<";
    private static final String REMOVE_ALL_BUTTON_TOOLTIP = "Remove all from list";
    private static final String REMOVE_ALL_BUTTON_LABEL = "<<";

    private JTree treeComponent;
    private JList listComponent;
    private DefaultTreeModel treeModel;
    private DefaultListModel listModel;
    private Translator translator;
    private static final EmptyBorder labelBorder = new EmptyBorder(0, 0, 4, 0);

    public TreeListSelector(TreeNode availableElements, String treeTitle, String listTitle) {
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        // build tree
        JPanel treePanel = new JPanel();
        treePanel.setLayout(new BorderLayout());
        JLabel treeLabel = new JLabel(treeTitle);
        treeLabel.setBorder(labelBorder);
        treePanel.add(treeLabel, BorderLayout.NORTH);
        treeModel = new DefaultTreeModel(availableElements);
        treeComponent = new JTree(treeModel);
        treeComponent.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        treeComponent.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                // add selected leaf objects on double click
                if (e.getClickCount() == 2) {
                    TreePath selPath = treeComponent.getSelectionPath();
                    if (null != selPath
                            && null != selPath.getLastPathComponent()
                            && treeModel.isLeaf(selPath.getLastPathComponent()))
                         addAction();
                }

            }
        });
        treeComponent.getSelectionModel().addTreeSelectionListener(this);
        JScrollPane treeScrollPane = new JScrollPane(treeComponent);
        treePanel.add(treeScrollPane, BorderLayout.CENTER);
        this.add(treePanel);

        // build buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(new EmptyBorder(90, 5, 0, 5));
        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(4, 1, 0, 4));
        JButton addButton = new JButton(ADD_BUTTON_LABEL);
        addButton.setToolTipText(ADD_BUTTON_TOOLTIP);
        addButton.addActionListener((ActionListener) EventHandler.create(ActionListener.class, this, "addAction"));
        buttons.add(addButton);
        JButton removeButton = new JButton(REMOVE_BUTTON_LABEL);
        removeButton.setToolTipText(REMOVE_BUTTON_TOOLTIP);
        removeButton.addActionListener((ActionListener) EventHandler.create(ActionListener.class, this, "removeAction"));
        buttons.add(removeButton);
        JButton addAllButton = new JButton(ADD_ALL_BUTTON_LABEL);
        addAllButton.setToolTipText(ADD_ALL_BUTTON_TOOLTIP);
        addAllButton.addActionListener((ActionListener) EventHandler.create(ActionListener.class, this, "addAllAction"));
        buttons.add(addAllButton);
        JButton removeAllButton = new JButton(REMOVE_ALL_BUTTON_LABEL);
        removeAllButton.setToolTipText(REMOVE_ALL_BUTTON_TOOLTIP);
        removeAllButton.addActionListener((ActionListener) EventHandler.create(ActionListener.class, this, "removeAllAction"));
        buttons.add(removeAllButton);
        buttonPanel.add(buttons);
        this.add(buttonPanel);

        // build list
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BorderLayout());
        JLabel listLabel = new JLabel(listTitle);
        listLabel.setBorder(labelBorder);
        listPanel.add(listLabel, BorderLayout.NORTH);
        listModel = new DefaultListModel();
        listComponent = new JList(listModel);
        listComponent.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        listComponent.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    removeAction();
            }
        });
        listComponent.getSelectionModel().addListSelectionListener(this);
        JScrollPane listScrollPane = new JScrollPane(listComponent);
        listPanel.add(listScrollPane, BorderLayout.CENTER);
        this.add(listPanel);
    }

    public void setSelectedList(List selectedList) {
        // reset tree expansion to only show first level
        TreeNode root = (TreeNode) treeModel.getRoot();
        TreePath rootPath = new TreePath(root);
        for (Enumeration e = root.children(); e.hasMoreElements();)
            expandAll(rootPath.pathByAddingChild(e.nextElement()), false);
        treeComponent.getSelectionModel().clearSelection();

        listModel.clear();
        for (Iterator iter = selectedList.iterator(); iter.hasNext();) {
            listModel.addElement(iter.next());
        }
    }

    public List getSelectedList() {
        return new LinkedList(Arrays.asList(listModel.toArray()));
    }

    public void addAction() {
        TreePath[] selPaths = treeComponent.getSelectionPaths();
        Set elements = new LinkedHashSet();
        if (null != selPaths)
            for (int i = 0; i < selPaths.length; i++) {
                if (null != selPaths[i].getLastPathComponent())
                    getLeafElements(selPaths[i].getLastPathComponent(), elements);
            }

        addSelectedElements(elements);
    }

    public void removeAction() {
        int[] selIndices = listComponent.getSelectedIndices();
        for (int i = selIndices.length - 1; i >= 0; i--)
            listModel.remove(selIndices[i]);
    }

    public void addAllAction() {
        Set elements = new LinkedHashSet();
        getLeafElements(treeModel.getRoot(), elements);
        addSelectedElements(elements);
    }

    public void removeAllAction() {
        listModel.removeAllElements();
    }

    private void getLeafElements(Object node, Set elements) {
        if (treeModel.isLeaf(node)) {
            elements.add(node);
        } else {
            for (int i = 0; i < treeModel.getChildCount(node); i++) {
                getLeafElements(treeModel.getChild(node, i), elements);
            }
        }
    }

    private void addSelectedElements(Set elements) {
        for (Iterator iter = elements.iterator(); iter.hasNext();) {
            Object element = iter.next();
            if (null != translator)
                element = translator.translate(element);
            listModel.addElement(element);
        }
    }

    public void setTreeCellRenderer(TreeCellRenderer treeCellRenderer) {
        treeComponent.setCellRenderer(treeCellRenderer);
    }

    public void setListCellRenderer(ListCellRenderer listCellRenderer) {
        listComponent.setCellRenderer(listCellRenderer);
    }

    public void setTranslator(Translator translator) {
        this.translator = translator;
    }

    public void valueChanged(TreeSelectionEvent e) {
        if (!treeComponent.isSelectionEmpty())
            listComponent.clearSelection();
    }

    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting() && !listComponent.isSelectionEmpty())
            treeComponent.clearSelection();
    }

    private void expandAll(TreePath parentPath, boolean expand) {
        TreeNode node = (TreeNode) parentPath.getLastPathComponent();
        for (Enumeration e = node.children(); e.hasMoreElements();) {
            TreeNode child = (TreeNode) e.nextElement();
            TreePath childPath = parentPath.pathByAddingChild(child);
            expandAll(childPath, expand);
        }

        if (expand)
            treeComponent.expandPath(parentPath);
        else
            treeComponent.collapsePath(parentPath);
    }

    public interface Translator {
        public Object translate(Object o);
    }
}
