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
import java.net.URL;

import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollection;
import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollectionStrategy;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.lock.LockFailureException;

/**
 * An {@link ImportDirectory} object that retrieves files from a Team Server
 * and caches them locally for quick access.
 */
public class BridgedImportDirectory implements ImportDirectory {

    protected String remoteURL;

    protected File importDirectory;

    protected ResourceBridgeClient client;

    protected long lastUpdateTime;

    protected BridgedImportDirectory(String remoteURL,
            FileResourceCollectionStrategy strategy) throws IOException {
        this.remoteURL = remoteURL;
        this.importDirectory = getCacheDirectoryForBridgedImport(remoteURL);
        this.importDirectory.mkdirs();

        FileResourceCollection localCollection = new FileResourceCollection(
                importDirectory, false);
        localCollection.setStrategy(strategy);
        this.client = new ResourceBridgeClient(localCollection, remoteURL, null);

        this.client.syncDown();
        this.lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * When an import directory originates from a remote location, it is cached
     * locally for quick access. This method returns the canonical location
     * where files should be cached for a particular URL.
     */
    protected static File getCacheDirectoryForBridgedImport(String remoteURL) {
        return new File(DirectoryPreferences.getMasterImportDirectory(),
                getImportId(remoteURL));
    }

    protected static String getImportId(String remoteURL) {
        String url = remoteURL;
        if (url.startsWith("https"))
            url = "http" + url.substring(5);
        return FileUtils.makeSafeIdentifier(url);
    }

    public String getDescription() {
        return remoteURL;
    }

    public File getDirectory() {
        return importDirectory;
    }

    public String getRemoteLocation() {
        return remoteURL;
    }

    public void validate() throws IOException {
        // syncDown will return true if changes were made, false if none were
        // needed. Give it three chances to sync before giving up. If the server
        // is unreachable, this will throw an IOException.
        if (client.syncDown() && client.syncDown() && client.syncDown())
            throw new IOException("Unable to sync down");
        lastUpdateTime = System.currentTimeMillis();
    }

    public void update() throws IOException {
        // this method may get called overzealously by code in different layers
        // of the application.  If it is called more than once within a few
        // milliseconds, don't repeat the update.
        long now = System.currentTimeMillis();
        long lastUpdateAge = now - lastUpdateTime;
        if (lastUpdateAge > 1000 || lastUpdateAge < 0) {
            client.syncDown();
            lastUpdateTime = System.currentTimeMillis();
        }
    }

    public void writeUnlockedFile(String filename, InputStream source)
            throws IOException, LockFailureException {
        ResourceBridgeClient.uploadSingleFile(new URL(remoteURL), filename,
            source);
        update();
    }

    public void deleteUnlockedFile(String filename)
            throws IOException, LockFailureException {
        ResourceBridgeClient.deleteSingleFile(new URL(remoteURL), filename);
        update();
    }

}
