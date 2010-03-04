// Copyright (C) 2002-2010 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.wbs;

/**
 * An interface for providing information about tasks that a project can list as
 * its dependencies.
 */
public interface TaskDependencySource extends TaskIDSource {

    public static final String UNKNOWN_NODE_DISPLAY_NAME = "?????";

    /**
     * Return a string that can be displayed to the user to represent the
     * dependent task with the given ID.
     * 
     * If no task can be found with the given ID, returns
     * {@link #UNKNOWN_NODE_DISPLAY_NAME}.
     */
    public String getDisplayNameForNode(String nodeId);

    /**
     * Return a tree of tasks which can be listed as dependencies.
     * 
     * The resulting object may not be up-to-date until the
     * {@link #updateTaskTree()} method is called.
     */
    public WBSModel getTaskTree();

    /**
     * Make certain that the tree returned by {@link #getTaskTree()} is as
     * up-to-date as possible.
     */
    public void updateTaskTree();

}
