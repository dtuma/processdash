// Copyright (C) 2002-2019 Tuma Solutions, LLC
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.HierarchyAlterer.HierarchyAlterationException;
import net.sourceforge.processdash.hier.PathRenamingInstruction;

public abstract class AbstractSyncWorker implements SyncWorker {

    protected List deletionPermissions = null;

    protected List completionPermissions = null;

    protected List nodesAdded = new ArrayList();

    protected List nodesDeleted = new ArrayList();

    protected List nodesCompleted = new ArrayList();

    protected List dataChanged = new ArrayList();

    protected boolean nodesWereRenamed = false;

    /** A list of PathRenamingOperations which can be applied to a path to
     * <b>undo</b> the node renames that have occurred.  That is, applying
     * these path renaming operations to a final node or data name will produce
     * the name that the node/data element would have had before the renames
     * took place.
     */
    protected List renameOperations = new ArrayList();


    public List getNodesDeleted() {
        return nodesDeleted;
    }

    public List getNodesCompleted() {
        return nodesCompleted;
    }

    public List getDataChanged() {
        return dataChanged;
    }

    public String getOriginalPath(String path) {
        return PathRenamingInstruction.renamePath(renameOperations, path);
    }

    public List getOriginalPaths(List paths) {
        if (paths == null || paths.isEmpty())
            return paths;

        List result = new ArrayList();
        for (Iterator i = paths.iterator(); i.hasNext();) {
            String onePath = (String) i.next();
            result.add(getOriginalPath(onePath));
        }
        return result;
    }

    public boolean nodesWereRenamed() {
        return nodesWereRenamed;
    }

    public void addTemplate(String path, String templateID)
            throws HierarchyAlterationException {
        doAddTemplate(path, templateID);
        nodesAdded.add(path);
    }

    protected abstract void doAddTemplate(String path, String templateID)
            throws HierarchyAlterationException;

    public void deleteNode(String path) throws HierarchyAlterationException {
        if (deletionPermissions == null
                || deletionPermissions.contains(getOriginalPath(path))) {
            doDeleteNode(path);
            nodesDeleted.add(path);
        }
    }

    public void renameNode(String oldPath, String newPath)
            throws HierarchyAlterationException {
        doRenameNode(oldPath, newPath);
        nodesWereRenamed = true;
        PathRenamingInstruction instr = new PathRenamingInstruction(newPath,
                oldPath);
        renameOperations.add(0, instr);
    }

    protected abstract void doRenameNode(String oldPath, String newPath)
            throws HierarchyAlterationException;

    public abstract boolean reorderNodes(String parentPath, List childNames)
            throws HierarchyAlterationException;

    public void setTemplateId(String nodePath, String templateID)
            throws HierarchyAlterationException {
    }

    protected abstract void doDeleteNode(String path)
            throws HierarchyAlterationException;

    public void markLeafComplete(String path) {
        if (completionPermissions == null
                || completionPermissions.contains(getOriginalPath(path))) {
            String completionDataName = dataName(path, "Completed");
            if (getSimpleValue(completionDataName) != null)
                return;

            SimpleData actualTime = getSimpleValue(dataName(path, "Time"));
            if (actualTime instanceof NumberData) {
                double time = ((NumberData) actualTime).getDouble();
                DoubleData estimatedTime = new DoubleData(time, true);
                doPutValue(dataName(path, "Estimated Time"), estimatedTime);
                doPutValue(dataName(path, syncDataName("Estimated Time")),
                        estimatedTime);
            }
            DateData now = new DateData();
            doPutValue(completionDataName, now);
            doPutValue(syncDataName(completionDataName), now);
            nodesCompleted.add(path);
        }
    }

    public boolean markLeafIncomplete(String path) {
        String completionDataName = dataName(path, "Completed");
        String syncDataName = syncDataName(completionDataName);
        SimpleData completionDate = getSimpleValue(completionDataName);
        SimpleData syncDate = getSimpleValue(syncDataName);
        if (completionDate != null && dataEquals(completionDate, syncDate)) {
            doPutValue(completionDataName, null);
            doPutValue(syncDataName, null);
            return true;
        } else {
            return false;
        }
    }

    public void markPSPTaskComplete(String path) {
        if (completionPermissions == null
                || completionPermissions.contains(getOriginalPath(path))) {
            String completionDataName = dataName(path, "Completed");
            if (getSimpleValue(completionDataName) != null)
                return;

            // Simple approach for now - just mark everything complete.
            // Don't try to adjust the estimated times to match the actuals.
            for (int i = 0; i < HierarchySynchronizer.PSP_PHASES.length; i++)
                markPhaseComplete(path, HierarchySynchronizer.PSP_PHASES[i]);

            nodesCompleted.add(path);
        }
    }

    public boolean markPSPTaskIncomplete(String path) {
        boolean madeChange = false;
        for (int i = 0; i < HierarchySynchronizer.PSP_PHASES.length; i++) {
            String phasePath = path + "/" + HierarchySynchronizer.PSP_PHASES[i];
            if (markLeafIncomplete(phasePath))
                madeChange = true;
        }
        return madeChange;
    }

    protected String dataName(String path, String name) {
        return DataRepository.createDataName(path, name);
    }

    protected void markPhaseComplete(String path, String phase) {
        String completionDataName = dataName(dataName(path, phase), "Completed");
        if (getSimpleValue(completionDataName) == null) {
            DateData now = new DateData();
            doPutValue(completionDataName, now);
            doPutValue(syncDataName(completionDataName), now);
        }
    }
    protected void noteDataChange(String dataName) {
        dataChanged.add(dataName);
    }

    protected abstract void doPutValue(String name, SaveableData value);

