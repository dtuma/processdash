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
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.BoxUtils;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;

import teamdash.wbs.WBSTabPanel.FindResult;
import teamdash.wbs.columns.NotesColumn;
import teamdash.wbs.columns.WBSNodeColumn;
import teamdash.wbs.icons.WrappedSearchIcon;

public class WBSFindAction extends AbstractAction {

    private WBSTabPanel tabPanel;

    private static final Resources resources = Resources
            .getDashBundle("WBSEditor.Find");

    public WBSFindAction(WBSTabPanel tabPanel) {
        super(resources.getString("Menu"), IconFactory.getFindIcon());
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, //
            MacGUIUtils.getCtrlModifier()));
        this.tabPanel = tabPanel;
    }


    private JDialog dialog;

    private JComboBoxWithHistory searchField;

    private enum Scope {
        All, Current, Selection, Name;
        public String toString() {
            return resources.getString("Within." + name());
        }
    };

    private JComboBox<Scope> searchScope;

    private enum Direction {
        Rows, Columns;
        public String toString() {
            return resources.getString("Search_By." + name());
        }
    };

    private JComboBox<Direction> rowsOrColumns;

    private WrappedIcon wrappedIcon;

    private List<String> selectedColumns, foundColumn;


    public void actionPerformed(ActionEvent e) {
        if (dialog == null)
            createDialog();

        dialog.setVisible(true);
        dialog.toFront();
        searchField.getEditor().selectAll();
        searchField.requestFocusInWindow();
        wrappedIcon.setVisible(false);
    }

    private void createDialog() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        GridBagLayout layout = new GridBagLayout();
        panel.setLayout(layout);

        FindPreviousAction findPrevious = new FindPreviousAction();
        FindNextAction findNext = new FindNextAction();
        CancelAction cancel = new CancelAction();

        setKey(panel, KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK, findPrevious);
        setKey(panel, KeyEvent.VK_N, InputEvent.ALT_DOWN_MASK, findNext);
        setKey(panel, KeyEvent.VK_ESCAPE, 0, cancel);

        GridBagConstraints lc = new GridBagConstraints();
        lc.gridx = lc.gridy = 0; lc.gridwidth = 2;
        lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(0, 5, 5, 5);
        panel.add(new JLabel(resources.getString("Label")), lc);

        GridBagConstraints tc = new GridBagConstraints();
        tc.gridx = 2; tc.gridy = 0;
        tc.fill = GridBagConstraints.BOTH;
        tc.weightx = 1; tc.gridwidth = 2;
        tc.insets = new Insets(0, 0, 5, 5);
        searchField = new JComboBoxWithHistory();
        searchField.getEditor().addActionListener(new FindNextTrigger(findNext));
        panel.add(searchField, tc);

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
        panel.add(new JLabel(resources.getString("Search_By.Label")), lc);
        rowsOrColumns = new JComboBox(Direction.values());
        rowsOrColumns.setSelectedItem(Direction.Rows);
        panel.add(rowsOrColumns, pc);

        GridBagConstraints wc = new GridBagConstraints();
        wc.gridx = 3; wc.gridy = lc.gridy;
        wc.anchor = GridBagConstraints.WEST;
        wc.insets = lc.insets;
        wrappedIcon = new WrappedIcon();
        panel.add(wrappedIcon, wc);

        GridBagConstraints bc = new GridBagConstraints();
        bc.gridy = lc.gridy + 1;
        bc.anchor = GridBagConstraints.EAST;
        bc.insets = new Insets(10, 30, 5, 5);
        bc.gridwidth = 4;
        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(new JButton(findPrevious));
        buttonBox.add(Box.createHorizontalStrut(5));
        buttonBox.add(new JButton(findNext));
        buttonBox.add(Box.createHorizontalStrut(20));
        buttonBox.add(new JButton(cancel));
        panel.add(buttonBox, bc);

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
            return new StringTest() {
                public boolean test(String s) {
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


    private static class JComboBoxWithHistory extends JComboBox<String> {

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

    private static class HistoryModel extends DefaultComboBoxModel<String> {

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


    private class FindAction extends AbstractAction {
        
        private boolean searchForward;

        public FindAction(String resKey, boolean forward) {
            super(resources.getString(resKey));
            this.searchForward = forward;
        }

        public void actionPerformed(ActionEvent e) {
            StringTest filter = makeFilter();
            if (filter == null)
                return;
            searchField.getEditor().selectAll();

            boolean searchByColumns = //
                    (rowsOrColumns.getSelectedItem() == Direction.Columns);
            List<String> columnScope = getColumnSearchScope();

            FindResult foundMatch = tabPanel.findNextMatch(filter,
                searchForward, searchByColumns, columnScope);
            foundColumn = tabPanel.getSelectedColumnIDs();

            if (foundMatch == FindResult.NotFound)
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
        FindNextTrigger(FindNextAction delegate) {
            timer = new Timer(20, delegate);
            timer.setRepeats(false);
        }
        public void actionPerformed(ActionEvent e) {
            timer.restart();
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
