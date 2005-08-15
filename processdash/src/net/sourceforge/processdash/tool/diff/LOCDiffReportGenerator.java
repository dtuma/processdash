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

package net.sourceforge.processdash.tool.diff;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.EscapeString;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTMLUtils;


public abstract class LOCDiffReportGenerator {

    public interface FileToCompare {
        public String getFilename();
        public InputStream getContentsBefore() throws IOException;
        public InputStream getContentsAfter() throws IOException;
    }

    protected static Resources resources = Resources.getDashBundle("LOCDiff");


    protected List languageFilters;
    protected StringBuffer addedTable;
    protected StringBuffer modifiedTable;
    protected StringBuffer deletedTable;
    protected File redlinesTempFile;
    protected PrintWriter redlinesOut;
    protected long base;
    protected long deleted;
    protected long modified;
    protected long added;
    protected long total;
    protected int counter;
    private String outputCharset = "iso-8859-1";
    protected boolean skipIdentical = true;
    protected String options = null;

    protected List progressListeners = new LinkedList();
    private String currentTask = null;
    protected int numTasksCompleted = 0;
    protected int numTasksTotal = 0;


    public LOCDiffReportGenerator() {
        this(HardcodedFilterLocator.getFilters());
    }

    public LOCDiffReportGenerator(List languageFilters) {
        this.languageFilters = languageFilters;
    }

    public String getOutputCharset() {
        return outputCharset;
    }

