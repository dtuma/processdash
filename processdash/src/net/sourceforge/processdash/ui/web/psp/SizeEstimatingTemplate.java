// Copyright (C) 2002-2009 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.web.psp;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.TagData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.StringUtils;


public class SizeEstimatingTemplate extends TinyCGIBase {

    public String partA, baseRow, partB, newRow, partC, reusedRow, partD, partE;
    private boolean needsInit = true;

    private static final String BASE_CUT  = "<!--BaseAdditions-->";
    private static final String NEW_CUT   = "<!--NewObjects-->";
    private static final String REUSE_CUT = "<!--ReusedObjects-->";
    private static final String CUT_END   = "<!--end-->";
    private static final String INS_POS   = "</FORM>";
    private static final String PLAN_FLAG = "\tp";
    private static final String ACTUAL_FLAG = "\ta";
    private static final String READONLY_FLAG = "\tr";
    private static final String EDITABLE_FLAG = "\t";

    protected static final Logger logger = Logger
            .getLogger(SizeEstimatingTemplate.class.getName());


    private void init() throws IOException {
        try {
            String uri = (String) env.get("SCRIPT_NAME");
            int pos = uri.lastIndexOf('/');
            pos = uri.indexOf('.', pos);
            uri = uri.substring(0, pos) + ".htm";
            String text = getRequestAsString(uri);

            int beg, end;
            end = text.indexOf(BASE_CUT);
            partA = text.substring(0, end);

            end = text.indexOf(CUT_END, beg = end);
            baseRow = text.substring(beg, end);

            end = text.indexOf(NEW_CUT, beg = end);
            partB = text.substring(beg, end);

            end = text.indexOf(CUT_END, beg = end);
            newRow = text.substring(beg, end);

            end = text.indexOf(REUSE_CUT, beg = end);
            partC = text.substring(beg, end);

            end = text.indexOf(CUT_END, beg = end);
            reusedRow = text.substring(beg, end);

            end = text.indexOf(INS_POS, beg = end);
            partD = text.substring(beg, end);

            partE = text.substring(end);
            needsInit = false;

        } catch (IndexOutOfBoundsException ioob) {
            needsInit = true;
            throw new IOException();
        }
    }

    protected static final String BASE_ADDITIONS_DATANAME =
        "Base_Additions_List";
    protected static final String DISABLE_FREEZING_DATANAME =
        "Size_Estimating_Template_Unlocked";
    protected static final String [] baseData = {
        BASE_ADDITIONS_DATANAME,
        "Base Additions/#//#/Description",
        "Base Additions/#//#/Type",
        "Base Additions/#//#/Methods",
        "Base Additions/#//#/Relative Size",
        "Base Additions/#//#/LOC",
        "Base Additions/#//#/Actual Methods",
        "Base Additions/#//#/Actual LOC" };
    protected static final String [] newData = {
        "New_Objects_List",
        "New Objects/#//#/Description",
        "New Objects/#//#/Type",
        "New Objects/#//#/Methods",
        "New Objects/#//#/Relative Size",
        "New Objects/#//#/LOC",
        "New Objects/#//#/Actual Methods",
        "New Objects/#//#/Actual LOC" };
    protected static final String [] reusedData = {
        "Reused_Objects_List",
        "Reused Objects/#//#/Description",
        "Reused Objects/#//#/LOC",
        "Reused Objects/#//#/Actual LOC" };
    private static final String ACTIVE_LIST_HTML =
        "<input type='hidden' name='[Base_Additions_List//Active]s'>\n" +
        "<input type='hidden' name='[New_Objects_List//Active]s'>\n" +
        "<input type='hidden' name='[Reused_Objects_List//Active]s'>\n";


