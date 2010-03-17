// Copyright (C) 2005-2010 Tuma Solutions, LLC
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.log.IDSource;
import net.sourceforge.processdash.log.SaveableDataSource;
import net.sourceforge.processdash.util.EnumerIterator;
import net.sourceforge.processdash.util.IteratorConcatenator;
import net.sourceforge.processdash.util.RobustFileWriter;
import net.sourceforge.processdash.util.TempFileFactory;

public class WorkingTimeLog implements ModifiableTimeLog, IDSource,
        SaveableDataSource {

    public static final String OLD_TIME_LOG_FILENAME = "time.log";

    public static final String TIME_LOG_FILENAME = "timelog.xml";

    public static final String TIME_LOG_MOD_FILENAME = "timelog2.xml";

    private static final String TIME_LOG_ENCODING = TimeLogIOConstants.ENCODING;

    private File directory;

    private BaseTimeLog historicalTimeLog;

    private TimeLogModifications realTimeMods;

    private long currentID = 0;

    public WorkingTimeLog(File directory) throws IOException {
        this.directory = directory;

        File histFile = ensureTimeLogFileExists(getFile(TIME_LOG_FILENAME));
        this.historicalTimeLog = new BaseTimeLog(histFile);

        File modFile = ensureTimeLogFileExists(getFile(TIME_LOG_MOD_FILENAME));
        this.realTimeMods = new TimeLogModifications(historicalTimeLog,
                modFile, this);

        maybeCleanup();
    }

    private File ensureTimeLogFileExists(File file) throws IOException {
        if (!file.exists()) {
            RobustFileWriter rout = new RobustFileWriter(file,
                    TIME_LOG_ENCODING);
            BufferedWriter out = new BufferedWriter(rout);
            TimeLogWriter.write(out, Collections.EMPTY_LIST.iterator());
        }
        return file;
    }

    public EnumerIterator filter(String path, Date from, Date to)
            throws IOException {
        return realTimeMods.filter(path, from, to);
    }

    public void addModification(ChangeFlaggedTimeLogEntry tle) {
        realTimeMods.addModification(tle);
    }

    public void addModifications(Iterator iter) {
        realTimeMods.addModifications(iter);
    }

    public void addTimeLogListener(TimeLogListener l) {
        realTimeMods.addTimeLogListener(l);
    }

    public void removeTimeLogListener(TimeLogListener l) {
        realTimeMods.removeTimeLogListener(l);
    }

    public synchronized long getNextID() {
        return ++currentID;
    }

    public CommittableModifiableTimeLog getDeferredTimeLogModifications() {
        return new TimeLogModifications(realTimeMods, this);
    }

    public boolean isDirty() {
        return realTimeMods.isDirty();
    }

    public void saveData() {
        realTimeMods.save();
    }

    public void reloadData() throws IOException {
        realTimeMods.maybeReloadData();
    }

    private File getFile(String filename) {
        return new File(this.directory, filename);
    }

    private synchronized void maybeCleanup() throws IOException {
        if (Settings.isReadOnly())
            return;

        File oldStyleFile = getFile(OLD_TIME_LOG_FILENAME);
        if (oldStyleFile.isFile() && oldStyleFile.length() > 0) {
            Iterator currentEntries = realTimeMods.filter(null, null, null);
            Iterator watcherCurrentEntries = new IDWatcher(currentEntries);
            Iterator extraEntries = new OldStyleTimeLogReader(oldStyleFile,
                    this);
            Iterator allEntries = new IteratorConcatenator(
                    watcherCurrentEntries, extraEntries);
            doCleanup(allEntries);
            getFile(OLD_TIME_LOG_FILENAME).delete();

        } else if (realTimeMods.isEmpty() == false) {
            Iterator currentEntries = realTimeMods.filter(null, null, null);
            Iterator watcherCurrentEntries = new IDWatcher(currentEntries);
            doCleanup(watcherCurrentEntries);

        } else {
            // no need to cleanup, but we still need to scan the existing log
            // to find out the highest numbered TimeLogEntry ID in use
            Iterator currentEntries = historicalTimeLog
                    .filter(null, null, null);
            Iterator watcherCurrentEntries = new IDWatcher(currentEntries);
            while (watcherCurrentEntries.hasNext())
                watcherCurrentEntries.next();
        }
    }

    private synchronized void doCleanup(Iterator timeLogEntries)
            throws IOException {
        if (Settings.isReadOnly())
            return;

        // start by writing the output to a temporary file. Although we could
        // actually write directly to the destination file, that would be
        // relying on implementation details (of both TimeLogReader and
        // RobustFileWriter) that we should know nothing about.
        File tempFile = TempFileFactory.get().createTempFile("timelog", ".xml");
        RobustFileWriter rout = new RobustFileWriter(tempFile,
                TIME_LOG_ENCODING);
        BufferedWriter out = new BufferedWriter(rout);
        TimeLogWriter.write(out, timeLogEntries);
        out.close();
        long checksum1 = rout.getChecksum();

        // Now, copy the file we just created to its real destination.
        File destFile = getFile(TIME_LOG_FILENAME);
        Reader in = new BufferedReader(new InputStreamReader(
                new FileInputStream(tempFile), TIME_LOG_ENCODING));
        rout = new RobustFileWriter(destFile, TIME_LOG_ENCODING);
        out = new BufferedWriter(rout);
        int c;
        while (true) {
            c = in.read();
            if (c == -1)
                break;
            out.write(c);
        }
        out.flush();

        // we're done with the temporary file.  Close and delete it.
        try {
            in.close();
            tempFile.delete();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        long checksum2 = rout.getChecksum();
        if (checksum1 != checksum2) {
            // if the contents we just copied didn't match the first version of
            // the file we wrote, abort. This would only happen in the rare
            // circumstance when we wrote and verified one thing to the temp
            // file, then read it back differently.
            rout.abort();
            throw new IOException("Unable to save time log to file " + destFile);
        } else {
            rout.close();
            realTimeMods.clear();
        }
    }

    void updateCurrentId(TimeLogEntry next) {
        if (next != null) {
            synchronized (WorkingTimeLog.this) {
                currentID = Math.max(currentID, next.getID());
            }
        }
    }

    private class IDWatcher implements Iterator {
        private Iterator i;

        public IDWatcher(Iterator i) {
            this.i = i;
            remove(); // no-op
        }

        public boolean hasNext() {
            return i.hasNext();
        }

        public Object next() {
            TimeLogEntry next = (TimeLogEntry) i.next();
            updateCurrentId(next);
            return next;
        }

        public void remove() {
            // not supported or necessary
        }

    }

}
