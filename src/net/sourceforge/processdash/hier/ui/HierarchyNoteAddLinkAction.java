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
import javax.swing.JComponent;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.hier.HierarchyNoteManager;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.StringUtils;

public class HierarchyNoteAddLinkAction extends AbstractAction {

    private JComponent parent;

    private DashboardContext ctx;

    private ActiveTaskModel activeTaskModel;

    public HierarchyNoteAddLinkAction(JComponent parent, DashboardContext ctx,
            ActiveTaskModel activeTaskModel) {
        super(Resources.getDashBundle("ProcessDashboard.ScriptEditor")
                .getDlgString("Add.Title"));
        this.parent = parent;
        this.ctx = ctx;
        this.activeTaskModel = activeTaskModel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String activeTask = activeTaskModel.getPath();
        String targetPath = getTargetPath(activeTask);
        HierarchyNoteEditLinkDialog dlg = new HierarchyNoteEditLinkDialog(
                activeTask, targetPath, null, null);
        int userChoice = dlg.display(parent);
        if (userChoice == HierarchyNoteEditLinkDialog.SAVE) {
            HierarchyNoteManager.replaceHyperlink(ctx.getData(),
                dlg.getNewPath(), null, dlg.getNewURL(), dlg.getNewLinkText());
        }
    }

    private String getTargetPath(String path) {
        String result = path;
        while (path.length() > 1) {
            PropertyKey node = ctx.getHierarchy().findExistingKey(path);
            if (StringUtils.hasValue(ctx.getHierarchy().getID(node)))
                result = path;
            path = DataRepository.chopPath(path);
        }
        return result;
    }

}
