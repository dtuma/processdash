// Copyright (C) 2002-2014 Tuma Solutions, LLC
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
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.JTableColumnVisibilityButton;
import net.sourceforge.processdash.ui.lib.PaddedIcon;
import net.sourceforge.processdash.util.StringUtils;

import teamdash.merge.ui.MergeConflictHyperlinkHandler;
import teamdash.wbs.columns.WorkflowOptionalColumn;

/** A graphical user interface for editing common workflows.
 */
public class WorkflowEditor implements MergeConflictHyperlinkHandler {

    /** The team project that these workflows belong to. */
    TeamProject teamProject;
    /** The data model for the workflows */
    WorkflowModel workflowModel;
    /** The table to display the workflows in */
    WBSJTable table;
    /** The total preferred width of visible optional columns */
    int optColumnWidth;
    /** The frame containing this workflow editor */
    JFrame frame;
    /** A toolbar for editing the workflows */
    JToolBar toolBar;
    /** An object for tracking undo operations */
    UndoList undoList;

    private static final Resources resources = Resources
            .getDashBundle("WBSEditor.Workflows");


    public WorkflowEditor(TeamProject teamProject) {
        this.teamProject = teamProject;
        this.workflowModel = new WorkflowModel
            (teamProject.getWorkflows(), teamProject.getTeamProcess());
        this.workflowModel.setEditingEnabled(teamProject.isReadOnly() == false);
        table = createWorkflowJTable
            (workflowModel, teamProject.getTeamProcess());
        JTableColumnVisibilityButton columnSelector = adjustColumnVisibility();

        undoList = new UndoList(workflowModel.getWBSModel());
        undoList.setForComponent(table);
        workflowModel.addTableModelListener(new UndoableEventRepeater());

        table.setEditingEnabled(teamProject.isReadOnly() == false);
        buildToolbar(columnSelector.getAction());
        frame = new JFrame(teamProject.getProjectName() +
                           " - Common Team Workflows");
        frame.getContentPane().add(columnSelector.install(new JScrollPane(table)));
        frame.getContentPane().add(toolBar, BorderLayout.NORTH);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(600 + optColumnWidth, 400);
        frame.setVisible(true);
    }

    public void show() {
        frame.setExtendedState(JFrame.NORMAL);
        frame.setVisible(true);
        frame.toFront();
    }

    public void hide() {
        frame.setVisible(false);
    }

    public void stopEditing() {
        table.stopCellEditing();
    }

    public boolean displayHyperlinkedItem(String item) {
        int nodeId = Integer.parseInt(item);
        if (table.selectAndShowNode(nodeId) == false)
            return false;

        show();
        return true;
    }


    public static WBSJTable createWorkflowJTable(WorkflowModel workflowModel, TeamProcess process) {
        // create the WBSJTable, then set its model to the workflow data model.
        WBSJTable table = new WBSJTable(workflowModel.getWBSModel(),
                getWorkflowIcons(process.getIconMap()),
                process.getNodeTypeMenu());
        table.setModel(workflowModel);
        // don't allow reordering, since the text displayed in several of the
        // columns is meant to be read from left to right.
        table.getTableHeader().setReorderingAllowed(false);
        // install the default editor for table data.
        table.setDefaultEditor(Object.class, new WorkflowCellEditor());

        // customize the behavior and appearance of the columns.
        for (int i = 0;  i < table.getColumnCount();  i++) {
            TableColumn tc = table.getColumnModel().getColumn(i);
            DataColumn dc = workflowModel.getColumn(tc.getModelIndex());
            DataTableColumn.init(tc, dc);
        }

        return table;
    }

    private static Map getWorkflowIcons(Map<String, Icon> processIconMap) {
        // make a copy of the icon map.
        Map result = new HashMap(processIconMap);
        // change the "project" icon to a special value for common workflows
        result.put(TeamProcess.PROJECT_TYPE,
            IconFactory.getCommonWorkflowsIcon());
        // replace each of the "task" icons with the correponsponding
        // "workflow task" icon.
        for (Map.Entry<String, Icon> e : processIconMap.entrySet()) {
            String key = e.getKey();
            if (key != null && key.endsWith(TeamProcess.WORKFLOW_TASK_SUFFIX)) {
                String taskType = StringUtils.findAndReplace(key,
                    TeamProcess.WORKFLOW_TASK_SUFFIX, TeamProcess.TASK_SUFFIX);
                Icon icon = e.getValue();
                result.put(taskType, icon);
            }
        }
        return result;
    }

