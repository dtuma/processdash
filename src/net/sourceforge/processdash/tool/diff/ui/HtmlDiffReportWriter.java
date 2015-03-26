// Copyright (C) 2001-2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.diff.ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.diff.AbstractLanguageFilter;
import net.sourceforge.processdash.tool.diff.engine.AccountingType;
import net.sourceforge.processdash.tool.diff.engine.DiffAdapter;
import net.sourceforge.processdash.tool.diff.engine.DiffEvent;
import net.sourceforge.processdash.tool.diff.engine.DiffFragment;
import net.sourceforge.processdash.tool.diff.engine.DiffResult;
import net.sourceforge.processdash.tool.diff.engine.FileToAnalyzeSubtitled;
import net.sourceforge.processdash.tool.diff.engine.LocDiffUtils;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.TempFileFactory;

public class HtmlDiffReportWriter extends DiffAdapter {

    private File reportFile;

    private String encoding;

    private boolean noRedlines;

    private boolean launchBrowser;

    private int tabWidth = 8;

    private File redlinesTmpFile;

    private OutputStream outStream;

    private PrintWriter out;

    private List<DiffResult> files;

    private Set<AccountingType> typesSeen;

    private int[] locCounts;

    public HtmlDiffReportWriter() throws IOException {
        this(null);
    }

    public HtmlDiffReportWriter(String filename) throws IOException {
        this(StringUtils.hasValue(filename)
                    ? new File(filename)
                    : TempFileFactory.get().createTempFile("diff", ".htm"),
             ENCODING);
        setLaunchBrowser(StringUtils.hasValue(filename) == false);
    }

