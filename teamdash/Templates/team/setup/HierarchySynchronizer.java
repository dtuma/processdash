import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pspdash.DashController;
import pspdash.HierarchyAlterer;
import pspdash.PSPProperties;
import pspdash.PropertyKey;
import pspdash.TinyCGIException;
import pspdash.XMLUtils;
import pspdash.HierarchyAlterer.HierarchyAlterationException;
import pspdash.data.DataRepository;
import pspdash.data.DoubleData;
import pspdash.data.SimpleData;

public class HierarchySynchronizer {

    private static final String SYNC_TEAM = "(team)";

    private PSPProperties hierarchy;
    private DataRepository data;
    private String projectPath;
    private String processID;
    private String initials, initialsPattern;
    private Element projectXML;
    private ArrayList changes;

    private String readOnlyNodeID;
    private String taskNodeID;
    private boolean whatIfMode = true;


    /** Create a hierarchy synchronizer for a team project */
    public HierarchySynchronizer(String projectPath,
                                 String processID,
                                 File wbsFile,
                                 String initials,
                                 boolean fullCopyMode,
                                 PSPProperties hierarchy,
                                 DataRepository data) throws IOException {
        this.projectPath = projectPath;
        this.processID = processID;
        this.hierarchy = hierarchy;
        this.data = data;

        if (initials == null) { // team
            this.initials = this.initialsPattern = SYNC_TEAM;
            this.readOnlyNodeID = processID + "/TeamNode";
            this.taskNodeID = null;
        } else { // individual
            this.initials = initials;
            this.initialsPattern = "," + initials + "=";
            this.readOnlyNodeID = processID + "/IndivReadOnlyNode";
            this.taskNodeID = processID + "/IndivEmptyNode";
        }

        openWBS(wbsFile);
        if (isTeam()) fullCopyMode = true;
        pruneWBS(projectXML, fullCopyMode);
    }

    public void setWhatIfMode(boolean whatIf) {
        this.whatIfMode = whatIf;
    }

    public boolean isTeam() {
        return initials == SYNC_TEAM;
    }

    public List getChanges() {
        return changes;
    }


    public void dumpChanges(PrintWriter out) {
        Iterator i = changes.iterator();
        while (i.hasNext()) {
            out.print(i.next());
            out.println();
        }
    }

