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

import net.sourceforge.processdash.tool.export.mgr.AbstractInstruction;
import net.sourceforge.processdash.tool.export.mgr.ImportDirectoryInstruction;
import net.sourceforge.processdash.tool.export.mgr.ImportInstructionDispatcher;
import net.sourceforge.processdash.tool.export.mgr.ImportManager;

public class ManageImportsPanel extends ManagePanel {

    public ManageImportsPanel(Wizard wizard) {
        super(wizard, ImportManager.getInstance(), "Import.Manage");
    }

    protected WizardPanel getAddPanel() {
        // hardcoded for now - only one choice.
        return new EditImportDirectoryPanel(wizard, null, true);
    }

    protected WizardPanel getEditPanel(AbstractInstruction instr) {
        return (WizardPanel) instr.dispatch(editPanelGenerator);
    }

    private class EditPanelGenerator implements ImportInstructionDispatcher {

        public Object dispatch(ImportDirectoryInstruction instr) {
            return new EditImportDirectoryPanel(wizard, instr, true);
        }

    }

    private EditPanelGenerator editPanelGenerator = new EditPanelGenerator();

}
