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

import static net.sourceforge.processdash.tool.diff.impl.git.ui.GitLOCDiffPanel.resources;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.TableModelEvent;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.awtui.CommitGraphPane;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotWalk;
import org.eclipse.jgit.revwalk.RevCommit;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.ui.lib.BoxUtils;
import net.sourceforge.processdash.ui.lib.JOptionPaneClickHandler;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;

public class GitIDBrowser {

    private GitLOCDiffPanel panel;

    private Repository repo;

    private BranchSelector branchSelector;

    private CommitTable commits;

    private BranchSelectionHandler branchSelectionHandler;

    private JPanel browser;

    public GitIDBrowser(GitLOCDiffPanel panel) {
        this.panel = panel;
    }

    public RevCommit browseForCommit() {
        if (browser == null)
            createUI();
        else
            branchSelector.refresh();

        Repository repo = panel.getRepo();
        if (repo == null)
            return null;
        setRepo(repo);

        int userChoice = JOptionPane.showConfirmDialog(panel.getConfigPanel(),
            browser, resources.getString("Choose_Commit"),
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        browser.setPreferredSize(browser.getSize());
        if (userChoice == JOptionPane.OK_OPTION)
            return commits.getSelectedCommit();
        else
            return null;
    }

    private void createUI() {
        branchSelector = new BranchSelector();
        Component branchBox = GitLOCDiffPanel.hbox(
            resources.getString("Branch"), 5, branchSelector, BoxUtils.GLUE);
        commits = new CommitTable();
        branchSelectionHandler = new BranchSelectionHandler();

        browser = new JPanel(new BorderLayout(0, 10));
        browser.add(branchBox, BorderLayout.NORTH);
        browser.add(new JScrollPane(commits), BorderLayout.CENTER);
        browser.add(new JOptionPaneTweaker.MakeResizable(), BorderLayout.WEST);
        browser.setPreferredSize(new Dimension(500, 400));
    }

    private void setRepo(Repository repo) {
        if (this.repo != repo) {
            this.repo = repo;
            branchSelector.refresh();
        }
    }


    private class BranchSelector extends JComboBox {

        private List<Ref> branches;

        BranchSelector() {
            branches = Collections.EMPTY_LIST;
        }

        void refresh() {
            branchSelectionHandler.armed = false;

            branches = Collections.EMPTY_LIST;
            removeAllItems();

            try {
                String currentBranchName = repo.getFullBranch();
                branches = Git.wrap(repo).branchList().setListMode(ListMode.ALL)
                        .call();
                for (Ref ref : branches) {
                    String name = ref.getName();
                    addItem(Repository.shortenRefName(name));
                    if (name.equals(currentBranchName))
                        setSelectedIndex(getItemCount() - 1);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                commits.loadCommits();
                branchSelectionHandler.armed = true;
            }
        }

        Ref getSelectedBranch() {
            int pos = getSelectedIndex();
            return (pos < 0 ? null : branches.get(pos));
        }

    }

    private class CommitTable extends CommitGraphPane {

        CommitTable() {
            columnModel.getColumn(0).setPreferredWidth(350);
            columnModel.getColumn(1).setPreferredWidth(75);
            columnModel.getColumn(2).setPreferredWidth(75);
            setIntercellSpacing(new Dimension(1, 0));
            new JOptionPaneClickHandler().install(this);
        }

        private void loadCommits() {
            PlotWalk revWalk = null;
            try {
                getCommitList().clear();

                Ref selectedBranch = branchSelector.getSelectedBranch();
                if (selectedBranch != null) {
                    ObjectId commitId = selectedBranch.getObjectId();
                    revWalk = new PlotWalk(repo);
                    revWalk.markStart(revWalk.parseCommit(commitId));
                    getCommitList().source(revWalk);

                    int numRows = Settings
                            .getInt("userPref.pspdiff.git.showNumCommits", 0);
                    if (numRows < 1)
                        numRows = Integer.MAX_VALUE;
                    getCommitList().fillTo(numRows);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (revWalk != null)
                    revWalk.dispose();
                tableChanged(new TableModelEvent(getModel()));
                scrollRectToVisible(getCellRect(0, 0, true));
            }
        }

        private RevCommit getSelectedCommit() {
            int row = getSelectedRow();
            if (row < 0)
                return null;
            else
                return (RevCommit) getCommitList().get(row);
        }
    }

    private class BranchSelectionHandler implements ActionListener {

        private boolean armed;

        BranchSelectionHandler() {
            branchSelector.addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (armed)
                commits.loadCommits();
        }
    }

}
