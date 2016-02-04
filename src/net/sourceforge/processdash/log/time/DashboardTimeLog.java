// Copyright (C) 2005-2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.time;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.NumberFunction;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.log.SaveableDataSource;
import net.sourceforge.processdash.util.EnumerIterator;

public class DashboardTimeLog implements ModifiableTimeLog,
        TimeLoggingApprover, SaveableDataSource {

    private WorkingTimeLog timeLog;

    private DataContext data;

    private DashHierarchy hierarchy;

    private TimeLoggingModel loggingModel;

    private TimingMetricsRecorder metricsRecorder;

    private List timingForbiddenPaths;

    private List listeners;

    public static TimeLog DEFAULT = null;

    public DashboardTimeLog(File directory, DataContext data,
            DashHierarchy hierarchy) throws IOException {
        this.timeLog = new WorkingTimeLog(directory);
        this.data = data;
        this.hierarchy = hierarchy;

        this.listeners = new LinkedList();
        this.timeLog.addTimeLogListener(new TimeLogEventRepeater(this));

        this.loggingModel = new DefaultTimeLoggingModel(this, this, data);
        this.metricsRecorder = new TimingMetricsRecorder(this, data, hierarchy,
                this);
        this.timingForbiddenPaths = Collections.EMPTY_LIST;
    }

    public CommittableModifiableTimeLog getDeferredTimeLogModifications() {
        return timeLog.getDeferredTimeLogModifications();
    }

    public void addModification(ChangeFlaggedTimeLogEntry tle) {
        timeLog.addModification(tle);
    }

    public void addModifications(Iterator iter) {
        timeLog.addModifications(iter);
    }

    public EnumerIterator filter(String path, Date from, Date to)
            throws IOException {
        return timeLog.filter(path, from, to);
    }

    public long getNextID() {
        return timeLog.getNextID();
    }

    public void addTimeLogListener(TimeLogListener l) {
        listeners.add(l);
    }

    public void removeTimeLogListener(TimeLogListener l) {
        listeners.remove(l);
    }

    public boolean isDirty() {
        return timeLog.isDirty();
    }

    public void saveData() {
        timeLog.saveData();
    }

    public void refreshMetrics() {
        metricsRecorder.refreshMetrics();
    }

    public void reloadData() throws IOException {
        timeLog.reloadData();
    }

    public void repeatTimeLogEvent(TimeLogEvent origEvent) {
        TimeLogEvent e = new TimeLogEvent(this, origEvent.getTimeLogEntry());
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
            TimeLogListener l = (TimeLogListener) iter.next();
            l.timeLogChanged(e);
        }
    }



    public void setTimingForbiddenPaths(List paths) {
        this.timingForbiddenPaths = paths;
    }

    public boolean isTimeLoggingAllowed(String path) {
        return timeLoggingAllowed(hierarchy.findExistingKey(path), hierarchy,
                data, timingForbiddenPaths);
    }

    public TimeLoggingModel getTimeLoggingModel() {
        return loggingModel;
    }

    public static void setDefault(TimeLog d) {
        DEFAULT = d;
    }

    /** @deprecated */
    public static TimeLog getDefault() {
        return DEFAULT;
    }

    static boolean timeLoggingAllowed(PropertyKey node,
            DashHierarchy props, DataContext data, List timingForbiddenPaths) {
        if (node == null || props == null || data == null
                || Settings.isReadOnly())
            return false;

        // if the node is in the list of forbidden paths, (or is a child of a
        // forbidden path), don't allow time to be logged there.
        if (Filter.matchesFilter(timingForbiddenPaths, node.path()))
            return false;

        // if the node has children, logging time here is not allowed, unless
        // the node explicitly defines a "Time_Logging_Allowed" marker.
        if (props.pget(node).getNumChildren() > 0) {
            String dataName = DataRepository.createDataName(node.path(),
                    "Time_Logging_Allowed");
            SimpleData marker = data.getSimpleValue(dataName);
            return (marker != null && marker.test());
        }

        // check to see if the current node defines time as a calculation.
        // if it does, logging time here is not allowed.
        String dataName = DataRepository.createDataName(node.path(), "Time");
        Object timeData = data.getValue(dataName);
        if (timeData == null)
            return true;
        if (!(timeData instanceof DoubleData))
            return false;
        if (timeData instanceof NumberFunction)
            return false;
        return true;
    }

    private static class TimeLogEventRepeater implements TimeLogListener {

        private WeakReference target;

        public TimeLogEventRepeater(DashboardTimeLog target) {
            this.target = new WeakReference(target);
        }

        public void timeLogChanged(TimeLogEvent e) {
            DashboardTimeLog timeLog = (DashboardTimeLog) target.get();
            if (timeLog != null)
                timeLog.repeatTimeLogEvent(e);
        }

    }

}
