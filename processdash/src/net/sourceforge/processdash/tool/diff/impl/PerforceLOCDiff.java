// Copyright (C) 2005-2010 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.diff.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import net.sourceforge.processdash.tool.diff.HardcodedFilterLocator;
import net.sourceforge.processdash.tool.diff.LOCDiffReportGenerator;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.util.RuntimeUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.TempFileFactory;


public class PerforceLOCDiff extends LOCDiffReportGenerator {

    private static final int DEFAULT_BRANCH_PREFIX_LEN = 3;
    private static final int BRANCH_IS_COPY_PREFIX_LEN = -1;
    private static final int BRANCH_IS_ADD_PREFIX_LEN = -2;

    protected List<String> changelists = new ArrayList<String>();

    protected String[] p4cmd = { "p4" };

    protected int branchPrefixLen = DEFAULT_BRANCH_PREFIX_LEN;

    protected List<String> userBranchPoints = new ArrayList<String>();

    protected List<String> precachedFstatOutput = new ArrayList<String>();

    protected boolean debug = false;

    public PerforceLOCDiff(List languageFilters) {
        super(languageFilters);
    }

    public PerforceLOCDiff(List languageFilters, String changelist) {
        super(languageFilters);
        addChangelist(changelist);
    }

    public boolean addChangelist(String changelist) {
        if (CHANGELIST_PAT.matcher(changelist).matches()) {
            changelists.add(changelist);
            return true;
        } else {
            return false;
        }
    }
    private static final Pattern CHANGELIST_PAT = Pattern.compile("default|\\d+");

    @Override
    public void setOptions(String options) {
        super.setOptions(options);
        if (options != null) {
            String optionsLC = options.toLowerCase();
            if (optionsLC.contains("-debug"))
                debug = true;

            branchPrefixLen = getBranchPrefixLength(optionsLC);
        }
    }

    private int getBranchPrefixLength(String optionsLC) {
        if (optionsLC.contains("-branchiscopy"))
            return BRANCH_IS_COPY_PREFIX_LEN;

        if (optionsLC.contains("-branchisadd"))
            return BRANCH_IS_ADD_PREFIX_LEN;

        Matcher m = BRANCH_PREFIX_OPTION.matcher(optionsLC);
        while (m.find())
            userBranchPoints.add(m.group(1));

        m = BRANCH_PREFIX_LENGTH_OPTION.matcher(optionsLC);
        if (m.find())
            return Integer.parseInt(m.group(1));

        return DEFAULT_BRANCH_PREFIX_LEN;
    }
    private static final Pattern BRANCH_PREFIX_OPTION = Pattern
            .compile("-bp=(\\S+)");
    private static final Pattern BRANCH_PREFIX_LENGTH_OPTION = Pattern
            .compile("-bplen=(\\d+)");

    private boolean branchIsCopy() {
        return branchPrefixLen == BRANCH_IS_COPY_PREFIX_LEN;
    }
    private boolean branchIsAdd() {
        return branchPrefixLen == BRANCH_IS_ADD_PREFIX_LEN;
    }

    public String[] extractPerforceArgs(String[] args) {
        if (args != null && args.length > 0) {
            ArrayList<String> argList = new ArrayList(Arrays.asList(args));
            int pos = argList.indexOf("-p4");
            if (pos != -1) {
                List<String> p4CmdLine = new ArrayList<String>();
                p4CmdLine.addAll(argList.subList(pos+1, argList.size()));
                argList.subList(pos, argList.size()).clear();

                String firstArg = p4CmdLine.get(0);
                File firstArgFile = new File(firstArg);
                if (!(firstArg.toLowerCase().contains("p4")
                        && firstArgFile.isFile()))
                    p4CmdLine.add(0, "p4");

                this.p4cmd = p4CmdLine.toArray(new String[p4CmdLine.size()]);
                args = argList.toArray(new String[argList.size()]);
            }
        }
        return args;
    }

