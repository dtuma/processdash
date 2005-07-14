// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.util;

import java.io.*;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Checksum;

public class RobustFileWriter extends Writer {

    public static final String OUT_PREFIX    = "tttt";
    public static final String BACKUP_PREFIX = OUT_PREFIX + "_bak_";

    boolean origFileExists;
    File outFile, backupFile, destFile;
    Writer out;
    Checksum checksum;

    public RobustFileWriter(String destFile) throws IOException {
        this(new File(destFile), null);
    }

    public RobustFileWriter(String destFile, String encoding)
        throws IOException {
        this(new File(destFile), encoding);
    }

    public RobustFileWriter(File destFile) throws IOException {
        this(destFile, null);
    }

    public RobustFileWriter(File destFile, String encoding)
        throws IOException
    {
        this(destFile.getParent(), destFile.getName(), encoding);
    }

    public RobustFileWriter(String directory, String filename, String encoding)
        throws IOException
    {
        File parentDir = new File(directory);
        if (!parentDir.isDirectory())
            parentDir.mkdirs();
        destFile   = new File(parentDir, filename);
        outFile    = new File(parentDir, OUT_PREFIX    + filename);
        backupFile = new File(parentDir, BACKUP_PREFIX + filename);

        if (destFile.isDirectory())
            throw new IOException("Cannot write to file '"+destFile+
                    "' - directory is in the way.");

        origFileExists = destFile.isFile();

        checksum = makeChecksum();
        OutputStream outStream = new FileOutputStream(outFile);
        CheckedOutputStream checkOutStream =
            new CheckedOutputStream(outStream, checksum);

        if (encoding == null) {
            out = new OutputStreamWriter(checkOutStream);
        } else {
            out = new OutputStreamWriter(checkOutStream, encoding);
        }
    }

    protected Checksum makeChecksum() {
        return new Adler32();
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        out.write(cbuf, off, len);
    }

    public void flush() throws IOException {
        out.flush();
    }

    public void close() throws IOException {
        // close the temporary files
        out.close();

        // get the value we expect to find from the checksum
        long expectedChecksum = checksum.getValue();
        // reread the written file to see if it was written correctly
        long actualChecksum = verifyChecksum(outFile);
        // if the checksums don't match, throw an exception
        if (expectedChecksum != actualChecksum)
            throw new IOException("Error writing file '" + destFile +
                    "' - verification of written data failed.");

        // temporarily move the original file out of the way, into the backup
        if (origFileExists && destFile.renameTo(backupFile) == false)
            throw new IOException("Error writing file '" + destFile +
                    "' - could not backup original file.");
        // rename the output file to the real destination file
        if (outFile.renameTo(destFile) == false) {
            // put the original file back in place
            if (origFileExists) {
                if (backupFile.renameTo(destFile) == false) {
                    System.err.println("Warning - couldn't restore '"+destFile+"' from backup '"+backupFile+"'.");
                }
            }
            throw new IOException("Error writing file '" + destFile + "'.");
        }

        // delete the backup
        if (origFileExists)
            backupFile.delete();
    }

    private long verifyChecksum(File file) throws IOException {
        Checksum verify = makeChecksum();
        InputStream in = new BufferedInputStream(new CheckedInputStream
                (new FileInputStream(file), verify));
        int b;
        while ((b = in.read()) != -1)
            ; // do nothing
        in.close();

        return verify.getValue();
    }

}
