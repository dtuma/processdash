// Copyright (C) 2005-2022 Tuma Solutions, LLC
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataNameFilter;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.repository.InvalidDatafileFormat;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.tool.export.DataImporter;
import net.sourceforge.processdash.tool.export.impl.ArchiveMetricsFileExporter;
import net.sourceforge.processdash.tool.export.impl.ExportFileStream;
import net.sourceforge.processdash.tool.export.impl.TextMetricsFileExporter;
import net.sourceforge.processdash.ui.lib.ProgressDialog;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

public class ExportManager extends AbstractManager {

    public static final String EXPORT_DATANAME = DataImporter.EXPORT_DATANAME;
    public static final String EXPORT_TIMES_SETTING = "export.timesOfDay";

    public static final int EXPORT_STARTING = 1;
    public static final int EXPORT_FINISHED = 2;
    public static final String EXPORT_ALL_PATH = "*";

    private static final String EXPORT_INSTRUCTIONS_SUFFIX = "/Instructions";
    private static final String EXPORT_DISABLED_SUFFIX = "/Disabled";
    private static final String EXPORT_URL_SUFFIX = "/Server_URL";
    private static final String EXPORT_TIMESTAMP_SUFFIX = "/Last_Export_Timestamp";
    private static final String DATANAME_ATTR = "_Instruction_Data_Name";

    private static ExportManager INSTANCE = null;

    private static Logger logger = Logger.getLogger(ExportManager.class
            .getName());

    public synchronized static ExportManager getInstance() {
        if (INSTANCE == null)
            INSTANCE = new ExportManager();
        return INSTANCE;
    }

    public static void init(DataRepository dataRepository,
            ProcessDashboard dashboard) {
        getInstance().setup(dataRepository, dashboard);
    }

    private ProcessDashboard dashboard;

    private ExportManager() {
        super();
        initialize();

        if (logger.isLoggable(Level.CONFIG))
            logger.config("ExportManager contents:\n" + getDebugContents());
    }

    private void setup(DataRepository data, ProcessDashboard dashboard) {
        this.dashboard = dashboard;
        setData(data, false);

        storeCapabilityData(data);
    }

    public ProcessDashboard getProcessDashboard() {
        return dashboard;
    }

    protected String getTextSettingName() {
        return "export.data";
    }

    protected String getXmlSettingName() {
        return "export.instructions";
    }

    protected void parseXmlInstruction(Element element) {
        if (ExportMetricsFileInstruction.matches(element))
            doAddInstruction(new ExportMetricsFileInstruction(element));
    }

    protected void parseTextInstruction(String left, String right) {
        String file = Settings.translateFile(left);
        Vector paths = new Vector(Arrays.asList(right.split(";")));
        doAddInstruction(new ExportMetricsFileInstruction(file, paths));
    }

    protected void handleAddedInstruction(AbstractInstruction instr) {
        if (instr.isEnabled()) {
            // use the visitor pattern to invoke the correct handler method
            instr.dispatch(instructionAdder);
        }
    }

    public void runOneTimeInstruction(AbstractInstruction instr) {
        ExportJanitor janitor = new ExportJanitor(data);
        janitor.startOneTimeExportOperation();

        handleAddedInstruction(instr);

        janitor.finishOneTimeExportOperation();
    }

    protected void handleRemovedInstruction(AbstractInstruction instr) {
        if (instr.isEnabled()) {
            // use the visitor pattern to invoke the correct handler method
            instr.dispatch(instructionRemover);
        }
    }

    private class InstructionAdder implements ExportInstructionDispatcher {

        public Object dispatch(ExportMetricsFileInstruction instr) {
            Runnable executor = getExporter(instr);
            executor.run();
            return null;
        }

    }

    private InstructionAdder instructionAdder = new InstructionAdder();

    private class InstructionRemover implements ExportInstructionDispatcher {

        public Object dispatch(ExportMetricsFileInstruction instr) {
            (new File(instr.getFile())).delete();
            return null;
        }

    }

    private InstructionRemover instructionRemover = new InstructionRemover();

    private class InstructionUpdater implements ExportInstructionDispatcher {