    private Process runPerforceCommand(String... args) {
        String[] cmd = new String[p4cmd.length + args.length];
        System.arraycopy(p4cmd, 0, cmd, 0, p4cmd.length);
        System.arraycopy(args, 0, cmd, p4cmd.length, args.length);
        try {
            if (debug)
                System.err.println("\t"
                        + StringUtils.join(Arrays.asList(cmd), " "));
            return Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            throw new PerforceNotFoundException();
        }
    }

    private BufferedReader getPerforceOutput(String... args) {
        Process proc = runPerforceCommand(args);
        return new BufferedReader(new InputStreamReader(proc.getInputStream()));
    }

    protected Collection getFilesToCompare() throws IOException {
        boolean checkBinaries = false;
        if (getOptions() != null && getOptions().contains("-checkBinaries"))
            checkBinaries = true;

        if (changelists.isEmpty())
            changelists.add("default");

        List<List> changelistFileSets = new ArrayList<List>();
        int totalFileCount = 0;
        for (String changelist : changelists) {
            List filesForOneChangelist = getFilesForOneChangelist(changelist,
                checkBinaries);
            changelistFileSets.add(filesForOneChangelist);
            totalFileCount += filesForOneChangelist.size();
        }

        maybeConfirmLargeOperation(totalFileCount);

        List result = new ArrayList();
        for (List filesForOneChangelist : changelistFileSets) {
            reconcileBranchedFiles(filesForOneChangelist);
            result.addAll(filesForOneChangelist);
        }
        return result;
    }

    private List getFilesForOneChangelist(String changelist,
            boolean checkBinaries) throws IOException {
        precacheFstatOutput(changelist);

        List<PerforceFile> result = new ArrayList();

        getOpenedFilesToCompare(result, changelist, checkBinaries);

        if (result.isEmpty())
            getSubmittedFilesToCompare(result, changelist, checkBinaries);

        return result;
    }

    private void precacheFstatOutput(String changelist) {
        try {
            BufferedReader in = getPerforceOutput("fstat", "-e", changelist,
                "//...");
            String line;
            while ((line = in.readLine()) != null)
                precachedFstatOutput.add(line);
        } catch (Exception e) {}
    }

    private void getOpenedFilesToCompare(List<PerforceFile> result,
            String changelist, boolean checkBinaries) throws IOException {
        BufferedReader in = getPerforceOutput("opened", "-c", changelist);
        String line;
        while ((line = in.readLine()) != null) {
            Matcher m = OPENED_FILE_PATTERN.matcher(line);
            if (!m.matches()) {
                System.err.println("Unrecognized output from p4 opened: '"+line+"'");
            } else {
                String filename = m.group(1);
                int revNum = Integer.parseInt(m.group(2));
                String action = m.group(3);
                if (checkBinaries || m.group(5).contains("text"))
                    result.add(new OpenedPerforceFile(filename, revNum, action));
            }
        }
    }
    private static final Pattern OPENED_FILE_PATTERN = Pattern.compile
        ("(//.*)\\#(\\d+) - (edit|add|delete|branch|integrate) "
                    + "(default change|change \\d+|\\d+ change) (.*)");

    private void getSubmittedFilesToCompare(List<PerforceFile> result,
            String changelist, boolean checkBinaries) throws IOException {
        BufferedReader in = getPerforceOutput("describe", "-s", changelist);
        String line;
        while ((line = in.readLine()) != null) {
            Matcher m = SUBMITTED_FILE_PATTERN.matcher(line);
            if (m.matches()) {
                String filename = m.group(1);
                int revNum = Integer.parseInt(m.group(2));
                String action = m.group(3);
                SubmittedPerforceFile file = new SubmittedPerforceFile(
                        filename, revNum, action);
                result.add(file);
            }
        }
        if (checkBinaries == false) {
            for (Iterator i = result.iterator(); i.hasNext();) {
                SubmittedPerforceFile file = (SubmittedPerforceFile) i.next();
                if (!file.isText())
                    i.remove();
            }
        }
    }
    private static final Pattern SUBMITTED_FILE_PATTERN = Pattern.compile
        ("\\.\\.\\. (//.*)#([0-9]+) (edit|add|delete|branch|integrate)");