    /** Generate CGI script output.
     *
     * This method should be overridden by child classes to generate
     * the contents of the script.
     */
    protected void writeContents() throws IOException {
        if (needsInit || parameters.get("init") != null) init();
        if (parameters.containsKey("testzero")) uniqueNumber = 0;

        boolean disableFreezing = hasValue(DISABLE_FREEZING_DATANAME);
        boolean freezeActual = !disableFreezing && hasValue("Completed");
        boolean freezePlan = !disableFreezing &&
                (freezeActual || hasValue("Planning/Completed"));

        out.write(partA);
        writeTable(fixupRow(baseRow, freezePlan, freezeActual),
                   baseData,   "moreBase",   1, 0, 5);
        out.write(replaceNum(partB, uniqueNumber));
        writeTable(fixupRow(newRow, freezePlan, freezeActual),
                   newData,    "moreNew",    1, 0, 5);
        out.write(replaceNum(partC, uniqueNumber));
        writeTable(fixupRow(reusedRow, freezePlan, freezeActual),
                   reusedData, "moreReused", 1, 0, 3);
        out.write(replaceNum(partD, uniqueNumber++));
        out.write(ACTIVE_LIST_HTML);
        out.write(partE);
    }

    static int uniqueNumber = 0;


    /** Write a table containing a variable number of rows.
     *
     * @param template HTML text which should be printed for each row.
     *    Occurrences of the text "#//#" will be replaced with the row number.
     * @param dataElements the names of the data elements that are being
     *    displayed in the table.
     * @param minRows the minimum number of rows to display.
     * @param padRows the number of blank rows to display after data
     * @param addRows the number of blank rows to add upon user request
     * @param queryArg a string which, if present in the query string,
     *    indicates a request from the user to add extra rows.
     */
    protected void writeTable(String template,
                              String [] dataElements,
                              String queryArg,
                              int minRows, int padRows, int addRows) {

        String prefix = (String) env.get("PATH_TRANSLATED");

        int extraRows = padRows;
        if (parameters.get(queryArg) != null && !isExporting())
            extraRows = addRows;

        ListData rows = configureSETList(getDataRepository(), prefix,
            dataElements, minRows, extraRows, !isExporting());
        writeTableRows(template, rows);
    }

    protected void writeTableRows(String template, ListData rows) {
        for (int i = 0; i < rows.size();  i++)
            out.print(replaceNum(template, (String) rows.get(i)));
    }

    protected static ListData configureSETList(DataRepository data,
            String prefix, String[] dataElements, int minRows, int padRows,
            boolean saveList) {
        String [] dataNames = new String[dataElements.length];
        for (int e = dataElements.length; e-- > 0; )
            dataNames[e] = prefix + "/" + dataElements[e];

        ListData populatedRows = new ListData();

        int rowNum, lastPopulatedRow, i;
        rowNum = lastPopulatedRow = -1;
    ROW:
        while (true) {
            rowNum++;
            i = dataNames.length;
            while (i-- > 1)
                if (data.getValue(replaceNum(dataNames[i], rowNum)) != null) {
                    lastPopulatedRow = rowNum;
                    populatedRows.add(Integer.toString(rowNum));
                    continue ROW;
                }
            // if we haven't seen any data for 20 consecutive rows,
            // we can safely conclude that there is no more data.
            if (rowNum - lastPopulatedRow > 20)
                break ROW;
        }

        int extraRows = Math.max(padRows, minRows - populatedRows.size());
        for (int e = 0;  e < extraRows;   e++)
            populatedRows.add(Integer.toString(lastPopulatedRow+e+1));

        if (saveList) {
            String listName = dataNames[0];
            String activeName = listName + "//Active";
            ListData currentActiveElements = ListData.asListData(data
                    .getValue(activeName));

            ListData newActiveElements = new ListData(populatedRows);
            newActiveElements.setAddAll(currentActiveElements);

            data.putValue(listName, newActiveElements);
            data.putValue(activeName, newActiveElements);
        }

        return populatedRows;
    }

