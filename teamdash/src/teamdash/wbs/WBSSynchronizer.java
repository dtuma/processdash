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

package teamdash.wbs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.VersionUtils;

import teamdash.XMLUtils;
import teamdash.team.TeamMember;
import teamdash.team.TeamMemberList;
import teamdash.team.WeeklySchedule;
import teamdash.wbs.columns.NotesColumn;
import teamdash.wbs.columns.SizeActualDataColumn;
import teamdash.wbs.columns.TeamActualTimeColumn;
import teamdash.wbs.columns.TeamCompletionDateColumn;
import teamdash.wbs.columns.TeamMemberActualTimeColumn;
import teamdash.wbs.columns.TeamMemberTimeColumn;
import teamdash.wbs.columns.TeamTimeColumn;

public class WBSSynchronizer {


    private TeamProject teamProject;

    private DataTableModel dataModel;

    private Date effectiveDate;

    private boolean createMissingTeamMembers = false;

    private boolean needsWbsEvent = false;

    private boolean foundActualData = false;

    private boolean foundActualSizeData = false;

    private boolean sizeDataIncomplete = false;

    private Map<String, SyncHandler> handlers;


    private static final Logger logger = Logger.getLogger(WBSSynchronizer.class
            .getName());

    public static String getSyncAttrName(String id) {
        return id.replace('_', '-') + " (Synced)";
    }

    private static final String EFFECTIVE_DATE_SUFFIX =
        "@Actual_Data_Effective_Date";

    public static final String EFFECTIVE_DATE_ATTR =
        "Team" + EFFECTIVE_DATE_SUFFIX;

    public static final String SYNC_NODE_TYPE_ATTR =
        getSyncAttrName("Node Type");


    public WBSSynchronizer(TeamProject teamProject, DataTableModel dataModel) {
        this.teamProject = teamProject;
        this.dataModel = dataModel;
        this.handlers = createSyncHandlers();
    }

    public boolean isCreateMissingTeamMembers() {
        return createMissingTeamMembers;
    }

    public void setCreateMissingTeamMembers(boolean createMissingTeamMembers) {
        this.createMissingTeamMembers = createMissingTeamMembers;
    }

    public void run() {
        effectiveDate = new Date(0);
        foundActualData = needsWbsEvent = false;
        foundActualSizeData = sizeDataIncomplete = false;
        Element directDumpData = getDirectDumpData();
        Map<String, File> exportFiles = getExportFiles();
        Map<Integer, WBSNode> nodeMap = teamProject.getWBS().getNodeMap();

        for (Iterator i = teamProject.getTeamMemberList().getTeamMembers()
                .iterator(); i.hasNext();) {
            TeamMember m = (TeamMember) i.next();
            Element dump = getUserDumpData(m, exportFiles, directDumpData);
            syncTeamMember(m, dump, nodeMap);
        }

        if (createMissingTeamMembers)
            addMissingTeamMembers(exportFiles);

        teamProject.getWBS().getRoot().setAttribute(EFFECTIVE_DATE_ATTR,
            effectiveDate);

        int col = dataModel.findColumn(TeamActualTimeColumn.COLUMN_ID);
        dataModel.columnChanged(dataModel.getColumn(col));

        if (needsWbsEvent)
            teamProject.getWBS().fireTableRowsUpdated(0, 0);
    }


    /**
     * Return true if any actual data was found during the reverse-sync, false
     * if none was found
     */
    public boolean getFoundActualData() {
        return foundActualData;
    }

    /**
     * Return true if actual size data was found during the reverse-sync, and
     * if all team members are reporting size.
     * 
     * Note that we only return true if <b>actual</b> size was discovered;
     * plan size only does not count.  This is because plan sizes will
     * appear immediately after the first sync, which occurs during the
     * bottom-up planning phase of the launch.  The appearance of non-launch
     * planned sizes would be a source of confusion.  So instead, we wait
     * for teams to enter some actual size data before these tabs appear.
     */
    public boolean getFoundActualSizeData() {
        return foundActualSizeData && !sizeDataIncomplete;
    }

