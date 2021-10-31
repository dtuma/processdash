// Copyright (C) 2008-2021 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * An {@link ImportDirectory} object that reads files directly from their
 * original source directory on the filesystem.
 */
public class LocalImportDirectory implements ImportDirectory {

    private File targetDirectory;

    protected LocalImportDirectory(File targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    public File getDirectory() {
        return targetDirectory;
    }

    public String getRemoteLocation() {
        return null;
    }

    public String getDescription() {
        return targetDirectory.getAbsolutePath();
    }

    public void validate() throws IOException {
        if (!targetDirectory.isDirectory())
            throw new FileNotFoundException(targetDirectory.getPath());
    }

    public void update() {}

}
