// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


import pspdash.data.DataRepository;
import pspdash.TinyWebServer;
import java.io.*;
import java.util.*;

public class sizeest extends pspdash.TinyCGIBase {

    public String partA, baseRow, partB, newRow, partC, reusedRow, partD;
    private boolean needsInit = true;

    private static final String BASE_CUT  = "<!--BaseAdditions-->";
    private static final String NEW_CUT   = "<!--NewObjects-->";
    private static final String REUSE_CUT = "<!--ReusedObjects-->";
    private static final String CUT_END   = "<!--end-->";

    private void init() throws IOException {
        try {
            String uri = "/0" + env.get("SCRIPT_NAME");
            uri = uri.substring(0, uri.length() - 6) + ".htm";
            String text = new String(getRequest(uri, true));

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

        out.write(partA);
        writeTable(baseRow,   baseData,   "moreBase",   1, 0, 5);
        out.write(replaceNum(partB, uniqueNumber));
        writeTable(newRow,    newData,    "moreNew",    1, 0, 5);
        out.write(replaceNum(partC, uniqueNumber));
        writeTable(reusedRow, reusedData, "moreReused", 1, 0, 3);
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

        int maxRow = -1, i;
    ROW:
        while (true) {
            maxRow++;
            i = dataNames.length;
            while (i-- > 0)
                if (data.getValue(replaceNum(dataNames[i], maxRow)) != null)
                    continue ROW;
            break ROW;
        }

        if (parameters.get(queryArg) != null)
            maxRow += addRows;
        else
            maxRow += padRows;

        if (maxRow < minRows)
            maxRow = minRows;

        for (int rowNum = 0;  rowNum < maxRow;  rowNum++)
            out.print(replaceNum(template, rowNum));
    }


    /** find and replace occurrences of a string within buf */
    protected String replace(String template, String text, String replacement)
    {
        int pos, len = text.length();
        StringBuffer buf = new StringBuffer(template);
        while ((pos = buf.toString().indexOf(text)) != -1)
            buf.replace(pos, pos+len, replacement);
        return buf.toString();
    }
    /** find and replace occurrences of "#//#" with a number */
    protected String replaceNum(String template, int replacement) {
        return replace(template, "#//#", Integer.toString(replacement));
    }

}
