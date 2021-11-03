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

import net.sourceforge.processdash.util.lock.LockFailureException;

public interface ImportDirectory {

    /**
     * Returns a user-friendly description of this import location.
     * 
     * @return a user-friendly description of this import location.
     */
    public String getDescription();

    /**
     * Test to ensure that this directory is reachable, that it is showing live
     * (not cached) data, and that all data is up-to-date.
     * 
     * @since 2.6.5.2
     */
    public void validate() throws IOException;

    /**
     * Returns the directory on the filesystem where imported files are stored.
     * 
     * @return the directory on the filesystem where imported files are stored.
     */
    public File getDirectory();

    /**
     * Returns a string describing the remote source of this import directory,
     * or null if this import directory is not from a remote source.
     * 
     * @return If this import directory originates from a remote location, this
     *         will return a string describing that location. The resulting
     *         string would be suitable as a parameter to
     *         {@link ImportDirectoryFactory#get(String...)}, enabling the
     *         reconstruction of this object in the future. If this import
     *         directory does not originate from a remote location, this will
     *         return null.
     */
    public String getRemoteLocation();

    /**
     * Update the contents of this import directory if any data has changed.
     * 
     * @throws IOException
     *                 if we were unable to update the directory for some
     *                 reason
     */
    public void update() throws IOException;

    /**
     * Write data to a file in this import directory.
     * 
     * @param filename
     *            the name of the file within the directory
     * @param source
     *            the data that should be written to the file
     * @throws IOException
     *             if the data could not be written
     * @throws LockFailureException
     *             if a write lock would be required to write to the named file
     * 
     * @since 2.6.5.2
     */
    public void writeUnlockedFile(String filename, InputStream source)
            throws IOException, LockFailureException;

    /**
     * Delete a file from this import directory.
     * 
     * @param filename
     *            the name of the file within the directory
     * @throws IOException
     *             if the directory could not be reached
     * @throws LockFailureException
     *             if a write lock would be required to delete the named file
     * 
     * @since 2.6.5.2
     */
    public void deleteUnlockedFile(String filename)
            throws IOException, LockFailureException;

}
