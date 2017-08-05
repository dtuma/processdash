// Copyright (C) 2012-2017 Tuma Solutions, LLC
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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.BoxUtils;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;

import teamdash.wbs.WBSTabPanel.FindResult;
import teamdash.wbs.columns.NotesColumn;
import teamdash.wbs.columns.WBSNodeColumn;
import teamdash.wbs.icons.WrappedSearchIcon;

public class WBSFindAction extends AbstractAction {

    private WBSTabPanel tabPanel;

    protected Action replaceAction;

    private static final Resources resources = Resources
            .getDashBundle("WBSEditor.Find");

    public WBSFindAction(WBSTabPanel tabPanel) {
        super(resources.getString("Menu"), IconFactory.getFindIcon());
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, //
            MacGUIUtils.getCtrlModifier()));
        this.tabPanel = tabPanel;
        this.replaceAction = new WBSReplaceAction();
    }


    private JDialog dialog;

    private JTabbedPane tabs;

    private JComboBoxWithHistory searchField;

    private JLabel replaceLabel;

    private JComboBoxWithHistory replaceField;

    private enum Scope {
        All, Current, Selection, Name;
        public String toString() {
            return resources.getString("Within." + name());
        }
    };

    private JComboBox searchScope;

    private enum Match {
        Within, Entire;
        public String toString() {
            return resources.getString("Match." + name());
        }
    }

    private JComboBox matchType;

    private enum Direction {
        Rows, Columns;
        public String toString() {
            return resources.getString("Search_By." + name());
        }
    };

    private JComboBox rowsOrColumns;

    private WrappedIcon wrappedIcon;

    private JButton replaceButton, prevButton;

    private FindAction findNext;

    private List<String> selectedColumns, foundColumn;


    public void actionPerformed(ActionEvent e) {
        showDialog(0);
    }

    private void showDialog(int tabToSelect) {
        if (dialog == null)
            createDialog();

        tabs.setSelectedIndex(tabToSelect);
        dialog.setVisible(true);
        dialog.toFront();
        searchField.getEditor().selectAll();
        searchField.requestFocusInWindow();
        wrappedIcon.setVisible(false);
    }

    private void createDialog() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(5, 8, 10, 8));
        GridBagLayout layout = new GridBagLayout();
        panel.setLayout(layout);

        FindPreviousAction findPrevious = new FindPreviousAction();
        findNext = new FindNextAction();
        ReplaceAction replace = new ReplaceAction();
        CancelAction cancel = new CancelAction();

        setKey(panel, KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK, findPrevious);
        setKey(panel, KeyEvent.VK_N, InputEvent.ALT_DOWN_MASK, findNext);
        setKey(panel, KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK, replace);
        setKey(panel, KeyEvent.VK_ESCAPE, 0, cancel);

        GridBagConstraints lc = new GridBagConstraints();
        lc.gridx = lc.gridy = 0; lc.gridwidth = 2;
        lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(33, 12, 5, 5);
        panel.add(new JLabel(resources.getString("Label")), lc);

        GridBagConstraints tc = new GridBagConstraints();
        tc.gridx = 2; tc.gridy = 0;
        tc.fill = GridBagConstraints.BOTH;
        tc.weightx = 1; tc.gridwidth = 2;
        tc.insets = new Insets(33, 0, 5, 12);
        searchField = new JComboBoxWithHistory();
        searchField.getEditor().addActionListener(new FindNextTrigger());
        panel.add(searchField, tc);

        lc.gridy++; tc.gridy++;
        lc.insets.top = tc.insets.top = 0;
        replaceLabel = new JLabel(resources.getString("Replace_Label"));
        panel.add(replaceLabel, lc);
        replaceField = new JComboBoxWithHistory();
        panel.add(replaceField, tc);

        panel.add(BoxUtils.createHorizontalStrut(
            replaceLabel.getPreferredSize().width), lc);
        panel.add(BoxUtils.createVerticalStrut(
            replaceField.getPreferredSize().height), tc);
        replaceLabel.setVisible(false);
        replaceField.setVisible(false);

        lc.gridy++;
        panel.add(BoxUtils.createVerticalStrut(10), lc);

        lc.gridy++; lc.gridwidth = 1;
        panel.add(new JLabel(resources.getString("Within.Label")), lc);

        GridBagConstraints pc = new GridBagConstraints();
        pc.gridx = 1; pc.gridy = lc.gridy; pc.gridwidth = 2;
        pc.insets = tc.insets;
        pc.fill = GridBagConstraints.BOTH;
        searchScope = new JComboBox(Scope.values());
        searchScope.setSelectedItem(Scope.All);
        searchScope.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                selectedColumns = null;
            }});
        panel.add(searchScope, pc);

        lc.gridy++; pc.gridy++;
        panel.add(new JLabel(resources.getString("Match.Label")), lc);
        matchType = new JComboBox(Match.values());
        matchType.setSelectedItem(Match.Within);
        panel.add(matchType, pc);

        lc.gridy++; pc.gridy++;
        lc.insets.bottom = pc.insets.bottom = 12;
        panel.add(new JLabel(resources.getString("Search_By.Label")), lc);
        rowsOrColumns = new JComboBox(Direction.values());
        rowsOrColumns.setSelectedItem(Direction.Rows);
        panel.add(rowsOrColumns, pc);

        GridBagConstraints wc = new GridBagConstraints();
        wc.gridx = 3; wc.gridy = lc.gridy;
        wc.anchor = GridBagConstraints.WEST;
        wc.insets = lc.insets; wc.insets.left = 5;
        wrappedIcon = new WrappedIcon();
        panel.add(wrappedIcon, wc);

        GridBagConstraints bc = new GridBagConstraints();
        bc.gridy = lc.gridy + 1;
        bc.anchor = GridBagConstraints.EAST;
        bc.insets = new Insets(10, 5, 0, 0);
        bc.gridwidth = 4;
        Box buttonBox = Box.createHorizontalBox();
        replaceButton = new JButton(replace);
        replaceButton.setVisible(false);
        buttonBox.add(replaceButton);
        buttonBox.add(Box.createHorizontalStrut(30));
        buttonBox.add(prevButton = new JButton(findPrevious));
        buttonBox.add(Box.createHorizontalStrut(5));
        buttonBox.add(new JButton(findNext));
        buttonBox.add(Box.createHorizontalStrut(20));
        buttonBox.add(new JButton(cancel));
        panel.add(buttonBox, bc);

        GridBagConstraints tpc = new GridBagConstraints();
        tpc.gridx = tpc.gridy = 0;
        tpc.gridwidth = 4; tpc.gridheight = bc.gridy;
        tpc.fill = GridBagConstraints.BOTH;
        tabs = new JTabbedPane();
        tabs.add(resources.getString("Find"), new JPanel());
        tabs.add(resources.getString("Replace"), new JPanel());
        tabs.addChangeListener(new TabHandler());
        panel.add(tabs, tpc);

        JFrame f = (JFrame) SwingUtilities.getWindowAncestor(tabPanel);
        dialog = new JDialog(f, resources.getString("Title"), false);
        dialog.getContentPane().add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(f);
    }

    private void setKey(JPanel panel, int keystroke, int mod, Action action) {
        String actionName = action.getClass().getSimpleName();
        panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(keystroke, mod), actionName);
        panel.getActionMap().put(actionName, action);

    }

    private StringTest makeFilter() {
        String searchText = (String) searchField.getEditor().getItem();
        searchText = searchText.trim();
        if (searchText.length() == 0) {
            Toolkit.getDefaultToolkit().beep();
            return null;
        } else {
            searchField.updateHistory(searchText);
            final String searchLower = searchText.toLowerCase();
            final Match match = (Match) matchType.getSelectedItem();
            return new StringTest() {
                public boolean test(String s) {
                    if (match == Match.Entire)
                        return s.toLowerCase().equals(searchLower);
                    else
                        return s.toLowerCase().contains(searchLower);
                }
            };
        }
    }

    private List<String> getColumnSearchScope() {
        switch ((Scope) searchScope.getSelectedItem()) {
        case All: default:
            return null;

        case Current:
            return tabPanel.getActiveTabColumnIDs();

        case Name:
            return Arrays.asList(WBSNodeColumn.COLUMN_ID,
                NotesColumn.COLUMN_ID);

        case Selection:
            List<String> currentColumnSelection = tabPanel
                    .getSelectedColumnIDs();
            if (selectedColumns == null
                    || !currentColumnSelection.equals(foundColumn)) {
                selectedColumns = currentColumnSelection;
            }
            if (selectedColumns.isEmpty())
                return tabPanel.getActiveTabColumnIDs();
            else
                return selectedColumns;
        }
    }

    private int replaceStartPos = 0;

    private String replaceContinuationToken;

    private void replaceText() {
        // perform a replacement. If successful, return
        String replaceErrorMessage = doReplace();
        if (replaceErrorMessage == null)
            return;

        // the replacement failed. Beep or display an error as appropriate
        replaceStartPos = 0;
        if (replaceErrorMessage.length() > 0) {
            JOptionPane.showMessageDialog(dialog, replaceErrorMessage,
                resources.getString("Cannot_Replace.Title"),
                JOptionPane.ERROR_MESSAGE);
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private String doReplace() {
        // identify the node where the replacement should occur
        int[] selRows = tabPanel.wbsTable.getSelectedRows();
        if (selRows == null || selRows.length != 1)
            return CANNOT_REPLACE;
        WBSNode node = tabPanel.wbsTable.wbsModel.getNodeForRow(selRows[0]);
        if (node == null)
            return CANNOT_REPLACE;

        // identify the column where the replacement should occur
        List<String> selCols = tabPanel.getSelectedColumnIDs();
        if (selCols.size() != 1)
            return CANNOT_REPLACE;
        DataTableModel data = (DataTableModel) tabPanel.dataTable.getModel();
        int colIndex = data.findColumn(selCols.get(0));
        if (colIndex == -1)
            return CANNOT_REPLACE;

        // see if the given cell is editable or read-only
        if (data.isCellEditable(node, colIndex) == false)
            return resources.getString("Cannot_Replace.Read_Only");

        // identify the search string we should be looking for
        String searchText = (String) searchField.getEditor().getItem();
        String searchLower = searchText.trim().toLowerCase();
        if (searchLower.length() == 0)
            return CANNOT_REPLACE;

        // get the value in the specified cell
        Object oldValue = data.getValueAt(node, colIndex);
        String oldStr = (oldValue == null ? "" : oldValue.toString());
        String oldStrL = oldStr.toLowerCase();

        // see if we are performing a second replacement on a particular cell
        String cellToken = node.getUniqueID() + "/" + selCols.get(0);
        if (!cellToken.equals(replaceContinuationToken))
            replaceStartPos = 0;

        // find the search text within the specified value
        int pos = oldStrL.indexOf(searchLower, replaceStartPos);
        if (pos == -1)
            return resources.getString("Cannot_Replace.Not_Found");

        // perform the replacement
        String replacementText = (String) replaceField.getEditor().getItem();
        replaceField.updateHistory(replacementText);
        try {
            Object newValue;
            DataColumn col = data.getColumn(colIndex);
            if (col instanceof ReplaceAwareColumn) {
                newValue = ((ReplaceAwareColumn) col).getReplacementValueAt(
                    searchText, replacementText, node);
            } else {
                newValue = oldStr.substring(0, pos) //
                        + replacementText //
                        + oldStr.substring(pos + searchText.length());
            }
            if (newValue != ReplaceAwareColumn.REPLACEMENT_REJECTED)
                data.setValueAt(newValue, node, colIndex);
        } catch (Exception e) {
            return resources.format("Cannot_Replace.Unsupported_FMT",
                data.getColumnName(colIndex));
        }

        // see if the replacement "took," or if it was rejected by the column
        Object finalValue = data.getValueAt(node, colIndex);
        String finalStr = (finalValue == null ? "" : finalValue.toString());
        if (finalStr.equals(oldStr)
                && !searchText.equalsIgnoreCase(replacementText))
            return resources.getString("Cannot_Replace.Rejected");

        // the replacement was successful. Record an undo list entry
        UndoList.madeChange(tabPanel, "Replace Text");

        // See if the current cell includes another instance of the search text
        int nextPos = pos + replacementText.length();
        nextPos = finalStr.toLowerCase().indexOf(searchLower, nextPos);
        if (nextPos != -1) {
            replaceStartPos = nextPos;
            replaceContinuationToken = cellToken;
            return null;
        }

        // auto-search for the next match
        replaceStartPos = 0;
        findNext.performFind(true);
        return null;
    }

    private static final String CANNOT_REPLACE = "";


    private static class JComboBoxWithHistory extends JComboBox {

        private JComboBoxWithHistory() {
            super(new HistoryModel(new Vector()));
            setEditable(true);
        }

        public void updateHistory(String item) {
            setSelectedItem(item);
            HistoryModel model = (HistoryModel) getModel();
            model.addHistoryItem(item);
        }

    }

    private static class HistoryModel extends DefaultComboBoxModel {

        Vector<String> objects;
        int maxSize;

        private HistoryModel(Vector<String> objects) {
            super(objects);
            this.objects = objects;
            this.maxSize = 10;
        }

        public void addHistoryItem(String item) {
            if (item != null) {
                if (getSize() == 0 || !item.equals(getElementAt(0)))
                    insertElementAt(item, 0);
                for (int i = getSize(); i-- > 1;) {
                    if (item.equals(getElementAt(i))) {
                        objects.removeElementAt(i);
                        fireIntervalRemoved(this, i, i);
                    }
                }
                while (getSize() > maxSize) {
                    objects.removeElementAt(maxSize);
                    fireIntervalRemoved(this, maxSize, maxSize);
                }
            }
        }
    }


    private class TabHandler implements ChangeListener {

        public void stateChanged(ChangeEvent e) {
            boolean isFind = (tabs.getSelectedIndex() == 0);
            boolean isReplace = !isFind;
            prevButton.setVisible(isFind);
            replaceLabel.setVisible(isReplace);
            replaceField.setVisible(isReplace);
            replaceButton.setVisible(isReplace);
        }

    }

    private class FindAction extends AbstractAction {
        
        private boolean searchForward;

        public FindAction(String resKey, boolean forward) {
            super(resources.getString(resKey));
            this.searchForward = forward;
        }

        public void actionPerformed(ActionEvent e) {
            performFind(false);
        }

        public void performFind(boolean suppressNotFoundError) {
            StringTest filter = makeFilter();
            if (filter == null)
                return;
            searchField.getEditor().selectAll();
            replaceContinuationToken = null;

            boolean searchByColumns = //
                    (rowsOrColumns.getSelectedItem() == Direction.Columns);
            List<String> columnScope = getColumnSearchScope();

            FindResult foundMatch = tabPanel.findNextMatch(filter,
                searchForward, searchByColumns, columnScope);
            foundColumn = tabPanel.getSelectedColumnIDs();

            if (foundMatch == FindResult.NotFound && !suppressNotFoundError)
                JOptionPane.showMessageDialog(dialog,
                    resources.getString("Not_Found.Message"),
                    resources.getString("Not_Found.Title"),
                    JOptionPane.PLAIN_MESSAGE);
            else
                wrappedIcon.setVisible(foundMatch == FindResult.Wrapped);
        }

    }

    private class FindPreviousAction extends FindAction {
        public FindPreviousAction() {
            super("Previous", false);
            putValue(MNEMONIC_KEY, new Integer('P'));
        }
    }

    private class FindNextAction extends FindAction {
        public FindNextAction() {
            super("Next", true);
            putValue(MNEMONIC_KEY, new Integer('N'));
            putValue(DISPLAYED_MNEMONIC_INDEX_KEY,
                ((String) getValue(NAME)).indexOf('N'));
        }
    }

    private class FindNextTrigger implements ActionListener {
        private Timer timer;
        FindNextTrigger() {
            timer = new Timer(20, findNext);
            timer.setRepeats(false);
        }
        public void actionPerformed(ActionEvent e) {
            timer.restart();
        }
    }

    private class ReplaceAction extends AbstractAction {
        ReplaceAction() {
            super(resources.getString("Replace"));
            putValue(MNEMONIC_KEY, new Integer('R'));
        }
        public void actionPerformed(ActionEvent e) {
            if (replaceButton.isVisible())
                replaceText();
        }
    }

    private class CancelAction extends AbstractAction {
        public CancelAction() {
            super(resources.getString("Cancel"));
        }

        public void actionPerformed(ActionEvent e) {
            dialog.dispose();
        }
    }

    private class WBSReplaceAction extends AbstractAction {
        public WBSReplaceAction() {
            super(resources.getString("Replace_Menu"));
        }
        public void actionPerformed(ActionEvent e) {
            showDialog(1);
        }
    }

    private class WrappedIcon extends JLabel implements ActionListener {
        private Timer timer;
        private WrappedSearchIcon icon;
        private int alpha;
        public WrappedIcon() {
            timer = new Timer(100, this);
            timer.setInitialDelay(5000);
            setIcon(icon = new WrappedSearchIcon());
            setToolTipText(resources.getString("Search_Wrapped"));
            super.setVisible(false);
        }

        @Override
        public void setVisible(boolean visible) {
            super.setVisible(visible);
            if (visible) {
                icon.setColor(Color.black);
                alpha = 255;
                timer.restart();
            } else {
                timer.stop();
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            alpha -= 25;
            if (alpha < 0) {
                timer.stop();
                super.setVisible(false);
            } else {
                Color c = new Color(0, 0, 0, alpha);
                icon.setColor(c);
                repaint();
            }
        }
    }

}
