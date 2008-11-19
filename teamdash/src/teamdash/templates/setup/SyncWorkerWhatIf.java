package teamdash.templates.setup;

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

    private Map localData = new HashMap();

    public SyncWorkerWhatIf(DataContext data, DashHierarchy hier) {
        this.data = data;
        this.hier = hier;
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
        name = getOriginalPath(name);
        data.putValue(name, value);
    }

}
