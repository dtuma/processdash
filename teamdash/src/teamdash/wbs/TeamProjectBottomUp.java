package teamdash.wbs;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import teamdash.TeamMember;
import teamdash.TeamMemberList;

public class TeamProjectBottomUp extends TeamProject {

    private Map subprojects;

    private FileWatcher watcher;

    public TeamProjectBottomUp(File directory, String projectName) {
        super(directory, projectName);
        super.setReadOnly(true);
        reloadBottomUpData();
        watcher = new FileWatcher();
    }

    public void dispose() {
        watcher.quit();
    }

    public String getProjectName() {
        return super.getProjectName() + " (Bottom Up)";
    }


    public void setReadOnly(boolean readOnly) {
        // do nothing.  We don't want to allow anyone to make this
        // project editable.
    }

    public void save() {
        // do nothing.  The read-only status of the parent project
        // ought to have the same effect, but we don't want to take
        // any chances.
    }

    public void reload() {
        if (getWBS() == null)
            // if the work breakdown structure is still null, then we are
            // loading data on behalf of our superclass's constructor.
            // It isn't safe to perform any of our own initializations yet.
            super.reload();
        else {
            // if the work breakdown structure is not null, then someone else
            // is calling us, requesting data to be reloaded.
            openProjectSettings();
            reloadBottomUpData();
        }
    }


    private void reloadBottomUpData() {
        if (refreshSubprojects())
            recalculateBottomUpData();
    }

    private boolean refreshSubprojects() {
        boolean result = false;

        if (subprojects == null) {
            subprojects = new LinkedHashMap();
            result = true;
        }

        Set subprojectChecklist = new HashSet(subprojects.keySet());

        Element settings = getProjectSettings();
        NodeList nl = settings.getElementsByTagName("subproject");
        for (int i = 0; i < nl.getLength(); i++) {
            Element subprojectElem = (Element) nl.item(i);
            String shortName = subprojectElem.getAttribute("shortName");

            TeamProject subproject = (TeamProject) subprojects.get(shortName);
            if (subproject == null) {
                File dir = getProjectDataDirectory(subprojectElem, true);
                if (dir != null) {
                    subproject = new TeamProject(dir, shortName);
                    subprojects.put(shortName, subproject);
                    result = true;
                }
            } else {
                subprojectChecklist.remove(shortName);
                if (subproject.maybeReload())
                    result = true;
            }
        }

        for (Iterator i = subprojectChecklist.iterator(); i.hasNext();) {
            String subprojectName = (String) i.next();
            subprojects.remove(subprojectName);
            result = true;
        }

        return result;
    }

    private void recalculateBottomUpData() {
        WBSModel newWbs = makeStartingWBS();
        TeamMemberList newTeam = new TeamMemberList();

        for (Iterator i = subprojects.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String shortName = (String) e.getKey();
            TeamProject subproject = (TeamProject) e.getValue();

            addWBSItems(newWbs, shortName, subproject);
            double totalSubprojectTime = sumUpTime(newWbs, shortName);
            addTeamMember(newTeam, shortName, subproject, totalSubprojectTime);
        }

        newWbs.copyNodeExpansionStates(getWBS(),
                MasterWBSUtil.NODE_ID_COMPARATOR);
        getWBS().copyFrom(newWbs);

        getTeamMemberList().copyFrom(newTeam);
    }

    private WBSModel makeStartingWBS() {
        WBSModel result = new WBSModel(getProjectName());
        WBSNode[] descendants = result.getDescendants(result.getRoot());
        result.deleteNodes(Arrays.asList(descendants));
        MasterWBSUtil.mergeFromMaster(super.readWBS(), getProjectID(), result);
        MasterWBSUtil.visitWBS(result, MASTER_WBS_CLEANER);
        return result;
    }

