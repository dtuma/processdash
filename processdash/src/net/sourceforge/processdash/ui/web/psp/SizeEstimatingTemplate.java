// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.web.psp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.StringUtils;


public class SizeEstimatingTemplate extends TinyCGIBase {

    public String partA, baseRow, partB, newRow, partC, reusedRow, partD;
    private boolean needsInit = true;

    private static final String BASE_CUT  = "<!--BaseAdditions-->";
    private static final String NEW_CUT   = "<!--NewObjects-->";
    private static final String REUSE_CUT = "<!--ReusedObjects-->";
    private static final String CUT_END   = "<!--end-->";
    private static final String PAGE_TITLE =
        "\n<TITLE>Size Estimating Template</TITLE>\n";
    private static final String PLAN_FLAG = "\tp";
    private static final String ACTUAL_FLAG = "\ta";
    private static final String READONLY_FLAG = "\tr";
    private static final String EDITABLE_FLAG = "\t";


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

            partD = text.substring(end);
            needsInit = false;

        } catch (IndexOutOfBoundsException ioob) {
            needsInit = true;
            throw new IOException();
        }
    }

    private static final String [] baseData = {
        "Base Additions/#//#/Description",
        "Base Additions/#//#/Type",
        "Base Additions/#//#/Methods",
        "Base Additions/#//#/Relative Size",
        "Base Additions/#//#/LOC",
        "Base Additions/#//#/Actual LOC" };
    private static final String [] newData = {
        "New Objects/#//#/Description",
        "New Objects/#//#/Type",
        "New Objects/#//#/Methods",
        "New Objects/#//#/Relative Size",
        "New Objects/#//#/LOC",
        "New Objects/#//#/Actual LOC" };
    private static final String [] reusedData = {
        "Reused Objects/#//#/Description",
        "Reused Objects/#//#/LOC",
        "Reused Objects/#//#/Actual LOC" };


    /** Generate CGI script output.
     *
     * This method should be overridden by child classes to generate
     * the contents of the script.
     */
    protected void writeContents() throws IOException {
        if (needsInit || parameters.get("init") != null) init();
        if (parameters.containsKey("testzero")) uniqueNumber = 0;

        boolean freezeActual = hasValue("Completed");
        boolean freezePlan = freezeActual || hasValue("Planning/Completed");

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
        String [] dataNames = new String[dataElements.length];
        for (int e = dataElements.length; e-- > 0; )
            dataNames[e] = prefix + "/" + dataElements[e];

        DataRepository data = getDataRepository();
        ArrayList populatedRows = new ArrayList();

        int rowNum, lastPopulatedRow, i;
        rowNum = lastPopulatedRow = -1;
    ROW:
        while (true) {
            rowNum++;
            i = dataNames.length;
            while (i-- > 0)
                if (data.getValue(replaceNum(dataNames[i], rowNum)) != null) {
                    lastPopulatedRow = rowNum;
                    populatedRows.add(new Integer(rowNum));
                    continue ROW;
                }
            // if we haven't seen any data for 20 consecutive rows,
            // we can safely conclude that there is no more data.
            if (rowNum - lastPopulatedRow > 20)
                break ROW;
        }

        Iterator p = populatedRows.iterator();
        while (p.hasNext())
            out.print(replaceNum(template, ((Integer) p.next()).intValue()));

        int extraRows = 0;

        if (parameters.get(queryArg) != null)
            extraRows = addRows;
        else
            extraRows = padRows;

        extraRows = Math.max(extraRows, minRows - populatedRows.size());

        for (int e = 0;  e < extraRows;   e++)
            out.print(replaceNum(template, lastPopulatedRow+e+1));
    }

    /** find and replace occurrences of "#//#" with a number */
    protected String replaceNum(String template, int replacement) {
        return StringUtils.findAndReplace
            (template, "#//#", Integer.toString(replacement));
    }

    /** fixup a row template based upon freeze flags. */
    protected String fixupRow(String row, boolean freezePlan,
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
        return (data.getSimpleValue(dataName) != null);
    }

}