        public Object dispatch(ExportMetricsFileInstruction instr) {
            String dataName = instr.getAttribute(DATANAME_ATTR);
            if (!StringUtils.hasValue(dataName)) return null;

            String serverUrl = instr.getServerUrl();
            if (!StringUtils.hasValue(serverUrl)) return null;

            String urlDataname = dataName + EXPORT_URL_SUFFIX;
            data.userPutValue(urlDataname, StringData.create(serverUrl));

            return null;
        }

    }

    private InstructionUpdater instructionUpdater = new InstructionUpdater();

    private class InstructionExecutorFactory implements
            ExportInstructionDispatcher {

        public Object dispatch(ExportMetricsFileInstruction instr) {
            String dest = instr.getFile();
            String url = instr.getServerUrl();
            Vector paths = instr.getPaths();

            File destFile = new File(dest);
            String targetPath = ExportFileStream.getExportTargetPath(destFile,
                url);
            if (dest.toLowerCase().endsWith(".txt"))
                return new ExportTask(targetPath, new TextMetricsFileExporter(
                        dashboard, destFile, paths));
            else
                return new ExportTask(targetPath, new ArchiveMetricsFileExporter(
                        dashboard, targetPath, paths, instr), instr);
        }

    }

    private InstructionExecutorFactory instructionExecutorFactory = new InstructionExecutorFactory();

    public Runnable getExporter(AbstractInstruction instr) {
        if (instr == null)
            return null;
        else
            return (Runnable) instr.dispatch(instructionExecutorFactory);
    }

    private class ExportTargetPathFactory implements ExportInstructionDispatcher {
        public Object dispatch(ExportMetricsFileInstruction instr) {
            String dest = instr.getFile();
            String url = instr.getServerUrl();
            File destFile = new File(dest);
            return ExportFileStream.getExportTargetPath(destFile, url);
        }
    }

    private ExportInstructionDispatcher exportTargetPathFactory = new ExportTargetPathFactory();

    public String getExportTargetPath(AbstractInstruction instr) {
        if (instr == null)
            return null;
        else
            return (String) instr.dispatch(exportTargetPathFactory);
    }

    public CompletionStatus exportDataForPrefix(String prefix) {
        String dataName = DataRepository.createDataName(prefix,
            ExportManager.EXPORT_DATANAME);
        AbstractInstruction instr = getExportInstructionFromData(dataName, true);
        Runnable task = getExporter(instr);
        if (task == null)
            return new CompletionStatus(CompletionStatus.NO_WORK_NEEDED,
                    null, null);

        task.run();

        if (task instanceof CompletionStatus.Capable)
            return ((CompletionStatus.Capable) task).getCompletionStatus();

        return new CompletionStatus(CompletionStatus.SUCCESS, null, null);
    }

    public void exportAll(Object window, DashboardContext parent) {
        // start with the export instructions registered with this manager
        List<AbstractInstruction> tasks = new LinkedList(instructions);
        // Look for data export instructions in the data repository.
        tasks.addAll(getExportInstructionsFromData());

        // nothing to do?
        if (tasks.isEmpty())
            return;

        ProgressDialog p = null;
        if (window != null)
            p = ProgressDialog.create(window, resource
                    .getString("ExportAutoExporting"), resource
                    .getString("ExportExportingDataDots"));

        // start the export operation
        final ExportJanitor janitor = new ExportJanitor(data);
        fireEvent(EXPORT_STARTING, EXPORT_ALL_PATH);

        // make a list of the targetPaths from the current instructions
        final List currentTargetPaths = new ArrayList<String>();
        for (AbstractInstruction instr : tasks) {
            currentTargetPaths.add(getExportTargetPath(instr));
        }

        // clean obsolete historically exported files
        if (p != null) {
            p.addTask(new Runnable() { public void run() {
                janitor.cleanHistoricalFiles(currentTargetPaths); }});
        } else {
            janitor.cleanHistoricalFiles(currentTargetPaths);
        }

        // export each of the files in the current list
        for (AbstractInstruction instr : tasks) {
            Runnable exporter = getExporter(instr);

            if (instr.isEnabled()) {
                if (p != null)
                    p.addTask(exporter);
                else
                    exporter.run();
            }
        }

        if (p != null) {
            p.run();
        }

        System.out.println("Completed user-scheduled data export.");
        Runtime.getRuntime().gc();
        fireEvent(EXPORT_FINISHED, EXPORT_ALL_PATH);
    }

