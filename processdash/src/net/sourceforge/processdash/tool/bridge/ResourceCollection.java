// Copyright (C) 2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.bridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ResourceCollection extends ResourceCollectionInfo {

    /**
     * Return an input stream that can be used to read the contents of a given
     * resource.
     * 
     * @param resourceName
     *                the name of a resource
     * @return an <code>InputStream</code> for reading the contents of the
     *         named resource
     * @throws IOException
     *                 if the named resource does not exist, can not be read, or
     *                 if any other IO error occurs when reading the resource
     */
    public InputStream getInputStream(String resourceName) throws IOException;

    /**
     * Return an output stream that can be used to write data to a particular
     * resource.
     * 
     * @param resourceName
     *                the name of a resource
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
    public OutputStream getOutputStream(String resourceName) throws IOException;

    /**
     * Permanently remove a resource from this collection.
     * 
     * @param resourceName
     *                the name of the resource to delete
     */
    public void deleteResource(String resourceName);

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
