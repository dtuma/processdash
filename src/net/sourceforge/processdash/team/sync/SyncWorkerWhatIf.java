// Copyright (C) 2002-2025 Tuma Solutions, LLC
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.HierarchyAlterer;
import net.sourceforge.processdash.hier.HierarchyAlterer.HierarchyAlterationException;

public class SyncWorkerWhatIf extends AbstractSyncWorker {

    private DataContext data;
    private DashHierarchy hier;
    private boolean allowForceData;

    private Map localData = new HashMap();

    public SyncWorkerWhatIf(DataContext data, DashHierarchy hier) {
        this.data = data;
        this.hier = hier;
        this.allowForceData = true;
    }

    public SyncWorkerWhatIf makeSafe() {
        allowForceData = false;
        return this;
    }

    protected void doAddTemplate(String path, String templateID)
            throws HierarchyAlterationException {
        HierarchyAlterer.doAddNode(hier, path);
        HierarchyAlterer.doSetTemplateId(hier, path, templateID, "");
    }

    protected void doDeleteNode(String path)
            throws HierarchyAlterationException {
        HierarchyAlterer.doDeleteNode(hier, path);
    }

    protected void doRenameNode(String oldPath, String newPath)
            throws HierarchyAlterationException {
        HierarchyAlterer.doRenameNode(hier, oldPath, newPath);
    }

    public boolean reorderNodes(String parentPath, List childNames)
            throws HierarchyAlterationException {
        return HierarchyAlterer.doReorderChildren(hier, parentPath, childNames);
    }

    public SaveableData getValue(String name) {
        name = getOriginalPath(name);
        SaveableData result = (SaveableData) localData.get(name);
        if (result != null)
            return result;
        else
            return data.getValue(name);
    }

    public SimpleData getSimpleValue(String name) {
        name = getOriginalPath(name);
        SaveableData result = (SaveableData) localData.get(name);
        if (result != null)
            return result.getSimpleValue();
        else
            return data.getSimpleValue(name);
    }

    protected void doPutValue(String name, SaveableData value) {
        name = getOriginalPath(name);
        localData.put(name, value);
    }

    public void doPutValueForce(String name, SaveableData value) {
        SaveableData currVal = getValue(name);
        name = getOriginalPath(name);
        if (allowForceData)
            data.putValue(name, value);
        else {
            localData.put(name, value);
            if (!dataEquals(value, currVal))
                noteDataChange(name);
        }
    }

}
