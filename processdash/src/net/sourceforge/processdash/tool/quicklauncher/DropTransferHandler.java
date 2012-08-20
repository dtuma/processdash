// Copyright (C) 2006-2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.quicklauncher;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import net.sourceforge.processdash.ui.lib.TransferHandlerUtils;
import net.sourceforge.processdash.util.LightweightSet;

public class DropTransferHandler extends TransferHandler {

    QuickLauncher launcher;

    InstanceLauncherFactory launcherFactory;

    public DropTransferHandler(QuickLauncher launcher,
            InstanceLauncherFactory launcherFactory) {
        this.launcher = launcher;
        this.launcherFactory = launcherFactory;
    }

    public boolean canImport(JComponent comp, DataFlavor[] flavors) {
        return TransferHandlerUtils.hasFileListFlavor(flavors);
    }

    public boolean importData(JComponent comp, Transferable t) {
        List files = TransferHandlerUtils.getTransferredFileList(t);
        if (files == null || files.isEmpty())
            return false;

        Set targets = new LightweightSet();
        for (Iterator i = files.iterator(); i.hasNext();) {
            File f = (File) i.next();

            if ("pspdash.jar".equals(f.getName())) {
                launcher.useDashboardJarFile(f);
                continue;
            }

            DashboardInstance l = launcherFactory.getLauncher(comp, f);
            if (l != null)
                targets.add(l);
        }

        launcher.launchInstances(targets);

        return true;
    }
}
