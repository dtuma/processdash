// Copyright (C) 2005 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.ui.wizard;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.beans.EventHandler;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.processdash.tool.export.mgr.AbstractInstruction;
import net.sourceforge.processdash.tool.export.mgr.AbstractManager;

public abstract class ManagePanel extends WizardPanel implements
        InstructionTable.Listener {

    protected AbstractManager manager;

    protected InstructionTable instructionTable;

    protected JButton addButton, editButton, deleteButton;

    protected abstract WizardPanel getAddPanel();

    protected abstract WizardPanel getEditPanel(AbstractInstruction instr);

    public ManagePanel(Wizard wizard, AbstractManager manager,
            String resourcePrefix) {
        super(wizard, resourcePrefix);
        this.manager = manager;
        buildUserInterface();
    }

    protected void buildMainPanelContents() {
        Box tableBox = Box.createHorizontalBox();
        tableBox.add(horizSpace(2));
        tableBox.add(new JScrollPane(createTable()));
        tableBox.add(horizSpace(2));

        Box editButtonBox = Box.createVerticalBox();
        editButtonBox.add(Box.createVerticalGlue());

        addButton = createManageButton("Add_Button", "addCallback");
        editButtonBox.add(addButton);
        editButtonBox.add(Box.createVerticalGlue());

        editButton = createManageButton("Edit_Button", "editCallback");
        editButtonBox.add(editButton);
        editButtonBox.add(Box.createVerticalGlue());

        deleteButton = createManageButton("Delete_Button", "deleteCallback");
        editButtonBox.add(deleteButton);
        editButtonBox.add(Box.createVerticalGlue());

        Dimension d = editButtonBox.getPreferredSize();
        editButtonBox.setMinimumSize(d);
        editButtonBox.setPreferredSize(d);
        editButtonBox.setMaximumSize(new Dimension(d.width, 10000));

        recalcEnablement();
        tableBox.add(editButtonBox);
        add(tableBox);
    }

    private JButton createManageButton(String labelKey, String callbackMethod) {
        JButton result = new JButton(getAbsoluteString(labelKey));
        result.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, callbackMethod));
        Dimension d = result.getPreferredSize();
        result.setMinimumSize(d);
        d.width = Integer.MAX_VALUE;
        result.setMaximumSize(d);
        return result;
    }

    @Override
    protected void addBottomPadding(Box verticalBox) { }

    public void addCallback() {
        add();
    }

    public void editCallback() {
        edit(instructionTable.getSelectedRow());
    }

    public void deleteCallback() {
        delete(instructionTable.getSelectedRow());
    }

    public void add() {
        wizard.goForward(getAddPanel());
    }

    public void edit(int row) {
        AbstractInstruction instr = manager.getInstruction(row);
        if (instr != null)
            wizard.goForward(getEditPanel(instr));
    }

    public void delete(int row) {
        AbstractInstruction instr = manager.getInstruction(row);
        if (instr == null)
            return;

        if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(this,
                getString("Confirm_Delete.Message"),
                getString("Confirm_Delete.Title"), JOptionPane.YES_NO_OPTION))
            return;

        manager.deleteInstruction(instr);
    }

    public void toggleEnabled(int row) {
        AbstractInstruction instr = manager.getInstruction(row);
        if (instr == null)
            return;

        if (instr.isEnabled()) {
            if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(this,
                    getString("Confirm_Disable.Message"),
                    getString("Confirm_Disable.Title"),
                    JOptionPane.YES_NO_OPTION))
                return;
        }

        AbstractInstruction newInstr = (AbstractInstruction) instr.clone();
        newInstr.setEnabled(!instr.isEnabled());
        manager.changeInstruction(instr, newInstr);
    }

    public void recalcEnablement() {
        boolean haveSelection = instructionTable.getSelectedRow() != -1;
        editButton.setEnabled(haveSelection);
        deleteButton.setEnabled(haveSelection);
    }

    private JTable createTable() {
        instructionTable = new InstructionTable(manager.getTableModel());
        instructionTable.getColumnModel().getColumn(0).setMaxWidth(70);
        instructionTable.getSelectionModel().addListSelectionListener(
                (ListSelectionListener) EventHandler.create(
                        ListSelectionListener.class, this, "recalcEnablement"));
        instructionTable.addInstructionTableListener(this);
        return instructionTable;
    }

    protected String getCancelButtonLabel() {
        return Wizard.resources.getString("Close_Button");
    }

    protected String getNextButtonLabel() {
        return null;
    }

    public void doNext() {
        // nothing to do
    }

}
