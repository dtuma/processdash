// Copyright (C) 2005-2014 Tuma Solutions, LLC
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.NumberFunction;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.repository.DataConsistencyEventSource;
import net.sourceforge.processdash.data.repository.DataConsistencyObserver;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.log.ChangeFlagged;

public class TimingMetricsRecorder implements TimeLogListener, DataConsistencyObserver {

    private ModifiableTimeLog timeLog;

    private DataContext data;

    private DashHierarchy hierarchy;

    private TimeLoggingApprover approver;

    private Set allPathsTouched;

    private Set pathsToCompute;



    public TimingMetricsRecorder(ModifiableTimeLog timeLog, DataContext data,
            DashHierarchy hierarchy, TimeLoggingApprover approver) {
        this.data = data;
        this.timeLog = timeLog;
        this.hierarchy = hierarchy;
        this.approver = approver;
        this.allPathsTouched = new HashSet();
        this.pathsToCompute = new HashSet();

        timeLog.addTimeLogListener(this);
    }

    public void refreshMetrics() {
        queueTiming(null);
    }

    public void dispose() {
        processQueue();
        timeLog.removeTimeLogListener(this);
    }

    public void timeLogChanged(TimeLogEvent e) {
        ChangeFlaggedTimeLogEntry tle = e.getTimeLogEntry();
        queueTiming(getPathToRecompute(tle));
        setStartTimeElements(tle);
    }

    private String getPathToRecompute(ChangeFlaggedTimeLogEntry tle) {
        if (tle != null
                && tle.getChangeFlag() == ChangeFlagged.ADDED
                && tle.getPath() != null)
            return tle.getPath();
        else
            return null;
    }

    protected void queueTiming(String basePath) {
        if (basePath == null) {
            pathsToCompute.clear();
            pathsToCompute.add(null);
        } else if (!pathsToCompute.contains(null)) {
            pathsToCompute.add(basePath);
        }
        if (data instanceof DataConsistencyEventSource)
            ((DataConsistencyEventSource) data).addDataConsistencyObserver(this);
        else
            processQueue();
    }

    protected void processQueue() {
        while (!pathsToCompute.isEmpty()) {
            String path = (String) pathsToCompute.iterator().next();
            if (saveTiming(path))
                pathsToCompute.remove(path);
            else
                break;
        }
    }

    protected boolean saveTiming(String basePath) {
        Map timings = getTimings(basePath);
        if (timings == null)
            return false;

        for (Iterator i = timings.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String pathName = (String) e.getKey();
            long[] pathValue = (long[]) e.getValue();

            String timeElementName = DataRepository.createDataName(
                    pathName, "Time");
            String orphanElementName = DataRepository.createDataName(
                pathName, "Orphaned Time");
            SaveableData currentVal = data.getValue(timeElementName);
            SaveableData orphanedVal = data.getValue(orphanElementName);

            if (pathValue == null) {
                // this is a parent node where time logging is not allowed,
                // and no time log entries were found there

                if (orphanedVal != null)
                    // if a previous orphan value exists, clear it.
                    data.putValue(orphanElementName, null);

                if (currentVal == null)
                    // the data repository agrees with us.  All is well.
                    continue;
                else if (!(currentVal instanceof DoubleData)
                        || (currentVal instanceof NumberFunction))
                    // something is in the repository, but we didn't put it
                    // there.  Leave it alone.
                    continue;
                else
                    // there is an existing number there;  erase it.
                    data.putValue(timeElementName, null);

            } else if (approver.isTimeLoggingAllowed(pathName) == false) {
                // the time log has entries for this path, but the time isn't
                // supposed to be logged there.  Record it as orphaned time.
                allPathsTouched.add(pathName);
                storeNumberIfChanged(orphanElementName, orphanedVal, pathValue);

            } else {
                allPathsTouched.add(pathName);
                storeNumberIfChanged(timeElementName, currentVal, pathValue);
                if (orphanedVal != null)
                    data.putValue(orphanElementName, null);
            }
        }

        return true;
    }

    private void storeNumberIfChanged(String dataName,
            SaveableData currentVal, long[] pathValue) {
        long pathTime = (pathValue)[0];
        if (currentVal instanceof NumberData
                && ((NumberData) currentVal).getDouble() == pathTime)
            return;

        data.putValue(dataName, new DoubleData(pathTime, false));
    }

