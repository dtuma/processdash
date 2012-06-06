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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.tool.diff.engine.FileAnalysisSet;
import net.sourceforge.processdash.tool.diff.engine.FileToAnalyze;
import net.sourceforge.processdash.tool.diff.engine.FilenameComparator;
import net.sourceforge.processdash.tool.diff.engine.LocDiffUtils;
import net.sourceforge.processdash.util.DateUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.XMLUtils;

public class SvnFileSet implements FileAnalysisSet {

    protected SvnExecutor svn;

    protected FilenameComparator filenameComparator;

    protected List<String> revisionsToTrack;

    protected Pattern token;

    protected String tokenLimitRevision;

    protected boolean includeLocalMods;

    protected String changelistName;

    protected List<SvnFile> files;

    protected Map<String, SvnFile> filesByPath;

    protected static final Logger logger = Logger.getLogger(SvnFileSet.class
            .getName());


    public SvnFileSet(SvnExecutor svn) {
        this.svn = svn;
        this.filenameComparator = FilenameComparator.DEFAULT;
        this.revisionsToTrack = new ArrayList<String>();
    }

    /**
     * Request that changes be displayed for a particular set of revisions.
     * 
     * @param revisionsToTrack
     *            revisions that were significant (i.e., performed by a
     *            particular user as part of a related change activity), and
     *            whose changes should be included in the redlines and counts.
     */
    public void addRevisionsToTrack(List<String> revisionsToTrack) {
        this.revisionsToTrack.addAll(revisionsToTrack);
    }

    /**
     * Request that changes be displayed for revisions whose log message
     * contained a particular token.
     * 
     * @param token the token to search for in the historical log messages
     */
    public void setLogMessageToken(String token) {
        if (XMLUtils.hasValue(token))
            this.token = Pattern.compile(token, Pattern.LITERAL
                    + Pattern.CASE_INSENSITIVE);
        else
            this.token = null;
    }

