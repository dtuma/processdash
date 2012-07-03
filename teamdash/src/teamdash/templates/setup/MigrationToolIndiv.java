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

import java.awt.Frame;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.log.defects.DefectLog;
import net.sourceforge.processdash.log.time.PathRenamer;
import net.sourceforge.processdash.log.time.WorkingTimeLog;
import net.sourceforge.processdash.tool.bridge.impl.DashboardInstanceStrategy;
import net.sourceforge.processdash.ui.lib.ProgressDialog;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.RobustFileWriter;
import net.sourceforge.processdash.util.RuntimeUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;
import net.sourceforge.processdash.util.lock.FileConcurrencyLock;
import net.sourceforge.processdash.util.lock.LockFailureException;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class MigrationToolIndiv {

    static final String PROP_PREFIX = "migrationToolIndiv.";

    static final String TARGET_DIR = PROP_PREFIX + "targetDir";

    static final String PROJECT_PATH = PROP_PREFIX + "projectPath";

    static final String MIGRATION_NEEDED = PROP_PREFIX + "migrationNeeded";

    static final String PROCESS_ID = PROP_PREFIX + "processID";

    static final String TARGET_PROCESS_ID = PROP_PREFIX + "targetPID";

    static final String PHASE_LIST = PROP_PREFIX + "phaseList";


    public static void main(String[] args) {
        MigrationToolIndiv mti = new MigrationToolIndiv();
        try {
            mti.run();
        } catch (Exception e) {
            mti.showException(e);
        }
        System.exit(0);
    }


    private File targetDir;

    private String projectPath;

    private boolean migrationNeeded;

    private String processID;

    private String targetPID;

    private boolean conversionNeeded;

    private List<String> phases;

    private ProgressDialog progressDialog;

    private Element hier;

    private Element projectRoot;

    private File extraBackupFile;

    private WorkingTimeLog timeLog;

    private FileConcurrencyLock fileLock;

    private boolean changesWritten;


    private MigrationToolIndiv() {}

    private void run() throws Exception {
        loadData();
        createTasks();
        progressDialog.run();
        restartDashboard();
    }

    private void loadData() {
        changesWritten = false;
        targetDir = new File(System.getProperty(TARGET_DIR));
        projectPath = System.getProperty(PROJECT_PATH);
        if (!projectPath.startsWith("/"))
            projectPath = "/" + projectPath;
        migrationNeeded = Boolean.getBoolean(MIGRATION_NEEDED);
        processID = System.getProperty(PROCESS_ID);
        targetPID = System.getProperty(TARGET_PROCESS_ID);
        conversionNeeded = hasValue(targetPID) && !targetPID.equals(processID);
        phases = Arrays.asList(System.getProperty(PHASE_LIST).split("/"));

        fileLock = new FileConcurrencyLock(
                file(DashboardInstanceStrategy.LOCK_FILE_NAME));
        fileLock.setListenForLostLock(false);
    }

    private void createTasks() throws Exception {
        String operation = (migrationNeeded ? "Migration" : "Conversion");
        String action = (migrationNeeded ? "Migrating" : "Converting");
        progressDialog = new ProgressDialog((Frame) null,
                "Personal Plan " + operation + " Tool",
                action + " data, please wait...");
        progressDialog.setSize(400, progressDialog.getHeight());
        progressDialog.setCompletionMessage(operation
                + " Complete. Press OK to restart the Process Dashboard.");
        progressDialog.setCloseText("OK");

        progressDialog.addTask(new WaitForLockTask());

        List<MigrationMethod> migrationSteps = new ArrayList<MigrationMethod>();
        for (Method method : MigrationToolIndiv.class.getDeclaredMethods()) {
            if (method.getAnnotation(MigrationTask.class) != null)
                migrationSteps.add(new MigrationMethod(method));
        }
        Collections.sort(migrationSteps);
        for (Runnable r : migrationSteps)
            progressDialog.addTask(r);
    }



    /**
     * Document a step in the migration process
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface MigrationTask {
        String value();
    }


    /**
     * Wait for the Process Dashboard to shut down, then lock the target
     * directory.
     */
    private class WaitForLockTask implements ProgressDialog.Task {

        private ChangeListener dialog;

        private int countdown = 100;

        public void addChangeListener(ChangeListener l) {
            dialog = l;
        }

        public String getMessage() {
            return "Waiting for the Process Dashboard to exit (" + countdown
                    + " sec)";
        }

        public int getPercentComplete() {
            return 100 - countdown;
        }

        public void run() {
            ChangeEvent ce = new ChangeEvent(this);
            while (countdown > 0) {
                // try to acquire a lock on the directory. If successful,
                // return from this method immediately.
                try {
                    fileLock.acquireLock("the Team Project Migration Tool");
                    return;
                } catch (LockFailureException e) {
                }

                // we couldn't acquire a lock. Wait for one second, update
                // the progess dialog, then try again.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                }
                countdown--;
                dialog.stateChanged(ce);
            }

            // if time runs out and we still have not acquired a lock,
            // abort the migration and terminate the JVM.
            System.exit(0);
        }

    }



    @MigrationTask("000 Backing up data before migration")
    protected void makeExtraBackup() throws IOException {
        if (fileLock == null || !fileLock.isLocked())
            System.exit(0);

        redirectOutput();

        File backupDir = file("backup");

        String[] backupFilenames = backupDir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("pdash-") && name.endsWith(".zip");
            }
        });
        Arrays.sort(backupFilenames);
        String mostRecentFilename = backupFilenames[backupFilenames.length - 1];
        File mostRecentBackupFile = new File(backupDir, mostRecentFilename);

        DateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss");
        String extraBackupFilename = "before-migration-"
                + dateFmt.format(new Date()) + ".zip";
        extraBackupFile = new File(backupDir, extraBackupFilename);

        FileUtils.copyFile(mostRecentBackupFile, extraBackupFile);
    }

    private void redirectOutput() {
        try {
            PrintStream out = new PrintStream(new FileOutputStream(
                    file("log.txt"), true));
            System.setOut(out);
            System.setErr(out);
        } catch (Exception e) {
            System.out.println("Could not redirect output");
            e.printStackTrace();
        }

        System.out.println();
        System.out.println(HORIZ_RULE);
        System.out.println("Migration tool starting for project " + projectPath);
    }

    private void restoreFromBackup() throws Exception {
        ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(
                new FileInputStream(extraBackupFile)));

        Exception exceptionEncountered = null;
        ZipEntry e;
        while ((e = zipIn.getNextEntry()) != null) {
            String name = e.getName();
            if (name.equals("log.txt") || name.indexOf('/') != -1
                    || name.indexOf('\\') != -1)
                continue;
            try {
                File f = file(name);
                FileUtils.copyFile(zipIn, f);
                f.setLastModified(e.getTime());
            } catch (Exception exc) {
                // if we encounter an exception on a particular file, try
                // to continue and restore the other files.  But record the
                // exception so we can pass it along.
                exceptionEncountered = exc;
            }
        }
        zipIn.close();

        if (exceptionEncountered != null)
            throw exceptionEncountered;
    }



    @MigrationTask("001 Loading project hierarchy")
    protected void loadHierarchy() throws IOException, SAXException {
        hier = XMLUtils.parse(new FileInputStream(file("state")))
                .getDocumentElement();
        projectRoot = findElement(projectPath);
        recordOriginalPaths(projectRoot, projectPath);
    }

    private void recordOriginalPaths(Element node, String path) {
        node.setAttribute(ORIGINAL_PATH, path);
        for (Element child : XMLUtils.getChildElements(node)) {
            if (NODE_TAG.equals(child.getTagName())) {
                String name = child.getAttribute(NAME_ATTR);
                recordOriginalPaths(child, path + "/" + name);
            }
        }
    }



    @MigrationTask("002 Consolidating time log data")
    protected void cleanupTimeLog() throws IOException {
        changesWritten = true;
        timeLog = new WorkingTimeLog(targetDir);
    }



    @MigrationTask("010 Streamlining project hierarchy")
    protected void streamlineHierarchy() {
        String parentPath = DataRepository.chopPath(projectPath);
        streamlineHierarchy(projectRoot, parentPath);

        // remove the defect log from the project root
        projectRoot.removeAttribute(DEFECTLOG_ATTR);
    }

    private String streamlineHierarchy(Element node, String parentPath) {
        if (migrationNeeded)
            migrateTemplateID(node);
        if (conversionNeeded)
            convertTemplateID(node);

        String myOrigName = node.getAttribute(NAME_ATTR);
        String myPath = parentPath + "/" + myOrigName;

        String myNewName = changeNodeName(node);
        if (myNewName != null) {
            String myNewPath = parentPath + "/" + myNewName;
            registerPathRename(myPath, myNewPath);
            myPath = myNewPath;
        }

        String phase = getPhaseStubName(node);
        if (phase != null) {
            node.getParentNode().removeChild(node);
            registerPathRename(myPath, parentPath);
            return phase;
        }

        for (Element child : XMLUtils.getChildElements(node)) {
            String childPhase = streamlineHierarchy(child, myPath);
            if (childPhase != null)
                phase = childPhase;
        }

        if (phase != null)
            node.setAttribute(EFF_PHASE, phase);

        return null;
    }

    private void migrateTemplateID(Element node) {
        String templateID = node.getAttribute(TEMPLATE_ATTR);
        if (!hasValue(templateID) || !templateID.startsWith(processID + "/"))
            return;

        String newTemplateID = null;

        // update the root node
        if (templateID.endsWith("/IndivRoot"))
            newTemplateID = processID + "/Indiv2Root";

        // update read only component nodes
        else if (templateID.endsWith("/IndivReadOnlyNode"))
            newTemplateID = processID + "/Indiv2ReadOnlyNode";

        // update various nodes for simple tasks
        else if (templateID.endsWith("/IndivFullNode")
                || templateID.endsWith("/IndivEmptyNode")
                || templateID.contains("/IndivTask/"))
            newTemplateID = processID + "/Indiv2Task";

        if (newTemplateID != null)
            node.setAttribute(TEMPLATE_ATTR, newTemplateID);
    }

    private void convertTemplateID(Element node) {
        String templateID = node.getAttribute(TEMPLATE_ATTR);
        if (templateID != null && templateID.startsWith(processID + "/")) {
            String templateSuffix = templateID.substring(processID.length());
            String newTemplateID = targetPID + templateSuffix;
            node.setAttribute(TEMPLATE_ATTR, newTemplateID);
        }
    }

    private String changeNodeName(Element node) {
        if (!migrationNeeded)
            return null;

        String origName = node.getAttribute(NAME_ATTR);

        String templateID = node.getAttribute(TEMPLATE_ATTR);
        if (!hasValue(templateID))
            return null;

        // don't change the name of the root project node.
        if (templateID.endsWith("/Indiv2Root"))
            return null;

        // the new and old styles both use the same naming strategy for
        // read-only nodes, so we do not need to perform any renames.
        if (templateID.endsWith("ReadOnlyNode"))
            return null;

        // children of the project root must keep their "Task" suffix
        if (node.getParentNode() == projectRoot)
            return null;

        // possibly trim the "Task" suffix off regular and PSP tasks.
        if (templateID.endsWith("/Indiv2Task") || templateID.startsWith("PSP")) {
            if (origName.endsWith(" Task")) {
                String newName = origName.substring(0, origName.length() - 5);
                node.setAttribute(NAME_ATTR, newName);
                return newName;
            }
        }

        // no rename performed
        return null;
    }

    private void registerPathRename(String oldPath, String newPath) {
        timeLog.addModification(PathRenamer.getRenameModification(oldPath,
            newPath));
    }

    private String getPhaseStubName(Element node) {
        String templateID = node.getAttribute(TEMPLATE_ATTR);
        if (templateID != null && templateID.contains(PHASE_ID_STR))
            return node.getAttribute(NAME_ATTR);
        else
            return null;
    }



    @MigrationTask("020 Updating project metrics")
    protected void tweakData() throws IOException {
        tweakData(projectRoot, projectPath);
    }

    private void tweakData(Element node, String path) throws IOException {
        String templateID = node.getAttribute(TEMPLATE_ATTR);
        String pidPrefix = (conversionNeeded ? targetPID : processID) + "/";
        if (templateID != null && templateID.startsWith(pidPrefix))
            tweakDataFile(node, path);

        for (Element child : XMLUtils.getChildElements(node)) {
            if (NODE_TAG.equals(child.getTagName())) {
                String name = child.getAttribute(NAME_ATTR);
                tweakData(child, path + "/" + name);
            }
        }
    }

    private void tweakDataFile(Element node, String path) throws IOException {
        String dataFileName = node.getAttribute(DATAFILE_ATTR);
        if (!hasValue(dataFileName))
            return;
        File dataFile = file(dataFileName);

        List<StringBuffer> data = readDataFile(dataFile);
        int origHashCode = data.toString().hashCode();

        changeIncludeLine(data);
        if (migrationNeeded) {
            namespaceDefectData(data);
            filterPhaseData(node, data);
        }
        if (conversionNeeded)
            reNamespaceData(data);
        if (migrationNeeded)
            setNodesAndLeaves(node, data);

        int finalHashCode = data.toString().hashCode();
        if (origHashCode != finalHashCode)
            saveDataFile(dataFile, data);
    }


    private List<StringBuffer> readDataFile(File dataFile) throws IOException {
        List<StringBuffer> result = new ArrayList<StringBuffer>();
        BufferedReader in = new BufferedReader(new FileReader(dataFile));
        String line;
        while ((line = in.readLine()) != null)
            result.add(new StringBuffer(line));
        in.close();
        return result;
    }

    private void saveDataFile(File dataFile, List<StringBuffer> data)
            throws IOException {
        BufferedWriter out = new BufferedWriter(new RobustFileWriter(dataFile));
        for (StringBuffer line : data) {
            if (line.length() > 0) {
                out.write(line.toString());
                out.newLine();
            }
        }
        out.close();
    }

    private void changeIncludeLine(List<StringBuffer> data) {
        StringBuffer includeLine = data.get(0);
        for (int i = 0; i < INCLUDE_CHANGES.length; i++) {
            StringUtils.findAndReplace(includeLine,
                INCLUDE_CHANGES[i][0], INCLUDE_CHANGES[i][1]);
        }
        if (conversionNeeded) {
            String oldIncludeStr = "#include <" + processID + "/";
            String newIncludeStr = "#include <" + targetPID + "/";
            StringUtils.findAndReplace(includeLine, oldIncludeStr,
                newIncludeStr);
        }
    }
    private static final String[][] INCLUDE_CHANGES = {
        {"/indiv_root_dataFile.txt", "/indiv2_root_dataFile.txt"},
        {"/indiv_full_dataFile.txt", "/indiv2_empty_dataFile.txt" },
        {"/indiv_empty_dataFile.txt", "/indiv2_empty_dataFile.txt" }
    };

    private void namespaceDefectData(List<StringBuffer> data) {
        for (int i = 1;  i < data.size();  i++) {
            StringBuffer line = data.get(i);
            if (isDefectCountLine(line))
                line.insert(0, processID + " /");
        }
    }

    private boolean isDefectCountLine(StringBuffer line) {
        int pos = line.indexOf(DEFECT_INJ_STR);
        if (pos == -1)
            pos = line.indexOf(DEFECT_REM_STR);

        return (pos != -1 && phases.contains(line.substring(0, pos)));
    }

    private void filterPhaseData(Element node, List<StringBuffer> data) {
        String phase = node.getAttribute(EFF_PHASE);

        for (int i = 1;  i < data.size();  i++) {
            StringBuffer line = data.get(i);
            String linePhase = getPhaseOf(line);
            if (linePhase == null)
                continue;
            else if (linePhase.equals(phase))
                line.delete(0, phase.length()+1);
            else
                line.insert(0, "= removed by migration tool = ");
        }

        if (hasValue(phase)) {
            String effPhaseLine = processID + " /Effective_Phase="
                    + StringData.saveString(phase);
            data.add(new StringBuffer(effPhaseLine));
            effPhaseLine = processID + " /Effective_Phase_Last_Synced_Val="
                    + StringData.saveString(phase);
            data.add(new StringBuffer(effPhaseLine));
        }
    }

