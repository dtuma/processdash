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

package net.sourceforge.processdash.data.repository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import net.sourceforge.processdash.data.*;
import net.sourceforge.processdash.util.EscapeString;
import net.sourceforge.processdash.util.RobustFileWriter;



/* This class imports data files into the repository */
public class DataImporter extends Thread {

    public static final String EXPORT_DATANAME = "EXPORT_FILE";
    private static final long TIME_DELAY = 10 * 60 * 1000; // 10 minutes
    private static Vector importers = new Vector();

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
            DataImporter i = new DataImporter(data, prefix, new File(dir));
            importers.add(i);
        }
    }

    private static String massagePrefix(String p) {
        p = p.replace(File.separatorChar, '/');
        if (!p.startsWith("/"))
            p = "/" + p;
        return p;
    }

    public static void refreshPrefix(String prefix) {
        prefix = massagePrefix(prefix);
        Iterator i = importers.iterator();
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

    private void checkFile(File f) throws IOException {
        // ignore temporary files created by the RobustFileWriter class.
        if (f == null || f.getName() == null ||
            f.getName().startsWith(RobustFileWriter.OUT_PREFIX))
            return;
        // only open text files for now.
        if (!f.getName().endsWith(".txt")) return;

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
    }


    public void importData(File f, DataRepository data)
        throws IOException
    {
        String prefix = makePrefix(f);
        System.out.println("importing " + f);
        importData(new FileInputStream(f), true, prefix, data);
        prefixes.put(f, prefix);
    }

    public void importData(InputStream inputStream, boolean close,
                           String prefix, DataRepository data)
        throws IOException
    {
        try {
            BufferedReader in =
                new BufferedReader(new InputStreamReader(inputStream));
            Map defns = new HashMap();

            String line, name, value;
            int commaPos;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("!")) break;

                commaPos = line.indexOf(',');
                if (commaPos == -1) return; // this isn't a valid dump file.

                name = line.substring(1, commaPos);
                name = EscapeString.unescape(name, '\\', ",", "c");
                value = line.substring(commaPos+1);

                // don't import the data elements which contain export
                // instructions - this would get us into an infinite
                // import/export loop.
                if (name.indexOf(EXPORT_DATANAME) != -1) continue;

                // To the best of my knowledge, the DataImporter is
                // currently only being used to import individual
                // data, for the purpose of calculating team rollups.
                // Rollups interact with this data in a fairly
                // predictable way; for now, I'll take advantage of
                // this predictable behavior by omitting data elements
                // which I know cannot affect rollups.  This will
                // significantly reduce the memory requirements of the
                // team dashboard.  In particular, I:
                //
                // (1) won't import "To Date" data, and
                if (name.endsWith(" To Date")) continue;
                //
                // (2) won't import data values that are zero or invalid.
                if (value.equals("0.0") || value.equals("NaN") ||
                    value.equals("Infinity")) continue;

                defns.put(name, parseValue(value));
            }

            // Protect this data from being viewed via external http requests.
            defns.put("_Password_", ImmutableDoubleData.READ_ONLY_ZERO);

            try {
                // We don't want these threads to flood the data repository
                // with multiple simultaneous mountImportedData() calls, so
                // we'll synchronize on a lock object.
                synchronized(SYNCH_LOCK) {
                    data.mountImportedData(prefix, defns);
                }
            } catch (InvalidDatafileFormat idf) {}
        } finally {
            if (close) inputStream.close();
        }
    }

    private static final Object SYNCH_LOCK = new Object();

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

    private static Object parseValue(String value) {
        SimpleData result;

        // is it a tag?
        if ("TAG".equalsIgnoreCase(value))
            return TagData.getInstance();

        // first, try to interpret the string as a number.
        if ("0.0".equals(value))
            return ImmutableDoubleData.READ_ONLY_ZERO;
        if ("NaN".equals(value))
            return ImmutableDoubleData.READ_ONLY_NAN;
        if (DoubleData.P_INF_STR.equals(value) ||
            DoubleData.N_INF_STR.equals(value))
            return ImmutableDoubleData.DIVIDE_BY_ZERO;
        if (value.length() > 0) switch (value.charAt(0)) {
        case '0': case '1': case '2': case '3': case '4': case '5': case '6':
        case '7': case '8': case '9': case '-': case '+': case '.': case ',':
            try {
                result = new DoubleData(value);
                result.setEditable(false);
                return result;
            } catch (MalformedValueException mfe) {}
        }

        // next, try to interpret the string as a date.
        try {
            result = DateData.create(value);
            result.setEditable(false);
            return result;
        } catch (MalformedValueException mfe) {}

        // give up and interpret it as a plain string.
        result = StringData.create(StringData.unescapeString(value));
        result.setEditable(false);
        return result;
    }

}
