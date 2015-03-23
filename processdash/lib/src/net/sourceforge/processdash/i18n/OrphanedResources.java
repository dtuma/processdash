// Copyright (C) 2003 Tuma Solutions, LLC
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

package net.sourceforge.processdash.i18n;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;

public class OrphanedResources extends MatchingTask {

    private static final String PROP_FILE_SUFFIX = ".properties";
    private int numOrphans;
    private File dir;

    public void setDir(File d) {
        dir = d;
    }

    public void execute() throws BuildException {
        DirectoryScanner ds = getDirectoryScanner(dir);
        String[] srcFiles = ds.getIncludedFiles();
        HashMap props = new HashMap();
        for (int j = 0; j < srcFiles.length; j++) {
            String key = getFileKey(srcFiles[j]);
            if (key != null)
                props.put(key, loadProps(srcFiles[j]));
        }

        numOrphans = 0;
        Iterator i = props.keySet().iterator();
        while (i.hasNext()) {
            String key = (String) i.next();
            checkForOrphans(key, props);
        }

        if (numOrphans > 0)
            throw new BuildException(
                numOrphans + " orphaned resources found.");
    }

    private String getFileKey(String filename) {
        if (filename.endsWith(PROP_FILE_SUFFIX))
            return filename.substring(
                0,
                filename.length() - PROP_FILE_SUFFIX.length());
        else
            return null;
    }

    private Object loadProps(String filename) {
        try {
            File f = new File(fileset.getDir(getProject()), filename);
            Properties p = new Properties();
            p.load(new FileInputStream(f));
            return p;
        } catch (IOException e) {
            throw new BuildException("File unexpectedly not found");
        }
    }

    private void checkForOrphans(String key, HashMap props) {
        String baseName = getBaseName(key);
        if (baseName == null)
            return;

        Properties p = (Properties) props.get(key);
        Properties b = (Properties) props.get(baseName);
        Iterator i = p.keySet().iterator();
        while (i.hasNext()) {
            Object res = i.next();
            if (b == null || !b.containsKey(res))
                printError(key, res);
        }
    }

    private void printError(String key, Object res) {
        String msg =
            "File '" + key + "', contains orphaned resource '" + res + "'.";
        System.out.println(msg);
        numOrphans++;
    }

    private String getBaseName(String key) {
        int pos = key.indexOf('_');
        if (pos == -1)
            return null;
        return key.substring(0, pos);
    }
}
