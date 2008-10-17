// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.logging.Logger;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.tool.bridge.client.ResourceBridgeClient;
import net.sourceforge.processdash.tool.bridge.client.TeamServerSelector;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.lock.LockFailureException;

public class ExportFileStream {

    private String lastUrl;

    private File directFile;

    private Object target;

    private URL serverUrl;

    private File tempOutFile;

    private AbortableOutputStream outStream;

    private static final Logger logger = Logger
            .getLogger(ExportFileStream.class.getName());

    private static final boolean exportViaTeamServer = Settings.getBool(
        "teamServer.useForDataExport", true);


    public ExportFileStream(String lastUrl, File directFile) {
        this.lastUrl = lastUrl;
        this.directFile = directFile;
        this.target = directFile;
    }

    public Object getTarget() {
        return target;
    }

    public void abort() {
        AbortableOutputStream os = outStream;
        if (os != null) {
            try {
                os.abort();
            } catch (Exception e) {
            }
        }
    }

    public OutputStream getOutputStream() throws IOException {
        this.target = validateTarget();

        tempOutFile = File.createTempFile("pdash-export-", ".tmp");
        tempOutFile.deleteOnExit();
        outStream = new AbortableOutputStream(tempOutFile);

        return outStream;
    }

    private Object validateTarget() throws IOException {
        // First, see if the "last known good URL" is still operational
        if (exportViaTeamServer) {
            serverUrl = TeamServerSelector.testServerURL(lastUrl,
                MIN_SERVER_VERSION);
            if (serverUrl != null)
                return serverUrl;
        }

        // Retrieve the "target directory" where the file would be written,
        // and see if a URL can be found for that target directory
        File exportDirectory = directFile.getParentFile();
        if (exportViaTeamServer) {
            serverUrl = TeamServerSelector.getServerURL(exportDirectory,
                MIN_SERVER_VERSION);
            if (serverUrl != null)
                return serverUrl;
        }

        // No URL-based approach was found, so we will be exporting the file
        // directly to the filesystem. If the destination directory does
        // not exist, or if the file exists and is read-only, abort.
        if ((exportDirectory == null || !exportDirectory.isDirectory())
                || (directFile.exists() && !directFile.canWrite()))
            throw new FileNotFoundException(directFile.getPath());

        return directFile;
    }

    public void finish() throws IOException {
        try {
            long checksum = outStream.getChecksum().getValue();
            if (tryCopyToServer(checksum) == false)
                copyToDestFile(checksum);

        } finally {
            outStream = null;
            if (tempOutFile != null)
                tempOutFile.delete();
        }
    }

    private boolean tryCopyToServer(long checksum) {
        if (serverUrl == null)
            return false;

        try {
            copyToServer(checksum);
            return true;
        } catch (Exception e) {
            String exceptionType = e.getClass().getName();
            if (e.getMessage() != null)
                exceptionType += " (" + e.getMessage() + ")";
            logger.warning(exceptionType + " while exporting file to '"
                    + serverUrl + "' - trying direct file route");
            return false;
        }
    }

    private void copyToServer(long checksum) throws IOException,
            LockFailureException {
        FileInputStream in = new FileInputStream(tempOutFile);
        String name = directFile.getName();
        Long serverSum = ResourceBridgeClient.uploadSingleFile(serverUrl, name,
            in);
        if (serverSum == null || serverSum != checksum)
            throw new IOException("checksum mismatch after uploading file");

        target = serverUrl;
    }

    private void copyToDestFile(long checksum) throws IOException {
        RobustFileOutputStream out = new RobustFileOutputStream(directFile,
                false);
        FileUtils.copyFile(tempOutFile, out);
        long copySum = out.getChecksum();
        if (copySum == checksum) {
            out.close();
            target = directFile;
        } else {
            out.abort();
            throw new IOException("Error writing to " + directFile
                    + " - checksums do not match");
        }
    }

    @Override
    public String toString() {
        return target.toString();
    }

    private static class AbortableOutputStream extends CheckedOutputStream {

        private volatile boolean aborted;

        public AbortableOutputStream(File dest) throws IOException {
            this(new BufferedOutputStream(new FileOutputStream(dest)));
        }

        public AbortableOutputStream(OutputStream out) {
            super(out, new Adler32());
            this.aborted = false;
        }

        public void abort() {
            this.aborted = true;
            try {
                close();
            } catch (Exception e) {
            }
        }

        private void checkStatus() throws IOException {
            if (aborted)
                throw new IOException("Output aborted");
        }

        public void write(int b) throws IOException {
            checkStatus();
            super.write(b);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            checkStatus();
            super.write(b, off, len);
        }

    }

    private static final String MIN_SERVER_VERSION = "1.2";
}
