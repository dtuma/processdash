// Copyright (C) 2002-2010 Tuma Solutions, LLC
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

package teamdash.templates.setup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.HierarchyAlterer;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.HierarchyAlterer.HierarchyAlterationException;
import net.sourceforge.processdash.tool.bridge.client.DirectoryPreferences;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RobustFileWriter;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

class MigrationToolTeam {

    private DashboardContext ctx;

    private String projectPath;

    private String targetPID;

    private static final Logger logger = Logger
            .getLogger(MigrationToolIndivLauncher.class.getName());



    MigrationToolTeam(DashboardContext ctx, String projectPath, String targetPID) {
        this.ctx = ctx;
        this.projectPath = projectPath;
        this.targetPID = targetPID;
    }

    public void migrate() throws Exception {
        File dataFile = getRootDataFile();

        makeExtraBackup();

        System.out.println("Starting migration of team project '" + projectPath
                + "'");

        DataRepository data = ctx.getData();
        data.closeDatafile(projectPath);

        migrateDatafileContents(dataFile);

        data.openDatafile(projectPath, dataFile.getPath());

        System.out.println("Finished migrating team project '" + projectPath
                + "'");
    }

    private void migrateDatafileContents(File dataFile) throws IOException {
        StringBuffer contents = slurpFile(dataFile);
        StringUtils.findAndReplace(contents, "/team_root_dataFile.txt>",
            "/team2_root_dataFile.txt>");
        String newline = System.getProperty("line.separator");
        contents.append(newline).append("Team_Project_Migration_Complete==1")
                .append(newline);
        Writer out = new BufferedWriter(new RobustFileWriter(dataFile));
        out.write(contents.toString());
        out.close();
    }


    public void convert() throws Exception {
        checkConversionPreconditions();
        File dataFile = getRootDataFile();

        makeExtraBackup();

        System.out.println("Starting conversion of team project '" + projectPath
                + "'");

        updateSettingsXmlFile();

        convertHierarchy();

        DataRepository data = ctx.getData();
        data.closeDatafile(projectPath);

        convertDatafileContents(dataFile);

        data.openDatafile(projectPath, dataFile.getPath());

        System.out.println("Finished converting team project '" + projectPath
                + "'");
    }

    private String currentPID;
    private File teamDataDirectory;
    private String newRootTemplateID;

    private void checkConversionPreconditions() throws MigrationException {
        if (Settings.isReadOnly())
            throw new MigrationException("isReadOnly");

        ProcessDashboard dash = (ProcessDashboard) ctx;
        if (dash.isHierarchyEditorOpen())
            throw new MigrationException("hierarchyEditorOpen");

        DataContext data = ctx.getData().getSubcontext(projectPath);

        SimpleData sd = data.getSimpleValue("Master_Project_Path");
        if (sd != null && sd.test())
            throw new MigrationException("belongsToMasterProject");

        sd = data.getSimpleValue("Team_Process_PID");
        currentPID = (sd == null ? null : sd.format());
        if (targetPID == null || targetPID.equals(currentPID))
            throw new MigrationException("targetPIDIsNoChange").add(
                "targetPID", targetPID);

        newRootTemplateID = targetPID + TEAM_ROOT;
        if (!DashController.getTemplates().containsKey(newRootTemplateID))
            throw new MigrationException("targetTemplateNotInstalled").add(
                "targetPID", targetPID);

        sd = data.getSimpleValue("Team_Data_Directory");
        if (sd == null || !sd.test())
            throw new MigrationException("noTeamDataDir");

        teamDataDirectory = new File(sd.format());
        if (!teamDataDirectory.isDirectory())
            throw new MigrationException("cannotFindTeamDataDir");
    }

    private void updateSettingsXmlFile() throws IOException {
        String jarName = tryToCopyProcessJar();

        File settingsFile = new File(teamDataDirectory, "settings.xml");
        StringBuffer settings = slurpFile(settingsFile);

        String oldPIDLine = "processID='" + currentPID + "'";
        String newPIDLine = "processID='" + targetPID + "'";
        StringUtils.findAndReplace(settings, oldPIDLine, newPIDLine);

        if (jarName != null) {
            Pattern p = Pattern.compile("/Templates/(.*\\.zip)");
            Matcher m = p.matcher(settings);
            if (m.find())
                settings.replace(m.start(1), m.end(1),
                    XMLUtils.escapeAttribute(jarName));
        }

        Writer out = new BufferedWriter(new RobustFileWriter(settingsFile));
        out.write(settings.toString());
        out.close();
    }

