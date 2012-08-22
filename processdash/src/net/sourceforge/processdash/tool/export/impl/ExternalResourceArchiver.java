// Copyright (C) 2007-2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.impl;

import java.io.IOException;
import java.util.zip.ZipOutputStream;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.tool.export.mgr.ImportInstructionDispatcher;

public interface ExternalResourceArchiver extends ImportInstructionDispatcher {

    /**
     * Register the dashboard context that is in use.
     */
    public void setDashboardContext(DashboardContext ctx);

    /**
     * Archive external resources by adding them to a ZIP file.
     *
     * Before this method is called, all ImportInstructions will be dispatched
     * to this object, so it can make a note of the external data that needs
     * archiving.
     * 
     * @param out the ZIP file to write data to. This method will NOT close this
     *        stream (the caller created/opened the stream and must be
     *        responsible for closing it).
     */
    public void export(ZipOutputStream out) throws IOException;

}
