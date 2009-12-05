// Copyright (C) 2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util;

import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * This class provides a service similar to File.createTempFile(), with two
 * important enhancements:
 * 
 * <ul>
 * <li>It is possible to create directories as well as files</li>
 * <li>This will automatically clean up old/outdated temp files that were
 * created by this class, even if they were created by a previous process
 * that failed to clean them up properly.</li>
 * </ul>
 * 
 * Note that the auto-cleanup provisions are a safeguard against unusual
 * scenarios (for example, when an application crashes without cleaning up
 * its temp files).  Clients of this class are still encouraged to clean up
 * their temp files as conscientiously as possible.
 */
public class TempFileFactory {

    private static final String DEFAULT_QUALIFIER = "pdash-tmp-";

    private String qualifier;

    private String prefix;

    private String suffix;

    private int maxAgeDays = 2;

    private File tempDirectory = null;

    private boolean immutable = false;

    private volatile long lastCleanupTimestamp = 0;


    public TempFileFactory() {
        this(null, null, null);
    }

    public TempFileFactory(String qualifier) {
        this(qualifier, null, null);
    }

    public TempFileFactory(String prefix, String suffix) {
        this(null, prefix, suffix);
    }

    public TempFileFactory(String qualifier, String prefix, String suffix) {
        this.qualifier = (StringUtils.hasValue(qualifier)
                ? qualifier : DEFAULT_QUALIFIER);
        this.prefix = (prefix == null ? "" : prefix);
        this.suffix = (suffix == null ? "" : suffix);
    }

    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        checkImmutable();
        this.qualifier = qualifier;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        checkImmutable();
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        checkImmutable();
        this.suffix = suffix;
    }

    public int getMaxAgeDays() {
        return maxAgeDays;
    }

    public void setMaxAgeDays(int maxAgeDays) {
        if (maxAgeDays < 1)
            throw new IllegalArgumentException("maxAgeDays must be at least 1");
        checkImmutable();

        this.maxAgeDays = maxAgeDays;
    }

    public File getTempDirectory() {
        return tempDirectory;
    }

    public void setTempDirectory(File tempDir) {
        checkImmutable();
        this.tempDirectory = tempDir;
    }

    public boolean useTempSubdirectory(String subdirName) {
        checkImmutable();
        if (subdirName == null || subdirName.length() == 0)
            return false;
        try {
            File f = File.createTempFile("tmp", ".tmp");
            f.delete();
            File tmpDir = f.getParentFile();
            File subdir = new File(tmpDir, subdirName);
            if (subdir.isDirectory() || subdir.mkdirs()) {
                setTempDirectory(subdir);
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    public void markImmutable() {
        this.immutable = true;
    }

    private void checkImmutable() {
        if (immutable)
            throw new IllegalStateException(
                    "Cannot alter immutable TempFileFactory object");
    }

    public File createTempFile() throws IOException {
        return createTempFile(prefix, suffix);
    }

    public File createTempFile(String prefix, String suffix) throws IOException {
        if (!StringUtils.hasValue(prefix) && !StringUtils.hasValue(suffix))
            throw new IllegalArgumentException("No prefix or suffix was "
                    + "supplied for creating a temporary file.");
        if (prefix == null) prefix = "";
        if (suffix == null) suffix = "";
        if (tempDirectory != null) tempDirectory.mkdirs();
        Date now = new Date();
        String fullPrefix = qualifier + DATE_FMT.format(now) + "-" + prefix;
        File result = File.createTempFile(fullPrefix, suffix, tempDirectory);
        maybeCleanup(result.getParentFile());
        return result;
    }

    public File createTempDirectory() throws IOException {
        return createTempDirectory(prefix, suffix);
    }

    public File createTempDirectory(String prefix, String suffix)
            throws IOException {
        File result = createTempFile(prefix, suffix);
        if (result.delete() && result.mkdir())
            return result;
        else
            throw new IOException("Could not create temporary directory");
    }

    private void maybeCleanup(File cleanupDir) {
        try {
            maybeCleanupImpl(cleanupDir);
        } catch (Exception e) {}
    }

    private void maybeCleanupImpl(File cleanupDir) {
        if (cleanupDir == null)
            return;

        long now = System.currentTimeMillis();
        long lastCleanupAge = now - lastCleanupTimestamp;
        if (lastCleanupAge < HOUR_MILLIS)
            return;

        String[] filenames = cleanupDir.list();
        if (filenames == null)
            return;

        int qualifierLen = qualifier.length();
        for (String name : filenames) {
            // only examine files whose name starts with our qualifier
            if (!name.startsWith(qualifier))
                continue;

            // extract the datestamp from the filename
            Date when;
            try {
                String dateStr = name.substring(qualifierLen, qualifierLen
                        + DATE_FMT_LEN);
                when = DATE_FMT.parse(dateStr);
            } catch (Exception e) {
                continue;
            }

            // compute the age of the file, and compare to our cutoff.
            long age = now - when.getTime();
            if (age < maxAgeDays * DAY_MILLIS)
                continue;

            // the file/directory should be deleted.
            File f = new File(cleanupDir, name);
            if (f.isFile()) {
                f.delete();
            } else if (f.isDirectory()) {
                try {
                    FileUtils.deleteDirectory(f, true);
                } catch (IOException e) {}
            }
        }

        lastCleanupTimestamp = now;
    }

    private static TempFileFactory DEFAULT_INSTANCE = null;

    public static TempFileFactory get() {
        if (DEFAULT_INSTANCE == null) {
            DEFAULT_INSTANCE = AccessController.doPrivileged(
                new PrivilegedAction<TempFileFactory>() {
                    public TempFileFactory run() {
                        return createDefaultInstance();
                    }});
        }

        return DEFAULT_INSTANCE;
    }

    private static TempFileFactory createDefaultInstance() {
        Properties p = new Properties();
        try {
            p.load(TempFileFactory.class
                .getResourceAsStream("TempFileFactory.properties"));
        } catch (Exception e) {}

        String qual = p.getProperty("default-temp-file-qualifier");
        String pref = p.getProperty("default-temp-file-prefix");
        String suff = p.getProperty("default-temp-file-suffix");
        String age = p.getProperty("default-temp-file-max-age");
        String subdir = p.getProperty("default-temp-file-subdir");
        TempFileFactory result = new TempFileFactory(qual, pref, suff);
        if (age != null) {
            try {
                result.setMaxAgeDays(Integer.parseInt(age));
            } catch (Exception nfe) {}
        }
        result.useTempSubdirectory(subdir);
        result.markImmutable();
        return result;
    }

    private static final DateFormat DATE_FMT = new SimpleDateFormat(
            "yyyyMMddHH");
    private static final int DATE_FMT_LEN = 10;

    private static final long HOUR_MILLIS =
        60 /*minutes*/ * 60 /*seconds*/ * 1000 /*millis*/;

    private static final long DAY_MILLIS = 24 /*hours*/ * HOUR_MILLIS;

}
