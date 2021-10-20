// Copyright (C) 2008-2021 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.impl;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Adler32;

import net.sourceforge.processdash.tool.bridge.ResourceCollection;
import net.sourceforge.processdash.tool.bridge.ResourceCollectionInfo;
import net.sourceforge.processdash.tool.bridge.report.XmlCollectionListing;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.TimedInputStream;
import net.sourceforge.processdash.util.lock.ConcurrencyLock;
import net.sourceforge.processdash.util.lock.ConcurrencyLockApprover;
import net.sourceforge.processdash.util.lock.LockFailureException;
import net.sourceforge.processdash.util.lock.ReadOnlyLockFailureException;


public class FileResourceCollection implements ResourceCollection,
        ConcurrencyLockApprover {

    public static final String DELETED = "deleted";

    public static final String ADDED_OR_MODIFIED = "modified";


    File directory;

    FileResourceCollectionStrategy strategy;

    boolean lenientFilter;

    Map<String, CachedFileData> cachedData;

    volatile long cacheInvalidationTimestamp;

    PropertyChangeSupport propSupport;

    final Object writeLock = new Object();

    File mostRecentBackup = null;


    public FileResourceCollection(File directory) {
        this(directory, true, null, false);
    }

    public FileResourceCollection(File directory, boolean enablePropSupport) {
        this(directory, enablePropSupport, null, false);
    }

    public FileResourceCollection(File directory, boolean enablePropSupport,
            FileResourceCollectionStrategy strategy) {
        this(directory, enablePropSupport, strategy, false);
    }

    public FileResourceCollection(File directory, boolean enablePropSupport,
            FileResourceCollectionStrategy strategy, boolean lenientFilter) {
        this.directory = directory;
        this.strategy = strategy;
        this.lenientFilter = lenientFilter;
        this.cachedData = Collections
                .synchronizedMap(new HashMap<String, CachedFileData>());
        if (enablePropSupport)
            this.propSupport = new PropertyChangeSupport(this);
    }

    public void setStrategy(FileResourceCollectionStrategy strategy) {
        this.strategy = strategy;
    }

    public File getDirectory() {
        return directory;
    }

    public String getDescription() {
        return directory.getPath();
    }


    public void validate() throws IOException {
        if (!directory.isDirectory())
            throw new FileNotFoundException(directory.getPath());
    }


    public List<String> listResourceNames() {
        return FileUtils.listRecursively(directory, strategy.getFilenameFilter());
    }


    public long getLastModified(String resourceName) {
        CachedFileData fd = getFileData(resourceName);
        return (fd == null ? 0 : fd.getLastModified());
    }


    public Long getChecksum(String resourceName) {
        CachedFileData fd = getFileData(resourceName);
        return (fd == null ? null : fd.getChecksum());
    }


    public InputStream getInputStream(String resourceName) throws IOException {
        if (!checkResourceName(resourceName))
            return null;

        File f = new File(directory, resourceName);
        return new TimedInputStream(new BufferedInputStream(
                new FileInputStream(f)));
    }

    public OutputStream getOutputStream(String resourceName) throws IOException {
        return getOutputStream(resourceName, -1);
    }

    public OutputStream getOutputStream(String resourceName, long modTime)
            throws IOException {
        if (!checkResourceName(resourceName))
            return null;

        return new BufferedOutputStream(new FDCOutputStream(resourceName,
                modTime));
    }

    public void deleteResource(String resourceName) {
        if (checkResourceName(resourceName)) {
            File f = new File(directory, resourceName);
            synchronized (writeLock) {
                f.delete();
            }
            cachedData.remove(resourceName);
            if (propSupport != null)
                propSupport.firePropertyChange(resourceName, null, DELETED);
        }
    }

    public void backupCollection(String backupQualifier) throws IOException {
        synchronized (writeLock) {
            mostRecentBackup = strategy.getBackupHandler(directory).backup(
                backupQualifier);
        }
    }

    public InputStream getBackupInputStream() throws IOException {
        if (mostRecentBackup == null)
            throw new IOException("No backup made since restart");

        // note: this strategy will only work if we KNOW that people have
        // performed a "backupCollection" call immediately before calling
        // this method.  If any time intervenes, a legacy dashboard could
        // run a backup, and our "mostRecentBackup" could become an
        // incremental backup instead of a full backup.
        return new TimedInputStream(new BufferedInputStream(
            new FileInputStream(mostRecentBackup)));
    }

    public boolean requiresWriteLock(String resourceName) {
        FilenameFilter unlocked = strategy.getUnlockedFilter();
        if (unlocked == null)
            return true;
        else
            return !unlocked.accept(null, resourceName);
    }

    public Object getLockTarget() {
        return new File(directory, strategy.getLockFilename());
    }

    public void approveLock(ConcurrencyLock lock, String extraInfo)
            throws LockFailureException {
        for (String name : listResourceNames()) {
            File f = new File(directory, name);
            if (f.canWrite() == false)
                throw new ReadOnlyLockFailureException(f);
        }
    }

    public void recheckAllFileTimestamps() {
        cacheInvalidationTimestamp = System.currentTimeMillis() + 1;
    }

    public void loadFileDataCache(File cacheFile) {
        try {
            if (!cacheFile.isFile())
                return;
            ResourceCollectionInfo info = XmlCollectionListing.parseListing(
                new BufferedInputStream(new FileInputStream(cacheFile)));
            for (String fileName : info.listResourceNames()) {
                CachedFileData fileData = getFileData(fileName);
                if (fileData != null) {
                    fileData.lastModified = info.getLastModified(fileName);
                    fileData.checksum = info.getChecksum(fileName);
                }
            }
        } catch (IOException ioe) {
        }
    }

    public void saveFileDataCache(File cacheFile) {
        RobustFileOutputStream rOut = null;
        try {
            rOut = new RobustFileOutputStream(cacheFile);
            OutputStream out = new BufferedOutputStream(rOut);
            XmlCollectionListing.INSTANCE.runReport(this,
                new ArrayList<String>(cachedData.keySet()), out);
            out.close();
        } catch (IOException ioe) {
            if (rOut != null) {
                try {
                    rOut.abort();
                } catch (Exception e) {
                }
            }
        }
    }

    // Methods for resource change notification

    public void addResourceListener(PropertyChangeListener l) {
        checkPropSupport();
        propSupport.addPropertyChangeListener(l);
    }

    public void addResourceListener(String resourceName,
            PropertyChangeListener l) {
        checkPropSupport();
        propSupport.addPropertyChangeListener(resourceName, l);
    }

    public void removeResourceListener(PropertyChangeListener l) {
        checkPropSupport();
        propSupport.removePropertyChangeListener(l);
    }

    public void removeResourceListener(String resourceName,
            PropertyChangeListener l) {
        checkPropSupport();
        propSupport.removePropertyChangeListener(resourceName, l);
    }

    private void checkPropSupport() {
        if (propSupport == null)
            throw new IllegalStateException("Property change notification "
                    + "is not enabled for this FileResourceCollection");
    }


    // Internal utility methods

    protected boolean checkResourceName(String resourceName) {
        if (resourceName.indexOf('\\') != -1)
            return false; // don't allow backslashes
        if (resourceName.indexOf(':') != -1)
            return false; // don't allow colons
        if (resourceName.indexOf("..") != -1)
            return false; // don't allow parent indicators
        if ("/".indexOf(resourceName.charAt(0)) != -1)
            return false; // don't allow absolute paths

        // if we've been configured to be lenient about filenames, don't
        // force the name to match the file filter
        if (lenientFilter)
            return true;

        // defer to our file filter
        return strategy.getFilenameFilter().accept(directory, resourceName);
    }


    protected CachedFileData getFileData(String resourceName) {
        if (!checkResourceName(resourceName))
            return null;

        CachedFileData result = cachedData.get(resourceName);
        if (result == null) {
            File file = new File(directory, resourceName);
            result = new CachedFileData(file);
            cachedData.put(resourceName, result);
        }
        return result;
    }


    protected class CachedFileData {

        private File f;

        private long lastChecked;

        private long lastModified;

        private Long checksum;

        public CachedFileData(File f) {
            this.f = f;
            this.lastModified = lastChecked = -1;
            this.checksum = null;
        }

        public void setLastModified(long mod) {
            f.setLastModified(mod);
            lastModified = mod;
        }

        public long getLastModified() {
            long now = System.currentTimeMillis();
            long lastCheckAge = now - lastChecked;
            if (lastCheckAge > 5000 || lastCheckAge < 0
                    || lastChecked < cacheInvalidationTimestamp) {
                long realLastMod = f.lastModified();
                synchronized (this) {
                    if (realLastMod != lastModified) {
                        lastModified = realLastMod;
                        checksum = null;
                    }
                    lastChecked = now;
                }
            }
            return lastModified;
        }

        public Long getChecksum() {
            while (true) {
                // Check the last modification time. If it has changed
                // since we last computed the checksum, this will invalidate
                // our checksum data.
                long lastMod = getLastModified();

                // Now look to see if the lastMod time is zero. That's an
                // indication that the file doesn't exist.
                if (lastMod < 1)
                    return null;

                // The file exists. If we have a valid checksum, return it.
                Long cksum = this.checksum;
                if (cksum != null)
                    return cksum;

                try {
                    // We don't have an up-to-date checksum. Calculate one.
                    Long newSum = FileUtils.computeChecksum(f, new Adler32());
                    // Save the new checksum. But don't return it yet! Start
                    // back at the top of the loop and make certain the last
                    // modified time hasn't changed since we calculated the
                    // checksum.
                    synchronized (this) {
                        checksum = newSum;
                        lastChecked = -1;
                    }

                } catch (IOException e) {
                    // encountered an error while computing the checksum?
                    // return null to indicate the error.
                    return null;
                }
            }
        }

    }


    protected class FDCOutputStream extends RobustFileOutputStream {

        private String resourceName;

        private long modTime;

        public FDCOutputStream(String resourceName, long modTime)
                throws IOException {
            super(new File(directory, resourceName));
            this.resourceName = resourceName;
            this.modTime = modTime;
        }

        @Override
        public void close() throws IOException {
            cachedData.remove(resourceName);
            synchronized (writeLock) {
                super.close();
            }
            CachedFileData fd = getFileData(resourceName);
            synchronized (fd) {
                if (modTime > 0)
                    fd.setLastModified(modTime);
                else
                    fd.getLastModified();
                fd.checksum = getChecksum();
            }

            if (propSupport != null)
                propSupport.firePropertyChange(resourceName, null,
                    ADDED_OR_MODIFIED);
        }

    }

}
