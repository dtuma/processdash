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

package net.sourceforge.processdash.tool.diff.impl.svn;

import java.io.IOException;
import java.io.InputStream;

public class SvnEmptyFile implements SvnFileVersion {

    private String revision;

    public SvnEmptyFile(String revision) {
        this.revision = revision;
    }

    public String getRevision() {
        return revision;
    }

    public InputStream getContents() throws IOException {
        return null;
    }

    /**
     * Reusable object representing the state of a file before it has been added
     * to version control.
     */
    public static final SvnEmptyFile ADDED = new SvnEmptyFile(SvnFile.resources
            .getString("Report.Added"));

    /**
     * Reusable object representing the state of a file after it has been
     * deleted from version control.
     */
    public static final SvnEmptyFile DELETED = new SvnEmptyFile(
            SvnFile.resources.getString("Report.Deleted"));

}
