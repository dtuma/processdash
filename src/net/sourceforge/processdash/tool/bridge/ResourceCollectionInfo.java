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

package net.sourceforge.processdash.tool.bridge;

import java.util.Collections;
import java.util.List;

public interface ResourceCollectionInfo {

    /**
     * Get a list of the resources known to this collection.
     */
    public List<String> listResourceNames();

    /**
     * Return the timestamp when the named resource was last modified.
     * 
     * @param resourceName
     *                the name of a resource
     * @return the timestamp when the named resource was last modified, or 0 if
     *         no such resource is known to this object.
     */
    public long getLastModified(String resourceName);

    /**
     * Return a checksum of the content of this resource
     * 
     * @param resourceName
     *                the name of a resource
     * @return the checksum of the named resource, or null if the resource does
     *         not exist or could not be read
     */
    public Long getChecksum(String resourceName);

    /**
     * An object that represents a read-only, empty resource collection
     */
    public ResourceCollectionInfo EMPTY_COLLECTION = //
            new ResourceCollectionInfo() {
                public List<String> listResourceNames() {
                    return Collections.EMPTY_LIST;
                }
                public long getLastModified(String resourceName) {
                    return 0;
                }
                public Long getChecksum(String resourceName) {
                    return null;
                }
            };

}
