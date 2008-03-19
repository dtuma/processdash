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

package net.sourceforge.processdash.tool.bridge.report;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;

import net.sourceforge.processdash.tool.bridge.ResourceCollection;
import net.sourceforge.processdash.tool.bridge.ResourceCollectionInfo;
import net.sourceforge.processdash.tool.bridge.ResourceFilter;


public class ListingHashcodeCalculator {

    /**
     * Computes a hashcode for the items in a resource collection.
     * 
     * The hashcode is sensitive to:
     * <ul>
     * <li>The names of the resources that appear in the collection and are
     * included by the given filter</li>
     * <li>The checksums of those files in the resource collection</li>
     * </ul>
     * 
     * Thus, if the list of resources in the collection changes, or if the
     * contents of a resource change, this hashcode is likely to change as well.
     * 
     * This method is analogous to asking the filter for a list of included
     * resource names, then passing those names to
     * {@link #getListingHashcode(ResourceCollection, List)}.
     * 
     * @param collection
     *                the resource collection to examine
     * @param filter
     *                a filter to apply to the resource collection
     * @return a hashcode for the names and checksums of the filtered files
     */
    public static long getListingHashcode(ResourceCollectionInfo collection,
            ResourceFilter filter) {
        List<String> resourceNames = filter.filterCollection(collection);
        return getListingHashcode(collection, resourceNames);
    }

    /**
     * Computes a hashcode for the items in a resource collection.
     * 
     * The hashcode is sensitive to:
     * <ul>
     * <li>The names of the resources that appear both in the collection and in
     * the <code>resourceNames</code> parameter</li>
     * <li>The checksums of those files in the resource collection</li>
     * </ul>
     * 
     * Thus, if the list of resources in the collection changes, or if the
     * contents of a resource change, this hashcode is likely to change as well.
     * 
     * @param collection
     *                the resource collection to examine
     * @param resourceNames
     *                the names of resources to include in the hash
     * @return a hashcode for the names and checksums of the listed files
     */
    public static long getListingHashcode(ResourceCollectionInfo collection,
            List<String> resourceNames) {
        String[] sortedNames = new String[resourceNames.size()];
        for (int i = 0; i < sortedNames.length; i++) {
            sortedNames[i] = resourceNames.get(i).toLowerCase();
        }
        Arrays.sort(sortedNames);

        Adler32 cksum = new Adler32();
        try {
            DataOutputStream out = new DataOutputStream(
                    new CheckedOutputStream(NULL_OUT, cksum));

            for (String resourceName : sortedNames) {
                long lastMod = collection.getLastModified(resourceName);
                if (lastMod < 1)
                    continue;

                Long checksum = collection.getChecksum(resourceName);
                if (checksum == null)
                    continue;

                out.writeUTF(resourceName);
                out.writeLong(checksum);
            }
        } catch (IOException e) {
            // can't happen
        }

        return cksum.getValue();
    }


    private static class NullOutputStream extends OutputStream {
        @Override
        public void write(int b) {}
    }

    private static final OutputStream NULL_OUT = new NullOutputStream();

}
