// Copyright (C) 2002-2013 Tuma Solutions, LLC
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

package teamdash.templates.setup;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVMetadata;
import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVSchedule.Period;
import net.sourceforge.processdash.ev.EVTaskDependency;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListData;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.hier.HierarchyAlterer.HierarchyAlterationException;
import net.sourceforge.processdash.hier.HierarchyNote;
import net.sourceforge.processdash.hier.HierarchyNote.InvalidNoteSpecification;
import net.sourceforge.processdash.hier.HierarchyNoteManager;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.log.defects.Defect;
import net.sourceforge.processdash.log.defects.DefectAnalyzer;
import net.sourceforge.processdash.templates.DashPackage;
import net.sourceforge.processdash.util.DateUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.ThreadThrottler;
import net.sourceforge.processdash.util.XMLUtils;


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
    private String projectID;
    private URL workflowLocation;
    private boolean workflowURLsSupported;
    private String initials, initialsPattern;
    private String ownerName;
    private boolean oldStyleSync;
    private String pspToDateSubset;
    private boolean promptForPspToDateSubset;
    private Element projectXML;
    private Map<Element, List<Element>> prunedChildren;
    private String dumpFileVersion;
    private Date dumpFileTimestamp;
    private Date startDate;
    private int endWeek;
    private double hoursPerWeek;
    private Map scheduleExceptions;
    private ArrayList changes;
    private ListData discrepancies;

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

    /** map of to-date subsets that should be used for various psp tasks. */
    private Map<String, String> pspSubsetSelections = Collections.EMPTY_MAP;

    /** Does the caller just want to find out if anything needs changing? */
    private boolean whatIfMode = true;

    /** In "what-if" mode, can we stop as soon as we discover that work is
     * needed?  This would be false if a full list of deletions/renames is
     * needed. */
    private boolean whatIfBrief = false;

    /** A list that holds optional debugging data */
    private List<String> debugLogInfo = null;

    /** Does the caller want to copy all nontask WBS items? */
    private boolean fullCopyMode;

    /** Does the user want us to sync the sizes of inspected work products? */
    private boolean syncInspectedSizes;

    private List deferredDeletions;

    private List deletionsPerformed;
    private List completionsPerformed;
    private List pspTasksNeedingSubsetPrompt;

    private Map sizeConstrPhases;
    private List allConstrPhases;

    private static Logger logger = Logger.getLogger(HierarchySynchronizer.class
            .getName());


    /** Create a hierarchy synchronizer for a team project */
    public HierarchySynchronizer(String projectPath,
                                 String processID,
                                 URL wbsLocation,
                                 URL workflowLocation,
                                 String initials,
                                 String ownerName,
                                 boolean fullCopyMode,
                                 String defaultPspSubset,
                                 boolean promptForPspSubset,
                                 DashHierarchy hierarchy,
                                 DataRepository data) throws IOException {
        this.projectPath = projectPath;
        this.processID = processID;
        this.workflowLocation = workflowLocation;
        this.hierarchy = hierarchy;
        this.data = this.dataRepository = data;
        this.projectKey = hierarchy.findExistingKey(projectPath);
        this.projectID = getStringData(getData(projectPath,
            TeamDataConstants.PROJECT_ID));

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
            this.ownerName = ownerName;
            String rootTemplate = getTemplateIDForKey(projectKey);
            if (rootTemplate.endsWith("/Indiv2Root")) {
                this.readOnlyNodeID = processID + "/Indiv2ReadOnlyNode";
                this.taskNodeID = processID + "/Indiv2Task";
                this.oldStyleSync = false;
            } else {
                this.readOnlyNodeID = processID + "/IndivReadOnlyNode";
                this.taskNodeID = processID + "/IndivEmptyNode";
                this.oldStyleSync = true;
            }
            this.pspToDateSubset = defaultPspSubset;
            this.promptForPspToDateSubset = promptForPspSubset;
            this.deletionPermissions = Collections.EMPTY_LIST;
            this.completionPermissions = Collections.EMPTY_LIST;
            this.deferredDeletions = new ArrayList();
            this.pspTasksNeedingSubsetPrompt = new ArrayList();
            this.syncInspectedSizes = Settings.getBool(
                "syncWBS.copyInspectedSizes", false);
        }

        loadProcessData();
        openWBS(wbsLocation);
        this.fullCopyMode = fullCopyMode;
        this.workflowURLsSupported = false;
    }

    public void setWhatIfMode(boolean whatIf) {
        this.whatIfMode = whatIf;
    }

    public boolean isWhatIfMode() {
        return whatIfMode;
    }

    public boolean isWhatIfBrief() {
        return whatIfBrief;
    }

    public void setWhatIfBrief(boolean whatIfBrief) {
        this.whatIfBrief = whatIfBrief;
        if (whatIfBrief)
            this.whatIfMode = true;
    }

    public void enableDebugLogging() {
        this.debugLogInfo = new ArrayList<String>();
    }

    public List<String> getDebugLogInfo() {
        return this.debugLogInfo;
    }

    public void setDeletionPermissions(List p) {
        this.deletionPermissions = p;
    }

    public void setCompletionPermissions(List p) {
        this.completionPermissions = p;
    }

    public void setPspSubsetSelections(Map<String, String> selections) {
        this.pspSubsetSelections = selections;
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

    public List getPspTasksNeedingSubsetPrompt() {
        return pspTasksNeedingSubsetPrompt;
    }

    public void dumpChanges(PrintWriter out) {
        Iterator i = changes.iterator();
        while (i.hasNext()) {
            out.print(i.next());
            out.println();
        }
    }

    public Map<String, String> checkIndivInitials() {
        if (isTeam())
            return null;

        Map<String, String> result = new LinkedHashMap<String, String>();

        for (Element e : XMLUtils.getChildElements(projectXML)) {
            if (TEAM_MEMBER_TYPE.equals(e.getTagName())) {
                String xmlInitials = e.getAttribute(INITIALS_ATTR);
                String xmlName = e.getAttribute(NAME_ATTR);
                if (initials.equalsIgnoreCase(xmlInitials))
                    return null;
                else if (XMLUtils.hasValue(xmlInitials)
                        && XMLUtils.hasValue(xmlName))
                    result.put(xmlInitials, xmlName);
            }
        }

        return result;
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

    private void openWBS(URL wbsLocation) throws IOException {
        InputStream in = null;
        try {
            URLConnection conn = wbsLocation.openConnection();
            in = new BufferedInputStream(conn.getInputStream());
            Document doc = XMLUtils.parse(in);
            projectXML = doc.getDocumentElement();
            prunedChildren = new HashMap<Element, List<Element>>();

            String projectTaskID = projectXML.getAttribute(TASK_ID_ATTR);
            projectTaskID = cleanupProjectIDs(projectTaskID);
            projectXML.setAttribute(TASK_ID_ATTR, projectTaskID);
            projectXML.setAttribute(ID_ATTR, ROOT_NODE_PSEUDO_ID);

            dumpFileVersion = projectXML.getAttribute(VERSION_ATTR);
            if (!StringUtils.hasValue(dumpFileVersion))
                dumpFileVersion = "0";

            dumpFileTimestamp = XMLUtils.getXMLDate(projectXML, SAVE_DATE_ATTR);
            if (dumpFileTimestamp == null) {
                long dumpTime = conn.getLastModified();
                if (dumpTime > 0)
                    dumpFileTimestamp = new Date(dumpTime);
            }
        } catch (Exception e) {
            throw new IOException
                ("The dashboard could not read the file containing the work " +
                 "breakdown structure for this team project.  The file may "+
                 "be corrupt.");
        } finally {
            if (in != null) try { in.close(); } catch (Exception e) {}
        }
    }

    public Element getProjectXML() {
        return projectXML;
    }

    private static final int NOT_A_NODE = -1;
    private static final int PRUNE = 0;
    private static final int QUASI_PRUNE = 1;
    private static final int DONT_PRUNE = 2;

    /** Return PRUNE if this element should be pruned. */
    private int pruneWBS(Element e, boolean onlyPruneTasks, Set keepIDs,
            Set parentPseudoIDs) {
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

        // Calculate an alternate list of names this node could be known by.
        Set pseudoIDs = new HashSet();
        for (Object oneParentID : parentPseudoIDs)
            pseudoIDs.add(oneParentID + "/" + nodeName);
        String nodeID = e.getAttribute(ID_ATTR);
        pseudoIDs.add(nodeID);

        // Look at each child and see if it is prunable.
        List children = XMLUtils.getChildElements(e);
        for (Iterator i = children.iterator(); i.hasNext();) {
            Element child = (Element) i.next();
            int childPrunable = pruneWBS(child, onlyPruneTasks, keepIDs,
                pseudoIDs);
            if (childPrunable == PRUNE)
                pruneChild(e, child);
            prunable = Math.max(prunable, childPrunable);
        }

        // if the task so far is prunable, check to see if the current
        // individual is assigned to it.
        String time = e.getAttribute(TIME_ATTR);
        if (time != null && time.toLowerCase().indexOf(initialsPattern) != -1)
            prunable = DONT_PRUNE;

        // if this node is in the list of items we must keep, don't prune it.
        if (setsIntersect(keepIDs, pseudoIDs))
            prunable = Math.max(prunable, QUASI_PRUNE);

        // If we were unable to prune this item because it (or one of its
        // children) is in the list of items we must keep, go ahead and mark
        // this item with a "PRUNED" attribute so we can recognize it later.
        if (prunable == QUASI_PRUNE)
            e.setAttribute(PRUNED_ATTR, "true");

        return prunable;
    }

    private void pruneChild(Element parent, Element child) {
        // remove the child from the XML document tree.
        parent.removeChild(child);
        // make a note of the removal for later reference.
        List<Element> pruneList = prunedChildren.get(parent);
        if (pruneList == null) {
            pruneList = new ArrayList<Element>();
            prunedChildren.put(parent, pruneList);
        }
        pruneList.add(child);
    }

    private List<Element> getChildrenPrunedFrom(Element parent) {
        List<Element> result = prunedChildren.get(parent);
        return (result == null ? Collections.EMPTY_LIST : result);
    }

    /** Ensure that the children of the given element have unique names, by
     * renaming children with duplicate names.   In the process, also scrub
     * the name for unexpected characters.
     */
    private void renameDuplicateChildren(Element e) {
        Set childNames = new HashSet();
        List children = XMLUtils.getChildElements(e);
        for (Iterator i = children.iterator(); i.hasNext();) {
            Element child = (Element) i.next();
            String type = child.getTagName();
            if (!NODE_TYPES.contains(type))
                continue;

            String name = child.getAttribute(NAME_ATTR);

            String scrubbedName = scrubHierarchyName(name);
            if (!scrubbedName.equals(name))
                child.setAttribute(NAME_ATTR, name = scrubbedName);

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

    private String scrubHierarchyName(String name) {
        // replace common extended characters found in Office documents
        for (int i = 0; i < CHARACTER_REPLACEMENTS.length; i++) {
            String repl = CHARACTER_REPLACEMENTS[i];
            for (int c = 1;  c < repl.length();  c++)
                name = name.replace(repl.charAt(c), repl.charAt(0));
        }
        // disallow slash characters
        name = name.replace('/', ',');
        // perform round-trip through default platform encoding, and trim
        name = new String(name.getBytes()).trim();
        return name;
    }
    private static final String[] CHARACTER_REPLACEMENTS = {
        "\"\u201C\u201D",       // opening and closing double quotes
        "-\u2013\u2014",        // Em-dash and En-dash
        " \u00A0\u2002\u2003"    // nonbreaking space, em-space, en-space
    };

    private Set getNonprunableIDs() {
        if (isTeam())
            return Collections.EMPTY_SET;

        getDeletableIndivNodes();

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
        results.add(getPseudoWbsIdForKey(key));

        // recurse over children.
        for (int i = hierarchy.getNumChildren(key); i-- > 0; )
             getNonprunableIDs(hierarchy.getChildKey(key, i), results);
    }

    private String getWbsIdForPath(String path) {
        return getStringData(getData(path, TeamDataConstants.WBS_ID_DATA_NAME));
    }

    private Set<String> allKnownWbsIds;
    private void getAllKnownWbsIds() {
        allKnownWbsIds = new HashSet<String>();
        getAllKnownWbsIds(projectXML);
    }
    private void getAllKnownWbsIds(Element wbsNode) {
        for (Element child : XMLUtils.getChildElements(wbsNode)) {
            String type = child.getTagName();
            if (NODE_TYPES.contains(type)) {
                String childID = child.getAttribute(ID_ATTR);
                if (XMLUtils.hasValue(childID))
                    allKnownWbsIds.add(childID);
                getAllKnownWbsIds(child);
            }
        }
    }
    private boolean isNodeStillPresentInWbs(String id) {
        return StringUtils.hasValue(id) && allKnownWbsIds.contains(id);
    }

    /** Nodes that came from the WBS will have a WBS ID.  Nodes that the user
     * created will not.  For such nodes, this method constructs a "pseudo ID"
     * composed of the ID of the nearest WBS parent, followed by the string
     * path to this node.  (For example, a user created node "Foo" underneath
     * a WBS node with ID 123 would have the pseudo WBS ID "123/Foo".)
     * 
     * This method will return the official WBS ID of a node, if it has one.
     * Otherwise, if this is the ROOT node, it will return "root".
     * Otherwise, this will construct a pseudo WBS ID as described above and
     * return that value.
     */
    private String getPseudoWbsIdForKey(PropertyKey key) {
        if (key == null)
            return null;
        else if (key.equals(projectKey))
            return ROOT_NODE_PSEUDO_ID;

        String id = getWbsIdForPath(key.path());
        if (StringUtils.hasValue(id))
            return id;

        return getPseudoWbsIdForKey(key.getParent()) + "/" + key.name();
    }

    private Set deletableIndividualNodes;
    private boolean isIndividualNodeDeletable(String path) {
        return deletableIndividualNodes != null
                && deletableIndividualNodes.contains(path);
    }
    private void getDeletableIndivNodes() {
        Set results = new HashSet();
        getDeletableIndivNodes(projectKey, results);
        deletableIndividualNodes = results;
    }
    private boolean getDeletableIndivNodes(PropertyKey key, Set results) {
        String path = key.path();
        boolean isDeletable = true;

        if (!isPSPTask(key)) {
            // recurse over children.
            for (int i = hierarchy.getNumChildren(key); i-- > 0; )
                if (!getDeletableIndivNodes(hierarchy.getChildKey(key, i), results))
                    // if our child isn't deletable, then we aren't either.
                    isDeletable = false;
        }

        // At this point, if "isDeletable" is true, then all our descendants
        // must be deletable.  Perform a few additional checks to see if this
        // node itself seems deletable.

        // We don't delete nodes that were manually created by a user.
        isDeletable = isDeletable && !isUserCreatedNode(key);

        // this node is only deletable if no time has been logged here
        isDeletable = isDeletable && isZero(getData(path, "Time"));

        // this node is only deletable if no defects have been logged here
        DefectCounter c = new DefectCounter();
        DefectAnalyzer.run(hierarchy, key, false, c);
        isDeletable = isDeletable && !c.hasDefects();

        if (isDeletable)
            results.add(path);

        return isDeletable;
    }

    private double getTotalActualTimeForIndivNode(PropertyKey node) {
        double result = 0;

        // add time logged directly to the node, if it exists.
        SimpleData d = getData(node.path(), ACT_TIME_DATA_NAME);
        result += getDoubleData(d);

        // if the node has children, add their time too.
        for (int i = hierarchy.getNumChildren(node);  i-- > 0; ) {
            PropertyKey child = hierarchy.getChildKey(node, i);
            result += getTotalActualTimeForIndivNode(child);
        }

        return result;
    }

    private double getTotalPlanTimeForIndivNode(PropertyKey node) {
        double result = 0;

        // add time logged directly to the node, if it exists.
        SimpleData d = getData(node.path(), EST_TIME_DATA_NAME);
        result += getDoubleData(d);

        // if the node is not a PSP task and has children, add their time too.
        if (!isPSPTask(node)) {
            for (int i = hierarchy.getNumChildren(node);  i-- > 0; ) {
                PropertyKey child = hierarchy.getChildKey(node, i);
                result += getTotalPlanTimeForIndivNode(child);
            }
        }
        return result;
    }

    private static class DefectCounter implements DefectAnalyzer.Task {
        private int count = 0;
        public void analyze(String path, Defect d) {
            count++;
        }
        public boolean hasDefects() {
            return count > 0;
        }
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

    private boolean isUserCreatedNode(PropertyKey key) {
        if (isTeam() || isPhaseStub(key))
            return false;
        return (getWbsIdForPath(key.path()) == null);
    }

    private boolean isPhaseStub(PropertyKey key) {
        if (oldStyleSync)
            return (hierarchy.getNumChildren(key) == 0)
                    && (getData(key.path(), PROCESS_ID_DATA_NAME) == null);
        else
            return false;
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
        phaseIDs = initPhaseIDs(processID);
        ListData nodeOrderData = new ListData();
        collectNodeOrderData(projectXML, nodeOrderData);
        getAllKnownWbsIds();
        Map<String, String> milestoneNames = syncMilestoneData(projectXML);
        ListData labelData = new ListData();
        ListData milestoneData = new ListData();
        if (isTeam()) {
            // for a team, get label data for all nodes in a project before
            // we prune the non-team nodes
            getLabelData(projectXML, milestoneNames, labelData, milestoneData);
            saveHierarchyFilterInfo(projectXML);
        }
        pruneWBS(projectXML, fullCopyMode, getNonprunableIDs(),
            Collections.EMPTY_SET);
        if (!isTeam()) {
            // for an individual, collect label data after pruning so we
            // only see labels that are relevant to our tasks.
            getLabelData(projectXML, milestoneNames, labelData, milestoneData);
            getScheduleData(projectXML);
        }

        changes = new ArrayList();
        discrepancies = new ListData();
        discrepancies.add(new Date());
        syncActions = buildSyncActions();

        SyncWorker syncWorker;

        if (whatIfMode) {
            DashHierarchy mockHierarchy = new DashHierarchy("");
            mockHierarchy.copy(this.hierarchy);
            this.hierarchy = mockHierarchy;
            syncWorker = new SyncWorkerWhatIf(dataRepository, mockHierarchy);
        } else {
            syncWorker = new SyncWorkerLive(dataRepository,
                    deletionPermissions, completionPermissions);
        }

        if (debugLogInfo != null)
            syncWorker = SyncWorkerLogger.wrapWorker(syncWorker, debugLogInfo);

        this.data = syncWorker;

        forceData(projectPath, LABEL_LIST_DATA_NAME, labelData);
        forceData(projectPath, MILESTONE_LIST_DATA_NAME, milestoneData);
        forceData(projectPath, NODE_ORDER_DATA_NAME, nodeOrderData);

        if (!isTeam()) {
            syncSchedule();
            saveWorkflowUrlData();
            checkForUserInactivity();
        }

        sync(syncWorker, projectPath, projectXML);

        processDeferredDeletions(syncWorker);

        readChangesFromWorker(syncWorker);

        String discrepancyDataName = DataRepository.createDataName(projectPath,
            SyncDiscrepancy.DISCREPANCIES_DATANAME);
        dataRepository.putValue(discrepancyDataName, discrepancies);

        if (!whatIfMode || changes.isEmpty()) {
            String timestampDataName = DataRepository.createDataName(
                projectPath, TeamDataConstants.LAST_SYNC_TIMESTAMP);
            dataRepository.putValue(timestampDataName, new DateData());
        }

        // if a team project has switched to a data-based filter for the first
        // time, this could trigger the deletion of a large number of nodes
        // from the hierarchy.  Detect that potential scenario and scrub the
        // unneeded files from the team dashboard directory.
        if (isTeam() && !whatIfMode && !fullCopyMode
                && !syncWorker.getNodesDeleted().isEmpty()) {
            try {
                DashController.scrubDataDirectory();
                logger.info("HierarchySynchronizer scrubbed team "
                        + "dashboard directory.");
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private Map<String, String> syncMilestoneData(Element projectXML) {
        Element milestonesXML;
        StringBuilder milestonesMetadata;

        if (DashPackage.compareVersions(dumpFileVersion, "4.0.1") >= 0) {
            // if this is a new enough dump file, prepare to build a list of
            // milestone info that we can save to the data repository.
            milestonesMetadata = new StringBuilder();
            milestonesMetadata.append("<milestones>");
            milestonesXML = projectXML;

        } else {
            // if this is an older dump file, it probably means that someone on
            // the team has not upgraded, and they saved changes to the WBS with
            // an older version of the WBS Editor.  Check to see if we have
            // milestone definitions cached locally.
            String mm = getStringData(getData(projectPath,
                TeamDataConstants.PROJECT_MILESTONES_INFO));
            try {
                milestonesMetadata = null;
                milestonesXML = XMLUtils.parse(mm).getDocumentElement();
            } catch (Exception e) {
                // if we have no milestone definitions, neither cached nor in
                // the dump file, abort.
                return Collections.EMPTY_MAP;
            }
        }

        // scan the list of milestones and build the data we need.
        Map<String, String> result = new HashMap<String, String>();
        NodeList milestones = milestonesXML.getElementsByTagName("milestone");
        for (int i = 0; i < milestones.getLength(); i++) {
            Element m = (Element) milestones.item(i);
            String milestoneId = m.getAttribute(MILESTONE_ID_ATTR);
            String milestoneName = m.getAttribute("labelName");
            result.put(milestoneId, milestoneName);

            if (milestonesMetadata != null)
                milestonesMetadata.append(stripXmlHeader(XMLUtils.getAsText(m)));
        }

        // possibly save milestones to the data repository
        if (milestonesMetadata != null) {
            milestonesMetadata.append("</milestones>");
            putData(projectPath, TeamDataConstants.PROJECT_MILESTONES_INFO,
                StringData.create(milestonesMetadata.toString()));
        }

        return result;
    }
    private String stripXmlHeader(String xml) {
        int pos = xml.indexOf("?>");
        return (pos == -1 ? xml : xml.substring(pos + 2));
    }

    private void getLabelData(Element projectXML,
            Map<String, String> milestoneNames, ListData labelList,
            ListData milestoneList) {
        Map<String, Set> labelData = new TreeMap();
        collectLabelData(projectXML, labelData);

        Map<String, Set> milestoneData = new HashMap();
        boolean haveMilestoneData = DashPackage.compareVersions(
            dumpFileVersion, "4.0.1") >= 0;
        if (haveMilestoneData)
            collectMilestoneData(projectXML, milestoneData);

        crossReferenceLabelsAndMilestones(haveMilestoneData, milestoneNames,
            milestoneData, labelData);

        buildLabelList(labelData, labelList);
        buildMilestoneList(milestoneNames, milestoneData, milestoneList);
    }

    private void collectLabelData(Element node, Map<String, Set> dest) {
        String labels = node.getAttribute(LABELS_ATTR);
        if (XMLUtils.hasValue(labels)) {
            String nodeID = getCanonicalTaskID(node);
            String[] labelList = labels.split(",");
            for (int i = 0; i < labelList.length; i++) {
                String oneLabel = labelList[i];
                if (XMLUtils.hasValue(oneLabel)) {
                    Set taskIDs = dest.get(oneLabel);
                    if (taskIDs == null)
                        dest.put(oneLabel, taskIDs = new TreeSet());
                    taskIDs.add(nodeID);
                }
            }
        }

        List children = XMLUtils.getChildElements(node);
        for (Iterator i = children.iterator(); i.hasNext();)
            collectLabelData((Element) i.next(), dest);
    }

    private void collectMilestoneData(Element node, Map<String, Set> dest) {
        String milestoneID = node.getAttribute(MILESTONE_ID_ATTR);
        if (XMLUtils.hasValue(milestoneID)) {
            String nodeID = getCanonicalTaskID(node);
            Set taskIDs = dest.get(milestoneID);
            if (taskIDs == null)
                dest.put(milestoneID, taskIDs = new TreeSet());
            taskIDs.add(nodeID);
        }

        List children = XMLUtils.getChildElements(node);
        for (Iterator i = children.iterator(); i.hasNext();)
            collectMilestoneData((Element) i.next(), dest);
    }

    private String getCanonicalTaskID(Element node) {
        return projectID + ":" + node.getAttribute(ID_ATTR);
    }

    private void crossReferenceLabelsAndMilestones(
            boolean haveMilestoneData,
            // keys are milestone IDs, values are names
            Map<String, String> milestoneNames,
            // keys are milestone IDs, values are sets of task IDs
            Map<String, Set> milestoneData,
            // keys are labels, values are sets of task IDs
            Map<String, Set> labelData) {

        for (Entry<String, String> e : milestoneNames.entrySet()) {
            String milestoneID = e.getKey();
            String milestoneName = e.getValue();

            if (haveMilestoneData) {
                // if milestone data was present in the dump file, retrieve
                // the list of tasks that were unequivocally associated with
                // this milestone. Use this to replace the task list of the
                // corresponding label, to correct for errors where a user
                // might have manually typed a label with the same name as
                // a milestone.
                Set<String> tasksForMilestone = milestoneData.get(milestoneID);
                labelData.put(milestoneName, tasksForMilestone);

            } else {
                // if milestone data was not present in the dump file (because
                // it was saved by someone with an old copy of the WBS Editor),
                // infer the milestone associations by finding tasks that have
                // a label with the same name as the milestone.
                Set tasksForLabel = labelData.get(milestoneName);
                milestoneData.put(milestoneID, tasksForLabel);
            }
        }
    }

    private void buildLabelList(Map<String, Set> labelData, ListData labelList) {
        labelList.add("label:");
        for (Map.Entry<String, Set> e : labelData.entrySet()) {
            String label = e.getKey();
            Set taskIDs = e.getValue();
            if (taskIDs != null) {
                labelList.add("label:" + label);
                for (Iterator i = taskIDs.iterator(); i.hasNext();)
                    labelList.add((String) i.next());
            }
        }
    }

    private void buildMilestoneList(Map<String, String> milestoneNames,
            Map<String, Set> milestoneData, ListData milestoneList) {
        milestoneList.add("label:");
        for (Map.Entry<String, Set> e : milestoneData.entrySet()) {
            String milestoneID = e.getKey();
            Set taskIDs = e.getValue();
            String milestoneName = milestoneNames.get(milestoneID);
            if (taskIDs != null && milestoneName != null) {
                milestoneList.add("label:Milestone:" + milestoneName);
                milestoneList.add("milestoneID:" + projectID + ":"
                        + milestoneID);
                milestoneList.add("label_data:hidden");
                for (Iterator i = taskIDs.iterator(); i.hasNext();)
                    milestoneList.add((String) i.next());
            }
        }
    }

    private void saveHierarchyFilterInfo(Element projectXML) {
        StringBuilder result = new StringBuilder();

        result.append("<components>");
        collectHierarchyFilterInfo(projectXML, result);
        result.append("</components>");

        putData(this.projectPath, TeamDataConstants.PROJECT_COMPONENT_INFO,
            StringData.create(result.toString()));
    }

    private void collectHierarchyFilterInfo(Element xml, StringBuilder result) {
        for (Element node : XMLUtils.getChildElements(xml)) {
            if (COMPONENT_TYPES.contains(node.getTagName())) {
                // add an XML tag to our document describing this component.
                String name = node.getAttribute(NAME_ATTR);
                if (isPhaseName(name))
                    name = name + "_";
                String id = node.getAttribute(ID_ATTR);
                result.append("<comp " + NAME_ATTR + "='")
                    .append(XMLUtils.escapeAttribute(name))
                    .append("' " + ID_ATTR + "='")
                    .append(XMLUtils.escapeAttribute(id))
                    .append("'>");
                int len = result.length();

                // recurse and add information for child components.
                collectHierarchyFilterInfo(node, result);

                // close our tag
                if (len == result.length()) {
                    result.setLength(len-1);
                    result.append("/>");
                } else {
                    result.append("</comp>");
                }
            }
        }
    }


    private void collectNodeOrderData(Element node, ListData result) {
        String type = node.getTagName();
        if (!NODE_TYPES.contains(type))
            return;

        String taskIDs = node.getAttribute(TASK_ID_ATTR);
        if (XMLUtils.hasValue(taskIDs)) {
            for (String oneID : taskIDs.split(",")) {
                if (XMLUtils.hasValue(oneID))
                    result.add(oneID);
            }
        }

        for (Element child : XMLUtils.getChildElements(node))
            collectNodeOrderData(child, result);
    }


    private void getScheduleData(Element projectXML) {
        // our schedule sync strategy depends upon data introduced in the
        // dump file in version 3.1.0.  If an earlier version wrote the file,
        // don't attempt to retrieve schedule data for sync purposes.
        if (DashPackage.compareVersions(dumpFileVersion, "3.1.0") < 0)
            return;

        startDate = null;
        endWeek = -1;
        hoursPerWeek = 0;
        scheduleExceptions = null;
        List children = XMLUtils.getChildElements(projectXML);
        for (Iterator i = children.iterator(); i.hasNext();) {
            Element e = (Element) i.next();
            if (TEAM_MEMBER_TYPE.equals(e.getTagName())
                    && initials.equalsIgnoreCase(e.getAttribute(INITIALS_ATTR))) {
                saveScheduleData(e);
                return;
            }
        }
    }

    private void saveScheduleData(Element e) {
        startDate = null;
        if (e.hasAttribute(START_CALENDAR_DATE_ATTR)) {
            try {
                startDate = new SimpleDateFormat("yyyy-MM-dd").parse(e
                        .getAttribute(START_CALENDAR_DATE_ATTR));
            } catch (Exception ex) {}
        }
        if (startDate == null)
            startDate = roundDate(XMLUtils.getXMLDate(e, START_DATE_ATTR));
        if (e.hasAttribute(END_WEEK_ATTR))
            endWeek = XMLUtils.getXMLInt(e, END_WEEK_ATTR);
        hoursPerWeek = XMLUtils.getXMLNum(e, HOURS_PER_WEEK_ATTR);
        scheduleExceptions = new HashMap();
        List exceptionNodes = XMLUtils.getChildElements(e);
        for (Iterator i = exceptionNodes.iterator(); i.hasNext();) {
            Element exc = (Element) i.next();
            if (SCHEDULE_EXCEPTION_TAG.equals(exc.getTagName())) {
                int week = XMLUtils.getXMLInt(exc, SCHEDULE_EXCEPTION_WEEK_ATTR);
                double hours = XMLUtils.getXMLNum(exc,
                    SCHEDULE_EXCEPTION_HOURS_ATTR);
                scheduleExceptions.put(new Integer(week), new Double(hours));
            }
        }
    }

    private static Date roundDate(Date d) {
        if (d == null)
            return null;

        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.add(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
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
        List names = EVTaskList.getPreferredTaskListsForPath(dataRepository,
            projectPath);
        if (names == null || names.size() != 1) return;
        String taskListName = (String) names.get(0);
        if (!EVTaskListData.validName(taskListName)) return;
        if (!EVTaskListData.exists(dataRepository, taskListName)) return;

        // open the schedule.
        EVTaskListData tl = new EVTaskListData(taskListName, dataRepository,
                hierarchy, false);
        EVSchedule currentSchedule = tl.getSchedule();

        // possibly save changes back into the repository to record the
        // schedule that is in use.  The vast majority of the time, this will
        // have no effect.  But if an individual deleted their personal EV
        // schedule and created a new one, this will store the ID of the new
        // schedule for future use.
        if (tl.getID() != null) {
            forceData(projectPath, TeamDataConstants.PROJECT_SCHEDULE_NAME,
                StringData.create(taskListName));
            forceData(projectPath, TeamDataConstants.PROJECT_SCHEDULE_ID,
                StringData.create(tl.getID()));
        }

        // see if it is OK for us to change this schedule.  If the dates are
        // locked, then we must have created it, so we can change it.
        // Otherwise, if it looks like a default schedule (only one manual row),
        // it is fine to replace it.
        boolean isLocked = currentSchedule.areDatesLocked();
        boolean isDefault = (currentSchedule.getRowCount() == 1
                || currentSchedule.get(2).isAutomatic());
        if (!isLocked && !isDefault)
            return;

        // construct a schedule according to the specifications in the WBS.
        EVSchedule wbsSchedule = new EVSchedule(startDate, hoursPerWeek,
            endWeek, scheduleExceptions,
            currentSchedule.getLevelOfEffort(), true);

        // retrieve information about the last values we synced to the schedule
        ListData lastSyncedHoursList = asListData(tl.getMetadata(
            TeamDataConstants.PROJECT_SCHEDULE_SYNC_SCHEDULE));
        double syncPdt = asDouble(tl.getMetadata(
            TeamDataConstants.PROJECT_SCHEDULE_SYNC_PDT));

        // flags to keep track of what changes are made.
        boolean madeChange = false;
        boolean needSyncUpdate = false;

        if (isDefault || lastSyncedHoursList == null) {
            if (!currentSchedule.isEquivalentTo(wbsSchedule)) {
                currentSchedule.copyFrom(wbsSchedule);
                madeChange = true;
            }
            needSyncUpdate = true;

        } else {
            EVSchedule lastSyncedSchedule = new EVSchedule(lastSyncedHoursList,
                true);
            EVSchedule origSchedule = currentSchedule.copy();
            mergeSchedules(currentSchedule, lastSyncedSchedule, syncPdt);

            if (wbsSchedule.isEquivalentTo(lastSyncedSchedule))
                // the wbs schedule hasn't changed since our last sync, so
                // there are no new changes to incorporate.  (Note that it
                // was still necessary for us to call mergeSchedules though,
                // to compute the list of user discrepancies for reverse sync)
                madeChange = false;

            else if (origSchedule.isEquivalentTo(currentSchedule))
                // no changes were made to the schedule, but we still need
                // to update the sync data.
                needSyncUpdate = true;

            else
                madeChange = needSyncUpdate = true;
        }

        // make certain the dates in the schedule are locked
        if (!currentSchedule.areDatesLocked()) {
            currentSchedule.setDatesLocked(true);
            madeChange = true;
        }

        // save the "last synced hours" value for future reference
        tl.setMetadata(TeamDataConstants.PROJECT_SCHEDULE_SYNC_SCHEDULE,
            wbsSchedule.getSaveList().formatClean());
        // save the "last synced pdt" value for future reference
        tl.setMetadata(TeamDataConstants.PROJECT_SCHEDULE_SYNC_PDT,
            Double.toString(hoursPerWeek * 60));

        if (madeChange) {
            // record the timezone that we've used to sync the schedule
            String tz = TimeZone.getDefault().getID();
            tl.setMetadata(EVMetadata.TimeZone.ID, tz);
            // save the task list
            if (!whatIfMode)
                tl.save();
            changes.add("Updated the earned value schedule");
        } else if (needSyncUpdate) {
            // no changes were made, but we need to save sync data.  It's OK to
            // do this, even in what-if mode.
            tl.save();
        }
    }

    private void mergeSchedules(EVSchedule currentSchedule,
            EVSchedule lastSyncedSchedule, double syncPdt) {
        // look at the three schedule end dates, and determine when the merged
        // schedule should end.
        int currentEndWeek = getEndWeekOfSchedule(currentSchedule, startDate);
        int syncedEndWeek = getEndWeekOfSchedule(lastSyncedSchedule, startDate);
        int mergedEndWeek;
        if (currentEndWeek == syncedEndWeek)
            mergedEndWeek = endWeek;
        else
            mergedEndWeek = currentEndWeek;

        // Keep a list of manual edits the user has made, for reverse sync
        Map<Date,Double> userExceptions = new HashMap<Date,Double>();

        // Make a list of "merged exceptions."  Begin with the ones that came
        // from the current WBS.
        Map mergedExceptions = new HashMap(scheduleExceptions);

        // Now, look for manual edits in the users current schedule. By
        // this, we mean places where the user has changed the planned time
        // manually after receiving the last schedule from the WBS.
        for (int i = 0;  i < currentSchedule.getRowCount();  i++) {
            Period c = currentSchedule.get(i+1);
            // if we've reached the automatic part of the current schedule,
            // we can stop looking for manual edits
            if (c.isAutomatic())
                break;

            Date beg = c.getBeginDate();
            Date end = c.getEndDate();
            long midTime = (beg.getTime() + end.getTime()) / 2;
            Date mid = new Date(midTime);

            // if the time in this period matches the value that came from the
            // last synced schedule, it isn't a manual edit.
            double currentTime = c.planDirectTime();
            Period s = lastSyncedSchedule.get(mid);
            if (s != null && eq(s.planDirectTime(), currentTime))
                continue;

            // if the manual edit happened in a week that preceeds the start or
            // follows the end of our new schedule, it isn't relevant.
            int week = (int) ((midTime - startDate.getTime()) / WEEK_MILLIS);
            if (week < 0)
                continue;
            if (mergedEndWeek != -1 && week >= mergedEndWeek)
                continue;

            if (eq(currentTime, syncPdt)) {
                // the user erased an exception that they received from the
                // WBS.  We should do the same thing for the new WBS data.
                mergedExceptions.remove(week);
                userExceptions.put(mid, null);

            } else {
                // the user created a new exception in the schedule.  Add it
                // to our map.
                Double hours = new Double(currentTime / 60.0);
                mergedExceptions.put(week, hours);
                userExceptions.put(mid, hours);
            }
        }

        // build a schedule from the information we've merged, and reset the
        // current schedule to use that information.
        double levelOfEffort = currentSchedule.getLevelOfEffort();
        EVSchedule mergedSchedule = new EVSchedule(startDate, hoursPerWeek,
                mergedEndWeek, mergedExceptions, levelOfEffort, true);
        currentSchedule.copyFrom(mergedSchedule);

        // save information about user edits for delivery to the WBS
        if (!userExceptions.isEmpty())
            discrepancies.add(new SyncDiscrepancy.EVSchedule(userExceptions));
    }

    private int getEndWeekOfSchedule(EVSchedule s, Date zeroDate) {
        Date end = s.getEndDate();
        if (end == null)
            return -1;
        long diff = end.getTime() - zeroDate.getTime();
        if (diff < 0)
            return -1;
        else
            return (int) (diff / WEEK_MILLIS);
    }

    private void saveWorkflowUrlData() {
        Element workflowXml = openWorkflowXml();

        // if the current dump file doesn't support URLs or we couldn't
        // read the URLs for some reason, we don't make any changes to the
        // workflow URL data in the current dashboard.  This attempts to
        // gracefully handle the transition scenario where some person on the
        // team hasn't upgraded to the new version of the WBS editor, and they
        // save changes to the WBS (omitting URL data from both dump files).
        if (workflowXml != null) {
            workflowURLsSupported = true;
            ListData result = new ListData();
            collectWorkflowUrls(result, workflowXml, null, null);
            String dataName = processID + " /"
                    + TeamDataConstants.PROJECT_WORKFLOW_URLS_DATA_NAME;
            forceData(projectPath, dataName, result);
        }
    }

    private Element openWorkflowXml() {
        // Handling of workflow URLs depends upon data introduced in the
        // dump file in version 3.9.0. If an earlier version wrote the file,
        // don't attempt to retrieve workflow URL data for sync purposes.
        if (DashPackage.compareVersions(dumpFileVersion, "3.9.0") < 0)
            return null;

        InputStream in = null;
        Element result = null;
        if (workflowLocation != null) {
            try {
                in = new BufferedInputStream(workflowLocation.openStream());
                result = XMLUtils.parse(in).getDocumentElement();
            } catch (Exception e) {}
        }
        if (in != null) {
            try { in.close(); } catch (Exception e) {}
        }
        return result;
    }

    private void collectWorkflowUrls(ListData result, Element xml, String path,
            String taskId) {
        String urlList = xml.getAttribute(URL_ATTR);
        if (StringUtils.hasValue(urlList)) {
            if (StringUtils.hasValue(path))
                result.add(path + "///" + urlList);
            if (REGISTER_WORKFLOW_SUBTASK_IDS && StringUtils.hasValue(taskId))
                result.add(taskId + "///" + urlList);
        }

        for (Element child : XMLUtils.getChildElements(xml)) {
            String childType = child.getTagName();
            String childID = child.getAttribute(ID_ATTR);
            if (WORKFLOW_TYPE.equals(childType)) {
                if (StringUtils.hasValue(childID))
                    collectWorkflowUrls(result, child, childID, null);
            } else if (NODE_TYPES.contains(childType) && path != null) {
                String childName = child.getAttribute(NAME_ATTR);
                if (StringUtils.hasValue(childName))
                    collectWorkflowUrls(result, child, path + "/" + childName,
                        childID);
            }
        }
    }

    /**
     * When URLs are assigned to the individual tasks within a workflow, we
     * typically match concrete project tasks to prototypical workflow tasks by
     * name. If this variable is true, they will be matched by workflow source
     * ID as well.<ul>
     * 
     * <li>This can be beneficial if a workflow has been instantiated several
     * times, and then someone tweaks the name of a phase in the Common Team
     * Workflows window. All of the concrete instantiations of the workflow will
     * still pull up the script.</li>
     * 
     * <li>However, it can be confusing if people instantiate a workflow, then
     * repurpose the nodes inside. For example, someone decides that they don't
     * really want to use the "Postmortem" node in this instantiation of the
     * workflow; so they change it to a second Code task. Now the new Code task
     * would pull up the Postmortem script, which would seem like a bug.</li>
     * 
     * </ul>Considering the tradeoffs, the risk of confusion outweighs the
     * potential benefit, so this variable is currently set to <tt>false</tt>.
     */
    private static final boolean REGISTER_WORKFLOW_SUBTASK_IDS = false;


    /**
     * On the personal side, possibly update a data element indicating whether
     * the user is still actively working on this project.
     */
    private void checkForUserInactivity() {
        int userActivityAge = getUserActivityAgeRange();

        if (userActivityAge == USER_ACTIVITY_RECENT) {
            // if the user has been working on the project fairly recently, mark
            // the project as "not done."
            forceData(projectPath, TeamDataConstants.USER_DONE_TIMESTAMP, null);

        } else if (userActivityAge == USER_ACTIVITY_OLD
                && wbsDumpFileIsOld()) {
            // if the user hasn't done any work on the project in a long time,
            // and no one has edited the WBS recently, mark the project "done"
            logger.info("User appears to be finished with work on project '"
                    + projectPath + "'; marking the project as 'done.'");
            forceData(projectPath, TeamDataConstants.USER_DONE_TIMESTAMP,
                new DateData());
        }
    }

    /** Check to see if the user has recorded project data recently */
    private int getUserActivityAgeRange() {
        // get the activity timestamp.  If it is not present, we don't have
        // any idea when the user recorded project data last. Stay on the
        // safe side and assume that they are working on the project.
        SimpleData d = getData(projectPath,
            TeamDataConstants.USER_ACTIVITY_TIMESTAMP);
        if (!(d instanceof DateData))
            return USER_ACTIVITY_MODERATE;

        // get the age of the activity timestamp
        DateData activityDate = (DateData) d;
        long now = System.currentTimeMillis();
        long activityAge = now - activityDate.getValue().getTime();

        // if the activity timestamp is more than 30 days old, they are
        // definitely not working on the project anymore
        int activityCutoff = Settings.getInt("teamProject.inactivityAge", 30);
        if (activityAge > activityCutoff * DateUtils.DAYS)
            return USER_ACTIVITY_OLD;

        // if the activity timestamp is less than 7 days old, they are
        // definitely working on the project.
        activityCutoff = Settings.getInt("teamProject.recentActivityAge", 7);
        if (activityAge < activityCutoff * DateUtils.DAYS)
            return USER_ACTIVITY_RECENT;

        // their last activity was between 7 and 30 days ago.
        return USER_ACTIVITY_MODERATE;
    }
    private static final int USER_ACTIVITY_OLD = 1;
    private static final int USER_ACTIVITY_MODERATE = 2;
    private static final int USER_ACTIVITY_RECENT = 3;

    /** Return false if the WBS has been edited (by anyone) recently */
    private boolean wbsDumpFileIsOld() {
        // it will be uncommon for the dump file timestamp to be missing.
        // This will only occur for teams that are using the PDES, and who
        // have only edited the WBS with an old version of TeamTools.jar.
        // In that scenario, we assume that the WBS has not been edited in
        // a long time.
        if (dumpFileTimestamp == null)
            return true;

        // check to see if the dump file timestamp is more than 7 days old.
        long now = System.currentTimeMillis();
        long dumpFileAge = now - dumpFileTimestamp.getTime();
        int wbsCutoffAgeDays = Settings.getInt("wbs.inactivityAge", 7);
        return (dumpFileAge > wbsCutoffAgeDays * DateUtils.DAYS);
    }

    private static boolean eq(double a, double b) {
        return Math.abs(a - b) < 0.01;
    }

    private static boolean eq(Object a, Object b) {
        if (a == b) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private boolean setsIntersect(Set a, Set b) {
        if (a == null || a.isEmpty() || b == null || b.isEmpty())
            return false;
        for (Iterator i = b.iterator(); i.hasNext();) {
            if (a.contains(i.next()))
                return true;
        }
        return false;
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

        if (isUserCreatedNode(key))
            // this node was created manually by the user.  Don't bother it.
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
        deletionsPerformed = worker.getOriginalPaths(worker.getNodesDeleted());
        for (Iterator i = deletionsPerformed.iterator(); i.hasNext();)
            changes.add("Deleted '" + i.next() + "'");

        completionsPerformed = worker.getOriginalPaths(worker.getNodesCompleted());
        for (Iterator i = completionsPerformed.iterator(); i.hasNext();)
            changes.add("Marked '" + i.next() + "' complete");

        boolean foundNullChangeTokens = false;
        while (changes.remove(null))
            foundNullChangeTokens = true;

        if (changes.isEmpty()
                && (foundNullChangeTokens || !worker.getDataChanged().isEmpty()))
            changes.add(MISC_CHANGE_COMMENT);
    }

    private Map buildSyncActions() {
        HashMap result = new HashMap();
        result.put(PROJECT_TYPE, new SyncProjectNode());
        SyncSimpleNode s = new SyncSimpleNode(readOnlyNodeID, "_", taskNodeID);
        result.put(SOFTWARE_TYPE, s);
        result.put(DOCUMENT_TYPE, s);

        if (!isTeam()) {
            if (oldStyleSync)
                result.put(TASK_TYPE, new SyncOldTaskNode());
            else
                result.put(TASK_TYPE, new SyncTaskNode());
            result.put(PSP_TYPE, new SyncPSPTaskNode());
        }

        return result;
    }

    private Map syncActions;

    private String sync(SyncWorker worker, String pathPrefix, Element node)
        throws HierarchyAlterationException
    {
        ThreadThrottler.tick();
        if (whatIfBrief && !changes.isEmpty())
            return null;
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
    private static final String WORKFLOW_ID_ATTR = "wid";
    private static final String MILESTONE_ID_ATTR = "mid";
    private static final String VERSION_ATTR = "dumpFileVersion";
    private static final String SAVE_DATE_ATTR = "dumpTimestamp";
    private static final String LABELS_ATTR = "labels";
    private static final String PHASE_NAME_ATTR = "phaseName";
    private static final String SYNC_PHASE_NAME_ATTR = "syncPhaseName";
    private static final String EFF_PHASE_ATTR = "effectivePhase";
    private static final String TIME_ATTR = "time";
    private static final String SYNC_TIME_ATTR = "syncTime";
    private static final String URL_ATTR = "url";
    private static final String PRUNED_ATTR = "PRUNED";
    private static final String ROOT_NODE_PSEUDO_ID = "root";


    private static final String PROJECT_TYPE = "project";
    private static final String WORKFLOW_TYPE = "workflow";
    private static final String SOFTWARE_TYPE = "component";
    private static final String DOCUMENT_TYPE = "document";
    static final String PSP_TYPE = "psp";
    private static final String TASK_TYPE = "task";
    private static final String DEPENDENCY_TYPE = "dependency";
    private static final String NOTE_TYPE = "note";
    static final List COMPONENT_TYPES = Arrays.asList(new String[] {
            PROJECT_TYPE, SOFTWARE_TYPE, DOCUMENT_TYPE });
    static final List NODE_TYPES = Arrays.asList(new String[] {
        PROJECT_TYPE, SOFTWARE_TYPE, DOCUMENT_TYPE, PSP_TYPE, TASK_TYPE });

    private static final String TEAM_MEMBER_TYPE = "teamMember";
    private static final String INITIALS_ATTR = "initials";
    private static final String START_DATE_ATTR = "startDate";
    private static final String START_CALENDAR_DATE_ATTR = "startCalendarDate";
    private static final String END_WEEK_ATTR = "endWeek";
    private static final String HOURS_PER_WEEK_ATTR = "hoursPerWeek";
    private static final String SCHEDULE_EXCEPTION_TAG = "scheduleException";
    private static final String SCHEDULE_EXCEPTION_WEEK_ATTR = "week";
    private static final String SCHEDULE_EXCEPTION_HOURS_ATTR = "hours";
    private static final long WEEK_MILLIS = 7l * 24 * 60 * 60 * 1000;


    private static final String PROCESS_ID_DATA_NAME = "Process_ID";
    private static final String EST_TIME_DATA_NAME = "Estimated Time";
    private static final String ACT_TIME_DATA_NAME = "Time";
    private static final String LABEL_LIST_DATA_NAME =
        "Synchronized_Task_Labels";
    private static final String NODE_ORDER_DATA_NAME =
        "Synchronized_Task_ID_WBS_Order";
    private static final String MILESTONE_LIST_DATA_NAME =
        "Synchronized_Task_Milestones";
    private static final String TEAM_NOTE_KEY = HierarchyNoteManager.NOTE_KEY;
    private static final String TEAM_NOTE_LAST_SYNC_KEY =
        HierarchyNoteManager.NOTE_BASE_KEY;
    private static final String TEAM_NOTE_CONFLICT_KEY =
        HierarchyNoteManager.NOTE_CONFLICT_KEY;
    static final String PSP_SUBSET = "PSP To Date Subset Prefix";
    private static final String NEEDS_PSP_SUBSET_PROMPT = PSP_SUBSET
            + "///Needs Prompt";
    static final String MISC_CHANGE_COMMENT =
        "Updated miscellaneous project information";



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

        protected void maybeFixPreviouslyClobberedTeamTimeElement(String path) {
            if (!isTeam() || whatIfMode)
                return;

            String dataName = DataRepository.createDataName(path,
                ACT_TIME_DATA_NAME);
            SaveableData sd = data.getValue(dataName);
            if (sd instanceof DoubleData || sd == null) {
                dataRepository.restoreDefaultValue(dataName);
            }
        }

        protected void maybeSaveNote(String path, Element node, String nodeID) {
            HierarchyNote wbsNote = getNoteData(node);
            Map<String, HierarchyNote> currentNotes = HierarchyNoteManager
                    .getNotesForPath(data, path);
            HierarchyNote localNote = (currentNotes == null ? null
                    : currentNotes.get(TEAM_NOTE_KEY));
            HierarchyNote lastSyncNote = (currentNotes == null ? null
                    : currentNotes.get(TEAM_NOTE_LAST_SYNC_KEY));

            // if the value from the WBS has changed since last sync,
            if (!eq(lastSyncNote, wbsNote)) {

                if (eq(localNote, lastSyncNote)) {
                    // our local value agrees with the last synced value, so
                    // we should propagate the new WBS value along.
                    localNote = lastSyncNote = wbsNote;
                    saveNoteData(data, path,
                            TEAM_NOTE_KEY, wbsNote,
                            TEAM_NOTE_LAST_SYNC_KEY, wbsNote,
                            TEAM_NOTE_CONFLICT_KEY, null);

                } else if (wbsNote == null) {
                    // the note has been modified locally, but no longer exists
                    // in the WBS (possibly because the WBS was restored from a
                    // backup).  Clear conflict flags, and allow our local
                    // modification to be reverse synced.
                    saveNoteData(forceData(), path, TEAM_NOTE_CONFLICT_KEY,
                            null);

                } else if (!eq(ownerName, wbsNote.getAuthor())) {
                    // our local value has been modified since the last sync,
                    // but some other user has altered the note in the meantime.
                    // record the new value of the note as a conflict.
                    saveNoteData(forceData(), path, TEAM_NOTE_CONFLICT_KEY,
                            wbsNote);

                } else {
                    // the note value in the WBS was written by the user who is
                    // performing this sync.

                    // it is possible that the user has locally edited the note
                    // multiple times between sync operations.  In that case,
                    // their local value might be more recent than the value
                    // that has been reverse-synced to the WBS.  Check to see
                    // whether this is true.
                    boolean wbsIsOutOfDate = true;
                    if (localNote != null && wbsNote != null
                            && localNote.getTimestamp() != null
                            && wbsNote.getTimestamp() != null)
                        wbsIsOutOfDate = localNote.getTimestamp().after(
                            wbsNote.getTimestamp());

                    if (wbsIsOutOfDate) {
                        // The current user has already modified the note since
                        // the last reverse sync.  But it's still OK to clear
                        // any conflict flags, and make a note that the WBS
                        // now has a value we entered.  This is important to
                        // allow our local modification to reverse sync properly.
                        lastSyncNote = wbsNote;
                        saveNoteData(forceData(), path,
                                TEAM_NOTE_LAST_SYNC_KEY, wbsNote,
                                TEAM_NOTE_CONFLICT_KEY, null);

                    } else {
                        // If the WBS is equal to the local value, it means
                        // the local value has been reverse synced successfully.
                        // If the WBS is more recent than the local value, it
                        // means that the current user has opened the WBS and
                        // edited the value there.  Either way, all values
                        // should be synced to the WBS value.
                        localNote = lastSyncNote = wbsNote;
                        saveNoteData(data, path,
                                TEAM_NOTE_KEY, wbsNote,
                                TEAM_NOTE_LAST_SYNC_KEY, wbsNote,
                                TEAM_NOTE_CONFLICT_KEY, null);
                    }
                }
            }

            if (!eq(localNote, lastSyncNote)) {
                // add reverse sync data
                discrepancies.add(new SyncDiscrepancy.ItemNote(path, nodeID,
                        lastSyncNote, localNote));
            }
        }

        protected HierarchyNote getNoteData(Element node) {
            NodeList nl = node.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node child = nl.item(i);
                if (child instanceof Element) {
                    Element childElem = (Element) child;
                    if (childElem.getTagName().equals(NOTE_TYPE)) {
                        try {
                            return new HierarchyNote(childElem);
                        } catch (InvalidNoteSpecification e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return null;
        }

        protected void saveNoteData(DataContext data, String path, Object... notes) {
            Map<String, HierarchyNote> noteData = new HashMap<String, HierarchyNote>();
            for (int i = 0;  i < notes.length;  i += 2)
                noteData.put((String) notes[i], (HierarchyNote) notes[i+1]);
            HierarchyNoteManager.saveNotesForPath(data, path, noteData);
        }

    }

    private class SyncProjectNode extends SyncNode {

        public boolean syncNode(SyncWorker worker, String pathPrefix,
                Element node) throws HierarchyAlterationException {

            try {
                String projIDs = node.getAttribute(TASK_ID_ATTR);
                setTaskIDs(pathPrefix, cleanupProjectIDs(projIDs));
                maybeFixPreviouslyClobberedTeamTimeElement(pathPrefix);
                if (!isTeam())
                    maybeSaveNote(pathPrefix, node, ROOT_NODE_PSEUDO_ID);
            } catch (Exception e) {}

            return super.syncNode(worker, pathPrefix, node);
        }

        public String getName(Element node) { return null; }
    }

    private class SyncSimpleNode extends SyncNode {

        String templateID, suffix;
        Collection compatibleTemplateIDs;
        String forcedRootChildSuffix = " Task";

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
            List matchingNodes = null;

            // If a node exists already in the desired location, and we're not
            // doing a team sync, we may need to move the node out of the way.
            if (currentTemplateID != null && !isTeam()) {
                boolean needsMove = false;

                if (!compatibleTemplateIDs.contains(currentTemplateID)) {
                    // the current node is not compatible with the template
                    // we need to create.  So the current node needs to be
                    // moved out of the way.
                    needsMove = true;

                } else if (!StringUtils.hasValue(currentNodeID)) {
                    // the node that is currently in place has no WBS ID - it
                    // was created by the user. But it's template ID is
                    // compatible, so we might be able to reuse it.  First,
                    // check to see if a node elsewhere has the ID we need.
                    // If so, that node elsewhere takes precedence, and we
                    // should move this user node out of the way.
                    matchingNodes = getCompatibleNodesWithID(nodeID);
                    if (!matchingNodes.isEmpty())
                        needsMove = true;

                } else if (!nodeID.equals(currentNodeID)) {
                    // the node that is in the way represents some other WBS
                    // node. Check to see if that node still exists elsewhere
                    // in the team WBS.  If it does, we can't co-opt this node.
                    // If it doesn't, the node in question was probably deleted
                    // from the WBS, and a new node with the exact same name
                    // was created in its place. This is a common misuse case
                    // that occurs when someone "didn't really" intend to
                    // delete a node - so in that case, we will silently reuse
                    // the old node to have the new ID.
                    if (isNodeStillPresentInWbs(currentNodeID))
                        needsMove = true;
                }

                if (needsMove) {
                    moveNodeOutOfTheWay(worker, path);
                    currentTemplateID = currentNodeID = null;
                }
            }

            // if the target node does not exist, try to find a match
            // elsewhere in the hierarchy to move to this location.
            if (currentTemplateID == null && !isTeam()) {
                if (matchingNodes == null)
                    matchingNodes = getCompatibleNodesWithID(nodeID);
                PropertyKey sourceNode = getBestNodeToMove(matchingNodes);
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
                nodeWasAdded(worker, path, node);

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

        protected void nodeWasAdded(SyncWorker worker, String path, Element node) {}

        protected List getCompatibleNodesWithID(String nodeID) {
            List result = findHierarchyNodesByID(nodeID);
            for (Iterator i = result.iterator(); i.hasNext();) {
                PropertyKey node = (PropertyKey) i.next();
                String templateID = getTemplateIDForKey(node);
                if (!compatibleTemplateIDs.contains(templateID))
                    i.remove();
            }
            return result;
        }

        private PropertyKey getBestNodeToMove(List movableNodes) {
            if (movableNodes == null || movableNodes.isEmpty())
                return null;

            PropertyKey result = (PropertyKey) movableNodes.get(0);

            // if there is more than one node to choose from, find the node
            // with the most actual time (that may not be a foolproof method,
            // but this is only a temporary legacy issue anyway)
            if (movableNodes.size() > 1) {
                double maxTime = -1;
                for (Iterator i = movableNodes.iterator(); i.hasNext();) {
                    PropertyKey node = (PropertyKey) i.next();
                    double nodeTime = getTotalActualTimeForIndivNode(node);
                    if (nodeTime > maxTime) {
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
            String workflowID = node.getAttribute(WORKFLOW_ID_ATTR);
            try {
                putData(path, TeamDataConstants.WBS_ID_DATA_NAME,
                    StringData.create(nodeID));
                if (!isPrunedNode(node))
                    setTaskIDs(path, taskID);
                if (workflowURLsSupported)
                    putData(path, TeamDataConstants.WORKFLOW_ID_DATA_NAME,
                        (XMLUtils.hasValue(workflowID)
                                ? StringData.create(workflowID) : null));
                maybeFixPreviouslyClobberedTeamTimeElement(path);
            } catch (Exception e) {}
            if (!isTeam() && !isPrunedNode(node)) {
                maybeSaveNodeSize(path, node);
                maybeSaveDependencies(path, node);
                maybeSaveNote(path, node, nodeID);
            }
        }

        protected boolean isPrunedNode(Element node) {
            return XMLUtils.hasValue(node.getAttribute(PRUNED_ATTR));
        }

        public String getName(Element node) {
            String result = super.getName(node);
            if (isPhaseName(result)) {
                if (suffix.length() > 0)
                    result = result + suffix;
                else if (node.getParentNode() == projectXML)
                    result = result + forcedRootChildSuffix;
            }
            return result;
        }

        protected void maybeSaveTimeValue(SyncWorker worker, String path,
                Element node) {
            double time = parseTime(node);
            if (!isPrunedNode(node) && time >= 0) {
                if (undoMarkTaskComplete(worker, path))
                    changes.add("Marked '" + path + "' incomplete.");
                if (okToChangeTimeEstimate(path)) {
                    // FIXME: if a user subdivides a task in their personal
                    // plan, and then the estimate is changed in the WBS, this
                    // branch will never execute and the two task estimates
                    // will remain out of sync indefinitely.
                    worker.setLastReverseSyncedValue(parseSyncTime(node) * 60);
                    putData(path, EST_TIME_DATA_NAME, new DoubleData(time * 60));
                }
            }

            PropertyKey key = hierarchy.findExistingKey(path);
            double finalTime = getTotalPlanTimeForIndivNode(key) / 60;
            if (eq(finalTime, time) == false)
                discrepancies.add(new SyncDiscrepancy.PlanTime(path,
                        getWbsIdForPath(path), finalTime));
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
            return parseTime(timeAttr);
        }

        protected double parseSyncTime(Element node) {
            String timeAttr = node.getAttribute(SYNC_TIME_ATTR);
            return Math.max(0, parseTime(timeAttr));
        }

        protected double parseTime(String timeAttr) {
            if (timeAttr == null) return -1;
            int beg = timeAttr.toLowerCase().indexOf(initialsPattern);
            if (beg == -1) return -1;
            beg += initialsPattern.length();
            int end = timeAttr.indexOf(',', beg);
            if (end == -1) return -1;
            try {
                return Double.parseDouble(timeAttr.substring(beg, end));
            } catch (Exception e) {}

            return -1;
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

        protected void maybeSaveNodeSize(String path, Element node) {
            // see if this node has size data.
            String units = node.getAttribute("sizeUnits");
            if (units == null || units.length() == 0)
                return;

            // determine what units qualifier should be used to save the data.
            String storeUnits = units;
            if (units.equals("LOC")) {
                if (!shouldSaveLOCData(path, node)) return;
                storeUnits = "New & Changed LOC";
            }

            // check to see if any size data exists for this node
            SimpleData d = getData(path, EST_SIZE_DATA_NAME);
            if (d != null && d.test()) return;
            d = getData(path, SIZE_UNITS_DATA_NAME);
            if (d != null && d.test()) return;

            // find out whether this individual is a contributor to the
            // construction of the given object
            double ratio = getTimeRatio(node, units);
            if (ratio == 0) return;

            // calculate the percentage of the object construction time
            // contributed by this individual
            double size;
            try {
                String sizeStr = node.getAttribute("sizeNC");
                size = Double.parseDouble(sizeStr);
                size = size * ratio;
            } catch (NumberFormatException nfe) {
                return;
            }

            // save the size data to the project.
            putData(path, EST_SIZE_DATA_NAME, new DoubleData(size));
            putData(path, SIZE_UNITS_DATA_NAME, StringData.create(storeUnits));
        }

        protected boolean shouldSaveLOCData(String path, Element node) {
            return false;
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

    protected void forceData(String dataPrefix, String name, SimpleData value) {
        String dataName = DataRepository.createDataName(dataPrefix, name);
        if (data instanceof SyncWorker) {
            SyncWorker worker = (SyncWorker) data;
            worker.doPutValueForce(dataName, value);
        } else {
            data.putValue(dataName, value);
        }
    }

    private class ForceDataContext implements DataContext {
        public SimpleData getSimpleValue(String name) {
            return data.getSimpleValue(name);
        }
        public SaveableData getValue(String name) {
            return data.getValue(name);
        }
        public void putValue(String name, SaveableData value) {
            SimpleData v = null;
            if (value != null)
                v = value.getSimpleValue();
            forceData("/", name, v);
        }
    }

    protected DataContext forceData() {
        if (whatIfMode)
            return new ForceDataContext();
        else
            return data;
    }

    private String getStringData(SimpleData val) {
        return (val == null ? null : val.format());
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

    private boolean testData(SimpleData d) {
        return (d == null ? false : d.test());
    }

    private static ListData asListData(String d) {
        if (StringUtils.hasValue(d))
            return new ListData(d);
        else
            return null;
    }

    private static double asDouble(String d) {
        if (StringUtils.hasValue(d)) {
            try {
                return Double.parseDouble(d);
            } catch (Exception e) {}
        }
        return Double.NaN;
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

    static String getEffectivePhaseDataName(String processID) {
        return processID + " /Effective_Phase";
    }



    private class SyncTaskNode extends SyncSimpleNode {
        private String phaseDataName;
        public SyncTaskNode() {
            super(taskNodeID, (oldStyleSync ? " Task" : ""), readOnlyNodeID);
            phaseDataName = getEffectivePhaseDataName(processID);
        }
        public boolean syncNode(SyncWorker worker, String pathPrefix, Element node)
            throws HierarchyAlterationException
        {
            if (super.syncNode(worker, pathPrefix, node) == false)
                return false;

            if (isPrunedNode(node))
                return true;

            String path = getPath(pathPrefix, node);
            maybeSaveInspSizeData(path, node);
            maybeClearTimeValue(worker, path, node);

            String phaseName = node.getAttribute(PHASE_NAME_ATTR);
            if (XMLUtils.hasValue(phaseName) == false) {
                // If the "phase" for this task is null, we don't need to add
                // any phase underneath this node.  (This can occur if a task
                // in the WBS is used as a parent;  the parent task will not
                // have any phase designator.)
                String effectivePhase = node.getAttribute(EFF_PHASE_ATTR);
                syncTaskEffectivePhase(worker, node, path, effectivePhase);
                return true;
            } else
                return syncTaskPhase(worker, node, path, phaseName);
        }

        protected boolean syncTaskPhase(SyncWorker worker, Element node,
                String path, String phaseName)
                throws HierarchyAlterationException {
            maybeSaveTimeValue(worker, path, node);
            syncTaskEffectivePhase(worker, node, path, phaseName);
            return true;
        }

        protected void syncTaskEffectivePhase(SyncWorker worker,
                Element node, String path, String phaseName)
                throws HierarchyAlterationException {
            if (XMLUtils.hasValue(phaseName) && !"Unknown".equals(phaseName)) {
                worker.setLastReverseSyncedValue(StringData.create(node
                        .getAttribute(SYNC_PHASE_NAME_ATTR)));
                putData(path, phaseDataName, StringData.create(phaseName));
            }

            // after the sync, if our phase type disagrees with the WBS, make
            // a note of the discrepancy for reverse sync purposes.
            String finalPhase = getStringData(getData(path, phaseDataName));
            if (StringUtils.hasValue(finalPhase) && !finalPhase.startsWith("?")
                    && !finalPhase.equals(phaseName))
                discrepancies.add(new SyncDiscrepancy.NodeType(path,
                        getWbsIdForPath(path), finalPhase));
        }

        protected boolean undoMarkTaskComplete(SyncWorker worker, String path) {
            return worker.markLeafIncomplete(path);
        }

        private void maybeSaveInspSizeData(String path, Element node) {
            // if the user doesn't want us to sync the sizes of inspected
            // work products, abort.
            if (!syncInspectedSizes)
                return;

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

        private void maybeClearTimeValue(SyncWorker worker, String path,
                Element node) {
            // when a user opens the WBS and subdivides an existing task, the
            // next "sync to WBS" operation will copy those subtasks into the
            // plan. In most cases, we will need to delete the estimated time
            // value from the parent task (which used to be a leaf but now has
            // children).  Otherwise, the user is likely to end up with a
            // top-down-bottom-up mismatch error in their personal plan.

            if (parseTime(node) > 0)
                return;  // this node has a time estimate in the WBS; abort

            if (isHierarchyLeaf(path))
                return;  // not a parent node, abort

            SimpleData timeEstimate = getData(path, EST_TIME_DATA_NAME);
            if (timeEstimate == null)
                return;  // no time estimate is present, nothing to clear.

            // check to see if the time estimate agrees with the last synced
            // value.  If so, it means that the time estimate came from the
            // WBS.  And since the WBS doesn't have a time estimate anymore,
            // we should delete the estimate from this plan to stay in sync.
            SimpleData lastSyncTimeEstimate = getData(path, AbstractSyncWorker
                    .syncDataName(EST_TIME_DATA_NAME));
            if (timeEstimate.equals(lastSyncTimeEstimate))
                putData(path, EST_TIME_DATA_NAME, null);
        }

        protected boolean okToChangeTimeEstimate(String path) {
            // in the new style framework, we only want to record estimates on
            // leaf tasks. If this code is executing, the WBS thinks that
            // "path" refers to a leaf. However, the user might have subdivided
            // the task in their own dashboard, making it a parent. We need to
            // check for that scenario, and decline to write an estimate into
            // the non-leaf parent.
            return isHierarchyLeaf(path);
        }

        protected boolean isHierarchyLeaf(String path) {
            PropertyKey key = hierarchy.findExistingKey(path);
            if (key == null) return false;

            int numKids = hierarchy.getNumChildren(key);
            return (numKids == 0);
        }

        @Override
        protected boolean shouldSaveLOCData(String path, Element node) {
            return "Code".equalsIgnoreCase(node.getAttribute(PHASE_NAME_ATTR));
        }

    }

    private class SyncOldTaskNode extends SyncTaskNode {
        SyncOldTaskNode() {
            phaseIDs = initPhaseIDs(processID);
        }

        protected boolean syncTaskPhase(SyncWorker worker, Element node,
                String path, String phaseName)
                throws HierarchyAlterationException {

            path = path + "/" + phaseName;
            String templateID = (String) phaseIDs.get(phaseName);
            if (templateID == null) {
                // there is a problem.
                changes.add("Could not create '" + path
                        + "' - unrecognized process phase");
                return false;
            }
            String currentID = getTemplateIDForPath(path);
            if (currentID == null) {
                worker.addTemplate(path, templateID);
                maybeSaveTimeValue(worker, path, node);
                changes.add("Created '"+path+"'");
                return true;
            } else if (templateID.equals(currentID)) {
                // the node exists with the given name.
                maybeSaveTimeValue(worker, path, node);
                return true;
            } else {
                // there is a problem.
                changes.add("Could not create '" + path
                        + "' - existing node is in the way");
                return false;
            }
        }

        protected void syncTaskEffectivePhase(SyncWorker worker, Element node,
                String path, String effPhase)
                throws HierarchyAlterationException {}

        protected void filterOutKnownChildren(Element node,
                List childrenToDelete) {
            String phaseName = node.getAttribute(PHASE_NAME_ATTR);
            childrenToDelete.remove(phaseName);
            super.filterOutKnownChildren(node, childrenToDelete);
        }

        protected boolean okToChangeTimeEstimate(String path) {
            return true;
        }

    }




    private static final String EST_SIZE_DATA_NAME =
        "Sized_Objects/0/Estimated Size";
    private static final String SIZE_UNITS_DATA_NAME =
        "Sized_Objects/0/Sized_Object_Units";



    private class SyncPSPTaskNode extends SyncSimpleNode {
        public SyncPSPTaskNode() {
            super("PSP2.1", (oldStyleSync ? " Task" : ""), null);
            compatibleTemplateIDs.add("PSP0.1");
            compatibleTemplateIDs.add("PSP1");
            compatibleTemplateIDs.add("PSP1.1");
            compatibleTemplateIDs.add("PSP2");
        }

        public void syncData(SyncWorker worker, String path, Element node) {
            super.syncData(worker, path, node);
            if (!isPrunedNode(node)) {
                maybeSaveTimeValue(worker, path, node);
            }
            if (testData(getData(path, NEEDS_PSP_SUBSET_PROMPT))) {
                String rollup = pspSubsetSelections.get(path);
                if (rollup != null && rollup.length() > 0) {
                    putData(path, PSP_SUBSET, StringData.create(rollup));
                    putData(path, NEEDS_PSP_SUBSET_PROMPT, null);
                    changes.add("Configured the task '" + path
                            + "' to draw its historical data from '" + rollup
                            + "'");
                } else {
                    pspTasksNeedingSubsetPrompt.add(path);
                    changes.add(null);
                }
            }
        }

        protected boolean okToChangeTimeEstimate(String path) {
            return (getData(path, "Planning/Completed") == null);
        }

        protected boolean undoMarkTaskComplete(SyncWorker worker, String path) {
            return worker.markPSPTaskIncomplete(path);
        }

        @Override
        protected void maybeSaveNodeSize(String path, Element node) {
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

        @Override
        protected void nodeWasAdded(SyncWorker w, String path, Element node) {
            if (pspToDateSubset != null)
                putData(path, PSP_SUBSET, StringData.create(pspToDateSubset));
            if (promptForPspToDateSubset)
                putData(path, NEEDS_PSP_SUBSET_PROMPT, ImmutableDoubleData.TRUE);
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
        // check to see if this node represents a phase from the given phase
        // list. If so, sum up the time that is attached to this node
        String phaseName = node.getAttribute(PHASE_NAME_ATTR);
        if (phaseName == null || phaseName.length() == 0)
            phaseName = node.getTagName();
        String timeAttr = node.getAttribute(TIME_ATTR);
        if (phaseList.contains(phaseName) &&
            timeAttr != null && timeAttr.length() != 0)
            addTimeData(timeAttr);
        // recurse and sum up time attached to the children of this node.
        NodeList children = node.getChildNodes();
        int len = children.getLength();
        Node child;
        for (int i = 0;   i < len;   i++) {
            child = (Node) children.item(i);
            if (child instanceof Element)
                sumUpConstructionPhases((Element) child, phaseList);
        }
        // since we are summing up time for the entire team, we also need to
        // consider nodes that were pruned from the hierarchy because they were
        // assigned to other individuals.
        for (Element prunedChild : getChildrenPrunedFrom(node))
            sumUpConstructionPhases(prunedChild, phaseList);
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
            if (initials.equalsIgnoreCase(who))
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