//    private Set<String> findActualPhasesPresent(List<StringBuffer> data) {
//        Set<String> result = new HashSet<String>();
//        for (StringBuffer line : data) {
//            if (line.indexOf("/node=") != -1)
//                result.add(getPhaseOf(line));
//        }
//        result.remove(null);
//        return result;
//    }

    private String getPhaseOf(StringBuffer line) {
        int slashPos = line.indexOf("/");
        if (slashPos == -1)
            return null;
        String result = line.substring(0, slashPos);
        if (phases.contains(result))
            return result;
        else
            return null;
    }

    private void reNamespaceData(List<StringBuffer> data) {
        for (int i = 1;  i < data.size();  i++) {
            StringBuffer line = data.get(i);
            if (StringUtils.startsWith(line, processID + " /")) {
                line.replace(0, processID.length(), targetPID);
            }
        }
    }

    private void setNodesAndLeaves(Element node, List<StringBuffer> data) {
        for (StringBuffer line : data) {
            if (line.indexOf("node=") != -1 || line.indexOf("leaf=") != -1)
                line.setLength(0);
        }
        if (XMLUtils.getChildElements(node).isEmpty())
            data.add(new StringBuffer("leaf==TAG"));
        else
            data.add(new StringBuffer("leaf=null"));
    }



    @MigrationTask("999 Writing data and cleaning up")
    protected void finish() throws IOException {
        saveHierarchy();
        timeLog.saveData();
        fileLock.releaseLock();
    }

    private void saveHierarchy() throws IOException {
        discardWorkingAttrs(projectRoot);
        writeHierarchyFile();
    }

    private void discardWorkingAttrs(Element node) {
        for (int i = 0; i < WORKING_ATTRS.length; i++) {
            node.removeAttribute(WORKING_ATTRS[i]);
        }
        for (Element child : XMLUtils.getChildElements(node)) {
            discardWorkingAttrs(child);
        }
    }

    private void writeHierarchyFile() throws IOException {
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                new RobustFileOutputStream(file("state")), "UTF-8"));
        out.write(XMLUtils.getAsText(hier));
        out.close();
    }



    private void restartDashboard() {
        System.out.println("Migration tool complete.");

        List<String> cmd = new ArrayList<String>();
        cmd.add(RuntimeUtils.getJreExecutable());
        cmd.add("-Xmx800m");
        cmd.add("-jar");
        cmd.add(RuntimeUtils.getClasspathFile(ProcessDashboard.class).getPath());
        String[] cmdLine = cmd.toArray(new String[cmd.size()]);

        try {
            Runtime.getRuntime().exec(cmdLine, null, targetDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /*
     * Convenience routines
     */

    /** Find a hierarchy element, by its full path */
    private Element findElement(String path) {
        if (path.startsWith("/"))
            path = path.substring(1);

        Element result = hier;
        for (String childName : path.split("/")) {
            Element child = findChildNode(result, childName);
            if (child == null)
                return null;
            else
                result = child;
        }
        return result;
    }

    private Element findChildNode(Element parentNode, String childName) {
        for (Element child : XMLUtils.getChildElements(parentNode)) {
            if (childName.equals(child.getAttribute("name")))
                return child;
        }
        return null;
    }


    /** Retrieve a handle to a file in the target dir */
    private File file(String name) {
        return new File(targetDir, name);
    }

    private boolean hasValue(String s) {
        return (s != null && s.length() > 0);
    }


    private class MigrationMethod implements ProgressDialog.Task,
            Comparable<MigrationMethod> {

        private Method method;

        private String ordinal;

        private String message;


        private MigrationMethod(Method method) throws Exception {
            this.method = method;
            String desc = method.getAnnotation(MigrationTask.class).value();
            this.ordinal = desc.substring(0, 3);
            this.message = desc.substring(4);
        }

        public void addChangeListener(ChangeListener l) {}

        public String getMessage() {
            return message;
        }

        public int getPercentComplete() {
            return 0;
        }

        public void run() {
            try {
                System.out.println("\t" + message);
                method.invoke(MigrationToolIndiv.this, new Object[0]);
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
                showException(e);
                System.exit(1);
            }
        }

        public int compareTo(MigrationMethod that) {
            return this.ordinal.compareTo(that.ordinal);
        }

    }

    private void showException(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        System.err.println("Unexpected Exception Encountered:");
        e.printStackTrace();

        String dataChangeMessage = maybeRestoreData(pw);
        Object msg = new Object[] {
                "A serious, unexpected problem was encountered during the " +
                "migration operation. More information is available below.",
                new JScrollPane(new JTextArea(sw.toString())),
                dataChangeMessage.split("\n")
         };
         JOptionPane.showMessageDialog(null, msg, "Encountered exception",
                    JOptionPane.PLAIN_MESSAGE);
    }

    private String maybeRestoreData(PrintWriter stackDump) {
        if (changesWritten == false) {
            System.err.println("Data was not modified.");
            return "Your data has not been modified, so you can continue "
                    + "using the Process Dashboard normally.";
        }

        try {
            restoreFromBackup();
            System.err.println("Data has been restored");
            return "Your data has been restored to the original values it had "
                    + "before the migration began, so you can continue using\n"
                    + "the Process Dashboard normally.";


        } catch (Exception restoreException) {
            stackDump.write(HORIZ_RULE + "\n");
            stackDump.write("Data restoration failed due to:\n");
            restoreException.printStackTrace(stackDump);
            System.err.println("Data restoration failed:");
            restoreException.printStackTrace();
            return "The migration tool attempted to restore your data "
                    + "to the original values it had before the migration\n"
                    + "began, but the restoration failed.  Your data is in "
                    + "a dangerous state, and needs to be manually repaired.\n"
                    + "Please contact the Process Dashboard development team "
                    + "immediately for assistance.";
        }
    }

    private static final String NODE_TAG = DashHierarchy.NODE_NODE_NAME;

    private static final String NAME_ATTR = DashHierarchy.NAME_ATTR;

    private static final String TEMPLATE_ATTR = DashHierarchy.TEMPLATE_ATTR;

    private static final String DATAFILE_ATTR = DashHierarchy.DATAFILE_ATTR;

    private static final String DEFECTLOG_ATTR = DashHierarchy.DEFECTLOG_ATTR;

    private static final String PHASE_ID_STR = "/PHASE/";

    private static final String DEFECT_INJ_STR = DefectLog.DEF_INJ_SUFFIX + "=";

    private static final String DEFECT_REM_STR = DefectLog.DEF_REM_SUFFIX + "=";

    private static final String HORIZ_RULE = "------------------------------------------------";


    private static final String ORIGINAL_PATH = "_originalPath";

    private static final String EFF_PHASE = "_effectivePhase";

    private static final String[] WORKING_ATTRS = { ORIGINAL_PATH, EFF_PHASE };
}
