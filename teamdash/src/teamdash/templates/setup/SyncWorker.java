package teamdash.templates.setup;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.HierarchyAlterer.HierarchyAlterationException;

public abstract class SyncWorker implements DataContext {

    protected List deletionPermissions = null;

    protected List completionPermissions = null;

    protected List nodesAdded = new ArrayList();

    protected List nodesDeleted = new ArrayList();

    protected List nodesCompleted = new ArrayList();

    protected List dataChanged = new ArrayList();


    public void addTemplate(String path, String templateID)
            throws HierarchyAlterationException {
        doAddTemplate(path, templateID);
        nodesAdded.add(path);
    }

    protected abstract void doAddTemplate(String path, String templateID)
            throws HierarchyAlterationException;

    public void deleteNode(String path) throws HierarchyAlterationException {
        if (deletionPermissions == null || deletionPermissions.contains(path)) {
            doDeleteNode(path);
            nodesDeleted.add(path);
        }
    }

    protected abstract void doDeleteNode(String path)
            throws HierarchyAlterationException;

    public void markLeafComplete(String path) {
        if (completionPermissions == null
                || completionPermissions.contains(path)) {
            String completionDataName = dataName(path, "Completed");
            if (getSimpleValue(completionDataName) != null)
                return;

            SimpleData actualTime = getSimpleValue(dataName(path, "Time"));
            doPutValue(dataName(path, "Estimated Time"), actualTime);
            doPutValue(completionDataName, new DateData());
            nodesCompleted.add(path);
        }
    }

    public void markPSPTaskComplete(String path) {
        if (completionPermissions == null
                || completionPermissions.contains(path)) {
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

    protected String dataName(String path, String name) {
        return DataRepository.createDataName(path, name);
    }

    protected void markPhaseComplete(String path, String phase) {
        String completionDataName = dataName(dataName(path, phase), "Completed");
        if (getSimpleValue(completionDataName) == null)
            doPutValue(completionDataName, new DateData());
    }
    protected void noteDataChange(String dataName) {
        dataChanged.add(dataName);
    }

    protected abstract void doPutValue(String name, SaveableData value);


    private static final String SYNC_VAL_SUFFIX = "_Last_Synced_Val";


    public void putValue(String name, SaveableData value) {
        SaveableData currVal = getValue(name);

        if (!(value instanceof NumberData)) {
            // this isn't a number.  Just check for equality with the current value.
            if (dataEquals(currVal, value))
                // no need to store the new value if it matches the current value.
                return;
            else {
                // the value has changed. save the new value.
                doPutValue(name, value);
                noteDataChange(name);
                return;
            }
        }

        String syncName = name + SYNC_VAL_SUFFIX;
        SimpleData lastSyncVal = getSimpleValue(syncName);
        if (isFalseSimpleValue(currVal) || dataEquals(currVal, lastSyncVal)) {
            if (dataEquals(currVal, value))
                // all three numbers are in agreement. Nothing needs to be done.
                return;

            // Update the value, and make a note of the value we're syncing.
            doPutValue(name, value);
            doPutValue(syncName, value);
            noteDataChange(name);

        } else if (dataEquals(currVal, value)) {
            // the new and old values match, but the sync doesn't.  This would
            // occur if  (a) the user had synced an estimate manually, or
            // (b) the sync occurred before sync records were kept.  The
            // right action is to store the sync value for future reference.
            doPutValue(syncName, value);
            noteDataChange(name);
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

}
