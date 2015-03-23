// Copyright (C) 2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import net.sourceforge.processdash.tool.bridge.impl.DashboardInstanceStrategy;
import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollectionStrategy;
import net.sourceforge.processdash.tool.bridge.impl.TeamDataDirStrategy;
import net.sourceforge.processdash.tool.quicklauncher.CompressedInstanceLauncher;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.TempFileFactory;
import net.sourceforge.processdash.util.XorInputStream;
import net.sourceforge.processdash.util.XorOutputStream;
import net.sourceforge.processdash.util.lock.AlreadyLockedException;
import net.sourceforge.processdash.util.lock.LockFailureException;
import net.sourceforge.processdash.util.lock.LockMessageHandler;
import net.sourceforge.processdash.util.lock.SentLockMessageException;

/**
 * Working directory implementation that retrieves/stores data in a ZIP file.
 * 
 * At this time, this implementation is only suitable for use with WBS
 * directories.  (Process Dashboard data directories are not yet supported.)
 */
public class CompressedWorkingDirectory extends AbstractWorkingDirectory {

    private File extractDirectory;

    protected CompressedWorkingDirectory(File targetZipFile,
            FileResourceCollectionStrategy strategy, File workingDirParent) {
        super(targetZipFile, null, strategy, workingDirParent);
        if (!(strategy instanceof TeamDataDirStrategy))
            throw new UnsupportedOperationException();
    }

    @Override
    public void acquireProcessLock(String msg, LockMessageHandler lockHandler)
            throws SentLockMessageException, LockFailureException {
        if (Boolean.getBoolean(NO_PROCESS_LOCK_PROPERTY) == false)
            super.acquireProcessLock(msg, lockHandler);
    }

    public void prepare() throws IOException {
        this.extractDirectory = TempFileFactory.get().createTempDirectory(
            "pdash-compressed-wd", ".tmp");

        File srcZip = getTargetZipFile();
        InputStream in = new FileInputStream(srcZip);
        if (isPdbk(srcZip))
            in = new XorInputStream(in, PDBK_XOR_BITS);

        boolean sawEntry = false;
        try {
            ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(
                    in));
            ZipEntry e;
            while ((e = zipIn.getNextEntry()) != null) {
                sawEntry = true;
                if (!e.isDirectory()) {
                    String filename = e.getName();
                    File f = new File(extractDirectory, filename);
                    if (filename.indexOf('/') != -1)
                        f.getParentFile().mkdirs();
                    FileUtils.copyFile(zipIn, f);
                    f.setLastModified(e.getTime());
                }
            }
        } finally {
            FileUtils.safelyClose(in);
        }
        if (!sawEntry)
            // When we try to open a file that is NOT a ZIP file, the loop
            // above may exit immediately without throwing an IOException.
            // check for that case and throw one ourselves.
            throw new ZipException("Not a valid ZIP file: " + srcZip);
    }

    public void update() throws IllegalStateException, IOException {
        throw new IllegalStateException("Not supported");
    }

    public void acquireWriteLock(LockMessageHandler lockHandler,
            String ownerName) throws AlreadyLockedException,
            LockFailureException {
        // no-op for now
    }

    public void assertWriteLock() throws LockFailureException {
        // no-op for now
    }

    public void releaseWriteLock() {
        // no-op for now
    }

    public URL doBackup(String qualifier) throws IOException {
        return super.doBackupImpl(extractDirectory, qualifier);
    }

    public boolean flushData() throws LockFailureException, IOException {
        List<String> filesToBackup = FileUtils.listRecursively(
            extractDirectory, CWD_FILE_FILTER);

        File destZip = getTargetZipFile();
        RobustFileOutputStream rOut = new RobustFileOutputStream(destZip);
        OutputStream out = rOut;
        if (isPdbk(destZip))
            out = new XorOutputStream(out, PDBK_XOR_BITS);

        try {
            ZipOutputStream zipOut = new ZipOutputStream(
                    new BufferedOutputStream(out));
            for (String filename : filesToBackup) {
                File f = new File(extractDirectory, filename);
                ZipEntry e = new ZipEntry(filename);
                e.setTime(f.lastModified());
                zipOut.putNextEntry(e);
                FileUtils.copyFile(f, zipOut);
                zipOut.closeEntry();
            }
            zipOut.finish();
            zipOut.flush();
        } catch (IOException ioe) {
            rOut.abort();
            throw ioe;
        }

        out.close();
        return true;
    }

    public File getDirectory() {
        return extractDirectory;
    }

    public File getTargetZipFile() {
        // the "targetDirectory" data field in the parent class is used to hold
        // the name of the ZIP file, even though it is slightly misnamed.
        return targetDirectory;
    }

    public void releaseLocks() {
        if (processLock != null)
            processLock.releaseLock();

        try {
            FileUtils.deleteDirectory(extractDirectory, true);
        } catch (IOException e) {
        }
    }

    private static boolean isPdbk(File f) {
        return f.getName().toLowerCase().endsWith(PDBK);
    }

    public static boolean isZipFormat(String filename) {
        String lower = filename.toLowerCase();
        return (lower.endsWith(".zip") || lower.endsWith(PDBK));
    }

    public static boolean isZipFileContaining(File f, String expectedFile) {
        if (!f.isFile() || !f.canRead())
            return false;

        boolean foundExpectedFile = false;
        InputStream in = null;
        try {
            // open the file in question
            in = new BufferedInputStream(new FileInputStream(f));
            if (isPdbk(f)) in = new XorInputStream(in, PDBK_XOR_BITS);
            // we use a ZipInputStream and scan the entire file, to ensure
            // that the file is intact and not corrupt near the end.
            ZipInputStream zipIn = new ZipInputStream(in);
            ZipEntry e;
            while ((e = zipIn.getNextEntry()) != null)
                if (e.getName().equalsIgnoreCase(expectedFile))
                    foundExpectedFile = true;
        } catch (Exception e) {
        }
        FileUtils.safelyClose(in);
        return foundExpectedFile;
    }

    /**
     * We do not ask the strategy for advice on which files we should ZIP up.
     * Instead, we assume that if we unzipped it, we should zip it back up
     * again. We only make exceptions for the lock files (which probably won't
     * be readable anyway) and the backup subdirectory.
     */
    private static class CWDFileFilter implements FilenameFilter {

        private Set<String> filesToSkip;

        public CWDFileFilter() {
            filesToSkip = new HashSet<String>();
            filesToSkip.add("backup/");
            filesToSkip.add(DashboardInstanceStrategy.LOCK_FILE_NAME);
            filesToSkip.add(TeamDataDirStrategy.LOCK_FILE_NAME);
        }

        public boolean accept(File dir, String name) {
            return !filesToSkip.contains(name);
        }

    }

    private static CWDFileFilter CWD_FILE_FILTER = new CWDFileFilter();

    private static final String PDBK = "." +
        CompressedInstanceLauncher.PDASH_BACKUP_EXTENSION;
    private static final int PDBK_XOR_BITS =
        CompressedInstanceLauncher.PDASH_BACKUP_XOR_BITS;

}
