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

package net.sourceforge.processdash.team.sync;

import java.util.List;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.hier.HierarchyAlterer.HierarchyAlterationException;


public interface SyncWorker extends DataContext {

    List getNodesDeleted();

    List getNodesCompleted();

    List getDataChanged();

    String getOriginalPath(String path);

    List getOriginalPaths(List paths);

    boolean nodesWereRenamed();

    void addTemplate(String path, String templateID)
            throws HierarchyAlterationException;

    void deleteNode(String path) throws HierarchyAlterationException;

    void renameNode(String oldPath, String newPath)
            throws HierarchyAlterationException;

    boolean reorderNodes(String parentPath, List childNames)
            throws HierarchyAlterationException;

    void setTemplateId(String nodePath, String templateID)
            throws HierarchyAlterationException;

    void markLeafComplete(String path);

    boolean markLeafIncomplete(String path);

    void markPSPTaskComplete(String path);

    boolean markPSPTaskIncomplete(String path);

    void doPutValueForce(String name, SaveableData value);

    void setLastReverseSyncedValue(SaveableData d);

    void setLastReverseSyncedValue(double d);

}
