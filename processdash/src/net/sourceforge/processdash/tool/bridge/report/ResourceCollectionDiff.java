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

package net.sourceforge.processdash.tool.bridge.report;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.processdash.tool.bridge.ResourceCollectionInfo;

/**
 * Compares two resource collections and finds the differences.
 */
public class ResourceCollectionDiff {

    private ResourceCollectionInfo collectionA;

    private ResourceCollectionInfo collectionB;

    private List<String> onlyInA;

    private List<String> onlyInB;

    private List<String> matching;

    private List<String> differing;

    /**
     * Compare two resource collections.
     * 
     * @param a
     *                the first {@link ResourceCollectionInfo} to compare
     * @param b
     *                the second {@link ResourceCollectionInfo} to compare
     */
    public ResourceCollectionDiff(ResourceCollectionInfo a,
            ResourceCollectionInfo b) {
        collectionA = a;
        collectionB = b;
        onlyInA = new ArrayList<String>();
        onlyInB = new ArrayList<String>();
        matching = new ArrayList<String>();
        differing = new ArrayList<String>();

        onlyInA.addAll(a.listResourceNames());

        for (String resourceName : b.listResourceNames()) {
            Long aSum = a.getChecksum(resourceName);
            if (aSum == null) {
                onlyInB.add(resourceName);
            } else {
                onlyInA.remove(resourceName);
                Long bSum = b.getChecksum(resourceName);
                if (aSum.equals(bSum))
                    matching.add(resourceName);
                else
                    differing.add(resourceName);
            }
        }

    }

    /**
     * @return the first {@link ResourceCollectionInfo} that was used in the
     *         comparison
     */
    public ResourceCollectionInfo getA() {
        return collectionA;
    }

    /**
     * @return the second {@link ResourceCollectionInfo} that was used in the
     *         comparison
     */
    public ResourceCollectionInfo getB() {
        return collectionB;
    }

    /**
     * @return a list of resource names that were only found in the first
     *         resource collection
     */
    public List<String> getOnlyInA() {
        return onlyInA;
    }

    /**
     * @return a list of resource names that were only found in the second
     *         resource collection
     */
    public List<String> getOnlyInB() {
        return onlyInB;
    }

    /**
     * @return a list of resource names that were found in both resource
     *         collections, and whose checksums match
     */
    public List<String> getMatching() {
        return matching;
    }

    /**
     * @return a list of resource names that were found in both resource
     *         collections, and whose checksums differ
     */
    public List<String> getDiffering() {
        return differing;
    }

    /**
     * @return true if the two resource collections appear to have the same
     *         contents
     */
    public boolean noDifferencesFound() {
        return onlyInA.isEmpty() && onlyInB.isEmpty() && differing.isEmpty();
    }

}
