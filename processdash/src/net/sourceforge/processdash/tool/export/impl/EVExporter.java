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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
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
import java.io.OutputStream;
import java.util.Map;

public interface EVExporter {

    /**
     * Export earned value data to a stream using the given writer.
     * 
     * @param out
     *            the stream to write data to. This method will NOT close this
     *            stream (the caller created/opened the stream and must be
     *            responsible for closing it).
     * @param schedules
     *            a collection of the earned value schedules to export. The keys
     *            are the names of earned value schedules, and whose values are
     *            {@link net.sourceforge.processdash.ev.EVTaskList}
     */
    public void export(OutputStream out, Map schedules) throws IOException;

}
