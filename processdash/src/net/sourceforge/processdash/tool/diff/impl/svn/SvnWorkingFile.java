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

package net.sourceforge.processdash.tool.diff.impl.svn;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.sourceforge.processdash.util.TimedInputStream;

public class SvnWorkingFile implements SvnFileVersion, SvnTask {

    private String relativePath;

    private SvnTaskHelper helper;

    private File file;

    public SvnWorkingFile(String relativePath) {
        this.relativePath = relativePath;
        this.helper = new SvnTaskHelper(this);
    }

    public String getRevision() {
        return SvnFile.resources.getString("Report.Locally_Modified");
    }

    public InputStream getContents() throws IOException {
        helper.waitTillReady();
        return new TimedInputStream(new FileInputStream(file));
    }

    public SvnTaskHelper getTaskHelper() {
        return helper;
    }

    public void execute(SvnExecutor svn) throws Exception {
        this.file = new File(svn.getBaseDirectory(), relativePath);
    }

}
