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

}
