// Copyright (C) 2005-2021 Tuma Solutions, LLC
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

import java.util.List;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.tool.export.impl.ExportFileStream;


/**
 * Cleans up export files that are no longer relevant.
 * 
 * Files are exported via the use of EXPORT_FILE instructions that appear
 * in the data repository.  These instructions are often calculated values.
 * When the calculations change, the name of the exported file changes as
 * well.  The next time an export occurs, a new file will be created but
 * the old file will remain.  This class contains logic to detect and delete
 * those out-of-date files.
 */
class ExportJanitor {

    /** Legacy entry in the data repository that was previously used to contain
     * the most recent filenames that had been exported */
    private static final String CURRENT_EXPORTED_FILES_DATANAME =
        "/Current_Exported_Filenames";

    /** Entry in the data repository that contains filenames that were
     * exported in the past. */
    private static final String HISTORICALLY_EXPORTED_DATANAME =
        "/Historically_Exported_Filenames";

    private DataContext data;

    public ExportJanitor(DataContext data) {
        this.data = data;
    }


    private SimpleData originalHistList;

    /**
     * Some export operations are performed manually by the user and are not
     * intended for repeated execution.  When we fail to see a repeat of such
     * an operation in the future, we should not interpret it as an indication
     * that the manually exported file is obsolete.  This method is used to
     * frame the beginning of such an operation.
     */
    public void startOneTimeExportOperation() {
        SimpleData histList = data
                .getSimpleValue(HISTORICALLY_EXPORTED_DATANAME);
        if (histList instanceof ListData)
            histList = new ListData((ListData) histList);
        this.originalHistList = histList;
    }

    public void finishOneTimeExportOperation() {
        data.putValue(HISTORICALLY_EXPORTED_DATANAME, originalHistList);
    }


    public void cleanHistoricalFiles(List<String> currentExportedFiles) {
        // get the previous list of exported files. If none, abort
        ListData historicallyExportedFiles =
            ListData.asListData(data.getValue(HISTORICALLY_EXPORTED_DATANAME));
        if (historicallyExportedFiles == null || !historicallyExportedFiles.test())
            return;

        // iterate over the old files. If any are missing from the list of
        // current exports, delete them
        for (int i = 0; i < historicallyExportedFiles.size(); ) {
            Object item = historicallyExportedFiles.get(i);
            String path = normalize(item);

            if (!currentExportedFiles.contains(path) && deletionSuccessful(path)) {
                historicallyExportedFiles.remove(item);
            }
            else {
                ++i;
            }
        }

        // write updated lists into the data repository
        data.putValue(CURRENT_EXPORTED_FILES_DATANAME, null);
        data.putValue(HISTORICALLY_EXPORTED_DATANAME, historicallyExportedFiles);
    }

    private boolean deletionSuccessful(String path) {
        return ExportFileStream.deleteExportTarget(path);
    }



    static void recordSuccessfulFileExport(DataContext data, String path) {
        recordExportedFile(data, HISTORICALLY_EXPORTED_DATANAME, path);
    }

    private static void recordExportedFile(DataContext data, String list,
            String path) {
        ListData exportedFiles = ListData.asListData(data.getValue(list));

        if (exportedFiles == null) {
            exportedFiles = new ListData();
        }

        if (exportedFiles.setAdd(normalize(path))) {
            exportedFiles.sortContents(String.CASE_INSENSITIVE_ORDER);
            data.putValue(list, exportedFiles);
        }
    }

    private static String normalize(Object s) {
        if (s == null)
            return "";
        else
            return s.toString().trim().replace('\\', '/');
    }
}
