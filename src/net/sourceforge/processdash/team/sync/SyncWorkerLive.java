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

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.HierarchyAlterer.HierarchyAlterationException;

public class SyncWorkerLive extends AbstractSyncWorker {

    private DataRepository data;

    private net.sourceforge.processdash.hier.HierarchyAlterer alterer;

    public SyncWorkerLive(DataRepository data, List deletionPermissions,
            List completionPermissions) {
        this.data = data;
        this.alterer = DashController.getHierarchyAlterer();
        this.deletionPermissions = deletionPermissions;
        this.completionPermissions = completionPermissions;
    }

    protected void doAddTemplate(String path, String templateID)
            throws HierarchyAlterationException {
        alterer.addTemplate(path, templateID);
    }

    protected void doDeleteNode(String path)
            throws HierarchyAlterationException {

        // before deleting a node, set its estimated time to zero.  This is
        // useful when a phase is deleted;  since the phase does not have its
        // own datafile, deleting the node will not automatically delete the
        // old time estimate that might be present.
        doPutValue(DataRepository.createDataName(path, "Estimated Time"),
                ImmutableDoubleData.READ_ONLY_ZERO);

        // delete the node.
        alterer.deleteNode(path);
    }



    protected void doRenameNode(String oldPath, String newPath)
            throws HierarchyAlterationException {
        alterer.renameNode(oldPath, newPath);
    }

    public boolean reorderNodes(String parentPath, List childNames)
            throws HierarchyAlterationException {
        return alterer.reorderChildren(parentPath, childNames);
    }


    public void setTemplateId(String nodePath, String templateID)
            throws HierarchyAlterationException {
        alterer.setTemplateId(nodePath, templateID);
    }

    public SaveableData getValue(String name) {
        return data.getValue(name);
    }

    public SimpleData getSimpleValue(String name) {
        return data.getSimpleValue(name);
    }

    protected void doPutValue(String name, SaveableData value) {
        data.userPutValue(name, value);
    }


}
