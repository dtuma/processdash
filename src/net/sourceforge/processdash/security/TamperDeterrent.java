// Copyright (C) 2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.security;

import static net.sourceforge.processdash.util.NullSafeObjectUtils.EQ;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.AccessControlException;
import java.util.Arrays;
import java.util.ServiceLoader;

import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RobustFileOutputStream;

/**
 * An object which can check whether an external source has tampered with the
 * contents of a sensitive file.
 * 
 * <b>Note:</b> tamper detection is performed on a best-effort basis and is not
 * bulletproof; thus, this class is designed to deter (rather than eradicate)
 * tampering.
 */
public class TamperDeterrent {

    public enum FileType {

        XML("<!--THUMBPRINT ", " -->", "UTF-8"), //
        WBS("<!--THUMBPRINT ", " -->", "UTF-8");

        private byte[] thumbStart, thumbEnd;

        private String encoding;

        private FileType(String thumbStart, String thumbEnd, String encoding) {
            try {
                this.thumbStart = thumbStart.getBytes(encoding);
                this.thumbEnd = thumbEnd.getBytes(encoding);
            } catch (UnsupportedEncodingException uee) {
                // can't happen
            }
            this.encoding = encoding;
        }

    }


    public class TamperException extends Exception {

        private File badFile;

        public TamperException(File badFile) {
            super(badFile.getPath());
            this.badFile = badFile;
        }

        public File getBadFile() {
            return badFile;
        };

    }


    private static TamperDeterrent INSTANCE = null;

    public static TamperDeterrent getInstance() {
        return INSTANCE;
    }


    protected static DashboardPermission PERMISSION = new DashboardPermission(
            "TamperDeterrent");

    public static void init() {
        // ensure entry criteria
        PERMISSION.checkPermission();
        if (INSTANCE != null)
            throw new IllegalStateException(
                    "TamperDeterrent was already initialized");

        // see if a tamper deterrent service implementation was provided
        try {
            ServiceLoader<TamperDeterrent> loader = ServiceLoader
                    .load(TamperDeterrent.class);
            INSTANCE = loader.iterator().next();
        } catch (Throwable t) {
        }

        // install a default (no-op) tamper deterrent
        if (INSTANCE == null)
            INSTANCE = new TamperDeterrent();
    }


    protected TamperDeterrent() {
        PERMISSION.checkPermission();
    }

    protected String getThumbprintType() {
        return null;
    }

    protected void calcThumbprint(FileData data) throws IOException {}


    /**
     * Read a file and write a new version containing a tamper-detection
     * thumbprint.
     * 
     * @param src
     *            the file to read. The file might or might not contain a
     *            thumbprint already. If it does, the existing thumbprint will
     *            be removed.
     * @param dest
     *            the file where the 'signed' data should be written. Can be the
     *            same as <tt>src</tt>
     * @param t
     *            the type of data the file contains
     * @throws IOException
     *             if data could not be read or written
     * @throws AccessControlException
     *             if this method is called by untrusted code. (Only core
     *             dashboard logic should be calling this method.)
     */
    public final void addThumbprint(File src, File dest, FileType t)
            throws IOException, AccessControlException {
        // only allow trusted code to add thumbprints to files
        PERMISSION.checkPermission();

        // if this is the no-op implementation, abort without changing the file
        String thisType = getThumbprintType();
        if (thisType == null) {
            if (!src.equals(dest)) {
                RobustFileOutputStream out = new RobustFileOutputStream(dest);
                FileUtils.copyFile(src, out);
                out.close();
            }
            return;
        }

        // read the file and calculate the thumbprint
        FileData fileData = readFile(src, t);
        fileData.thumbprint = null;
        calcThumbprint(fileData);
        String thumbData = thisType + SEP + fileData.thumbprint;

        // write the file as requested, with the new thumbprint at the end
        OutputStream out = new BufferedOutputStream(
                new RobustFileOutputStream(dest));
        if (fileData.thumbStartPos > 0)
            out.write(fileData.content, 0, fileData.thumbStartPos);
        if (fileData.thumbEndPos < fileData.content.length)
            out.write(fileData.content, fileData.thumbEndPos,
                fileData.content.length - fileData.thumbEndPos);
        out.write(t.thumbStart);
        out.write(thumbData.getBytes(t.encoding));
        out.write(t.thumbEnd);
        out.close();
    }


