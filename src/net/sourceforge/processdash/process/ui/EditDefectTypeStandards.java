// Copyright (C) 2003-2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.process.ui;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.process.DefectTypeStandard;
import net.sourceforge.processdash.tool.perm.PermissionsManager;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;



/** CGI script for editing defect type standards.
 */
public class EditDefectTypeStandards extends TinyCGIBase {

    protected static final String ACTION = "action";
    protected static final String CONFIRM = "confirm";
    protected static final String CREATE = "create";
    protected static final String NAME = "name";
    protected static final String SAVE = "save";
    protected static final String SET_DEFAULT = "setDefault";
    protected static final String EXPORT = "export";
    protected static final String IMPORT = "import";
    protected static final String IMPORT_CONFIRM = "doImport";
    protected static final String CONTENTS = "contents";

    protected static final String[] OPTIONS = {
        "View", "Edit", "Delete", "Copy", "Default" };

    private static final int VIEW = 0;
    private static final int EDIT = 1;
    private static final int DELETE = 2;
    private static final int COPY = 3;
    private static final int DEFAULT = 4;

    private static int uniqueNumber = 0;

    public static boolean canEdit() {
        return Settings.isReadWrite() && PermissionsManager.getInstance()
                .hasPermission("pdash.defectTypes");
    }

    protected void doPost() throws IOException {
        DashController.checkIP(env.get("REMOTE_ADDR"));
        rejectCrossSiteRequests(env);

        boolean canEdit = canEdit();

        String contentType = (String) env.get("CONTENT_TYPE");
        if (contentType.toLowerCase().contains("multipart/form-data"))
            parseMultipartFormData();
        else
            parseFormData();

        if (canEdit && parameters.containsKey(SAVE)) {
            save(getParameter(NAME), getParameter(CONTENTS));
        } else if (canEdit && parameters.containsKey(SET_DEFAULT)) {
            saveDefault(getParameter(NAME));
        } else if (parameters.containsKey(EXPORT)) {
            handleExport();
            return;
        } else if (canEdit && parameters.containsKey(IMPORT)) {
            handleImportForm();
            return;
        } else if (canEdit && parameters.containsKey(IMPORT_CONFIRM)) {
            handleImportConfirm();
        }

        out.print("Location: dtsEdit.class?"+uniqueNumber+"\r\n\r\n");
        uniqueNumber++;
    }

    /** Generate CGI script output. */
    protected void doGet() throws IOException {
        writeHeader();

        boolean canEdit = canEdit();
        String action = getParameter(ACTION);
        String name = getParameter(NAME);
        if (canEdit && CREATE.equals(action))
            createNew();
        else if (OPTIONS[VIEW].equals(action))
            showStandard(name);
        else if (canEdit && OPTIONS[EDIT].equals(action))
            editExisting(name);
        else if (canEdit && OPTIONS[DELETE].equals(action))
            deleteExisting(name);
        else if (canEdit && OPTIONS[COPY].equals(action))
            copyExisting(name);
        else if (canEdit && OPTIONS[DEFAULT].equals(action))
            setDefault(name);
        else if (EXPORT.equals(action))
            showExportPage();
        else if (canEdit && IMPORT.equals(action))
            showImportForm(null);
        else
            showListOfDefinedStandards();
    }

    private static final Resources resources =
        Resources.getDashBundle("Defects.Standard");

    protected void writeHTMLHeader() {
        String title = resources.getString("Page_Title");
        out.print("<html><head><title>");
        out.print(title);
        out.print("</title>\n"+
                  "<link rel=stylesheet type='text/css' href='/style.css'>\n"+
                  "<style>\n"+
                  "  TD { padding-right: 0.3cm }\n"+
                  "</style>\n"+
                  "</head><body><h1>");
        out.print(title);
        out.print("</h1>\n");
    }

    protected void showStandard(String name) throws IOException {
        out.print("<html><head>");
        String uri = "../reports/dts.class?name=" + HTMLUtils.urlEncode(name);
        writeRedirectInstruction(uri, 0);
        out.print("</head><body>&nbsp;</body></html>");
    }


