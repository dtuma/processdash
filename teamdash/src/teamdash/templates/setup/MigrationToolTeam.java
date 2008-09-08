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

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.tool.bridge.client.DirectoryPreferences;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RobustFileWriter;
import net.sourceforge.processdash.util.StringUtils;

class MigrationToolTeam {

    private DashboardContext ctx;

    private String projectPath;

    private static final Logger logger = Logger
            .getLogger(MigrationToolIndivLauncher.class.getName());



    MigrationToolTeam(DashboardContext ctx, String projectPath) {
        this.ctx = ctx;
        this.projectPath = projectPath;
    }

    public void migrate() throws Exception {
        String dataFileName = getDataFileName(ctx, projectPath);
        File dataFile = getDataDirFile(dataFileName);
        if (dataFile == null)
            throw new MigrationException("notTeamProject");

        makeExtraBackup();

        System.out.println("Starting migration of team project '" + projectPath
                + "'");

        DataRepository data = ctx.getData();
        data.closeDatafile(projectPath);

        tweakDatafileContents(dataFile);

        data.openDatafile(projectPath, dataFile.getPath());

        System.out.println("Finished migrating team project '" + projectPath
                + "'");
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

    private void tweakDatafileContents(File dataFile) throws IOException {
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



    private StringBuffer slurpFile(File f) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(f));
        StringBuffer result = new StringBuffer();
        int i;
        while ((i = in.read()) != -1)
            result.append((char) i);
        in.close();
        return result;
    }


}
