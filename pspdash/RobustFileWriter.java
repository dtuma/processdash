// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

package pspdash;

import java.io.*;

public class RobustFileWriter extends Writer {

    public static final String OUT_PREFIX    = "tttt";
    public static final String BACKUP_PREFIX = OUT_PREFIX + "_";

    File outFile, backupFile, destFile;
    Writer out, backup;

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
        if (encoding == null) {
            out    = new FileWriter(outFile);
            backup = new FileWriter(backupFile);
        } else {
            out = new OutputStreamWriter
                (new FileOutputStream(outFile), encoding);
            backup = new OutputStreamWriter
                (new FileOutputStream(backupFile), encoding);
        }
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        out.write(cbuf, off, len);
        backup.write(cbuf, off, len);
    }

    public void flush() throws IOException {
        out.flush();
        backup.flush();
    }

    public void close() throws IOException {
        // close the temporary files
        out.close();
        backup.close();

        // rename to the real destination file
        destFile.delete();
        outFile.renameTo(destFile);

        // delete the backup
        backupFile.delete();
    }

}