    protected void showListOfDefinedStandards() throws IOException {
        DataRepository data = getDataRepository();
        String[] standards = DefectTypeStandard.getDefinedStandards(data);
        boolean canEdit = canEdit();

        String defaultName = DefectTypeStandard.get("", data).getName();

        writeHTMLHeader();
        out.print("<p>");
        out.println(resources.getHTML("Welcome_Prompt"));
        out.println("<ul>");

        if (canEdit) {
            out.print("<li><a href=\"dtsEdit.class?"+ACTION+"="+CREATE+"\">");
            out.print(resources.getHTML("Create_Option"));
            out.println("</a></li>");
        }

        if (standards.length > 0) {
            out.print("<li>");
            out.print(resources.getHTML("Manage_Option"));
            out.println("<table>");

            for (int i = 0;   i < standards.length;   i++) {
                String htmlName = HTMLUtils.escapeEntities(standards[i]);
                String urlName = HTMLUtils.urlEncode(standards[i]);

                out.print("<tr><td><ul><li>&quot;<b>");
                out.print(htmlName);
                out.print("</b>&quot;</li></ul></td>");

                int numOptions = (canEdit ? OPTIONS.length : 1);
                for (int o = 0; o < numOptions; o++) {
                    if (o == DEFAULT && standards[i].equals(defaultName)) {
                        out.print("<td><i>(default)</i></td>");
                    } else {
                        String opt = OPTIONS[o];
                        out.print("<td><a href='dtsEdit.class?"+ACTION+"="+opt+
                                  "&"+NAME+"="+urlName+"'>");
                        out.print(resources.getHTML(opt));
                        out.print("</a></td>");
                    }
                }
                out.println("</tr>");
            }
            out.print("</table></li>");
        }

        out.print("<li>");
        if (canEdit) {
            out.print("<a href=\"dtsEdit.class?"+ACTION+"="+IMPORT+"\">");
            out.print(resources.getHTML("Import_Option"));
            out.println("</a> / ");
        }
        out.print("<a href=\"dtsEdit.class?"+ACTION+"="+EXPORT+"\">");
        out.print(resources.getHTML("Export_Option"));
        out.println("</a></li>");

        out.print("</ul></body></html>");
    }

    protected void showName(String standardName, boolean editable)
        throws IOException
    {
        out.print("<p><b>");
        out.print(resources.getHTML("Name_Prompt"));
        out.print("</b>&nbsp;");

        if (!editable)
            out.print(HTMLUtils.escapeEntities(standardName));

        out.print("<input type=");
        out.print(editable ? "text size=40" : "hidden");
        out.print(" name='"+NAME+"' value='");
        out.print(HTMLUtils.escapeEntities(standardName));
        out.print("'>");
    }

    protected void showEditBox(String standardName) throws IOException {
        DefectTypeStandard defectTypeStandard = null;
        if (standardName != null)
            defectTypeStandard = DefectTypeStandard.getByName
                (standardName, getDataRepository());

        out.print("<p>");
        out.println(resources.getHTML("Edit_Instructions"));
        out.print("<br><textarea name='"+CONTENTS+"' rows=12 cols=80>");

        if (defectTypeStandard == null) {
            out.print(resources.getHTML("Sample_Defect_Type"));
        } else {
            String type, description;
            for (int i=0;  i<defectTypeStandard.options.size();  i++) {
                type = (String) defectTypeStandard.options.elementAt(i);
                description = (String) defectTypeStandard.comments.get(type);
                out.print(HTMLUtils.escapeEntities(type));
                if (description != null && description.length() > 0) {
                    out.print(" (");
                    out.print(HTMLUtils.escapeEntities(description));
                    out.print(")");
                }
                out.println();
            }
        }
        out.println("</textarea>");
    }

    protected void drawForm(String headerKey,
                            String nameToDisplay,
                            boolean nameEditable,
                            String realName) throws IOException {

        writeHTMLHeader();
        out.print("<h2>");
        out.print(resources.getHTML(headerKey));
        out.println("</h2>");
        out.println("<form action='dtsEdit.class' method='POST'>");
        showName(nameToDisplay, nameEditable);
        showEditBox(realName);

        out.print("<p><input type=submit name='"+SAVE+"' value='");
        out.print(resources.getHTML("Save"));
        out.print("'>&nbsp;");

        out.print("<input type=submit name='cancel' value='");
        out.print(resources.getHTML("Cancel"));
        out.print("'>");
        out.print("</form></body></html>");
    }

    protected void editExisting(String standardName) throws IOException {
        drawForm("Edit_Existing", standardName, false, standardName);
    }