    /**
     * When the WBS Editor is opened by an individual from their personal
     * dashboard, it is desirable to retrieve their up-to-the-second most
     * recent data (instead of the data they wrote when they exported last).
     * To arrange this, the caller sets a system property pointing to a URL
     * that will return up-to-date sync data.  This method checks for that
     * property, and possibly loads the data in question.
     */
    private Element getDirectDumpData() {
        String revSyncURL = System.getProperty("teamdash.wbs.reverseSyncURL");
        if (!XMLUtils.hasValue(revSyncURL))
            return null;

        try {
            URL u = new URL(revSyncURL);
            return XMLUtils.parse(
                new BufferedInputStream(u.openStream())).getDocumentElement();
        } catch (MalformedURLException e) {
            logger.severe("Malformed reverse sync URL " + revSyncURL);
        } catch (SAXException e) {
            logger.severe("Could not understand data from reverse sync URL "
                    + revSyncURL);
        } catch (Exception e) {
            logger.severe("Could not connect to reverse sync URL " + revSyncURL);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Find data export files in the team storage directory.
     * 
     * @return a map whose keys are team member initials (in lower case), and
     *     whose values are File objects for the associated team member
     */
    private Map<String, File> getExportFiles() {
        Map<String, File> result = new HashMap<String, File>();
        File dir = teamProject.getStorageDirectory();
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            String filename = files[i].getName().toLowerCase();
            if (filename.endsWith(EXPORT_FILENAME_ENDING)) {
                String initials = filename.substring(0, filename.length()
                        - EXPORT_FILENAME_ENDING.length());
                result.put(initials, files[i]);
            }
        }
        return result;
    }

    /**
     * Locate and return reverse sync XML data for a team member.
     * 
     * @param m a team member
     * @param exportFiles a Map of export files, indexed by team member initials
     * @param directDumpData an XML document containing reverse sync data that
     *     was downloaded directly (instead of from an export file)
     * @return an XML document containing reverse sync data for the given
     *     team member, or null if no data for that person was found.
     */
    private Element getUserDumpData(TeamMember m, Map<String, File> exportFiles,
            Element directDumpData) {
        String initials = m.getInitials();
        if (initials == null)
            return null; // this individual is not fully set up

        // find the pdash file for the person with these initials
        File f = exportFiles.remove(initials.toLowerCase());

        // check to see if the directly downloaded sync data is available, and
        // if it represents the user in question.  If so, return it.
        if (directDumpData != null) {
            String dsdInitials = directDumpData.getAttribute(INITIALS_ATTR);
            if (initials.equalsIgnoreCase(dsdInitials))
                return directDumpData;
        }

        if (f == null)
            return null; // this individual has not exported a pdash file
        else
            return getUserDumpData(f);
    }

    private Element getUserDumpData(File f) {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(f);
            ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(
                    fileInputStream));
            ZipEntry e;
            while ((e = zipIn.getNextEntry()) != null)
                if (e.getName().equals(USER_DUMP_ENTRY_NAME))
                    return XMLUtils.parse(zipIn).getDocumentElement();
        } catch (Exception e) {
            logger.severe("Unable to read user dump data from file " + f);
            e.printStackTrace();
        }

