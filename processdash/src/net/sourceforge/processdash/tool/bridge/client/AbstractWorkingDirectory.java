// Copyright (C) 2008-2012 Tuma Solutions, LLC
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

import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollectionStrategy;
import net.sourceforge.processdash.util.FileUtils;
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

    protected AbstractWorkingDirectory(File targetDirectory, String remoteURL,
            FileResourceCollectionStrategy strategy, File workingDirectoryParent) {
        this.targetDirectory = targetDirectory;
        this.remoteURL = remoteURL;
        this.strategy = strategy;
        this.lockFilename = strategy.getLockFilename();

        this.workingDirectory = new File(workingDirectoryParent, getWorkingId());

        File lockFile = new File(workingDirectory, lockFilename);
        this.processLock = new FileConcurrencyLock(lockFile);
        this.processLock.setListenForLostLock(false);
    }

    protected String getWorkingId() {
        if (remoteURL != null) {
            String url = remoteURL;
            if (url.startsWith("https"))
                url = "http" + url.substring(5);
            return FileUtils.makeSafeIdentifier(url);

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
        File result = strategy.getBackupHandler(directory).backup(qualifier);
        return result.toURI().toURL();
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
