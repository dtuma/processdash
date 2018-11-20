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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import net.sourceforge.processdash.tool.diff.ui.AbstractLOCDiffReport;

public class Main extends AbstractLOCDiffReport {

    private Repository repo;

    private GitFileSet fileSet;

    public Main(String... args) throws IOException {
        this(new ArrayList<String>(Arrays.asList(args)));
    }

    public Main(List<String> args) throws IOException {
        super.processArgs(args);
        createFileSet(args);
    }

    private void createFileSet(List<String> args) throws IOException {
        // check for git-specific command line args
        String renameScore = getArg(args, "--find-renames");

        // get the IDs of the commits to compare
        List<String> idArgs = extractIdArgs(args);

        // now that we've extracted all the git-specific args, the remaining
        // args should be considered arguments for the diff engine.
        super.setEngineOptions(args);

        // build a repository for the current directory
        File cwd = new File(System.getProperty("user.dir"));
        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder()
                .setMustExist(true).findGitDir(cwd);
        if (repositoryBuilder.getGitDir() == null)
            throw new GitDiffException.NotGitWorkingDir();
        repo = repositoryBuilder.build();

        // build a file set for the comparison
        fileSet = new GitFileSet(repo, idArgs.get(0), idArgs.get(1));

        // possibly set git-specific arguments
        if (renameScore != null)
            fileSet.setRenameScore(parseRenameScore(renameScore));
    }

    private List<String> extractIdArgs(List<String> args) {
        int idArgPos = getFirstIdArgPos(args);
        List<String> idArgs = args.subList(idArgPos, args.size());
        List<String> ids = new ArrayList<String>(idArgs);
        idArgs.clear();

        if (ids.isEmpty()) {
            // when no args are given, compare the working dir to the index
            return Arrays.asList(GitFileSet.INDEX, GitFileSet.WORKING_DIR);

        } else if (ids.size() > 2) {
            // if more than two ID args are given, abort
            throw new GitDiffException.TooManyIdArgsException(ids);

        } else if ("--cached".equals(ids.get(0))) {
            if (ids.size() == 1)
                // with a single "--cached" arg, compare the index to HEAD
                return Arrays.asList("HEAD", GitFileSet.INDEX);
            else
                // with "--cached" and another arg, compare index to that commit
                return Arrays.asList(ids.get(1), GitFileSet.INDEX);

        } else {
            if (ids.size() == 1)
                // with a single commit arg, compare working dir to that commit
                return Arrays.asList(ids.get(0), GitFileSet.WORKING_DIR);
            else
                // with two commit args, compare the commits with each other
                return ids;
        }
    }

    private int getFirstIdArgPos(List<String> args) {
        for (int i = args.size(); i-- > 0;) {
            String arg = args.get(i);
            if ("--cached".equals(arg)) {
                return i;
            } else if (arg.startsWith("-") || arg.startsWith("+")) {
                return i + 1;
            }
        }
        return 0;
    }

    private int parseRenameScore(String arg) {
        try {
            if (arg.endsWith("%"))
                return Integer.parseInt(arg.substring(0, arg.length() - 1));
            else
                return (int) (100 * Double.parseDouble("0." + arg));
        } catch (Exception e) {
            return -1;
        }
    }

    private void run() throws IOException {
        engine.addFilesToAnalyze(fileSet);
        engine.run();
        fileSet.close();
        repo.close();
    }

    public static void main(String[] args) {
        try {
            new Main(args).run();
        } catch (GitDiffException.NotGitWorkingDir e) {
            System.err.println(
                "This program must be run from within a git working directory.");
        } catch (GitDiffException.TooManyIdArgsException e) {
            System.err.println(
                "More than two commit IDs were listed on the command line. "
                        + e.getArgs());
        } catch (GitDiffException.RefNotFoundException e) {
            System.err.println(
                "Could not find any commit with ID " + e.getMissingRef());
        } catch (IOException ioe) {
            System.err.println("Unexpected problem encountered:");
            ioe.printStackTrace();
        }
    }

}
