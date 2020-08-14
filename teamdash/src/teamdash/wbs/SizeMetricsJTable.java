// Copyright (C) 2020 Tuma Solutions, LLC
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

package teamdash.wbs;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import net.sourceforge.processdash.hier.ui.icons.HierarchyIcons;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.ui.lib.ScaledIcon;

public class SizeMetricsJTable extends WBSJTable {

    private Resources resources = SizeMetricsEditor.resources;

    public SizeMetricsJTable(SizeMetricsDataModel model) {
        super(model, makeIconMap(), new JMenu());

        customizeColumns();
        tweakBehaviors();
    }

    private static Map makeIconMap() {
        Map result = new HashMap();
        result.put(SizeMetricsWBSModel.METRIC_LIST_TYPE,
            WBSZoom.icon(IconFactory.getSizeMetricListIcon()));
        result.put(SizeMetricsWBSModel.SIZE_METRIC_TYPE,
            WBSZoom.icon(IconFactory.getSizeMetricIcon()));
        result.put(null, WBSZoom.icon(IconFactory.getModifiedIcon(
            HierarchyIcons.getComponentIcon(), IconFactory.ERROR_ICON)));
        return result;
    }

    private void customizeColumns() {
        // customize the behavior and appearance of the columns.
        DataTableModel.installColumnCustomizations(this);

        // remove the traditional WBS node editor, so we can force a more
        // deliberate editing pattern for size metric names
        setDefaultEditor(WBSNode.class, new SizeMetricNameEditor());
    }

    private void tweakBehaviors() {
        // do not allow indentation; size metrics are a flat list
        setIndentationDisabled(true);

        // don't display a text editing cursor over size metric node names
        removeMouseMotionListener(MOTION_LISTENER);
    }

    @Override
    void installCustomActions(JComponent component) {
        // skip the installation of these actions, as we wish to force more
        // deliberate editing patterns for size metrics
    }

    @Override
    void installTableActions() {
        // skip the installation of these actions, as we wish to force more
        // deliberate editing patterns for size metrics
    }



    private class SizeMetricNameEditor extends DefaultCellEditor {

        public SizeMetricNameEditor() {
            // our superclass requires *some* editing component to be supplied.
            // Pass one in, even though it will never be used.
            super(new JTextField());
        }

        @Override
        public boolean isCellEditable(EventObject anEvent) {
            // if we notice an appropriate event, trigger the rename action
            if (shouldStartRenameAction(anEvent))
                SwingUtilities.invokeLater(RENAME_METRIC_ACTION);

            // the rename action will handle all of the editing that is needed.
            // return false from this method, so the table doesn't attempt to
            // perform any of the standard table-cell-editing logic.
            return false;
        }

        private boolean shouldStartRenameAction(EventObject e) {
            if (e instanceof MouseEvent)
                // start rename on double-click
                return ((MouseEvent) e).getClickCount() >= 2;
            else if (e instanceof KeyEvent)
                // start rename if the space bar is pressed on this cell
                return ((KeyEvent) e).getKeyChar() == ' ';
            else
                // ignore all other keys/gestures
                return false;
        }
    }



    private class AddMetricAction extends AbstractAction
            implements EnablementCalculation {

        public AddMetricAction() {
            super(resources.getString("Add.Title"),
                    IconFactory.getAddRowIcon());
            addEnablementCalculation(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // ask the user to enter a name for the new metric
            String newName = promptForMetricName(this,
                resources.getString("Add.Prompt"), "", null);

            // if a valid name was entered, add a new metric to our model.
            if (newName != null) {
                WBSNode node = new WBSNode(wbsModel, newName,
                        SizeMetricsWBSModel.SIZE_METRIC_TYPE, 1, false);
                wbsModel.add(node);

                // select the row for the newly added metric
                int row = wbsModel.getRowForNode(node);
                selectRows(new int[] { row }, true);

                // note the change that was made
                UndoList.madeChange(SizeMetricsJTable.this, "Add size metric");
            }
        }

        @Override
        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(isEditingEnabled());
        }
    }
    final AddMetricAction ADD_METRIC_ACTION = new AddMetricAction();