    private Collection getExportInstructionsFromData() {
        Collection result = new LinkedList();

        DataRepository data = dashboard.getData();
        Object hints = new DataNameFilter.PrefixLocal() {
            public boolean acceptPrefixLocalName(String p, String localName) {
                return localName.endsWith(EXPORT_DATANAME);
            }};
        for (Iterator iter = data.getKeys(null, hints); iter.hasNext();) {
            String name = (String) iter.next();
            AbstractInstruction instr = getExportInstructionFromData(name, true);
            if (instr != null)
                result.add(instr);
        }

        return result;
    }

    private AbstractInstruction getExportInstructionFromData(String name,
            boolean retrieveXmlInstructionData) {
        if (!name.endsWith("/"+EXPORT_DATANAME))
            return null;
        SimpleData dataVal = data.getSimpleValue(name);
        if (dataVal == null || !dataVal.test())
            return null;
        String filename = Settings.translateFile(dataVal.format());

        String path = name.substring(0,
                name.length() - EXPORT_DATANAME.length() - 1);
        Vector filter = new Vector();
        filter.add(path);

        ExportMetricsFileInstruction instr = new ExportMetricsFileInstruction(
                filename, filter);
        instr.setAttribute(DATANAME_ATTR, name);

        String instrDataname = name + EXPORT_INSTRUCTIONS_SUFFIX;
        SimpleData instrVal = data.getSimpleValue(instrDataname);
        if (retrieveXmlInstructionData && instrVal != null && instrVal.test())
            addXmlDataToInstruction(instr, instrVal.format());

        String disableDataname = name + EXPORT_DISABLED_SUFFIX;
        SimpleData disableVal = data.getSimpleValue(disableDataname);
        if (disableVal != null && disableVal.test())
            instr.setEnabled(false);

        String urlDataname = name + EXPORT_URL_SUFFIX;
        SimpleData urlVal = data.getSimpleValue(urlDataname);
        if (urlVal != null && urlVal.test())
            instr.setServerUrl(urlVal.format());

        return instr;
    }

    private void addXmlDataToInstruction(AbstractInstruction instr,
            String instrVal) {
        String xml = instrVal;
        if (instrVal.startsWith("file:")) {
            try {
                String uri = instrVal.substring(5);
                xml = dashboard.getWebServer().getRequestAsString(uri);
            } catch (Exception e) {
                logger.log(Level.WARNING,
                        "Couldn't open XML instructions file", e);
                return;
            }
        }

        try {
            Element elem = XMLUtils.parse(xml).getDocumentElement();
            instr.mergeXML(elem);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Couldn't understand XML instruction", e);
        }
    }

    /** @since 1.15.3 */
    private void maybeRecordExportTimestamp(AbstractInstruction instr,
            Runnable target) {
        // see if this export instruction originated from a data element
        String dataName = instr.getAttribute(DATANAME_ATTR);
        if (dataName == null)
            return;

        // check to see if this instruction completed successfully
        if (target instanceof CompletionStatus.Capable) {
            CompletionStatus.Capable c = (CompletionStatus.Capable) target;
            CompletionStatus status = c.getCompletionStatus();
            if (status == null
                    || !CompletionStatus.SUCCESS.equals(status.getStatus()))
                return;
        }

        // record the timestamp when data was last exported for this instr
        String exportTimestampDataName = dataName + EXPORT_TIMESTAMP_SUFFIX;
        data.putValue(exportTimestampDataName, new DateData());
    }

    public static String exportedScheduleDataPrefix(String owner, String scheduleName) {
        return EVTaskList.MAIN_DATA_PREFIX
            + exportedScheduleName(owner, scheduleName);
    }

    private static String exportedScheduleName(String owner, String scheduleName) {
        owner = (owner == null ? "?????" : safeName(owner));
        String ownerSuffix = "";
        if (owner.length() > 0)
            ownerSuffix = " (" + owner + ")";
        String name = safeName(scheduleName) + ownerSuffix;
        return name;
    }