    private String tryToCopyProcessJar() {
        String sourceJarFileName = wizard
                .findTeamProcessJarfile(newRootTemplateID);
        if (sourceJarFileName == null)
            return null;

        File srcJar = new File(sourceJarFileName);
        if (!srcJar.isFile())
            return null;

        File teamDir = teamDataDirectory.getParentFile().getParentFile();
        File templatesDir = new File(teamDir, "Templates");
        if (!templatesDir.isDirectory() && !templatesDir.mkdirs())
            return null;

        String jarName = srcJar.getName();
        File destJar = new File(templatesDir, jarName);
        if (!destJar.exists()) {
            try {
                FileUtils.copyFile(srcJar, destJar);
            } catch (IOException e) {
                return null;
            }
        }

        return jarName;
    }

    private void convertHierarchy() throws HierarchyAlterationException {
        ProcessDashboard dash = (ProcessDashboard) ctx;
        HierarchyAlterer alt = new HierarchyAlterer(dash);
        DashHierarchy hier = ctx.getHierarchy();
        PropertyKey key = hier.findExistingKey(projectPath);

        // delete the children of the team project root node.
        for (int i = hier.getNumChildren(key);  i-- > 0; ) {
            PropertyKey child = hier.getChildKey(key, i);
            alt.deleteNode(child.path());
        }

        // change the template ID of the root node.
        alt.setTemplateId(projectPath, newRootTemplateID);
    }

    private void convertDatafileContents(File dataFile) throws IOException {
        StringBuffer contents = slurpFile(dataFile);

        String oldIncludeStr = "#include <" + currentPID + "/";
        String newIncludeStr = "#include <" + targetPID + "/";
        StringUtils.findAndReplace(contents, oldIncludeStr, newIncludeStr);

        Writer out = new BufferedWriter(new RobustFileWriter(dataFile));
        out.write(contents.toString());
        out.close();
    }


    private File getRootDataFile() throws MigrationException {
        String dataFileName = getDataFileName(ctx, projectPath);
        File dataFile = getDataDirFile(dataFileName);
        if (dataFile == null)
            throw new MigrationException("notTeamProject");
        return dataFile;
    }

    private String getDataFileName(DashboardContext ctx, String path) {
        DashHierarchy hier = ctx.getHierarchy();
        PropertyKey key = hier.findExistingKey(path);
        if (key == null) {
            logger.severe("No property key found for '" + path + "'");
            return null;
        }

        Prop p = hier.pget(key);
        if (p == null) {
            logger.severe("No prop found for '" + path + "'");
            return null;
        }

        String dataFileName = p.getDataFile();
        if (dataFileName == null) {
            logger.severe("No datafile associated with '" + path + "'");
            return null;
        }
        return dataFileName;

    }

    private File getDataDirFile(String filename) {
        if (filename == null)
            return null;

        File result = new File(ProcessDashboard.getDefaultDirectory(), filename);
        if (!result.isFile()) {
            logger.severe("File does not exist '" + result.getPath() + "'");
            return null;
        }

        return result;
    }

    private void makeExtraBackup() throws IOException {
        String newBackupFilename = getExtraBackupFilename();
        File backupDir = getExtraBackupDir();
        File newBackupFile = new File(backupDir, newBackupFilename);

        System.out.println("Before migration, backing up data to '"
                + newBackupFile.getPath() + "'");
        File backup = DashController.backupData();
        FileUtils.copyFile(backup, newBackupFile);
    }

    private String getExtraBackupFilename() {
        DateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss");
        String newBackupFilename = "before-migration-"
                + dateFmt.format(new Date()) + ".zip";
        return newBackupFilename;
    }

    private File getExtraBackupDir() {
        File dataDir = new File(ProcessDashboard.getDefaultDirectory());
        File backupSubdir = new File(dataDir, "backup");
        if (backupSubdir.isDirectory())
            return backupSubdir;

        File appDir = DirectoryPreferences.getApplicationDirectory();
        backupSubdir = new File(appDir, "backup");
        if (backupSubdir.isDirectory() || backupSubdir.mkdirs())
            return backupSubdir;

        return new File(".");
    }




    private StringBuffer slurpFile(File f) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(f));
        StringBuffer result = new StringBuffer();
        int i;
        while ((i = in.read()) != -1)
            result.append((char) i);
        in.close();
        return result;
    }

    private static final String TEAM_ROOT = "/TeamRoot";

}
