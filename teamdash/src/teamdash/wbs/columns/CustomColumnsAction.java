// Copyright (C) 2015 Tuma Solutions, LLC
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

package teamdash.wbs.columns;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.EventHandler;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;

import teamdash.wbs.DataColumn;

public class CustomColumnsAction extends AbstractAction {

    private CustomColumnManager columnManager;

    private Window parentWindow;

    private JList columnList;

    private JPanel content;

    private DataColumnList model;

    private static final Resources resources = Resources
            .getDashBundle("WBSEditor.CustomColumns");


    public CustomColumnsAction(Component parent,
            CustomColumnManager columnManager) {
        super(resources.getString("Menu"));
        this.columnManager = columnManager;
        this.parentWindow = SwingUtilities.getWindowAncestor(parent);

        this.columnList = new JList();
        this.columnList.addListSelectionListener(EventHandler.create(
            ListSelectionListener.class, this, "updateEnablement"));
        JScrollPane sp = new JScrollPane(columnList);
        sp.setPreferredSize(new Dimension(150, 150));

        GridBagLayout layout = new GridBagLayout();
        this.content = new JPanel(layout);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = c.gridy = 0;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 0, 10);

        addButtons(layout, c, addAction, editAction, deleteAction,
            importAction, exportAction);
        addComponent(sp, layout, c);
    }

    private void addButtons(GridBagLayout layout, GridBagConstraints listc,
            Action... actions) {
        GridBagConstraints bc = new GridBagConstraints();
        bc.fill = GridBagConstraints.HORIZONTAL;
        bc.gridx = 1;
        bc.gridy = 0;
        GridBagConstraints sc = new GridBagConstraints();
        sc.gridx = 1;
        sc.gridy = -1;
        sc.weighty = 1;

        for (Action a : actions) {
            if (bc.gridy > 0)
                addComponent(new JPanel(), layout, sc);

            addComponent(new JButton(a), layout, bc);

            bc.gridy += 2;
            sc.gridy += 2;
        }

        addComponent(new JOptionPaneTweaker.MakeResizable(), layout, bc);

        listc.gridheight = bc.gridy + 1;
    }

    private void addComponent(Component comp, GridBagLayout layout,
            GridBagConstraints constraints) {
        content.add(comp);
        layout.setConstraints(comp, constraints);
    }

    public void actionPerformed(ActionEvent e) {
        init();
        JOptionPane.showMessageDialog(parentWindow, content,
            resources.getString("Window_Title"), JOptionPane.PLAIN_MESSAGE);
    }

    private void init() {
        model = new DataColumnList();
        columnList.setModel(model);
        updateEnablement();
    }

    public void updateEnablement() {
        boolean hasSelection = columnList.getSelectedIndex() != -1;
        editAction.setEnabled(hasSelection);
        deleteAction.setEnabled(hasSelection);
        boolean hasContent = columnList.getModel().getSize() > 0;
        exportAction.setEnabled(hasContent);
    }

    private class DataColumnList extends AbstractListModel {

        private List<DataColumn> dataColumns;

        private DataColumnList() {
            dataColumns = columnManager.getProjectSpecificColumns();
        }

        public int getSize() {
            return dataColumns.size();
        }

        public Object getElementAt(int index) {
            return dataColumns.get(index).getColumnName();
        }

    }

    private class ColumnAction extends AbstractAction {

        private ColumnAction(String resKey) {
            super(resources.getString(resKey));
        }

        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(null, "Not yet implemented");
        }

    }


    private class AddAction extends ColumnAction {

        public AddAction() {
            super("Add_Button");
        }

    }

    private AddAction addAction = new AddAction();


    private class EditAction extends ColumnAction {

        public EditAction() {
            super("Edit_Button");
        }

    }

    private EditAction editAction = new EditAction();


    private class DeleteAction extends ColumnAction {

        public DeleteAction() {
            super("Delete_Button");
        }

    }

    private DeleteAction deleteAction = new DeleteAction();


    private class ImportAction extends ColumnAction {

        public ImportAction() {
            super("Import_Button");
        }

    }

    private ImportAction importAction = new ImportAction();


    private class ExportAction extends ColumnAction {

        public ExportAction() {
            super("Export_Button");
        }

    }

    private ExportAction exportAction = new ExportAction();

}