    private abstract class SingleMetricAction extends AbstractAction
            implements EnablementCalculation {

        public SingleMetricAction(String resKey, Icon icon) {
            super(resources.getString(resKey), icon);
            addEnablementCalculation(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] rows = getSelectedRows();
            recalculateEnablement(rows);
            if (isEnabled()) {
                int row = rows[0];
                actionPerformed(wbsModel.getNodeForRow(row), row);
            }
        }

        public abstract void actionPerformed(WBSNode node, int row);

        @Override
        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(isEditingEnabled() //
                    && selectedRows != null //
                    && selectedRows.length == 1 //
                    && selectedRows[0] != 0);
        }
    }



    private class RenameAction extends SingleMetricAction implements Runnable {

        public RenameAction() {
            super("Rename.Title", IconFactory.getRenameIcon());
        }

        @Override
        public void run() {
            actionPerformed(null);
        }

        @Override
        public void actionPerformed(WBSNode node, int row) {
            // get the current name of the node
            String oldName = node.getName();

            // prompt the user to enter a new name
            String newName = promptForMetricName(this,
                resources.format("Rename.Prompt_FMT", oldName), //
                oldName, SizeMetricsWBSModel.getMetricID(node));

            // if the user entered a valid new name, apply it
            if (newName != null && !newName.equals(oldName)) {
                ((SizeMetricsWBSModel) wbsModel).renameMetric(node, newName);
                UndoList.madeChange(SizeMetricsJTable.this,
                    "Rename size metric");
            }
        }
    }
    final RenameAction RENAME_METRIC_ACTION = new RenameAction();



    private class DeleteMetricAction extends SingleMetricAction {

        public DeleteMetricAction() {
            super("Delete.Title", IconFactory.getDeleteIcon());
        }

        @Override
        public void actionPerformed(WBSNode node, int row) {
            // ask the user to confirm the deletion of the selected metric
            String title = (String) getValue(Action.NAME);
            Icon icon = getLargeIcon(this);
            String message = resources.format("Delete.Prompt_FMT",
                node.getName());
            int userChoice = JOptionPane.showConfirmDialog(
                SizeMetricsJTable.this, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, icon);

            // if the user agrees, make the change
            if (userChoice == JOptionPane.YES_OPTION) {
                wbsModel.deleteNodes(Collections.singletonList(node));
                UndoList.madeChange(SizeMetricsJTable.this,
                    "Delete size metric");
            }
        }
    }
    final DeleteMetricAction DELETE_METRIC_ACTION = new DeleteMetricAction();



    private String promptForMetricName(Action action, String prompt,
            String defaultMetricName, String acceptableID) {
        // create a text field for the user to enter a size metric name
        JTextField name = new JTextField(defaultMetricName);

        // prepare the contents of the confirmation dialog
        String title = (String) action.getValue(Action.NAME);
        Icon icon = getLargeIcon(action);
        Object message = new Object[] { prompt, name,
                new JOptionPaneTweaker.GrabFocus(name) };

        while (true) {
            // display the dialog (prompting for a name) with OK/Cancel options
            name.selectAll();
            int userChoice = JOptionPane.showConfirmDialog(this, message, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, icon);

            // if the user didn't press OK (or hit enter), abort
            if (userChoice != JOptionPane.OK_OPTION)
                return null;

            // if the user didn't enter a name, abort
            String newName = name.getText().trim();
            newName = WBSClipSelection.scrubName(newName);
            if (newName.length() == 0)
                return null;

            // see if there is already another metric with this name
            String existingID = ((SizeMetricsWBSModel) wbsModel)
                    .getIdForMetric(newName);
            if (existingID == null || existingID.equals(acceptableID)) {
                return newName;
            } else {
                // show an error, then repeat the loop to get a different name
                JOptionPane.showMessageDialog(this,
                    resources.format("Duplicate_Name.Message_FMT", newName),
                    resources.getString("Duplicate_Name.Title"),
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static Icon getLargeIcon(Action action) {
        Icon icon = (Icon) action.getValue(Action.SMALL_ICON);
        return new ScaledIcon(icon, 1.75);
    }

}
