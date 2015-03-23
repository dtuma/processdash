// Copyright (C) 2011 Tuma Solutions, LLC
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

import java.io.File;
import java.io.PrintWriter;
import java.util.Map;
import java.util.prefs.Preferences;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class StudataExporterPrefs {

    private static final Preferences PREFS = Preferences.userNodeForPackage(
        StudataExporterPrefs.class).node("StudataExporter");

    private static final String EXPORT_METHOD_KEY = "method";

    private static final String EXPORT_DIR_KEY = "xmlExportDir";

    private static final String USE_STUDENT_NAME_KEY = "xmlUseStudentName";

    private static final Resources resources = Resources
            .getDashBundle("PspForEng.StudataExporter");


    public enum ExportMethod {
        Unset, Clipboard, Xml
    };

    private ExportMethod method;

    private File targetDir;

    private boolean targetDirValid;

    private boolean includeStudentName;

    private File exportFile;


    public StudataExporterPrefs() {
        // from prefs, read the desired export method
        String methodPref = PREFS.get(EXPORT_METHOD_KEY, "");
        try {
            this.method = ExportMethod.valueOf(methodPref);
        } catch (Exception e) {}
        if (this.method == null)
            this.method = ExportMethod.Unset;

        // from prefs, read the target directory for XML export. If not set,
        // default to the user's home dir.
        String targetDirPref = PREFS.get(EXPORT_DIR_KEY, null);
        if (!hasValue(targetDirPref))
            targetDirPref = System.getProperty("user.home");
        if (!hasValue(targetDirPref))
            targetDirPref = ".";
        this.targetDir = new File(targetDirPref).getAbsoluteFile();
        this.targetDirValid = this.targetDir.isDirectory()
                && this.targetDir.canWrite();

        // from prefs, read whether the filename should include the student's
        // name.  If not set, default to yes.
        String includeNamePref = PREFS.get(USE_STUDENT_NAME_KEY, "true");
        this.includeStudentName = Boolean.parseBoolean(includeNamePref);
    }

    public boolean isValid() {
        switch (method) {

        case Clipboard:
            return true;

        case Xml:
            return targetDirValid;

        case Unset:
        default:
            return false;
        }
    }

    public ExportMethod getMethod() {
        return method;
    }

    public File getTargetFile(String ownerName) {
        StringBuilder filename = new StringBuilder("Studata");
        if (includeStudentName && hasValue(ownerName))
            filename.append("_").append(FileUtils.makeSafe(ownerName.trim()));
        filename.append(".xml");

        this.exportFile = new File(targetDir, filename.toString());
        return this.exportFile;
    }

    public String getHeaderMessage() {
        switch (method) {
        case Clipboard:
            return escHtml(resources.getString("Clipboard.Header"));

        case Xml:
            return escHtml(resources.format("Xml.Header_FMT", exportFile
                    .getPath()));

        default:
            throw new IllegalStateException("No export method known");
        }
    }

    public String getFooterMessage() {
        String html = escHtml(resources.getString(method + ".Footer"));
        html = StringUtils.findAndReplace(html, "\n", "</p>\n<p>");
        return "<p>" + html + "</p>";
    }

    public void writeForm(PrintWriter out, Map parameters) {
        out.print("<html><head>");
        out.print("<title>Student Data Export Options</title><style>\n");
        out.print(".indented { margin-left: 1cm }\n");
        out.print(".error { font-weight: bold; color: #a00 }\n");
        out.print("</style></head><body>\n");
        out.print("<h1>Student Data Export Options</h1>\n");

        out.print("<p>Student data can be exported in various formats. Please "
                + "make a selection from the options below.</p>\n");

        if (parameters.containsKey("mustSelectMethod"))
            out.print("<p class='error'>You must select an export method.</p>");

        out.print("<form method='post' action='studata'>\n");
        out.print("<div class='indented'>\n");

        out.print("<p><input type='radio' name='exportMethod' value='");
        out.print(ExportMethod.Xml.toString());
        if (this.method == ExportMethod.Xml)
            out.print("' checked='checked");
        out.print("'> Export XML Data to File</p>\n");
        out.print("<div class='indented'>\n");
        String targetDirHtml = HTMLUtils.escapeEntities(targetDir.getPath());
        out.print("<p>Export to directory:&nbsp;<input type='text' size='40' "
                + "name='targetDir' value='");
        out.print(targetDirHtml);
        out.print("'></p>\n");
        if (!targetDirValid)
            out.print("<p class='indented error'>The directory '"
                    + targetDirHtml
                    + "' does not exist or is not writable.</p>\n");
        out.print("<p>Include name of student in filename:&nbsp;<input "
                + "type='checkbox' name='includeName'");
        if (includeStudentName)
            out.print(" checked='checked'");
        out.print("></p>\n<p><i>");
        out.print(escHtml(resources.getString("Xml.Explanation")));
        out.print("</i></p>\n</div>\n");

        out.print("<p><input type='radio' name='exportMethod' value='");
        out.print(ExportMethod.Clipboard.toString());
        if (this.method == ExportMethod.Clipboard)
            out.print("' checked='checked");
        out.print("'> Export Excel Data to Clipboard</p>\n");
        out.print("<div class='indented'>\n<p><i>");
        out.print(escHtml(resources.getString("Clipboard.Explanation")));
        out.print("</i></p>\n</div>\n");

        out.print("</div><br/>\n");
        out.print("<input type='submit' name='Save' value='Save'>\n");
        out.print("<input type='submit' name='Cancel' value='Cancel'>\n");

        out.print("</form></body></html>");
    }

    public String saveNewPrefs(Map parameters) {
        if (!parameters.containsKey("Save"))
            return null;

        String newDir = (String) parameters.get("targetDir");
        if (hasValue(newDir))
            PREFS.put(EXPORT_DIR_KEY, newDir.trim());

        boolean includeName = (parameters.containsKey("includeName"));
        PREFS.put(USE_STUDENT_NAME_KEY, Boolean.toString(includeName));

        String newMethod = (String) parameters.get("exportMethod");
        if (!hasValue(newMethod))
            return "mustSelectMethod";

        PREFS.put(EXPORT_METHOD_KEY, newMethod.trim());
        return null;
}

    private static boolean hasValue(String s) {
        return s != null && s.trim().length() > 0;
    }

    private static String escHtml(String s) {
        s = HTMLUtils.escapeEntities(s);
        s = s.replace('[', '<');
        s = s.replace(']', '>');
        return s;
    }
}
