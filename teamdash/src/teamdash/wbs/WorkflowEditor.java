// Copyright (C) 2002-2020 Tuma Solutions, LLC
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
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.GuiPrefs;
import net.sourceforge.processdash.ui.lib.JTableColumnVisibilityAction;
import net.sourceforge.processdash.ui.lib.JTableColumnVisibilityButton;
import net.sourceforge.processdash.ui.lib.PaddedIcon;

import teamdash.merge.ui.MergeConflictHyperlinkHandler;
import teamdash.wbs.AbstractLibraryEditor.Mode;
import teamdash.wbs.columns.WorkflowNonpersonalColumn;
import teamdash.wbs.columns.WorkflowOptionalColumn;
import teamdash.wbs.columns.WorkflowSizeUnitsColumn;
import teamdash.wbs.icons.WBSEditorIcon;

/** A graphical user interface for editing common workflows.
 */
public class WorkflowEditor implements MergeConflictHyperlinkHandler {

    /** The team project that these workflows belong to. */
    TeamProject teamProject;
    /** The data model for the workflows */
    WorkflowDataModel workflowModel;
    /** The table to display the workflows in */
    WorkflowJTable table;
    /** The total preferred width of visible optional columns */
    int optColumnWidth;
    /** The frame containing this workflow editor */
    JFrame frame;
    /** A toolbar for editing the workflows */
    JToolBar toolBar;
    /** An object for tracking undo operations */
    UndoList undoList;

    static final Resources resources = Resources
            .getDashBundle("WBSEditor.Workflows");


    public WorkflowEditor(TeamProject teamProject, WBSWindowTitle title,
            GuiPrefs guiPrefs) {
        this.teamProject = teamProject;
        this.workflowModel = new WorkflowDataModel(teamProject.getWorkflows(),
                teamProject.getTeamProcess(), teamProject.getTeamMemberList());
        this.workflowModel.setEditingEnabled(isEditable(teamProject));

        boolean hideProbe = teamProject.getBoolUserSetting("hideProbeTask");
        UnitsColumnVisibilityMgr unitsColMgr = new UnitsColumnVisibilityMgr();
        table = new WorkflowJTable(workflowModel, teamProject.getTeamProcess(),
                hideProbe == false, unitsColMgr);
        JTableColumnVisibilityButton columnSelector = adjustColumnVisibility();
        unitsColMgr.init();
        guiPrefs.load("workflowTable", table);

        undoList = new UndoList(workflowModel.getWBSModel());
        undoList.setForComponent(table);
        workflowModel.addTableModelListener(new UndoableEventRepeater());

        table.setEditingEnabled(isEditable(teamProject));
        buildToolbar(columnSelector.getAction());
        frame = title.register(new JFrame(resources.getString("Window_Title")));
        WBSEditorIcon.setWindowIcon(frame);
        frame.getContentPane().add(columnSelector.install(new JScrollPane(table)));
        frame.getContentPane().add(toolBar, BorderLayout.NORTH);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(800 + optColumnWidth, 400);
        WBSZoom.get().manage(frame, "size~");
        guiPrefs.load("workflowWindow", frame);
        frame.setVisible(true);
    }

    public static boolean isEditable(TeamProject teamProject) {
        return teamProject.isReadOnly() == false
                && WBSPermissionManager.hasPerm("wbs.workflows", "2.3.1.2");
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
        if (table.displayHyperlinkedItem(item) == false)
            return false;

        show();
        return true;
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

            } else if (teamProject.isPersonalProject()
                    && column instanceof WorkflowNonpersonalColumn) {
                table.getColumnModel()
                        .removeColumn(table.getColumnModel().getColumn(col));
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
        table.tweakClipboardActions(resources,
            IconFactory.getCopyWorkflowIcon(),
            IconFactory.getPasteWorkflowIcon());
        addToolbarButtons(table.getEditingActions());
        addToolbarButtons(actions);
        toolBar.addSeparator();

        if (isEditable(teamProject) == false) {
            IMPORT.setEnabled(false);
            IMPORT_ORG.setEnabled(false);
        }
        TeamProcess teamProcess = teamProject.getTeamProcess();
        if (WorkflowLibraryEditor.orgAssetsAreAvailable(teamProcess))
            addToolbarButton(IMPORT_ORG);
        addToolbarButton(IMPORT);
        addToolbarButton(EXPORT);
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
        if (a instanceof JTableColumnVisibilityAction)
            button.setIcon(IconFactory.getAddColumnIcon());
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
        IconFactory.setDisabledIcon(button);
    }

