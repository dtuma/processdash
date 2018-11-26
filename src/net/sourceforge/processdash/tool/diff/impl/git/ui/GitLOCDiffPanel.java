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

package net.sourceforge.processdash.tool.diff.impl.git.ui;

import static org.eclipse.jgit.diff.DiffEntry.Side.NEW;
import static org.eclipse.jgit.diff.DiffEntry.Side.OLD;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Closeable;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.diff.engine.DiffEngine;
import net.sourceforge.processdash.tool.diff.engine.FileAnalysisSet;
import net.sourceforge.processdash.tool.diff.impl.git.GitDiffException;
import net.sourceforge.processdash.tool.diff.impl.git.GitFileSet;
import net.sourceforge.processdash.tool.diff.ui.LOCDiffDialog;
import net.sourceforge.processdash.tool.diff.ui.LOCDiffDialog.PanelInvalidException;
import net.sourceforge.processdash.ui.lib.BoxUtils;
import net.sourceforge.processdash.ui.lib.FileSelectionField;

public class GitLOCDiffPanel implements LOCDiffDialog.Panel, Closeable {

    private Component configPanel;

    private FileSelectionField baseDirSelector;

    private Repository repo;

    private GitIDBrowser browser;

    private GitIDSelector oldID, newID;

    private DirectoryChangeHandler directoryChangeHandler;

    @Override
    public String getId() {
        return "git";
    }

    @Override
    public String getShortName() {
        return getRes("Name");
    }

    @Override
    public void close() {
        if (repo != null) {
            repo.close();
            repo = null;
        }
    }

    @Override
    public Component getConfigPanel() {
        if (configPanel == null)
            configPanel = buildConfigPanel();
        return configPanel;
    }

    private Component buildConfigPanel() {
        baseDirSelector = new FileSelectionField(LOCDiffDialog.PREFS,
                "recentGitDirectories", JFileChooser.DIRECTORIES_ONLY,
                getRes("Browse"));

        browser = new GitIDBrowser(this);
        oldID = new GitIDSelector(OLD, browser, null);
        newID = new GitIDSelector(NEW, browser, oldID);

        directoryChangeHandler = new DirectoryChangeHandler();

        return BoxUtils.vbox( //
            hbox(getRes("Base_Dir_Prompt"), GLUE), 5, //
            hbox(PAD, baseDirSelector, 10, GLUE), SPACE, GLUE, //
            hbox(getRes("New_Prompt"), GLUE), 5, //
            hbox(PAD, newID.getUIControls(), 10, GLUE), SPACE, GLUE, //
            hbox(getRes("Old_Prompt"), GLUE), 5, //
            hbox(PAD, oldID.getUIControls(), 10, GLUE), GLUE);
    }

    static Component hbox(Object... contents) {
        BoxUtils result = BoxUtils.hbox(contents);
        Dimension d = result.getPreferredSize();
        d.width = 3000;
        result.setMaximumSize(d);
        return result;
    }

    private static final Object GLUE = BoxUtils.GLUE;

    private static final int PAD = 30;

    private static final int SPACE = 10;

    Repository getRepo() {
        try {
            return getRepoImpl();
        } catch (PanelInvalidException pie) {
            pie.show(configPanel);
            return null;
        }
    }

    private Repository getRepoImpl() {
        // retrieve the working directory the user selected
        File dir = baseDirSelector.getSelectedFile();
        if (dir == null)
            throw new PanelInvalidException(getRes("Working_Dir_Missing"));

        // if we already have a repository object, see if it is for the
        // selected filesystem directory. If so, return it. If not, dispose it.
        if (repo != null) {
            File currentRepoDir = repo.getWorkTree();
            if (dir.equals(currentRepoDir)) {
                return repo;
            } else {
                close();
            }
        }

        // Look for a git repository on or above the selected dir
        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder()
                .setMustExist(true).findGitDir(dir);
        if (repositoryBuilder.getGitDir() == null)
            throw new PanelInvalidException(getRes("Not_Working_Dir"));

        // Create a new repository object
        try {
            repo = repositoryBuilder.build();
            directoryChangeHandler.armed = false;
            baseDirSelector.getTextField()
                    .setText(repo.getWorkTree().getPath());
            return repo;
        } catch (NoWorkTreeException nwte) {
            close();
            throw new PanelInvalidException(getRes("Not_Working_Dir"));
        } catch (Exception e) {
            e.printStackTrace();
            throw new PanelInvalidException(getRes("Unexpected_Error"));
        } finally {
            directoryChangeHandler.armed = true;
        }
    }


    @Override
    public FileAnalysisSet getFileAnalysisSet(DiffEngine engine)
            throws PanelInvalidException {
        // get the repository
        Repository repo = getRepoImpl();

        // get the ID of the trees/commits we are counting
        String afterId = newID.getSelectedID();
        if (afterId == null)
            throw new PanelInvalidException(getRes("New_Missing"));
        String beforeId = oldID.getSelectedID();
        if (beforeId == null)
            beforeId = GitFileSet.PARENT;

        // create and validate a git file set
        GitFileSet result = new GitFileSet(repo, beforeId, afterId);
        try {
            result.validate();
        } catch (GitDiffException.RefNotFoundException nfe) {
            throw new PanelInvalidException(
                    resources.format("Bad_Revision_FMT", nfe.getMissingRef()));
        } catch (Exception e) {
            e.printStackTrace();
            throw new PanelInvalidException(getRes("Unexpected_Error"));
        }

        // set rename score threshold from user prefs
        int rs = Settings.getInt("userPref.pspdiff.git.renameScore", -1);
        if (rs > 0 && rs <= 100)
            result.setRenameScore(rs);

        baseDirSelector.savePreferences();
        return result;
    }

    private class DirectoryChangeHandler
            implements DocumentListener, ActionListener {

        private Timer timer;

        private boolean armed;

        public DirectoryChangeHandler() {
            timer = new Timer(20, this);
            timer.setRepeats(false);
            armed = true;
            baseDirSelector.getTextField().getDocument()
                    .addDocumentListener(this);
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            directoryChanged();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            directoryChanged();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            directoryChanged();
        }

        private void directoryChanged() {
            if (armed)
                timer.restart();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (oldID.hasNonstandardID() || newID.hasNonstandardID()) {
                oldID.selectMenuItem(0);
                newID.selectMenuItem(0);
            }
        }

    }

    static String getRes(String key) {
        return resources.getString(key);
    }

    static final Resources resources = Resources
            .getDashBundle("LOCDiff.Dialog.Git");

}
