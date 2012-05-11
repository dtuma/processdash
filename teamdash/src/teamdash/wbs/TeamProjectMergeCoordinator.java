// Copyright (C) 2012 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.wbs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;

import net.sourceforge.processdash.tool.bridge.client.BridgedWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectory;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollection;
import net.sourceforge.processdash.tool.bridge.impl.TeamDataDirStrategy;
import net.sourceforge.processdash.tool.bridge.report.ListingHashcodeCalculator;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.TempFileFactory;

public class TeamProjectMergeCoordinator {

    private TeamProject teamProject;

    private WorkingDirectory workingDir;

    private FileResourceCollection mainDir;

    private FileResourceCollection baseDir;

    private long baseHashcode;

    public TeamProjectMergeCoordinator(TeamProject teamProject,
            WorkingDirectory workingDir) throws IOException {
        this.teamProject = teamProject;
        this.workingDir = workingDir;

        // create a FileResourceCollection for the main/working directory
        this.mainDir = makeCollection(workingDir.getDirectory());

        // create a new directory and FileResourceCollection to hold the
        // "base" branch of the merge
        TempFileFactory tempFileFactory = new TempFileFactory("wbs-tmp-");
        tempFileFactory.useTempSubdirectory("pdash-tmp");
        this.baseDir = makeCollection(tempFileFactory.createTempDirectory(
            "dir", ".tmp", true, true));

        // populate the base branch with the starting contents of the main dir
        copyMainToBase();

        if (workingDir instanceof BridgedWorkingDirectory)
            ((BridgedWorkingDirectory) workingDir).setAllowUpdateWhenLocked(true);
    }

    public synchronized TeamProjectMerger doMerge() throws IOException {
        // ensure we have the most recent contents of the working directory
        workingDir.update();

        // if the directory holding the main branch is unavailable (for example,
        // if it is on a network drive that has become unreachable), abort.
        if (!mainDir.getDirectory().isDirectory())
            throw new FileNotFoundException(mainDir.getDirectory().getPath());

        // see if the hashcodes have changed. If not, no merge is needed.
        long mainHashcode = getHash(mainDir);
        if (mainHashcode == baseHashcode) {
            return null;
        } else {
            return performMerge();
        }
    }

    public synchronized void acceptChangesInMain() throws IOException {
        copyMainToBase();
    }

    private TeamProjectMerger performMerge() throws IOException {
        TeamProject base = new QuickTeamProject(baseDir.getDirectory(), "base");
        TeamProject main = new QuickTeamProject(mainDir.getDirectory(), "main");
        TeamProject incoming = this.teamProject;
        TeamProjectMerger merger = new TeamProjectMerger(base, main, incoming);
        merger.run();
        copyMainToBase();
        return merger;
    }

    private void copyMainToBase() throws IOException {
        for (String name : FILENAMES)
            copyMainToBase(name);
        baseHashcode = getHash(baseDir);
    }

    private void copyMainToBase(String name) throws IOException {
        File mainFile = new File(mainDir.getDirectory(), name);
        File baseFile = new File(baseDir.getDirectory(), name);
        if (mainFile.exists()) {
            FileUtils.copyFile(mainFile, baseFile);
        } else if (baseFile.exists()) {
            baseFile.delete();
        }
    }

    private FileResourceCollection makeCollection(File dir) {
        FileResourceCollection result = new FileResourceCollection(dir, false);
        result.setStrategy(TeamDataDirStrategy.INSTANCE);
        return result;
    }

    private long getHash(FileResourceCollection collection) {
        collection.recheckAllFileTimestamps();
        return ListingHashcodeCalculator.getListingHashcode(collection,
            FILENAMES);
    }

    /**
     * In a master project/subproject environment, the TeamProject object
     * automatically creates ImportDirectory objects for all of the
     * interrelated projects.  That is unnecessary for our purposes, so we
     * create this subclass which skips the ImportDirectory creation step.
     */
    private static class QuickTeamProject extends TeamProject {

        public QuickTeamProject(File directory, String projectName) {
            super(directory, projectName);
        }

        @Override
        protected ImportDirectory getProjectDataDirectory(Element e,
                boolean checkExists) {
            return null;
        }

    }

    private static final List<String> FILENAMES = Arrays.asList(
        TeamProject.SETTINGS_FILENAME, //
        TeamProject.USER_SETTINGS_FILENAME, //
        TeamProject.TEAM_LIST_FILENAME, //
        TeamProject.FLOW_FILENAME, //
        TeamProject.MILESTONES_FILENAME, //
        TeamProject.WBS_FILENAME);

}
