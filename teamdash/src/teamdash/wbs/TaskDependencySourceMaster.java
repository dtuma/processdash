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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 * A source of dependency information for a team project that belongs to a
 * master project.
 * 
 * If this is a standalone team project, use
 * {@link teamdash.wbs.TaskDependencySourceMaster} instead.
 * 
 */
public class TaskDependencySourceMaster extends TaskDependencySourceAbstract
        implements TableModelListener {

    /** A bottom-up team project holding data from the other subprojects in
     * this master project. */
    private TeamProjectBottomUp bottomUpMaster;

    /** For efficiency, a map of nodeID -> WBSNode for nodes in the
     * bottom-up WBS */
    private Map masterNodeMap;

    /** The WBS of the project that is using this dependency source. */
    private WBSModel liveWbs;

    /** True if we have received a change event from the liveWBS since we
     * last refreshed our dependency tree. */
    private boolean liveWbsHasChanged;

    /** For efficiency, a map of nodeID -> WBSNode for nodes in the live WBS */
    private Map liveNodeMap;


    public TaskDependencySourceMaster(TeamProject teamProject) {
        super(new WBSModel(), teamProject.getProjectID());

        this.bottomUpMaster = new TeamProjectBottomUp(
                teamProject.getMasterProjectDirectory(),
                "Dependent Tasks", false, false,
                Collections.singleton(getProjectID()));
        this.masterNodeMap = new HashMap();

        this.liveWbs = teamProject.getWBS();
        this.liveWbs.addTableModelListener(this);
        this.liveWbsHasChanged = true;
        this.liveNodeMap = new HashMap();

        updateTaskTree();
    }

    public String getDisplayNameForNode(String nodeId) {
        if (nodeId.startsWith(getProjectID()))
            return getDisplayNameForNode(liveWbs, liveNodeMap, nodeId);
        else
            return getDisplayNameForNode(bottomUpMaster.getWBS(),
                    masterNodeMap, nodeId);
    }


    public void updateTaskTree() {
        if (maybeReloadMaster() || liveWbsHasChanged) {
            getTaskTree().copyFrom(bottomUpMaster.getWBS());
            MasterWBSUtil.mergeFromSubproject(liveWbs, getProjectID(), "", "",
                    Collections.EMPTY_LIST, false, getTaskTree());
            liveWbsHasChanged = false;
        }
    }

    private boolean maybeReloadMaster() {
        boolean result = bottomUpMaster.maybeReload();
        if (result)
            // whenever the master refreshes itself, it will discard and
            // recreate all the nodes in its WBS.  Our cache will no longer
            // be valid.
            masterNodeMap.clear();
        return result;
    }

    public void tableChanged(TableModelEvent e) {
        liveWbsHasChanged = true;
    }

}
