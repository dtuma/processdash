// Copyright (C) 2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.diff.engine;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * A file object which is to be analyzed and counted.
 */
public interface FileToAnalyze {

    /**
     * The name of the file
     */
    public String getFilename();

    /**
     * A list of objects representing versions of the file.
     * 
     * The first object in the list should represent the initial state of the
     * file. The second object (if present) represents a change that should be
     * counted and analyzed.  The third object (if present) represents a
     * subsequent change that should not be counted.  Additional objects
     * continue this alternating pattern.
     */
    public List getVersions();

    /**
     * Retrieve the contents of this file for a particular version.
     */
    public InputStream getContents(Object version) throws IOException;

}
