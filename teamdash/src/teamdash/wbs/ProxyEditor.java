// Copyright (C) 2014-2018 Tuma Solutions, LLC
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

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.GuiPrefs;

import teamdash.merge.ui.MergeConflictHyperlinkHandler;
import teamdash.wbs.AbstractLibraryEditor.Mode;

public class ProxyEditor implements MergeConflictHyperlinkHandler {

    /** The team project that these proxies belong to. */
    TeamProject teamProject;

    /** The data model for the proxy tables */
    ProxyDataModel proxyModel;

    /** The table to display the proxy tables in */
    ProxyJTable table;

    /** An object for tracking undo operations */
    UndoList undoList;

    /** The frame containing this proxy editor */
    JFrame frame;

    /** A toolbar for editing the milestones */
    JToolBar toolBar;

    static final Resources resources = Resources
            .getDashBundle("WBSEditor.Proxies");


    public ProxyEditor(TeamProject teamProject, ProxyDataModel proxyModel,
            GuiPrefs guiPrefs) {
        this.teamProject = teamProject;
        this.proxyModel = proxyModel;
        this.proxyModel.setEditingEnabled(isEditable(teamProject));

        table = createProxyJTable();
        table.setEditingEnabled(isEditable(teamProject));

        buildToolbar();

        frame = new JFrame(teamProject.getProjectName() + " - "
                + resources.getString("Window_Title"));
        frame.getContentPane().add(new JScrollPane(table));
        frame.getContentPane().add(toolBar, BorderLayout.NORTH);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(600, 400);
        WBSZoom.get().manage(frame, "size~");
        guiPrefs.load("proxyWindow", frame);
    }

    public static boolean isEditable(TeamProject teamProject) {
        return teamProject.isReadOnly() == false
                && WBSPermissionManager.hasPerm("wbs.proxies", "2.3.1.2");
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

    private ProxyJTable createProxyJTable() {
        ProxyJTable table = new ProxyJTable(proxyModel);
        table.renderer.setRootNodeName(resources.getString("Root_Name"));

        undoList = new UndoList(proxyModel.getWBSModel());
        undoList.setForComponent(table);
        proxyModel.addTableModelListener(new UndoableEventRepeater());

        return table;
    }

    private void buildToolbar() {
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setMargin(new Insets(0, 0, 0, 0));

        addToolbarButton(undoList.getUndoAction());
        addToolbarButton(undoList.getRedoAction());
        table.tweakClipboardActions(resources, IconFactory.getCopyProxyIcon(),
            IconFactory.getPasteProxyIcon());
        addToolbarButton(table.CUT_ACTION);
        addToolbarButton(table.COPY_ACTION);
        addToolbarButton(table.PASTE_ACTION);
        addToolbarButton(table.EXPAND_ACTION);
        addToolbarButton(table.COLLAPSE_ACTION);
        addToolbarButton(table.MOVEUP_ACTION);
        addToolbarButton(table.MOVEDOWN_ACTION);
        addToolbarButton(table.DELETE_ACTION);
        toolBar.addSeparator();

        if (!isEditable(teamProject)) {
            IMPORT.setEnabled(false);
            IMPORT_ORG.setEnabled(false);
        }
        TeamProcess teamProcess = teamProject.getTeamProcess();
        if (ProxyLibraryEditor.orgAssetsAreAvailable(teamProcess))
            addToolbarButton(IMPORT_ORG);
        addToolbarButton(IMPORT);
        addToolbarButton(EXPORT);
    }

    /** Add a button to the internal tool bar */
    private void addToolbarButton(Action a) {
        JButton button = new JButton(a);
        button.setFocusPainted(false);
        button.setToolTipText((String) a.getValue(Action.NAME));
        button.setText(null);
        IconFactory.setDisabledIcon(button);
        toolBar.add(button);
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
                    IconFactory.getExportProxiesIcon());
        }
        public void actionPerformed(ActionEvent e) {
            try {
                new ProxyLibraryEditor(teamProject, frame, Mode.Export);
            } catch (ProxyLibraryEditor.UserCancelledException uce) {
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    final Action EXPORT = new ExportAction();



    private class ImportOrgAction extends AbstractAction {
        public ImportOrgAction() {
            super(resources.getString("Import_Org_Tooltip"), //
                    IconFactory.getImportOrgProxiesIcon());
        }
        public void actionPerformed(ActionEvent e) {
            try {
                new ProxyLibraryEditor(teamProject, frame, Mode.ImportOrg);
                undoList.madeChange("Imported organizational proxies");
            } catch (ProxyLibraryEditor.UserCancelledException uce) {
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    final Action IMPORT_ORG = new ImportOrgAction();



    private class ImportAction extends AbstractAction {
        public ImportAction() {
            super(resources.getString("Import_Tooltip"), //
                    IconFactory.getImportProxiesIcon());
        }
        public void actionPerformed(ActionEvent e) {
            try {
                new ProxyLibraryEditor(teamProject, frame, Mode.Import);
                undoList.madeChange("Imported proxies");
            } catch (ProxyLibraryEditor.UserCancelledException uce) {
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

}
