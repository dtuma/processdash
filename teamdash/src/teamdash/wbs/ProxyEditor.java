// Copyright (C) 2014 Tuma Solutions, LLC
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
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import net.sourceforge.processdash.i18n.Resources;

import teamdash.merge.ui.MergeConflictHyperlinkHandler;

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


    public ProxyEditor(TeamProject teamProject, ProxyDataModel proxyModel) {
        this.teamProject = teamProject;
        this.proxyModel = proxyModel;
        this.proxyModel.setEditingEnabled(teamProject.isReadOnly() == false);

        table = createProxyJTable();
        table.setEditingEnabled(teamProject.isReadOnly() == false);

        buildToolbar();

        frame = new JFrame(resources.format("Window_Title_FMT",
            teamProject.getProjectName()));
        frame.getContentPane().add(new JScrollPane(table));
        frame.getContentPane().add(toolBar, BorderLayout.NORTH);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(600, 400);
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

    private ProxyJTable createProxyJTable() {
        ProxyJTable table = new ProxyJTable(proxyModel);

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
        addToolbarButton(tweakAction(table.CUT_ACTION,
            resources.getString("Cut"), null));
        addToolbarButton(tweakAction(table.COPY_ACTION,
            resources.getString("Copy"), IconFactory.getCopyProxyIcon()));
        addToolbarButton(tweakAction(table.PASTE_ACTION,
            resources.getString("Paste"), IconFactory.getPasteProxyIcon()));
        addToolbarButton(table.EXPAND_ACTION);
        addToolbarButton(table.COLLAPSE_ACTION);
        addToolbarButton(table.MOVEUP_ACTION);
        addToolbarButton(table.MOVEDOWN_ACTION);
        addToolbarButton(table.DELETE_ACTION);
        toolBar.addSeparator();

        if (teamProject.isReadOnly())
            IMPORT.setEnabled(false);
        addToolbarButton(IMPORT);
        addToolbarButton(EXPORT);
    }

    private Action tweakAction(AbstractAction a, String name, Icon icon) {
        if (name != null)
            a.putValue(Action.NAME, name);
        if (icon != null)
            a.putValue(Action.SMALL_ICON, icon);
        return a;
    }

    /** Add a button to the internal tool bar */
    private void addToolbarButton(Action a) {
        JButton button = new JButton(a);
        button.setFocusPainted(false);
        button.setToolTipText((String) a.getValue(Action.NAME));
        button.setText(null);

        Icon icon = button.getIcon();
        if (icon != null && !(icon instanceof ImageIcon))
            button.setDisabledIcon(IconFactory.getModifiedIcon(icon,
                IconFactory.DISABLED_ICON));

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
                new ProxyLibraryEditor(teamProject, frame, true);
            } catch (ProxyLibraryEditor.UserCancelledException uce) {
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    final Action EXPORT = new ExportAction();



    private class ImportAction extends AbstractAction {
        public ImportAction() {
            super(resources.getString("Import_Tooltip"), //
                    IconFactory.getImportProxiesIcon());
        }
        public void actionPerformed(ActionEvent e) {
            try {
                new ProxyLibraryEditor(teamProject, frame, false);
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
