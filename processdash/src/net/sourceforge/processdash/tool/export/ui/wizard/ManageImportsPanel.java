// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2005 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.ui.wizard;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import net.sourceforge.processdash.tool.export.mgr.AbstractInstruction;
import net.sourceforge.processdash.tool.export.mgr.ImportDirectoryInstruction;
import net.sourceforge.processdash.tool.export.mgr.ImportManager;

public class ManageImportsPanel extends ManagePanel {

    public ManageImportsPanel(Wizard wizard) {
        super(wizard, ImportManager.getInstance(), "Import.Manage");
    }

    protected WizardPanel getAddPanel() {
        return new EditImportDirectoryPanel(wizard, null, true);
    }

    protected WizardPanel getEditPanel(AbstractInstruction instr) {
        if (instr instanceof ImportDirectoryInstruction)
            return new EditImportDirectoryPanel(wizard,
                    (ImportDirectoryInstruction) instr, true);

        throw new IllegalArgumentException("Unrecognized instruction type");
    }

}
