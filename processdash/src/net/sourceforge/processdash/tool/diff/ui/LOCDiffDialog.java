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

package net.sourceforge.processdash.tool.diff.ui;


import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.tool.diff.AbstractLanguageFilter;
import net.sourceforge.processdash.tool.diff.LOCDiff;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.EscapeString;
import net.sourceforge.processdash.util.HTMLUtils;



public class LOCDiffDialog extends TinyCGIBase
    implements ActionListener, Runnable {

    static Resources resources = Resources.getDashBundle("LOCDiff");

    public LOCDiffDialog() {}

    /** Write the CGI header. */
    protected void writeHeader() {
        out.print("Expires: 0\r\n");
        super.writeHeader();
    }

    /** Generate CGI script output. */
    protected void writeContents() throws IOException {
        DashController.checkIP(env.get("REMOTE_ADDR"));
        new LOCDiffDialog(getTinyWebServer());
        DashController.printNullDocument(out);
    }

    protected JFrame frame;
    protected WebServer webServer;
    protected JTextField fileA, fileB;
    protected JButton browseA, browseB, compareButton, closeButton;
    protected JButton cancelButton = null;
    protected static JFileChooser fileChooser = null;

    StringBuffer addedTable, modifiedTable, deletedTable;
    File redlinesTempFile;
    PrintWriter redlinesOut;
    long base, deleted, modified, added, total;
    int counter;



    public LOCDiffDialog(WebServer webServer) {
        this.webServer = webServer;

        frame = new JFrame(resources.getString("Dialog.Window_Title"));
        frame.setIconImage(DashboardIconFactory.getWindowIconImage());

        Box vBox = Box.createVerticalBox();
        Box hBox = Box.createHorizontalBox();
        hBox.add(new JLabel(resources.getString("Dialog.File_A_Prompt")));
        hBox.add(Box.createHorizontalStrut(150));
        hBox.add(Box.createHorizontalGlue());
        vBox.add(hBox);

        hBox = Box.createHorizontalBox();
        hBox.add(fileA = new JTextField());
        dontStretchVertically(fileA);
        hBox.add(browseA = new JButton(resources.getDlgString("Browse")));
        browseA.addActionListener(this);
        vBox.add(hBox);

        vBox.add(Box.createVerticalStrut(5));
        vBox.add(Box.createVerticalGlue());
        hBox = Box.createHorizontalBox();
        hBox.add(new JLabel(resources.getString("Dialog.File_B_Prompt")));
        hBox.add(Box.createHorizontalGlue());
        vBox.add(hBox);

        hBox = Box.createHorizontalBox();
        hBox.add(fileB = new JTextField());
        dontStretchVertically(fileB);
        hBox.add(browseB = new JButton(resources.getDlgString("Browse")));
        browseB.addActionListener(this);
        vBox.add(hBox);

        vBox.add(Box.createVerticalStrut(5));
        vBox.add(Box.createVerticalGlue());
        hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalGlue());
        hBox.add(compareButton = new JButton(resources.getString("Dialog.Compare")));
        compareButton.addActionListener(this);
        hBox.add(Box.createHorizontalGlue());
        hBox.add(closeButton = new JButton(resources.getString("Close")));
        closeButton.addActionListener(this);
        hBox.add(Box.createHorizontalGlue());
        vBox.add(hBox);

        frame.getContentPane().add(vBox);
        frame.pack();
        frame.show();
    }

    private void dontStretchVertically(JComponent c) {
        Dimension size = c.getPreferredSize();
        size.width = 1000;
        c.setMaximumSize(size);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == closeButton)
            closeWindow();
        else if (e.getSource() == compareButton)
            compare();
        else if (e.getSource() == browseA)
            browseFile(fileA);
        else if (e.getSource() == browseB)
            browseFile(fileB);
        else if (cancelButton != null && e.getSource() == cancelButton)
            userCancelled = true;
    }

    public void closeWindow() {
        frame.setVisible(false);
        frame.dispose();
    }

    private void beep(JTextField field) {
        java.awt.Toolkit.getDefaultToolkit().beep();
        if (field != null) {
            field.requestFocus();
            field.selectAll();
        }
    }

    File compareA, compareB;
    private JLabel currentTaskLabel;
    private volatile boolean userCancelled;
    private class UserCancel extends RuntimeException {}
    private JDialog workingDialog = null;

    private boolean validateInput() {

        /* Valid input:
         *  - A is blank, B is a file -> count compareB.
         *  - A is blank, B is a directory -> count directoryB.
         *  - A is a file, B is a file -> compare them
         *  - A is a directory, B is a directory -> compare them
         * all other permutations are errors.
         */

        String filenameA = fileA.getText();
        String filenameB = fileB.getText();
        if (filenameB.length() == 0) {
            beep(fileB);
            return false;
        }

        compareA = null;
        if (filenameA.length() != 0) {
            compareA = new File(filenameA);
            if (!compareA.exists()) {
                beep(fileA);
                return false;
            }
        }

        compareB = new File(filenameB);
        if (!compareB.exists()) {
            beep(fileB);
            return false;
        }

        if (compareA != null &&
            (compareA.isDirectory() != compareB.isDirectory())) {
            beep(fileA);
            return false;
        }

        return true;
    }

    public void compare() {
        if (!validateInput()) return;

        workingDialog = new JDialog
            (frame, resources.getString("Dialog.Comparing"), true);
        Box vBox = Box.createVerticalBox();
        vBox.add(currentTaskLabel = new JLabel
            (resources.getString("Dialog.Starting")));
        Dimension d = currentTaskLabel.getPreferredSize();
        d.width = 200;
        currentTaskLabel.setPreferredSize(d);
        currentTaskLabel.setMinimumSize(d);
        currentTaskLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        vBox.add(Box.createVerticalStrut(3));
        cancelButton = new JButton(resources.getString("Cancel"));
        cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelButton.addActionListener(this);
        vBox.add(cancelButton);
        workingDialog.getContentPane().add(vBox);
        userCancelled = false;

        Thread t = new Thread(this);
        t.start();
        workingDialog.pack();
        workingDialog.show();   // this will block until done.
    }

    public void updateProgress(String filename) {
        if (userCancelled) {
            workingDialog.hide();
            throw new UserCancel();
        }

        if (currentTaskLabel != null)
            currentTaskLabel.setText(filename);
    }

    public void run() {
        try {
            generateDiffs();
            workingDialog.hide();
        } catch (UserCancel uc) {}
    }

    protected void generateDiffs() {
        try {
            setupForDiff();
        } catch (IOException ioe) { beep(null); return; }

        if (compareB.isDirectory())
            compareDirectories(compareA, compareB, compareB.getPath());
        else
            compareFile(compareA, compareB, false,
                        compareB.getParentFile().getPath());

        try {
            displayDiffResults();
        } catch (IOException ioe) {}
    }

    protected void browseFile(JTextField dest) {
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode
                (JFileChooser.FILES_AND_DIRECTORIES);
        }

        if (fileChooser.showOpenDialog(frame) ==
            JFileChooser.APPROVE_OPTION) {
            File f = fileChooser.getSelectedFile();
            if (f != null)
                dest.setText(f.getPath());
        }
    }

    private void setupForDiff() throws IOException {
        addedTable = new StringBuffer();
        modifiedTable = new StringBuffer();
        deletedTable = new StringBuffer();
        redlinesTempFile = File.createTempFile("diff", ".txt");
        redlinesTempFile.deleteOnExit();
        redlinesOut = new PrintWriter(new OutputStreamWriter
                (new FileOutputStream(redlinesTempFile), getDefaultCharset()));
        base = deleted = modified = added = total = 0;
    }

    private void compareDirectories(File dirA, File dirB, String basePath) {
        TreeSet filenames = new TreeSet();
        addFiles(filenames, dirA);
        addFiles(filenames, dirB);

        Iterator i = filenames.iterator();
        String filename;
        while (i.hasNext()) {
            filename = (String) i.next();
            File fileA = makeFile(dirA, filename);
            File fileB = makeFile(dirB, filename);
            if (isFile(fileA) || isFile(fileB))
                compareFile(fileA, fileB, true, basePath);
            if (isDir(fileA) || isDir(fileB))
                compareDirectories(fileA, fileB, basePath);
        }
    }
    private boolean isFile(File f) { return (f != null && f.isFile()); }
    private boolean isDir(File f)  { return (f != null && f.isDirectory()); }

    private boolean filesAreIdentical, filesAreBinary;
    private void examineFiles(File a, File b) {
        filesAreIdentical = true;
        filesAreBinary = false;

        if (a == null || b == null) filesAreIdentical = false;
        else if (a.length() != b.length()) filesAreIdentical = false;

        Reader inA = null, inB = null;
        try {
            if (a != null) inA = new BufferedReader(new FileReader(a));
            if (b != null) inB = new BufferedReader(new FileReader(b));

            int charA = -2, charB = -2;
            int count = 0;
            while (true) {
                if (inA != null && charA != -1) charA = inA.read();
                if (inB != null && charB != -1) charB = inB.read();
                if (charA != charB) filesAreIdentical = false;
                if (charA == 0 || charB == 0) {
                    filesAreBinary = true; break; }
                if ((charA == -1 && charB == -1) ||
                    (count++ > 4096 && !filesAreIdentical)) break;
            }
        } catch (IOException ioe) {
        } finally {
            try { if (inA != null) inA.close(); } catch (IOException i) {}
            try { if (inB != null) inB.close(); } catch (IOException i) {}
        }
    }
    private void addFiles(Set filenames, File dir) {
        if (!isDir(dir)) return;
        String [] files = dir.list();
        for (int i = files.length;   i-- > 0; )
            if (!".".equals(files[i]) && !"..".equals(files[i]))
                filenames.add(files[i]);
    }
    private File makeFile(File dir, String name) {
        if (!isDir(dir)) return null;
        return new File(dir, name);
    }

    private void compareFile(File fileA, File fileB,
                             boolean skipIdentical, String basePath) {
        String filename = null;
        if (fileB != null)
            filename = fileB.getPath();
        else if (fileA != null)
            filename = fileA.getPath();
        if (filename != null && basePath != null &&
            filename.startsWith(basePath)) {
            filename = filename.substring(basePath.length());
            if (filename.startsWith(File.separator))
                filename = filename.substring(1);
        }
        updateProgress(filename);
        if (!isFile(fileA)) fileA = null;
        if (!isFile(fileB)) fileB = null;

        examineFiles(fileA, fileB);
        if (skipIdentical && filesAreIdentical) return;

        StringBuffer fileTable;
        String label, htmlName;
        if (fileA == null) {
            fileTable = addedTable;
            label = resources.getHTML("Report.Added");
        } else if (fileB == null) {
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
        String contentsA = getContents(fileA);
        String contentsB = getContents(fileB);
        LOCDiff diff = new LOCDiff(webServer, contentsA, contentsB,
                                   filename, null);

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
        redlinesOut.print("'\"><h1>");
        redlinesOut.print(label);
        redlinesOut.print(": <a name='file");
        redlinesOut.print(counter++);
        redlinesOut.print("'>");
        redlinesOut.print(htmlName);
        redlinesOut.print("</a></h1>");
        diff.displayHTMLRedlines(redlinesOut);
        redlinesOut.print("</DIV>\n\n\n");

        diff.dispose();
    }
    private String getContents(File f) {
        String results = "";
        if (f != null && f.isFile()) try {
            FileInputStream in = new FileInputStream(f);
            results = new String(WebServer.slurpContents(in, true));
        } catch (IOException e) { }
        return results;
    }

    private static final String SCRIPT =
        "<SCRIPT LANGUAGE=\"JavaScript\">\n" +
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


    private void intlWrite(BufferedWriter out, String text) throws IOException {
        out.write(resources.interpolate(text, HTMLUtils.ESC_ENTITIES));
    }

    private void displayDiffResults() throws IOException {
        File outFile = File.createTempFile("diff", ".htm");
        outFile.deleteOnExit();
        FileOutputStream outStream = new FileOutputStream(outFile);
        BufferedWriter out = new BufferedWriter
            (new OutputStreamWriter(outStream, getDefaultCharset()));

        intlWrite(out,
                  "<html><head><title>${Report.Title}</title>\n" +
                  "<meta http-equiv=\"Content-Type\""+
                  " content=\"text/html; charset=" +
                  getDefaultCharset() + "\">\n" +
                  SCRIPT + "</head>\n" +
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
        Browser.launch(outFile.toURL().toString());
    }


}
