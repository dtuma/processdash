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
import java.util.logging.Logger;

import net.sourceforge.processdash.util.lock.LockFailureException;

/**
 * An import directory that dynamically recomputes the location of source data.
 * 
 * There are three primary ways of accessing imported data:
 * 
 * <ol>
 * <li>Accessing the files in the target directory through the filesystem
 *     (using a {@link LocalImportDirectory})</li>
 * <li>Accessing the files through a team server (using a
 *     {@link BridgedImportDirectory})</li>
 * <li>Accessing the files on the local hard drive that were left behind by a
 *     previously operational bridged import directory (this access is provided
 *     by a {@link CachedImportDirectory})</li>
 * </ol>
 * 
 * This class attempts to switch dynamically between these three options in
 * response to changing network connectivity.
 */
public class DynamicImportDirectory implements ImportDirectory {

    /** The list of locations we can use to search for import data */
    private String[] locations;

    /** The currently active ImportDirectory <b>(never null)</b> */
    private ImportDirectory delegate;

    /** The time when we last checked for the need to replace the delegate */
    private long lastUpdateDelegateTime;


    private static final Logger logger = Logger
            .getLogger(DynamicImportDirectory.class.getName());


    public DynamicImportDirectory(String[] locations) throws IOException {
        this.locations = locations;
        maybeUpdateDelegate();
        if (delegate == null)
            throw new IOException();
    }

    public String getDescription() {
        return delegate.getDescription();
    }

    public File getDirectory() {
        return delegate.getDirectory();
    }

    public String getRemoteLocation() {
        return delegate.getRemoteLocation();
    }

    public void validate() throws IOException {
        recheckDelegate();
        delegate.validate();
    }

    public void update() throws IOException {
        maybeUpdateDelegate();
        delegate.update();
    }

    public boolean needsCacheUpdate() {
        if (delegate instanceof CachedImportDirectory) {
            lastUpdateDelegateTime = -1;
            return true;
        } else {
            return false;
        }
    }

    public void writeUnlockedFile(String filename, InputStream source)
            throws IOException, LockFailureException {
        recheckDelegate();
        delegate.writeUnlockedFile(filename, source);
    }

    public void deleteUnlockedFile(String filename)
            throws IOException, LockFailureException {
        recheckDelegate();
        delegate.deleteUnlockedFile(filename);
    }

    private void recheckDelegate() {
        lastUpdateDelegateTime = RECHECK_LOCAL_DIRECTORY;
        maybeUpdateDelegate();
    }

    private void maybeUpdateDelegate() {
        if (isUpdateDelegateNeeded()) {
            ImportDirectory newDelegate = ImportDirectoryFactory.getInstance()
                    .getImpl(locations);
            lastUpdateDelegateTime = System.currentTimeMillis();

            if (newDelegate != null && newDelegate != delegate) {
                this.delegate = newDelegate;

                String type = delegate.getClass().getSimpleName();
                String path = delegate.getDirectory().getPath();
                logger.fine("Using " + type + " " + path);
            }
        }
    }

    private boolean isUpdateDelegateNeeded() {
        // if we have no delegate, we need to update!
        if (delegate == null)
            return true;

        // this method may get called overzealously by code in different
        // layers of the application.  If it is called more than once within a
        // few milliseconds, don't repeat the update.
        long now = System.currentTimeMillis();
        long lastUpdateDelegateAge = now - lastUpdateDelegateTime;
        if (lastUpdateDelegateAge > 0 && lastUpdateDelegateAge < 1000)
            return false;

        // Our main preference is a BridgedImportDirectory. If we already
        // have one, stick with it. Note: the implication of this decision
        // is that we will never switch from a bridged directory back to a
        // local directory. But the need to do that is rare: it would only
        // be useful if the Team Server shut down while we were running.
        if (delegate instanceof BridgedImportDirectory
                || delegate instanceof BridgedImportSubdirectory)
            return false;

        // If we're using cached files on our hard drive, we always want to
        // see if a better option is available.
        if (delegate instanceof CachedImportDirectory)
            return true;

        // check update conditions when using a local import directory
        if (delegate instanceof LocalImportDirectory
                || delegate instanceof CachingLocalImportDirectory) {
            // when requested, recheck the contents of the local directory (for
            // example, to see if it's been migrated to a server)
            if (lastUpdateDelegateTime == RECHECK_LOCAL_DIRECTORY)
                return true;

            // if the local directory exists, no need to recheck
            if (delegate.getDirectory().isDirectory())
                return false;

            // if the local directory doesn't exist, consider a recheck if
            // (a) there is more than one location in our "locations" list,
            //     allowing the chance that a different location might better,
            // (b) a default team server is in effect, leading to the
            //     possibility that we might be able to access the location
            //     through an implicitly constructed URL.
            return (locations.length > 1
                    || TeamServerSelector.isDefaultTeamServerConfigured());
        }

        return false;
    }

    private static final int RECHECK_LOCAL_DIRECTORY = -2;

}
