// Copyright (C) 2006-2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.net.cms;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.util.RobustFileOutputStream;

/** Simple persistence service which saves files into a directory on
 * the filesystem.
 */
public class FilePersistenceService implements PersistenceService {

    private String qualifier;

    private File baseDirectory;

    public FilePersistenceService(String qualifier, File baseDirectory) {
        this.qualifier = qualifier;
        this.baseDirectory = baseDirectory;
    }

    public InputStream open(String filename) throws IOException {
        File f = getFile(filename);
        if (f.exists())
            return new FileInputStream(f);
        else
            return null;
    }

    public OutputStream save(String qualifier, String filename)
            throws IOException {
        if (Settings.isReadOnly())
            return null;

        // see if our qualifier matches the specified qualifier.
        if (this.qualifier != null
                && !this.qualifier.equalsIgnoreCase(qualifier))
            return null;

        // if our target directory does not exist and cannot be created,
        // abort.
        if (!baseDirectory.isDirectory() && !baseDirectory.mkdirs())
            return null;

        File f = getFile(filename);
        RobustFileOutputStream out = new RobustFileOutputStream(f);
        return out;
    }

    private File getFile(String filename) {
        return new File(baseDirectory, filename);
    }

}