    public void addChangeListener(ChangeListener l) {
        undoList.addChangeListener(l);
    }

    public void removeChangeListener(ChangeListener l) {
        undoList.removeChangeListener(l);
    }

    private class ExportAction extends AbstractAction {
        public ExportAction() {
            super(resources.getString("Export_Tooltip"), //
                    IconFactory.getExportWorkflowsIcon());
        }
        public void actionPerformed(ActionEvent e) {
            try {
                new WorkflowLibraryEditor(teamProject, frame, Mode.Export);
            } catch (WorkflowLibraryEditor.UserCancelledException uce) {
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    final Action EXPORT = new ExportAction();



    private class ImportAssetAction extends AbstractAction {
        public ImportAssetAction() {
            super(resources.getString("Import_Org_Tooltip"), //
                    IconFactory.getImportOrgWorkflowsIcon());
        }
        public void actionPerformed(ActionEvent e) {
            try {
                new WorkflowLibraryEditor(teamProject, frame, Mode.ImportOrg);
                undoList.madeChange("Imported organizational workflows");
            } catch (WorkflowLibraryEditor.UserCancelledException uce) {
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    final Action IMPORT_ORG = new ImportAssetAction();



    private class ImportAction extends AbstractAction {
        public ImportAction() {
            super(resources.getString("Import_Tooltip"), //
                    IconFactory.getImportWorkflowsIcon());
        }
        public void actionPerformed(ActionEvent e) {
            try {
                new WorkflowLibraryEditor(teamProject, frame, Mode.Import);
                undoList.madeChange("Imported workflows");
            } catch (WorkflowLibraryEditor.UserCancelledException uce) {
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    final Action IMPORT = new ImportAction();



    private class UndoableEventRepeater implements TableModelListener {

        public void tableChanged(TableModelEvent e) {
            if (e.getColumn() > 0 && e.getFirstRow() > 0
                    && e.getFirstRow() == e.getLastRow())
                undoList.madeChange("Edited value");
        }

    }

    private class UnitsColumnVisibilityMgr implements ActionListener {

        private int unitsModelPos;
        private TableColumn unitsColumn;
        private boolean hasProbe;

        public void init() {
            unitsModelPos = workflowModel
                    .findColumn(WorkflowSizeUnitsColumn.COLUMN_ID);
            int unitsViewPos = table.convertColumnIndexToView(unitsModelPos);
            unitsColumn = table.getColumnModel().getColumn(unitsViewPos);

            WBSModel wbsModel = workflowModel.getWBSModel();
            for (WBSNode node : wbsModel.getDescendants(wbsModel.getRoot())) {
                if (node.getIndentLevel() > 1
                        && TeamProcess.isProbeTask(node.getType()))
                    hasProbe = true;
            }
            adjustVisibility();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            hasProbe = true;
            adjustVisibility();
        }

        private void adjustVisibility() {
            int unitsViewPos = table.convertColumnIndexToView(unitsModelPos);
            boolean unitsIsVisible = (unitsViewPos != -1);

            boolean unitsShouldBeVisible = hasProbe;
            if (unitsIsVisible == unitsShouldBeVisible)
                return;
            else if (unitsShouldBeVisible)
                showUnitsColumn();
            else
                hideUnitsColumn();
        }

        private void showUnitsColumn() {
            TableColumnModel columnModel = table.getColumnModel();
            columnModel.addColumn(unitsColumn);

            int srcPos = table.convertColumnIndexToView(unitsModelPos);
            int destPos = 2;
            if (srcPos != destPos)
                columnModel.moveColumn(srcPos, destPos);
        }

        private void hideUnitsColumn() {
            table.getColumnModel().removeColumn(unitsColumn);
        }

    }

}
