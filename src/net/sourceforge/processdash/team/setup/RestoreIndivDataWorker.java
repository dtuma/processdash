// Copyright (C) 2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.setup;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataNameFilter;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.log.ChangeFlagged;
import net.sourceforge.processdash.log.defects.Defect;
import net.sourceforge.processdash.log.defects.DefectAnalyzer;
import net.sourceforge.processdash.log.defects.DefectLog;
import net.sourceforge.processdash.log.defects.DefectLogID;
import net.sourceforge.processdash.log.defects.ImportedDefectManager;
import net.sourceforge.processdash.log.time.ImportedTimeLogManager;
import net.sourceforge.processdash.log.time.ModifiableTimeLog;
import net.sourceforge.processdash.log.time.TimeLogEntry;
import net.sourceforge.processdash.log.time.TimeLogEntryVO;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.tool.export.DataImporter;
import net.sourceforge.processdash.tool.export.impl.ArchiveMetricsFileImporter;


public class RestoreIndivDataWorker implements TeamDataConstants, DefectAnalyzer.Task {

    DashboardContext ctx;

    String projectPrefix;

    File pdashFile;

    String importPrefix;

    String defectDirectory;

    String importedProjectRoot;

    Map<String, String> hierarchyPathsToIDs;

    Map<String, String> hierarchyIDsToPaths;

    Map<String, String> importedPathsToIDs;

    Map<String, String> importedIDsToPaths;

    public RestoreIndivDataWorker(DashboardContext ctx, String projectPrefix,
            File pdashFile) throws IOException {
        this.ctx = ctx;
        this.projectPrefix = projectPrefix;
        this.pdashFile = pdashFile;
        this.importPrefix = DataImporter.getPrefix("/RestoreIndivData",
            pdashFile);

        File settingsFile = new File(DashController.getSettingsFileName());
        this.defectDirectory = settingsFile.getParent() + File.separator;
    }

    public void run() throws IOException {
        openData();
        scanImportedDataForIDs();
        scanHierarchyForIDs();

        checkPreconditions();
        restoreTimeLogData();
        restoreDefects();
        restoreCompletionDates();
        // TODO: restore actual size metrics, flat view order

        closeData();
    }

    private void openData() throws IOException {
        ArchiveMetricsFileImporter task = new ArchiveMetricsFileImporter(ctx
                .getData(), pdashFile, importPrefix);
        task.doImport();
    }

    private void scanHierarchyForIDs() {
        hierarchyPathsToIDs = new HashMap<String, String>();
        hierarchyIDsToPaths = new HashMap<String, String>();

        DashHierarchy hier = ctx.getHierarchy();
        PropertyKey projectNode = hier.findExistingKey(projectPrefix);
        scanHierarchyForIDs(hier, projectNode);
    }

    private void scanHierarchyForIDs(DashHierarchy hier, PropertyKey node) {
        String path = node.path();
        String id = getStrData(path, WBS_ID_DATA_NAME);
        if (id != null) {
            hierarchyPathsToIDs.put(path, id);
            hierarchyIDsToPaths.put(id, path);
        }

        for (int i = hier.getNumChildren(node); i-- > 0;)
            scanHierarchyForIDs(hier, hier.getChildKey(node, i));
    }

    private void scanImportedDataForIDs() {
        importedPathsToIDs = new HashMap<String, String>();
        importedIDsToPaths = new HashMap<String, String>();

        Iterator names = ctx.getData().getKeys(importPrefix,
            DataNameFilter.EXPLICIT_ONLY);
        while (names.hasNext()) {
            String dataName = (String) names.next();
            if (Filter.pathMatches(dataName, importPrefix)) {
                if (dataName.endsWith("/" + WBS_ID_DATA_NAME)) {
                    String id = getStrData(dataName);
                    if (id != null) {
                        String path = DataRepository.chopPath(dataName);
                        importedPathsToIDs.put(path, id);
                        importedIDsToPaths.put(id, path);
                    }
                } else if (dataName.endsWith(" Indiv Root Tag")) {
                    importedProjectRoot = DataRepository.chopPath(dataName);
                }
            }
        }
    }

    private enum MapType {
        NearestParent, KeepExtra, RequireMatch
    };