    private void reconcileBranchedFiles(List<PerforceFile> files)
            throws IOException {
        // make a list of branched files. If there are none, exit this method.
        List<PerforceFile> branchedFiles = getBranchedFiles(files);
        if (branchedFiles.isEmpty())
            return;

        System.out.println("Reconciling branched files...");

        // Examine the integrated files in this changelist to infer a list
        // of the branch points that were used to create this change.
        List<String> branchPoints = guessBranchPoints(files);
        branchPoints.addAll(userBranchPoints);

        // make a list of deleted files, so we can look for extraneous entries
        // (i.e., files that were branched into a file on our list as part
        // of a rename operation)
        Map<String, PerforceFile> deletedFiles = getDeletedFiles(files);

        // Now do the work.
        for (PerforceFile f : branchedFiles)
            reconcileBranchedFile(f, files, deletedFiles, branchPoints);
    }

    private List<PerforceFile> getBranchedFiles(List<PerforceFile> files) {
        List<PerforceFile> branchedFiles = new ArrayList<PerforceFile>();
        for (PerforceFile f : files)
            if (f.type == BRANCH)
                branchedFiles.add(f);
        return branchedFiles;
    }

    private Map<String, PerforceFile> getDeletedFiles(List<PerforceFile> files) {
        Map<String, PerforceFile> result = new HashMap<String, PerforceFile>();
        for (PerforceFile f : files) {
            if (f.type == DELETED)
                result.put(f.filename, f);
        }
        return result;
    }

    private List<String> guessBranchPoints(List<PerforceFile> files)
            throws IOException {
        List<String> result = new ArrayList<String>();

        // guessing branch points takes work.  Skip this work if the user
        // has supplied a hardcoded branching policy.
        if (branchPrefixLen < 0)
            return result;

        // guess branches for each integrated file.
        for (PerforceFile f : files)
            if (f.type == INTEGRATED)
                guessBranchPoints(result, f);
        return result;
    }

    private void guessBranchPoints(List<String> result, PerforceFile f)
            throws IOException {
        // If this file already falls underneath a known branch point, then
        // we don't need to replicate that test.  Note that we're assuming
        // the common branching model (with two copies of a particular codebase
        // and an integration from one to the other) - so once we find the
        // base branch prefix for a particular codebase we don't need to keep
        // checking over and over again.  Also, if multiple "source" branches
        // were all integrated into the destination branch, our algorithm
        // doesn't really require knowledge of those source branches, so we
        // don't need to chase down multiple source branches.
        if (getPrefixForPath(f.filename, result) != null)
            return;

        String srcBranchFile = null;
        String destBranchFile = null;

        String fileKey = f.filename + "#" + f.revNum;
        BufferedReader in = getPerforceOutput("filelog", fileKey);
        String line;
        while ((line = in.readLine()) != null) {
            // if we've already found the info we need, just skip this loop.
            // (But we continue looping so we can consume and discard all of
            // the output from the p4 process.)
            if (srcBranchFile != null) continue;

            // check to see if this line describes the integration operation
            Matcher m = FILELOG_PATTERN.matcher(line);
            if (!m.matches()) continue;
            String partnerFile = m.group(3);

            // detect the branch points from the filenames, if possible.
            String branchRelativeFilename = getSharedPathSuffix(f.filename,
                partnerFile);
            srcBranchFile = stripSuffix(partnerFile, branchRelativeFilename);
            destBranchFile = stripSuffix(f.filename, branchRelativeFilename);
            if (branchRelativeFilename.length() > 0) {
                addBranchPath(srcBranchFile, result);
                addBranchPath(destBranchFile, result);
            }
        }

    }