    /** Write a value into the data repository;  do it always, even if
     * we're in "what-if" mode. */
    public void doPutValueForce(String name, SaveableData value) {
        doPutValue(name, value);
    }


    private static final String SYNC_VAL_SUFFIX = "_Last_Synced_Val";

    static String syncDataName(String dataName) {
        return dataName + SYNC_VAL_SUFFIX;
    }

    private static final String REV_SYNC_TIMESTAMP_SUFFIX = "_Rev_Sync_Time";

    static String revSyncTimeDataName(String dataName) {
        return dataName + REV_SYNC_TIMESTAMP_SUFFIX;
    }

    private SaveableData lastReverseSyncedValue = null;

    public void setLastReverseSyncedValue(SaveableData d) {
        lastReverseSyncedValue = d;
    }
    public void setLastReverseSyncedValue(double d) {
        setLastReverseSyncedValue(new DoubleData(d));
    }

    public void putValue(String name, SaveableData newValue) {
        SaveableData currVal = getValue(name);
        SaveableData lastRevSyncVal = lastReverseSyncedValue;
        lastReverseSyncedValue = null;

        if (lastRevSyncVal == null && !(newValue instanceof NumberData)) {
            // This isn't a number, and isn't a value that is "reverse
            // syncable."  Just check for equality with the current
            // value.  Note that we place value first and currVal second; this
            // is important.  A ListData element turns into a StringData
            // element after a shutdown/restart cycle;  if the new value is a
            // list, and the currVal is a string, the order below will result
            // in a positive match (while the reverse order would return false)
            if (dataEquals(newValue, currVal))
                // no need to store the new value if it matches the current value.
                return;
            else {
                // the value has changed. save the new value.
                doPutValue(name, newValue);
                noteDataChange(name);
                return;
            }
        }

        String syncName = syncDataName(name);
        SimpleData lastSyncVal = getSimpleValue(syncName);

        if (dataEquals(newValue, currVal) && (currVal instanceof SimpleData)) {
            if (dataEquals(newValue, lastSyncVal)) {
                // all three values are in agreement. Nothing needs to be done.
                return;

            } else {
                // the new and old values match, but the sync doesn't.  This
                // would occur if:
                //   (a) the user has synced a value manually,
                //   (b) the WBS was updated via reverse sync, or
                //   (c) the sync occurred before sync records were kept.
                // The right action is to store the sync value for future
                // reference.  We will do this silently, even in what-if mode,
                // and won't report any change having been made.
                doPutValueForce(syncName, newValue);
            }

        } else if (isFalseSimpleValue(currVal)
                || !(currVal instanceof SimpleData)
                || dataEquals(currVal, lastSyncVal)
                || dataEquals(currVal, lastRevSyncVal)) {
            // Update the value, and make a note of the value we're syncing.
            doPutValue(name, newValue);
            doPutValue(syncName, newValue);
            noteDataChange(name);
        }
    }

    public DataSyncResult putValue(String name, SaveableData newValue,
            String wbsTimestamp) {
        SimpleData currVal = getSimpleValue(name);
        String syncName = syncDataName(name);
        SimpleData lastSyncVal = getSimpleValue(syncName);
        String syncTimeName = revSyncTimeDataName(name);
        SimpleData lastRevSyncTime = getSimpleValue(syncTimeName);

        if (dataEquals(newValue, currVal)) {
            // the current value already agrees with the new value. Update
            // metadata if necessary to reflect agreement
            if (!dataEquals(newValue, lastSyncVal))
                doPutValueForce(syncName, newValue);
            if (lastRevSyncTime != null)
                doPutValueForce(syncTimeName, null);
            return null;

        } else  if (currVal == null
                || !currVal.isDefined()
                || dataEquals(currVal, lastSyncVal)
                || !localIsNewer(lastRevSyncTime, wbsTimestamp)) {
            // if the current value is missing, or is the same as the last
            // sync, or is older than the WBS, adopt the WBS value
            doPutValue(name, newValue);
            doPutValue(syncName, newValue);
            doPutValue(syncTimeName, null);
            noteDataChange(name);
            DataSyncResult result = new DataSyncResult();
            result.name = name;
            result.localValue = currVal;
            return result;

        } else {
            // the current value has been edited more recently than the WBS.
            // return a discrepancy value to indicate this
            if (!(lastRevSyncTime instanceof DateData)) {
                lastRevSyncTime = new DateData();
                doPutValueForce(syncTimeName, lastRevSyncTime);
            }
            DataSyncResult result = new DataSyncResult();
            result.name = name;
            result.localValue = currVal;
            result.localTimestamp = (DateData) lastRevSyncTime;
            return result;
        }
    }

    protected boolean isFalseSimpleValue(SaveableData value) {
        if (value == null) return true;
        SimpleData simpleValue = value.getSimpleValue();
        return (simpleValue == null || simpleValue.test() == false);
    }

    protected boolean dataEquals(SaveableData valueA, SaveableData valueB) {
        if (valueA == valueB)
            return true;
        if (valueA == null || valueB == null)
            return false;
        if (valueA instanceof SimpleData && valueB instanceof SimpleData)
            return ((SimpleData) valueA).equals((SimpleData) valueB);
        return valueA.equals(valueB);
    }

    protected boolean localIsNewer(SimpleData localTime, String wbsTime) {
        if (wbsTime == null || !wbsTime.startsWith("@")
                || !(localTime instanceof DateData))
            return true;
        try {
            long localMillis = ((DateData) localTime).getValue().getTime();
            long wbsMillis = Long.parseLong(wbsTime.substring(1));
            return localMillis > wbsMillis;
        } catch (Exception e) {
            return true;
        }
    }

}