    private String mapPathToHierarchy(String path, MapType mapType) {
        String workingPath = path;
        while (workingPath != null) {
            String id = importedPathsToIDs.get(workingPath);
            String hierarchyPath = hierarchyIDsToPaths.get(id);
            if (hierarchyPath != null) {
                String result = hierarchyPath;
                if (mapType == MapType.KeepExtra)
                    result = result + path.substring(workingPath.length());
                return result;
            }

            if (mapType == MapType.RequireMatch)
                return null;
            else
                workingPath = DataRepository.chopPath(workingPath);
        }

        String result = projectPrefix;
        if (mapType == MapType.KeepExtra)
            result = result + path.substring(importedProjectRoot.length());
        return result;
    }

    private void checkPreconditions() throws IOException {
        String hierProjectID = getStrData(projectPrefix, PROJECT_ID);
        String importProjectID = getStrData(importedProjectRoot, PROJECT_ID);
        if (!hierProjectID.equals(importProjectID))
            throw new TinyCGIException(400, "Project IDs do not match");
    }

    private void restoreTimeLogData() throws IOException {
        // make a list of the time log entries that are already logged against
        // the project within the current dashboard.
        Set<Date> knownEntries = new HashSet();
        ModifiableTimeLog timeLog = (ModifiableTimeLog) ctx.getTimeLog();
        Iterator i = timeLog.filter(projectPrefix, null, null);
        while (i.hasNext()) {
            TimeLogEntry tle = (TimeLogEntry) i.next();
            knownEntries.add(tle.getStartTime());
        }

        // now scan the imported time log entries, and add any missing entries
        // to the time log.
        i = ImportedTimeLogManager.getInstance().getImportedTimeLogEntries(
            importPrefix);
        while (i.hasNext()) {
            TimeLogEntry tle = (TimeLogEntry) i.next();
            if (!knownEntries.contains(tle.getStartTime())) {
                String importedPath = tle.getPath();
                String hierPath = mapPathToHierarchy(importedPath,
                    MapType.KeepExtra);
                TimeLogEntryVO newTle = new TimeLogEntryVO(timeLog.getNextID(),
                        hierPath, tle.getStartTime(), tle.getElapsedTime(),
                        tle.getInterruptTime(), tle.getComment(),
                        ChangeFlagged.ADDED);
                timeLog.addModification(newTle);
            }
        }
    }

    private void restoreDefects() {
        String[] prefixes = { importedProjectRoot };
        ImportedDefectManager.run(ctx.getHierarchy(), ctx.getData(), prefixes,
            true, this);
    }

    public void analyze(String path, Defect d) {
        DashHierarchy hier = ctx.getHierarchy();
        PropertyKey node = hier.findClosestKey(path);
        DefectLogID defectLogID = hier.defectLog(node, defectDirectory);
        DefectLog defectLog = new DefectLog(defectLogID.filename,
                defectLogID.path.path(), ctx.getData());
        if (defectLog.getDefect(d.number) == null)
            defectLog.writeDefect(d);
    }

    private void restoreCompletionDates() {
        for (String importedPath : importedPathsToIDs.keySet()) {
            SimpleData completionDate = getData(importedPath, COMPLETED);
            if (completionDate == null)
                continue;

            String hierarchyPath = mapPathToHierarchy(importedPath,
                MapType.RequireMatch);
            if (hierarchyPath == null)
                continue;

            putData(hierarchyPath, COMPLETED, completionDate);
        }
    }


    private void closeData() {
        DataImporter.closeImportedFile(ctx.getData(), importPrefix);
    }

    private String getStrData(String prefix, String name) {
        return getStrData(DataRepository.createDataName(prefix, name));
    }

    private String getStrData(String dataName) {
        SimpleData sd = ctx.getData().getSimpleValue(dataName);
        return (sd == null ? null : sd.format());
    }

    private SimpleData getData(String prefix, String name) {
        return getData(DataRepository.createDataName(prefix, name));
    }

    private SimpleData getData(String dataName) {
        return ctx.getData().getSimpleValue(dataName);
    }

    private void putData(String prefix, String name, SimpleData value) {
        putData(DataRepository.createDataName(prefix, name), value);
    }

    private void putData(String dataName, SimpleData value) {
        ctx.getData().putValue(dataName, value);
    }

    private static final String COMPLETED = "Completed";

}
