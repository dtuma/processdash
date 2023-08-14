// Copyright (C) 2008-2023 Tuma Solutions, LLC
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
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import net.sourceforge.processdash.tool.bridge.ResourceBridgeConstants;
import net.sourceforge.processdash.tool.bridge.bundle.CloudStorageUtils;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectory;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectoryFactory;
import net.sourceforge.processdash.tool.bridge.client.TeamServerSelector;
import net.sourceforge.processdash.tool.export.mgr.ExternalLocationMapper;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.TempFileFactory;
import net.sourceforge.processdash.util.lock.LockFailureException;

public class ExportFileStream {

    private String location;

    private String filename;

    private ImportDirectory destDir;

    private Object target;

    private File tempOutFile;

    private AbortableOutputStream outStream;


    public ExportFileStream(String targetPath) {
        // break the targetPath into location and filename
        int slashPos = targetPath.lastIndexOf('/');
        this.location = targetPath.substring(0, slashPos);
        this.filename = targetPath.substring(slashPos + 1);
    }

    public Object getTarget() {
        return target;
    }

    public boolean isCloudStorage() {
        return CloudStorageUtils.isCloudStorage(destDir);
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
        validateTarget();

        tempOutFile = TempFileFactory.get().createTempFile("pdash-export-", ".tmp");
        tempOutFile.deleteOnExit();
        outStream = new AbortableOutputStream(tempOutFile);

        return outStream;
    }

    public boolean delete() {
        try {
            validateTarget();
            destDir.deleteUnlockedFile(filename);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private void validateTarget() throws IOException {
        // check for remappings. Store our best guess for the final target
        String remapped = ExternalLocationMapper.getInstance()
                .remapFilename(location);
        target = (TeamServerSelector.isUrlFormat(remapped) ? remapped
                : new File(denormalize(remapped), filename));

        // retrieve an ImportDirectory for the given location
        destDir = ImportDirectoryFactory.getInstance().get(remapped);
        if (destDir == null)
            throw new IOException("Cannot reach " + remapped);

        // validate readability of the target file/collection
        String destUrl = destDir.getRemoteLocation();
        if (destUrl != null) {
            target = destUrl;
            TeamServerSelector.testServerURL(destUrl, null,
                ResourceBridgeConstants.Permission.write);

        } else {
            File destFile = new File(denormalize(remapped), filename);
            if (destFile.exists() && !destFile.canWrite())
                throw new FileNotFoundException(destFile.getPath());
        }

        destDir.validate();
    }

    private String denormalize(String path) {
        return path.replace('/', File.separatorChar);
    }

    public void finish() throws IOException {
        FileInputStream in = null;
        try {
            in = new FileInputStream(tempOutFile);
            destDir.writeUnlockedFile(filename, in);
            in.close();

        } catch (LockFailureException lfe) {
            // shouldn't happen, since we're only writing PDASH files
            throw new IOException(lfe);

        } finally {
            FileUtils.safelyClose(in);
            outStream = null;
            if (tempOutFile != null)
                tempOutFile.delete();
        }
    }

    @Override
    public String toString() {
        return target.toString();
    }

    private static class AbortableOutputStream extends FilterOutputStream {

        private volatile boolean aborted;

        public AbortableOutputStream(File dest) throws IOException {
            super(new BufferedOutputStream(new FileOutputStream(dest)));
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


    /**
     * Attempt to delete a file that was exported by this class in the past.
     * 
     * @param targetPath
     *            a string that describes a past export target; this should be a
     *            value previously returned by
     *            {@link #getExportTargetPath(File, String)}.
     * @return true if the path was recognized and successfully deleted, or if
     *         the file named by the path no longer exists
     */
    public static boolean deleteExportTarget(String targetPath) {
        if (!StringUtils.hasValue(targetPath))
            return true;

        // create an ExportFileStream and ask it to delete the file
        return new ExportFileStream(targetPath).delete();
    }

}