    private String getSharedPathSuffix(String pathA, String pathB) {
        if (pathA.equals(pathB))
            return pathA;

        String[] segmentsA = pathA.substring(2).split("/", 0);
        String[] segmentsB = pathB.substring(2).split("/", 0);
        int posA = segmentsA.length;
        int posB = segmentsB.length;
        String result = "";
        while (posA-- > 0 && posB-- > 0) {
            if (segmentsA[posA].equals(segmentsB[posB]))
                result = "/" + segmentsA[posA] + result;
            else
                break;
        }
        if (result.length() > 0)
            result = result.substring(1);
        return result;
    }

    private String stripSuffix(String path, String suffix) {
        return path.substring(0, path.length() - suffix.length());
    }

    private void addBranchPath(String newPath, List<String> branchPaths) {
        for (Iterator i = branchPaths.iterator(); i.hasNext();) {
            String oneOldPath = (String) i.next();
            if (oneOldPath.equals(newPath))
                // the new path is already in the list.
                return;
            else if (oneOldPath.startsWith(newPath))
                // the new path is at a higher level than one in the list.
                // prefer the newer path and remove the old path.
                i.remove();
        }
        branchPaths.add(newPath);
    }

    private void reconcileBranchedFile(PerforceFile f,
            List<PerforceFile> allFiles, Map<String, PerforceFile> deletedFiles,
            List<String> branchPoints) throws IOException {

        String branchSrcFile = null;
        int branchSrcRev = 0;

        String fileKey = f.filename + "#" + f.revNum;
        BufferedReader in = getPerforceOutput("filelog", "-i", fileKey);
        String line;
        while ((line = in.readLine()) != null) {
            // if we've already found the file we need, just skip this loop.
            // (But we continue looping so we can consume and discard all of
            // the output from the p4 process.)
            if (branchSrcFile != null) continue;

            // check to see if this line describes a branch or integration
            // operation
            Matcher m = FILELOG_PATTERN.matcher(line);
            if (!m.matches()) continue;
            String partnerFile = m.group(3);
            int partnerRev = Integer.parseInt(m.group(5));

            if (m.group(2) != null) {
                // this is a "branch from" operation.  If the source file is
                // on the same branch as the current changelist target, it
                // means one of two things:
                //  (a) this was a plain copy of a file from one location to
                //      another within the branch, or
                //  (b) this source file was copied to a sandbox branch,
                //      modified there, then copied back to this main branch
                // either way, this source file should be used as the starting
                // point for comparison.
                if (onSameBranch(f.filename, partnerFile, branchPoints)) {
                    branchSrcFile = partnerFile;
                    branchSrcRev = partnerRev;
                }

            } else if (m.group(1) != null) {
                // This is an "edit from," "copy from," or "merge from"
                // operation.  This means one of two things:
                //  (a) this was the most recent point of integration from
                //      the mainline branch into a sandbox branch, or
                //  (b) the named file forms the source for the current
                //      changelist target.
                // Either way, use the named file as our comparison source.
                branchSrcFile = partnerFile;
                branchSrcRev = partnerRev;
            }
        }

        if (branchSrcFile == null) {
            // if we didn't find an appropriate branch source, it means that
            // the file was added on a sandbox branch and integrated back to
            // the mainline. Treat this like an added file.
            f.type = ADDED;
        } else {
            f.branchSrcFilename = branchSrcFile;
            f.branchSrcRev = branchSrcRev;

            // if this branched file traces its ancestry back to a file that
            // was deleted in this changelist, the pair represent a rename
            // operation.  Drop the deleted file
            PerforceFile deletedFile = deletedFiles.remove(branchSrcFile);
            if (deletedFile != null)
                allFiles.remove(deletedFile);
        }
    }
    private static final Pattern FILELOG_PATTERN = Pattern.compile(
        "\\Q... ...\\E ((branch)|merge|copy|edit) from (//[^#]*)(#\\d+,)?#(\\d+)");

