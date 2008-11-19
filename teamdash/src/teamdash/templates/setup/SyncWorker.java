package teamdash.templates.setup;

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