    protected void createNew() throws IOException {
        drawForm("Create_New", "Enter Name", true, null);
    }

    protected void copyExisting(String standardName) throws IOException {
        drawForm("Copy_Existing", "Enter New Name", true, standardName);
    }


    protected void save(String standardName, String contents) {
        String[] types = null;
        if (contents != null)
            types = StringUtils.split(contents, "\n");
        DefectTypeStandard.save
            (standardName, getDataRepository(), types,
             resources.getString("Sample_Defect_Type"));
    }

    protected void deleteExisting(String standardName) throws IOException {
        writeHTMLHeader();
        out.print("<h2>");
        out.print(resources.getHTML("Delete_Existing"));
        out.println("</h2><p>");
        out.print(resources.getHTML("Delete_Existing_Prompt"));
        out.println("<form action='dtsEdit.class' method='POST'>");
        showName(standardName, false);

        out.print("<p><input type=submit name='"+SAVE+"' value='");
        out.print(resources.getHTML("OK"));
        out.print("'>&nbsp;");

        out.print("<input type=submit name='cancel' value='");
        out.print(resources.getHTML("Cancel"));
        out.print("'>");
        out.print("</form></body></html>");
    }

    protected void setDefault(String standardName) throws IOException {
        writeHTMLHeader();
        out.print("<h2>");
        out.print(resources.getHTML("Set_As_Default"));
        out.println("</h2><p>");
        out.print(resources.getHTML("Set_As_Default_Prompt"));
        out.println("<form action='dtsEdit.class' method='POST'>");
        showName(standardName, false);

        out.print("<p><input type=submit name='"+SET_DEFAULT+"' value='");
        out.print(resources.getHTML("OK"));
        out.print("'>&nbsp;");

        out.print("<input type=submit name='cancel' value='");
        out.print(resources.getHTML("Cancel"));
        out.print("'>");
        out.print("</form></body></html>");
    }

    protected void saveDefault(String standardName) {
        DefectTypeStandard.saveDefault(getDataRepository(), "", standardName);
    }

    protected void showExportPage() throws IOException {
        String[] standards = DefectTypeStandard
                .getDefinedStandards(getDataRepository());
        if (standards.length == 0) {
            showListOfDefinedStandards();
            return;
        }

        writeHTMLHeader();
        out.print("<h2>");
        out.print(resources.getHTML("Export.Title"));
        out.println("</h2>\n<p>");
        out.print(resources.getHTML("Export.Prompt"));
        out.println("<form action='dtsEdit.class' method='POST'>");

        out.print("<ul style='list-style:none'>");
        for (int i = 0; i < standards.length; i++) {
            String stdName = standards[i];
            out.print("<li><input type='hidden' name='std"+i+"' value='");
            out.print(HTMLUtils.escapeEntities(stdName));
            out.print("'/><input type='checkbox' name='sel' value='"+i+"'/> ");
            out.print(HTMLUtils.escapeEntities(stdName));
            out.print("</li>\n");
        }
        out.print("</ul>");

        out.print("<p><input type=submit name='" + EXPORT + "' value='");
        out.print(resources.getHTML("Export"));
        out.print("'></p>");

        out.print("<p><a href='dtsEdit.class'>");
        out.print(resources.getHTML("Export.Return"));
        out.print("</a></p>");

        out.print("</form></body></html>");
    }

    protected void handleExport() throws IOException {
        List<DefectTypeStandard> standardsToExport = getStandardsToExport();
        if (standardsToExport.isEmpty()) {
            writeHeader();
            showExportPage();
            return;
        }

        out.print("Content-Type: text/xml\r\n");
        out.print("Content-Disposition: attachment; "
                + "filename=\"defectTypeStandards.dtsxml\"\r\n\r\n");
        out.flush();

        XmlSerializer xml = XMLUtils.getXmlSerializer(true);
        xml.setOutput(outStream, "UTF-8");
        xml.startDocument("UTF-8", null);

        xml.startTag(null, DefectTypeStandard.STANDARDS_TAG);
        xml.attribute(null, "exportTime", XMLUtils.saveDate(new Date()));
        xml.attribute(null, "srcDataset", DashController.getDatasetID());

        for (DefectTypeStandard std : standardsToExport) {
            std.getAsXml(xml, false);
        }

        xml.endTag(null, DefectTypeStandard.STANDARDS_TAG);
        xml.endDocument();
    }

