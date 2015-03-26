// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ResourceCollection extends ReadableResourceCollection {

    /**
     * Return an output stream that can be used to write data to a particular
     * resource.
     * 
     * @param resourceName
     *                the name of a resource
     * @param modTime
     *                the desired modification timestamp for the resource; can
     *                be 0 to indicate the current time
     * 
     * @return an <code>OutputStream</code> for writing data to the named
     *         resource. Note that data written to this stream is not guaranteed
     *         to be committed until the stream is closed, and
     *         {@link #flushOutput()} is called.
     * 
     * @throws IOException
     *                 if the named resource can not be created/written, or if
     *                 any other IO error occurs when opening the resource
     */
    public OutputStream getOutputStream(String resourceName, long modTime)
            throws IOException;

    /**
     * Permanently remove a resource from this collection.
     * 
     * @param resourceName
     *                the name of the resource to delete
     */
    public void deleteResource(String resourceName);

    /**
     * Returns true if a lock should be obtained before modifying or deleting a
     * particular resource.
     * 
     * @param resourceName
     *                the name of a resource in the collection
     * @return true if a lock must be obtained before modifying or deleting the
     *         named resource
     */
    public boolean requiresWriteLock(String resourceName);

    /**
     * Return an object that can be used to obtain a lock on this collection.
     */
    public Object getLockTarget();

    /**
     * Make a backup of the data in this collection.
     * 
     * @param backupQualifier
     *                a compact, human-readable string that might be used to
     *                describe the occasion for making this backup
     */
    public void backupCollection(String backupQualifier) throws IOException;

    /**
     * Return a stream containing the contents of the most recently made backup
     * for this collection.
     * 
     * @return a stream in ZIP format. The caller must close this stream after
     *         reading it.
     * @throws IOException
     *                 if the most recent backup could not be read, or if no
     *                 recent backup could be found
     */
    public InputStream getBackupInputStream() throws IOException;

}
