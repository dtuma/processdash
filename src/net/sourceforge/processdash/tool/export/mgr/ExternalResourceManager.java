// Copyright (C) 2007-2017 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.mgr;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipOutputStream;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.tool.export.impl.ExternalResourceArchiver;
import net.sourceforge.processdash.tool.export.impl.ExternalResourceArchiverXMLv1;
import net.sourceforge.processdash.tool.export.impl.ExternalResourceAutoLocator;
import net.sourceforge.processdash.tool.export.impl.ExternalResourceManifestXMLv1;
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

    DashboardContext dashboardContext = null;

    ExternalLocationMapper mapper = null;

    File defaultMapDataSource = null;

    String datasetUrl = null;

    List<ExternalResourceManifestXMLv1.MCFEntry> mcfMappings = null;

    ExternalResourceManager() {}

    public boolean isArchivedItem(String filename) {
        return filename.indexOf(ExternalResourceXmlConstantsv1.ARCHIVE_PATH) != -1;
    }

    public void setDashboardContext(DashboardContext dashboardContext) {
        this.dashboardContext = dashboardContext;
        String workingDirLocation = dashboardContext.getWorkingDirectory()
                .getDescription();
        if (workingDirLocation != null && workingDirLocation.startsWith("http"))
            datasetUrl = workingDirLocation;
    }

    public void addExternalResourcesToBackup(ZipOutputStream out)
            throws IOException {
        ExternalResourceArchiver archiver = new ExternalResourceArchiverXMLv1();
        archiver.setDashboardContext(dashboardContext);
        dispatchAllImportInstructions(archiver);
        archiver.export(out);
    }

    public void initializeMappings(File baseDir) {
        String setting = System.getProperty(INITIALIZATION_MODE_PROPERTY_NAME);
        logger.log(Level.FINE, "initialization mode property is {0}", setting);
        initializeMappings(baseDir, setting);
    }

    public void initializeMappings(File baseDir, String setting) {
        mapper = ExternalLocationMapper.getInstance();

        if (INITIALIZATION_MODE_ARCHIVE.equalsIgnoreCase(setting)) {
            // if this data was extracted from a ZIP File, check to see if
            // external resources were included in that ZIP.
            ExternalResourceManifestXMLv1 loader = new ExternalResourceManifestXMLv1();
            if (mapper.loadMappings(loader.load(baseDir))) {
                defaultMapDataSource = baseDir;
                datasetUrl = loader.getDatasetUrl();
                mcfMappings = loader.getMcfEntries();
            } else {
                // the zip file did not contain any archived external resources.
                // this might mean it wasn't created by the FileBackupManager,
                // but by an individual zipping up a set of team data
                // directories. Fall back to the auto-search mode.
                setting = INITIALIZATION_MODE_AUTO;
            }
        }

        if (INITIALIZATION_MODE_AUTO.equalsIgnoreCase(setting)) {
            final ExternalResourceAutoLocator loader = new ExternalResourceAutoLocator();
            dispatchAllImportInstructions(new ImportInstructionDispatcher() {
                public Object dispatch(ImportDirectoryInstruction instr) {
                    String directory = instr.getDirectory();
                    if (directory != null && directory.length() > 0)
                        loader.addImportedPath(directory);
                    return null;
                }});
            mapper.loadMappings(loader.load(baseDir));
            // NOTE: this style of resource mapping is not supported by the
            // "default map data source" functionality at this time.
        }
    }

    private void dispatchAllImportInstructions(ImportInstructionDispatcher d) {
        int i = ImportManager.getInstance().getInstructionCount();
        while (i-- > 0)
            ImportManager.getInstance().getInstruction(i).dispatch(d);
    }

    public String remapFilename(String origFile) {
        if (mapper == null)
            return origFile;
        else
            return mapper.remapFilename(origFile);
    }

    public String getDatasetUrl() {
        return datasetUrl;
    }

    public List<ExternalResourceManifestXMLv1.MCFEntry> getMcfs() {
        if (mcfMappings == null)
            return Collections.EMPTY_LIST;
        else
            return Collections.unmodifiableList(mcfMappings);
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

    /**
     * Sometimes it is necessary to launch a child process that is aware of
     * the mappings used by this class.  Call this method to retrieve a list
     * of arguments, then pass them to the child JVM.  Then,
     * {@link ExternalLocationMapper} will be able to load the mappings
     * without using extensive classpath dependencies.
     * 
     * @return a list of JVM args for mapping.  If no mapping is needed, will
     *     return an empty list.
     */
    public Map<String,String> getJvmArgsForMapping() {
        if (defaultMapDataSource == null)
            return Collections.EMPTY_MAP;
        else
            return Collections.singletonMap(
                    ExternalLocationMapper.DEFAULT_MAP_DATA_SOURCE_PROPERTY,
                    defaultMapDataSource.getAbsolutePath());
    }
}
