// Copyright (C) 2022 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.merge;

import java.io.IOException;

import net.sourceforge.processdash.tool.bridge.ReadableResourceCollection;
import net.sourceforge.processdash.tool.bridge.ResourceCollection;

public interface DashboardFileMergeHandler {

    /**
     * Perform a three-way merge of a single file.
     * 
     * @param filename
     *            the name of the file to merge
     * @param parent
     *            a resource collection that can provide the contents of the
     *            file before it was changed.
     * @param first
     *            a resource collection that can provide the contents of the
     *            file after the first client changed it.
     * @param second
     *            a resource collection that can provide the contents of the
     *            file after the second client changed it.
     * @param dest
     *            a resource collection where the merged contents should be
     *            written.
     * @throws IOException
     *             if any errors occurred during the merge
     */
    public void mergeFile(String filename, //
            ReadableResourceCollection parent, ReadableResourceCollection first,
            ReadableResourceCollection second, ResourceCollection dest)
            throws IOException;

}
