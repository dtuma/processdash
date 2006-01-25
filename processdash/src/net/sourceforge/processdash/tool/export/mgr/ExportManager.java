// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2005 Software Process Dashboard Initiative
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListXML;
import net.sourceforge.processdash.tool.export.DataImporter;
import net.sourceforge.processdash.tool.export.impl.ArchiveMetricsFileExporter;
import net.sourceforge.processdash.tool.export.impl.TextMetricsFileExporter;
import net.sourceforge.processdash.ui.lib.ProgressDialog;

import org.w3c.dom.Element;

public class ExportManager extends AbstractManager {

    public static final String EXPORT_DATANAME = DataImporter.EXPORT_DATANAME;

    private static ExportManager INSTANCE = null;

    public static ExportManager getInstance() {
        return INSTANCE;
    }

    public static void init(DataRepository dataRepository,
            ProcessDashboard dashboard) {
        INSTANCE = new ExportManager(dataRepository, dashboard);
    }

    private ProcessDashboard dashboard;

    private boolean initializing;

    private ExportManager(DataRepository data, ProcessDashboard dashboard) {
        super(data);
        this.dashboard = dashboard;
        initializing = true;
        initialize();
        initializing = false;

        // System.out.println("ExportManager contents:");
        // for (Iterator iter = instructions.iterator(); iter.hasNext();) {
        // AbstractInstruction instr = (AbstractInstruction) iter.next();
        // System.out.println(instr);
        // }
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

    public void handleAddedInstruction(AbstractInstruction instr) {
        if (!initializing && instr.isEnabled()) {
            // use the visitor pattern to invoke the correct handler method
            instr.dispatch(instructionAdder);
        }
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

    private class InstructionExecutorFactory implements
            ExportInstructionDispatcher {

        public Object dispatch(ExportMetricsFileInstruction instr) {
            String dest = instr.getFile();
            Vector paths = instr.getPaths();

            if (dest.toLowerCase().endsWith(".txt"))
                return new TextMetricsFileExporter(dashboard, new File(dest),
                        paths);
            else
                return new ArchiveMetricsFileExporter(dashboard,
                        new File(dest), paths);
        }

    }

    private InstructionExecutorFactory instructionExecutorFactory = new InstructionExecutorFactory();

    public Runnable getExporter(AbstractInstruction instr) {
        return (Runnable) instr.dispatch(instructionExecutorFactory);
    }

    public void exportAll(Object window, DashboardContext parent) {
        ProgressDialog p = ProgressDialog.create(window, resource
                .getString("ExportAutoExporting"), resource
                .getString("ExportExportingDataDots"));

        // start with the export instructions registered with this manager
        List tasks = new LinkedList(instructions);
        // Look for data export instructions in the data repository.
        tasks.addAll(getExportInstructionsFromData());

        // nothing to do?
        if (tasks.isEmpty())
            return;

        for (Iterator iter = tasks.iterator(); iter.hasNext();) {
            AbstractInstruction instr = (AbstractInstruction) iter.next();
            Runnable exporter = getExporter(instr);
            p.addTask(exporter);
        }

        p.run();
        System.out.println("Completed user-scheduled data export.");
    }

    private Collection getExportInstructionsFromData() {
        Collection result = new LinkedList();

        DataRepository data = dashboard.getData();
        for (Iterator iter = data.getKeys(); iter.hasNext();) {
            String name = (String) iter.next();
            int pos = name.indexOf(EXPORT_DATANAME);
            if (pos < 1)
                continue;
            SimpleData dataVal = data.getSimpleValue(name);
            if (dataVal == null || !dataVal.test())
                continue;
            String filename = Settings.translateFile(dataVal.format());

            Vector filter = new Vector();
            filter.add(name.substring(0, pos - 1));
            Object instr = new ExportMetricsFileInstruction(filename, filter);
            result.add(instr);
        }

        return result;
    }

    /**
     * If the named schedule were to be exported to a file, what would the data
     * element be named?
     */
    public static String exportedScheduleDataName(String owner,
            String scheduleName) {
                return exportedScheduleDataPrefix(owner, scheduleName) + "/"
                + EVTaskListXML.XML_DATA_NAME;
    }

        public static String exportedScheduleDataPrefix(String owner, String scheduleName) {
                return EVTaskList.MAIN_DATA_PREFIX
                + exportedScheduleName(owner, scheduleName);
        }

    public static String exportedScheduleName(String owner, String scheduleName) {
        owner = (owner == null ? "?????" : safeName(owner));
        String name = safeName(scheduleName) + " (" + owner + ")";
        return name;
    }

    private static String safeName(String n) {
        return n.replace('/', '_').replace(',', '_');
    }

}
