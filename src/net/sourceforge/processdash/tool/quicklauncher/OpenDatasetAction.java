// Copyright (C) 2011-2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.quicklauncher;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.export.ui.SaveBackupAction;

public class OpenDatasetAction extends AbstractAction {

    Component parent;

    SaveBackupAction saveAction;

    SimpleInternalLauncher launcher;

    private static final Resources resources = Resources
            .getDashBundle("ProcessDashboard");


    public OpenDatasetAction(Component parent, SaveBackupAction saveAction) {
        super(resources.getString("Menu.Open_Dataset"));
        this.parent = parent;
        this.saveAction = saveAction;
        this.launcher = new SimpleInternalLauncher();
    }

    public void actionPerformed(ActionEvent e) {
        String title = resources.getString("Menu.Open_Dataset");
        File file = saveAction.selectBackupFile(parent, title);
        if (file != null) {
            launcher.launchFile(parent, file);
        }
    }

}
