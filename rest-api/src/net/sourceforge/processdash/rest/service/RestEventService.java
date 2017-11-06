// Copyright (C) 2017 Tuma Solutions, LLC
// REST API Add-on for the Process Dashboard
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

package net.sourceforge.processdash.rest.service;

import static net.sourceforge.processdash.rest.service.RestTaskService.JSON_ATTR_DATA_NAME_MAP;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.Timer;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.log.time.DashboardTimeLog;
import net.sourceforge.processdash.log.time.TimeLoggingModel;
import net.sourceforge.processdash.rest.to.RestEvent;
import net.sourceforge.processdash.util.PatternList;

public class RestEventService {

    private static RestEventService svc;

    public static RestEventService get() {
        if (svc == null)
            svc = new RestEventService();
        return svc;
    }



    private Map<String, RestEvent> events;

    private Timer eventArrivedTimer;


    private RestEventService() {
        events = new LinkedHashMap<String, RestEvent>();
        eventArrivedTimer = new Timer(10, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                eventArrived();
            }
        });
        eventArrivedTimer.setRepeats(false);

        listenForTimingEvents();
        listenForHierarchyEvents();
        listenForTaskListEvents();
        listenForDataEvents();
    }

    public List<RestEvent> eventsAfter(long id, long maxWait) {
        // retrieve the events whose ID is larger than the given value
        List<RestEvent> result = new ArrayList<RestEvent>();
        synchronized (events) {
            getEventsAfter(id, result);
            if (result.isEmpty()) {
                // if no events were found, wait for some to arrive
                try {
                    events.wait(maxWait);
                } catch (Exception e) {
                }
                getEventsAfter(id, result);
            }
        }

        // sort the events in numerical order and return them
        if (result.size() > 1)
            Collections.sort(result);
        return result;
    }

    private void getEventsAfter(long id, List<RestEvent> result) {
        for (RestEvent e : events.values()) {
            if (e.getId() > id)
                result.add(e);
        }
    }


    private void listenForTimingEvents() {
        DashboardContext ctx = RestDashContext.get();
        DashboardTimeLog timeLog = (DashboardTimeLog) ctx.getTimeLog();
        TimeLoggingModel tlm = timeLog.getTimeLoggingModel();
        tlm.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                String prop = evt.getPropertyName();
                if (TimeLoggingModel.PAUSED_PROPERTY.equals(prop)) {
                    RestEvent event = new RestEvent("timer");
                    event.put("timing", evt.getOldValue());
                    addEvent("TimerEvent", event);
                } else if (TimeLoggingModel.ACTIVE_TASK_PROPERTY.equals(prop)) {
                    String taskPath = (String) evt.getNewValue();
                    RestEvent event = new RestEvent("activeTask");
                    event.put("task", RestTaskService.get().loadData( //
                        RestTaskService.get().byPath(taskPath)));
                    addEvent("ActiveTaskEvent", event);
                }
            }
        });
    }

    private void listenForHierarchyEvents() {
        DashboardContext ctx = RestDashContext.get();
        DashHierarchy hier = ctx.getHierarchy();
        hier.addHierarchyListener(new DashHierarchy.Listener() {
            public void hierarchyChanged(DashHierarchy.Event e) {
                addEvent("HierarchyEvent", new RestEvent("hierarchy"));
            }
        });
    }

    private void listenForTaskListEvents() {
        _taskListSaveListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String taskListName = e.getActionCommand();
                RestEvent evt = new RestEvent("taskList");
                evt.put("name", taskListName);
                addEvent("TaskListEvent/" + taskListName, evt);
            }
        };
        EVTaskList.addTaskListSaveListener(_taskListSaveListener);
    }
    private ActionListener _taskListSaveListener;

    private void listenForDataEvents() {
        try {
            DashboardContext ctx = RestDashContext.get();
            DataRepository data = ctx.getData();
            PatternList p = new PatternList();
            for (String dataNameSuffix : JSON_ATTR_DATA_NAME_MAP.values())
                p.addLiteralEndsWith(dataNameSuffix);
            data.addDataListener(p, new DataListener() {
                public void dataValuesChanged(Vector v) {
                    for (Object e : v)
                        handleDataEvent((DataEvent) e);
                }
                public void dataValueChanged(DataEvent e) {
                    handleDataEvent(e);
                }
            });
        } catch (Throwable t) {
            // earlier versions of the dashboard do not provide the method
            // to register a data listener for a PatternList. When running
            // in an earlier version, this will throw a NoSuchMethodError.
            // Ignore the error, and don't provide these notifications.
        }
    }

    private void handleDataEvent(DataEvent e) {
        String dataName = e.getName();
        String jsonAttr = getJsonAttrForDataName(dataName);
        if (jsonAttr == null)
            return;
        String taskPath = dataName.substring(0, dataName.lastIndexOf('/'));

        RestEvent evt = new RestEvent("taskData");
        evt.set("name", jsonAttr);
        evt.set("task", RestTaskService.get()
                .loadData(RestTaskService.get().byPath(taskPath)));
        addEvent("DataEvent/" + dataName, evt);
    }

    private String getJsonAttrForDataName(String name) {
        for (Entry<String, String> e : JSON_ATTR_DATA_NAME_MAP.entrySet()) {
            if (name.endsWith(e.getValue()))
                return e.getKey();
        }
        return null;
    }


    private void addEvent(String key, RestEvent event) {
        synchronized (events) {
            events.remove(key);
            events.put(key, event);
            eventArrivedTimer.restart();
        }
    }

    private void eventArrived() {
        synchronized (events) {
            events.notifyAll();
        }
    }

}
