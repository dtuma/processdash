// Copyright (C) 2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.EventHandler;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreePath;


public class JFilterableTreeComponent extends JPanel {
    public static final String NODE_ACCEPTED = "nodeAccepted";

    JTextField filterTextField;

    JFilterableTreeTable treeTable;

    boolean matchEntirePath;

    TreePath anchorPath;

    protected EventListenerList listenerList = new EventListenerList();

    private final SelectNodeAction selectNodeAction = new SelectNodeAction();

    private final SelectPreviousNodeAction previousNodeAction = new SelectPreviousNodeAction();

    private final SelectNextNodeAction nextNodeAction = new SelectNextNodeAction();

    public JFilterableTreeComponent(TreeTableModel treeTableModel) {
        this(treeTableModel, "Find: ", true);
    }

    public JFilterableTreeComponent(TreeTableModel treeTableModel,
            String prompt, boolean rootVisible) {
        super(new BorderLayout());

        // Create the components.
        filterTextField = new JTextField(20);
        JPanel filterTextPanel = new JPanel();
        filterTextPanel.setLayout(new BoxLayout(filterTextPanel,
                BoxLayout.X_AXIS));
        filterTextPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        if (prompt != null)
            filterTextPanel.add(new JLabel(prompt));
        filterTextPanel.add(filterTextField);
        filterTextField.getDocument().addDocumentListener(EventHandler.create(
            DocumentListener.class, this, "refilter"));
        filterTextField.getInputMap().put(KeyStroke.getKeyStroke("ENTER"),
            "selectNode");
        filterTextField.getInputMap().put(KeyStroke.getKeyStroke("UP"),
            "previousNode");
        filterTextField.getInputMap().put(KeyStroke.getKeyStroke("DOWN"),
            "nextNode");

        filterTextField.getActionMap().put("selectNode", selectNodeAction);
        filterTextField.getActionMap().put("previousNode", previousNodeAction);
        filterTextField.getActionMap().put("nextNode", nextNodeAction);

        treeTable = new JFilterableTreeTable(treeTableModel);
        treeTable.setRootVisible(rootVisible);
        treeTable.getSelectionModel().setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION);
        treeTable.getTree().addTreeExpansionListener(new ExpansionHandler());
        treeTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    handleClick();
                }
            }
        });
        treeTable.getInputMap().put(KeyStroke.getKeyStroke("ENTER"),
            "selectNode");
        treeTable.getInputMap().put(KeyStroke.getKeyStroke("UP"),
        "previousNode");
        treeTable.getInputMap().put(KeyStroke.getKeyStroke("DOWN"),
        "nextNode");

        treeTable.getActionMap().put("selectNode", selectNodeAction);
        treeTable.getActionMap().put("previousNode", previousNodeAction);
        treeTable.getActionMap().put("nextNode", nextNodeAction);

        selectFirstLeaf();

        JScrollPane sp = new JScrollPane(treeTable);
        sp.getViewport().setBackground(Color.white);

        // Lay them out.
        add(filterTextPanel, BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);
    }

    public void selectFirstLeaf() {
        selectFirstLeafAfter(-1);
    }

    public void setAnchorSelectedNode(Object nodeToSelect) {
        anchorPath = setSelectedNode(nodeToSelect);
    }

    public TreePath setSelectedNode(Object nodeToSelect) {
        for (int i = 0;  i < treeTable.getRowCount();  i++) {
            TreePath path = treeTable.getPathForRow(i);
            Object node = path.getLastPathComponent();
            if (node.equals(nodeToSelect)) {
                selectAndShowRow(i);
                return path;
            }
        }
        return null;
    }

    public void selectLeafNearestAnchor() {
        int bestRow = -1;

        if (anchorPath != null) {
            int bestMatchLen = -1;
            for (int i = 0; i < treeTable.getRowCount(); i++) {
                TreePath path = treeTable.getPathForRow(i);
                Object node = path.getLastPathComponent();
                if (treeTable.getTree().getModel().isLeaf(node)) {
                    int matchLen = getMatchLen(path, anchorPath);
                    if (matchLen > bestMatchLen) {
                        bestMatchLen = matchLen;
                        bestRow = i;
                    }
                }
            }
        }
        if (bestRow == -1)
            selectFirstLeaf();
        else
            selectAndShowRow(bestRow);
    }

    private int getMatchLen(TreePath a, TreePath b) {
        int pos = 0;
        while (pos < a.getPathCount() && pos < b.getPathCount()) {
            Object aPart = a.getPathComponent(pos);
            Object bPart = b.getPathComponent(pos);
            if (!aPart.equals(bPart))
                return pos;
            else
                pos++;
        }
        return pos;
    }

    public boolean isMatchEntirePath() {
        return matchEntirePath;
    }

    public void setMatchEntirePath(boolean matchEntirePath) {
        if (this.matchEntirePath != matchEntirePath) {
            this.matchEntirePath = matchEntirePath;
            refilter();
        }
    }

    public void refilter() {
        filter(filterTextField.getText());
    }

    private void filter(String text) {
        try {
            if (text == null || text.trim().length() == 0)
                treeTable.setFilter(null);
            else
                treeTable.setFilter(new NodeNamePatternMatcher(text,
                    matchEntirePath));
            selectLeafNearestAnchor();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    private class NodeNamePatternMatcher implements JFilterableTreeTable.Filter {

        private String[] patterns;

        boolean matchEntirePath;

        public NodeNamePatternMatcher(String text, boolean matchEntirePath) {
            this.patterns = text.toLowerCase().split("\\s+");
            this.matchEntirePath = matchEntirePath;
        }

        public boolean test(TreePath treePath) {
            if (treePath == null)
                return false;

            String str = getMatchStringForNode(treePath);
            for (int i = 0; i < patterns.length; i++) {
                if (str.indexOf(patterns[i]) == -1)
                    return false;
            }
            return true;
        }

        private String getMatchStringForNode(TreePath treePath) {
            if (matchEntirePath == false) {
                return asString(treePath.getLastPathComponent()).toLowerCase();
            } else {
                StringBuilder sb = new StringBuilder(256);
                Object[] path = treePath.getPath();
                int i = (treeTable.isRootVisible() ? 0 : 1);
                while (i < path.length) {
                    sb.append('/').append(asString(path[i++]).toLowerCase());
                }
                return sb.toString();
            }
        }

    }

    private String asString(Object obj) {
        return (obj == null ? "" : obj.toString());
    }

    private void handleClick() {
        if (isAcceptable()) {
            fireNodeAccepted();
        }
    }

    private void selectFirstLeafBefore(int startRow) {
        if (startRow < 0)
            startRow = 0;

        for (int i = startRow - 1; i >= 0; i--) {
            TreePath path = treeTable.getPathForRow(i);
            Object node = path.getLastPathComponent();
            if (treeTable.getTree().getModel().isLeaf(node)) {
                selectAndShowRow(i);
                return;
            }
        }

        // scroll to top
        treeTable.scrollRectToVisible(treeTable.getCellRect(0, 0, true));
    }

    private void selectFirstLeafAfter(int startRow) {
        if (startRow < 0)
            startRow = -1;

        for (int i = startRow + 1; i < treeTable.getRowCount(); i++) {
            TreePath path = treeTable.getPathForRow(i);
            Object node = path.getLastPathComponent();
            if (treeTable.getTree().getModel().isLeaf(node)) {
                selectAndShowRow(i);
                return;
            }
        }
    }

    private void selectAndShowRow(int row) {
        treeTable.getSelectionModel().setSelectionInterval(row, row);
        treeTable.scrollRectToVisible(treeTable.getCellRect(row, 0, true));
    }

    private boolean isAcceptable() {
        Object leaf = getSelectedLeaf();
        return leaf != null;
    }

    public Object getSelectedLeaf() {
        int selectedRow = treeTable.getSelectedRow();
        if (selectedRow < 0)
            return null;

        TreePath path = treeTable.getPathForRow(selectedRow);
        if (path == null)
            return null;

        Object node = path.getLastPathComponent();
        if (treeTable.getTree().getModel().isLeaf(node))
            return node;
        else
            return null;
    }

    public JTextField getFilterTextField() {
        return this.filterTextField;
    }

    public JFilterableTreeTable getTreeTable() {
        return this.treeTable;
    }

    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    protected void fireNodeAccepted() {
        ActionListener[] listeners = listenerList
                .getListeners(ActionListener.class);
        for (int i = 0; i < listeners.length; i++) {
            ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                    NODE_ACCEPTED);
            listeners[i].actionPerformed(e);
        }
    }

    private class SelectNodeAction extends AbstractAction {
        public SelectNodeAction() {
            super("selectNode");
        }

        public void actionPerformed(ActionEvent evt) {
            if (isAcceptable())
                fireNodeAccepted();
        }
    }

    private class SelectPreviousNodeAction extends AbstractAction {
        public SelectPreviousNodeAction() {
            super("previousNode");
        }

        public void actionPerformed(ActionEvent evt) {
            selectFirstLeafBefore(treeTable.getSelectedRow());
        }
    }

    private class SelectNextNodeAction extends AbstractAction {
        public SelectNextNodeAction() {
            super("nextNode");
        }

        public void actionPerformed(ActionEvent evt) {
            selectFirstLeafAfter(treeTable.getSelectedRow());
        }
    }

    private class ExpansionHandler implements TreeExpansionListener {

        private boolean performingExpandAll;

        public void treeExpanded(TreeExpansionEvent event) {
            if (performingExpandAll == false && event.getPath() != null) {
                try {
                    performingExpandAll = true;
                    treeTable.expandAllRowsUnder(event.getPath());
                } finally {
                    performingExpandAll = false;
                }
            }
        }

        public void treeCollapsed(TreeExpansionEvent event) {}
    }
}