    private boolean onSameBranch(String pathA, String pathB,
            List<String> knownBranchPoints) {
        // if the user wants all branch operations to be treated as copies,
        // return true. (This effectively treats the entire repository as a
        // single big branch where files are copied around, giving no special
        // treatment to integrations across branches.)
        if (branchIsCopy())
            return true;

        // try to examine our list of known branch points.  If either of the
        // files falls on a recognized branch, then the other must fall on the
        // same branch for this method to return true.
        String branchA = getPrefixForPath(pathA, knownBranchPoints);
        String branchB = getPrefixForPath(pathB, knownBranchPoints);
        if (branchA != null || branchB != null)
            return branchA != null && branchA.equals(branchB);

        // fall back to our branch prefix length comparison.  Two files are
        // on the same branch if they share a minimum number of common initial
        // path components.
        int pathCount = 0;
        int minLength = Math.min(pathA.length(), pathB.length());
        for (int i = 2;  i < minLength;  i++) {
            char charA = pathA.charAt(i);
            char charB = pathB.charAt(i);
            if (charA != charB)
                break;
            if (charA == '/')
                pathCount++;
        }
        return pathCount >= branchPrefixLen;
    }

    private String getPrefixForPath(String path, List<String> prefixes) {
        for (String prefix : prefixes)
            if (path.startsWith(prefix))
                return prefix;
        return null;
    }

    private void maybeConfirmLargeOperation(int fileCount) throws IOException {
        if (fileCount < 250)
            return;

        if (getOptions() != null && getOptions().contains("-confirmLarge"))
            return;

        Object[] message = new String[] {
            "This LOC counting operation will examine",
            "     " + fileCount + " files",
            "Extremely large counting operations may strain the Perforce",
            "server.  Are you certain you wish to proceed?"
        };
        int userChoice = JOptionPane.showConfirmDialog(null, message,
            "Confirm Large Operation", JOptionPane.OK_CANCEL_OPTION);
        if (userChoice != JOptionPane.OK_OPTION)
            throw new UserCancelledException();
    }

    private Map<String, File> cachedFilesFromPerforce = new HashMap<String, File>();

    protected InputStream getFileFromPerforce(String filename, int revNum)
            throws IOException {
        if (revNum == 0)
            return null;

        String fileKey = filename + "#" + revNum;
        File result = cachedFilesFromPerforce.get(fileKey);

        if (result == null) {
            result = TempFileFactory.get().createTempFile("p4diff", ".tmp");
            result.deleteOnExit();
            Process proc = runPerforceCommand("print", "-o",
                result.getPath(), "-q", fileKey);
            RuntimeUtils.consumeOutput(proc, null, null, true);
            cachedFilesFromPerforce.put(fileKey, result);
        }

        return new FileInputStream(result);
    }

    private static final int ADDED = 0;
    private static final int MODIFIED = 1;
    private static final int DELETED = 2;
    private static final int INTEGRATED = 3;
    private static final int BRANCH = 5;

    private abstract class PerforceFile implements FileToCompare {

        protected String filename;
        protected int revNum;
        protected int type;

        protected String clientFilename;
        protected Boolean isTextFile;

        protected String branchSrcFilename;
        protected int branchSrcRev;

        public PerforceFile(String filename, int revNum, String type) {
            this.filename = filename;
            this.revNum = revNum;
            if ("add".equals(type))
                this.type = ADDED;
            else if ("delete".equals(type))
                this.type = DELETED;
            else if ("edit".equals(type))
                this.type = MODIFIED;
            else if ("integrate".equals(type))
                this.type = INTEGRATED;
            else if ("branch".equals(type))
                this.type = (branchIsAdd() ? ADDED : BRANCH);
            else
                throw new IllegalArgumentException("Unrecognized Perforce change type '"+type+"'");
        }

        public String getFilename() {
            return filename;
        }