    /**
     * Examine a file and verify that its contents match the embedded
     * thumbprint.
     * 
     * @param f
     *            the file to check
     * @param t
     *            the type of data the file contains
     * @throws IOException
     *             if data could not be read
     * @throws TamperException
     *             if the file contents do match the thumbprint; if the
     *             thumbprint is missing and a tamper deterrent implementation
     *             is active; or if the thumbprint type does not match the type
     *             of the active tamper deterrent implementation
     */
    public final void verifyThumbprint(File f, FileType t)
            throws IOException, TamperException {
        // read the file and find the thumbprint it contains
        FileData fileData = readFile(f, t);

        // if the type of the embedded thumbprint does not match the active
        // tamper deterrent implementation, throw an exception.
        String thisType = getThumbprintType();
        if (!EQ(thisType, fileData.thumprintType))
            throw new TamperException(f);

        // retrieve the thumbprint from the file, then erase that information
        // from the data we provide to the service implementation
        String thumbprintCurrentlyEmbeddedInFile = fileData.thumbprint;
        fileData.thumbprint = null;

        // calculate the correct thumbprint for the file. If it does not match
        // the embedded thumbprint, throw an exception.
        calcThumbprint(fileData);
        if (!EQ(thumbprintCurrentlyEmbeddedInFile, fileData.thumbprint))
            throw new TamperException(f);
    }


    /**
     * A class to hold information about the contents of a file; including the
     * postition and content of any thumbprint it contains.
     */
    protected class FileData {

        // the type of the file
        public FileType fileType;

        // the raw contents of the file
        public byte[] content;

        // the position in the array above where the thumbprint begins and ends.
        // both values will be zero if the file did not contain a thumbprint.
        public int thumbStartPos, thumbEndPos;

        // the type and value of the thumbprint. Will be null if the file did
        // not contain a thumbprint.
        public String thumprintType, thumbprint;

    }


    /**
     * Read a file and locate the thumbprint inside, if one exists.
     */
    private FileData readFile(File f, FileType t) throws IOException {
        // read the contents of the file in question
        FileData result = new FileData();
        result.fileType = t;
        result.content = FileUtils.slurpContents(
            new BufferedInputStream(new FileInputStream(f)), true);

        // find the beginning of the thumbprint. If no thumbprint was found,
        // return the FileData object indicating that fact.
        int thumbStart = lastIndexOf(result.content, t.thumbStart);
        if (thumbStart == -1)
            return result;
        result.thumbStartPos = thumbStart;
        int thumbDataStart = thumbStart + t.thumbStart.length;

        // find the end of the thumbprint. If it wasn't found, treat everything
        // to the end of the document as thumb content.
        int thumbDataEnd = indexOf(result.content, thumbDataStart, t.thumbEnd);
        if (thumbDataEnd == -1) {
            result.thumbEndPos = thumbDataEnd = result.content.length;
        } else {
            result.thumbEndPos = thumbDataEnd + t.thumbEnd.length;
        }

        // parse the thumb data and split on the separator.
        int thumbLen = thumbDataEnd - thumbDataStart;
        String thumbData = new String(result.content, thumbDataStart, thumbLen,
                t.encoding);
        int sepPos = thumbData.indexOf(SEP);
        if (sepPos != -1) {
            result.thumprintType = thumbData.substring(0, sepPos).trim();
            result.thumbprint = thumbData.substring(sepPos + SEP.length())
                    .trim();
        }

        // redact the thumbprint data from the content array
        Arrays.fill(result.content, thumbStart, result.thumbEndPos, (byte) 0);

        return result;
    }


    private int lastIndexOf(byte[] data, byte[] pattern) {
        for (int pos = data.length - pattern.length; pos >= 0; pos--) {
            if (matches(data, pos, pattern))
                return pos;
        }
        return -1;
    }

    private int indexOf(byte[] data, int beg, byte[] pattern) {
        int last = data.length - pattern.length;
        for (int pos = beg; pos <= last; pos++) {
            if (matches(data, pos, pattern))
                return pos;
        }
        return -1;
    }

    private boolean matches(byte[] data, int pos, byte[] pattern) {
        if (pos < 0 || pos + pattern.length > data.length)
            return false;
        for (int i = pattern.length; i-- > 0;) {
            if (data[pos + i] != pattern[i])
                return false;
        }
        return true;
    }

    private static final String SEP = "//";

}
