// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2005 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.log.time;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.NumberFunction;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
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

    private List listeners;

    public static TimeLog DEFAULT = null;

    public DashboardTimeLog(File directory, DataContext data,
            DashHierarchy hierarchy) throws IOException {
        this.timeLog = new WorkingTimeLog(directory);
        this.data = data;
        this.hierarchy = hierarchy;

        this.listeners = new LinkedList();
        this.timeLog.addTimeLogListener(new TimeLogEventRepeater(this));

        this.loggingModel = new DefaultTimeLoggingModel(this, this);
        this.metricsRecorder = new TimingMetricsRecorder(this, data, hierarchy,
                this);
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

    public void repeatTimeLogEvent(TimeLogEvent origEvent) {
        TimeLogEvent e = new TimeLogEvent(this, origEvent.getTimeLogEntry());
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
            TimeLogListener l = (TimeLogListener) iter.next();
            l.timeLogChanged(e);
        }
    }



    public boolean isTimeLoggingAllowed(String path) {
        return timeLoggingAllowed(hierarchy.findExistingKey(path), hierarchy,
                data);
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

    public static boolean timeLoggingAllowed(PropertyKey node,
            DashHierarchy props, DataContext data) {
        if (node == null || props == null || data == null
                || Settings.isReadOnly())
            return false;

        // if the node has children, logging time here is not allowed.
        if (props.pget(node).getNumChildren() > 0)
            return false;

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
