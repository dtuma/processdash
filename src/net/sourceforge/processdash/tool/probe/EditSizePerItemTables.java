// Copyright (C) 2009-2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.probe;

import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.process.ProcessUtil;
import net.sourceforge.processdash.tool.probe.SizePerItemTable.ParseException;
import net.sourceforge.processdash.tool.probe.SizePerItemTable.RelativeSize;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class EditSizePerItemTables extends TinyCGIBase {

    private static final String SCRIPT = "sizePerItemEdit";
    private static final String ACTION = "action";
    private static final String NAME = "name";
    private static final String UNITS = "units";
    private static final String CONTENTS = "contents";
    private static final String SAVE = "save";
    private static final String HEADER_KEY = "headerKey";
    private static final String NAME_EDITABLE = "nameEditable";

    public static final String URI = "dash/" + SCRIPT;
    public static final String CHANGE_DATA_NAME = "///Size_Per_Item_Save_Count";

    private enum Action {
        View, Edit, Delete, Copy, Create
    };
    private static final Action LAST_READ_ONLY_ACTION = Action.View;
    private static final Action LAST_TABLE_SPECIFIC_ACTION = Action.Copy;

    private static final Resources resources = Resources
            .getDashBundle("PROBE.SizePerItem");

    protected void doPost() throws IOException {
        DashController.checkIP(env.get("REMOTE_ADDR"));

        parseFormData();
        if (parameters.containsKey(SAVE) && Settings.isReadWrite()) {
            try {
                save(getParameter(NAME), getParameter(UNITS),
                    getParameter(CONTENTS));
                getDataRepository().putValue(CHANGE_DATA_NAME,
                    new DoubleData(uniqueNumber));
            } catch (ParseException e) {
                writeHeader();
                redrawForm(e);
                return;
            }
        }

        out.print("Location: " + SCRIPT + "?"+uniqueNumber+"\r\n\r\n");
        uniqueNumber++;
    }
    private static int uniqueNumber = 0;


    protected void doGet() throws IOException {
        writeHeader();

        Action action = getAction();
        if (action == Action.Create) {
            createNew();
            return;
        }

        String name = getParameter(NAME);
        SizePerItemTable table = SizePerItemTable.getByName(name,
            getDataRepository());
        if (table == null)
            action = null;

        if (action == Action.View)
            showTable(table);
        else if (action == Action.Edit)
            editExisting(table);
        else if (action == Action.Delete)
            deleteExisting(name, table.getSizeUnits());
        else if (action == Action.Copy)
            copyExisting(table);
        else
            showListOfDefinedStandards();

    }


    private Action getAction() {
        String action = getParameter(ACTION);
        if (StringUtils.hasValue(action)) {
            try {
                return Action.valueOf(action);
            } catch (Exception e) {
            }
        }
        return null;
    }


    protected void writeHTMLHeader() {
        String title = resources.getHTML("Page_Title");
        out.print("<html><head><title>");
        out.print(title);
        out.print("</title>\n"
                + "<link rel=stylesheet type='text/css' href='/style.css'>\n"
                + "<style>\n"
                + "  td.pad { padding-right: 0.3cm }\n"
                + "  table.view td { text-align: center; width: 5em }\n"
                + "  div.error { background-color: #ff5050; padding:10px; border: 1px solid black }\n"
                + "</style>\n" + "</head><body><h1>");
        out.print(title);
        out.print("</h1>\n");
    }


    protected void showListOfDefinedStandards() throws IOException {
        SortedMap<String, SizePerItemTable> tables = SizePerItemTable
                .getDefinedTables(getDataRepository());

        writeHTMLHeader();
        out.print("<p>");
        out.println(resources.getHTML("Welcome_Prompt"));
        out.println("<ul>");
        if (Settings.isReadWrite()) {
            out.print("<li><a href=\"" + SCRIPT + "?" + ACTION + "="
                    + Action.Create + "\">");
            out.print(resources.getHTML("Create_Option"));
            out.println("</a></li>");
        }

        if (!tables.isEmpty()) {
            out.print("<li>");
            out.print(resources.getHTML("Manage_Option"));
            out.println("<table>");

            for (Map.Entry<String, SizePerItemTable> e : tables.entrySet()) {
                String htmlName = esc(e.getKey());
                String urlName = HTMLUtils.urlEncode(e.getKey());

                out.print("<tr><td class='pad'><ul><li>&quot;<b>");
                out.print(htmlName);
                out.print("</b>&quot;&nbsp;(");
                out.print(esc(e.getValue().getSizeUnits()));
                out.print(")</li></ul></td>");

                for (Action opt : Action.values()) {
                    out.print("<td class='pad'><a href='" + SCRIPT + "?"
                            + ACTION + "=" + opt + "&" + NAME + "=" + urlName
                            + "'>");
                    out.print(resources.getHTML(opt.toString()));
                    out.print("</a></td>");

                    if (Settings.isReadOnly() && opt == LAST_READ_ONLY_ACTION)
                        break;
                    if (opt == LAST_TABLE_SPECIFIC_ACTION)
                        break;
                }

                out.println("</tr>");
            }
            out.print("</table></li>");
        }

        out.print("<li><a href='/reports/sizeEstParts.shtm'>");
        out.print(resources.getHTML("View_Hist_Data_Option"));
        out.println("</a></li>");

        out.print("</ul></body></html>");
    }


    private void showTable(SizePerItemTable table) throws IOException {
        writeHTMLHeader();

        out.print("<h2>");
        out.print(esc(table.getTableName()));
        out.println("</h2>");

        showUnits(table.getSizeUnits(), false);

        interpOut("<table border class='view'><tr>"
            + "<th>${Category}</th>"
            + "<th>${Very_Small}</th>"
            + "<th>${Small}</th>"
            + "<th>${Medium}</th>"
            + "<th>${Large}</th>"
            + "<th>${Very_Large}</th></tr>");

        for (String category : table.getCategoryNames()) {
            out.print("<tr><td>");
            out.print(esc(category));
            for (RelativeSize relSize : RelativeSize.values()) {
                out.print("</td><td class='center'>");
                out.print(FormatUtil.formatNumber(table.getSize(category,
                    relSize), 2));
            }
            out.println("</td><tr>");
        }

        out.println("</table>");
        interpOut("<p><a href='" + SCRIPT + "'>${Show_List}</a></p>");
        out.println("</body></html>");
    }




    private void editExisting(SizePerItemTable table) throws IOException {
        drawForm("Edit_Existing", table.getTableName(), false, table
                .getSizeUnits(), table, null);
    }

    private void createNew() throws IOException {
        drawForm("Create_New", resources.getString("Enter_New_Name"), true,
            new ProcessUtil(getDataContext()).getSizeUnits(), null, null);
    }

    private void copyExisting(SizePerItemTable table) throws IOException {
        drawForm("Copy_Existing", resources.getString("Enter_New_Name"), true,
            table.getSizeUnits(), table, null);
    }

    protected void deleteExisting(String tableName, String units) throws IOException {
        writeHTMLHeader();
        out.print("<h2>");
        out.print(resources.getHTML("Delete_Existing"));
        out.println("</h2><p>");
        out.print(resources.getHTML("Delete_Existing_Prompt"));
        out.println("<form action='" + SCRIPT + "' method='POST'>");
        showName(tableName, false);
        showUnits(units, false);
        out.print("<input type='hidden' name='" + CONTENTS + "' value=''>");

        out.print("<p><input type=submit name='"+SAVE+"' value='");
        out.print(resources.getHTML("OK"));
        out.print("'>&nbsp;");

        out.print("<input type=submit name='cancel' value='");
        out.print(resources.getHTML("Cancel"));
        out.print("'>");
        out.print("</form></body></html>");
    }

    private void redrawForm(ParseException e) throws IOException {
        String headerKey = getParameter(HEADER_KEY);
        String nameToDisplay = getParameter(NAME);
        String units = getParameter(UNITS);
        boolean nameEditable = parameters.containsKey(NAME_EDITABLE);
        drawForm(headerKey, nameToDisplay, nameEditable, units, null, e);
    }

    protected void drawForm(String headerKey, String nameToDisplay,
            boolean nameEditable, String sizeUnits, SizePerItemTable table,
            ParseException err) throws IOException {

        writeHTMLHeader();
        out.print("<h2>");
        out.print(resources.getHTML(headerKey));
        out.println("</h2>");
        out.println("<form action='" + SCRIPT + "' method='POST'>");
        showError(err);
        showName(nameToDisplay, nameEditable);
        showUnits(sizeUnits, true);
        showEditBox(table);

        out.print("<input type='hidden' name='" + HEADER_KEY + "' value='"
                + headerKey + "'>");
        if (nameEditable) {
            out.print("<input type='hidden' name='" + NAME_EDITABLE
                    + "' value='t'>");
        }

        out.print("<p><input type=submit name='" + SAVE + "' value='");
        out.print(resources.getHTML("Save"));
        out.print("'>&nbsp;");

        out.print("<input type=submit name='cancel' value='");
        out.print(resources.getHTML("Cancel"));
        out.print("'>");
        out.print("</form></body></html>");
    }

    protected void showError(ParseException pe) throws IOException {
        if (pe != null) {
            String key = "Errors." + pe.getResKey();
            Object[] args = pe.getArgs();

            out.print("<div class='error'>");
            if (args == null || args.length == 0)
                out.print(resources.getHTML(key));
            else
                out.print(esc(resources.format(key, args)));
            out.println("</div>");
        }
    }

    protected void showName(String tableName, boolean editable)
            throws IOException {
        out.print("<p><b>");
        out.print(resources.getHTML("Name_Prompt"));
        out.print("</b>&nbsp;");

        if (!editable)
            out.print(HTMLUtils.escapeEntities(tableName));

        out.print("<input type=");
        out.print(editable ? "text size='40'" : "hidden");
        out.print(" name='" + NAME + "' value='");
        out.print(HTMLUtils.escapeEntities(tableName));
        out.print("'></p>");
    }

    protected void showUnits(String units, boolean editable) throws IOException {
        out.print("<p><b>");
        out.print(resources.getHTML("Size_Units_Prompt"));
        out.print("</b>&nbsp;");

        if (!editable)
            out.print(units == null ? "?????" : HTMLUtils.escapeEntities(units));

        out.print("<input type=");
        out.print(editable ? "text size='10'" : "hidden");
        out.print(" name='" + UNITS + "' value='");
        if (units != null)
            out.print(HTMLUtils.escapeEntities(units));
        out.print("'></p>");
    }

    protected void showEditBox(SizePerItemTable table) throws IOException {
        out.print("<p>");
        out.println(resources.getHTML("Edit_Instructions"));
        out.print("<br><textarea name='"+CONTENTS+"' rows=12 cols=80>");

        String redrawnContents = getParameter(CONTENTS);
        if (StringUtils.hasValue(redrawnContents)) {
            out.print(esc(redrawnContents));
        } else if (table == null) {
            out.print(resources.getHTML("Sample_Category"));
        } else {
            out.print(esc(table.formatForDisplay()));
        }
        out.println("</textarea>");
    }

    private void save(String name, String units, String contents)
            throws ParseException {
        if (resources.getString("Enter_New_Name").equals(name))
            name = null;
        SizePerItemTable table = new SizePerItemTable(name, units, contents);
        table.save(getDataRepository());
    }


    private void interpOut(String s) {
        out.print(resources.interpolate(s, HTMLUtils.ESC_ENTITIES));
    }

    private static String esc(String s) {
        return HTMLUtils.escapeEntities(s);
    }

}
