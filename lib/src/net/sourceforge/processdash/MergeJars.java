// Copyright (C) 2005 Tuma Solutions, LLC
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

package net.sourceforge.processdash;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.ZipFileSet;


public class MergeJars extends Task {

    private List jarFiles = new LinkedList();
    private List fileSets = new LinkedList();
    private File outputFile;
    private boolean mergeManifests = true;

    private List openedJarFiles;

    public void setDestfile(File dest) {
        this.outputFile = dest;
    }

    public void setMergeManifests(boolean merge) {
        this.mergeManifests = merge;
    }

    public void addConfiguredJar(Path jarFiles) {
        String[] jarFileNames = jarFiles.list();
        for (int i = 0; i < jarFileNames.length; i++) {
            this.jarFiles.add(new File(jarFileNames[i]));
        }
    }

    public void addConfiguredZipfileset(ZipFileSet extraFiles) {
        fileSets.add(extraFiles);
    }
    public void addConfiguredFileset(FileSet extraFiles) {
        fileSets.add(extraFiles);
    }

    public void execute() throws BuildException {
        validate();
        try {
            log("Building jar: " + outputFile);
            run();
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }


    private void validate() throws BuildException {
        if (outputFile == null)
            throw new BuildException("dest must be specified.");
        if (outputFile.exists() && !outputFile.canWrite())
            throw new BuildException("cannot write to file '" + outputFile + "'.");
        if (jarFiles.isEmpty())
            throw new BuildException("no input jar files were specified.");

        openedJarFiles = new LinkedList();
        for (Iterator i = jarFiles.iterator(); i.hasNext();) {
            File f = (File) i.next();

            if (!f.exists())
                throw new BuildException
                    ("input jarfile '" + f +"' does not exist.");
            if (!f.canRead())
                throw new BuildException
                    ("input jarfile '" + f +"' can not be read.");

            try {
                JarFile jf = new JarFile(f);
                openedJarFiles.add(jf);
            } catch (IOException ioe) {
                throw new BuildException
                    ("the input file '" + f +
                     "' does not appear to be a valid jarfile.");
            }
        }

        if (openedJarFiles.size() < 2)
            mergeManifests = false;
    }

    private void run() throws Exception {
        ZipOutputStream out = openOutputFile();
        copyMetaInf(out);
        copyContents(out);
        copyExtraFiles(out);
        closeStreams(out);
    }

    private ZipOutputStream openOutputFile() throws IOException {
        FileOutputStream fos = new FileOutputStream(outputFile);
        if (mergeManifests) {
            Manifest mf = consolidateManifest();
            return new JarOutputStream(fos, mf);
        } else {
            return new ZipOutputStream(fos);
        }
    }

    private Manifest consolidateManifest() throws IOException {
        Iterator i = openedJarFiles.iterator();
        JarFile jf = (JarFile) i.next();

        Manifest result = jf.getManifest();

        while (i.hasNext()) {
            jf = (JarFile) i.next();
            mergeManifest(result, jf.getManifest());
        }

        return result;
    }

    private void mergeManifest(Manifest dest, Manifest src) {
        mergeAttributes(dest.getMainAttributes(), src.getMainAttributes());
        for (Iterator i = src.getEntries().entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map .Entry) i.next();
            String sectionName = (String) e.getKey();
            Attributes srcAttrs = (Attributes) e.getValue();
            Attributes destAttrs = dest.getAttributes(sectionName);
            if (destAttrs == null)
                dest.getEntries().put(sectionName, srcAttrs);
            else
                mergeAttributes(destAttrs, srcAttrs);
        }
    }

    private void mergeAttributes(Attributes dest, Attributes src) {
        for (Iterator i = src.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            Object attrName = e.getKey();
            if (!dest.containsKey(attrName))
                dest.put(attrName, e.getValue());
        }
    }

    private static final Pattern META_INF_FILES_PATTERN =
        Pattern.compile("META-INF/.+", Pattern.CASE_INSENSITIVE);
    private static final Pattern MANIFEST_FILE_PATTERN =
        Pattern.compile("META-INF/MANIFEST.MF", Pattern.CASE_INSENSITIVE);


    private void copyMetaInf(ZipOutputStream out) throws IOException {
        Pattern exclusionPattern = (mergeManifests ? MANIFEST_FILE_PATTERN : null);
        copyFiles(out, META_INF_FILES_PATTERN, exclusionPattern);
    }

    private void copyContents(ZipOutputStream out) throws IOException {
        copyFiles(out, null, META_INF_FILES_PATTERN);
    }

    private void copyFiles(ZipOutputStream out, Pattern includes, Pattern excludes) throws IOException {
        Set possibleDuplicateFiles = new HashSet();
        for (Iterator i = openedJarFiles.iterator(); i.hasNext();) {
            JarFile jf = (JarFile) i.next();
            for (Enumeration e = jf.entries(); e.hasMoreElements();) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                String filename = entry.getName();
                if (excludes != null && excludes.matcher(filename).matches())
                    continue;
                if (includes != null && !includes.matcher(filename).matches())
                    continue;

                if (entry.isDirectory() || MANIFEST_FILE_PATTERN.matcher(filename).matches()) {
                    if (possibleDuplicateFiles.contains(entry.getName()))
                        continue;
                    possibleDuplicateFiles.add(entry.getName());
                }

                copyFile(entry, out, jf.getInputStream(entry));
            }
        }
    }

    private void copyExtraFiles(ZipOutputStream out) throws IOException {
        for (Iterator i = fileSets.iterator(); i.hasNext();) {
            copyFileSet((FileSet) i.next(), out);
        }
    }

    private void copyFileSet(FileSet fileset, ZipOutputStream out) throws IOException {
        String prefix = null;
        String fullPath = null;
        if (fileset instanceof ZipFileSet) {
            ZipFileSet zfs = (ZipFileSet) fileset;
            fullPath = zfs.getFullpath(getProject());
            prefix = zfs.getPrefix(getProject());
            if (prefix != null && !prefix.endsWith("/"))
                prefix = prefix + "/";
        }
        DirectoryScanner ds = fileset.getDirectoryScanner(getProject());
        String[] srcFiles = ds.getIncludedFiles();
        if (fullPath != null && srcFiles.length > 1)
            throw new BuildException("fullpath specified for fileset matching multiple files");
        File dir = fileset.getDir(getProject());

        for (int i = 0; i < srcFiles.length; i++) {
            String filename = srcFiles[i];
            File inputFile = new File(dir, filename);
            FileInputStream in = new FileInputStream(inputFile);

            filename = filename.replace(File.separatorChar, '/');
            if (fullPath != null)
                filename = fullPath;
            else if (prefix != null)
                filename = prefix + filename;

            ZipEntry zipEntry = new ZipEntry(filename);
            zipEntry.setTime(inputFile.lastModified());
            copyFile(zipEntry, out, in);
        }
    }

    byte[] buffer = new byte[1024];
    private void copyFile(ZipEntry entry, ZipOutputStream zipout, InputStream in) throws IOException {
        zipout.putNextEntry(entry);

        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1)
            zipout.write(buffer, 0, bytesRead);
    }

    private void closeStreams(ZipOutputStream out) throws IOException {
        out.close();
        for (Iterator i = openedJarFiles.iterator(); i.hasNext();) {
            JarFile jf = (JarFile) i.next();
            jf.close();
        }
    }

}