    public HtmlDiffReportWriter(File reportFile, String encoding)
            throws IOException {
        this.reportFile = reportFile;
        this.encoding = encoding;
        this.noRedlines = false;
        this.launchBrowser = false;

        this.redlinesTmpFile = TempFileFactory.get().createTempFile("diff",
            ".txt");
        this.outStream = new FileOutputStream(redlinesTmpFile);
        this.out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                outStream, encoding)));

        this.files = new ArrayList<DiffResult>();
        this.typesSeen = new HashSet();
        this.locCounts = new int[AccountingType.values().length];
    }

    public boolean isNoRedlines() {
        return noRedlines;
    }

    public void setNoRedlines(boolean noRedlines) {
        this.noRedlines = noRedlines;
    }

    public boolean isLaunchBrowser() {
        return launchBrowser;
    }

    public void setLaunchBrowser(boolean launchBrowser) {
        this.launchBrowser = launchBrowser;
    }

    public int getTabWidth() {
        return tabWidth;
    }

    public void setTabWidth(int tabWidth) {
        this.tabWidth = tabWidth;
    }

    public File getReportFile() {
        return reportFile;
    }

    public void fileAnalysisFinished(DiffEvent e) {
        DiffResult diff = e.getDiffResult();
        if (diff == null)
            return;

        // record this file in the list of changes.
        int pos = files.size();
        files.add(diff);
        typesSeen.add(diff.getChangeType());

        // keep a running total of the LOC counts.
        for (AccountingType type : AccountingType.values())
            locCounts[type.ordinal()] += diff.getLocCount(type);

        // write redlines, if applicable
        if (diff.hasRedlines() && noRedlines == false) {
            out.write("<hr>\n");
            writeFileRedlines(diff, getFileAnchor(diff, pos));
        }
    }

    private void writeFileRedlines(DiffResult diff, String anchorName) {
        String filename = HTMLUtils.escapeEntities(diff.getFile().getFilename());
        out.write("<div id=\"" + anchorName + "\" title=\"" + filename + "\">");

        out.write("<h2>");
        out.write(filename);
        out.write("</h2>\n");

        if (diff.getFile() instanceof FileToAnalyzeSubtitled) {
            FileToAnalyzeSubtitled st = (FileToAnalyzeSubtitled) diff.getFile();
            String subtitle = st.getSubtitle();
            if (StringUtils.hasValue(subtitle)) {
                out.write("<p><i>");
                out.write(HTMLUtils.escapeEntities(subtitle));
                out.write("</i></p>\n");
            }
        }

        int tabWidth = getTabWidth(diff.getOptions());

        out.println("<table class='locDiff' cellpadding=0 cellspacing=0 border=0>");

        for (DiffFragment f : diff.getRedlines()) {
            out.write(ROW_BEGIN[f.type.ordinal()]);

            String text = f.text;
            if (text.endsWith("\n"))
                text = text.substring(0, text.length()-1);
            String html = LocDiffUtils.fixupCodeForHtml(text, tabWidth);
            if (text.startsWith("\n"))
                out.print("&nbsp;");
            out.print(html);
            if (text.endsWith("\n"))
                out.print("&nbsp;");

            out.println("</pre></td></tr>");
        }

        out.println("</table></div>");
    }
    private static final String[] ROW_BEGIN = {
        // Base
        "<tr><td>&nbsp;</td><td><pre>",

        // Deleted
        "<tr><td class='locDelHdr'>&nbsp;</td>"+
        "<td class='locDelBody'><pre>",

        // Modified - not yet used/implemented
        "",

        // Added
        "<tr><td class='locAddHdr'>&nbsp;</td>"+
            "<td class='locAddBody'><pre>" };

    private int getTabWidth(String options) {
        String tabWidthOption = LocDiffUtils.getOption(options, "-tabWidth");
        if (tabWidthOption != null) {
            try {
                return Integer.parseInt(tabWidthOption);
            } catch (NumberFormatException nfe) {}
        }
        return this.tabWidth;
    }


    public void analysisFinished(DiffEvent e) {
        try {
            finishReport();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void finishReport() throws IOException {
        // close the writer that has been capturing redlines.
        out.close();

        // open a new writer for the final report.
        outStream = new FileOutputStream(reportFile);
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                outStream, encoding)));

        writeHtmlHeader();
        writeMetrics();
        copyRedlines();
        writeHtmlFooter();

        out.close();

        if (launchBrowser)
            Browser.launch(getReportFile().toURI().toURL().toString());
    }

    private void writeHtmlHeader() throws IOException {
        out.write(HTML_STRICT_DOCTYPE);
        out.write("<html><head>\n<title>");
        out.write(resources.getHTML("Report.Title"));
        out.write("</title>\n");
        out.write("<meta http-equiv=\"Content-Type\""
                + " content=\"text/html; charset=" + encoding + "\">\n");
        writeCssHtml(out);
        out.write("</head><body>\n<h1>");
        out.write(resources.getHTML("Report.Title"));
        out.write("</h1>\n");
    }

    private void writeCssHtml(Object write) throws IOException {
        Reader in = new BufferedReader(new InputStreamReader(
                HtmlDiffReportWriter.class
                        .getResourceAsStream("HtmlDiffReportWriter.css"),
                "UTF-8"));

        int c;
        while ((c = in.read()) != -1)
            out.write(c);
    }

    private void writeMetrics() {
        out.write("<div class='metrics'>");

        writeMetricsTable(AccountingType.Added);
        writeMetricsTable(AccountingType.Modified);
        writeMetricsTable(AccountingType.Deleted);
        writeMetricsTable(AccountingType.Base);
        writeSummaryTable();

        out.write("</div>");
    }

    private void writeMetricsTable(AccountingType type) {
        if (!typesSeen.contains(type))
            return;

        out.write("<table border>");

        // write the header row
        out.write("<tr><th>");
        out.write(resources.getHTML("Report." + RESOURCE_KEYS[type.ordinal()]
                + "_Files"));
        out.write("</th>");
        for (AccountingType col : AccountingType.values()) {
            if (SHOW_COLUMNS[type.ordinal()][col.ordinal()]) {
                out.write("<th>");
                out.write(resources.getHTML("Report." + col.toString() + "_Abbr"));
                out.write("</th>");
            }
        }
        out.write("<th>");
        out.write(resources.getHTML("Report.File_Type"));
        out.write("</th></tr>\n");

        // write a row for each file
        for (int i = 0; i < files.size();  i++) {
            DiffResult file = files.get(i);
            if (file.getChangeType() == type) {
                out.write("<tr><td>");
                boolean hasRedlines = file.hasRedlines() && !noRedlines;
                if (hasRedlines)
                    out.write("<a href=\"#" + getFileAnchor(file, i) + "\">");
                out.write(HTMLUtils.escapeEntities(file.getFile().getFilename()));
                if (hasRedlines)
                    out.write("<span title=\""
                            + resources.getHTML("Report.SET_Drag_Tooltip")
                            + "\" class=\"setHelp\">&nbsp;</span></a>");
                out.write("</td>");
                for (AccountingType col : AccountingType.values()) {
                    if (SHOW_COLUMNS[type.ordinal()][col.ordinal()]) {
                        out.write("<td>");
                        out.write(Integer.toString(file.getLocCount(col)));
                        out.write("</td>");
                    }
                }
                out.write("<td>");
                out.write(HTMLUtils.escapeEntities(AbstractLanguageFilter
                        .getFilterName(file.getLanguageFilter())));
                out.write("</td></tr>\n");
            }
        }

        out.write("</table>\n");
    }

    private void writeSummaryTable() {
        out.write("<table border>\n");
        if (typesSeen.contains(AccountingType.Modified)
                || typesSeen.contains(AccountingType.Deleted)) {
            writeSummaryRow(AccountingType.Base);
            writeSummaryRow(AccountingType.Deleted);
            writeSummaryRow(AccountingType.Modified);
            writeSummaryRow(AccountingType.Added);
            writeSummaryRow("New_And_Changed",
                locCounts[AccountingType.Added.ordinal()] +
                locCounts[AccountingType.Modified.ordinal()]);
        }
        writeSummaryRow(AccountingType.Total);
        out.write("</table>");
    }

    private void writeSummaryRow(AccountingType t) {
        writeSummaryRow(t.toString(), locCounts[t.ordinal()]);
    }

    private void writeSummaryRow(String resKey, int loc) {
        out.write("<tr><td>");
        out.write(resources.getHTML("Report." + resKey));
        out.write(":&nbsp;</td><td>");
        out.write(Integer.toString(loc));
        out.write("</td></tr>\n");
    }


    private void copyRedlines() throws IOException {
        out.flush();
        if (noRedlines == false)
            FileUtils.copyFile(redlinesTmpFile, outStream);
        redlinesTmpFile.delete();
    }

    private void writeHtmlFooter() {
        out.write("</body></html>");
    }

    private String getFileAnchor(DiffResult r, int pos) {
        StringBuilder result = new StringBuilder();
        result.append("diffFile:");
        for (AccountingType t : AccountingType.values()) {
            result.append(t.toString().charAt(0));
            result.append(r.getLocCount(t));
            result.append(":");
        }
        encodeForAnchor(result, r.getFile().getFilename());
        return result.toString();
    }

    private void encodeForAnchor(StringBuilder dest, String s) {
        try {
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (isAcceptableIdChar(c)) {
                    dest.append(c);
                } else if (c < 128) {
                    appendAnchorEncodedByte(dest, (byte) c);
                } else {
                    for (byte b : s.substring(i, i+1).getBytes("UTF-8"))
                        appendAnchorEncodedByte(dest, b);
                }
            }
        } catch (UnsupportedEncodingException e) {
            // can't happen...java guarantees the availability of UTF-8
        }
    }

    private static boolean isAcceptableIdChar(char c) {
        if ('A' <= c && c <= 'Z') return true;
        if ('a' <= c && c <= 'z') return true;
        if ('0' <= c && c <= '9') return true;
        if (c == '-' || c == '.') return true;
        return false;
    }
    private static void appendAnchorEncodedByte(StringBuilder dest, byte b) {
        dest.append('_');
        String hex = Integer.toHexString(b);
        if (hex.length() == 1)
            dest.append('0');
        dest.append(hex);
    }


    private static Resources resources = Resources.getDashBundle("LOCDiff");

    private static final String HTML_STRICT_DOCTYPE =
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\"\n" +
        "    \"http://www.w3.org/TR/html4/strict.dtd\">\n";

    private static final String ENCODING = "UTF-8";

    private static final boolean[][] SHOW_COLUMNS = {
        { true,  false, false, false, false }, // base files
        { true,  false, false, false, false }, // deleted files
        { true,  true,  true,  true,  true  }, // modified files
        { false, false, false, false, true  }, // added files
    };

    private static final String[] RESOURCE_KEYS = {
        "Unchanged", "Deleted", "Modified", "Added"
    };

}