        // the user's exported pdash file did not contain a dump file.
        logger.fine("No " + USER_DUMP_ENTRY_NAME + " file found in " + f);
        if (fileInputStream != null) {
            try {
                fileInputStream.close();
            } catch (IOException ioe) {}
        }
        return null;
    }

    private void addMissingTeamMembers(Map<String, File> unclaimedExportFiles) {
        Date timestamp = getLastTeamListReverseSyncDate();
        TeamMemberList team = new TeamMemberList(teamProject.getTeamMemberList());
        boolean madeChange = false;

        for (File file : unclaimedExportFiles.values())
            if (addMissingTeamMember(timestamp, team, file))
                madeChange = true;

        if (madeChange)
            teamProject.getTeamMemberList().copyFrom(team);

        teamProject.getWBS().getRoot().setAttribute(TEAM_LIST_SYNC_TIMESTAMP,//
            Long.toString(System.currentTimeMillis()));
    }

    // get a timestamp indicating when the team member list was last synced
    private Date getLastTeamListReverseSyncDate() {
        // if we've recorded a sync date, return it.
        Object lastSync = teamProject.getWBS().getRoot()
                .getAttribute(TEAM_LIST_SYNC_TIMESTAMP);
        if (lastSync instanceof String) {
            try {
                return new Date(Long.parseLong((String) lastSync));
            } catch (Exception e) {}
        }

        // if this project has a completely empty team member list (typical for
        // a brand new project), use the zero date. This will add people to
        // the list who joined before the WBS was opened the first time.
        if (teamProject.getTeamMemberList().getRowCount() == 0)
            return new Date(0);

        // Otherwise, return the current date. This allows legacy projects
        // to draw a new line in the sand after the point in time when team
        // member reverse sync was enabled. Potentially obsolete PDASH files
        // written before that point in time will not be reverse synced.
        return new Date();
    }

    private boolean addMissingTeamMember(Date syncTimestamp,
            TeamMemberList team, File f) {
        Element xml = getUserDumpData(f);
        if (xml == null)
            return false;

        Date dumpTimestamp = XMLUtils.getXMLDate(xml, TIMESTAMP_ATTR);
        if (dumpTimestamp == null || dumpTimestamp.before(syncTimestamp))
            return false;

        String initials = xml.getAttribute(INITIALS_ATTR);
        String fullName = xml.getAttribute(OWNER_FULLNAME_ATTR);
        if (!XMLUtils.hasValue(initials) || !XMLUtils.hasValue(fullName))
            return false;

        String username = xml.getAttribute(OWNER_USERNAME_ATTR);
        if (containsTeamMember(team, initials, fullName, username))
            return false;

        addTeamMember(team, initials, username, fullName);
        return true;
    }

    private boolean containsTeamMember(TeamMemberList team, String initials,
            String fullName, String username) {
        for (TeamMember m : team.getTeamMembers()) {
            // same initials as an existing team member?
            if (initials.equalsIgnoreCase(m.getInitials()))
                return true;
            // same name as an existing team member?
            if (XMLUtils.hasValue(fullName)
                    && fullName.equalsIgnoreCase(m.getName()))
                return true;
            // username matches the server identity of an existing team member?
            String serverInfo = m.getServerIdentityInfo();
            if (XMLUtils.hasValue(username) && XMLUtils.hasValue(serverInfo)
                    && HTMLUtils.parseQuery(serverInfo).containsValue(username))
                return true;
        }

        return false;
    }

    private void addTeamMember(TeamMemberList team, String initials,
            String username, String fullName) {
        team.maybeAddEmptyRow();
        TeamMember m = team.get(team.getRowCount() - 1);
        m.setName(fullName);
        m.setInitials(initials);
        if (XMLUtils.hasValue(username))
            m.setServerIdentityInfo("searchType=username&search=" + username
                    + "&username=" + username);
    }


    /**
     * Synchronize the WBS with reverse sync data for a given team member.
     * 
     * @param m a team member
     * @param dumpData the reverse sync data for that team member
     */
    private void syncTeamMember(TeamMember m, Element dumpData,
            Map<Integer, WBSNode> nodeMap) {
        if (dumpData == null)
            return;

        logger.log(Level.FINE, "Reverse synchronizing data for {0}", m
                .getName());

        String dumpVersion = dumpData.getAttribute(DUMP_VERSION_ATTR);
        if (VersionUtils.compareVersions(dumpVersion, MIN_SIZEDATA_VERSION) < 0)
            sizeDataIncomplete = true;

        Date indivEffDate = XMLUtils.getXMLDate(dumpData, TIMESTAMP_ATTR);
        teamProject.getWBS().getRoot().setAttribute(
            getIndivEffectiveDateAttrName(m.getInitials()), indivEffDate);
        if (indivEffDate != null && indivEffDate.after(effectiveDate))
            effectiveDate = indivEffDate;

        NodeList children = dumpData.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                Element e = (Element) child;
                SyncHandler handler = handlers.get(e.getTagName());
                if (handler != null)
                    handler.sync(teamProject, m, nodeMap, e);
            }
        }

    }

    private Map<String, SyncHandler> createSyncHandlers() {
        Map<String, SyncHandler> result = new HashMap<String, SyncHandler>();
        if (isEnabled(SCHEDULE_CHANGE_TAG))
            result.put(SCHEDULE_CHANGE_TAG, new ScheduleSynchronizer());
        if (isEnabled(PLAN_TIME_CHANGE_TAG))
            result.put(PLAN_TIME_CHANGE_TAG, new PlanTimeSynchronizer());
        if (isEnabled(NODE_TYPE_CHANGE_TAG))
            result.put(NODE_TYPE_CHANGE_TAG, new NodeTypeSynchronizer());
        if (isEnabled(NOTE_CHANGE_TAG))
            result.put(NOTE_CHANGE_TAG, new NoteSynchronizer());
        if (isEnabled(ACTUAL_DATA_TAG))
            result.put(ACTUAL_DATA_TAG, new ActualDataLoader());
        if (isEnabled(SIZE_DATA_TAG))
            result.put(SIZE_DATA_TAG, new SizeDataLoader());
        if (isEnabled(NEW_TASK_TAG))
            result.put(NEW_TASK_TAG, new NewTaskLoader());
        return result;
    }

    private boolean isEnabled(String item) {
        String settingName = "reverseSync." + item + ".disabled";
        String setting = teamProject.getUserSetting(settingName);
        return !"true".equalsIgnoreCase(setting);
    }


    private interface SyncHandler {
        public void sync(TeamProject teamProject, TeamMember individual,
                Map<Integer, WBSNode> nodeMap, Element dumpData);
    }

    /**
     * Class to read user changes to the EV schedule, and incorporate them
     * into the WBS schedule for the team member.
     */
    private class ScheduleSynchronizer implements SyncHandler {

        public void sync(TeamProject teamProject, TeamMember individual,
                Map<Integer, WBSNode> nodeMap, Element scheduleChangeTag) {
            WeeklySchedule schedule = individual.getSchedule();
            NodeList exceptions = scheduleChangeTag
                    .getElementsByTagName(SCHEDULE_EXCEPTION_TAG);

            for (int i = 0;  i < exceptions.getLength();  i++) {
                Element exc = (Element) exceptions.item(i);
                Date when = XMLUtils.getXMLDate(exc, WHEN_ATTR);
                int week = schedule.dateToWeekValue(when);

                if (DEFAULT_HOURS_VAL.equals(exc.getAttribute(HOURS_ATTR)))
                    schedule.removeException(week);
                else {
                    double hours = XMLUtils.getXMLNum(exc, HOURS_ATTR);
                    schedule.addException(week, hours);
                }
            }
        }

    }

    /**
     * Class to read user changes in planned time, and incorporate them into
     * the planned times for tasks in the WBS.
     */
    private class PlanTimeSynchronizer implements SyncHandler {

        private TeamMember m;
        private int indivTimeColumn;
        private String syncAttrName;

        private void setTeamMember(TeamMember m) {
            if (this.m != m) {
                this.m = m;

                String columnID = TeamMemberTimeColumn.getColumnID(m);
                indivTimeColumn = dataModel.findColumn(columnID);
                syncAttrName = getSyncAttrName(columnID);
            }
        }

        public void sync(TeamProject teamProject, TeamMember individual,
                Map<Integer, WBSNode> nodeMap, Element planTimeTag) {
            setTeamMember(individual);

            int wbsId = XMLUtils.getXMLInt(planTimeTag, WBS_ID_ATTR);
            WBSNode node = nodeMap.get(wbsId);
            if (node == null)
                return;

            double newUserTime = XMLUtils.getXMLNum(planTimeTag, TIME_ATTR);
            double lastUserTime = node.getNumericAttribute(syncAttrName);

            if (Double.isNaN(lastUserTime) || !eq(newUserTime, lastUserTime)) {
                dataModel.setValueAt(new Double(newUserTime), node,
                    indivTimeColumn);
                node.setNumericAttribute(syncAttrName, newUserTime);
            }
        }

    }

    /**
     * Class to read user changes in node type, and incorporate them into the
     * node types for tasks in the WBS.
     */
    private class NodeTypeSynchronizer implements SyncHandler {

        public void sync(TeamProject teamProject, TeamMember individual,
                Map<Integer, WBSNode> nodeMap, Element nodeTypeTag) {
            int wbsId = XMLUtils.getXMLInt(nodeTypeTag, WBS_ID_ATTR);
            WBSNode node = nodeMap.get(wbsId);
            if (node == null)
                return;

            String newUserType = nodeTypeTag.getAttribute(NODE_TYPE_ATTR);
            if (!XMLUtils.hasValue(newUserType))
                return;
            newUserType = newUserType + " Task";

            Object lastUserType = node.getAttribute(SYNC_NODE_TYPE_ATTR);

            if (!newUserType.equals(lastUserType)) {
                node.setType(newUserType);
                node.setAttribute(SYNC_NODE_TYPE_ATTR, newUserType);
                needsWbsEvent = true;
            }
        }

    }

    /**
     * Class to read user changes to notes, and incorporate them into the WBS.
     */
    private class NoteSynchronizer implements SyncHandler {

        public void sync(TeamProject teamProject, TeamMember individual,
                Map<Integer, WBSNode> nodeMap, Element noteTag) {
            Object wbsId = getNodeMapKey(noteTag, WBS_ID_ATTR);
            WBSNode node = nodeMap.get(wbsId);
            if (node == null)
                return;

            String wbsText = NotesColumn.getTextAt(node);
            Date wbsTimestamp = NotesColumn.getTimestampAt(node);
            Date baseTimestamp = XMLUtils.getXMLDate(noteTag, BASE_TIMESTAMP_ATTR);
            if (!XMLUtils.hasValue(wbsText) || wbsTimestamp == null
                    || wbsTimestamp.equals(baseTimestamp)) {
                String text = XMLUtils.getTextContents(noteTag);
                String author = noteTag.getAttribute(AUTHOR_ATTR);
                if (!XMLUtils.hasValue(author))
                    author = individual.getName();
                Date timestamp = XMLUtils.getXMLDate(noteTag, TIMESTAMP_ATTR);
                NotesColumn.saveSyncData(node, text, author, timestamp);
            }
        }

    }

    /**
     * Class to load actual data reported by individuals into the WBS.
     */
    private class ActualDataLoader implements SyncHandler {

        private TeamMember m;
        private String actualTimeAttrName;
        private String completionDateAttrName;
        private String subtaskDataAttrName;

        private void setTeamMember(TeamMember m) {
            if (this.m != m) {
                this.m = m;

                actualTimeAttrName = TeamMemberActualTimeColumn
                        .getNodeDataAttrName(m);
                completionDateAttrName = TeamCompletionDateColumn
                        .getMemberNodeDataAttrName(m);
                subtaskDataAttrName = TeamMemberActualTimeColumn
                        .getSubtaskDataAttrName(m);
            }
        }

        public void sync(TeamProject teamProject, TeamMember individual,
                Map<Integer, WBSNode> nodeMap, Element actualDataTag) {
            setTeamMember(individual);

            int wbsId = XMLUtils.getXMLInt(actualDataTag, WBS_ID_ATTR);
            WBSNode node = nodeMap.get(wbsId);
            if (node == null)
                return;

            double time = XMLUtils.getXMLNum(actualDataTag, TIME_ATTR);
            node.setNumericAttribute(actualTimeAttrName, time);

            Date date = XMLUtils.getXMLDate(actualDataTag, COMPLETION_DATE_ATTR);
            node.setAttribute(completionDateAttrName, date);

            List subtaskData = null;
            NodeList subtaskNodes = actualDataTag
                    .getElementsByTagName(SUBTASK_DATA_TAG);
            if (subtaskNodes != null && subtaskNodes.getLength() > 0) {
                subtaskData = new ArrayList();
                for (int i = 0;  i < subtaskNodes.getLength();  i++)
                    subtaskData.add(new ActualSubtaskData(
                            (Element) subtaskNodes.item(i)));
            }
            node.setAttribute(subtaskDataAttrName, subtaskData);

            foundActualData = true;
        }

    }

    public class ActualSubtaskData {
        double planTime;
        double actualTime;
        Date completionDate;
        private ActualSubtaskData(Element xml) {
            planTime = XMLUtils.getXMLNum(xml, EST_TIME_ATTR);
            actualTime = XMLUtils.getXMLNum(xml, TIME_ATTR);
            completionDate = XMLUtils.getXMLDate(xml, COMPLETION_DATE_ATTR);
        }
        public double getPlanTime()     { return planTime;       }
        public double getActualTime()   { return actualTime;     }
        public Date getCompletionDate() { return completionDate; }
    }

    private class SizeDataLoader implements SyncHandler {

        public void sync(TeamProject teamProject, TeamMember individual,
                Map<Integer, WBSNode> nodeMap, Element sizeDataTag) {

            int wbsId = XMLUtils.getXMLInt(sizeDataTag, WBS_ID_ATTR);
            WBSNode node = nodeMap.get(wbsId);
            if (node == null)
                // no node with that WBS ID found? log the data to the root.
                node = nodeMap.get(null);

            String sizeUnits = sizeDataTag.getAttribute(UNITS_ATTR);
            double planSize = XMLUtils.getXMLNum(sizeDataTag, EST_SIZE_ATTR);
            double actSize = XMLUtils.getXMLNum(sizeDataTag, ACTUAL_SIZE_ATTR);
            SizeActualDataColumn.storeData(node, sizeUnits, planSize, actSize);

            if (actSize > 0)
                foundActualSizeData = true;
        }

    }

    private class NewTaskLoader implements SyncHandler {

        private TeamMember m;
        private String estTimeAttrName;
        private String estTimeSyncAttrName;
        private String assignedZeroAttrName;
        private String actualTimeAttrName;
        private String completionDateAttrName;
        private String teamTimeAttrName = TeamTimeColumn.getNodeDataAttrName();

        private void setTeamMember(TeamMember m) {
            if (this.m != m) {
                this.m = m;

                estTimeAttrName = TeamMemberTimeColumn
                        .getMemberNodeDataAttrName(m);
                estTimeSyncAttrName = getSyncAttrName(TeamMemberTimeColumn
                        .getColumnID(m));
                assignedZeroAttrName = TeamTimeColumn
                        .getMemberAssignedZeroAttrName(m);
                actualTimeAttrName = TeamMemberActualTimeColumn
                        .getNodeDataAttrName(m);
                completionDateAttrName = TeamCompletionDateColumn
                        .getMemberNodeDataAttrName(m);
            }
        }

        public void sync(TeamProject teamProject, TeamMember individual,
                Map<Integer, WBSNode> nodeMap, Element newTaskTag) {
            setTeamMember(individual);

            WBSNode parentNode = getParentNode(nodeMap, newTaskTag);
            if (parentNode == null)
                return;

            int insertionPos = getInsertionPos(teamProject.getWBS(),
                parentNode, newTaskTag);
            if (insertionPos == -1)
                return;

            // we are either (a) subdividing an existing leaf, or (b) adding
            // new tasks underneath an existing parent.  In either case, any
            // previous top-down time estimate on the parent node is now
            // out-of-date.  Delete it so the bottom-up estimates from the
            // new children will prevail.
            parentNode.setAttribute(estTimeAttrName, null);
            parentNode.setAttribute(estTimeSyncAttrName, null);
            parentNode.setAttribute(teamTimeAttrName, null);
            // create and insert the new children
            List newTasks = createNewTasks(teamProject.getWBS(), parentNode,
                newTaskTag);
            teamProject.getWBS().insertNodesAt(newTasks, insertionPos, true);
            needsWbsEvent = false;
        }

        private WBSNode getParentNode(Map<Integer, WBSNode> nodeMap,
                Element newTaskTag) {
            if ("root".equals(newTaskTag.getAttribute(PARENT_ID_ATTR))) {
                return nodeMap.get(null);
            } else {
                int parentID = XMLUtils.getXMLInt(newTaskTag, PARENT_ID_ATTR);
                return nodeMap.get(parentID);
            }
        }

        private int getInsertionPos(WBSModel wbsModel, WBSNode parentNode,
                Element tag) {
            WBSNode[] children = wbsModel.getChildren(parentNode);

            String name = tag.getAttribute(TASK_NAME_ATTR);
            if (name == null)
                return -1;

            WBSNode prevSibling = null;
            WBSNode nextSibling = null;
            String prevSiblingName = tag.getAttribute(PREV_SIBLING_NAME_ATTR);
            int prevSiblingID = XMLUtils.getXMLInt(tag, PREV_SIBLING_ID_ATTR);
            int nextSiblingID = XMLUtils.getXMLInt(tag, NEXT_SIBLING_ID_ATTR);
            for (WBSNode node : children) {
                if (name.equals(node.getName()))
                    // it appears that a previous reverse sync operation has
                    // already added this node to the WBS.  Do nothing.
                    return -1;

                if (prevSiblingID != -1) {
                    if (prevSiblingID == node.getUniqueID())
                        prevSibling = node;
                } else if (nextSiblingID != -1) {
                    if (nextSiblingID == node.getUniqueID())
                        nextSibling = node;
                } else if (prevSiblingName != null) {
                    if (prevSiblingName.equals(node.getName()))
                        prevSibling = node;
                }
            }

            if (nextSibling != null) {
                return wbsModel.getIndexOfNode(nextSibling);
            } else {
                WBSNode afterNode = prevSibling;
                if (afterNode == null)
                    afterNode = parentNode;
                WBSNode[] descendants = wbsModel.getDescendants(afterNode);
                if (descendants != null && descendants.length > 0)
                    afterNode = descendants[descendants.length-1];
                return wbsModel.getIndexOfNode(afterNode) + 1;
            }
        }

        private List<WBSNode> createNewTasks(WBSModel wbsModel,
                WBSNode parentNode, Element newTaskTag) {
            List<WBSNode> result = new ArrayList<WBSNode>();
            int indentLevel = parentNode.getIndentLevel() + 1;
            createNewTasks(result, wbsModel, newTaskTag, indentLevel,
                WBSNode.UNKNOWN_TYPE);
            return result;
        }

        private void createNewTasks(List<WBSNode> result, WBSModel wbsModel,
                Element task, int level, String defaultType) {
            // retrieve the name of the new item
            String name = task.getAttribute(TASK_NAME_ATTR);

            // calculate the node type of the new item
            String type = task.getAttribute(NODE_TYPE_ATTR);
            if (type == null || type.length() == 0)
                type = defaultType;
            else if ("/PSP/".equals(type))
                type = TeamProcess.PSP_TASK_TYPE;
            else
                type = type + " Task";

            // create the new node
            WBSNode node = new WBSNode(wbsModel, name, type, level, false);

            // set node attributes for est/actual time, completion date
            double estTime = XMLUtils.getXMLNum(task, EST_TIME_ATTR);
            setNodeAttr(node, estTimeAttrName, estTime);
            setNodeAttr(node, estTimeSyncAttrName, estTime);
            double actualTime = XMLUtils.getXMLNum(task, TIME_ATTR);
            setNodeAttr(node, actualTimeAttrName, actualTime);
            Date actualDate = XMLUtils.getXMLDate(task, COMPLETION_DATE_ATTR);
            node.setAttribute(completionDateAttrName, actualDate);

            // add the new node to the result list.
            result.add(node);

            // recurse over children and create nodes for them, too.
            boolean isLeaf = true;
            NodeList subtasks = task.getChildNodes();
            for (int i = 0;  i < subtasks.getLength();  i++) {
                Node subtask = subtasks.item(i);
                if (subtask instanceof Element) {
                    createNewTasks(result, wbsModel, (Element) subtask,
                        level + 1, type);
                    isLeaf = false;
                }
            }

            // set the "assigned with zero hours" flag if needed.
            if (isLeaf && estTime == 0)
                node.setAttribute(assignedZeroAttrName, "t");
        }

        private void setNodeAttr(WBSNode node, String attrName, double value) {
            if (value > 0)
                node.setNumericAttribute(attrName, value);
        }

    }

    private static Object getNodeMapKey(Element e, String tagName) {
        int wbsId = XMLUtils.getXMLInt(e, tagName);
        if (wbsId == -1 && "root".equals(e.getAttribute(tagName)))
            return null;
        else
            return wbsId;
    }

    private static boolean eq(double a, double b) {
        if (Double.isNaN(a) || Double.isNaN(b))
            return false;
        else
            return Math.abs(a - b) < 0.01;
    }

    public static final String getIndivEffectiveDateAttrName(String initials) {
        return "Ind" + initials.toLowerCase() + EFFECTIVE_DATE_SUFFIX;
    }

    private static final String USER_DUMP_ENTRY_NAME = "userDump.xml";

    private static final String EXPORT_FILENAME_ENDING = "-data.pdash";

    private static final String DUMP_VERSION_ATTR = "dumpFileVersion";

    private static final String INITIALS_ATTR = "initials";

    private static final String TIMESTAMP_ATTR = "timestamp";

    private static final String OWNER_USERNAME_ATTR = "userName";

    private static final String OWNER_FULLNAME_ATTR = "fullName";

    private static final String TEAM_LIST_SYNC_TIMESTAMP = "Team List Reverse Sync Timestamp";

    private static final String PLAN_TIME_CHANGE_TAG = "planTimeChange";

    private static final String WBS_ID_ATTR = "wbsId";

    private static final String TIME_ATTR = "time";

    private static final String SCHEDULE_CHANGE_TAG = "scheduleChange";

    private static final String SCHEDULE_EXCEPTION_TAG = "scheduleException";

    private static final String WHEN_ATTR = "when";

    private static final String HOURS_ATTR = "hours";

    private static final String DEFAULT_HOURS_VAL = "DEFAULT";

    private static final String ACTUAL_DATA_TAG = "actualData";

    private static final String SUBTASK_DATA_TAG = "subtaskData";

    // private static final String START_DATE_ATTR = "started";

    private static final String COMPLETION_DATE_ATTR = "completed";

    private static final String NEW_TASK_TAG = "newTask";

    private static final String TASK_NAME_ATTR = "name";

    private static final String PARENT_ID_ATTR = "parentWbsId";

    private static final String PREV_SIBLING_ID_ATTR = "prevSiblingId";

    private static final String PREV_SIBLING_NAME_ATTR = "prevSiblingName";

    private static final String NEXT_SIBLING_ID_ATTR = "nextSiblingId";

    private static final String EST_TIME_ATTR = "estTime";

    private static final String NODE_TYPE_ATTR = "nodeType";

    private static final String NODE_TYPE_CHANGE_TAG = "nodeTypeChange";

    private static final String NOTE_CHANGE_TAG = "noteChange";

    private static final String BASE_TIMESTAMP_ATTR = "baseTimestamp";

    private static final String AUTHOR_ATTR = "author";

    private static final String MIN_SIZEDATA_VERSION = "3.10.1";

    private static final String SIZE_DATA_TAG = "sizeData";

    // private static final String DESCRIPTION_ATTR = "description";

    private static final String UNITS_ATTR = "units";

    private static final String EST_SIZE_ATTR = "estSize";

    private static final String ACTUAL_SIZE_ATTR = "actSize";

}