    private static String safeName(String n) {
        return n.replace('/', '_').replace(',', '_');
    }

    private static void storeCapabilityData(DataRepository data) {
        try {
            Map capabilities = Collections.singletonMap(
                    "Supports_pdash_format", ImmutableDoubleData.TRUE);
            data.mountPhantomData("//Export_Manager", capabilities);
        } catch (InvalidDatafileFormat e) {
            logger.log(Level.WARNING, "Unexpected error", e);
        }
    }

    /** @since 2.0.9 */
    public boolean hasEnabledExportTask(String path) {
        AbstractInstruction instr = getExportInstructionFromData(path + "/"
                + EXPORT_DATANAME, false);
        return (instr != null && instr.isEnabled());
    }


    public static class BGTask implements Runnable {

        private DashboardContext context;

        public void setDashboardContext(DashboardContext context) {
            this.context = context;
        }

        public void run() {
            try {
                ExportManager.getInstance().exportAll(null, context);
            } catch (Exception e) {
                logger.log(Level.SEVERE,
                        "Encountered exception when performing auto export", e);
            }
        }

    }



    private class ExportTask implements Runnable, CompletionStatus.Capable, Cancellable {

        private String path;

        private Runnable target;

        private AbstractInstruction instr;

        private int origHashCode;

        public ExportTask(String path, Runnable target) {
            this(path, target, null);
        }

        public ExportTask(String path, Runnable target,
                AbstractInstruction instr) {
            this.path = path;
            this.target = target;
            this.instr = instr;
            if (instr != null)
                this.origHashCode = instr.hashCode();
        }

        public void run() {
            exportTaskStarting();
            target.run();
            exportTaskFinished();
            maybeUpdateInstruction();

            ExportJanitor.recordSuccessfulFileExport(data, path);
        }

        public void tryCancel() {
            if (target instanceof Cancellable)
                ((Cancellable) target).tryCancel();
        }

        public CompletionStatus getCompletionStatus() {
            return ((CompletionStatus.Capable) target).getCompletionStatus();
        }

        private void exportTaskStarting() {
            fireEvent(EXPORT_STARTING, path);

            ExportTask task = putTask();
            if (task == this)
                return;

            long waitUntil = System.currentTimeMillis() + 5000;
            task.tryCancel();
            do {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {}
                if (putTask() == this)
                    return;
            } while (System.currentTimeMillis() < waitUntil);

            EXPORT_TASKS_IN_PROGRESS.put(path, this);
        }

        private ExportTask putTask() {
            synchronized (EXPORT_TASKS_IN_PROGRESS) {
                ExportTask current = (ExportTask) EXPORT_TASKS_IN_PROGRESS
                        .get(path);
                if (current == null) {
                    EXPORT_TASKS_IN_PROGRESS.put(path, this);
                    return this;
                } else {
                    return current;
                }
            }
        }

        private void exportTaskFinished() {
            boolean isCurrentTask;
            synchronized (EXPORT_TASKS_IN_PROGRESS) {
                Object current = EXPORT_TASKS_IN_PROGRESS.get(path);
                isCurrentTask = (current == this);
                if (isCurrentTask)
                    EXPORT_TASKS_IN_PROGRESS.remove(path);
            }
            if (isCurrentTask) {
                maybeRecordExportTimestamp(instr, target);
                fireEvent(EXPORT_FINISHED, path);
            }
        }

        private void maybeUpdateInstruction() {
            if (instr == null)
                return;

            if (instr instanceof CompletionStatus.Listener
                    && target instanceof CompletionStatus.Capable) {
                CompletionStatus.Listener l = (CompletionStatus.Listener) instr;
                CompletionStatus.Capable t = (CompletionStatus.Capable) target;
                l.completionStatusReady(new CompletionStatus.Event(t));
            }

            int finalHashCode = instr.hashCode();
            if (finalHashCode != origHashCode)
                instr.dispatch(instructionUpdater);
        }
    }

    private static final Map EXPORT_TASKS_IN_PROGRESS = Collections
            .synchronizedMap(new HashMap());

}
