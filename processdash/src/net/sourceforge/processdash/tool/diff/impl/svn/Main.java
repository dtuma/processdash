// Copyright (C) 2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.diff.impl.svn;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.tool.diff.ui.AbstractLOCDiffReport;

public class Main extends AbstractLOCDiffReport {

    private SvnExecutor svn;

    private SvnFileSet files;

    public Main(String... args) throws IOException {
        this(new ArrayList<String>(Arrays.asList(args)));
    }

    public Main(List<String> args) throws IOException {
        createSvnExecutor(args);
        createFileset(args);
        processArgs(args);
    }

    private void createSvnExecutor(List<String> args) {
        svn = new SvnExecutor();
        configureBaseDirectory(args);
        configureSvnOptions(args);
        configureNumThreads(args);
        svn.validate();
    }

    private void configureBaseDirectory(List<String> args) {
        String baseDir = getArg(args, "-baseDir");
        if (baseDir != null)
            svn.setBaseDirectory(new File(baseDir));
    }

    private void configureSvnOptions(List<String> args) {
        int svnPos = args.lastIndexOf("-svn");
        if (svnPos != -1) {
            // extract svn options from the main arg line
            List<String> svnArgs = args.subList(svnPos, args.size());
            List<String> svnOptions = new ArrayList<String>(svnArgs);
            svnOptions.remove(0); // delete the "-svn" flag
            svnArgs.clear();

            // check to see if the first argument is an executable
            String firstOpt = svnOptions.get(0);
            if (new File(firstOpt).isFile()) {
                svn.setSvnCommand(firstOpt);
                svnOptions.remove(0);
            }

            if (!svnOptions.isEmpty())
                svn.setSvnOptions(svnOptions);
        }
    }

    private void configureNumThreads(List<String> args) {
        Integer numThreads = getInt(args, "-numThreads");
        if (numThreads != null)
            svn.setNumThreads(numThreads);
    }

    private void createFileset(List<String> args) {
        files = new SvnFileSet(svn);
        boolean sawChanges = false;
        if (configureLogToken(args)) sawChanges = true;
        if (configureRevisions(args)) sawChanges = true;
        configureLocalMods(args, sawChanges == false);
    }

    private boolean configureLogToken(List<String> args) {
        String token = getArg(args, "-logToken");
        String regexp = getArg(args, "-logRegexp");
        if (token != null)
            files.setLogMessageToken(token);
        else if (regexp != null)
            files.setLogMessageTokenRegexp(regexp);
        else
            return false;

        String limit = getArg(args, "-logLimitRev");
        Integer limitDays = getInt(args, "-logLimitDays");
        if (limit != null)
            files.setLogMessageTokenLimit(limit);
        else if (limitDays != null)
            files.setLogMessageTokenLimit(limitDays);

        return true;
    }

    private boolean configureRevisions(List<String> args) {
        int pos = args.indexOf("-c");
        if (pos == -1)
            return false;

        args.remove(pos);

        List<String> revisions = new ArrayList<String>();
        while (pos < args.size()) {
            String oneArg = args.get(pos);
            Matcher m = REVISON_PATTERN.matcher(oneArg);
            if (!m.matches())
                break;

            String revA = m.group(1);
            String revB = m.group(3);
            if (revB == null) {
                revisions.add(revA);
            } else {
                int aNum = Integer.parseInt(revA);
                int bNum = Integer.parseInt(revB);
                for (int r = aNum;  r <= bNum; r++)
                    revisions.add(Integer.toString(r));
            }
            args.remove(pos);
        }
        files.addRevisionsToTrack(revisions);

        return !revisions.isEmpty();
    }

    private static final Pattern REVISON_PATTERN = Pattern
            .compile("(\\d+)(-(\\d+))?");

    private void configureLocalMods(List<String> args,
            boolean forceEnableLocalMods) {
        if (getFlag(args, "-local") || forceEnableLocalMods)
            files.setIncludeLocalMods(true);

        String changelist = getArg(args, "--changelist");
        if (changelist != null)
            files.setIncludeLocalMods(true, changelist);
    }

    private void run() throws IOException {
        engine.addFilesToAnalyze(files);
        engine.run();
    }

    public static void main(String[] args) {
        try {
            new Main(args).run();
        } catch (SvnDiffException.AppNotFound e) {
            System.err.println("Could not locate the 'svn' executable");
        } catch (SvnDiffException.NotWorkingCopy e) {
            System.err.println("Not a svn working copy");
        } catch (IOException ioe) {
            System.err.println("Unexpected problem encountered:");
            ioe.printStackTrace();
        }
    }

}
