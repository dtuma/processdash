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

package net.sourceforge.processdash.team.setup.migrate;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.process.ProcessUtil;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.RuntimeUtils;
import net.sourceforge.processdash.util.StringUtils;

public class MigrationToolIndivLauncher {

    private DashboardContext ctx;

    private String projectPath;

    private PropertyKey projectRoot;

    private File targetDir;

    private String targetPID;

    private Map<String, String> args;

    private ListData failedPreconditions;

    private boolean showDeleteAdvice;

    private String[] cmdLine;

    private static final Resources resources = Resources
            .getDashBundle("TeamDataMigration");


    MigrationToolIndivLauncher(DashboardContext ctx, String projectPath) {
        this.ctx = ctx;
        this.projectPath = projectPath;
        this.args = new HashMap<String, String>();
    }

    public void startMigration() throws Exception {
        loadData();
        checkPreconditions();
        buildProcessCommandLine();

        new Thread() {
            public void run() {
                launchMigrationProcess();
            }
        }.start();
    }

    private void loadData() throws Exception {
        // validate the project path, and add to args
        projectRoot = ctx.getHierarchy().findExistingKey(projectPath);
        if (projectRoot == null)
            throw new MigrationException("notTeamProject");
        args.put(MigrationToolIndiv.PROJECT_PATH, projectPath);

        // retrieve the target directory, validate it, and add to args
        String targetDirName = ProcessDashboard.getDefaultDirectory();
        targetDir = new File(targetDirName);
        if (!targetDir.isDirectory())
            throw new MigrationException(new FileNotFoundException(
                    targetDirName));
        args.put(MigrationToolIndiv.TARGET_DIR, targetDir.getAbsolutePath());

        // check that the backup subdirectory exists - otherwise we're
        // probably in bridged mode.
        File backupSubdir = new File(targetDir, "backup");
        if (!backupSubdir.isDirectory())
            throw new MigrationException("cannotMigrate").add("bridgedMode");

        // check to see if we are performing a migration
        String rootTemplateID = ctx.getHierarchy().pget(projectRoot).getID();
        if (!rootTemplateID.endsWith(INDIV2_ROOT))
            args.put(MigrationToolIndiv.MIGRATION_NEEDED, "true");

        // add the process ID to the args
        String processId = getValue("Team_Process_PID").format();
        args.put(MigrationToolIndiv.PROCESS_ID, processId);

        // for a conversion, add the target process ID to the args
        SimpleData sd = getValue("Team_Project_Conversion_Needed");
        if (sd != null && sd.test()) {
            targetPID = sd.format();
            args.put(MigrationToolIndiv.TARGET_PROCESS_ID, targetPID);
        }

        // add the phase list to the args
        ProcessUtil proc = new ProcessUtil(ctx.getData(), projectPath);
        List phases = proc.getProcessListPlain("Phase_List");
        args.put(MigrationToolIndiv.PHASE_LIST, StringUtils.join(phases, "/"));
    }

    private void checkPreconditions() throws Exception {
        failedPreconditions = new ListData();
        showDeleteAdvice = false;

        assertTargetProcessInstalled();
        assertNoRootDefects();
        assertNoRootPhases();
        assertNoMultiplePhases();

        if (failedPreconditions.test()) {
            putValue("Failed_Migration_Preconditions", failedPreconditions);
            MigrationException me = new MigrationException("failedPreconditions");
            if (showDeleteAdvice)
                me.add("showDeleteAdvice");
            throw me;
        }
    }

    private void assertTargetProcessInstalled() {
        if (targetPID == null)
            return;

        String targetRootTemplateID = targetPID + INDIV2_ROOT;
        if (DashController.getTemplates().containsKey(targetRootTemplateID))
            return;

        String message = getRes("Error.Target_Process_Not_Found_FMT",
            esc(targetPID));
        failedPreconditions.add(message);
    }

    private void assertNoRootDefects() {
        Prop p = ctx.getHierarchy().pget(projectRoot);
        String defectLogFilename = p.getDefectLog();
        if (!StringUtils.hasValue(defectLogFilename))
            return;

        File defectLogFile = new File(targetDir, defectLogFilename);
        if (defectLogFile.length() == 0)
            return;

        String message = getRes("Error.Root_Defects_FMT", esc(projectPath));
        failedPreconditions.add(message);
    }

    private void assertNoRootPhases() {
        List<String> rootPhases = getPhaseChildren(projectRoot);
        if (rootPhases == null || rootPhases.isEmpty())
            return;

        String message = getRes("Error.Root_Phases_FMT",
            enumeratePaths(projectPath, rootPhases));
        failedPreconditions.add(message);
        showDeleteAdvice = true;
    }

    private void assertNoMultiplePhases() {
        assertNoMultiplePhases(projectRoot);
    }

