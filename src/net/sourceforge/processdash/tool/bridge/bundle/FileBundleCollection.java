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

package net.sourceforge.processdash.tool.bridge.bundle;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.sourceforge.processdash.tool.bridge.ReadableResourceCollection;
import net.sourceforge.processdash.tool.bridge.ResourceCollectionInfo;


/**
 * An object that can read file data from a bundle without extracting it.
 */
public class FileBundleCollection
        implements ReadableResourceCollection, Closeable {

    private FileBundleManifest manifest;

    private ResourceCollectionInfo files;

    private File zipFile;

    private ZipFile bundleZip;

    public FileBundleCollection(FileBundleManifest manifest, File zipFile) {
        this.manifest = manifest;
        this.files = manifest.getFiles();
        this.zipFile = zipFile;
    }

    public FileBundleID getBundleID() {
        return manifest.getBundleID();
    }

    public FileBundleManifest getManifest() {
        return manifest;
    }

    public List<String> listResourceNames() {
        return files.listResourceNames();
    }

    public long getLastModified(String resourceName) {
        return files.getLastModified(resourceName);
    }

    public Long getChecksum(String resourceName) {
        return files.getChecksum(resourceName);
    }

    /**
     * Read a file from within this bundle.
     * 
     * Calls to this method should be followed, at some later time, by a call to
     * {@link #close()}.
     */
    public synchronized InputStream getInputStream(String resourceName)
            throws IOException {
        if (bundleZip == null)
            bundleZip = new ZipFile(zipFile);

        ZipEntry e = bundleZip.getEntry(resourceName);
        if (e == null)
            throw new FileNotFoundException(resourceName);

        return bundleZip.getInputStream(e);
    }

    /**
     * Close resources opened by previous calls to
     * {@link #getInputStream(String)}
     */
    public synchronized void close() throws IOException {
        if (bundleZip != null) {
            bundleZip.close();
            bundleZip = null;
        }
    }

    public String getDescription() {
        return "FileBundleCollection[" + zipFile + "]";
    }

}