    protected Map getTimings(String path) {
        try {
            Map result = new HashMap();
            initMapFromTouchedPaths(path, result);
            initMapFromHierarchy(path, result);
            addTimeFromTimeLog(path, result);
            reparentOrphanedTime(result);
            return result;
        } catch (IOException e) {
            return null;
        } catch (IONoSuchElementException e) {
            return null;
//        } catch (Exception e) {
//            return null;
        }
    }

    private void initMapFromTouchedPaths(String prefix, Map result) {
        for (Iterator iter = allPathsTouched.iterator(); iter.hasNext();) {
            String path = (String) iter.next();
            if (prefix == null || Filter.pathMatches(path, prefix, true))
                result.put(path, null);
        }
    }

    private void initMapFromHierarchy(String path, Map result) {
        PropertyKey startingKey = null;
        if (path == null)
            startingKey = PropertyKey.ROOT;
        else
            startingKey = hierarchy.findExistingKey(path);
        if (startingKey != null)
            initMapFromHierarchy(startingKey, result);
    }

    private void initMapFromHierarchy(PropertyKey key, Map result) {
        int numChildren = hierarchy.pget(key).getNumChildren();
        if (numChildren == 0) {
            getTime(result, key.path());
        } else {
            result.put(key.path(), null);
            while (numChildren-- > 0)
                initMapFromHierarchy(hierarchy.getChildKey(key, numChildren),
                        result);
        }
    }

    private void addTimeFromTimeLog(String path, Map result) throws IOException {
        Iterator i = timeLog.filter(path, null, null);
        while (i.hasNext()) {
            TimeLogEntry tle = (TimeLogEntry) i.next();
            String tlePath = tle.getPath();
            long[] totalTime = getTime(result, tlePath);
            totalTime[0] += tle.getElapsedTime();
        }
    }

    private void reparentOrphanedTime(Map result) {
        List orphanedEntries = new ArrayList();
        for (Iterator i = result.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String path = (String) e.getKey();
            String hierarchyPath = getHierarchyPath(path);
            if (!path.equals(hierarchyPath)) {
                long[] orphanedTime = (long[]) e.getValue();
                e.setValue(null);
                if (orphanedTime != null && orphanedTime[0] > 0)
                    orphanedEntries.add(new OrphanedEntry(path, hierarchyPath,
                            orphanedTime[0]));
            }
        }
        for (Iterator i = orphanedEntries.iterator(); i.hasNext();) {
            OrphanedEntry e = (OrphanedEntry) i.next();
            long[] ancestorTime = getTime(result, e.retargetedPath);
            ancestorTime[0] += e.time;
        }
    }

    private static class OrphanedEntry {
        public String retargetedPath;
        public long time;
        public OrphanedEntry(String orphanedPath, String retargetedPath, long time) {
            this.retargetedPath = retargetedPath;
            this.time = time;
        }

    }

    private long[] getTime(Map timeMap, String key) {
        long[] time = (long[]) timeMap.get(key);
        if (time == null) {
            time = new long[1];
            time[0] = 0;
            timeMap.put(key, time);
        }
        return time;
    }

    /** Returns the path to the existing element of the hierarchy which
     * is equivalent to or the first ancestor of the given path
     */
    private String getHierarchyPath(String path) {
        String result = hierarchy.findClosestKey(path).path();
        if (result.length() > 0)
            return result;
        else
            return "/";
    }

    protected void setStartTimeElements(TimeLogEntry tle) {
        if (tle instanceof ChangeFlagged
                && ((ChangeFlagged) tle).getChangeFlag() == ChangeFlagged.ADDED)
            setStartTimeElements(tle.getPath(), new DateData(tle.getStartTime(), true));
    }

    protected void setStartTimeElements(String path, DateData startTime) {
        if (path == null || path.length() == 0 || startTime == null)
            return;

        // calculate the name of the Start Date data element for this path.
        String dataName = DataRepository.createDataName(path, "Started");

        // if our start date has not already been set, set it to d
        if (data.getValue(dataName) == null)
            data.putValue(dataName, startTime);

        int slashPos = path.lastIndexOf('/');
        if (slashPos > 0)
            setStartTimeElements(path.substring(0, slashPos), startTime);
    }

    public void dataIsConsistent() {
        processQueue();
    }

}
