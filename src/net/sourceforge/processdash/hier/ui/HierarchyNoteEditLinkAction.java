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

package net.sourceforge.processdash.hier.ui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.hier.HierarchyNoteManager;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.process.ScriptID;

public class HierarchyNoteEditLinkAction extends AbstractAction {

    private DashboardContext ctx;

    private ActiveTaskModel activeTaskModel;

    private ScriptID scriptID;

    private static final String TOOLTIP = Resources
            .getDashBundle("ProcessDashboard")
            .getDlgString("ScriptEditor.Edit.Title");


    public HierarchyNoteEditLinkAction(DashboardContext ctx,
            ScriptID scriptID) {
        this.ctx = ctx;
        this.activeTaskModel = ((ProcessDashboard) ctx).getActiveTaskModel();
        this.scriptID = scriptID;
        putValue(SHORT_DESCRIPTION, TOOLTIP);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // retrieve information about the script we should edit
        String activeTask = activeTaskModel.getPath();
        String targetPath = scriptID.getDataPath();
        String linkUrl = scriptID.getHref();
        String linkText = scriptID.getDisplayName();

        // create a link editor and display it
        HierarchyNoteEditLinkDialog dlg = new HierarchyNoteEditLinkDialog(
                activeTask, targetPath, linkUrl, linkText);
        int userChoice = dlg.display((ProcessDashboard) ctx);

        // if the user cancelled the operation, return
        if (userChoice == HierarchyNoteEditLinkDialog.CANCEL)
            return;

        // if the link was deleted or relocated, remove it from the old note
        if (userChoice == HierarchyNoteEditLinkDialog.DELETE
                || dlg.isLinkPathChanged()) {
            HierarchyNoteManager.replaceHyperlink(ctx.getData(), targetPath,
                linkUrl, null, null);
            linkUrl = dlg.getNewURL();
        }

        // if a new link was saved, store it in the new note
        if (userChoice == HierarchyNoteEditLinkDialog.SAVE) {
            HierarchyNoteManager.replaceHyperlink(ctx.getData(),
                dlg.getNewPath(), linkUrl, dlg.getNewURL(),
                dlg.getNewLinkText());
        }
    }

}
