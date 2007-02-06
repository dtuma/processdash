package teamdash.templates.setup;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVTaskDependency;
import net.sourceforge.processdash.ev.EVTaskListData;
import net.sourceforge.processdash.ev.EVSchedule.Period;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.HierarchyAlterer.HierarchyAlterationException;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class HierarchySynchronizer {

    public static final String SYNC_TEAM = "(team)";
    public static final String SYNC_MASTER = "(master)";
    private static final String WBS_SOURCE = "wbs";

    private DashHierarchy hierarchy;
    private DataRepository dataRepository;
    private DataContext data;
    private String projectPath;
    private PropertyKey projectKey;
    private String processID;
    private String initials, initialsPattern;
    private Element projectXML;
    private Date startDate;
    private double hoursPerWeek;
    private ArrayList changes;

    private String readOnlyNodeID;
    private String taskNodeID;

    /** list of nodes which this object has user permission to delete.
     * null impllies permission to delete anything; the empty list implies
     * permission to delete nothing. */
    private List deletionPermissions = Collections.EMPTY_LIST;

    /** list of nodes which this object has user permission to mark complete.
     * null impllies permission to complete anything; the empty list implies
     * permission to complete nothing. */
    private List completionPermissions = Collections.EMPTY_LIST;

    /** Does the caller just want to find out if anything needs changing? */
    private boolean whatIfMode = true;

    /** Does the caller want to copy all nontask WBS items? */
    private boolean fullCopyMode;

    private List deferredDeletions;

    private List deletionsPerformed;
    private List completionsPerformed;

    private Map sizeConstrPhases;
    private List allConstrPhases;


    /** Create a hierarchy synchronizer for a team project */
    public HierarchySynchronizer(String projectPath,
                                 String processID,
                                 File wbsFile,
                                 String initials,
                                 boolean fullCopyMode,
                                 DashHierarchy hierarchy,
                                 DataRepository data) throws IOException {
        this.projectPath = projectPath;
        this.processID = processID;
        this.hierarchy = hierarchy;
        this.data = this.dataRepository = data;
        this.projectKey = hierarchy.findExistingKey(projectPath);

        if (SYNC_TEAM.equals(initials)) { // team
            this.initials = this.initialsPattern = SYNC_TEAM;
            this.readOnlyNodeID = processID + "/TeamNode";
            this.taskNodeID = null;
            this.deletionPermissions = null;
        } else if (SYNC_MASTER.equals(initials)) { // master
            this.initials = this.initialsPattern = SYNC_MASTER;
            this.readOnlyNodeID = processID + "/MasterNode";
            this.taskNodeID = null;
            this.deletionPermissions = null;
        } else { // individual
            this.initials = initials;
            this.initialsPattern = "," + initials.toLowerCase() + "=";
            this.readOnlyNodeID = processID + "/IndivReadOnlyNode";
            this.taskNodeID = processID + "/IndivEmptyNode";
            this.deletionPermissions = Collections.EMPTY_LIST;
            this.completionPermissions = Collections.EMPTY_LIST;
            this.deferredDeletions = new ArrayList();
        }

        loadProcessData();
        openWBS(wbsFile);
        if (isTeam()) fullCopyMode = true;
        this.fullCopyMode = fullCopyMode;
    }

    public void setWhatIfMode(boolean whatIf) {
        this.whatIfMode = whatIf;
    }

    public boolean isWhatIfMode() {
        return whatIfMode;
    }

    public void setDeletionPermissions(List p) {
        this.deletionPermissions = p;
    }

    public void setCompletionPermissions(List p) {
        this.completionPermissions = p;
    }

    public boolean isTeam() {
        return initials == SYNC_TEAM || initials == SYNC_MASTER;
    }

    public List getChanges() {
        return changes;
    }

    public boolean isFollowOnWorkNeeded() {
        return deferredDeletions != null && !deferredDeletions.isEmpty();
    }

    public List getTaskDeletions() {
        return deletionsPerformed;
    }

    public List getTaskCompletions() {
        return completionsPerformed;
    }


    public void dumpChanges(PrintWriter out) {
        Iterator i = changes.iterator();
        while (i.hasNext()) {
            out.print(i.next());
            out.println();
        }
    }

    private void loadProcessData() {
        List sizeMetricsList = getProcessDataList("Custom_Size_Metric_List");

        sizeConstrPhases = new HashMap();
        for (Iterator i = sizeMetricsList.iterator(); i.hasNext();) {
            String metric = (String) i.next();
            List phases = getProcessDataList(metric
                    + "_Development_Phase_List");
            sizeConstrPhases.put(metric, phases);
        }

        List dldPhases = getProcessDataList("DLD_Phase_List");
        dldPhases.add("psp");
        sizeConstrPhases.put("DLD Lines", dldPhases);

        List codePhases = getProcessDataList("CODE_Phase_List");
        codePhases.add("psp");
        sizeConstrPhases.put("LOC", codePhases);

        allConstrPhases = getProcessDataList(
                "All_Sizes_Development_Phase_List");
        allConstrPhases.addAll(dldPhases);
        allConstrPhases.addAll(codePhases);
    }

    private void openWBS(File wbsFile) throws IOException {
        if (wbsFile == null || !wbsFile.exists())
            throw new IOException
                ("Could not find the file containing the work breakdown " +
                 "structure for this team project.");

        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(wbsFile));
            Document doc = XMLUtils.parse(in);
            projectXML = doc.getDocumentElement();

            String projectTaskID = projectXML.getAttribute(TASK_ID_ATTR);
            projectTaskID = cleanupProjectIDs(projectTaskID);
            projectXML.setAttribute(TASK_ID_ATTR, projectTaskID);
        } catch (Exception e) {
            throw new IOException
                ("The dashboard could not read the file containing the work " +
                 "breakdown structure for this team project.  The file may "+
                 "be corrupt.");
        } finally {
            if (in != null) try { in.close(); } catch (Exception e) {}
        }
    }

    private static final int NOT_A_NODE = -1;
    private static final int PRUNE = 0;
    private static final int QUASI_PRUNE = 1;
    private static final int DONT_PRUNE = 2;

    /** Return PRUNE if this element should be pruned. */
    private int pruneWBS(Element e, boolean onlyPruneTasks, Set keepIDs) {
        String type = e.getTagName();
        if (!NODE_TYPES.contains(type))
            return NOT_A_NODE;

        // if this node has no name, prune it.
        String nodeName = e.getAttribute(NAME_ATTR);
        if (nodeName == null || nodeName.trim().length() == 0)
            return PRUNE;

        // assume this node is prunable until we determine otherwise.
        int prunable = PRUNE;

        // if this is a task (PSP or otherwise),
        if (TASK_TYPE.equals(type) || PSP_TYPE.equals(type)) {
            // and we are doing a team synchronization, prune it.
            if (isTeam()) return PRUNE;
        } else if (onlyPruneTasks)
            // if this isn't a task, and we're only pruning tasks, then this
            // node isn't prunable.
            prunable = DONT_PRUNE;

        // Look for children with duplicate names, and rename them if they
        // exist.
        renameDuplicateChildren(e);

        // Look at each child and see if it is prunable.
        List children = XMLUtils.getChildElements(e);
        for (Iterator i = children.iterator(); i.hasNext();) {
            Element child = (Element) i.next();
            int childPrunable = pruneWBS(child, onlyPruneTasks, keepIDs);
            if (childPrunable == PRUNE)
                e.removeChild(child);
            prunable = Math.max(prunable, childPrunable);
        }

        // if the task so far is prunable, check to see if the current
        // individual is assigned to it.
        String time = e.getAttribute(TIME_ATTR);
        if (time != null && time.toLowerCase().indexOf(initialsPattern) != -1)
            prunable = DONT_PRUNE;

        // if this node is in the list of items we must keep, don't prune it.
        String nodeID = e.getAttribute(ID_ATTR);
        if (keepIDs.contains(nodeID))
            prunable = Math.max(prunable, QUASI_PRUNE);

        // If we were unable to prune this item because it (or one of its
        // children) is in the list of items we must keep, go ahead and mark
        // this item with a "PRUNED" attribute so we can recognize it later.
        if (prunable == QUASI_PRUNE)
            e.setAttribute(PRUNED_ATTR, "true");

        return prunable;
    }

    /** Ensure that the children of the given element have unique names, by
     * renaming children with duplicate names.
     */
    private void renameDuplicateChildren(Element e) {
        Set childNames = new HashSet();
        List children = XMLUtils.getChildElements(e);
        for (Iterator i = children.iterator(); i.hasNext();) {
            Element child = (Element) i.next();
            String name = child.getAttribute(NAME_ATTR);
            if (childNames.contains(name)) {
                String newName;
                int j = 2;
                do {
                    newName = name + " (duplicate " + (j++) + ")";
                } while (childNames.contains(newName));
                child.setAttribute(NAME_ATTR, newName);
                name = newName;
            }
            childNames.add(name);
        }
    }

    private Set getNonprunableIDs() {
        if (isTeam())
            return Collections.EMPTY_SET;

        Set results = new HashSet();
        getNonprunableIDs(projectKey, results);
        results.remove(null);
        return results;
    }
    private void getNonprunableIDs(PropertyKey key, Set results) {
        String path = key.path();

        if (whatIfMode) {
            if (isIndividualNodeDeletable(path))
                // no actual time has been logged against this node or any of
                // its children, so they are all potentially prunable.
                return;
        } else {
            if (deletionPermissions != null
                    && deletionPermissions.contains(path))
            // the user has granted us permission to delete this node and
            // all its decendants, so they are definitely prunable.
            return;
        }

        // if we reach this point, the node in question exists in the user's
        // hierarchy, and either (a) it has actual data or (b)  we don't have
        // the permission to delete it.  Thus, this node will be present in
        // the user's final WBS when we're done.  As a result, we'll need to
        // know the canonical location of the node in the project WBS, so we
        // request that it not be pruned.
        results.add(getWbsIdForPath(path));

        // recurse over children.
        for (int i = hierarchy.getNumChildren(key); i-- > 0; )
             getNonprunableIDs(hierarchy.getChildKey(key, i), results);
    }

    private String getWbsIdForPath(String path) {
        return getStringData(getData(path, WBS_ID_DATA_NAME));
    }

    private boolean isIndividualNodeDeletable(String path) {
        return (isZero(getData(path, "Time"))
                && isZero(getData(path, "Defects Injected"))
                && isZero(getData(path, "Defects Removed")));
    }

    private List findHierarchyNodesByID(String id) {
        ArrayList results = new ArrayList();
        findHierarchyNodesByID(projectKey, id, results);
        return results;
    }
    private void findHierarchyNodesByID(PropertyKey node, String id,
            List results) {
        if (node == null)
            return;

        String nodeID = getWbsIdForPath(node.path());
        if (id.equals(nodeID))
            results.add(node);

        for (int i = hierarchy.getNumChildren(node);  i-- > 0; ) {
            PropertyKey child = hierarchy.getChildKey(node, i);
            findHierarchyNodesByID(child, id, results);
        }
    }


    public void dumpXML(Writer out) throws IOException {
        out.write(String.valueOf(projectXML));
    }

    public void sync() throws HierarchyAlterationException {
        try {
            if (!whatIfMode) getProjectSyncLock();
            doSync();
        } finally {
            releaseProjectSyncLock();
        }
    }
    private void doSync() throws HierarchyAlterationException {
        ListData labelData = null;
        if (isTeam())
            // for a team, get label data for all nodes in a project before
            // we prune the non-team nodes
            labelData = getLabelData(projectXML);
        pruneWBS(projectXML, fullCopyMode, getNonprunableIDs());
        if (!isTeam()) {
            // for an individual, collect label data after pruning so we
            // only see labels that are relevant to our tasks.
            labelData = getLabelData(projectXML);
            getScheduleData(projectXML);
        }

        changes = new ArrayList();
        syncActions = buildSyncActions();
        phaseIDs = initPhaseIDs(processID);

        SyncWorker syncWorker;

        if (whatIfMode) {
            DashHierarchy mockHierarchy = new DashHierarchy("");
            mockHierarchy.copy(this.hierarchy);
            this.hierarchy = mockHierarchy;
            syncWorker = new SyncWorkerWhatIf(dataRepository, mockHierarchy);
        } else
            syncWorker = new SyncWorkerLive(dataRepository,
                    deletionPermissions, completionPermissions);
        this.data = syncWorker;

        putData(projectPath, LABEL_LIST_DATA_NAME, labelData);

        if (!isTeam())
            syncSchedule();

        sync(syncWorker, projectPath, projectXML);

        processDeferredDeletions(syncWorker);

        readChangesFromWorker(syncWorker);
    }

    private ListData getLabelData(Element projectXML) {
        Map labelData = new TreeMap();
        collectLabelData(projectXML, labelData);
        ListData labelList = new ListData();
        labelList.add("label:");
        for (Iterator iter = labelData.entrySet().iterator(); iter.hasNext();) {
            Map.Entry e = (Map.Entry) iter.next();
            String label = (String) e.getKey();
            labelList.add("label:" + label);
            Set taskIDs = (Set) e.getValue();
            for (Iterator i = taskIDs.iterator(); i.hasNext();)
                labelList.add((String) i.next());
        }
        return labelList;
    }

    private void collectLabelData(Element node, Map dest) {
        String nodeID = node.getAttribute(TASK_ID_ATTR);
        String labels = node.getAttribute(LABELS_ATTR);
        if (XMLUtils.hasValue(nodeID) && XMLUtils.hasValue(labels)) {
            String[] labelList = labels.split(",");
            Set idSet = new HashSet(Arrays.asList(nodeID.split("\\s*,\\s*")));
            idSet.remove("");
            for (int i = 0; i < labelList.length; i++) {
                String oneLabel = labelList[i];
                if (XMLUtils.hasValue(oneLabel)) {
                    Set taskIDs = (Set) dest.get(oneLabel);
                    if (taskIDs == null)
                        dest.put(oneLabel, taskIDs = new TreeSet());
                    taskIDs.addAll(idSet);
                }
            }
        }

        List children = XMLUtils.getChildElements(node);
        for (Iterator i = children.iterator(); i.hasNext();)
            collectLabelData((Element) i.next(), dest);
    }


    private void getScheduleData(Element projectXML) {
        startDate = null;
        hoursPerWeek = 0;
        List children = XMLUtils.getChildElements(projectXML);
        for (Iterator i = children.iterator(); i.hasNext();) {
            Element e = (Element) i.next();
            if (TEAM_MEMBER_TYPE.equals(e.getTagName())
                    && initials.equalsIgnoreCase(e.getAttribute(INITIALS_ATTR))) {
                startDate = XMLUtils.getXMLDate(e, START_DATE_ATTR);
                hoursPerWeek = XMLUtils.getXMLNum(e, HOURS_PER_WEEK_ATTR);
                return;
            }
        }
    }


    /** If we are synchronizing a project for an individual, and that
     * individual has not edited their earned value schedule yet, sync
     * the initial period in their earned value schedule to reflect the
     * information entered in the team plan.
     */
    private void syncSchedule() {
        // only operate on individual schedules.
        if (isTeam()) return;
        // if no schedule information was found, there is nothing to do.
        if (startDate == null && hoursPerWeek == 0) return;

        // look up the name of the EV schedule for the individual. If we
        // don't find a valid/existing schedule name, abort.
        String taskListName = getStringData(getData(projectPath,
                TeamDataConstants.PROJECT_SCHEDULE_NAME));
        if (!EVTaskListData.validName(taskListName)) return;
        if (!EVTaskListData.exists(dataRepository, taskListName)) return;

        // open the schedule.
        EVTaskListData tl = new EVTaskListData(taskListName, dataRepository,
                hierarchy, false);
        EVSchedule schedule = tl.getSchedule();

        // if the schedule has more than one non-automatic row, the user MUST
        // have manually edited it.  Don't make any changes.
        if (schedule.getRowCount() > 1 && !schedule.get(2).isAutomatic())
            return;

        // The schedule only has one explicit row.  Compare that row to the
        // last synced values for the schedule.  If they don't match, don't
        // make any changes.
        Period p = schedule.get(1);
        double syncPdt = getDoubleData(getData(projectPath,
                TeamDataConstants.PROJECT_SCHEDULE_SYNC_PDT));
        if (p.planDirectTime() != syncPdt)
            return;
        Date syncDate = getDateData(getData(projectPath,
                TeamDataConstants.PROJECT_SCHEDULE_SYNC_DATE));
        if (!p.getBeginDate().equals(syncDate))
            return;

        boolean madeChange = false;

        // if we have hours per week information, and it differs from the
        // data in the schedule, update the schedule.
        int pdt = (int) (hoursPerWeek * 60);
        if (pdt != 0 && Math.abs(p.planDirectTime() - pdt) > 0.01) {
            p.setPlanDirectTime(Integer.toString(pdt));
            if (!whatIfMode)
                dataRepository.putValue(DataRepository.createDataName(
                        projectPath,
                        TeamDataConstants.PROJECT_SCHEDULE_SYNC_PDT),
                        new DoubleData(pdt, false));
            madeChange = true;
        }

        // if we have a start date, and it differs from the data in the
        // schedule, update the schedule.
        if (startDate != null && !startDate.equals(p.getBeginDate())) {
            p.setBeginDate(startDate);
            if (!whatIfMode)
                dataRepository.putValue(DataRepository.createDataName(
                        projectPath,
                        TeamDataConstants.PROJECT_SCHEDULE_SYNC_DATE),
                        new DateData(startDate, false));
            madeChange = true;
        }

        if (madeChange)
            changes.add("Updated the earned value schedule");

        if (!whatIfMode)
            tl.save();
    }


    private void processDeferredDeletions(SyncWorker worker)
            throws HierarchyAlterationException {
        if (deferredDeletions != null && !worker.nodesWereRenamed()) {
            // sort the list, to ensure that parents appear in the list before
            // their children do
            Collections.sort(deferredDeletions);
            while (!deferredDeletions.isEmpty()) {
                // get the next item that needs deleting
                String nodeToDelete = (String) deferredDeletions.remove(0);

                // if our deletion list contains children of the item in
                // question, they are redundant.  Discard them.
                for (Iterator i = deferredDeletions.iterator(); i.hasNext();) {
                    String path = (String) i.next();
                    if (Filter.pathMatches(path, nodeToDelete, true))
                        i.remove();
                }

                // perform the deletion/completion.
                PropertyKey key = hierarchy.findExistingKey(nodeToDelete);
                completeOrDeleteNode(worker, key);
            }
        }
    }

    private void completeOrDeleteNode(SyncWorker worker, PropertyKey key)
            throws HierarchyAlterationException {
        int numChildren = hierarchy.getNumChildren(key);
        boolean isLeaf = (numChildren == 0);
        String path = key.path();

        if (!isLeaf && getData(path, WBS_ID_DATA_NAME) == null)
            // this node has no WBS_ID.  It must have been created
            // manually by the user.  Don't bother it.
            return;

        if (isIndividualNodeDeletable(path)) {
            worker.deleteNode(path);

        } else if (isLeaf) {
            worker.markLeafComplete(path);

        } else {
            setTaskIDs(path, "");
            if (isPSPTask(key))
                worker.markPSPTaskComplete(path);
            else {
                List children = new ArrayList();
                for (int i = 0;  i < numChildren;   i++)
                    children.add(hierarchy.getChildKey(key, i));
                for (Iterator i = children.iterator(); i.hasNext();)
                    completeOrDeleteNode(worker, (PropertyKey) i.next());
            }
        }
    }

    private boolean isPSPTask(PropertyKey key) {
        String templateID = hierarchy.getID(key);
        return (templateID != null && templateID.startsWith("PSP"));
    }

    private void readChangesFromWorker(SyncWorker worker) {
        deletionsPerformed = worker.getOriginalPaths(worker.nodesDeleted);
        for (Iterator i = deletionsPerformed.iterator(); i.hasNext();)
            changes.add("Deleted '" + i.next() + "'");

        completionsPerformed = worker.getOriginalPaths(worker.nodesCompleted);
        for (Iterator i = completionsPerformed.iterator(); i.hasNext();)
            changes.add("Marked '" + i.next() + "' complete");

        boolean foundNullChangeTokens = false;
        while (changes.remove(null))
            foundNullChangeTokens = true;

        if (changes.isEmpty()
                && (foundNullChangeTokens || !worker.dataChanged.isEmpty()))
            changes.add("Updated miscellaneous project information");
    }

    private Map buildSyncActions() {
        HashMap result = new HashMap();
        result.put(PROJECT_TYPE, new SyncProjectNode());
        SyncSimpleNode s = new SyncSimpleNode(readOnlyNodeID, "_", taskNodeID);
        result.put(SOFTWARE_TYPE, s);
        result.put(DOCUMENT_TYPE, s);

        if (!isTeam()) {
            result.put(TASK_TYPE, new SyncTaskNode());
            result.put(PSP_TYPE, new SyncPSPTaskNode());
        }

        return result;
    }

    private Map syncActions;

    private String sync(SyncWorker worker, String pathPrefix, Element node)
        throws HierarchyAlterationException
    {
        String type = node.getTagName();
        SyncNode s = (SyncNode) syncActions.get(type);
        if (s != null) {
            s.syncNode(worker, pathPrefix, node);
            return s.getName(node);
        } else
            return null;
    }

    private static final String NAME_ATTR = "name";
    private static final String ID_ATTR = "id";
    private static final String TASK_ID_ATTR = "tid";
    private static final String LABELS_ATTR = "labels";
    private static final String PHASE_NAME_ATTR = "phaseName";
    private static final String TIME_ATTR = "time";
    private static final String PRUNED_ATTR = "PRUNED";


    private static final String PROJECT_TYPE = "project";
    private static final String SOFTWARE_TYPE = "component";
    private static final String DOCUMENT_TYPE = "document";
    static final String PSP_TYPE = "psp";
    private static final String TASK_TYPE = "task";
    private static final String DEPENDENCY_TYPE = "dependency";
    static final List NODE_TYPES = Arrays.asList(new String[] {
        PROJECT_TYPE, SOFTWARE_TYPE, DOCUMENT_TYPE, PSP_TYPE, TASK_TYPE });

    private static final String TEAM_MEMBER_TYPE = "teamMember";
    private static final String INITIALS_ATTR = "initials";
    private static final String START_DATE_ATTR = "startDate";
    private static final String HOURS_PER_WEEK_ATTR = "hoursPerWeek";

    private static final String WBS_ID_DATA_NAME = "WBS_Unique_ID";
    private static final String EST_TIME_DATA_NAME = "Estimated Time";
    private static final String LABEL_LIST_DATA_NAME =
        "Synchronized_Task_Labels";


    private class SyncNode {

        public boolean syncNode(SyncWorker worker, String pathPrefix, Element node)
            throws HierarchyAlterationException
        {
            syncChildren(worker, pathPrefix, node);
            return true;
        }

        public void syncChildren(SyncWorker worker, String pathPrefix,
                                 Element node)
            throws HierarchyAlterationException
        {
            pathPrefix = getPath(pathPrefix, node);

            List wbsChildren = XMLUtils.getChildElements(node);
            List wbsChildNames = new ArrayList();
            for (Iterator i = wbsChildren.iterator(); i.hasNext();) {
                Element child = (Element) i.next();
                String childName = sync(worker, pathPrefix, child);
                if (childName != null)
                    wbsChildNames.add(childName);
            }

            List childrenToDelete = getHierarchyChildNames(pathPrefix);
            childrenToDelete.removeAll(wbsChildNames);
            filterOutKnownChildren(node, childrenToDelete);
            if (!childrenToDelete.isEmpty())
                deleteHierarchyChildren(worker, pathPrefix, childrenToDelete);

            if (wbsChildNames.size() > 1)
                if (worker.reorderNodes(pathPrefix, wbsChildNames))
                    madeMiscChange();
        }

        /** This method is presented with a list of children that tentatively
         * face deletion, because they have no corresponding XML Element in the
         * projDump.xml file.  Subclasses should override this method and
         * remove children they recognize from the list, to prevent them from
         * being deleted.
         */
        protected void filterOutKnownChildren(Element node,
                List childrenToDelete) {
        }

        public String getName(Element node) {
            return node.getAttribute(NAME_ATTR);
        }

        public String getPath(String pathPrefix, Element node) {
            String nodeName = getName(node);
            if (nodeName == null)
                return pathPrefix;
            else
                return pathPrefix + "/" + nodeName;
        }


        private List getHierarchyChildNames(String pathPrefix) {
            ArrayList result = new ArrayList();
            PropertyKey parent = hierarchy.findExistingKey(pathPrefix);
            if (parent == null) return Collections.EMPTY_LIST;

            int numChildren = hierarchy.getNumChildren(parent);
            for (int i = 0;   i < numChildren;  i++)
                result.add(hierarchy.getChildName(parent, i));

            return result;
        }


        private void deleteHierarchyChildren(SyncWorker worker,
                                             String pathPrefix,
                                             List childrenToDelete)
            throws HierarchyAlterationException
        {
            Iterator i = childrenToDelete.iterator();
            while (i.hasNext()) {
                String nodeToDelete = pathPrefix + "/" + i.next();
                if (isTeam())
                    worker.deleteNode(nodeToDelete);
                else
                    deferredDeletions.add(nodeToDelete);
            }
        }
    }

    private class SyncProjectNode extends SyncNode {

        public boolean syncNode(SyncWorker worker, String pathPrefix,
                Element node) throws HierarchyAlterationException {

            try {
                String projIDs = node.getAttribute(TASK_ID_ATTR);
                setTaskIDs(pathPrefix, cleanupProjectIDs(projIDs));
            } catch (Exception e) {}

            return super.syncNode(worker, pathPrefix, node);
        }

        public String getName(Element node) { return null; }
    }

    private class SyncSimpleNode extends SyncNode {

        String templateID, suffix;
        Collection compatibleTemplateIDs;

        public SyncSimpleNode(String templateID, String suffix,
                String compatibleTemplateID) {
            this.templateID = templateID;
            this.suffix = suffix;
            this.compatibleTemplateIDs = new HashSet();
            this.compatibleTemplateIDs.add(templateID);
            if (compatibleTemplateID != null)
                this.compatibleTemplateIDs.add(compatibleTemplateID);
        }

        public boolean syncNode(SyncWorker worker, String pathPrefix, Element node)
            throws HierarchyAlterationException {
            String path = getPath(pathPrefix, node);
            String currentTemplateID = getTemplateIDForPath(path);

            String nodeID = node.getAttribute(ID_ATTR);
            String currentNodeID = getWbsIdForPath(path);

            // If there is an incompatible node in the way, move it.
            if (currentTemplateID != null && !isTeam()
                    && (!compatibleTemplateIDs.contains(currentTemplateID)
                            || !nodeID.equals(currentNodeID))) {
                moveNodeOutOfTheWay(worker, path);
                currentTemplateID = currentNodeID = null;
            }

            // if the target node does not exist, try to find a match
            // elsewhere in the hierarchy to move to this location.
            if (currentTemplateID == null && !isTeam()) {
                List movableNodes = findHierarchyNodesByID(nodeID);
                filterIncompatibleNodes(movableNodes);
                PropertyKey sourceNode = getBestNodeToMove(movableNodes);
                if (sourceNode != null) {
                    currentTemplateID = getTemplateIDForKey(sourceNode);
                    String oldPath = sourceNode.path();
                    worker.renameNode(oldPath, path);
                    changes.add("Moved '" + worker.getOriginalPath(oldPath)
                            + "' to '" + path + "'");
                    deferredDeletions.remove(oldPath);
                }
            }

            // if the target node still does not exist, we need to create it.
            if (currentTemplateID == null) {
                worker.addTemplate(path, templateID);
                changes.add("Created '"+path+"'");

            // if it exists but with the wrong template ID, convert it.
            } else if (!currentTemplateID.equals(templateID)) {
                worker.setTemplateId(path, templateID);
                madeMiscChange();
            }

            // Now, sync the data and children for the node.
            syncData(worker, path, node);
            syncChildren(worker, pathPrefix, node);

            if (isPrunedNode(node))
                deferredDeletions.add(path);

            return true;
        }

        protected void filterIncompatibleNodes(List movableNodes) {
            for (Iterator i = movableNodes.iterator(); i.hasNext();) {
                PropertyKey node = (PropertyKey) i.next();
                String templateID = getTemplateIDForKey(node);
                if (!compatibleTemplateIDs.contains(templateID))
                    i.remove();
            }
        }

        private PropertyKey getBestNodeToMove(List movableNodes) {
            if (movableNodes == null || movableNodes.isEmpty())
                return null;

            PropertyKey result = (PropertyKey) movableNodes.get(0);

            // if there is more than one node to choose from, find the node
            // with the most actual time (that may not be a foolproof method,
            // but this is only a temporary legacy issue anyway)
            if (movableNodes.size() > 1) {
                SimpleData maxTime = new DoubleData(-1);
                for (Iterator i = movableNodes.iterator(); i.hasNext();) {
                    PropertyKey node = (PropertyKey) i.next();
                    SimpleData nodeTime = getData(node.path(), "Time");
                    if (nodeTime.greaterThan(maxTime)) {
                        result = node;
                        maxTime = nodeTime;
                    }
                }
            }

            return result;
        }

        public void syncData(SyncWorker worker, String path, Element node) {
             String nodeID = node.getAttribute(ID_ATTR);
             String taskID = node.getAttribute(TASK_ID_ATTR);
             try {
                 putData(path, WBS_ID_DATA_NAME, StringData.create(nodeID));
                 if (!isPrunedNode(node))
                     setTaskIDs(path, taskID);
            } catch (Exception e) {}
            if (!isTeam() && !isPrunedNode(node)) {
                maybeSaveDocSize(path, node);
                maybeSaveDependencies(path, node);
            }
        }

        protected boolean isPrunedNode(Element node) {
            return XMLUtils.hasValue(node.getAttribute(PRUNED_ATTR));
        }

        public String getName(Element node) {
            String result = super.getName(node);
            if (isPhaseName(result))
                result = result + suffix;
            return result;
        }

        protected void maybeSaveTimeValue(SyncWorker worker, String path,
                Element node) {
            if (isPrunedNode(node)) return;
            double time = parseTime(node);
            if (time == 0) return;

            if (undoMarkTaskComplete(worker, path))
                changes.add("Marked '" + path + "' incomplete.");
            if (okToChangeTimeEstimate(path))
                putData(path, EST_TIME_DATA_NAME, new DoubleData(time * 60));
        }

        protected boolean okToChangeTimeEstimate(String path) {
            return true;
        }

        /** If the hierarchy synchronizer marked the given task complete in
         * the past, mark it incomplete.
         * 
         * @return true if a change was made
         */
        protected boolean undoMarkTaskComplete(SyncWorker worker, String path) {
            return false;
        }

        protected double parseTime(Element node) {
            String timeAttr = node.getAttribute(TIME_ATTR);
            if (timeAttr == null) return 0;
            int beg = timeAttr.toLowerCase().indexOf(initialsPattern);
            if (beg == -1) return 0;
            beg += initialsPattern.length();
            int end = timeAttr.indexOf(',', beg);
            if (end == -1) return 0;
            try {
                return Double.parseDouble(timeAttr.substring(beg, end));
            } catch (Exception e) {}

            return 0;
        }

        protected void putNumber(String path, String name, String value, double ratio) {
            try {
                if (value == null || value.length() == 0)
                    value = "0";
                DoubleData d = new DoubleData(value);
                if (ratio != 1.0)
                    d = new DoubleData(d.getDouble() * ratio);
                putData(path, name, d);
            } catch (Exception e) {}
        }

        private void maybeSaveDocSize(String path, Element node) {
            // see if this node has doc size data.
            String units = node.getAttribute("sizeUnits");
            if (units == null || "LOC".equals(units))
                return;

            // check to see if any doc size data exists for this node
            SimpleData d = getData(path, EST_SIZE_DATA_NAME);
            if (d != null && d.test()) return;
            d = getData(path, SIZE_UNITS_DATA_NAME);
            if (d != null && d.test()) return;

            // find out whether this individual is a contributor to the
            // construction of the given document
            double ratio = getTimeRatio(node, units);
            if (ratio == 0) return;

            // calculate the percentage of the document construction time
            // contributed by this individual
            double size;
            try {
                String sizeStr = node.getAttribute("sizeNC");
                size = Double.parseDouble(sizeStr);
                size = size * ratio;
            } catch (NumberFormatException nfe) {
                return;
            }

            // save the document size data to the project.
            putData(path, EST_SIZE_DATA_NAME, new DoubleData(size));
            putData(path, SIZE_UNITS_DATA_NAME, StringData.create(units));
        }

        protected void maybeSaveDependencies(String path, Element node) {
            List deps = readDependenciesFromNode(node);
            if (EVTaskDependency.setTaskDependencies(data, path, deps,
                    WBS_SOURCE, whatIfMode))
                changes.add("Updated task dependencies for '" + path + "'");
        }

        protected List readDependenciesFromNode(Element node) {
            List result = null;

            NodeList nl = node.getChildNodes();
            for (int i = 0;   i < nl.getLength();   i++) {
                Node child = nl.item(i);
                if (child instanceof Element) {
                    Element childElem = (Element) child;
                    if (childElem.getTagName().equals(DEPENDENCY_TYPE)) {
                        if (result == null)
                            result = new LinkedList();
                        result.add(new EVTaskDependency(childElem));
                    }
                }
            }

            return result;
        }
    }


    private SimpleData getData(String dataPrefix, String name) {
        String dataName = DataRepository.createDataName(dataPrefix, name);
        return data.getSimpleValue(dataName);
    }

    protected void moveNodeOutOfTheWay(SyncWorker worker, String path)
            throws HierarchyAlterationException {
        int i = 1;
        String suffix = " (non-WBS)";
        String newPath;
        while (true) {
            newPath = path + suffix;
            if (hierarchy.findExistingKey(newPath) == null)
                break;
            i++;
            suffix = " (non-WBS " + i + ")";
        }
        worker.renameNode(path, newPath);
        madeMiscChange();
        if (deferredDeletions.remove(path))
            deferredDeletions.add(newPath);
    }

    protected void putData(String dataPrefix, String name, SimpleData value) {
        String dataName = DataRepository.createDataName(dataPrefix, name);
        data.putValue(dataName, value);
    }

    private String getStringData(SimpleData val) {
        return (val == null ? null : val.format());
    }

    private Date getDateData(SimpleData val) {
        if (val instanceof DateData)
            return ((DateData) val).getValue();
        else
            return null;
    }

    private double getDoubleData(SimpleData val) {
        if (val instanceof NumberData)
            return ((NumberData) val).getDouble();
        else
            return 0;
    }

    private boolean isZero(SimpleData d) {
        if (d instanceof NumberData)
            return ((NumberData) d).getDouble() == 0;
        else
            return (d == null);
    }

    private HashMap phaseIDs;

    static HashMap initPhaseIDs(String processID) {
        HashMap phaseIDs = new HashMap();
        Iterator i = DashController.getTemplates().entrySet().iterator();
        Map.Entry e;
        String phasePrefix = processID + "/PHASE/";
        while (i.hasNext()) {
            e = (Map.Entry) i.next();
            String templateID = (String) e.getKey();
            String name = (String) e.getValue();
            if (templateID.startsWith(phasePrefix))
                phaseIDs.put(name, templateID);
        }
        return phaseIDs;
    }

    private class SyncTaskNode extends SyncSimpleNode {

        public SyncTaskNode() {
            super(taskNodeID, " Task", readOnlyNodeID);
            phaseIDs = initPhaseIDs(processID);
        }
        public boolean syncNode(SyncWorker worker, String pathPrefix, Element node)
            throws HierarchyAlterationException
        {
            if (super.syncNode(worker, pathPrefix, node) == false)
                return false;

            String path = getPath(pathPrefix, node);
            maybeSaveInspSizeData(path, node);

            String phaseName = node.getAttribute(PHASE_NAME_ATTR);
            String templateID = (String) phaseIDs.get(phaseName);
            if (templateID == null) {
                // If the "phase" for this task is null, we don't need to add
                // any phase underneath this node.  (This can occur if a task
                // in the WBS is used as a parent;  the parent task will not
                // have any phase designator.)
                return true;
            }
            path = path + "/" + phaseName;
            String currentID = getTemplateIDForPath(path);
            if (currentID == null) {
                worker.addTemplate(path, templateID);
                maybeSaveTimeValue(worker, path, node);
                changes.add("Created '"+path+"'");
            } else if (templateID.equals(currentID)) {
                // the node exists with the given name.
                maybeSaveTimeValue(worker, path, node);
            } else {
                // there is a problem.
                changes.add("Could not create '"+path+"' - existing node is in the way");
                return false;
            }

            return true;
        }

        protected boolean undoMarkTaskComplete(SyncWorker worker, String path) {
            return worker.markLeafIncomplete(path);
        }

        private void maybeSaveInspSizeData(String path, Element node) {
            // see if this node has inspection size data.
            String inspUnits = node.getAttribute("inspUnits");
            if (inspUnits == null || inspUnits.length() == 0)
                return;
            if (inspUnits.equals("LOC"))
                inspUnits = "New & Changed LOC";

            // check to see if any inspection size data exists for this node.
            SimpleData d = getData(path, EST_SIZE_DATA_NAME);
            if (d != null && d.test()) return;
            d = getData(path, SIZE_UNITS_DATA_NAME);
            if (d != null && d.test()) return;

            // save the inspection size data to the project.
            putNumber(path, EST_SIZE_DATA_NAME, node.getAttribute("inspSize"), 1.0);
            putData(path, SIZE_UNITS_DATA_NAME,
                    StringData.create("Inspected " + inspUnits));
        }
        protected void filterOutKnownChildren(Element node, List childrenToDelete) {
            String phaseName = node.getAttribute(PHASE_NAME_ATTR);
            childrenToDelete.remove(phaseName);
            super.filterOutKnownChildren(node, childrenToDelete);
        }


    }



    private static final String EST_SIZE_DATA_NAME =
        "Sized_Objects/0/Estimated Size";
    private static final String SIZE_UNITS_DATA_NAME =
        "Sized_Objects/0/Sized_Object_Units";



    private class SyncPSPTaskNode extends SyncSimpleNode {
        public SyncPSPTaskNode() {
            super("PSP2.1", " Task", null);
        }

        public void syncData(SyncWorker worker, String path, Element node) {
            super.syncData(worker, path, node);
            if (!isPrunedNode(node)) {
                maybeSaveTimeValue(worker, path, node);
                maybeSaveSizeData(path, node);
            }
        }

        protected boolean okToChangeTimeEstimate(String path) {
            return (getData(path, "Planning/Completed") == null);
        }

        protected boolean undoMarkTaskComplete(SyncWorker worker, String path) {
            return worker.markPSPTaskIncomplete(path);
        }

        private void maybeSaveSizeData(String path, Element node) {
            // ensure that this node has size data.
            if (!"LOC".equals(node.getAttribute("sizeUnits")))
                return;

            // check to see if any size data exists for this PSP2.1 project.
            for (int i = 0;   i < locSizeDataNames.length;   i++) {
                SimpleData d = getData(path, locSizeDataNames[i]);
                if (d != null && d.test()) return;
            }

            // find out what percentage of this task the user will perform.
            double ratio = getTimeRatio(node, "LOC");

            // save the size data to the project.
            for (int i = 0;   i < sizeAttrNames.length;   i++)
                putNumber(path, locSizeDataNames[i],
                          node.getAttribute(sizeAttrNames[i]),
                          (locSizeDataNeedsRatio[i] ? ratio : 1.0));
        }

        protected void filterOutKnownChildren(Element node, List childrenToDelete) {
            for (int i = 0; i < PSP_PHASES.length; i++)
                childrenToDelete.remove(PSP_PHASES[i]);
            super.filterOutKnownChildren(node, childrenToDelete);
        }


    }

    private static final String[] sizeAttrNames = new String[] {
        "sizeBase", "sizeDel", "sizeMod", "sizeAdd", "sizeReu", "sizeNC" };
    private static final boolean[] locSizeDataNeedsRatio = new boolean[] {
         false,      true,      true,      true,      false,     true    };
    private static final String[] locSizeDataNames = new String[] {
        "Estimated Base LOC",
        "Estimated Deleted LOC",
        "Estimated Modified LOC",
        "New Objects/0/LOC",
        "Reused Objects/0/LOC",
        "Estimated New & Changed LOC" };
    public static final String[] PSP_PHASES = { "Planning", "Design",
        "Design Review", "Code", "Code Review", "Compile", "Test",
        "Postmortem" };



    private boolean isPhaseName(String name) {
        return phaseIDs.containsKey(name);
    }

    private String getTemplateIDForPath(String path) {
        PropertyKey key = hierarchy.findExistingKey(path);
        return getTemplateIDForKey(key);
    }

    private String getTemplateIDForKey(PropertyKey key) {
        if (key == null) return null;
        String actualID = hierarchy.getID(key);
        return (actualID == null ? "" : actualID);
    }





    private double getTimeRatio(Element node, String units) {
        List shortList = (List) sizeConstrPhases.get(units);

        double result = getTimeRatio(node, shortList);
        if (Double.isNaN(result))
            result = getTimeRatio(node, allConstrPhases);

        while (Double.isNaN(result)) {
            node = getParentElement(node);
            if (node == null) break;
            result = getTimeRatio(node, shortList);
            if (Double.isNaN(result))
                result = getTimeRatio(node, allConstrPhases);
        }

        if (Double.isNaN(result))
            return 0;
        else
            return result;
    }

    private Element getParentElement(Element node) {
        Node n = node;
        while (true) {
            n = n.getParentNode();
            if (n instanceof Element) return (Element) n;
            if (n == null) return null;
        }
    }

    private double getTimeRatio(Element node, List phaseList) {
        if (phaseList == null) return Double.NaN;

        timeRatioTotal = timeRatioPersonal = 0;
        sumUpConstructionPhases(node, phaseList);
        return timeRatioPersonal / timeRatioTotal;
    }


    private double timeRatioTotal, timeRatioPersonal;

    private void sumUpConstructionPhases(Element node, List phaseList) {
        String phaseName = node.getAttribute(PHASE_NAME_ATTR);
        if (phaseName == null || phaseName.length() == 0)
            phaseName = node.getTagName();
        String timeAttr = node.getAttribute(TIME_ATTR);
        if (phaseList.contains(phaseName) &&
            timeAttr != null && timeAttr.length() != 0)
            addTimeData(timeAttr);
        NodeList children = node.getChildNodes();
        int len = children.getLength();
        Node child;
        for (int i = 0;   i < len;   i++) {
            child = (Node) children.item(i);
            if (child instanceof Element)
                sumUpConstructionPhases((Element) child, phaseList);
        }
    }
    private void addTimeData(String attr) {
        StringTokenizer tok = new StringTokenizer(attr, ",");
        while (tok.hasMoreTokens()) try {
            String time = tok.nextToken();
            int pos = time.indexOf('=');
            if (pos == -1) continue;
            String who = time.substring(0, pos);
            double amount = Double.parseDouble(time.substring(pos+1));
            timeRatioTotal += amount;
            if (initials.equals(who))
                timeRatioPersonal += amount;
        } catch (NumberFormatException nfe) {}
    }

    private List getProcessDataList(String name) {
        List result = new LinkedList();
        String dataName = "/" + processID + "/" + name;
        SimpleData val = data.getSimpleValue(dataName);
        if (val instanceof StringData)
            val = ((StringData) val).asList();
        if (val instanceof ListData) {
            ListData l = (ListData) val;
            for (int i = 0;  i < l.size();  i++)
                result.add(l.get(i));
        }
        return result;
    }

    private void setTaskIDs(String path, String ids) {
        List currentIDs = EVTaskDependency.getTaskIDs(data, path);
        if (currentIDs != null && !currentIDs.isEmpty()) {
            String currentStr = StringUtils.join(currentIDs, ",");
            if (currentStr.equals(ids))
                // The repository already agrees with what we want to set.
                // Nothing needs to be done.
                return;

            // The repository doesn't agree with our list of IDs.  To be safe,
            // delete all of the IDs in the repository, so we can add the
            // correct list of IDs below.
            for (Iterator i = currentIDs.iterator(); i.hasNext();) {
                String id = (String) i.next();
                EVTaskDependency.removeTaskID(data, path, id);
            }
        }

        EVTaskDependency.addTaskIDs(data, path, ids);
    }

    private String cleanupProjectIDs(String ids) {
        if (ids == null)
            return null;
        else
            return ids.replaceAll(":\\d+", ":root");
    }

    private boolean madeMiscChange() {
        return changes.add(null);
    }


    private static Set SYNC_LOCKS = Collections.synchronizedSet(new HashSet());

    private Object projectSyncLock = null;

    private void getProjectSyncLock() {
         while (true) {
             synchronized (SYNC_LOCKS) {
                 if (SYNC_LOCKS.contains(projectPath)) {
                     try {
                         SYNC_LOCKS.wait();
                     } catch (InterruptedException e) {}
                 } else {
                     SYNC_LOCKS.add(projectPath);
                     projectSyncLock = projectPath;
                     return;
                 }
             }
         }
    }
    private void releaseProjectSyncLock() {
        if (projectSyncLock != null) {
            synchronized (SYNC_LOCKS) {
                SYNC_LOCKS.remove(projectSyncLock);
                SYNC_LOCKS.notifyAll();
            }
        }
    }

}