    private void addTeamMember(TeamMemberList newTeam, String shortName,
            TeamProject subproject, double totalSubprojectTime) {
        newTeam.maybeAddEmptyRow();
        int pos = newTeam.getRowCount() - 1;
        newTeam.setValueAt(shortName, pos, TeamMemberList.NAME_COLUMN);
        newTeam.setValueAt(shortName, pos, TeamMemberList.INITIALS_COLUMN);

        // now, decide how to set the start date and hours per week.
        //
        // start date is easy to determine: just find out when the earliest
        // team member begins.
        //
        // hours per week is trickier. Ultimately, we want the TeamTimePanel
        // to display the correct end date for the subproject. So first, we
        // calculate the desired end date, by choosing the date the last team
        // member will finish.  Then, knowing how the TeamTimePanel
        // calculates the end date for a bar, we choose a value for "hours
        // per week" that will produce that end date.

        long startTime = Long.MAX_VALUE;
        long endTime = 0;
        List subprojMembers = subproject.getTeamMemberList().getTeamMembers();
        for (Iterator i = subprojMembers.iterator(); i.hasNext();) {
            TeamMember m = (TeamMember) i.next();
            double hoursPerWeek = m.getHoursPerWeek().doubleValue();
            if (hoursPerWeek == 0)
                continue;

            long memberStart;
            if (m.getStartDate() == null)
                memberStart = System.currentTimeMillis();
            else
                memberStart = m.getStartDate().getTime();
            startTime = Math.min(startTime, memberStart);

            double memberTime = sumUpTime(subproject.getWBS(), m.getInitials());
            double memberWeeks = memberTime / hoursPerWeek;
            long memberEnd = memberStart
                    + (long) (memberWeeks * MILLIS_PER_WEEK);
            endTime = Math.max(endTime, memberEnd);
        }

        Date startDate = new Date(startTime);
        double elapsedWeeks = ((double) (endTime - startTime)) / MILLIS_PER_WEEK;
        Double effHrsPerWeek = new Double(totalSubprojectTime / elapsedWeeks);

        newTeam.setValueAt(startDate, pos, TeamMemberList.START_DATE_COLUMN);
        newTeam.setValueAt(effHrsPerWeek, pos, TeamMemberList.HOURS_COLUMN);
    }

    private void addWBSItems(WBSModel newWbs, String shortName,
            TeamProject subproject) {
        List teamMemberInitials = getTeamMemberInitials(subproject);
        String projectID = subproject.getProjectID();
        WBSModel subprojectWBS = subproject.getWBS();

        MasterWBSUtil.mergeFromSubproject(subprojectWBS, projectID, shortName,
                teamMemberInitials, newWbs);
    }

    private List getTeamMemberInitials(TeamProject proj) {
        List result = new LinkedList();
        List teamMembers = proj.getTeamMemberList().getTeamMembers();
        for (Iterator i = teamMembers.iterator(); i.hasNext();) {
            TeamMember t = (TeamMember) i.next();
            result.add(t.getInitials());
        }
        return result;
    }

    private double sumUpTime(WBSModel wbs, String name) {
        SumAttribute sum = new SumAttribute(name + "-Time (Top Down)");
        MasterWBSUtil.visitWBS(wbs, sum);
        return sum.getTotal();
    }

    private static class SumAttribute implements WBSNodeVisitor {
        private double total = 0;
        private String attrName;
        public SumAttribute(String attrName) {
            this.attrName = attrName;
        }
        public void visit(WBSNode parent, WBSNode child) {
            double val = child.getNumericAttribute(attrName);
            if (!Double.isNaN(val))
                total += val;
        }
        public double getTotal() {
            return total;
        }
    }

    private static class MasterWBSCleaner implements WBSNodeVisitor {

        public void visit(WBSNode parent, WBSNode child) {
            child.setReadOnly(false);
            child.setAttribute("Time (Top Down)", null);
        }

    }
    private static final WBSNodeVisitor MASTER_WBS_CLEANER = new MasterWBSCleaner();



    private class FileWatcher extends Thread {
        public volatile boolean isRunning;

        public FileWatcher() {
            super(FileWatcher.class.getName());
            setDaemon(true);
            isRunning = true;
            start();
        }

        public void run() {
            while (isRunning) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                }
                reload();
            }
        }

        public void quit() {
            isRunning = false;
        }
    }


    private static final long MILLIS_PER_WEEK = 7L /*days*/ * 24 /*hours*/
            * 60 /*min*/ * 60 /*sec*/ * 1000 /*millis*/;
}