    private void openWBS(File wbsFile) throws IOException {
        if (wbsFile == null || !wbsFile.exists())
            throw new IOException
                ("Could not find the file containing the work breakdown " +
                 "structure for this team project.");

        try {
            Document doc = XMLUtils.parse(new FileInputStream(wbsFile));
            projectXML = doc.getDocumentElement();
        } catch (Exception e) {
            throw new IOException
                ("Could not read the file containing the work breakdown " +
                 "structure for this team project.");
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

        // assume this node is prunable until we determine otherwise.
        boolean prunable = true;

        // if this is a task (PSP or otherwise),
        if (TASK_TYPE.equals(type) || PSP_TYPE.equals(type)) {
            // and we are doing a team synchronization, prune it.
            if (initials == SYNC_TEAM) return PRUNE;
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
        if (time != null && time.indexOf(initialsPattern) != -1)
            prunable = false;

        return (prunable ? PRUNE : DONT_PRUNE);
    }

    public void dumpXML(Writer out) throws IOException {
        out.write(String.valueOf(projectXML));
    }

    public void sync() throws IOException {
        if (DashController.isHierarchyEditorOpen())
            throw new IOException("You must close the hierarchy editor "+
                                  "before you can synchronize this project.");

        changes = new ArrayList();
        syncActions = buildSyncActions();
        initPhaseIDs();
        HierarchyAlterer alterer = DashController.getHierarchyAlterer();

        try {
            sync(alterer, projectPath, projectXML);
        } catch (HierarchyAlterationException e) {
            throw new TinyCGIException(500, e.getMessage());
        }
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

    private void sync(HierarchyAlterer alterer, String pathPrefix, Element node)
        throws HierarchyAlterer.HierarchyAlterationException
    {
        String type = node.getTagName();
        SyncNode s = (SyncNode) syncActions.get(type);
        if (s != null)
            s.syncNode(alterer, pathPrefix, node);
    }

    private static final String NAME_ATTR = "name";
    private static final String ID_ATTR = "id";
    //private static final String TYPE_ATTR = "type";
    private static final String PHASE_NAME_ATTR = "phaseName";
    private static final String PHASE_TYPE_ATTR = "phaseType";
    private static final String TIME_ATTR = "time";


    private static final String PROJECT_TYPE = "project";
    private static final String SOFTWARE_TYPE = "component";
    private static final String DOCUMENT_TYPE = "document";
    private static final String PSP_TYPE = "psp";
    private static final String TASK_TYPE = "task";
    private static final List NODE_TYPES = Arrays.asList(new String[] {
        PROJECT_TYPE, SOFTWARE_TYPE, DOCUMENT_TYPE, PSP_TYPE, TASK_TYPE });

    private static final String WBS_ID_DATA_NAME = "WBS_Unique_ID";
    private static final String EST_TIME_DATA_NAME = "Estimated Time";


    private class SyncNode {

        public boolean syncNode(HierarchyAlterer alterer, String pathPrefix, Element node)
            throws HierarchyAlterationException
        {
            syncChildren(alterer, pathPrefix, node);
            return true;
        }

        public void syncChildren(HierarchyAlterer alterer, String pathPrefix,
                                 Element node)
            throws HierarchyAlterationException
        {
            pathPrefix = getPath(pathPrefix, node);
            NodeList childNodes = node.getChildNodes();
            int len = childNodes.getLength();
            for (int i = 0;   i < len;   i++) {
                Node child = childNodes.item(i);
                if (child instanceof Element)
                    sync(alterer, pathPrefix, (Element) child);
            }
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
    }

    private class SyncProjectNode extends SyncNode {
        public String getName(Element node) { return null; }
    }

    private class SyncSimpleNode extends SyncNode {

        String templateID, suffix;

        public SyncSimpleNode(String templateID, String suffix) {
            this.templateID = templateID;
            this.suffix = suffix;
        }

        public boolean syncNode(HierarchyAlterer alterer, String pathPrefix, Element node)
            throws HierarchyAlterationException {
            String path = getPath(pathPrefix, node);
            String currentID = getIDForPath(path);
            if (currentID == null) {
                if (!whatIfMode) {
                    alterer.addTemplate(path, templateID);
                    syncData(path, node);
                }
                changes.add("Created '"+path+"' ("+templateID+")");
            } else if (templateID.equals(currentID)) {
                // the node exists with the given name.  Just sync its data.
                if (!whatIfMode)
                    syncData(path, node);
            } else {
                // there is a problem.
                changes.add("Could not create '"+path+"' - existing node is in the way");
                return false;
            }

            syncChildren(alterer, pathPrefix, node);
            return true;
        }
        public void syncData(String path, Element node) {
             String nodeID = node.getAttribute(ID_ATTR);
             try {
                 putData(path, WBS_ID_DATA_NAME, new DoubleData(nodeID));
            } catch (Exception e) {}
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

            SimpleData d = getData(path, EST_TIME_DATA_NAME);
            if (d == null || !d.test())
                putData(path, EST_TIME_DATA_NAME, new DoubleData(time * 60));
        }

        protected double parseTime(Element node) {
            String timeAttr = node.getAttribute(TIME_ATTR);
            if (timeAttr == null) return 0;
            int beg = timeAttr.indexOf(initialsPattern);
            if (beg == -1) return 0;
            beg += initialsPattern.length();
            int end = timeAttr.indexOf(',', beg);
            if (end == -1) return 0;
            try {
                return Double.parseDouble(timeAttr.substring(beg, end));
            } catch (Exception e) {}

            return 0;
        }
    }

    private SimpleData getData(String dataPrefix, String name) {
        String dataName = DataRepository.createDataName(dataPrefix, name);
        return data.getSimpleValue(dataName);
    }

    protected void putData(String dataPrefix, String name, SimpleData value) {
        String dataName = DataRepository.createDataName(dataPrefix, name);
        data.userPutValue(dataName, value);
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
        public boolean syncNode(HierarchyAlterer alterer, String pathPrefix, Element node)
            throws HierarchyAlterationException
        {
            if (super.syncNode(alterer, pathPrefix, node) == false)
                return false;

            String path = getPath(pathPrefix, node);
            String phaseName = node.getAttribute(PHASE_NAME_ATTR);
            String templateID = (String) phaseIDs.get(phaseName);
            if (templateID == null) {
                changes.add("Could not find template for '"+phaseName+"'");
                return false;
            }
            path = path + "/" + phaseName;
            String currentID = getIDForPath(path);
            if (currentID == null) {
                if (!whatIfMode) {
                    alterer.addTemplate(path, templateID);
                    maybeSaveTimeValue(path, node);
                }
                changes.add("Created '"+path+"' ("+templateID+")");
            } else if (templateID.equals(currentID)) {
                // the node exists with the given name.
                if (!whatIfMode)
                    maybeSaveTimeValue(path, node);
            } else {
                // there is a problem.
                changes.add("Could not create '"+path+"' - existing node is in the way");
                return false;
            }

            return true;
        }
    }

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
            for (int i = 0;   i < sizeDataNames.length;   i++) {
                SimpleData d = getData(path, sizeDataNames[i]);
                if (d != null && d.test()) return;
            }

            // save the size data to the project.
            for (int i = 0;   i < sizeAttrNames.length;   i++)
                putNumber(path, sizeDataNames[i],
                          node.getAttribute(sizeAttrNames[i]));
        }

        private void putNumber(String path, String name, String value) {
            try {
                if (value == null || value.length() == 0)
                    value = "0";
                putData(path, name, new DoubleData(value));
            } catch (Exception e) {}
        }
    }

    private static final String[] sizeAttrNames = new String[] {
        "sizeBase", "sizeDel", "sizeMod", "sizeAdd", "sizeReu", "sizeNC" };
    private static final String[] sizeDataNames = new String[] {
        "Estimated Base LOC",
        "Estimated Deleted LOC",
        "Estimated Modified LOC",
        "New Objects/0/LOC",
        "Reused Objects/0/LOC",
        "Estimated New & Changed LOC" };


    private boolean isPhaseName(String name) {
        return phaseIDs.containsKey(name);
    }

    private String getIDForPath(String path) {
        PropertyKey key = hierarchy.findExistingKey(path);
        if (key == null) return null;

        String actualID = hierarchy.getID(key);
        return (actualID == null ? "" : actualID);
    }

    private static final String[] docSizeUnits = {
        "Text Pages", "Reqts Pages", "HLD Pages", "DLD Lines" };
    private static final String[] constructionPhaseTypes = {
        "STP", "ITP", "TD", "MGMT", "STRAT", "PLAN", "REQ", "HLD", "DLD",
        "DLDR", "CODE", "CR", "COMP", "DOC" };

    private double constrPhaseTotal, constrPhasePersonal;
    private void sumUpConstructionPhases(Element node) {
        String attr = node.getAttribute(TIME_ATTR);
        if (attr != null && attr.length() != 0)
            addTimeData(attr);
        NodeList children = node.getChildNodes();
        int len = children.getLength();
        Node child;
        for (int i = 0;   i < len;   i++) {
            child = (Node) children.item(i);
            if (child instanceof Element)
                sumUpConstructionPhases((Element) child);
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
            constrPhaseTotal += amount;
            if (initials.equals(who))
                constrPhasePersonal += amount;
        } catch (NumberFormatException nfe) {}
    }

}
