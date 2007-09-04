package teamdash.wbs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.sourceforge.processdash.util.StringUtils;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import teamdash.XMLUtils;
import teamdash.team.TeamMember;
import teamdash.team.WeeklySchedule;
import teamdash.wbs.columns.TeamActualTimeColumn;
import teamdash.wbs.columns.TeamMemberActualTimeColumn;
import teamdash.wbs.columns.TeamCompletionDateColumn;
import teamdash.wbs.columns.TeamMemberTimeColumn;

public class WBSSynchronizer {

    private static final String USER_DUMP_ENTRY_NAME = "userDump.xml";

    private static final String EXPORT_FILENAME_ENDING = "-data.pdash";

    private TeamProject teamProject;

    private DataTableModel dataModel;

    private boolean foundActualData = false;

    private Map<String, SyncHandler> handlers;


    private static final Logger logger = Logger.getLogger(WBSSynchronizer.class
            .getName());

    public static String getSyncAttrName(String id) {
        return id.replace('_', '-') + " (Synced)";
    }


    public WBSSynchronizer(TeamProject teamProject, DataTableModel dataModel) {
        this.teamProject = teamProject;
        this.dataModel = dataModel;
        this.handlers = createSyncHandlers();
    }

    public void run() {
        foundActualData = false;
        Element directDumpData = getDirectDumpData();
        Map<String, File> exportFiles = getExportFiles();
        Map<Integer, WBSNode> nodeMap = teamProject.getWBS().getNodeMap();

        for (Iterator i = teamProject.getTeamMemberList().getTeamMembers()
                .iterator(); i.hasNext();) {
            TeamMember m = (TeamMember) i.next();
            Element dump = getUserDumpData(m, exportFiles, directDumpData);
            syncTeamMember(m, dump, nodeMap);
        }

        int col = dataModel.findColumn(TeamActualTimeColumn.COLUMN_ID);
        dataModel.columnChanged(dataModel.getColumn(col));
    }


    /**
     * Return true if any actual data was found during the reverse-sync, false
     * if none was found
     */
    public boolean getFoundActualData() {
        return foundActualData;
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
        if (!StringUtils.hasValue(revSyncURL))
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

        // check to see if the directly downloaded sync data is available, and
        // if it represents the user in question.  If so, return it.
        if (directDumpData != null) {
            String dsdInitials = directDumpData.getAttribute(INITIALS_ATTR);
            if (initials.equalsIgnoreCase(dsdInitials))
                return directDumpData;
        }

        File f = exportFiles.get(initials.toLowerCase());
        if (f == null)
            return null; // this individual has not exported a pdash file

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
        result.put(SCHEDULE_CHANGE_TAG, new ScheduleSynchronizer());
        result.put(PLAN_TIME_CHANGE_TAG, new PlanTimeSynchronizer());
        result.put(ACTUAL_DATA_TAG, new ActualDataLoader());
        return result;
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
     * Class to load actual data reported by individuals into the WBS.
     */
    private class ActualDataLoader implements SyncHandler {

        private TeamMember m;
        private String actualTimeAttrName;
        private String completionDateAttrName;

        private void setTeamMember(TeamMember m) {
            if (this.m != m) {
                this.m = m;

                actualTimeAttrName = TeamMemberActualTimeColumn
                        .getNodeDataAttrName(m);
                completionDateAttrName = TeamCompletionDateColumn
                        .getMemberNodeDataAttrName(m);
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

            foundActualData = true;
        }

    }

    private static boolean eq(double a, double b) {
        if (Double.isNaN(a) || Double.isNaN(b))
            return false;
        else
            return Math.abs(a - b) < 0.01;
    }

    private static final String INITIALS_ATTR = "initials";

    private static final String PLAN_TIME_CHANGE_TAG = "planTimeChange";

    private static final String WBS_ID_ATTR = "wbsId";

    private static final String TIME_ATTR = "time";

    private static final String SCHEDULE_CHANGE_TAG = "scheduleChange";

    private static final String SCHEDULE_EXCEPTION_TAG = "scheduleException";

    private static final String WHEN_ATTR = "when";

    private static final String HOURS_ATTR = "hours";

    private static final String DEFAULT_HOURS_VAL = "DEFAULT";

    private static final String ACTUAL_DATA_TAG = "actualData";

    // private static final String START_DATE_ATTR = "started";

    private static final String COMPLETION_DATE_ATTR = "completed";
}
