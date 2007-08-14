// Copyright (C) 2003-2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

package net.sourceforge.processdash.tool.export;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.log.defects.ImportedDefectManager;
import net.sourceforge.processdash.log.time.ImportedTimeLogManager;
import net.sourceforge.processdash.tool.export.impl.ArchiveMetricsFileImporter;
import net.sourceforge.processdash.tool.export.impl.TextMetricsFileImporter;
import net.sourceforge.processdash.util.RobustFileOutputStream;



/* This class imports data files into the repository */
public class DataImporter extends Thread {

    public static final String EXPORT_FILE_OLD_SUFFIX = ".txt";
    public static final String EXPORT_FILE_SUFFIX = ".pdash";
    public static final String EXPORT_DATANAME = "EXPORT_FILE";

    private static final long TIME_DELAY = 10 * 60 * 1000; // 10 minutes
    private static Hashtable importers = new Hashtable();

    private DataRepository data;
    private String importPrefix;
    private File directory;
    private volatile boolean isRunning = true;
    private HashMap modTimes = new HashMap();
    private HashMap prefixes = new HashMap();


    public static void addImport(DataRepository data, String prefix, String dir) {
        DataImporter i = new DataImporter(data, prefix, new File(dir));
        importers.put(getKey(prefix, dir), i);
    }
    public static void removeImport(String prefix, String dir) {
        DataImporter i = (DataImporter) importers.remove(getKey(prefix, dir));
        if (i != null)
            i.dispose();
    }

    private static String getKey(String prefix, String dir) {
        return prefix+"=>"+dir.replace(File.separatorChar, '/');
    }


    private static String massagePrefix(String p) {
        p = p.replace(File.separatorChar, '/');
        if (!p.startsWith("/"))
            p = "/" + p;
        return p;
    }

    public static void refreshPrefix(String prefix) {
        refreshPrefixWithFeedback(prefix);
    }
    public static List refreshPrefixWithFeedback(String prefix) {
        List result = new ArrayList();
        prefix = massagePrefix(prefix);
        Iterator i = importers.values().iterator();
        DataImporter importer;
        while (i.hasNext()) {
            importer = (DataImporter) i.next();
            if (importer.importPrefix != null &&
                importer.importPrefix.startsWith(prefix)) {
                System.out.println("checking "+importer.importPrefix+
                                   "=>"+importer.directory);
                importer.checkFiles(result);
            }
        }
        return result;
    }

    public DataImporter(DataRepository data, String prefix, File directory) {
        if (!directory.isDirectory()) return;

        this.data = data;
        this.importPrefix = prefix;
        this.directory = directory;

        checkFiles(null);
        this.setDaemon(true);
        this.start();
    }

    public void quit() {
        isRunning = false;
        this.interrupt();
    }


    public void run() {
        while (isRunning) try {
            sleep(TIME_DELAY);
            checkFiles(null);
        } catch (InterruptedException e) {}
    }

    private void checkFiles(List feedback) {
        try {
            Set currentFiles = new HashSet(modTimes.keySet());

            // list the files in the import directory.
            File [] files = getFilesToImport();

            // check them all to see if they need importing.
            for (int i = files.length;  i-- > 0;  ) {
                if (checkFile(files[i]))
                    if (feedback != null)
                        feedback.add(files[i]);
                currentFiles.remove(files[i]);
            }

            // if any previously imported files no longer exist, close
            // the corresponding datafiles.
            Iterator i = currentFiles.iterator();
            while (i.hasNext())
                closeFile((File) i.next());

        } catch (IOException ioe) {}
    }

    private File[] getFilesToImport() {
        // get a list of files in the directory
        File[] files = directory.listFiles();
        if (files == null)
            return new File[0];

        // look through all the files, and make a list of the ones that we
        // should import. Files whose names do not match recognized patterns
        // are discarded. If a file appears more than once with different
        // recognized suffixes, only keep the newest version.
        Map baseFileLists = new HashMap();
        for (int i = 0; i < files.length; i++) {
            File oneFile = files[i];
            String basename = getBaseImportName(oneFile.getName());
            if (basename != null && oneFile.isFile()) {
                List oneBaseList = (List) baseFileLists.get(basename);
                if (oneBaseList == null) {
                    oneBaseList = new ArrayList();
                    baseFileLists.put(basename, oneBaseList);
                }
                oneBaseList.add(oneFile);
            }
        }

        // now find the newest file for each base filename.
        List results = new ArrayList();
        for (Iterator i = baseFileLists.values().iterator(); i.hasNext();) {
            List oneBaseList = (List) i.next();
            results.add(getNewestFile(oneBaseList));
        }

        // return the results we found.
        return (File[]) results.toArray(new File[0]);
    }

