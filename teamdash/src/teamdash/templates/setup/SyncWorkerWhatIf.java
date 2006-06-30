package teamdash.templates.setup;

import java.util.HashMap;
import java.util.Map;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.hier.HierarchyAlterer.HierarchyAlterationException;

public class SyncWorkerWhatIf extends SyncWorker {

    private DataContext data;

    private Map localData = new HashMap();

    public SyncWorkerWhatIf(DataContext data) {
        this.data = data;
    }

    protected void doAddTemplate(String path, String templateID)
            throws HierarchyAlterationException {
        // do nothing.
    }

    protected void doDeleteNode(String path)
            throws HierarchyAlterationException {
        // do nothing.
    }

    public SaveableData getValue(String name) {
        SaveableData result = (SaveableData) localData.get(name);
        if (result != null)
            return result;
        else
            return data.getValue(name);
    }

    public SimpleData getSimpleValue(String name) {
        SaveableData result = (SaveableData) localData.get(name);
        if (result != null)
            return result.getSimpleValue();
        else
            return data.getSimpleValue(name);

    }

    protected void doPutValue(String name, SaveableData value) {
        localData.put(name, value);
    }


}
