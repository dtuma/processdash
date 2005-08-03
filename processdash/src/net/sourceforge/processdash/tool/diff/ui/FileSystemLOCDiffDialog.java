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

package net.sourceforge.processdash.tool.diff.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.diff.AbstractLanguageFilter;
import net.sourceforge.processdash.tool.diff.HardcodedFilterLocator;
import net.sourceforge.processdash.tool.diff.LOCDiff;
import net.sourceforge.processdash.tool.diff.LOCDiffReportGenerator;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.util.EscapeString;
import net.sourceforge.processdash.util.HTMLUtils;


public class FileSystemLOCDiffDialog extends LOCDiffReportGenerator
        implements ActionListener, Runnable {


    protected JFrame frame;
    protected JTextField fileA, fileB;
    protected JButton browseA, browseB, compareButton, closeButton;
    protected JButton cancelButton = null;
    protected static JFileChooser fileChooser = null;

    public FileSystemLOCDiffDialog(List languageFilters) {
        super(languageFilters);
    }

    public void showDialog() {
        frame = new JFrame(resources.getString("Dialog.Window_Title"));
        frame.setIconImage(DashboardIconFactory.getWindowIconImage());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

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

    protected void dontStretchVertically(JComponent c) {
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

    protected void beep(JTextField field) {
        java.awt.Toolkit.getDefaultToolkit().beep();
        if (field != null) {
            field.requestFocus();
            field.selectAll();
        }
    }

    File compareA, compareB;
    protected JLabel currentTaskLabel;
    protected volatile boolean userCancelled;
    private class UserCancel extends RuntimeException {}
    protected JDialog workingDialog = null;

    protected boolean validateInput() {
        if (fileA == null && fileB == null)
            return true;

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
            if (workingDialog != null)
                workingDialog.hide();
            throw new UserCancel();
        }

        if (currentTaskLabel != null)
            currentTaskLabel.setText(filename);
    }

    public void run() {
        try {
            File outFile = File.createTempFile("diff", ".htm");
            if (workingDialog != null)
                outFile.deleteOnExit();

            generateDiffs(outFile, getFilesToCompare());
            Browser.launch(outFile.toURL().toString());

            if (workingDialog != null)
                workingDialog.hide();

        } catch (IOException ioe) {
            beep(null);
            ioe.printStackTrace();
        } catch (UserCancel uc) {}
    }



    private Collection getFilesToCompare() {
        if (compareB.isDirectory()) {
            TreeSet result = new TreeSet();
            listAllFiles(result, compareA, compareA);
            listAllFiles(result, compareB, compareB);
            return result;
        } else {
            skipIdentical = false;
            return Collections.singleton
                (new SimpleFileComparison(compareA, compareB));
        }
    }

    private void listAllFiles(TreeSet result, File dir, File baseDir) {
        String basePath = "";
        if (baseDir != null)
            basePath = baseDir.getAbsolutePath() + File.separator;
        listAllFiles(result, dir, basePath);
    }

    private void listAllFiles(TreeSet result, File dir, String basePath) {
        if (!isDir(dir))
            return;
        File [] files = dir.listFiles();
        for (int i = files.length;   i-- > 0; ) {
            String filename = files[i].getName();
            if (!".".equals(filename) && !"..".equals(filename)) {
                if (isFile(files[i])) {
                    filename = files[i].getAbsolutePath();
                    if (filename.startsWith(basePath))
                        filename = filename.substring(basePath.length());
                    result.add(new FileInDirectory(filename));
                } else if (isDir(files[i])) {
                    listAllFiles(result, files[i], basePath);
                }
            }
        }
    }


    private class SimpleFileComparison implements FileToCompare {
        private File fileA, fileB;

        public SimpleFileComparison(File fileA, File fileB) {
            this.fileA = fileA;
            this.fileB = fileB;
        }

        public String getFilename() {
            return fileB.getName();
        }

        public InputStream getContentsBefore() throws IOException {
            return openFile(fileA);
        }

        public InputStream getContentsAfter() throws IOException {
            return openFile(fileB);
        }

        private InputStream openFile(File f) throws IOException {
            if (isFile(f))
                return new FileInputStream(f);
            else
                return null;
        }
    }

    private class FileInDirectory implements FileToCompare, Comparable {

        private String filename;

        public FileInDirectory(String filename) {
            this.filename = filename;
        }

        public String getFilename() {
            return filename;
        }

        public InputStream getContentsBefore() throws IOException {
            return openFile(compareA);
        }

        public InputStream getContentsAfter() throws IOException {
            return openFile(compareB);
        }

        private InputStream openFile(File dir) throws IOException {
            if (!isDir(dir))
                return null;
            File file = new File(dir, filename);
            if (isFile(file))
                return new FileInputStream(file);
            else
                return null;
        }


        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof FileInDirectory)) return false;
            FileInDirectory that = (FileInDirectory) obj;
            return (filename.equals(that.filename));
        }
        public int hashCode() {
            return filename.hashCode();
        }

        public int compareTo(Object o) {
            return filename.compareTo(((FileInDirectory)o).filename);
        }
    }


    protected boolean isFile(File f) { return (f != null && f.isFile()); }
    protected boolean isDir(File f)  { return (f != null && f.isDirectory()); }


    public static void main(String[] args) {
        FileSystemLOCDiffDialog dlg = new FileSystemLOCDiffDialog
            (HardcodedFilterLocator.getFilters());

        if (args.length == 0) {
            dlg.showDialog();

        } else if (args.length == 1) {
            dlg.compareA = null;
            dlg.compareB = new File(args[0]);
            dlg.run();

        } else if (args.length == 2) {
            dlg.compareA = new File(args[0]);
            dlg.compareB = new File(args[1]);
            dlg.run();

        } else {
            printUsage();

        }
    }

    protected static void printUsage() {
        System.out.println("Usage: java " +
                FileSystemLOCDiffDialog.class.getName() + " [fileA] fileB");
    }
}
