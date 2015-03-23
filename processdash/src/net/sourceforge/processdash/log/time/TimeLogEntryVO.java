// Copyright (C) 2005 Tuma Solutions, LLC
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

import java.util.Date;

import net.sourceforge.processdash.log.ChangeFlagged;


public class TimeLogEntryVO implements ChangeFlaggedTimeLogEntry, Cloneable, Comparable {

    long ID;

    String path;

    Date startTime;

    long elapsedTime;

    long interruptTime;

    String comment;

    int flag;

    public TimeLogEntryVO(TimeLogEntry tle) {
        this(tle.getID(), tle.getPath(), tle.getStartTime(),
                tle.getElapsedTime(), tle.getInterruptTime(), tle.getComment(),
                (tle instanceof ChangeFlagged
                        ? ((ChangeFlagged) tle).getChangeFlag()
                        : NO_CHANGE));
    }

    public TimeLogEntryVO(TimeLogEntry tle, int flag) {
        this(tle.getID(), tle.getPath(), tle.getStartTime(),
                tle.getElapsedTime(), tle.getInterruptTime(), tle.getComment(),
                flag);
    }

    public TimeLogEntryVO(long id, String path, Date startTime,
            long elapsedTime, long interruptTime, String comment) {
        this(id, path, startTime, elapsedTime, interruptTime, comment,
                NO_CHANGE);
    }

    public TimeLogEntryVO(long id, String path, Date startTime,
            long elapsedTime, long interruptTime, String comment, int flag) {
        this.ID = id;
        this.path = path;
        this.startTime = startTime;
        this.elapsedTime = elapsedTime;
        this.interruptTime = interruptTime;
        this.comment = comment;
        this.flag = flag;
    }


    public long getID() {
        return ID;
    }

    public String getPath() {
        return path;
    }

    public Date getStartTime() {
        return startTime;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public long getInterruptTime() {
        return interruptTime;
    }

    public String getComment() {
        return comment;
    }

    public int getChangeFlag() {
        return flag;
    }

    public static TimeLogEntry applyChanges(TimeLogEntry base, TimeLogEntry diff, boolean merge) {
        if (diff == null)
            return base;
        if (base.getID() != diff.getID())
            throw new IllegalArgumentException();

        String path = (String) nvl(diff.getPath(), base.getPath());
        Date startTime = (Date) nvl(diff.getStartTime(), base.getStartTime());
        long elapsedTime = diff.getElapsedTime() + base.getElapsedTime();
        long interruptTime = diff.getInterruptTime() + base.getInterruptTime();
        String comment = (String) nvl(diff.getComment(), base.getComment());
        if ("".equals(comment) && merge == false)
            comment = null;

        if (merge == true && base instanceof MutableTimeLogEntry) {
            MutableTimeLogEntry mbase = (MutableTimeLogEntry) base;
            mbase.setPath(path);
            mbase.setStartTime(startTime);
            mbase.setElapsedTime(elapsedTime);
            mbase.setInterruptTime(interruptTime);
            mbase.setComment(comment);
            return base;
        } else {
            int flag = NO_CHANGE;
            if (base instanceof ChangeFlagged)
                flag = ((ChangeFlagged) base).getChangeFlag();
            return new TimeLogEntryVO(base.getID(), path, startTime, elapsedTime, interruptTime, comment, flag);
        }
    }

    private static Object nvl(Object a, Object b) {
        if (a != null)
            return a;
        else
            return b;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (!(obj instanceof TimeLogEntry)) return false;
        TimeLogEntry that = (TimeLogEntry) obj;

        if (this.ID != that.getID()) return false;
        if (!eql(this.startTime, that.getStartTime())) return false;
        if (this.elapsedTime != that.getElapsedTime()) return false;
        if (this.interruptTime != that.getInterruptTime()) return false;
        if (!eql(this.comment, that.getComment())) return false;

        int thatFlag = NO_CHANGE;
        if (that instanceof ChangeFlagged)
            thatFlag = ((ChangeFlagged) that).getChangeFlag();
        return this.flag == thatFlag;
    }

    private boolean eql(Object a, Object b) {
        if (a == b)
            return true;
        else if (a == null)
            return false;
        else
            return a.equals(b);
    }

    public int hashCode() {
        long value = (ID ^ hc(path) ^ hc(startTime)
                ^ (elapsedTime << 4 | interruptTime << 2 | flag) ^ hc(comment));
        return (int) (value ^ (value >>> 32));
    }

    private int hc(Object o) {
        if (o == null)
            return 0;
        else
            return o.hashCode();
    }

    public int compareTo(Object that) {
        return TimeLogEntryComparator.INSTANCE.compare(this, that);
    }

}
