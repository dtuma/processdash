// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2006 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ev;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ImmutableStringData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class EVTaskDependency {

    private static final Logger logger = Logger
            .getLogger(EVTaskDependency.class.getName());

    private String taskID;

    private String displayName;

    private String taskListName;

    private boolean unresolvable;

    private String assignedTo;

    private double percentComplete;

    public EVTaskDependency(String taskID, String displayName) {
        this.taskID = taskID;
        this.displayName = displayName;
    }

    public EVTaskDependency(Element e) {
        this.taskID = getAttr(e, TASK_ID_ATTR);
        this.displayName = getAttr(e, DISPLAY_NAME_ATTR);
        this.taskListName = getAttr(e, TASK_LIST_ATTR);
        this.assignedTo = getAttr(e, ASSIGNED_TO_ATTR);
        this.percentComplete = XMLUtils.getXMLNum(e, PERCENT_COMPLETE_ATTR);
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

    public String getAssignedTo() {
        return assignedTo;
    }

    public double getPercentComplete() {
        return percentComplete;
    }

    public boolean isUnresolvable() {
        return unresolvable;
    }

    public void setResolvedDetails(boolean unresolvable, String assignedTo,
            double percentComplete, String displayName) {
        this.unresolvable = unresolvable;
        this.assignedTo = assignedTo;
        this.percentComplete = percentComplete;
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
        if (indent != null)
            out.append(indent);
        out.append("<").append(DEPENDENCY_TAG);
        addAttr(out, TASK_ID_ATTR, getTaskID());
        addAttr(out, DISPLAY_NAME_ATTR, getDisplayName());
        addAttr(out, TASK_LIST_ATTR, getTaskListName());
        if (includeResolvedInformation) {
            addAttr(out, ASSIGNED_TO_ATTR, getAssignedTo());
            if (percentComplete > 0)
                addAttr(out, PERCENT_COMPLETE_ATTR, Double
                        .toString(percentComplete));
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

        boolean madeChange = false;
        List list = getDependencies(data, taskPath);
        if (list == null) {
            list = dependencies;
            madeChange = true;
        } else {
            for (Iterator i = dependencies.iterator(); i.hasNext();) {
                EVTaskDependency d = (EVTaskDependency) i.next();
                if (!list.contains(d)) {
                    list.add(d);
                    madeChange = true;
                }
            }
        }

        if (madeChange && whatIfMode == false)
            saveDependencies(data, taskPath, list);

        return madeChange;
    }

    public static List getDependencies(DataContext data, String taskPath) {
        String dataName = DataRepository.createDataName(taskPath,
                TASK_DEPENDENCIES_DATA_NAME);
        SimpleData currentValue = data.getSimpleValue(dataName);
        if (currentValue == null || !currentValue.test())
            return null;

        List result = new LinkedList();
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
            Collection dependencies) {
        SimpleData value = null;

        if (dependencies != null && !dependencies.isEmpty()) {
            StringBuffer xml = new StringBuffer();
            xml.append("<list>");
            for (Iterator i = dependencies.iterator(); i.hasNext();) {
                EVTaskDependency d = (EVTaskDependency) i.next();
                d.getAsXML(xml, false);
            }
            xml.append("</list>");
            value = new ImmutableStringData(xml.toString());
        }

        String dataName = DataRepository.createDataName(taskPath,
                TASK_DEPENDENCIES_DATA_NAME);
        data.putValue(dataName, value);
    }


    private static final String TASK_ID_DATA_NAME = "EV_Task_IDs";

    private static final String TASK_DEPENDENCIES_DATA_NAME = "EV_Task_Dependencies";

    protected static final String DEPENDENCY_TAG = "dependency";

    private static final String TASK_ID_ATTR = "tid";

    private static final String DISPLAY_NAME_ATTR = "name";

    private static final String TASK_LIST_ATTR = "taskList";

    private static final String ASSIGNED_TO_ATTR = "who";

    private static final String PERCENT_COMPLETE_ATTR = "pctComplete";

}
