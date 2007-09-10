// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.lib;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.EventListenerList;
import javax.swing.tree.TreePath;


public class JFilterableTreeComponent extends JPanel {
    public static final String NODE_ACCEPTED = "nodeAccepted";

    JFilterableTreeTable treeTable;

    protected EventListenerList listenerList = new EventListenerList();

    private final SelectNodeAction selectNodeAction = new SelectNodeAction();

    private final SelectPreviousNodeAction previousNodeAction = new SelectPreviousNodeAction();

    private final SelectNextNodeAction nextNodeAction = new SelectNextNodeAction();

    public JFilterableTreeComponent(TreeTableModel treeTableModel) {
        super(new BorderLayout());

        // Create the components.
        final JLabel filterTextLabel = new JLabel("Find: ");
        final JTextField filterTextField = new JTextField(20);
        final JPanel filterTextPanel = new JPanel();
        filterTextPanel.setLayout(new BoxLayout(filterTextPanel,
                BoxLayout.X_AXIS));
        filterTextPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        filterTextPanel.add(filterTextLabel);
        filterTextPanel.add(filterTextField);
        filterTextField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                if (!e.isActionKey())
                    filter(filterTextField.getText());
            }
        });
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
        treeTable.getSelectionModel().setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION);
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

        JScrollPane sp = new JScrollPane(treeTable);
        sp.getViewport().setBackground(Color.white);

        // Lay them out.
        add(filterTextPanel, BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);
    }

    private void filter(String text) {
        try {
            if (text == null || text.trim().length() == 0)
                treeTable.setFilter(null);
            else
                treeTable.setFilter(new NodeNamePatternMatcher(text));
            selectFirstLeafAfter(0);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    private class NodeNamePatternMatcher implements JFilterableTreeTable.Filter {

        private String[] patterns;

        public NodeNamePatternMatcher(String text) {
            patterns = text.toLowerCase().split("\\s+");
        }

        public boolean test(Object treeNode) {
            if (treeNode == null)
                return false;

            String str = treeNode.toString().toLowerCase();
            for (int i = 0; i < patterns.length; i++) {
                if (str.indexOf(patterns[i]) == -1)
                    return false;
            }
            return true;
        }

    }

    private void handleClick() {
        if (isAcceptable()) {
            fireNodeAccepted();
        } else {
            int selectedRow = treeTable.getSelectedRow();
            selectFirstLeafAfter(selectedRow);
        }
    }

    private void selectFirstLeafBefore(int startRow) {
        if (startRow < 0)
            startRow = 0;

        for (int i = startRow - 1; i >= 0; i--) {
            TreePath path = treeTable.getPathForRow(i);
            Object node = path.getLastPathComponent();
            if (treeTable.getTree().getModel().isLeaf(node)) {
                treeTable.getSelectionModel().setSelectionInterval(i, i);
                treeTable.scrollRectToVisible(treeTable.getCellRect(i, 0, true));
                return;
            }
        }

        // scroll to top
        treeTable.scrollRectToVisible(treeTable.getCellRect(0, 0, true));
    }

    private void selectFirstLeafAfter(int startRow) {
        if (startRow < 0)
            startRow = 0;

        for (int i = startRow + 1; i < treeTable.getRowCount(); i++) {
            TreePath path = treeTable.getPathForRow(i);
            Object node = path.getLastPathComponent();
            if (treeTable.getTree().getModel().isLeaf(node)) {
                treeTable.getSelectionModel().setSelectionInterval(i, i);
                treeTable.scrollRectToVisible(treeTable.getCellRect(i, 0, true));
                return;
            }
        }
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
}
