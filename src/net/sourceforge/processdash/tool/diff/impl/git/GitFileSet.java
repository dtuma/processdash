// Copyright (C) 2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.diff.impl.git;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffEntry.Side;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.Sequence;
import org.eclipse.jgit.diff.SequenceComparator;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.FileHeader.PatchType;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.util.io.NullOutputStream;

import net.sourceforge.processdash.tool.diff.engine.FileAnalysisSet;
import net.sourceforge.processdash.tool.diff.engine.FileToAnalyze;

public class GitFileSet implements FileAnalysisSet, Closeable {

    public static final String WORKING_DIR = "<working>";

    public static final String INDEX = "<index>";

    public static final String PARENT = "<parent>";


    private Repository repo;

    private String beforeId, afterId;

    private AbstractTreeIterator oldTree, newTree;

    private int renameScore;

    private ObjectReader reader;

    private TextCollectingFormatter diff;


    public GitFileSet(Repository repo, String beforeId, String afterId) {
        this.repo = repo;
        if (PARENT.equals(beforeId))
            this.beforeId = getParentOf(afterId);
        else
            this.beforeId = beforeId;
        this.afterId = afterId;
        this.renameScore = -1;
    }

    private String getParentOf(String id) {
        if (WORKING_DIR.equals(id))
            return INDEX;
        else if (INDEX.equals(id))
            return "HEAD";
        else
            return id + "^";
    }

    public int getRenameScore() {
        return renameScore;
    }

    public void setRenameScore(int renameScore) {
        this.renameScore = renameScore;
    }


    @Override
    public List<? extends FileToAnalyze> getFilesToAnalyze()
            throws GitDiffException, IOException {
        // get the trees to use for comparison
        validate();

        // request a diff operation on the given trees
        diff = new TextCollectingFormatter();
        List<DiffEntry> changedFiles = diff.scan(oldTree, newTree);
        oldTree = newTree = null;

        // create a list of changed resources
        List<GitFile> files = new ArrayList<GitFile>();
        for (DiffEntry e : changedFiles)
            files.add(new GitFile(e));
        return files;
    }

    public void validate() throws GitDiffException, IOException {
        // get the trees to use for comparison
        if (newTree == null)
            newTree = getTree(afterId);
        if (oldTree == null)
            oldTree = getTree(beforeId);
    }

    private AbstractTreeIterator getTree(String id)
            throws GitDiffException, IOException {
        // return a "working directory" tree if requested
        if (WORKING_DIR.equals(id))
            return new FileTreeIterator(repo);

        // return a "git index" tree if requested
        if (INDEX.equals(id))
            return new DirCacheIterator(repo.readDirCache());

        // resolve the given ref in the repository
        ObjectId ref;
        try {
            ref = repo.resolve(id + "^{tree}");
        } catch (Exception e) {
            throw new GitDiffException.RefNotFoundException(id, e);
        }
        if (ref == null)
            throw new GitDiffException.RefNotFoundException(id);

        // return a tree for the commit referenced by the given ID
        CanonicalTreeParser p = new CanonicalTreeParser();
        if (reader == null)
            reader = repo.newObjectReader();
        p.reset(reader, ref);
        return p;
    }

    public void close() {
        oldTree = newTree = null;
        if (diff != null)
            diff.close();
        if (reader != null)
            reader.close();
    }


    private class TextCollectingFormatter extends DiffFormatter {

        private GitFile loadedFile;

        public TextCollectingFormatter() {
            super(NullOutputStream.INSTANCE);
            setRepository(repo);
            setDetectRenames(true);
            if (renameScore > 0)
                getRenameDetector().setRenameScore(renameScore);
            setDiffAlgorithm(new NullDiffAlgorithm());
        }

        private void loadGitFile(GitFile gitFile) throws IOException {
            if (loadedFile != null)
                loadedFile.dispose();

            loadedFile = gitFile;
            super.format(gitFile.entry);
        }

        @Override
        public void format(FileHeader head, RawText oldText, RawText newText)
                throws IOException {
            loadedFile.setData(head, oldText, newText);
        }

        @Override
        public void close() {
            if (loadedFile != null)
                loadedFile.dispose();
            super.close();
        }

    }


    private class NullDiffAlgorithm extends DiffAlgorithm {

        @Override
        public <S extends Sequence> EditList diff(
                SequenceComparator<? super S> cmp, S a, S b) {
            return new EditList();
        }

        @Override
        public <S extends Sequence> EditList diffNonCommon(
                SequenceComparator<? super S> cmp, S a, S b) {
            return null;
        }

    }


    private class GitFile implements FileToAnalyze {

        private DiffEntry entry;

        private FileHeader fileHeader;

        private RawText oldText, newText;

        public GitFile(DiffEntry entry) {
            this.entry = entry;
        }

        private void setData(FileHeader head, RawText oldText,
                RawText newText) {
            this.fileHeader = head;
            this.oldText = isChangeType(ChangeType.ADD) ? NO_FILE : oldText;
            this.newText = isChangeType(ChangeType.DELETE) ? NO_FILE : newText;
        }

        private void dispose() {
            fileHeader = null;
            oldText = null;
            newText = null;
        }

        @Override
        public String getFilename() {
            if (isChangeType(ChangeType.DELETE))
                return entry.getOldPath();
            else
                return entry.getNewPath();
        }

        @Override
        public List getVersions() {
            return VERSIONS;
        }

        @Override
        public InputStream getContents(Object version) throws IOException {
            if (fileHeader == null)
                diff.loadGitFile(this);

            Side side = (Side) version;
            RawText r = (Side.OLD.equals(side) ? oldText : newText);
            if (r == NO_FILE)
                return null;

            byte[] data;
            if (PatchType.BINARY.equals(fileHeader.getPatchType())) {
                data = new byte[] { 0, 0, 1, 2, (byte) side.ordinal() };
            } else if (r != null) {
                data = r.getRawContent();
            } else {
                data = new byte[0];
            }

            return new ByteArrayInputStream(data);
        }

        private boolean isChangeType(ChangeType changeType) {
            return changeType.equals(entry.getChangeType());
        }

    }

    private static final RawText NO_FILE = new RawText(new byte[0]);

    private static final List VERSIONS = Arrays.asList(Side.values());

}
