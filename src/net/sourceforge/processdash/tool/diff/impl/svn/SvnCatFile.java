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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import net.sourceforge.processdash.util.Disposable;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.TempFileFactory;

public abstract class SvnCatFile implements SvnFileVersion, SvnTask, Disposable {

    private List<String> catArgs;

    private File tmpFile;

    protected SvnTaskHelper helper;

    public SvnCatFile(String... catArgs) {
        this.catArgs = Arrays.asList(catArgs);
        this.helper = new SvnTaskHelper(this);
    }

    public InputStream getContents() throws IOException {
        helper.waitTillReady();
        return new BufferedInputStream(new FileInputStream(tmpFile));
    }

    public void dispose() {
        if (tmpFile != null)
            tmpFile.delete();
    }

    public SvnTaskHelper getTaskHelper() {
        return helper;
    }

    public void execute(SvnExecutor svn) throws Exception {
        File result = TempFileFactory.get().createTempFile("svnLocDiffFile",
            ".tmp");
        InputStream in = svn.exec("cat", catArgs);
        OutputStream out = new FileOutputStream(result);
        FileUtils.copyFile(in, out);
        in.close();
        out.close();

        this.tmpFile = result;
    }

}
