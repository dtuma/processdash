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

package pspdash.data;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/* This class imports data files into the repository */
public class DataImporter extends Thread {

    private static final long TIME_DELAY = 1 * 60 * 1000; // 10 minutes

    private DataRepository data;
    private String importPrefix;
    private File directory;
    private volatile boolean isRunning = true;
    private HashMap files = new HashMap();

    public static void init(DataRepository data, String userSetting) {
        if (userSetting == null || userSetting.length() == 0) return;

        StringTokenizer tok = new StringTokenizer(userSetting, ";");
        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();
            int separatorPos = token.indexOf("=>");
            if (separatorPos == -1) continue;

            String prefix = massagePrefix(token.substring(0, separatorPos));
            String dir = token.substring(separatorPos+2);
            DataImporter i = new DataImporter(data, prefix, new File(dir));
        }
    }

    private static String massagePrefix(String p) {
        p = p.replace(File.separatorChar, '/');
        if (!p.startsWith("/"))
            p = "/" + p;
        return p;
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
            // list the files in the import directory.
            File [] files = directory.listFiles();

            // check them all to see if they need importing.
            for (int i = files.length;  i-- > 0;  )
                checkFile(files[i]);

        } catch (IOException ioe) {}
    }

    private void checkFile(File f) throws IOException {
        Long prevModTime = (Long) files.get(f);
        long modTime = f.lastModified();

        // If this file is new (we've never seen it before), or if has
        // been modified since we imported it last,
        if (prevModTime == null || prevModTime.longValue() < modTime) {
            importData(f, data);                    // import it, and
            files.put(f, new Long(modTime));    // save its mod time
        }
    }


    public void importData(File f, DataRepository data)
        throws IOException
    {
        System.out.println("imporing " + f);
        importData(new FileInputStream(f), true, makeExtraPrefix(f), data);
    }

    public void importData(InputStream inputStream, boolean close,
                           String extraPrefix, DataRepository data)
        throws IOException
    {
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
            value = line.substring(commaPos+1);
            defns.put(name, parseValue(value));
        }

        // Protect this data from being viewed via external http requests.
        defns.put("_Password_", ImmutableDoubleData.READ_ONLY_ZERO);

        String prefix = DataRepository.createDataName(importPrefix,
                                                      extraPrefix);
        try {
            data.mountImportedData(prefix, defns);
        } catch (InvalidDatafileFormat idf) {}
    }

    private static String makeExtraPrefix(File f) throws IOException {
        return Integer.toString(f.getCanonicalFile().hashCode());
    }

    private static Object parseValue(String value) {

        // is it a tag?
        if ("TAG".equalsIgnoreCase(value))
            return TagData.getInstance();

        // first, try to interpret the string as a number.
        try {
            return new DoubleData(value);
        } catch (MalformedValueException mfe) {}

        // next, try to interpret the string as a date.
        try {
            return DateData.create(value);
        } catch (MalformedValueException mfe) {}

        // give up and interpret it as a plain string.
        return StringData.create(value);
    }

}