        public InputStream getContentsBefore() throws IOException {
            if (type == ADDED)
                return null;
            else if (type == BRANCH)
                return getFileFromPerforce(branchSrcFilename, branchSrcRev);
            else
                return getContentsBeforeImpl();
        }

        protected abstract InputStream getContentsBeforeImpl() throws IOException;

        public InputStream getContentsAfter() throws IOException {
            if (type == DELETED)
                return null;
            else
                return getContentsAfterImpl();
        }

        protected abstract InputStream getContentsAfterImpl() throws IOException;

        protected void getFileStats() throws IOException {
            if (getPrecachedFileStats() == false)
                runSingleFileFstat();
        }

        private boolean getPrecachedFileStats() {
            String fstatStartLine = "... depotFile " + filename;
            int pos = precachedFstatOutput.indexOf(fstatStartLine);
            if (pos == -1)
                return false;

            while (++pos < precachedFstatOutput.size()) {
                String line = precachedFstatOutput.get(pos);
                if (line.startsWith("... depotFile "))
                    break;
                else
                    processFstatLine(line);
            }
            return true;
        }

        private void runSingleFileFstat() throws IOException {
            BufferedReader in = getPerforceOutput("fstat", filename);
            String line;
            while ((line = in.readLine()) != null) {
                processFstatLine(line);
            }
        }

        private void processFstatLine(String line) {
            if (line.startsWith("... clientFile "))
                clientFilename = line.substring(15);
            else if (line.startsWith("... type ") || line.startsWith("... headType "))
                isTextFile = line.contains("text");
        }

    }

    private class OpenedPerforceFile extends PerforceFile {

        public OpenedPerforceFile(String filename, int revNum, String type) {
            super(filename, revNum, type);
        }

        protected InputStream getContentsBeforeImpl() throws IOException {
            return getFileFromPerforce(filename, revNum);
        }

        protected InputStream getContentsAfterImpl() throws IOException {
            getFileStats();
            if (clientFilename == null)
                return null;
            else
                return new FileInputStream(clientFilename);
        }
    }

    private class SubmittedPerforceFile extends PerforceFile {

        public SubmittedPerforceFile(String filename, int revNum, String type) {
            super(filename, revNum, type);
        }
        public boolean isText() {
            if (isTextFile == null) {
                try {
                    getFileStats();
                } catch (IOException e) {}
            }
            return (isTextFile != null && isTextFile);
        }

        protected InputStream getContentsBeforeImpl() throws IOException {
            return getFileFromPerforce(filename, revNum-1);
        }
        protected InputStream getContentsAfterImpl() throws IOException {
            return getFileFromPerforce(filename, revNum);
        }
    }

    private class PerforceNotFoundException extends RuntimeException {}

    private class UserCancelledException extends RuntimeException {}


    public static void main(String[] args) {
        PerforceLOCDiff diff = new PerforceLOCDiff
            (HardcodedFilterLocator.getFilters());
        args = diff.extractPerforceArgs(args);
        args = collectOptions(args);
        diff.setOptions(args[0]);
        diff.addChangeListener(new StdOutChangeListener());

        for (int i = 1;  i < args.length;  i++) {
            if (diff.addChangelist(args[i]) == false) {
                printUsage();
                return;
            }
        }

        try {
            File out = diff.generateDiffs();
            Browser.launch(out.toURI().toURL().toString());
        } catch (PerforceNotFoundException pnfe) {
            System.err.println("Could not execute the Perforce command line client.  Please");
            System.err.println("take one of the following corrective actions:");
            System.err.println("    * Ensure the 'p4' command is in your path, or ");
            System.err.println("    * Append arguments to your command line of the form ");
            System.err.println("             -p4 <path to p4 application>");
            System.err.println("Then try again.");
        } catch (UserCancelledException uce) {
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java " + PerforceLOCDiff.class.getName()
                + " [-branchIsAdd | -branchIsCopy | -bp=<path> ...]"
                + " [changelist...] [-p4 perforce command line options]");
    }
}
