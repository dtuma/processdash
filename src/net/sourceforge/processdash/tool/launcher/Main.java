// Copyright (C) 2018-2021 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.launcher;

import net.sourceforge.processdash.tool.bridge.impl.TLSConfig;
import net.sourceforge.processdash.tool.launcher.macosx.LauncherMacOSX;
import net.sourceforge.processdash.tool.launcher.pdes.PDESMain;
import net.sourceforge.processdash.ui.LookAndFeelUtil;

public class Main {

    public static void main(String[] args) {
        // configure TLS settings for the current computer
        TLSConfig.autoConfigure();

        // check to see if we were launched to open a specific file or files
        if (LauncherMacOSX.setupOnMacAndCheckForStartupFileOpen())
            return;
        else if (Launcher.launch(args))
            return;

        // if we weren't launched to open specific files, display the GUI
        LookAndFeelUtil.setDefaultLAF();
        PDESMain.main(args);
    }

}
