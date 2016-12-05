// Copyright (C) 2013-2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.db;

import java.util.Collection;
import java.util.Set;

public interface StudyGroupManager {

    /**
     * Create a study group representing an arbitrary set of integers.
     * 
     * @param members
     *            the numeric items to include in the study group
     * @param groupIdentifier
     *            a unique name that we can assign to this study group. If
     *            another call arrives in the future with the same unique name,
     *            the old study group will be discarded as part of creating a
     *            group with the new contents.
     * @return the numeric key of a newly created study group which holds the
     *         given list of integers
     * @since TPDW 1.5.4
     */
    public int getGenericGroup(Set<Integer> members, String groupIdentifier);

    /**
     * Create a study group representing the keys for a particular group of data
     * blocks.
     * 
     * @param projectKeys
     *            the numeric keys of the projects whose plan items we care
     *            about
     * @param datasetIDs
     *            a list of process dashboard dataset IDs that generated the
     *            data blocks we're seeking
     * @param onBehalfOfDataName
     *            the name of the data element we are calculating that would
     *            like to know this value
     * @return the numeric key of a newly created study group which holds the
     *         database keys of the desired data blocks
     * @since TPDW 1.5.4.1
     */
    public int getDataBlockGroup(Collection<Integer> projectKeys,
            Set<String> datasetIDs, String onBehalfOfDataName);

    /**
     * Create a study group representing the keys for a particular group of plan
     * items.
     * 
     * @param projectKeys
     *            the numeric keys of the projects whose plan items we care
     *            about
     * @param taskIDs
     *            a list of process dashboard EV Task IDs for the plan items
     *            we're seeking
     * @param includeNonWbsChildren
     *            true if we should include plan items that only appear in
     *            personal plans (not the WBS) that are children of the matching
     *            items.
     * @param onBehalfOfDataName
     *            the name of the data element we are calculating that would
     *            like to know this value
     * @return the numeric key of a newly created study group which holds the
     *         database keys of the desired plan items
     */
    public int getPlanItemGroup(Collection<Integer> projectKeys,
            Set<String> taskIDs, boolean includeNonWbsChildren,
            String onBehalfOfDataName);

}
