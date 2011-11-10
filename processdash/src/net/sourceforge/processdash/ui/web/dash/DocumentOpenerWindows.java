// Copyright (C) 2001-2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.dash;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DocumentOpenerWindows implements DocumentOpener {

    private static final Logger log = Logger
            .getLogger(DocumentOpenerWindows.class.getName());

    public DocumentOpenerWindows() {
        if (System.getProperty("os.name").indexOf("Windows") == -1)
            throw new UnsupportedOperationException();
    }

    public boolean openDocument(File doc) {
        String[] cmdLine = new String[] { "rundll32",
                "url.dll,FileProtocolHandler", doc.getPath() };
        try {
            Runtime.getRuntime().exec(cmdLine);
            return true;
        } catch (IOException e) {
            log.log(Level.WARNING, "Unable to open file '" + doc.getPath()
                    + "'", e);
            return false;
        }
    }

}
