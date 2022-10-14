// Copyright (C) 2008-2022 Tuma Solutions, LLC
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Set;

import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollectionStrategy;
import net.sourceforge.processdash.util.DirectoryBackup;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.IncrementalDirectoryBackup;
import net.sourceforge.processdash.util.lock.FileConcurrencyLock;
import net.sourceforge.processdash.util.lock.LockFailureException;
import net.sourceforge.processdash.util.lock.LockMessageHandler;
import net.sourceforge.processdash.util.lock.SentLockMessageException;

public abstract class AbstractWorkingDirectory implements WorkingDirectory {

    protected File targetDirectory;

    protected String remoteURL;

    protected FileResourceCollectionStrategy strategy;

    protected String lockFilename;

    protected File workingDirectory;

    protected FileConcurrencyLock processLock;

    private Set<File> filesWithNullBytes;

    protected AbstractWorkingDirectory(File targetDirectory, String remoteURL,
            FileResourceCollectionStrategy strategy, File workingDirectoryParent) {
        this.targetDirectory = targetDirectory;
        this.remoteURL = remoteURL;
        this.strategy = strategy;
        this.lockFilename = strategy.getLockFilename();
        createWorkingDirAndProcessLock(workingDirectoryParent);
    }

    protected void createWorkingDirAndProcessLock(File workingDirectoryParent) {
        this.workingDirectory = new File(workingDirectoryParent, getWorkingId());

        File lockFile = new File(workingDirectory, lockFilename);
        this.processLock = new FileConcurrencyLock(lockFile);
        this.processLock.setListenForLostLock(false);
    }

    protected String getWorkingId() {
        if (remoteURL != null) {
            return DirectoryPreferences.getWorkingIdForUrl(remoteURL);

        } else {
            String path = targetDirectory.getAbsolutePath();
            return FileUtils.makeSafeIdentifier(path);
        }
    }

    public File getTargetDirectory() {
        return targetDirectory;
    }

    public String getDescription() {
        if (remoteURL != null)
            return remoteURL;
        else
            return targetDirectory.getPath();
    }

    public FileResourceCollectionStrategy getStrategy() {
        return strategy;
    }

    public String getModeDescriptor() {
        return "";
    }

    public void acquireProcessLock(String msg, LockMessageHandler lockHandler)
            throws SentLockMessageException, LockFailureException {
        workingDirectory.mkdirs();
        processLock.acquireLock(msg, lockHandler, null);
    }

    protected URL doBackupImpl(File directory, String qualifier)
            throws IOException {
        // get an appropriate handler and back up the directory
        DirectoryBackup handler = strategy.getBackupHandler(directory);
        File result = handler.backup(qualifier);

        // possibly keep track of null byte files that were found
        if ("startup".equals(qualifier)
                && handler instanceof IncrementalDirectoryBackup)
            filesWithNullBytes = ((IncrementalDirectoryBackup) handler)
                    .getFilesWithNullBytes();
        else
            filesWithNullBytes = null;

        return result.toURI().toURL();
    }

    public Set<File> getFilesWithNullBytes() {
        return filesWithNullBytes;
    }

    protected String getMetadata(String name) throws IOException {
        File metadataFile = getMetadataFile(name);
        if (!metadataFile.isFile())
            return null;
        InputStream in = new FileInputStream(metadataFile);
        byte[] data = FileUtils.slurpContents(in, true);
        return new String(data, METADATA_ENCODING);
    }

    protected void setMetadata(String name, String value) throws IOException {
        File metadataFile = getMetadataFile(name);
        if (value == null) {
            metadataFile.delete();
        } else {
            File metadataDir = metadataFile.getParentFile();
            if (!metadataDir.isDirectory())
                metadataDir.mkdirs();
            Writer out = new OutputStreamWriter(new FileOutputStream(
                    metadataFile), METADATA_ENCODING);
            out.write(value);
            out.close();
        }
    }

    private File getMetadataFile(String name) {
        File metadataDir = new File(workingDirectory, "metadata");
        return new File(metadataDir, name + ".txt");
    }

    public String toString() {
        return getClass().getSimpleName() + "[" + getDescription()
                + getModeDescriptor() + "]";
    }

    public static final String NO_PROCESS_LOCK_PROPERTY =
        WorkingDirectory.class.getName() + ".noProcessLock";

    private static final String METADATA_ENCODING = "UTF-8";

}
