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

import java.util.HashMap;
import java.util.Map;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 * A source of dependency information for a standalone team project.
 * 
 * If the project is part of a master project, use
 * {@link teamdash.wbs.TaskDependencySourceMaster} instead.
 * 
 */
public class TaskDependencySourceSimple extends TaskDependencySourceAbstract
        implements TableModelListener {

    /** The WBS of the project that is using this dependency source. */
    private WBSModel liveWbs;

    /** True if we have received a change event from the liveWBS since we
     * last refreshed our dependency tree. */
    private boolean liveWbsHasChanged;

    /** For efficiency, a map of nodeID -> WBSNode for nodes in the wbs */
    private Map nodeCache;

    public TaskDependencySourceSimple(TeamProject project) {
        super(new WBSModel(), project.getProjectID());

        this.liveWbs = project.getWBS();
        this.liveWbs.addTableModelListener(this);
        this.liveWbsHasChanged = true;
        this.nodeCache = new HashMap();

        updateTaskTree();
    }

    public String getDisplayNameForNode(String nodeId) {
        return getDisplayNameForNode(liveWbs, nodeCache, nodeId);
    }

    public void updateTaskTree() {
        if (liveWbsHasChanged) {
            getTaskTree().copyFrom(liveWbs);
            liveWbsHasChanged = false;
        }
    }

    public void tableChanged(TableModelEvent e) {
        liveWbsHasChanged = true;
    }
}
