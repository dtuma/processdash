// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Vector;


public class TimeLog {

    /** When a line in the time log file begins with this flag, it is
     *  considered to be a replacement for the preceeding line in the
     *  file.
     */
    public static final String CONTINUATION_FLAG = "*";

    static final long MILLI_PER_MINUTE = 60000; // 60 sec/min * 1000 milli/sec
    static private String defaultTimeLogFile = null;

    Vector v = new Vector();


    public TimeLog () {
        TimeLogEntry.sortByPhase =
            "true".equalsIgnoreCase(Settings.getVal("timeLog.sortByPhase"));
    }

    public static void setDefaultFilename(String filename) {
        defaultTimeLogFile = filename;
    }

    public void readDefault() throws IOException {
        if (defaultTimeLogFile == null)
            throw new IOException();
        else
            read(defaultTimeLogFile);
    }

    public void read (String logFile) throws IOException {

        BufferedReader in = new BufferedReader(new FileReader(logFile));

        String line;
        TimeLogEntry tle;

        v.removeAllElements();
        while ((line = in.readLine()) != null) {
            try {
                if (line.startsWith(CONTINUATION_FLAG)) {
                    line = line.substring(CONTINUATION_FLAG.length());
                    v.remove(v.size() - 1);
                }
                tle = TimeLogEntry.valueOf(line);
                v.addElement (tle);
            } catch (IllegalArgumentException iae) { }
        }
        in.close();
        sort();
    }

    public TimeLogEntry add (TimeLogEntry tle) {
        v.addElement (tle);
        return tle;
    }

    /** @return the TimeLogEntry that was replaced. */
    public TimeLogEntry addOrUpdate (TimeLogEntry tle) {
        // search through the time log for an entry with the same create
        // date and path as tle.  If we find one, replace it with tle.
        TimeLogEntry anEntry;
        for (int i = v.size();  i-- > 0; ) {
            anEntry = (TimeLogEntry) v.elementAt(i);
            if (tle != null && anEntry != null &&
                tle.isSimilarTo(anEntry)) {
                v.setElementAt(tle, i);
                return anEntry;
            }
        }
        // The time log doesn't contain an entry matching tle.  Add tle to the log.
        add(tle);
        return null;
    }

    public void remove (TimeLogEntry tle) {
        v.removeElement (tle);
    }


    // save writes the logFile data to a temporary file.  After the save is
    // complete, the temporary file is renamed to the actual log file.
    public void save (String logFile) throws IOException {

        String fileSep = System.getProperty("file.separator");

        File logfile = new File(logFile);

        // Create temporary files
        File tempFile = new File(logfile.getParent() + fileSep +
                                                            "tttt_tim.log");
        File backupFile = new File(logfile.getParent() + fileSep +
                                   "tttttime.log");

        BufferedWriter out = new BufferedWriter (new FileWriter (tempFile));
        BufferedWriter backup = new BufferedWriter (new FileWriter (backupFile));

        // write the logfile outputs to the temporary files
        for (int ii = 0; ii < v.size(); ii++) {
            out.write (((TimeLogEntry)v.elementAt(ii)).toString());
            backup.write (((TimeLogEntry)v.elementAt(ii)).toString());
        }
        //close the temporary output files
        out.close();
        backup.close();

        // rename out to the real datafile
        logfile.delete();
        tempFile.renameTo(logfile);

        // delete the backup
        backupFile.delete();
    }

    public long timeOf (PropertyKey k, boolean childrenAlso) {
        long         t = 0;
        TimeLogEntry tle;
        for (int i = 0; i < v.size(); i++) {
            tle = (TimeLogEntry)v.elementAt (i);
            if ((tle.key.key().equals (k.key())) ||
                (childrenAlso && tle.key.isChildOf (k)))
                t += tle.minutesElapsed;
        }
        return t;
    }

    public long timeOf (PropertyKey k, Date from, Date to,
                        boolean childrenAlso) {
        long         t  = 0;
        Date         endTime = new Date();
        boolean      ok;
        TimeLogEntry tle;
        if (k == null) {
            System.err.println ("TimeLog.timeOf: Getting time for null key");
            return t;
        }
        for (int i = 0; i < v.size(); i++) {
            tle = (TimeLogEntry)v.elementAt (i);
            if ((tle.key.key().equals (k.key())) ||
                (childrenAlso && tle.key.isChildOf (k))) {
                endTime.setTime (tle.createTime.getTime() +
                                 (tle.minutesElapsed * MILLI_PER_MINUTE));
                ok = true;
                if (tle.createTime != null) {
                    if ((from != null) && (endTime.before (from)))
                        ok = false;
                    if ((to != null) && (tle.createTime.after (to)))
                        ok = false;
                }
                if (ok)
                    t += tle.minutesElapsed;
            }
        }
        return t;
    }

