// Copyright (C) 2020 Tuma Solutions, LLC
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

package teamdash.sync;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.processdash.util.DateUtils;


public class ElapsedTimeMonitor {

    private long observationWindowMillis;

    private List<Observation> observations;

    long lastStartTimestamp;


    public ElapsedTimeMonitor() {
        this(20);
    }

    public ElapsedTimeMonitor(int observationWindowMinutes) {
        this(observationWindowMinutes, 0);
    }

    public ElapsedTimeMonitor(int observationWindowMinutes,
            long placeholderMillis) {
        this.observationWindowMillis = observationWindowMinutes
                * DateUtils.MINUTES;
        this.observations = new LinkedList<ElapsedTimeMonitor.Observation>();
        if (placeholderMillis > 0)
            this.observations.add(new Observation(placeholderMillis, 0));
    }

    public void start() {
        this.lastStartTimestamp = System.currentTimeMillis();
    }

    public void finish() {
        finish(1);
    }

    public void finish(int completedItemCount) {
        // compute how long the current run has taken
        long now = System.currentTimeMillis();
        long elapsed = now - lastStartTimestamp;
        long elapsedPerItem = elapsed / Math.max(1, completedItemCount);

        // flush any observations that are older than our window
        for (Iterator<Observation> i = observations.iterator(); i.hasNext();) {
            if (i.next().expiration < now)
                i.remove();
        }

        // add the current observation to the end of the list
        observations.add(new Observation(elapsedPerItem, now));
    }

    public long getMaxTime() {
        long result = 0;
        for (Observation oneTime : observations)
            result = Math.max(result, oneTime.elapsedTime);
        return result;
    }

    public long getAverageTime() {
        long sum = 0;
        for (Observation oneTime : observations)
            sum += oneTime.elapsedTime;
        return (sum == 0 ? 0 : sum / observations.size());
    }


    private class Observation {

        private long elapsedTime;

        private long expiration;

        public Observation(long elapsedTime, long currentTime) {
            this.elapsedTime = elapsedTime;
            this.expiration = currentTime + observationWindowMillis;
        }

    }

}
