// Copyright (C) 2007-2010 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.util.EnumerIterator;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.IteratorConcatenator;
import net.sourceforge.processdash.util.StringMapper;
import net.sourceforge.processdash.util.TempFileFactory;

public class ImportedTimeLogManager {

    private static final ImportedTimeLogManager INSTANCE = new ImportedTimeLogManager();

    public static ImportedTimeLogManager getInstance() {
        return INSTANCE;
    }

    Map importedLogs;

    private ImportedTimeLogManager() {
        importedLogs = Collections.synchronizedMap(new HashMap());
    }


    /**
     * Remove the time log associated with the given prefix from our cache.
     */
    public void closeTimeLogs(String prefix) {
        File oldFile = (File) importedLogs.remove(cleanupPrefix(prefix));
        if (oldFile != null)
            oldFile.delete();
    }

    /**
     * Empty our cache of all data and cleanup any temporary files we have
     * created.
     */
    public void dispose() {
        for (Object f : importedLogs.values())
            ((File) f).delete();
        importedLogs.clear();
    }

    /**
     * Import a time log, and associate it with the given prefix. If another
     * time log is already present in the cache with the same prefix, it will be
     * replaced by the incoming time log.
     * 
     * @param prefix the prefix where the time log should be mounted
     * @param timeLogData an input stream containing XML time log data
     */
    public void importTimeLog(String prefix, InputStream timeLogData)
            throws IOException {
        File oldFile;
        prefix = cleanupPrefix(prefix);

        // read the time log entries from the stream
        Iterator timeLogEntries = new TimeLogReader(timeLogData, false);

        if (!timeLogEntries.hasNext()) {
            // if the incoming time log is empty, don't bother saving it. Just
            // delete any time log for the given prefix.
            oldFile = (File) importedLogs.remove(prefix);

        } else {
            // prepend the prefix to all time log entries
            timeLogEntries = new TimeLogEntryVOPathFilter(timeLogEntries,
                    new PrefixAppender(prefix));

            // create a temporary file, and copy the frobbed entries to it
            File destFile = TempFileFactory.get().createTempFile(
                "tempImportedTimeLog", ".xml");
            TimeLogWriter.write(destFile, timeLogEntries);

            // save the new file into our map of imported log files.
            oldFile = (File) importedLogs.put(prefix, destFile);
        }

        // cleanup after the old file, if it was present.
        if (oldFile != null)
            oldFile.delete();
    }

    /**
     * Return an iterator of the time log entries mounted at the given prefix.
     * If no imported defects are mounted at the given prefix, returns null.
     */
    public EnumerIterator getImportedTimeLogEntries(String prefix)
            throws IOException {
        prefix = cleanupPrefix(prefix);
        List timeLogIterators = new ArrayList();

        Map snapshot;
        synchronized (importedLogs) {
            snapshot = new HashMap(importedLogs);
        }
        for (Iterator i = snapshot.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String importedPrefix = (String) e.getKey();
            File file = (File) e.getValue();

            if (Filter.pathMatches(importedPrefix, prefix)) {
                // this set of imported time log data is either (a) mounted
                // at the exact prefix requested, or (b) mounted at a prefix
                // underneath the one requested. In either case, we should
                // return all imported entries.
                Iterator content = new TimeLogReader(file);
                timeLogIterators.add(content);
            } else if (Filter.pathMatches(prefix, importedPrefix)) {
                // in this case, the requested prefix identifies a subset of
                // the imported time log data. We must filter that imported
                // data to return the requested entries.
                Iterator content = new TimeLogReader(file);
                Iterator filteredContent = new TimeLogIteratorFilter(content,
                        prefix, null, null);
                if (filteredContent.hasNext())
                    timeLogIterators.add(filteredContent);
            }
        }

        if (timeLogIterators.isEmpty())
            return null;
        else if (timeLogIterators.size() == 1)
            return (EnumerIterator) timeLogIterators.get(0);
        else
            return new IteratorConcatenator(timeLogIterators);
    }

    private String cleanupPrefix(String prefix) {
        while (prefix.length() > 1 && prefix.endsWith("/"))
            prefix = prefix.substring(0, prefix.length() - 1);
        if (!prefix.startsWith("/"))
            prefix = "/" + prefix;
        return prefix;
    }

    private void touchOldCachedFiles() {
        Map snapshot;
        synchronized (importedLogs) {
            snapshot = new HashMap(importedLogs);
        }
        for (Iterator i = snapshot.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String importedPrefix = (String) e.getKey();
            File file = (File) e.getValue();
            long lastMod = file.lastModified();
            if (lastMod < 1) continue;

            long age = System.currentTimeMillis() - lastMod;
            if (age < DAY_MILLIS) continue;

            File newFile = touchCachedFile(file);
            if (newFile == null) continue;

            boolean abortChange = false;
            synchronized (importedLogs) {
                // check to make certain the mapping for this prefix hasn't
                // been altered since we began this loop
                if (importedLogs.get(importedPrefix) == file)
                    importedLogs.put(importedPrefix, newFile);
                else
                    abortChange = true;
            }

            if (abortChange)
                newFile.delete();
            else
                file.delete();
        }
    }

    private File touchCachedFile(File oldFile) {
        try {
            File newFile = TempFileFactory.get().createTempFile(
                "tempImportedTimeLog", ".xml");

            FileUtils.copyFile(oldFile, newFile);
            return newFile;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Filter for TimeLogEntryVO objects, that prepends a prefix to the path of
     * each entry.
     */
    private static class PrefixAppender implements StringMapper {
        String prefixToPrepend;

        public PrefixAppender(String prefixToPrepend) {
            this.prefixToPrepend = prefixToPrepend;
        }

        public String getString(String path) {
            return prefixToPrepend + path;
        }
    }

    /**
     * Background task that can run once a day to keep our cached temp files
     * from expiring
     */
    public static class CacheMinder implements Runnable {
        public void run() {
            getInstance().touchOldCachedFiles();
        }
    }

    private static final long DAY_MILLIS = 24L /*hours*/ * 60 /*minutes*/
            * 60 /*seconds*/ * 1000 /*milis*/;
}
