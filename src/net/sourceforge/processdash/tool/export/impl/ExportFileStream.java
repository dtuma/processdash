// Copyright (C) 2008-2020 Tuma Solutions, LLC
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
import net.sourceforge.processdash.tool.bridge.ResourceBridgeConstants;
import net.sourceforge.processdash.tool.bridge.client.ResourceBridgeClient;
import net.sourceforge.processdash.tool.bridge.client.TeamServerSelector;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HttpException;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.TempFileFactory;
import net.sourceforge.processdash.util.lock.LockFailureException;

public class ExportFileStream {

    private String lastUrl;

    private File exportFile;

    private File directFile;

    private Object target;

    private URL serverUrl;

    private File tempOutFile;

    private AbortableOutputStream outStream;

    private static final Logger logger = Logger
            .getLogger(ExportFileStream.class.getName());


    public ExportFileStream(String lastUrl, File exportFile) {
        this.lastUrl = lastUrl;
        this.exportFile = exportFile;
        this.target = exportFile;
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
        if (tempOutFile != null)
            tempOutFile.delete();
    }

    public OutputStream getOutputStream() throws IOException {
        this.target = validateTarget();

        tempOutFile = TempFileFactory.get().createTempFile("pdash-export-", ".tmp");
        tempOutFile.deleteOnExit();
        outStream = new AbortableOutputStream(tempOutFile);

        return outStream;
    }

    private Object validateTarget() throws IOException {
        boolean exportViaTeamServer = Settings.getBool(
            "teamServer.useForDataExport", true);

        // Before starting, check to see if the export file is a full path,
        // capable of direct filesystem access.
        File exportDirectory = exportFile.getParentFile();
        if (exportDirectory != null)
            directFile = exportFile;

        // First, see if the "last known good URL" is still operational, or
        // whether we can find a viable alternative
        if (exportViaTeamServer) {
            serverUrl = TeamServerSelector.resolveServerURL(lastUrl,
                MIN_SERVER_VERSION);
            if (serverUrl != null && checkWrite(serverUrl))
                return serverUrl;
        }

        // If the export file is just a simple filename (as would be the case
        // for a URL-only export), don't attempt to query the filesystem.
        if (exportDirectory == null) {
            target = lastUrl;
            throw new IOException("Cannot contact server '" + lastUrl + "'");
        }

        // Look in the "target directory" where the file would be written,
        // and see if a URL can be found for that target directory
        if (exportViaTeamServer) {
            serverUrl = TeamServerSelector.getServerURL(exportDirectory,
                MIN_SERVER_VERSION);
            if (serverUrl != null && checkWrite(serverUrl))
                return serverUrl;
        }

        // No URL-based approach was found, so we will be exporting the file
        // directly to the filesystem. If the destination directory does
        // not exist, or if the file exists and is read-only, abort.
        if (!exportDirectory.isDirectory()
                || (directFile.exists() && !directFile.canWrite()))
            throw new FileNotFoundException(directFile.getPath());

        return directFile;
    }

    private boolean checkWrite(URL u) throws HttpException {
        TeamServerSelector.testServerURL(u.toString(), null,
            ResourceBridgeConstants.Permission.write);
        return true;
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

    private boolean tryCopyToServer(long checksum) throws IOException {
        if (serverUrl == null)
            return false;

        try {
            copyToServer(checksum);
            return true;
        } catch (Exception e) {
            if (directFile == null) {
                IOException ioe = new IOException("Could not contact server "
                        + serverUrl);
                ioe.initCause(e);
                throw ioe;
            }

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
        String name = exportFile.getName();
        Long serverSum = ResourceBridgeClient.uploadSingleFile(serverUrl, name,
            in);
        if (serverSum == null || serverSum != checksum)
            throw new IOException("checksum mismatch after uploading file ("
                    + serverSum + " != " + checksum + ")");

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



    /**
     * Return a path which uniquely describes the destination of a file
     * which might be exported by this class.
     */
    public static String getExportTargetPath(File file, String url) {
        File dir = file.getParentFile();
        if (dir != null || !StringUtils.hasValue(url))
            return file.getPath().replace('\\', '/');

        String result = url;
        if (!result.endsWith("/"))
            result = result + "/";
        result = result + file.getName();
        return result;
    }

    public interface ExportTargetDeletionFilter {
        public boolean shouldDelete(URL exportTarget);
    }

    /**
     * Attempt to delete a file that was exported by this class in the past.
     * 
     * @param targetPath
     *            a string that describes a past export target; this should be a
     *            value previously returned by
     *            {@link #getExportTargetPath(File, String)}.
     * @param deletionFilter
     *            a filter than can determine whether files should be deleted.
     * @return true if the path was recognized and successfully deleted, or if
     *            the deletion filter said it didn't need to be deleted.
     */
    public static boolean deleteExportTarget(String targetPath,
            ExportTargetDeletionFilter deletionFilter) {
        if (!StringUtils.hasValue(targetPath))
            return true;

        if (TeamServerSelector.isUrlFormat(targetPath))
            return deleteUrlExportTarget(targetPath, deletionFilter);
        else
            return deleteFilesystemExportTarget(targetPath, deletionFilter);
    }

    private static boolean deleteUrlExportTarget(String url,
            ExportTargetDeletionFilter deletionFilter) {
        int slashPos = url.lastIndexOf('/');
        if (slashPos == -1)
            return false;

        try {
            URL fullURL = new URL(url);
            if (deletionFilter != null
                    && deletionFilter.shouldDelete(fullURL) == false)
                return true;
        } catch (Exception e) {}

        try {
            URL baseURL = new URL(url.substring(0, slashPos));
            String filename = url.substring(slashPos + 1);
            return ResourceBridgeClient.deleteSingleFile(baseURL, filename);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean deleteFilesystemExportTarget(String path,
            ExportTargetDeletionFilter deletionFilter) {
        path = path.replace('/', File.separatorChar);
        File file = new File(path);

        if (file.exists()) {
            try {
                URL fileURL = file.toURI().toURL();
                if (deletionFilter != null
                        && deletionFilter.shouldDelete(fileURL) == false)
                    return true;
            } catch (Exception e) {}

            return file.delete();
        }

        // There are 2 possibilities here :
        //     1. The file has already been deleted
        //     2. The file is on an unmounted network drive.
        // To determine which possibility we're facing, we check to see if the
        // parent directory exists. Since the file should reside somewhere in
        // the data directory, if we can't access one level up, we assume that
        // the file resides on a unmounted network drive. In that case, the
        // deletion is not successful. If we can access one level up, we assume
        // that it has already been deleted. In that case, the deletion is
        // successful, not because we did it, but because some external process
        // already deleted the file.

        return file.getParentFile() != null && file.getParentFile().exists();
    }

}