    /** find and replace occurrences of "#//#" with a number */
    protected static String replaceNum(String template, int replacement) {
        return replaceNum(template, Integer.toString(replacement));
    }
    protected static String replaceNum(String template, String replacement) {
        return StringUtils.findAndReplace(template, "#//#", replacement);
    }

    /** fixup a row template based upon freeze flags. */
    private String fixupRow(String row, boolean freezePlan,
                              boolean freezeActual) {
        row = StringUtils.findAndReplace
            (row, PLAN_FLAG,   freezePlan   ? READONLY_FLAG : EDITABLE_FLAG);
        row = StringUtils.findAndReplace
            (row, ACTUAL_FLAG, freezeActual ? READONLY_FLAG : EDITABLE_FLAG);
        return row;
    }

    /** @return true if the data element named by prefix/name is nonnull. */
    protected boolean hasValue(String name) {
        String prefix = (String) env.get("PATH_TRANSLATED");
        DataRepository data = getDataRepository();

        String dataName = DataRepository.createDataName(prefix, name);
        SimpleData value = data.getSimpleValue(dataName);
        return (value != null && value.test());
    }


    /**
     * Perform cleanup operations on data that was collected using Process
     * Dashboard 1.10.4 or earlier.
     * 
     * Versions of the dashboard up through and including version 1.10.4
     * used the <tt>search()</tt> function in their datafile to make lists of
     * base additions, new objects, and reused objects. The search() function
     * is very expensive and inefficient, so this was replaced by a new
     * strategy using precomputed lists where data is known to have been
     * recorded. This method will migrate legacy data to the new strategy if
     * it has not already been migrated.
     * 
     * @param hierarchy the hierarchy of dashboard tasks
     * @param data the data repository
     */
    public static void migrateLegacyData(DashHierarchy hierarchy,
            DataRepository data) {
        if (Settings.getBool(SET_FLAG_SETTING, false))
            return;

        for (Iterator i = hierarchy.keySet().iterator(); i.hasNext();) {
            PropertyKey key = (PropertyKey) i.next();
            String path = key.path();
            maybeMigrateLegacyData(data, path);
        }
        InternalSettings.set(SET_FLAG_SETTING, "true");
    }

    private static void maybeMigrateLegacyData(DataRepository data, String path) {
        String tagName = DataRepository.createDataName(path, SET_TAG);
        if (data.getValue(tagName) instanceof TagData) {
            logger.info("Migrating Size Estimating Template data for project "
                    + path);
            renameLegacyDataElements(data, path);
            configureSETList(data, path, baseData, 0, 0, true);
            configureSETList(data, path, newData, 1, 0, true);
            configureSETList(data, path, reusedData, 1, 0, true);
        }
    }
    private static void renameLegacyDataElements(DataRepository data,
            String path) {
        for (int i = 0; i < ELEMENTS_TO_RENAME.length; i++) {
            String legacyName = ELEMENTS_TO_RENAME[i][0];
            String newName = ELEMENTS_TO_RENAME[i][1];

            String legacyDataName = DataRepository.createDataName(path, legacyName);
            String newDataName = DataRepository.createDataName(path, newName);

            SaveableData value = data.getValue(legacyDataName);
            if (value instanceof DoubleData) {
                data.putValue(newDataName, value);
                data.restoreDefaultValue(legacyDataName);
            }
        }
    }

    private static final String SET_FLAG_SETTING = "internal.ranNewSETCleanup";
    private static final String SET_TAG = "Size Estimating Template Tag";
    private static final String[][] ELEMENTS_TO_RENAME = {
        { "Estimated Base LOC", "Base_Parts/0/Base" },
        { "Estimated Deleted LOC", "Base_Parts/0/Deleted" },
        { "Estimated Modified LOC", "Base_Parts/0/Modified" },
        { "Base LOC", "Base_Parts/0/Actual Base" },
        { "Deleted LOC", "Base_Parts/0/Actual Deleted" },
        { "Modified LOC", "Base_Parts/0/Actual Modified" },
    };

}
