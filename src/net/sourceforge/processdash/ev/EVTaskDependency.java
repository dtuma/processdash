// Copyright (C) 2006-2017 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

package net.sourceforge.processdash.ev;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ImmutableStringData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

public class EVTaskDependency implements Cloneable {

    private static final Logger logger = Logger
            .getLogger(EVTaskDependency.class.getName());

    private String taskID;

    private String displayName;

    private String taskListName;

    private String source;

    private boolean unresolvable;

    private String assignedTo;

    private double percentComplete;

    private Date projectedDate;

    private Date parentDate;

    public EVTaskDependency(String taskID, String displayName) {
        this.taskID = taskID;
        this.displayName = displayName;
    }

    public EVTaskDependency(Element e) {
        this.taskID = getAttr(e, TASK_ID_ATTR);
        this.displayName = getAttr(e, DISPLAY_NAME_ATTR);
        this.taskListName = getAttr(e, TASK_LIST_ATTR);
        this.source = getAttr(e, SOURCE_ATTR);
        this.assignedTo = getAttr(e, ASSIGNED_TO_ATTR);
        this.percentComplete = XMLUtils.getXMLNum(e, PERCENT_COMPLETE_ATTR);
        this.projectedDate = XMLUtils.getXMLDate(e, PROJ_DATE_ATTR);
    }

    public EVTaskDependency(Map waitingIndividuals) {
        this.taskID = REVERSE_PSEUDO_TASK;
        initForNameList(waitingIndividuals.keySet());
        this.projectedDate = EVCalculator.minStartDate(waitingIndividuals
                .values());
    }

    public EVTaskDependency(Collection<String> collaborators) {
        this.taskID = COLLAB_PSEUDO_TASK;
        initForNameList(collaborators);
    }