    public Hashtable getTimes(Date from, Date to) {
        Hashtable times = new Hashtable(100);
        TimeLogEntry tle;
        Long t;
        Date endTime = new Date(0);

        for (int i = v.size(); i-- != 0; ) {
            tle = (TimeLogEntry) v.elementAt (i);

            if (tle.createTime != null) {
                if (from != null)
                    if (from.after(tle.createTime)) continue;

                if (to != null) {
                    endTime.setTime (tle.createTime.getTime() +
                                     (tle.minutesElapsed * MILLI_PER_MINUTE));
                    if (endTime.after(to)) continue;
                }
            }

            t = (Long) times.get(tle.key);
            times.put(tle.key, new Long(tle.minutesElapsed +
                                        (t == null ? 0 : t.longValue())));
        }

        return times;
    }

    public Enumeration filter (PropertyKey k, Date from, Date to) {
        return new FilterEnumeration (v, k, from, to);
    }

    public void sort () {
        sort(v, 0, v.size() - 1);
    }

    // the next 3 routines implement a quicksort of the vector.  The first two
    // routines are generic, but the third is customized for the TimeLogEntry
    // object.
    private void sort(Vector rgo, int nLow0, int nHigh0) {
        int nLow = nLow0;
        int nHigh = nHigh0;

        Object oMid;

        if (nHigh0 > nLow0) {
            oMid = rgo.elementAt ( (nLow0 + nHigh0) / 2 );

            while(nLow <= nHigh) {
                while((nLow < nHigh0) && lessThan(rgo.elementAt(nLow), oMid))
                    ++nLow;

                while((nLow0 < nHigh) && lessThan(oMid, rgo.elementAt(nHigh)))
                    --nHigh;

                if(nLow <= nHigh) {
                    swap(rgo, nLow++, nHigh--);
                }
            }

            if(nLow0 < nHigh) sort(rgo, nLow0, nHigh);

            if(nLow < nHigh0) sort(rgo, nLow, nHigh0);
        }
    }

    private void swap(Vector rgo, int i, int j) {
        Object o;

        o = rgo.elementAt(i);
        rgo.setElementAt (rgo.elementAt(j), i);
        rgo.setElementAt (o, j);
    }

    protected boolean lessThan(Object oFirst, Object oSecond) {
        return ((TimeLogEntry)oFirst).lessThan ((TimeLogEntry)oSecond);
    }

    public class FilterEnumeration implements Enumeration {
        PropertyKey filterKey;
        Date        fromDate;
        Date        toDate;
        int         nextIndex;
        Vector      v;

        protected void setIndex() {
            TimeLogEntry tle;
            Date         endTime = new Date();
            boolean      ok = false;
            nextIndex++;
            while (nextIndex < v.size()) {
                ok = true;
                tle = (TimeLogEntry)v.elementAt (nextIndex);
                endTime.setTime (tle.createTime.getTime() +
                                 (tle.minutesElapsed * MILLI_PER_MINUTE));
                if (filterKey != null) {
                    if ( !tle.key.key().equals(filterKey.key()) &&
                        (!tle.key.isChildOf (filterKey)))
                        ok = false;
                }
                if (tle.createTime != null) {
                    if ((fromDate != null) && (endTime.before (fromDate)))
                        ok = false;
                    if ((toDate != null) && (tle.createTime.after (toDate)))
                        ok = false;
                }
                if (ok)
                    return;
                nextIndex++;
            }
            nextIndex = -1;
        }

        public boolean hasMoreElements() {
            return (nextIndex != -1);
        }

        public Object nextElement() {
            if (nextIndex == -1)
                throw new NoSuchElementException ("No more elements");
            int index = nextIndex;
            setIndex ();
            return v.elementAt (index);
        }

        FilterEnumeration (Vector vec, PropertyKey k, Date from, Date to) {
            v         = vec;
            filterKey = k;
            fromDate  = from;
            toDate    = to;
            nextIndex = -1;
            setIndex ();
        }
    }

}