    private List<DefectTypeStandard> getStandardsToExport() {
        String[] selected = (String[]) parameters.get("sel_ALL");
        if (selected == null || selected.length == 0)
            return Collections.EMPTY_LIST;

        List<DefectTypeStandard> result = new ArrayList(selected.length);
        DataRepository data = getDataRepository();
        for (String selectedNum : selected) {
            String selectedName = getParameter("std" + selectedNum);
            DefectTypeStandard selectedStandard = DefectTypeStandard.getByName(
                selectedName, data);
            if (selectedStandard != null)
                result.add(selectedStandard);
        }
        return result;
    }

    private void showImportForm(String errKey) throws IOException {
        writeHTMLHeader();
        out.print("<h2>");
        out.print(resources.getHTML("Import.Title"));
        out.println("</h2>");

        if (errKey != null) {
            out.print("<p style='background-color:#faa; padding:0.5em'>");
            out.print(resources.getHTML(errKey));
            out.println("</p>");
        }

        out.print("<p>");
        out.print(resources.getHTML("Import.Prompt"));
        out.println("<form action='dtsEdit.class' method='POST' "
                + "enctype='multipart/form-data'>");

        out.print("<input type='file' name='file'>");

        out.print("<p><input type=submit name='"+IMPORT+"' value='");
        out.print(resources.getHTML("Next"));
        out.print("'>&nbsp;");

        out.print("<input type=submit name='cancel' value='");
        out.print(resources.getHTML("Cancel"));
        out.print("'>");
        out.print("</form></body></html>");
    }

    private void handleImportForm() throws IOException {
        writeHeader();

        // retrieve the file that was uploaded in the form
        byte[] file = (byte[]) parameters.remove("file_CONTENTS");
        if (file == null || file.length == 0) {
            showImportForm("Import.No_File_Uploaded");
            return;
        }

        // parse the file as XML and find the defect type standards
        NodeList nl = null;
        try {
            Element xml = XMLUtils.parse(new ByteArrayInputStream(file))
                    .getDocumentElement();
            if (DefectTypeStandard.STANDARDS_TAG.equals(xml.getTagName()))
                nl = xml.getElementsByTagName(DefectTypeStandard.STANDARD_TAG);
        } catch (Exception e) {}
        if (nl == null || nl.getLength() == 0) {
            showImportForm("Import.Not_DTS_File");
            return;
        }

        // parse the defect type standards from the XML
        List<DefectTypeStandard> standards = new ArrayList();
        for (int i = 0; i < nl.getLength(); i++)
            standards.add(new DefectTypeStandard((Element) nl.item(i)));

        // display a page asking the user which standards they wish to import
        writeHTMLHeader();
        out.print("<h2>");
        out.print(resources.getHTML("Import.Title"));
        out.println("</h2>\n<p>");
        out.print(resources.getHTML("Import.Confirm_Prompt"));
        out.println("<form action='dtsEdit.class' method='POST'>");

        out.print("<ul style='list-style:none'>");
        for (int i = 0; i < standards.size(); i++) {
            DefectTypeStandard standard = standards.get(i);
            out.print("<li><input type='hidden' name='name"+i+"' value='");
            out.print(XMLUtils.escapeAttribute(standard.getName()));
            out.print("'/><input type='hidden' name='spec"+i+"' value='");
            out.print(XMLUtils.escapeAttribute(standard.getSpec()));
            out.print("'/><input type='checkbox' name='sel' value='"+i+"'/> ");
            out.print(HTMLUtils.escapeEntities(standard.getName()));
            out.print("</li>\n");
        }
        out.print("</ul>");

        out.print("<p><input type=submit name='"+IMPORT_CONFIRM+"' value='");
        out.print(resources.getHTML("Import"));
        out.print("'>&nbsp;");

        out.print("<input type=submit name='cancel' value='");
        out.print(resources.getHTML("Cancel"));
        out.print("'>");
        out.print("</form></body></html>");
    }

    protected void handleImportConfirm() throws IOException {
        // save each of the selected defect type standards
        String[] selected = (String[]) parameters.get("sel_ALL");
        if (selected != null) {
            DataRepository data = getDataRepository();
            for (String selectedNum : selected) {
                String oneName = getParameter("name" + selectedNum);
                String oneSpec = getParameter("spec" + selectedNum);
                DefectTypeStandard.save(oneName, data, oneSpec);
            }
        }
    }

}