    private void assertNoMultiplePhases(PropertyKey node) {
        Prop p = ctx.getHierarchy().pget(node);
        if (p.getNumChildren() == 0)
            return;

        String templateID = p.getID();
        if (templateID != null && templateID.startsWith("PSP"))
            return;

        List<String> phases = getPhaseChildren(node);
        int nonPhaseChildCount = p.getNumChildren() - phases.size();

        if (node == projectRoot) {
            // don't perform these tests for the root node - they aren't
            // applicable there.

        } else if (!phases.isEmpty() && nonPhaseChildCount > 0) {
            String message = getRes("Error.Hybrid_Node_FMT", esc(node.path()),
                enumeratePaths(node.path(), phases));
            failedPreconditions.add(message);
            showDeleteAdvice = true;

        } else if (phases.size() > 1) {
            String message = getRes("Error.Multiple_Phases_FMT",
                esc(node.path()), enumeratePaths(node.path(), phases));
            failedPreconditions.add(message);
            showDeleteAdvice = true;
        }

        for (int i = 0;  i < p.getNumChildren();  i++)
            assertNoMultiplePhases(p.getChild(i));
    }

    private List<String> getPhaseChildren(PropertyKey node) {
        List<String> result = new ArrayList<String>();
        Prop p = ctx.getHierarchy().pget(node);
        for (int i = 0;  i < p.getNumChildren();  i++) {
            PropertyKey childKey = p.getChild(i);
            Prop child = ctx.getHierarchy().pget(childKey);
            String templateID = child.getID();
            if (templateID != null && templateID.contains("/PHASE/"))
                result.add(childKey.name());
        }
        return result;
    }

    private String enumeratePaths(String parentPath, List<String> kids) {
        String basePath = esc(parentPath) + "/";
        StringBuffer result = new StringBuffer("<ul>");
        for (String oneKid : kids)
            result.append("<li>").append(basePath).append(esc(oneKid)).append(
                "</li>");
        result.append("</ul>");
        return result.toString();
    }

    private void buildProcessCommandLine() throws Exception {
        List<String> cmdLine = new ArrayList<String>();

        cmdLine.add(RuntimeUtils.getJreExecutable());

        // add the propagated JVM args, to ensure security
        cmdLine.addAll(Arrays.asList(RuntimeUtils.getPropagatedJvmArgs()));

        // add the program arguments defined by the setup tasks
        for (Map.Entry<String, String> e : args.entrySet()) {
            cmdLine.add("-D" + e.getKey() + "=" + e.getValue());
        }

        // classpath configuration
        cmdLine.add("-cp");
        cmdLine.add(getClasspathFor(ProcessDashboard.class)
                + System.getProperty("path.separator")
                + getClasspathFor(MigrationToolIndiv.class));

        // The actual class we want to launch.
        cmdLine.add(MigrationToolIndiv.class.getName());

        this.cmdLine = cmdLine.toArray(new String[cmdLine.size()]);
    }

    private String getClasspathFor(Class clz) throws Exception {
        File jarFile = RuntimeUtils.getClasspathFile(clz);
        if (!jarFile.isFile())
            throw new MigrationException("cannotMigrate").add("dirClasspath");
        return jarFile.getAbsolutePath();
    }

    private void launchMigrationProcess() {
        try {
            System.out.println("Launching indiv migration tool with command line:");
            for (int i = 0; i < cmdLine.length; i++)
                System.out.println("\t" + cmdLine[i]);

            Runtime.getRuntime().exec(cmdLine);

            // Wait a few seconds, to ensure that the web browser has an
            // opportunity to retrieve the "please wait" web page before we
            // shut down the dashboard's web server.
            Thread.sleep(2000);

            // shut down the dashboard (skip the typical 'shutdown data export'
            // for expediency)
            System.setProperty(Settings.SYS_PROP_PREFIX
                    + "export.disableAutoExport", "true");
            ProcessDashboard dash = (ProcessDashboard) ctx;
            dash.exitProgram();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                "Unable to launch migration tool", "Migration Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private SimpleData getValue(String name) {
        String dataName = DataRepository.createDataName(projectPath, name);
        return ctx.getData().getSimpleValue(dataName);
    }

    private void putValue(String name, SaveableData value) {
        String dataName = DataRepository.createDataName(projectPath, name);
        ctx.getData().putValue(dataName, value);
    }

    private static String getRes(String key, Object... args) {
        String text = resources.getString(key);
        if (args.length > 0)
            text = MessageFormat.format(text, args);
        return text;
    }

    private static String esc(String onePath) {
        return HTMLUtils.escapeEntities(onePath);
    }

    private static final String INDIV2_ROOT = "/Indiv2Root";

}
