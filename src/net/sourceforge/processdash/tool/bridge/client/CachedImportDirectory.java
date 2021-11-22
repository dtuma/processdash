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
import java.io.IOException;
import java.io.InputStream;

/**
 * Other ImportDirectory implementations copy files in a collection to a local
 * directory for quick access.  This class provides access to those locally
 * cached files, even when no network is available.
 */
public class CachedImportDirectory implements ImportDirectory {

    protected String location;

    protected String remoteURL;

    protected File importDirectory;

    protected CachedImportDirectory(String location, String remoteURL) {
        this.location = location;
        this.remoteURL = remoteURL;
        this.importDirectory = BridgedImportDirectory
                .getCacheDirectoryForBridgedImport(location);
    }

    public String getDescription() {
        return location;
    }

    public File getDirectory() {
        return importDirectory;
    }

    public String getRemoteLocation() {
        return remoteURL;
    }

    public Boolean isBadDelegate() {
        // If we're using cached files on our hard drive, we always want to
        // see if a better option is available.
        return Boolean.TRUE;
    }

    public void validate() throws IOException {
        throw new IOException("Using cached data for " + location);
    }

    public void update() {}

    public void writeUnlockedFile(String filename, InputStream source)
            throws IOException {
        throw new IOException("Unable to reach " + location);
    }

    public void deleteUnlockedFile(String filename) throws IOException {
        throw new IOException("Unable to reach " + location);
    }

}
