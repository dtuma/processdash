// Copyright (C) 2015-2021 Tuma Solutions, LLC
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

package teamdash.hist;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.tool.bridge.bundle.FileBundleUtils;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectoryFactory;
import net.sourceforge.processdash.util.DateUtils;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.XMLUtils;

import teamdash.wbs.ChangeHistory;
import teamdash.wbs.ChangeHistory.Entry;
import teamdash.wbs.WBSFilenameConstants;

public class ProjectHistoryLocal implements ProjectHistory<Entry> {

    private File dir;

    private List<Entry> versions;

    private Map<String, File> zipFiles;

    public ProjectHistoryLocal(File dir) throws IOException {
        if (!dir.isDirectory())
            throw new FileNotFoundException("No such directory " + dir);
        this.dir = dir;
        refresh();
    }

    public void refresh() throws IOException {
        loadChangeHistory();
        List<Entry> firstEntries = null;
        if (versions.size() > 1)
            firstEntries = new ArrayList(versions.subList(0, 2));
        findZipFiles(dir);
        for (Iterator i = versions.iterator(); i.hasNext();) {
            Entry e = (Entry) i.next();
            if (!zipFiles.containsKey(e.getUid()))
                i.remove();
        }
        if (firstEntries != null && !versions.isEmpty()
                && versions.get(0) == firstEntries.get(1))
            versions.add(0, firstEntries.get(0));
    }

    private void loadChangeHistory() {
        File dir = this.dir;
        if (FileBundleUtils.isBundledDir(dir))
            dir = ImportDirectoryFactory.getInstance().get(dir.getPath())
                    .getDirectory();
        versions = new ChangeHistory(dir).getEntries();
    }

    private void findZipFiles(File dir) throws IOException {
        zipFiles = new HashMap<String, File>();
        findZipFilesInBackupSubdir(dir);
        findZipFilesInBundlesSubdir(dir);
    }

    private void findZipFilesInBackupSubdir(File dir) throws IOException {
        File backupDir = new File(dir, "backup");
        File[] backupFiles = backupDir.listFiles();
        if (backupFiles == null || backupFiles.length == 0)
            return;

        for (File oneFile : backupFiles) {
            String zipVersionUid = getZipFileVersionUid(oneFile);
            if (zipVersionUid != null)
                zipFiles.put(zipVersionUid, oneFile);
        }
    }

    private String getZipFileVersionUid(File oneFile) throws IOException {
        // only examine backups created by a save operation
        Matcher m = SAVED_ZIP_FILENAME_PAT.matcher(oneFile.getName());
        if (!m.matches())
            return null;

        // if the backup contained a changeHistory.xml file, read its UID
        String changeHistUid = getZipFileChangeHistoryUid(oneFile);
        if (changeHistUid != null)
            return changeHistUid;

        // extract the user name and save date from the ZIP filename.
        long zipDate;
        try {
            zipDate = SAVE_DATE_FMT.parse(m.group(1)).getTime();
        } catch (ParseException pe) {
            return null;
        }
        String safeZipUser = m.group(2);

        // look through the change entries for one that could correspond to this
        // ZIP backup file.
        String bestMatch = null;
        long bestDelta = DateUtils.DAY;
        for (Entry e : versions) {
            // if this ZIP file was saved by a different person, skip it.
            String safeVersionUser = FileUtils.makeSafe(e.getUser());
            if (!safeZipUser.equalsIgnoreCase(safeVersionUser))
                continue;

            // compare the change history timestamp with the ZIP file timestamp.
            // If they match, return this date.
            long oneDelta = Math.abs(e.getTimestamp().getTime() - zipDate);
            if (oneDelta < 2000)
                return e.getUid();

            // The ZIP file timestamp does not include a time zone offset, so
            // the two values above could differ if the file was saved by a
            // user in a different timezone. If the values differ by an amount
            // that looks like a time zone delta, consider this to be a match.
            // (Usually this would be an even number of hours; but since some
            // time zones have a 30 minute offset, we allow for that.)
            long oneDiff = oneDelta % (30 * DateUtils.MINUTES);
            if (oneDiff > 2000)
                continue;

            // in the extremely rare case where a WBS was saved twice with a
            // separation of exactly "n" hours, select the closest save time.
            if (bestDelta > oneDelta) {
                bestMatch = e.getUid();
                bestDelta = oneDelta;
            }
        }
        return bestMatch;
    }

    private void findZipFilesInBundlesSubdir(File dir) throws IOException {
        File bundlesDir = new File(dir, "bundles");
        File[] bundleFiles = bundlesDir.listFiles();
        if (bundleFiles == null || bundleFiles.length == 0)
            return;

        for (File oneFile : bundleFiles) {
            if (oneFile.getName().endsWith("-wbs.zip")) {
                String bundleUid = getZipFileChangeHistoryUid(oneFile);
                if (bundleUid != null)
                    zipFiles.put(bundleUid, oneFile);
            }
        }
    }

    private String getZipFileChangeHistoryUid(File oneFile) throws IOException {
        // see if the backup contained a changeHistory.xml file. If so, the last
        // entry in that file identifies its version.
        try {
            InputStream changeHist = getFileFromZip(oneFile,
                WBSFilenameConstants.CHANGE_HISTORY_FILE);
            if (changeHist != null) {
                Element xml = XMLUtils.parse(changeHist).getDocumentElement();
                ChangeHistory zipHist = new ChangeHistory(xml);
                Entry zipHistEntry = zipHist.getLastEntry();
                if (zipHistEntry != null)
                    return zipHistEntry.getUid();
            }
        } catch (SAXException se) {
        }

        return null;
    }

    private static final Pattern SAVED_ZIP_FILENAME_PAT = Pattern.compile(
        "backup-(\\d{14})-saved_by_(.*)\\.zip", Pattern.CASE_INSENSITIVE);

    private static final DateFormat SAVE_DATE_FMT = new SimpleDateFormat(
            "yyyyMMddHHmmss");

    public List<Entry> getVersions() {
        return versions;
    }

    public Date getVersionDate(Entry version) {
        return version.getTimestamp();
    }

    public String getVersionAuthor(Entry version) {
        return version.getUser();
    }

    public InputStream getVersionFile(Entry version, String filename)
            throws IOException {
        File srcZip = zipFiles.get(version.getUid());
        if (srcZip == null)
            return null;
        else
            return getFileFromZip(srcZip, filename);
    }

    private InputStream getFileFromZip(File srcZip, String filename)
            throws IOException {
        ZipFile zip = new ZipFile(srcZip);
        ZipEntry entry = zip.getEntry(filename);
        if (entry == null)
            return null;

        return zip.getInputStream(entry);
    }

    @Override
    public ProjectHistoryException wrapException(Throwable e) {
        return new ProjectHistoryException(e, "Dir.Cannot_Read_HTML_FMT",
                dir.getPath());
    }

}
