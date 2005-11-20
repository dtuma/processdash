// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import net.sourceforge.processdash.data.*;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.repository.InvalidDatafileFormat;
import net.sourceforge.processdash.log.ImportedDefectManager;
import net.sourceforge.processdash.tool.export.impl.ArchiveMetricsFileImporter;
import net.sourceforge.processdash.tool.export.impl.TextMetricsFileImporter;
import net.sourceforge.processdash.util.EscapeString;
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

    public static void init(DataRepository data, String userSetting) {
        if (userSetting == null || userSetting.length() == 0) return;

        StringTokenizer tok = new StringTokenizer(userSetting, "|;");
        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();
            int separatorPos = token.indexOf("=>");
            if (separatorPos == -1) continue;

            String prefix = massagePrefix(token.substring(0, separatorPos));
            String dir = token.substring(separatorPos+2);
            addImport(data, prefix, dir);
        }
    }

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
        prefix = massagePrefix(prefix);
        Iterator i = importers.values().iterator();
        DataImporter importer;
        while (i.hasNext()) {
            importer = (DataImporter) i.next();
            if (importer.importPrefix != null &&
                importer.importPrefix.startsWith(prefix)) {
                System.out.println("checking "+importer.importPrefix+
                                   "=>"+importer.directory);
                importer.checkFiles();
            }
        }
    }

    public DataImporter(DataRepository data, String prefix, File directory) {
        if (!directory.isDirectory()) return;

        this.data = data;
        this.importPrefix = prefix;
        this.directory = directory;

        checkFiles();
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
            checkFiles();
        } catch (InterruptedException e) {}
    }

    private void checkFiles() {
        try {
            Set currentFiles = new HashSet(modTimes.keySet());

            // list the files in the import directory.
            File [] files = directory.listFiles();

            // check them all to see if they need importing.
            for (int i = files.length;  i-- > 0;  ) {
                checkFile(files[i]);
                currentFiles.remove(files[i]);
            }

            // if any previously imported files no longer exist, close
            // the corresponding datafiles.
            Iterator i = currentFiles.iterator();
            while (i.hasNext())
                closeFile((File) i.next());

        } catch (IOException ioe) {}
    }

    public void dispose() {
        for (Iterator i = modTimes.keySet().iterator(); i.hasNext();) {
            closeFile((File) i.next());
        }
    }


    private void checkFile(File f) throws IOException {
        // ignore temporary files created by the RobustFileWriter class.
        if (f == null || f.getName() == null ||
            f.getName().startsWith(RobustFileOutputStream.OUT_PREFIX))
            return;

        // only open text and pdash files
        String filename = f.getName().toLowerCase();
        if (!filename.endsWith(EXPORT_FILE_OLD_SUFFIX)
                && !filename.endsWith(EXPORT_FILE_SUFFIX))
            return;

        Long prevModTime = (Long) modTimes.get(f);
        long modTime = f.lastModified();

        // If this file is new (we've never seen it before), or if has
        // been modified since we imported it last,
        if (prevModTime == null || prevModTime.longValue() < modTime) {
            importData(f, data);                   // import it, and
            modTimes.put(f, new Long(modTime));    // save its mod time
        }
    }

    private void closeFile(File f) {
        String prefix = (String) prefixes.get(f);
        if (prefix == null) return;
        data.closeDatafile(prefix);
        ImportedDefectManager.closeDefects(prefix);
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
