// Copyright (C) 2001-2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.ImportedEVManager;
import net.sourceforge.processdash.log.defects.ImportedDefectManager;
import net.sourceforge.processdash.log.time.ImportedTimeLogManager;
import net.sourceforge.processdash.security.DashboardPermission;
import net.sourceforge.processdash.tool.bridge.client.DynamicImportDirectory;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectory;
import net.sourceforge.processdash.tool.export.impl.ArchiveMetricsFileImporter;
import net.sourceforge.processdash.tool.export.impl.TextMetricsFileImporter;
import net.sourceforge.processdash.tool.export.mgr.ImportInstructionSpecProvider;
import net.sourceforge.processdash.tool.export.mgr.ImportManager;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.XMLUtils;



/* This class imports data files into the repository */
public class DataImporter extends Thread {

    public static final String EXPORT_FILE_OLD_SUFFIX = ".txt";
    public static final String EXPORT_FILE_SUFFIX = ".pdash";
    public static final String EXPORT_DATANAME = "EXPORT_FILE";

    private static final long TIME_DELAY = 10 * 60 * 1000; // 10 minutes
    private static Hashtable<String, DataImporter> importers = new Hashtable();
    private static List initializingImporters = Collections
            .synchronizedList(new ArrayList());
    private static boolean DYNAMIC_IMPORT = true;
    private static DashboardPermission SET_DYNAMIC_PERMISSION =
        new DashboardPermission("dataImporter.setDynamic");
    private static DashboardPermission ADD_IMPORT_PERMISSION =
        new DashboardPermission("dataImporter.addImport");
    private static DashboardPermission REMOVE_IMPORT_PERMISSION =
        new DashboardPermission("dataImporter.addImport");
    private static Logger logger = Logger.getLogger(DataImporter.class
            .getName());

    private DataRepository data;
    private String importPrefix;
    private ImportDirectory directory;
    private Element instructionSpec;
    private ActionListener listener;
    private volatile boolean isRunning = true;
    private Map<String, Long> modTimes = new HashMap<String, Long>();
    private Map<String, String> prefixes = new HashMap<String, String>();