    /** If the file is one that could be imported, return its filename, in
     * lowercase, without the suffix. Otherwise, return null.
     */
    private String getBaseImportName(String filename) {
        filename = filename.toLowerCase();

        if (filename.startsWith(RobustFileOutputStream.OUT_PREFIX))
            // ignore temporary files created by the RobustFileWriter class.
            return null;

        else if (filename.endsWith(EXPORT_FILE_OLD_SUFFIX))
            // accept files whose name ends with the old export suffix.
            return filename.substring(0,
                    filename.length() - EXPORT_FILE_OLD_SUFFIX.length());

        else if (filename.endsWith(EXPORT_FILE_SUFFIX))
            // accept files whose name ends with the new export suffix.
            return filename.substring(0,
                    filename.length() - EXPORT_FILE_SUFFIX.length());

        else
            // reject other files.
            return null;
    }

    /** Return the newest file from a list.
     * 
     * Previously, we were sorting files using FileAgeComparator, then returning
     * the first item in the list.  However, FileAgeComparator performs as many
     * as  n<sup>2</sup> calls to File.lastModified(). When network file I/O is
     * very slow, this can take forever.  The method below is optimized for
     * the minimal number of File I/O calls.
     */
    private Object getNewestFile(List baseFiles) {
        if (baseFiles == null || baseFiles.isEmpty())
            throw new IllegalArgumentException();
        else if (baseFiles.size() == 1)
            return baseFiles.get(0);  // most common case

        Iterator i = baseFiles.iterator();
        File result = (File) i.next();
        long resultLastMod = result.lastModified();

        while (i.hasNext()) {
            File oneFile = (File) i.next();
            long oneFileLastMod = oneFile.lastModified();
            if (oneFileLastMod > resultLastMod) {
                result = oneFile;
                resultLastMod = oneFileLastMod;
            }
        }

        return result;
    }

    public void dispose() {
        for (Iterator i = modTimes.keySet().iterator(); i.hasNext();) {
            closeFile((File) i.next());
        }
    }


    private boolean checkFile(File f) throws IOException {
        Long prevModTime = (Long) modTimes.get(f);
        long modTime = f.lastModified();

        // If this file is new (we've never seen it before), or if has
        // been modified since we imported it last,
        if (prevModTime == null || prevModTime.longValue() < modTime) {
            importData(f, data);                   // import it, and
            modTimes.put(f, new Long(modTime));    // save its mod time
            return true;
        }

        return false;
    }

    private void closeFile(File f) {
        String prefix = (String) prefixes.get(f);
        if (prefix == null) return;
        System.out.println("closing import " + f);
        data.closeDatafile(prefix);
        ImportedDefectManager.closeDefects(prefix);
        ImportedTimeLogManager.getInstance().closeTimeLogs(prefix);
        modTimes.remove(f);
    }


    public void importData(File f, DataRepository data)
        throws IOException
    {
        String prefix = makePrefix(f);
        System.out.println("importing " + f);

        String filename = f.getName().toLowerCase();
        if (filename.endsWith(EXPORT_FILE_OLD_SUFFIX)) {
            TextMetricsFileImporter task = new TextMetricsFileImporter(data, f, prefix);
            task.doImport();
        } else if (filename.endsWith(EXPORT_FILE_SUFFIX)) {
            ArchiveMetricsFileImporter task = new ArchiveMetricsFileImporter(data, f, prefix);
            task.doImport();
        }

        prefixes.put(f, prefix);
    }

    public String makePrefix(File f) throws IOException {
        return DataRepository.createDataName(importPrefix, makeExtraPrefix(f));
    }

    public static String getPrefix(String importPrefix, File f)
        throws IOException
    {
        return DataRepository.createDataName(importPrefix, makeExtraPrefix(f));
    }

    private static String makeExtraPrefix(File f) throws IOException {
        return Integer.toString(Math.abs(f.getCanonicalFile().hashCode()));
    }
}
