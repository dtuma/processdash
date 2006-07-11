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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVTaskDependency;
import net.sourceforge.processdash.hier.DashHierarchy;
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

    private DashHierarchy hierarchy;
    private DataRepository dataRepository;
    private DataContext data;
    private String projectPath;
    private String processID;
    private String initials, initialsPattern;
    private Element projectXML;
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
        }

        loadProcessData();
        openWBS(wbsFile);
        if (isTeam()) fullCopyMode = true;
        pruneWBS(projectXML, fullCopyMode);
    }

    public void setWhatIfMode(boolean whatIf) {
        this.whatIfMode = whatIf;
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
        } catch (Exception e) {
            throw new IOException
                ("The dashboard could not read the file containing the work " +
                 "breakdown structure for this team project.  The file may "+
                 "be corrupt.");
        } finally {
            if (in != null) try { in.close(); } catch (Exception e) {}
        }
    }

    private static final int PRUNE = 0;
    private static final int DONT_PRUNE = 1;
    private static final int NOT_A_NODE = 2;

    /** Return PRUNE if this element should be pruned. */
    private int pruneWBS(Element e, boolean onlyPruneTasks) {
        String type = e.getTagName();
        if (!NODE_TYPES.contains(type))
            return NOT_A_NODE;

        // if this node has no name, prune it.
        String nodeName = e.getAttribute(NAME_ATTR);
        if (nodeName == null || nodeName.trim().length() == 0)
            return PRUNE;

        // assume this node is prunable until we determine otherwise.
        boolean prunable = true;

        // if this is a task (PSP or otherwise),
        if (TASK_TYPE.equals(type) || PSP_TYPE.equals(type)) {
            // and we are doing a team synchronization, prune it.
            if (isTeam()) return PRUNE;
        } else if (onlyPruneTasks)
            // if this isn't a task, and we're only pruning tasks, then this
            // node isn't prunable.
            prunable = false;

        // get a list of the children of this node.
        ArrayList children = new ArrayList();
        NodeList childNodes = e.getChildNodes();
        int len = childNodes.getLength();
        for (int i = 0;   i < len;   i++)
            children.add(childNodes.item(i));

        // Look at each child and see if it is prunable.
        Iterator i = children.iterator();
        Node child;
        while (i.hasNext()) {
            child = (Node) i.next();
            if (child instanceof Element)
                switch (pruneWBS((Element) child, onlyPruneTasks)) {
                case PRUNE: e.removeChild(child); break;
                case DONT_PRUNE: prunable = false; break;
                case NOT_A_NODE: break; // do nothing
                }
        }

        // if the task so far is prunable, check to see if the current
        // individual is assigned to it.
        String time = e.getAttribute(TIME_ATTR);
        if (time != null && time.toLowerCase().indexOf(initialsPattern) != -1)
            prunable = false;

        return (prunable ? PRUNE : DONT_PRUNE);
    }

    public void dumpXML(Writer out) throws IOException {
        out.write(String.valueOf(projectXML));
    }

    public void sync() throws HierarchyAlterationException {
        changes = new ArrayList();
        syncActions = buildSyncActions();
        initPhaseIDs();

        SyncWorker syncWorker;

        if (whatIfMode)
            syncWorker = new SyncWorkerWhatIf(dataRepository);
        else
            syncWorker = new SyncWorkerLive(dataRepository,
                    deletionPermissions, completionPermissions);
        this.data = syncWorker;

        sync(syncWorker, projectPath, projectXML);

        readChangesFromWorker(syncWorker);
    }

    private void readChangesFromWorker(SyncWorker worker) {
        deletionsPerformed = worker.nodesDeleted;
        for (Iterator i = deletionsPerformed.iterator(); i.hasNext();)
            changes.add("Deleted '" + i.next() + "'");

        completionsPerformed = worker.nodesCompleted;
        for (Iterator i = completionsPerformed.iterator(); i.hasNext();)
            changes.add("Marked '" + i.next() + "' complete");

        if (changes.isEmpty() && !worker.dataChanged.isEmpty())
            changes.add("Updated miscellaneous project information");
    }

    private Map buildSyncActions() {
        HashMap result = new HashMap();
        result.put(PROJECT_TYPE, new SyncProjectNode());
        SyncSimpleNode s = new SyncSimpleNode(readOnlyNodeID, "_");
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
    private static final String PHASE_NAME_ATTR = "phaseName";
    private static final String TIME_ATTR = "time";


    private static final String PROJECT_TYPE = "project";
    private static final String SOFTWARE_TYPE = "component";
    private static final String DOCUMENT_TYPE = "document";
    private static final String PSP_TYPE = "psp";
    private static final String TASK_TYPE = "task";
    private static final String DEPENDENCY_TYPE = "dependency";
    private static final List NODE_TYPES = Arrays.asList(new String[] {
        PROJECT_TYPE, SOFTWARE_TYPE, DOCUMENT_TYPE, PSP_TYPE, TASK_TYPE });

    private static final String WBS_ID_DATA_NAME = "WBS_Unique_ID";
    private static final String EST_TIME_DATA_NAME = "Estimated Time";


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
            List hierarchyChildren = getHierarchyChildNames(pathPrefix);
            List childrenToDelete = new ArrayList(hierarchyChildren);

            NodeList childNodes = node.getChildNodes();
            int len = childNodes.getLength();
            for (int i = 0;   i < len;   i++) {
                Node child = childNodes.item(i);
                if (child instanceof Element)
                    childrenToDelete.remove
                        (sync(worker, pathPrefix, (Element) child));
            }
            removeKnownChildren(node, childrenToDelete);
            if (!childrenToDelete.isEmpty())
                deleteHierarchyChildren(worker, pathPrefix, childrenToDelete);
        }

        /** This method presents a list of children which tentatively face
         * deletion, because they have no corresponding XML Element in the
         * projDump.xml file.  Subclasses should override this method and
         * remove children they recognize.
         */
        protected void removeKnownChildren(Element node, List childrenToDelete) {
        }

        public String getName(Element node) {
            return node.getAttribute(NAME_ATTR);
        }

        public String getPath(String pathPrefix, Element node) {
            String nodeName = getName(node);
            if (nodeName == null)
                return pathPrefix;

            String wbsID = node.getAttribute(ID_ATTR);
            PropertyKey wbsChild = findHierarchyChildByID(pathPrefix, wbsID);
            if (wbsChild != null)
                return wbsChild.path();

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

        private PropertyKey findHierarchyChildByID(String pathPrefix, String id) {
            PropertyKey parent = hierarchy.findExistingKey(pathPrefix);
            if (parent == null) return null;

            int numChildren = hierarchy.getNumChildren(parent);
            for (int i = 0;   i < numChildren;  i++) {
                PropertyKey child = hierarchy.getChildKey(parent, i);
                String childPath = child.path();
                String dataName = DataRepository.createDataName(childPath, WBS_ID_DATA_NAME);
                SimpleData val = data.getSimpleValue(dataName);
                if (val != null && id.equals(val.format()))
                    return child;
            }

            return null;
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
                else {
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
                    for (int i = numChildren;   i-- > 0; )
                        completeOrDeleteNode(worker,
                                hierarchy.getChildKey(key, i));
                }
            }
        }

        private boolean isIndividualNodeDeletable(String path) {
            return (isZero(getData(path, "Time"))
                    && isZero(getData(path, "Defects Injected"))
                    && isZero(getData(path, "Defects Removed")));
        }
        private boolean isZero(SimpleData d) {
            if (d instanceof NumberData)
                return ((NumberData) d).getDouble() == 0;
            else
                return (d == null);
        }
        private boolean isPSPTask(PropertyKey key) {
            String templateID = hierarchy.getID(key);
            return (templateID != null && templateID.startsWith("PSP"));
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

        public SyncSimpleNode(String templateID, String suffix) {
            this.templateID = templateID;
            this.suffix = suffix;
        }

        public boolean syncNode(SyncWorker worker, String pathPrefix, Element node)
            throws HierarchyAlterationException {
            String path = getPath(pathPrefix, node);
            String currentID = getIDForPath(path);
            if (currentID == null) {
                worker.addTemplate(path, templateID);
                syncData(path, node);
                changes.add("Created '"+path+"'");
            } else if (templateID.equals(currentID)) {
                // the node exists with the given name.  Just sync its data.
                syncData(path, node);
            } else {
                // there is a problem.
                changes.add("Could not create '"+path+"' - existing node is in the way");
                return false;
            }

            syncChildren(worker, pathPrefix, node);
            return true;
        }
        public void syncData(String path, Element node) {
             String nodeID = node.getAttribute(ID_ATTR);
             String taskID = node.getAttribute(TASK_ID_ATTR);
             try {
                 putData(path, WBS_ID_DATA_NAME, StringData.create(nodeID));
                 setTaskIDs(path, taskID);
            } catch (Exception e) {}
            if (!isTeam()) {
                maybeSaveDocSize(path, node);
                maybeSaveDependencies(path, node);
            }
        }

        public String getName(Element node) {
            String result = super.getName(node);
            if (isPhaseName(result))
                result = result + suffix;
            return result;
        }

        protected void maybeSaveTimeValue(String path, Element node) {
            double time = parseTime(node);
            if (time == 0) return;

            putData(path, EST_TIME_DATA_NAME, new DoubleData(time * 60));
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
            if (deps != null) {
                // We found dependencies; add them to this node.
                if (EVTaskDependency.addTaskDependencies(data, path, deps,
                            whatIfMode))
                    changes.add("Updated task dependencies for '" + path + "'");

            } else {
                List currentDeps = EVTaskDependency.getDependencies(data, path);
                if (currentDeps != null && !currentDeps.isEmpty())
                    // We found no dependencies in the project dump, but there
                    // *are* dependencies for this node in the dashboard.  We
                    // need to cross reference manually, and remove any old
                    // synced dependencies from this node in the dashboard.
                    removePastSyncedDependencies(currentDeps, path);
            }
        }

        protected List readDependenciesFromNode(Element node) {
            List result = null;

            NodeList nl = node.getChildNodes();
            for (int i = 0;   i < nl.getLength();   i++) {
                Node child = nl.item(i);
                if (child instanceof Element) {
                    Element childElem = (Element) child;
                    if (childElem.getTagName().equals(DEPENDENCY_TYPE)) {
                        try {
                            // try to set the source attribute if it isn't
                            // already present.  This supports project data
                            // dumps that were saved with an earlier version
                            // of the dashboard that didn't write the source
                            // attribute. If this fails because the DOM
                            // implementation doesn't support mutable
                            // attributes, just ignore the error and press on.
                            childElem.setAttribute("source", "wbs");
                        } catch (Throwable t) {}
                        if (result == null)
                            result = new LinkedList();
                        result.add(new EVTaskDependency(childElem));
                    }
                }
            }

            return result;
        }

        protected void removePastSyncedDependencies(List deps, String path) {
            boolean madeChange = false;

            if (REMOVE_DEPENDENCIES_BY_SOURCE == false) {
                // if we have no way of detecting the sources of
                // dependencies, make the most likely assumption: they
                // came from a past sync and all need to be deleted.
                // (This unfortunate choice will only affect individuals who
                // have not upgraded their dashboard installation.)
                deps = Collections.EMPTY_LIST;
                madeChange = true;

            } else {
                // scan existing dependencies, and remove any that came from
                // the WBS.  Then save the resulting list.
                for (Iterator i = deps.iterator(); i.hasNext();) {
                    EVTaskDependency d = (EVTaskDependency) i.next();
                    if ("wbs".equals(d.getSource())) {
                        i.remove();
                        madeChange = true;
                    }
                }
            }

            if (madeChange) {
                EVTaskDependency.saveDependencies(data, path, deps);
                changes.add("Updated task dependencies for '" + path + "'");
            }
        }
    }

    private static boolean REMOVE_DEPENDENCIES_BY_SOURCE = true;
    static {
        try {
            EVTaskDependency.class.getMethod("getSource", null).getName();
        } catch (Throwable t) {
            REMOVE_DEPENDENCIES_BY_SOURCE = false;
        }
    }


    private SimpleData getData(String dataPrefix, String name) {
        String dataName = DataRepository.createDataName(dataPrefix, name);
        return data.getSimpleValue(dataName);
    }

    protected void putData(String dataPrefix, String name, SimpleData value) {
        String dataName = DataRepository.createDataName(dataPrefix, name);
        data.putValue(dataName, value);
    }

    private HashMap phaseIDs;

    private void initPhaseIDs() {
        phaseIDs = new HashMap();
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
    }

    private class SyncTaskNode extends SyncSimpleNode {

        public SyncTaskNode() {
            super(taskNodeID, " Task");
            initPhaseIDs();
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
                changes.add("Could not find template for '"+phaseName+"'");
                return false;
            }
            path = path + "/" + phaseName;
            String currentID = getIDForPath(path);
            if (currentID == null) {
                worker.addTemplate(path, templateID);
                maybeSaveTimeValue(path, node);
                changes.add("Created '"+path+"'");
            } else if (templateID.equals(currentID)) {
                // the node exists with the given name.
                maybeSaveTimeValue(path, node);
            } else {
                // there is a problem.
                changes.add("Could not create '"+path+"' - existing node is in the way");
                return false;
            }

            return true;
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
        protected void removeKnownChildren(Element node, List childrenToDelete) {
            String phaseName = node.getAttribute(PHASE_NAME_ATTR);
            childrenToDelete.remove(phaseName);
            super.removeKnownChildren(node, childrenToDelete);
        }


    }



    private static final String EST_SIZE_DATA_NAME =
        "Sized_Objects/0/Estimated Size";
    private static final String SIZE_UNITS_DATA_NAME =
        "Sized_Objects/0/Sized_Object_Units";



    private class SyncPSPTaskNode extends SyncSimpleNode {
        public SyncPSPTaskNode() {
            super("PSP2.1", " Task");
        }

        public void syncData(String path, Element node) {
            super.syncData(path, node);
            maybeSaveTimeValue(path, node);
            maybeSaveSizeData(path, node);
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

        protected void removeKnownChildren(Element node, List childrenToDelete) {
            for (int i = 0; i < PSP_PHASES.length; i++)
                childrenToDelete.remove(PSP_PHASES[i]);
            super.removeKnownChildren(node, childrenToDelete);
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

    private String getIDForPath(String path) {
        PropertyKey key = hierarchy.findExistingKey(path);
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
        if (phaseName == null) phaseName = node.getTagName();
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

}