    private JTableColumnVisibilityButton adjustColumnVisibility() {
        // make a list of the columns that MUST be displayed
        IntList mandatoryColumns = new IntList();
        for (int col = 0; col < workflowModel.getColumnCount(); col++) {
            DataColumn column = workflowModel.getColumn(col);
            if (column instanceof WorkflowOptionalColumn == false)
                mandatoryColumns.add(col);
        }

        // create a button that allows the user to select visible columns.
        JTableColumnVisibilityButton button = new JTableColumnVisibilityButton(
                table, resources, null, mandatoryColumns.getAsArray());

        // look through the optional columns and hide if needed.
        for (int col = workflowModel.getColumnCount(); col-- > 0;) {
            DataColumn column = workflowModel.getColumn(col);
            if (column instanceof WorkflowOptionalColumn) {
                WorkflowOptionalColumn optCol = (WorkflowOptionalColumn) column;
                if (optCol.shouldHideColumn(workflowModel))
                    table.getColumnModel().removeColumn(
                        table.getColumnModel().getColumn(col));
                else
                    optColumnWidth += column.getPreferredWidth();
            }
        }

        return button;
    }

    private void buildToolbar(Action... actions) {
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setMargin(new Insets(0,0,0,0));

        addToolbarButton(undoList.getUndoAction());
        addToolbarButton(undoList.getRedoAction());
        addToolbarButtons(table.getEditingActions());
        addToolbarButtons(actions);
        toolBar.addSeparator();
        addToolbarButtons(getWorkflowActions());
    }

    /** Add one or more buttons to the internal tool bar */
    private void addToolbarButtons(Action[] actions) {
        for (int i = 0; i < actions.length; i++)
            if (actions[i].getValue(Action.SMALL_ICON) != null)
                addToolbarButton(actions[i]);
    }

    /** Add a button to the internal tool bar */
    private void addToolbarButton(Action a) {
        JButton button = new JButton(a);
        //button.setMargin(new Insets(0, 0, 0, 0));
        button.setFocusPainted(false);
        button.setToolTipText((String) a.getValue(Action.NAME));
        button.setText(null);
        tweakIconsForToolbarButton(button);

        toolBar.add(button);
    }

    private void tweakIconsForToolbarButton(JButton button) {
        Icon icon = button.getIcon();
        if (icon == null)
            return;

        // most of our icons are 16x16, but some are smaller.  The toolbar
        // draws a smaller button for those icons, which looks bad.  Pad the
        // small icons to avoid this problem.
        int width = icon.getIconWidth();
        int height = icon.getIconHeight();
        if (width < 16 || height < 16) {
            int padL = Math.max(0, (16 - width) / 2);
            int padR = Math.max(0, 16 - width - padL);
            int padT = Math.max(0, (16 - height) / 2);
            int padB = Math.max(0, 16 - height - padT);
            icon = new PaddedIcon(icon, padT, padL, padB, padR);
            button.setIcon(icon);
        }

        // make sure we have a disabled icon set if needed
        if (!(icon instanceof ImageIcon))
            button.setDisabledIcon(IconFactory.getModifiedIcon(icon,
                    IconFactory.DISABLED_ICON));
    }

    public void addChangeListener(ChangeListener l) {
        undoList.addChangeListener(l);
    }

    public void removeChangeListener(ChangeListener l) {
        undoList.removeChangeListener(l);
    }

    private static class WorkflowCellEditor extends DefaultCellEditor {

        public WorkflowCellEditor() {
            super(new JTextField());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int column) {
            Component result = super.getTableCellEditorComponent(table,
                ErrorValue.unwrap(value), isSelected, row, column);

            if (result instanceof JTextField)
                ((JTextField) result).selectAll();

            return result;
        }

    }

    private class ExportAction extends AbstractAction {
        public ExportAction() {
            super("Export...", IconFactory.getExportIcon());
            super.putValue(SHORT_DESCRIPTION, "Export Workflows");
        }
        public void actionPerformed(ActionEvent e) {
            try {
                new WorkflowLibraryEditor(teamProject, frame, true);
            } catch (WorkflowLibraryEditor.UserCancelledException uce) {
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    final Action EXPORT = new ExportAction();



    private class ImportAction extends AbstractAction {
        public ImportAction() {
            super("Import...", IconFactory.getImportIcon());
            super.putValue(SHORT_DESCRIPTION, "Import Workflows");
        }
        public void actionPerformed(ActionEvent e) {
            try {
                new WorkflowLibraryEditor(teamProject, frame, false);
                undoList.madeChange("Imported workflows");
            } catch (WorkflowLibraryEditor.UserCancelledException uce) {
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    final Action IMPORT = new ImportAction();



    public Action[] getWorkflowActions() {
        if (teamProject.isReadOnly())
            IMPORT.setEnabled(false);
        return new Action[] { IMPORT, EXPORT };
    }

    private class UndoableEventRepeater implements TableModelListener {

        public void tableChanged(TableModelEvent e) {
            if (e.getColumn() > 0 && e.getFirstRow() > 0
                    && e.getFirstRow() == e.getLastRow())
                undoList.madeChange("Edited value");
        }

    }

    /*
    private Set saveListeners = null;
    public void addSaveListener(SaveListener l) {
        if (saveListeners == null)
            saveListeners = new HashSet();
        saveListeners.add(l);
    }
    public void removeSaveListener(SaveListener l) {
        if (saveListeners != null)
            saveListeners.remove(l);
    }
    */
}