    public static void setDynamic(boolean d) throws IllegalStateException {
        if (DYNAMIC_IMPORT == d)
            return;

        SET_DYNAMIC_PERMISSION.checkPermission();
        if (!importers.isEmpty())
            throw new IllegalStateException(
                    "setDynamic must be called before any imports are registered");

        DYNAMIC_IMPORT = d;
    }
    public static void addImport(DataRepository data, String prefix,
            String dirInfo, ImportDirectory importDir,
            ImportInstructionSpecProvider specProvider, ActionListener l) {
        ADD_IMPORT_PERMISSION.checkPermission();
        String key = getKey(prefix, dirInfo);
        DataImporter i = new DataImporter(data, prefix, importDir,
                specProvider, l);
        importers.put(key, i);
    }
    public static void removeImport(String prefix, String dirInfo) {
        REMOVE_IMPORT_PERMISSION.checkPermission();
        String key = getKey(prefix, dirInfo);
        DataImporter i = (DataImporter) importers.remove(key);
        if (i != null)
            i.dispose();
    }
    public static void waitForAllInitialized() {
        synchronized (initializingImporters) {
            while (!initializingImporters.isEmpty())
                try {
                    initializingImporters.wait();
                } catch (InterruptedException e) {
                }
        }
    }
    public static void shutDown() {
        for (Iterator i = importers.values().iterator(); i.hasNext();) {
            DataImporter imp = (DataImporter) i.next();
            imp.quit();
        }
        importers.clear();
        ImportedTimeLogManager.getInstance().dispose();
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
    public static List<String> refreshPrefixWithFeedback(String prefix) {
        List<String> result = new ArrayList<String>();
        prefix = massagePrefix(prefix);
        Iterator i = importers.values().iterator();
        DataImporter importer;
        while (i.hasNext()) {
            importer = (DataImporter) i.next();
            if (importer.importPrefix != null &&
                importer.importPrefix.startsWith(prefix)) {
                logger.info("checking " + importer.importPrefix + "=>"
                        + importer.directory.getDescription());
                importer.checkFiles(result);
            }
        }
        return result;
    }
    public static void refreshCachedFiles() {
        for (DataImporter importer : importers.values())
            importer.refreshIfCached();
    }

    private DataImporter(DataRepository data, String prefix,
            ImportDirectory importDir,
            ImportInstructionSpecProvider specProvider, ActionListener l) {
        this.data = data;
        this.importPrefix = prefix;
        this.directory = importDir;
        this.listener = l;

        if (specProvider != null)
            loadInstructionSpec(specProvider);

        if (PARALLEL_INIT && DYNAMIC_IMPORT)
            initializingImporters.add(this);
        else
            checkFiles(null);

        this.setDaemon(true);
        if (DYNAMIC_IMPORT)
            this.start();
    }

    public void refreshIfCached() {
        if (directory instanceof DynamicImportDirectory
                && ((DynamicImportDirectory) directory).needsCacheUpdate())
            checkFiles(null);
    }

    public void quit() {
        isRunning = false;
        this.interrupt();
    }


    public void run() {
        if (PARALLEL_INIT) {
            checkFiles(null);
            synchronized (initializingImporters) {
                initializingImporters.remove(this);
                if (initializingImporters.isEmpty())
                    initializingImporters.notifyAll();
            }
        }
        while (isRunning) try {
            sleep(TIME_DELAY);
            if (isRunning)
                checkFiles(null);
        } catch (InterruptedException e) {}
    }

    private void loadInstructionSpec(ImportInstructionSpecProvider provider) {
        try {
            // ask the spec provider for the instructions we should use
            // when importing data from this directory
            String dirID = getImportDirectoryID();
            this.instructionSpec = provider.getImportInstructionSpec(dirID);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to read import spec", e);
        }
    }

    private String getImportDirectoryID() throws IOException, SAXException {
        // Look in the directory to see if it contains a settings file
        directory.update();
        File settings = new File(directory.getDirectory(), "settings.xml");
        if (!settings.isFile())
            return null;

        // if so, retrieve the alphanumeric project ID for this directory
        InputStream in = new BufferedInputStream(new FileInputStream(
                settings));
        Element xml = XMLUtils.parse(in).getDocumentElement();
        String projectId = xml.getAttribute("projectID");

        // return the value, if one was found.
        if (XMLUtils.hasValue(projectId))
            return projectId;
        else
            return null;
    }

    private synchronized void checkFiles(List<String> feedback) {
        try {
            FILE_IO_LOCK.acquireUninterruptibly();
            Set<String> currentFilenames = new HashSet<String>(modTimes
                    .keySet());

            // list the files in the import directory.
            directory.update();
            File [] files = getFilesToImport();

            // check them all to see if they need importing.
            for (int i = files.length;  i-- > 0;  ) {
                String filename = files[i].getName();
                try {
                    if (checkFile(files[i]))
                        if (feedback != null)
                            feedback.add(getDescription(files[i]));
                    currentFilenames.remove(filename);
                } catch (Throwable t) {
                    // if an error is encountered when trying to import one
                    // of the files, log a message and attempt to continue
                    // with the remaining files.
                    String errMsg = "Error importing file '" + files[i] +"'";
                    logger.log(Level.SEVERE, errMsg, t);
                }
            }

            // if any previously imported files no longer exist, close
            // the corresponding datafiles.
            for (String filename : currentFilenames)
                closeFile(filename);

        } catch (IOException ioe) {
            logger.log(Level.FINE, "IOException in DataImporter", ioe);
        } finally {
            FILE_IO_LOCK.release();
        }
    }

    private File[] getFilesToImport() {
        // get a list of files in the directory
        File[] files = directory.getDirectory().listFiles();
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

        else if (filename.startsWith("."))
            // ignore invisible system files created on Unix or Mac systems.
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

    private String getDescription(File file) {
        return getDescription(file.getName());
    }

    private String getDescription(String filename) {
        String directoryDescription = directory.getDescription();
        if (directoryDescription == null)
            return filename;
        else if (directoryDescription.indexOf('/') != -1)
            return directoryDescription + "/" + filename;
        else
            return directoryDescription + "\\"  + filename;
    }

    public synchronized void dispose() {
        isRunning = false;
        for (String filename : new ArrayList<String>(modTimes.keySet())) {
            closeFile(filename);
        }
    }


    private boolean checkFile(File f) throws IOException {
        String filename = f.getName();
        Long prevModTime = modTimes.get(filename);
        long modTime = f.lastModified();

        // If this file is new (we've never seen it before), or if has
        // been modified since we imported it last,
        if (prevModTime == null || prevModTime.longValue() < modTime) {
            importData(f, data);                   // import it, and
            modTimes.put(filename, modTime);       // save its mod time
            return true;
        }

        return false;
    }

    private void closeFile(String filename) {
        String prefix = prefixes.get(filename);
        if (prefix == null) return;
        logger.info("closing import " + getDescription(filename));
        closeImportedFile(data, prefix);
        modTimes.remove(filename);
    }

    public static void closeImportedFile(DataRepository data, String prefix) {
        data.closeDatafile(prefix);
        ImportedDefectManager.closeDefects(prefix);
        ImportedTimeLogManager.getInstance().closeTimeLogs(prefix);
        ImportedEVManager.getInstance().closeTaskLists(prefix);
    }


    public void importData(File f, DataRepository data)
        throws IOException
    {
        String prefix = makePrefix(f);
        logger.info("importing " + f);

        String filename = f.getName().toLowerCase();
        if (filename.endsWith(EXPORT_FILE_OLD_SUFFIX)) {
            TextMetricsFileImporter task = new TextMetricsFileImporter(data, f, prefix);
            task.doImport();
        } else if (filename.endsWith(EXPORT_FILE_SUFFIX)) {
            ArchiveMetricsFileImporter task = new ArchiveMetricsFileImporter(
                    data, f, prefix, instructionSpec);
            task.doImport();
        }

        prefixes.put(f.getName(), prefix);
        if (listener != null)
            listener.actionPerformed(new ActionEvent(this,
                    ImportManager.FILE_IMPORTED, f.getPath()));
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

    private static final Semaphore FILE_IO_LOCK = makeFileIOLock();
    private static Semaphore makeFileIOLock() {
        int maxOperations = Settings.getInt("slowNetwork.numParallelReads", 10);
        return new Semaphore(maxOperations);
    }
    private static final boolean PARALLEL_INIT = Settings.getBool(
        "dataImporter.parallelInit", false);
}
