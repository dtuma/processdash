package teamdash.templates.setup;

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
