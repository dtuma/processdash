// Copyright (C) 2012-2013 Tuma Solutions, LLC
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

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;

public class WBSFindAction extends AbstractAction {

    private WBSJTable wbsTable;

    private static final Resources resources = Resources
            .getDashBundle("WBSEditor.Find");

    public WBSFindAction(WBSJTable wbsTable) {
        super(resources.getString("Menu"), IconFactory.getFindIcon());
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, //
            MacGUIUtils.getCtrlModifier()));
        this.wbsTable = wbsTable;
    }


    private JDialog dialog;

    private JTextField searchField;

    public void actionPerformed(ActionEvent e) {
        if (dialog == null)
            createDialog();

        searchField.selectAll();
        dialog.setVisible(true);
        dialog.toFront();
    }

    private void createDialog() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        FindPreviousAction findPrevious = new FindPreviousAction();
        FindNextAction findNext = new FindNextAction();
        CancelAction cancel = new CancelAction();

        setKey(panel, KeyEvent.VK_UP, findPrevious);
        setKey(panel, KeyEvent.VK_DOWN, findNext);
        setKey(panel, KeyEvent.VK_ESCAPE, cancel);

        JLabel label = new JLabel(resources.getString("Prompt"), SwingConstants.LEFT);
        label.setAlignmentX(0);
        panel.add(label);
        panel.add(Box.createVerticalStrut(5));

        searchField = new JTextField();
        searchField.setAction(findNext);
        searchField.setAlignmentX(0);
        panel.add(searchField);
        panel.add(Box.createVerticalStrut(5));

        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(Box.createHorizontalStrut(30));
        buttonBox.add(new JButton(findPrevious));
        buttonBox.add(Box.createHorizontalStrut(5));
        buttonBox.add(new JButton(findNext));
        buttonBox.add(Box.createHorizontalStrut(20));
        buttonBox.add(new JButton(cancel));
        buttonBox.add(Box.createHorizontalStrut(1));
        buttonBox.setAlignmentX(0);
        panel.add(buttonBox);

        JFrame f = (JFrame) SwingUtilities.getWindowAncestor(wbsTable);
        dialog = new JDialog(f, resources.getString("Title"), false);
        dialog.getContentPane().add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(f);
    }

    private void setKey(JPanel panel, int keystroke, Action action) {
        String actionName = action.getClass().getSimpleName();
        panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(keystroke, 0), actionName);
        panel.getActionMap().put(actionName, action);

    }

    private WBSFilter makeFilter() {
        String searchText = searchField.getText().trim();
        if (searchText.length() == 0) {
            Toolkit.getDefaultToolkit().beep();
            return null;
        } else {
            return WBSFilterFactory.createTextFilter(searchText);
        }
    }

    private abstract class FindAction extends AbstractAction {

        public FindAction(String resKey) {
            super(resources.getString(resKey));
        }

        public void actionPerformed(ActionEvent e) {
            WBSFilter filter = makeFilter();
            if (filter == null)
                return;
            searchField.selectAll();

            WBSModel wbsModel = (WBSModel) wbsTable.getModel();

            WBSNode selectedNode = null;
            int row = wbsTable.getSelectedRow();
            if (row != -1)
                selectedNode = wbsModel.getNodeForRow(row);

            WBSNode foundNode = doFind(wbsModel, filter, selectedNode);

            if (wbsTable.selectAndShowNode(foundNode) == false)
                JOptionPane.showMessageDialog(dialog,
                    resources.getString("Not_Found.Message"),
                    resources.getString("Not_Found.Title"),
                    JOptionPane.PLAIN_MESSAGE);
        }

        protected abstract WBSNode doFind(WBSModel model, WBSFilter filter,
                WBSNode from);
    }

    private class FindPreviousAction extends FindAction {
        public FindPreviousAction() {
            super("Previous");
        }

        protected WBSNode doFind(WBSModel model, WBSFilter f, WBSNode from) {
            return model.findPreviousNodeMatching(f, from, true);
        }
    }

    private class FindNextAction extends FindAction {
        public FindNextAction() {
            super("Next");
        }

        protected WBSNode doFind(WBSModel model, WBSFilter f, WBSNode from) {
            return model.findNextNodeMatching(f, from, true);
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

}