    private void initForNameList(Collection<String> indivNames) {
        this.displayName = this.taskListName = null;
        this.assignedTo = StringUtils.join(indivNames, ", ");
        this.percentComplete = 0;
        this.unresolvable = false;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTaskID() {
        return taskID;
    }

    public String getTaskListName() {
        return taskListName;
    }

    public void setTaskListName(String taskListName) {
        this.taskListName = taskListName;
    }

    /** Get the origin of this dependency.
     * 
     * Dependencies that were created manually by a user with the Task and
     * Schedule dialog will have null for their source.  Dependencies that
     * were created programatically (such as via a synchronization operation)
     * will have a source indicating which GUI was used to originally define
     * the dependency.
     */
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public List getAssignedToList() {
        if (!StringUtils.hasValue(assignedTo))
            return Collections.EMPTY_LIST;
        else
            return Arrays.asList(assignedTo.split(", "));
    }

    public boolean isIncomplete() {
        return percentComplete < 1.0;
    }

    public double getPercentComplete() {
        return percentComplete;
    }

    public Date getProjectedDate() {
        return projectedDate;
    }

    public Date getParentDate() {
        return parentDate;
    }

    public void loadParentDate(Object parent) {
        if (parent instanceof EVTask) {
            EVTask parentTask = (EVTask) parent;
            this.parentDate = getDependencyComparisonDate(parentTask);
            if (isReverse()) {
                this.percentComplete =
                    (parentTask.getActualDate() == null ? 0 : 1);
            }
        } else
            this.parentDate = null;
    }

    public boolean isUnresolvable() {
        return unresolvable;
    }

    public boolean isReverse() {
        return REVERSE_PSEUDO_TASK.equals(taskID);
    }

    public boolean isCollab() {
        return COLLAB_PSEUDO_TASK.equals(taskID);
    }

    public boolean isMisordered() {
        if (isReverse())
            return isNotBefore(parentDate, projectedDate);
        else
            return isNotBefore(projectedDate, parentDate);
    }

    private static boolean isNotBefore(Date a, Date b) {
        return (a != null && b != null && b.before(a));
    }



    public void setResolvedDetails(boolean unresolvable, String assignedTo,
            double percentComplete, Date projectedDate, String displayName) {
        this.unresolvable = unresolvable;
        this.assignedTo = assignedTo;
        this.percentComplete = percentComplete;
        this.projectedDate = projectedDate;
        if (!unresolvable && displayName != null && displayName.length() > 1)
            this.displayName = displayName;
    }

    public boolean equals(Object obj) {
        if (obj instanceof EVTaskDependency) {
            EVTaskDependency that = (EVTaskDependency) obj;
            return this.taskID.equals(that.taskID);
        } else
            return false;
    }

    public int hashCode() {
        return taskID.hashCode();
    }

    public void getAsXML(StringBuffer out, boolean includeResolvedInformation) {
        getAsXML(out, null, includeResolvedInformation);
    }
    public void getAsXML(StringBuffer out, String indent,
            boolean includeResolvedInformation) {
        if (isReverse() || isCollab())
            return;  // don't persist reverse dependency/collaboration info.
        if (indent != null)
            out.append(indent);
        out.append("<").append(DEPENDENCY_TAG);
        addAttr(out, TASK_ID_ATTR, getTaskID());
        addAttr(out, DISPLAY_NAME_ATTR, getDisplayName());
        addAttr(out, TASK_LIST_ATTR, getTaskListName());
        addAttr(out, SOURCE_ATTR, getSource());
        if (includeResolvedInformation) {
            addAttr(out, ASSIGNED_TO_ATTR, getAssignedTo());
            if (percentComplete > 0)
                addAttr(out, PERCENT_COMPLETE_ATTR, Double
                        .toString(percentComplete));
            if (projectedDate != null)
                addAttr(out, PROJ_DATE_ATTR, XMLUtils.saveDate(projectedDate));
        }
        out.append("/>");
        if (indent != null)
            out.append("\n");
    }

    private void addAttr(StringBuffer out, String name, String value) {
        if (XMLUtils.hasValue(value))
            out.append(" ").append(name).append("='").append(
                    XMLUtils.escapeAttribute(value)).append("'");
    }

    private String getAttr(Element e, String name) {
        String result = e.getAttribute(name);
        if (XMLUtils.hasValue(result))
            return result;
        else
            return null;
    }

    public EVTaskDependency getWithNewParent(Object newParent) {
        if (newParent instanceof EVTask) {
            EVTaskDependency result = clone();
            result.loadParentDate(newParent);
            return result;
        } else {
            return this;
        }
    }

    public EVTaskDependency clone() {
        try {
            return (EVTaskDependency) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void addTaskIDs(DataContext data, String taskPath, String id) {
        if (id == null || id.length() == 0)
            return;

        String dataName = DataRepository.createDataName(taskPath,
                TASK_ID_DATA_NAME);
        SimpleData currentValue = data.getSimpleValue(dataName);
        ListData newValue;
        if (currentValue instanceof ListData)
            newValue = (ListData) currentValue;
        else if (currentValue instanceof StringData)
            newValue = ((StringData) currentValue).asList();
        else
            newValue = new ListData();

        boolean valueChanged = false;
        String[] idList = id.split(",");
        for (int i = 0; i < idList.length; i++) {
            if (newValue.setAdd(idList[i]))
                valueChanged = true;
        }
        if (valueChanged)
            data.putValue(dataName, newValue);
    }

    public static void removeTaskID(DataContext data, String taskPath, String id) {
        String dataName = DataRepository.createDataName(taskPath,
                TASK_ID_DATA_NAME);
        SimpleData currentValue = data.getSimpleValue(dataName);
        ListData list = null;

        if (currentValue instanceof ListData)
            list = (ListData) currentValue;
        else if (currentValue instanceof StringData)
            list = ((StringData) currentValue).asList();

        if (list != null && list.remove(id))
            data.putValue(dataName, list);
    }

    public static List getTaskIDs(DataContext data, String taskPath) {
        String dataName = DataRepository.createDataName(taskPath,
                TASK_ID_DATA_NAME);
        SimpleData currentValue = data.getSimpleValue(dataName);
        ListData list = null;

        if (currentValue == null || currentValue.test() == false)
            return null;
        if (currentValue instanceof ListData)
            list = (ListData) currentValue;
        else if (currentValue instanceof StringData)
            list = ((StringData) currentValue).asList();
        else
            return null;

        List result = new LinkedList();
        for (int i = 0;  i < list.size();  i++)
            result.add(list.get(i));
        if (!result.isEmpty())
            return result;
        else
            return null;
    }

    public static boolean addTaskDependencies(DataContext data,
            String taskPath, List dependencies) {
        return addTaskDependencies(data, taskPath, dependencies, false);
    }

    public static boolean addTaskDependencies(DataContext data,
            String taskPath, List dependencies, boolean whatIfMode) {
        if (dependencies == null || dependencies.isEmpty())
            return false;

        return setTaskDependencies(data, taskPath, dependencies, null,
                whatIfMode);
    }

    public static boolean setTaskDependencies(DataContext data,
            String taskPath, List dependencies, String source,
            boolean whatIfMode) {

        boolean madeChange = false;
        List<EVTaskDependency> list = getDependencies(data, taskPath);
        if (list == null || list.isEmpty()) {
            // there are currently no dependencies for the given node.  Adopt
            // the incoming dependencies as the definitive list.
            list = dependencies;
            madeChange = (dependencies != null && !dependencies.isEmpty());

        } else if ((dependencies == null || dependencies.isEmpty())
                && StringUtils.hasValue(source)) {
            // the caller is asking us to clear the list of dependencies for
            // a given source.
            for (Iterator i = list.iterator(); i.hasNext();) {
                EVTaskDependency d = (EVTaskDependency) i.next();
                if (source.equals(d.getSource())) {
                    i.remove();
                    madeChange = true;
                }
            }

        } else {
            // keep track of the sources that are adding these dependencies.
            Set incomingSources = new HashSet();
            incomingSources.add(source);
            for (Iterator i = dependencies.iterator(); i.hasNext();) {
                EVTaskDependency d = (EVTaskDependency) i.next();
                incomingSources.add(d.getSource());

                int currPos = list.indexOf(d);
                if (currPos == -1) {
                    // the item isn't already in the list.  Add it.
                    list.add(d);
                    madeChange = true;

                } else if (d.getSource() != null) {
                    // the item is already in the list; check to see if
                    // the new object is contributing source information that
                    // wasn't already present
                    EVTaskDependency curDep =
                        (EVTaskDependency) list.get(currPos);
                    if (curDep.getSource() == null) {
                        // the new item has source data, but the old one
                        // didn't; replace the old dependency with the new one.
                        list.set(currPos, d);
                        madeChange = true;
                    }
                }
            }

            incomingSources.remove(null);
            if (!incomingSources.isEmpty()) {
                // if the added dependencies came from programmatic sources,
                // then we interpret them as a complete list from that source.
                // scan the existing list to see if it contains items from
                // the same sources that are no longer applicable.  If so,
                // remove them.
                for (Iterator i = list.iterator(); i.hasNext();) {
                    EVTaskDependency d = (EVTaskDependency) i.next();
                    if (incomingSources.contains(d.getSource())
                            && !dependencies.contains(d)) {
                        i.remove();
                        madeChange = true;
                    }
                }
            }
        }

        if (madeChange && whatIfMode == false)
            saveDependencies(data, taskPath, list);

        return madeChange;
    }

    public static List getAllDependencies(DataContext data, String taskPath,
            String ignoreIndividual) {
        if (taskPath == null || taskPath.length() == 0)
            return null;

        LinkedHashMap result = new LinkedHashMap();
        Map waitingIndividuals = null;
        EVTaskDependency collaborators = null;

        while (taskPath != null && taskPath.length() > 1) {
            List taskDep = getDependencies(data, taskPath);
            if (taskDep != null)
                for (Iterator i = taskDep.iterator(); i.hasNext();) {
                    EVTaskDependency d = (EVTaskDependency) i.next();
                    result.put(d.taskID, d);
                }

            waitingIndividuals = EVTaskDependencyResolver.getInstance()
                    .getIndividualsWaitingOnTask(waitingIndividuals,
                            getTaskIDs(data, taskPath), ignoreIndividual);

            if (collaborators == null)
                collaborators = getTaskCollaborators(data, taskPath);

            taskPath = DataRepository.chopPath(taskPath);
        }

        List resultList = new ArrayList(result.size() + 1);
        resultList.addAll(result.values());

        if (waitingIndividuals != null && !waitingIndividuals.isEmpty())
            resultList.add(new EVTaskDependency(waitingIndividuals));

        if (collaborators != null)
            resultList.add(collaborators);

        return resultList;
    }

    public static EVTaskDependency getTaskCollaborators(DataContext data,
            String taskPath) {
        String dataName = DataRepository.createDataName(taskPath,
            COLLABORATORS_DATA_NAME);
        SimpleData sd = data.getSimpleValue(dataName);
        ListData ld = ListData.asListData(sd);
        if (ld == null || !ld.test())
            return null;
        else
            return new EVTaskDependency(ld.asList()); 
    }

    public static List<EVTaskDependency> getDependencies(DataContext data,
            String taskPath) {
        String dataName = DataRepository.createDataName(taskPath,
                TASK_DEPENDENCIES_DATA_NAME);
        SimpleData currentValue = data.getSimpleValue(dataName);
        if (currentValue == null || !currentValue.test())
            return null;

        List<EVTaskDependency> result = new LinkedList<EVTaskDependency>();
        try {
            Element e = XMLUtils.parse(currentValue.format())
                    .getDocumentElement();
            NodeList nl = e.getElementsByTagName(DEPENDENCY_TAG);
            for (int i = 0; i < nl.getLength(); i++) {
                result.add(new EVTaskDependency((Element) nl.item(i)));
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to parse dependencies for "
                    + taskPath, e);
        }
        return result;
    }

    public static void saveDependencies(DataContext data, String taskPath,
            Collection<EVTaskDependency> dependencies) {
        SimpleData value = null;

        if (dependencies != null && !dependencies.isEmpty()) {
            StringBuffer xml = new StringBuffer();
            xml.append("<list>");
            for (EVTaskDependency d : dependencies) {
                d.getAsXML(xml, false);
            }
            if (xml.length() > 6) {
                xml.append("</list>");
                value = new ImmutableStringData(xml.toString());
            }
        }

        String dataName = DataRepository.createDataName(taskPath,
                TASK_DEPENDENCIES_DATA_NAME);
        data.putValue(dataName, value);
    }


    public static final int CMP_NONE = 0;
    public static final int CMP_PLAN = 1;
    public static final int CMP_REPLAN = 2;
    public static final int CMP_FORECAST = 3;

    protected static int getDependencyComparisonDateType() {
        String setting = Settings.getVal("ev.dependencies.compareDates");
        if ("false".equalsIgnoreCase(setting) || "no".equalsIgnoreCase(setting)
                || "none".equalsIgnoreCase(setting))
            return CMP_NONE;
        else if ("plan".equalsIgnoreCase(setting))
            return CMP_PLAN;
        else if ("forecast".equalsIgnoreCase(setting))
            return CMP_FORECAST;
        else
            return CMP_REPLAN;    // default value
    }

    public static Date getDependencyComparisonDate(EVTask task) {
        return getDependencyComparisonDate(task, true);
    }

    public static Date getDependencyComparisonDate(EVTask task,
            boolean rejectNever) {
        Date needDate = null;
        switch (getDependencyComparisonDateType()) {
        case CMP_FORECAST:
            needDate = task.getForecastDate();

        case CMP_REPLAN:
            if (needDate == null
                    || (rejectNever && EVSchedule.NEVER.equals(needDate)))
                needDate = task.getReplanDate();

        case CMP_PLAN:
            if (needDate == null
                    || (rejectNever && EVSchedule.NEVER.equals(needDate)))
                needDate = task.getPlanDate();

            if (rejectNever && EVSchedule.NEVER.equals(needDate))
                needDate = null;
        }
        return needDate;
    }

    public static Date getDependencyComparisonDate(Element task) {
        Date needDate = null;
        switch (getDependencyComparisonDateType()) {
        case CMP_FORECAST:
            needDate = XMLUtils.getXMLDate(task, "fd");

        case CMP_REPLAN:
            if (needDate == null || EVSchedule.NEVER.equals(needDate))
                needDate = XMLUtils.getXMLDate(task, "rpd");

        case CMP_PLAN:
            if (needDate == null || EVSchedule.NEVER.equals(needDate))
                needDate = XMLUtils.getXMLDate(task, "pd");

            if (EVSchedule.NEVER.equals(needDate))
                needDate = null;
        }
        return needDate;
    }



    private static final String TASK_ID_DATA_NAME = "EV_Task_IDs";

    private static final String TASK_DEPENDENCIES_DATA_NAME = "EV_Task_Dependencies";

    private static final String COLLABORATORS_DATA_NAME = "Collaborator_Names";

    protected static final String DEPENDENCY_TAG = "dependency";

    protected static final String TASK_ID_ATTR = "tid";

    private static final String DISPLAY_NAME_ATTR = "name";

    private static final String TASK_LIST_ATTR = "taskList";

    private static final String SOURCE_ATTR = "source";

    private static final String ASSIGNED_TO_ATTR = "who";

    private static final String PERCENT_COMPLETE_ATTR = "pctComplete";

    // the string on the next line is not a typo.  It was originally
    // created as "planDate", and altering this text would break backward
    // compatibility of data imported/exported by older dashboards.
    private static final String PROJ_DATE_ATTR = "planDate";

    public static final String REVERSE_PSEUDO_TASK = "REVERSE";

    public static final String COLLAB_PSEUDO_TASK = "COLLAB";

}