    public void setOutputCharset(String outputCharset) {
        this.outputCharset = outputCharset;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    protected void updateProgress(String filename) {
        startTask(filename);
    }

    protected void startTask(String description) {
        numTasksCompleted++;
        currentTask = description;

        if (!progressListeners.isEmpty()) {
            ChangeEvent e = new ChangeEvent(this);
            for (Iterator i = progressListeners.iterator(); i.hasNext();) {
                ChangeListener l = (ChangeListener) i.next();
                l.stateChanged(e);
            }
        }
    }


    public String getMessage() {
        return currentTask;
    }

    public int getPercentComplete() {
        return numTasksCompleted * 100 / numTasksTotal;
    }

    public void addChangeListener(ChangeListener l) {
        progressListeners.add(l);
    }

    public void removeChangeListener(ChangeListener l) {
        progressListeners.remove(l);
    }

    protected Collection getFilesToCompare() throws IOException {
        throw new UnsupportedOperationException
            ("The getFilesToCompare() has not been implemented by subclass " +
             getClass().getName());
    }

    public File generateDiffs() throws IOException {
        File outFile = File.createTempFile("diff", ".htm");
        generateDiffs(outFile);
        return outFile;
    }

    public void generateDiffs(File outFile) throws IOException {
        numTasksTotal = 1;
        numTasksCompleted = -1;
        startTask(resources.getString("Dialog.Starting"));

        Collection filesToCompare = getFilesToCompare();
        numTasksTotal += filesToCompare.size();

        generateDiffs(outFile, filesToCompare);
    }


    public void generateDiffs(File outFile, Collection filesToCompare) throws IOException {
        setupForDiff();

        for (Iterator i = filesToCompare.iterator(); i.hasNext();) {
            FileToCompare f = (FileToCompare) i.next();
            compareFile(f);
        }

        generateDiffResults(outFile);
    }

    protected void setupForDiff() throws IOException {
        addedTable = new StringBuffer();
        modifiedTable = new StringBuffer();
        deletedTable = new StringBuffer();
        redlinesTempFile = File.createTempFile("diff", ".txt");
        redlinesTempFile.deleteOnExit();
        redlinesOut = new PrintWriter(new OutputStreamWriter
                (new FileOutputStream(redlinesTempFile), outputCharset));
        base = deleted = modified = added = total = 0;
        counter = 0;
        for (Iterator i = languageFilters.iterator(); i.hasNext();) {
            LanguageFilter f = (LanguageFilter) i.next();
            f.setCharset(outputCharset);
        }
    }


    protected boolean filesAreIdentical, filesAreBinary;
    protected void examineFiles(FileToCompare f) throws IOException {
        filesAreIdentical = true;
        filesAreBinary = false;

        InputStream a = f.getContentsBefore();
        InputStream b = f.getContentsAfter();

        if (a == null || b == null) filesAreIdentical = false;

        Reader inA = null, inB = null;
        try {
            if (a != null) inA = new BufferedReader(new InputStreamReader(a));
            if (b != null) inB = new BufferedReader(new InputStreamReader(b));

            int charA = -2, charB = -2;
            int count = 0;
            while (true) {
                if (inA != null && charA != -1) charA = inA.read();
                if (inB != null && charB != -1) charB = inB.read();
                if (charA != charB) { filesAreIdentical = false; }
                if (charA == 0 || charB == 0) { filesAreBinary = true; }
                if (charA < 0 && charB < 0) break;
                if (filesAreBinary && !filesAreIdentical) break;
            }
        } catch (IOException ioe) {
        } finally {
            try { if (inA != null) inA.close(); } catch (IOException i) {}
            try { if (inB != null) inB.close(); } catch (IOException i) {}
        }
    }

    protected void compareFile(FileToCompare file) throws IOException {
        String filename = file.getFilename();
        updateProgress(filename);
        examineFiles(file);
        if (skipIdentical && filesAreIdentical) return;

        InputStream contentsBefore = file.getContentsBefore();
        InputStream contentsAfter = file.getContentsAfter();

        StringBuffer fileTable;
        String label, htmlName;
        if (contentsBefore == null) {
            fileTable = addedTable;
            label = resources.getHTML("Report.Added");
        } else if (contentsAfter == null) {
            fileTable = deletedTable;
            label = resources.getHTML("Report.Deleted");
        } else {
            fileTable = modifiedTable;
            label = resources.getHTML("Report.Modified");
        }
        htmlName = HTMLUtils.escapeEntities(filename);

        // Don't try to compare binary files.
        if (filesAreBinary) {
            fileTable.append("<tr><td nowrap>").append(htmlName);
            if (fileTable == modifiedTable)
                fileTable.append("</td><td></td><td></td><td></td><td>");
            fileTable.append("</td><td></td><td>")
                .append(resources.getHTML("Report.Binary"))
                .append("</td></tr>\n");
            return;
        }

        // Compare the file.
        String contentsA = getContents(contentsBefore);
        String contentsB = getContents(contentsAfter);
        LOCDiff diff = new LOCDiff(languageFilters, contentsA, contentsB,
                                   filename, options);

        // the two files may not have been byte-for-byte identical, but
        // they also may not contain any significant differences. (For example,
        // the files may have differed in whitespace only.)
        if (skipIdentical &&
            (diff.getDeleted() + diff.getAdded() + diff.getModified() == 0))
            return;

        // keep running metrics.
        base     += diff.getBase();
        deleted  += diff.getDeleted();
        added    += diff.getAdded();
        modified += diff.getModified();
        total    += diff.getTotal();

        // add to the LOC table.
        fileTable.append("<tr><td nowrap><a href='#file")
            .append(counter).append("'>").append(htmlName).append("</a>");
        if (fileTable != addedTable)
            fileTable.append("</td><td>").append(diff.getBase());
        if (fileTable == modifiedTable)
            fileTable.append("</td><td>").append(diff.getDeleted())
                .append("</td><td>").append(diff.getModified())
                .append("</td><td>").append(diff.getAdded());
        if (fileTable != deletedTable)
            fileTable.append("</td><td>").append(diff.getTotal());
        fileTable.append("</td><td>")
            .append(AbstractLanguageFilter.getFilterName(diff.getFilter()))
            .append("</td></tr>\n");

        // add to the redlines output.
        redlinesOut.print("<hr><DIV onMouseOver=\"window.defaultStatus='");
        redlinesOut.print(EscapeString.escape(htmlName, '\\', "\""));
        redlinesOut.print("'\"><h1><a name='file");
        redlinesOut.print(counter++);
        redlinesOut.print("'>");
        redlinesOut.print(label);
        redlinesOut.print(": ");
        redlinesOut.print(htmlName);
        redlinesOut.print("</a></h1>");
        diff.displayHTMLRedlines(redlinesOut);
        redlinesOut.print("</DIV>\n\n\n");

        diff.dispose();
    }

    protected String getContents(InputStream in) throws IOException {
        String results = "";
        if (in != null )
            results = new String(FileUtils.slurpContents(in, true));
        return results;
    }


    protected static final String SCRIPT = "<SCRIPT LANGUAGE=\"JavaScript\">\n" +
            "    function updateFilename(e) {\n" +
            "      for (var i = document.anchors.length;  i > 0; ) {\n" +
            "        i--;\n" +
            "        if (document.anchors[i].y < e.pageY) {\n" +
            "            window.defaultStatus = document.anchors[i].text;\n" +
            "            return;\n" +
            "        }\n" +
            "      }\n" +
            "      window.defaultStatus = \"${Report.Metrics}\";\n" +
            "    }\n\n" +
            "    if (navigator.appName == \"Netscape\") {\n" +
            "        window.captureEvents(Event.MOUSEMOVE);\n" +
            "        window.onMouseMove=updateFilename;\n" +
            "    }\n" +
            "</SCRIPT>\n";


    protected void intlWrite(BufferedWriter out, String text) throws IOException {
        out.write(resources.interpolate(text, HTMLUtils.ESC_ENTITIES));
    }


    protected void generateDiffResults(File outFile) throws IOException {
        FileOutputStream outStream = new FileOutputStream(outFile);
        BufferedWriter out = new BufferedWriter
            (new OutputStreamWriter(outStream, outputCharset));

        intlWrite(out,
                  "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0//EN\">\n" +
                  "<html><head><title>${Report.Title}</title>\n" +
                  "<meta http-equiv=\"Content-Type\""+
                  " content=\"text/html; charset=" +
                  outputCharset + "\">\n" +
                  SCRIPT + "<style>\n");
        out.write(LOCDiff.getCssText());
        intlWrite(out,
                  "</style></head>\n" +
                  "<body bgcolor='#ffffff'>\n" +
                  "<div onMouseOver=\"window.defaultStatus=" +
                  "'${Report.Metrics}'\">\n");

        if (addedTable.length() > 0) {
            intlWrite(out,
                      "<table border><tr>" +
                      "<th>${Report.Added_Files}</th>"+
                      "<th>${Report.Added_Abbr}</th>" +
                      "<th>${Report.File_Type}</th></tr>");
            out.write(addedTable.toString());
            out.write("</table><br><br>");
        }
        if (modifiedTable.length() > 0) {
            intlWrite(out,
                      "<table border><tr>" +
                      "<th>${Report.Modified_Files}</th>" +
                      "<th>${Report.Base_Abbr}</th>" +
                      "<th>${Report.Deleted_Abbr}</th>" +
                      "<th>${Report.Modified_Abbr}</th>" +
                      "<th>${Report.Added_Abbr}</th>" +
                      "<th>${Report.Total_Abbr}</th>" +
                      "<th>${Report.File_Type}</th></tr>");
            out.write(modifiedTable.toString());
            out.write("</table><br><br>");
        }
        if (deletedTable.length() > 0) {
            intlWrite(out,
                      "<table border><tr>" +
                      "<th>${Report.Deleted_Files}</th>"+
                      "<th>${Report.Deleted_Abbr}</th>" +
                      "<th>${Report.File_Type}</th></tr>");
            out.write(deletedTable.toString());
            out.write("</table><br><br>");
        }

        out.write("<table name=METRICS BORDER>\n");
        if (modifiedTable.length() > 0 || deletedTable.length() > 0) {
            intlWrite(out, "<tr><td>${Report.Base}:&nbsp;</td><td>");
            out.write(Long.toString(base));
            intlWrite(out, "</td></tr>\n<tr><td>${Report.Deleted}:&nbsp;</td><td>");
            out.write(Long.toString(deleted));
            intlWrite(out, "</td></tr>\n<tr><td>${Report.Modified}:&nbsp;</td><td>");
            out.write(Long.toString(modified));
            intlWrite(out, "</td></tr>\n<tr><td>${Report.Added}:&nbsp;</td><td>");
            out.write(Long.toString(added));
            intlWrite(out, "</td></tr>\n<tr><td>${Report.New_And_Changed}:&nbsp;</td><td>");
            out.write(Long.toString(added+modified));
            out.write("</td></tr>\n");
        }
        intlWrite(out, "<tr><td>${Report.Total}:&nbsp;</td><td>");
        out.write(Long.toString(total));
        out.write("</td></tr>\n</table></div>");

        // copy redlines output from the temp file to the final file
        redlinesOut.close();
        out.flush();
        InputStream redlines = new FileInputStream(redlinesTempFile);
        byte [] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = redlines.read(buffer)) != -1)
            outStream.write(buffer, 0, bytesRead);

        // finish the HTML
        outStream.write("</BODY></HTML>".getBytes());
        outStream.close();
    }

    /** Look for command line options, and consolodate them into the first
     * position of the String array.
     * 
     * A command line option are items starting with a dash "-" or a plus "+".
     * If no command line arguments are found, the first position of the result
     * array will contain null.
     * 
     * @param args an array of arguments, as would be passed to main()
     */
    public static String[] collectOptions(String[] args) {
        String options = "";
        List argsOut = new LinkedList();
        for (int i = 0; args != null && i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-") || arg.startsWith("+"))
                options = options + " " + arg;
            else
                argsOut.add(arg);
        }

        if (options.length() > 0)
            options = options.substring(1);
        else
            options = null;
        argsOut.add(0, options);
        return (String[]) argsOut.toArray(new String[argsOut.size()]);
    }
}
