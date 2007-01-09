// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.mgr;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipOutputStream;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.tool.export.impl.ExternalResourceArchiver;
import net.sourceforge.processdash.tool.export.impl.ExternalResourceArchiverXMLv1;
import net.sourceforge.processdash.tool.export.impl.ExternalResourceAutoLocator;
import net.sourceforge.processdash.tool.export.impl.ExternalResourceXmlConstantsv1;

public class ExternalResourceManager {

    public static final String INITIALIZATION_MODE_PROPERTY_NAME = //
    ExternalResourceManager.class.getName() + ".initMode";

    public static final String INITIALIZATION_MODE_ARCHIVE = "fromArchive";

    public static final String INITIALIZATION_MODE_AUTO = "autoSearch";

    private static final Logger logger = Logger
            .getLogger(ExternalResourceManager.class.getName());


    private static ExternalResourceManager INSTANCE = null;

    public static synchronized ExternalResourceManager getInstance() {
        if (INSTANCE == null)
            INSTANCE = new ExternalResourceManager();
        return INSTANCE;
    }

    Map pathRemappings = null;

    Map generalizedRemappings = null;

    ExternalResourceManager() {}

    public boolean isArchivedItem(String filename) {
        return filename.indexOf(ExternalResourceXmlConstantsv1.ARCHIVE_PATH) != -1;
    }

    public void addExternalResourcesToBackup(ZipOutputStream out)
            throws IOException {
        ExternalResourceArchiver archiver = new ExternalResourceArchiverXMLv1();
        dispatchAllImportInstructions(archiver);
        archiver.export(out);
    }

    public void initializeMappings(File baseDir) {
        pathRemappings = null;

        String setting = System.getProperty(INITIALIZATION_MODE_PROPERTY_NAME);
        logger.log(Level.FINE, "initialization mode property is {0}", setting);

        if (INITIALIZATION_MODE_ARCHIVE.equalsIgnoreCase(setting)) {
            // if this data was extracted from a ZIP File, check to see if
            // external resources were included in that ZIP.
            ExternalResourceArchiverXMLv1 loader = new ExternalResourceArchiverXMLv1();
            pathRemappings = normalizeMappings(loader.load(baseDir));
            if (pathRemappings == null)
                // the zip file did not contain any archived external resources.
                // this might mean it wasn't created by the FileBackupManager,
                // but by an individual zipping up a set of team data
                // directories. Fall back to the auto-search mode.
                setting = INITIALIZATION_MODE_AUTO;
        }

        if (INITIALIZATION_MODE_AUTO.equalsIgnoreCase(setting)) {
            ExternalResourceAutoLocator loader = new ExternalResourceAutoLocator();
            dispatchAllImportInstructions((ImportInstructionDispatcher) loader);
            pathRemappings = normalizeMappings(loader.load(baseDir));
        }

        generalizedRemappings = ExternalResourceAutoLocator
                .getGeneralizedMappings(pathRemappings);

        if (setting != null) {
            logger.config("Path remappings: " + pathRemappings);
            logger.config("Generalized remappings: " + generalizedRemappings);
        }
    }

    private Map normalizeMappings(Map mappings) {
        if (mappings == null || mappings.isEmpty())
            return null;

        Map result = new HashMap();

        for (Iterator i = mappings.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String origPath = (String) e.getKey();
            String newPath = (String) e.getValue();
            result.put(normalize(origPath), normalize(newPath));
        }
        return result;
    }

    private String normalize(String path) {
        return path.replace('\\', '/');
    }

    private String denormalize(String path) {
        return path.replace('/', File.separatorChar);
    }

    private void dispatchAllImportInstructions(ImportInstructionDispatcher d) {
        int i = ImportManager.getInstance().getInstructionCount();
        while (i-- > 0)
            ImportManager.getInstance().getInstruction(i).dispatch(d);
    }

    public String remapFilename(String origFile) {
        if (pathRemappings == null || origFile == null)
            return origFile;

        String origNormalizedPath = normalize(origFile);

        for (Iterator i = pathRemappings.entrySet().iterator(); i.hasNext();) {
            String remappedPath = performAbsoluteRemapping(origNormalizedPath,
                    (Map.Entry) i.next());
            if (remappedPath != null)
                return denormalize(remappedPath);
        }

        for (Iterator i = generalizedRemappings.entrySet().iterator(); i
                .hasNext();) {
            String remappedPath = performGeneralizedRemapping(
                    origNormalizedPath, (Map.Entry) i.next());
            if (remappedPath != null)
                return denormalize(remappedPath);
        }

        return origFile;
    }

    static String performAbsoluteRemapping(String path, Map.Entry mapping) {
        return performAbsoluteRemapping(path, (String) mapping.getKey(),
                (String) mapping.getValue());
    }

    static String performAbsoluteRemapping(String origPath, String fromPath,
            String toPath) {
        if (origPath.equalsIgnoreCase(fromPath))
            return toPath;
        if (origPath.regionMatches(true, 0, fromPath, 0, fromPath.length())
                && origPath.charAt(fromPath.length()) == '/')
            return toPath + origPath.substring(fromPath.length());
        return null;
    }

    static String performGeneralizedRemapping(String path, Map.Entry mapping) {
        return performGeneralizedRemapping(path, (String) mapping.getKey(),
                (String) mapping.getValue());
    }

    static String performGeneralizedRemapping(String origPath,
            String generalizedPath, String toPath) {
        String origLower = origPath.toLowerCase();
        generalizedPath = generalizedPath.toLowerCase();

        if (origLower.endsWith(generalizedPath))
            return toPath;
        int pos = origLower.indexOf(generalizedPath);
        if (pos != -1) {
            int end = pos + generalizedPath.length();
            if (origPath.charAt(end) == '/')
                return toPath + origPath.substring(end);
        }
        return null;
    }

    /**
     * Discard an irrelevant externalResources subdirectory if appropriate. The
     * "externalResources" subdirectory is only intended for use when we are
     * running in "fromArchive" mode - that is, when the user has used the
     * QuickLauncher to open a ZIP file. In any other circumstance, it normally
     * should not exist. Unfortuanely, there is a series of events which could
     * leave one lying around:
     * <ol>
     * <li>User restores their data to a former state by unzipping files in
     * their backup subdirectory. This creates a bogus externalResources subdir,
     * containing snapshots of the external files as of the date of their newest
     * backup file.</li>
     * <li>Subsequently, the user <b>manually</b> ZIPs up their data directory
     * (rather than using the automated menu option).</li>
     * <li>Someone opens the resulting ZIP with the QuickLauncher. It will
     * mistakenly think that the externalResources directory is relevant.</li>
     * </ol>
     * This might seem like a corner case, but the instant step 1 happens above,
     * the problem would never resolve itself. If the dashboard instance in
     * question was a team dashboard instance, this could lead to very confusing
     * behavior on the part of people using QuickLauncher. Manually created ZIPs
     * would show old data, while automatically created ZIPs would show new
     * data. This method addresses the problem by deleting the externalResources
     * subdirectory if we are in any initialization mode other than
     * "fromArchive". Thus, the first time a dashboard instance was started
     * normally after step 1 occurred, this method would cleanup the bogus
     * externalResources directory that was extracted from the backup.
     */
    public void cleanupBogusExtResDirectory(File baseDir) {
        String setting = System.getProperty(INITIALIZATION_MODE_PROPERTY_NAME);
        if (INITIALIZATION_MODE_ARCHIVE.equals(setting) == false
                && Settings.isReadWrite())
            ExternalResourceArchiverXMLv1.cleanupBogusArchiveDirectory(baseDir);
    }

}
