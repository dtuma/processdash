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

package net.sourceforge.processdash;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;


public class CVSZip extends Task {

    private File startDir;
    private File destFile;

    public void setDir(File dir) {
        startDir = dir.getAbsoluteFile();
    }

    public void setDest(File dest) {
        destFile = dest;
    }


    private void validate() throws BuildException {
        if (startDir == null)
            throw new BuildException("dir attribute must be specified.");
        if (destFile == null)
            throw new BuildException("dest attribute must be specified.");
    }


    public void execute() throws BuildException {
        validate();

        try {
            ZipOutputStream out =
                new ZipOutputStream(new FileOutputStream(destFile));
            writeFiles(out);
            out.close();
        } catch (IOException ioe) {
            throw new BuildException("could not write to \""+destFile+"\"");
        }
    }


    private void writeFiles(ZipOutputStream out) throws BuildException {
        try {
            String dirName = startDir.getName();
            writeFiles(out, startDir, dirName);
        } catch (IOException e) {
            throw new BuildException("IO error encountered: "+e);
        }
    }

    private void writeFiles(ZipOutputStream out, File dir, String dirPath)
        throws IOException
    {
        List entries = getCVSEntries(dir);
        Iterator i = entries.iterator();
        while (i.hasNext()) {
            String filename = (String) i.next();
            String path = dirPath + '/' + filename;
            File file = new File(dir, filename);
            if (file.isDirectory())
                writeFiles(out, file, path);
            else if (file.isFile() && file.canRead())
                writeFile(out, file, path);
            else
                throw new BuildException("Cannot read the file \""+file+"\".");
        }
    }

    private List getCVSEntries(File dir) throws IOException {
        File cvsDir = new File(dir, "CVS");
        File entriesFile = new File(cvsDir, "Entries");
        if (!entriesFile.exists())
            return Collections.EMPTY_LIST;

        List result = new LinkedList();
        BufferedReader entries =
            new BufferedReader(new FileReader(entriesFile));
        String line;
        while ((line = entries.readLine()) != null) {
            int beg = line.indexOf('/');
            if (beg == -1) continue;
            int end = line.indexOf('/', beg+1);
            if (end == -1) continue;
            String filename = line.substring(beg+1, end);
            result.add(filename);
        }

        Collections.sort(result);
        return result;
    }

    private byte[] BUF = new byte[1024];

    private void writeFile(ZipOutputStream out, File file, String path) throws IOException {
        ZipEntry entry = new ZipEntry(path);
        entry.setTime(file.lastModified());
        entry.setSize(file.length());
        out.putNextEntry(entry);

        InputStream in = new FileInputStream(file);
        int bytesRead;
        while ((bytesRead = in.read(BUF)) != -1)
            out.write(BUF, 0, bytesRead);

        in.close();
        out.closeEntry();
    }

}
