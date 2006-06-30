package teamdash.templates.setup;

import java.util.List;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.HierarchyAlterer.HierarchyAlterationException;

public class SyncWorkerLive extends SyncWorker {

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
        alterer.deleteNode(path);
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