    /**
     * Request that changes be displayed for revisions whose log message
     * contained a particular regular expression.
     * 
     * @param token the expression to search for in the historical log messages
     */
    public void setLogMessageTokenRegexp(String regexp) {
        if (XMLUtils.hasValue(regexp))
            this.token = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);
        else
            this.token = null;
    }

    /**
     * Request that changes be displayed for revisions whose log message
     * contained a particular regular expression.
     * 
     * @param token the expression to search for in the historical log messages
     */
    public void setLogMessageTokenRegexp(Pattern regexp) {
        this.token = regexp;
    }

    /**
     * Configure a limit on how far back to search in the logs for the token.
     * 
     * @param cutoff the maximum number of days in the past to search
     */
    public void setLogMessageTokenLimit(int maxDays) {
        long now = System.currentTimeMillis();
        Date cutoff = new Date(now - maxDays * DateUtils.DAYS);
        setLogMessageTokenLimit(cutoff);
    }

    /**
     * Configure a limit on how far back to search in the logs for the token.
     * 
     * @param cutoff the maximum date in the past to search through logs
     */
    public void setLogMessageTokenLimit(Date cutoff) {
        String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(cutoff);
        setLogMessageTokenLimit("{" + dateStr + "}");
    }

    /**
     * Configure a limit on how far back to search in the logs for the token.
     * 
     * @param limitRevision the oldest revision to consider in the search
     */
    public void setLogMessageTokenLimit(String limitRevision) {
        this.tokenLimitRevision = limitRevision;
    }

    /**
     * Request that local modifications be included in the redlines and counts.
     */
    public void setIncludeLocalMods(boolean includeLocalMods) {
        setIncludeLocalMods(includeLocalMods, null);
    }

    /**
     * Request that local modifications from a particular named changset be
     * included in the redlines and counts.
     */
    public void setIncludeLocalMods(boolean includeLocalMods,
            String changelistName) {
        this.includeLocalMods = includeLocalMods;
        this.changelistName = trimToNull(changelistName);
    }



    /**
     * Calculate the list of files and revisions that should be included in
     * the redlines.
     */
    public List<? extends FileToAnalyze> getFilesToAnalyze() throws IOException {
        files = new ArrayList<SvnFile>();
        filesByPath = new HashMap<String, SvnFile>();

        if (token != null)
            searchLogsForToken();

        if (!revisionsToTrack.isEmpty())
            processLogEntries();

        if (includeLocalMods)
            processLocalMods();

        if (filenameComparator != null)
            LocDiffUtils.sortFiles(files, filenameComparator);

        enqueueFileRetrievalTasks();

        return files;
    }

    private void searchLogsForToken() throws IOException {
        // if no token limit was specified, install a default of 180 days.
        if (!XMLUtils.hasValue(tokenLimitRevision))
            setLogMessageTokenLimit(180);

        logger.fine("Searching through logs for token '" + token + "'");
        List<Element> logEntries = XMLUtils.xPathElems("/log/logentry", //
            svn.execXml("log", "--xml", "-r", tokenLimitRevision + ":HEAD"));
        for (Element log : logEntries) {
            String logMessage = XMLUtils.xPathStr("msg", log);
            if (token.matcher(logMessage).find()) {
                String revision = log.getAttribute("revision");
                revisionsToTrack.add(revision);
                logger.fine("Token found in revision " + revision);
            }
        }
    }

    protected void processLogEntries() throws IOException {
        logger.fine("Processing log entries for revisions " + revisionsToTrack);
        String logRevisionRange = getLogRevisionRange();
        if (logRevisionRange != null) {
            try {
                List<Element> logEntries = XMLUtils.xPathElems("/log/logentry", //
                    svn.execXml("log", "-v", "--xml", "-r", logRevisionRange));
                for (Element oneLog : logEntries)
                    processLogEntry(oneLog);
            } catch (IOException ioe) {
                // If the given revision does not exist in this branch of the
                // repository, svn will generate an error which is invalid XML.
                // Detect this and return normally, without recording any diffs.
                if (ioe.getCause() instanceof SAXException)
                    return;
                else
                    throw ioe;
            }
        }

        discardApparentDirectories();
    }

    protected String getLogRevisionRange() {
        int minRev = Integer.MAX_VALUE;
        int maxRev = 0;
        for (String revStr : revisionsToTrack) {
            try {
                int oneRev = Integer.parseInt(revStr);
                minRev = Math.min(oneRev, minRev);
                maxRev = Math.max(oneRev, maxRev);
            } catch (NumberFormatException nfe) {
            }
        }
        if (maxRev == 0)
            // no revisions found?  return null.
            return null;
        else if (includeLocalMods)
            // if we are including local modifications, we need to scan all
            // of the revisions up to and including "HEAD".  (We assume that
            // the user is up-to-date before mixing repository revisions and
            // local modifications in a single changeset.)
            return minRev + ":HEAD";
        else
            // if local mods aren't being included, we just need to search
            // the range of revisions in question.
            return minRev + ":" + maxRev;
    }

    protected void processLogEntry(Element log) throws IOException {
        String revNum = log.getAttribute("revision");
        boolean trackChanges = revisionsToTrack.contains(revNum);

        Map<String, SvnFile> filesDeletedByThisRev = handleDeletedLogEntries(
            log, revNum, trackChanges);

        handleAddedLogEntries(log, revNum, trackChanges, filesDeletedByThisRev);

        handleModifiedLogEntries(log, revNum, trackChanges);
    }

    protected Map<String, SvnFile> handleDeletedLogEntries(Element log,
            String revNum, boolean trackChanges) throws IOException {

        Map<String, SvnFile> deletedFiles = new HashMap<String, SvnFile>();
        List<Element> deletions = XMLUtils.xPathElems(
            "paths/path[@action='D']", log);
        for (Element deletion : deletions) {
            String delPath = XMLUtils.getTextContents(deletion);
            String delKind = deletion.getAttribute("kind");

            // on svn 1.6 repositories, the delKind attribute will be either
            // "file" or "dir".  On earlier repositories, this field will be
            // empty "".  In the optimistic case, if the repository is using
            // the recent format, make a record of the fact that this is
            // definitely a directory entry.
            boolean isDefinitelyDirEntry = "dir".equals(delKind);

            if (!"file".equals(delKind)) {
                // this entry either has a kind of "dir" or "".  If it is "dir",
                // we make a list of our working files that were deleted by this
                // operation.  If it is "", we need to guess whether this entry
                // is a directory or not.  The best way to do that is to make
                // the aforementioned list of deleted files; if anything
                // matches, we assume this is a directory entry.
                Map<String, SvnFile> filesToDelete = findFilesInDirectory(
                    filesByPath, delPath);
                for (Map.Entry<String, SvnFile> e : filesToDelete.entrySet()) {
                    String oneDelPath = e.getKey();
                    SvnFile oneDelFile = e.getValue();
                    oneDelFile.addVersion(SvnEmptyFile.DELETED, trackChanges);
                    deletedFiles.put(oneDelPath, oneDelFile);

                    isDefinitelyDirEntry = true;
                }
            }

            if (isDefinitelyDirEntry == false) {
                // this entry might be a file.  Handle it accordingly.
                String delUrl = getUrlForPath(delPath);
                SvnFile delFile = filesByPath.get(delPath);

                if (delFile == null && trackChanges) {
                    delFile = new SvnFile(getFilenameFromPath(delPath));
                    files.add(delFile);
                    filesByPath.put(delPath, delFile);
                    delFile.addVersion(new SvnRemoteFile(delUrl,
                            previousRev(revNum)), false);
                }

                if (delFile != null) {
                    delFile.addVersion(SvnEmptyFile.DELETED, trackChanges);
                    deletedFiles.put(delPath, delFile);
                }
            }
        }

        return deletedFiles;
    }


    protected void handleAddedLogEntries(Element log, String revNum,
            boolean trackChanges, Map<String, SvnFile> currentDeletions)
            throws IOException {

        List<Element> additions = XMLUtils.xPathElems(
            "paths/path[@action='A']", log);
        for (Element addition : additions) {
            if (trackChanges == false && currentDeletions.isEmpty())
                break;

            String addedPath = XMLUtils.getTextContents(addition);
            String addedUrl = getUrlForPath(addedPath);
            String addedKind = addition.getAttribute("kind");
            String histPath = addition.getAttribute("copyfrom-path");
            String histRev = addition.getAttribute("copyfrom-rev");

            // on svn 1.6 repositories, the addedKind attribute will be either
            // "file" or "dir".  On earlier repositories, this field will be
            // empty "".  In the optimistic case, if the repository is using
            // the recent format, make a record of the fact that this is
            // definitely a directory entry.
            boolean isDefinitelyDirEntry = "dir".equals(addedKind);

            if (!"file".equals(addedKind) && XMLUtils.hasValue(histPath)) {
                // this entry was added with history, and has a kind of either
                // "dir" or "".  If it is "dir", we make a list of currently
                // deleted files that match the history path, so we can rename
                // them.  If it is "", we need to guess whether this entry
                // is a directory or not.  The best way to do that is to make
                // the aforementioned list of deleted files; if anything
                // matches, we assume this is a renamed directory entry.
                Map<String, SvnFile> filesToRename = findFilesInDirectory(
                    currentDeletions, histPath);
                for (Map.Entry<String, SvnFile> e : filesToRename.entrySet()) {
                    String oldPath = e.getKey();
                    String relativePath = oldPath.substring(histPath.length());
                    String newPath = addedPath + relativePath;
                    SvnFile renamedFile = e.getValue();

                    renamedFile.undeleteAndRename(getFilenameFromPath(newPath));
                    renamedFile.addVersion(new SvnRemoteFile(
                            getUrlForPath(newPath), revNum), trackChanges);

                    filesByPath.remove(oldPath);
                    filesByPath.put(newPath, renamedFile);

                    isDefinitelyDirEntry = true;
                }
            }

            if (isDefinitelyDirEntry == false) {
                // this entry might be a file.  Handle it accordingly.
                SvnFile file = currentDeletions.remove(histPath);
                if (file != null) {
                    file.undeleteAndRename(getFilenameFromPath(addedPath));
                    filesByPath.remove(histPath);
                    filesByPath.put(addedPath, file);

                } else if (trackChanges) {
                    file = new SvnFile(getFilenameFromPath(addedPath));
                    if (XMLUtils.hasValue(histPath) == false)
                        file.addVersion(SvnEmptyFile.ADDED, false);
                    else
                        file.addVersion(new SvnRemoteFile(
                            getUrlForPath(histPath), histRev), false);

                    files.add(file);
                    filesByPath.put(addedPath, file);
                }

                if (file != null)
                    file.addVersion(new SvnRemoteFile(addedUrl, revNum),
                        trackChanges);
            }
        }
    }


    protected void handleModifiedLogEntries(Element log, String revNum,
            boolean trackChanges) throws IOException {

        List<Element> modifications = XMLUtils.xPathElems(
            "paths/path[@action='M']", log);
        for (Element modification : modifications) {
            String modKind = modification.getAttribute("kind");
            if ("dir".equals(modKind))
                continue;

            String modPath = XMLUtils.getTextContents(modification);
            String modUrl = getUrlForPath(modPath);
            SvnFile modFile = filesByPath.get(modPath);

            if (modFile == null && trackChanges) {
                modFile = new SvnFile(getFilenameFromPath(modPath));
                files.add(modFile);
                filesByPath.put(modPath, modFile);
                modFile.addVersion(new SvnRemoteFile(modUrl, revNum,
                        previousRev(revNum)), false);
            }

            if (modFile != null)
                modFile.addVersion(new SvnRemoteFile(modUrl, revNum),
                    trackChanges);
        }
    }


    protected void discardApparentDirectories() {
        TreeSet<String> allPaths = new TreeSet<String>(filesByPath.keySet());
        String lastPath = "no-such-path";
        for (String onePath : allPaths) {
            if (onePath.startsWith(lastPath + "/")) {
                logger.fine("discarding apparent directory " + lastPath);
                SvnFile dirFile = filesByPath.remove(lastPath);
                files.remove(dirFile);
            }
            lastPath = onePath;
        }
    }




    // Methods for handling local modifications.  For this logic, we don't have
    // to worry about creating and queueing SvnTask objects because these
    // operations do not contact the server, so the execute very quickly.

    protected void processLocalMods() throws IOException {
        Document status = svn.execXml("status", "--xml", getChangelistArgs());
        handleLocalModifications(status);
        Map<String, SvnFile> filesDeletedLocally = handleLocalDeletions(status);
        handleLocalAdditions(status, filesDeletedLocally);
    }

    protected List<String> getChangelistArgs() {
        if (changelistName == null)
            return null;
        else
            return Arrays.asList(new String[] { "--changelist",
                    changelistName });
    }

    protected void handleLocalModifications(Document status) {
        List<Element> modifiedWcStats = XMLUtils.xPathElems(
            "/status/*/entry/wc-status[@item='modified']", status);
        for (Element oneModStat : modifiedWcStats) {
            Element oneMod = (Element) oneModStat.getParentNode();
            String modWcPath = oneMod.getAttribute("path");
            String modPath = getPathFromWcPath(modWcPath);

            SvnFile modFile = filesByPath.get(modPath);
            if (modFile == null) {
                modFile = new SvnFile(getFilenameFromPath(modPath));
                files.add(modFile);
                filesByPath.put(modPath, modFile);
            }

            modFile.addVersion(new SvnBaseFile(modWcPath), false);
            modFile.addVersion(new SvnWorkingFile(modWcPath), true);
        }
    }

    protected Map<String, SvnFile> handleLocalDeletions(Document status) {
        Map<String, SvnFile> deletions = new HashMap<String, SvnFile>();

        List<Element> deletedWcStats = XMLUtils.xPathElems(
            "/status/*/entry/wc-status[@item='deleted']", status);
        for (Element oneDelStat : deletedWcStats) {
            Element oneDel = (Element) oneDelStat.getParentNode();
            String delWcPath = oneDel.getAttribute("path");
            String delPath = getPathFromWcPath(delWcPath);
            SvnFile delFile = filesByPath.get(delPath);

            if (delFile == null) {
                delFile = new SvnFile(getFilenameFromPath(delPath));
                files.add(delFile);
                filesByPath.put(delPath, delFile);
            }

            delFile.addVersion(new SvnBaseFile(delWcPath), false);
            delFile.addVersion(SvnEmptyFile.DELETED, true);
            deletions.put(delPath, delFile);
        }

        return deletions;
    }

    protected void handleLocalAdditions(Document status,
            Map<String, SvnFile> filesDeletedLocally) throws IOException {

        List<Element> addedWcStats = XMLUtils.xPathElems(
            "/status/*/entry/wc-status[@item='added']", status);
        for (Element oneAddStat : addedWcStats) {
            Element oneAdd = (Element) oneAddStat.getParentNode();
            String addWcPath = oneAdd.getAttribute("path");
            String addedPath = getPathFromWcPath(addWcPath);

            String histPath = null;
            if ("true".equals(oneAddStat.getAttribute("copied"))) {
                Document info = svn.execXml("info", "--xml", addWcPath);
                String histUrl = XMLUtils.xPathStr(
                    "/info/entry/wc-info/copy-from-url", info);
                histPath = getPathForUrl(histUrl);
            }

            SvnFile file = filesDeletedLocally.remove(histPath);
            if (file != null) {
                file.undeleteAndRename(getFilenameFromPath(addedPath));
                filesByPath.remove(histPath);
                filesByPath.put(addedPath, file);

            } else {
                file = new SvnFile(getFilenameFromPath(addedPath));
                files.add(file);
                filesByPath.put(addedPath, file);

                if (histPath == null)
                    file.addVersion(SvnEmptyFile.ADDED, false);
                else
                    file.addVersion(new SvnBaseFile(addWcPath), false);
            }

            file.addVersion(new SvnWorkingFile(addWcPath), true);
        }

    }



    protected void enqueueFileRetrievalTasks() {
        for (SvnFile file : files) {
            for (Object fileVer : file.getVersions()) {
                if (fileVer instanceof SvnTask) {
                    svn.queue((SvnTask) fileVer);
                }
            }
        }
    }



    /// Various utility routines

    protected Map<String, SvnFile> findFilesInDirectory(
            Map<String, SvnFile> files, String dirPath) {
        String dirPrefix = dirPath;
        if (!dirPrefix.endsWith("/"))
            dirPrefix = dirPrefix + "/";
        Map<String, SvnFile> result = null;
        for (Map.Entry<String, SvnFile> e : files.entrySet()) {
            if (e.getKey().startsWith(dirPrefix)) {
                if (result == null)
                    result = new HashMap<String, SvnFile>();
                result.put(e.getKey(), e.getValue());
            }
        }
        return (result == null ? Collections.EMPTY_MAP : result);
    }

    protected String getUrlForPath(String path) {
        return svn.getRootUrl() + path;
    }

    protected String getPathForUrl(String url) {
        String rootUrl = svn.getRootUrl();
        if (url.startsWith(rootUrl)) {
            url = url.substring(rootUrl.length());
            return HTMLUtils.urlDecode(url);
        } else {
            return null;
        }
    }

    protected String getFilenameFromUrl(String url) {
        String baseUrl = svn.getBaseUrl();
        if (url.startsWith(baseUrl) && url.length() > baseUrl.length())
            url = url.substring(baseUrl.length() + 1);
        return HTMLUtils.urlDecode(url);
    }

    protected String getFilenameFromPath(String path) {
        String basePath = svn.getBasePath();
        if (path.startsWith(basePath) && path.length() > basePath.length())
            path = path.substring(basePath.length() + 1);
        return path;
    }

    protected String getPathFromWcPath(String wcPath) {
        return svn.getBasePath() + "/" + wcPath.replace('\\', '/');
    }

    protected String previousRev(String revision) {
        int revNum = Integer.parseInt(revision);
        int prevNum = revNum - 1;
        return Integer.toString(prevNum);
    }

    protected String trimToNull(String s) {
        if (s == null)
            return null;
        s = s.trim();
        if (s.length() == 0)
            return null;
        else
            return s;
    }

}
