// Copyright (C) 2005-2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.diff.impl.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import net.sourceforge.processdash.tool.diff.engine.FileAnalysisSet;
import net.sourceforge.processdash.tool.diff.engine.FileToAnalyze;


public class FileSystemFileSet implements FileAnalysisSet {

    private List<File> bases;
    private boolean caseInsensitive;

    public FileSystemFileSet(File... bases) {
        this(new ArrayList<File>(Arrays.asList(bases)));
    }

    public FileSystemFileSet(List<File> bases) {
        this.bases = bases;
    }

    public FileSystemFileSet() {
        this(new ArrayList<File>());
    }

    public void addFile(File f) {
        bases.add(f);
    }

    public void validate() throws FileSystemDiffException {
        getDiffType();
    }

    public List<? extends FileToAnalyze> getFilesToAnalyze() throws IOException {
        DiffType type = getDiffType();
        if (type == DiffType.File) {
            return Collections.singletonList(new SimpleFileComparison());

        } else if (type == DiffType.Dir) {
            caseInsensitive = testCaseInsensitive();
            TreeSet result = new TreeSet();
            for (File dir : bases)
                listAllFiles(result, dir, dir);
            return new ArrayList<FileToAnalyze>(result);

        } else {
            throw new FileSystemDiffException.NoFilesListed();
        }
    }

    private enum DiffType { File, Dir };

    private DiffType getDiffType() throws FileSystemDiffException {
        DiffType result = null;
        for (File f : bases) {
            if (!f.exists()) {
                throw new FileSystemDiffException.FileNotFound(f);
            } else if (isFile(f)) {
                if (result == DiffType.Dir)
                    throw new FileSystemDiffException.TypeMismatch();
                else
                    result = DiffType.File;
            } else if (isDir(f)) {
                if (result == DiffType.File)
                    throw new FileSystemDiffException.TypeMismatch();
                else
                    result = DiffType.Dir;
            }
        }
        return result;
    }

    private boolean testCaseInsensitive() {
        File dir = bases.get(0);
        File test1 = new File(dir, "test.txt");
        File test2 = new File(dir, "TEST.TXT");
        return test1.equals(test2);
    }

    private void listAllFiles(TreeSet result, File dir, File baseDir) {
        String basePath = "";
        if (baseDir != null)
            basePath = baseDir.getAbsolutePath() + File.separator;
        listAllFiles(result, dir, basePath);
    }

    private void listAllFiles(TreeSet result, File dir, String basePath) {
        if (!isDir(dir) || dir.getName().startsWith("."))
            return;
        File [] files = dir.listFiles();
        for (int i = files.length;   i-- > 0; ) {
            String filename = files[i].getName();
            if (!".".equals(filename) && !"..".equals(filename)
                    && !filename.endsWith("~")) {
                if (isFile(files[i])) {
                    filename = files[i].getAbsolutePath();
                    if (filename.startsWith(basePath))
                        filename = filename.substring(basePath.length());
                    result.add(new FileInDirectory(filename));
                } else if (isDir(files[i])) {
                    listAllFiles(result, files[i], basePath);
                }
            }
        }
    }


    private class SimpleFileComparison implements FileToAnalyze {

        public SimpleFileComparison() {}

        public String getFilename() {
            return bases.get(bases.size() - 1).getPath();
        }

        public List getVersions() {
            return bases;
        }

        public InputStream getContents(Object version) throws IOException {
            return new FileInputStream((File) version);
        }
    }

    private class FileInDirectory implements FileToAnalyze,
            Comparable<FileInDirectory> {

        private String filename;
        private String cmpName;

        public FileInDirectory(String filename) {
            this.filename = filename;

            if (caseInsensitive)
                cmpName = filename.toLowerCase();
            else
                cmpName = filename;
        }

        public String getFilename() {
            return filename;
        }

        public List getVersions() {
            return bases;
        }

        public InputStream getContents(Object version) throws IOException {
            File dir = (File) version;
            File file = new File(dir, filename);
            if (isFile(file))
                return new FileInputStream(file);
            else
                return null;
        }


        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof FileInDirectory)) return false;
            FileInDirectory that = (FileInDirectory) obj;
            return this.cmpName.equals(that.cmpName);
        }

        public int hashCode() {
            return cmpName.hashCode();
        }

        public int compareTo(FileInDirectory that) {
            return this.cmpName.compareTo(that.cmpName);
        }
    }


    protected boolean isFile(File f) { return (f != null && f.isFile()); }
    protected boolean isDir(File f)  { return (f != null && f.isDirectory()); }


}
