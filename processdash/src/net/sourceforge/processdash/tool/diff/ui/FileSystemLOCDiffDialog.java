// Copyright (C) 2005-2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentListener;

import net.sourceforge.processdash.tool.diff.HardcodedFilterLocator;
import net.sourceforge.processdash.tool.diff.impl.FileSystemLOCDiff;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.lib.ProgressDialog;


public class FileSystemLOCDiffDialog extends FileSystemLOCDiff
        implements ActionListener, ProgressDialog.CancellableTask {


    protected JFrame frame;
    private JTextField fileA, fileB;
    private JButton browseA, browseB, compareButton, closeButton;
    private JCheckBox countIdentical;
    private static JFileChooser fileChooser = null;

    public FileSystemLOCDiffDialog(List languageFilters) {
        super(languageFilters);
    }

    public void showDialog() {
        frame = new JFrame(resources.getString("Dialog.Window_Title"));
        frame.setIconImage(DashboardIconFactory.getWindowIconImage());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        DocumentListener dl = (DocumentListener) EventHandler.create(
                DocumentListener.class, this, "recalculateEnablement");

        Box vBox = Box.createVerticalBox();
        vBox.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        Box hBox = Box.createHorizontalBox();
        hBox.add(new JLabel(resources.getString("Dialog.File_A_Prompt")));
        hBox.add(Box.createHorizontalStrut(150));
        hBox.add(Box.createHorizontalGlue());
        vBox.add(hBox);

        hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalStrut(10));
        hBox.add(fileA = new JTextField());
        dontStretchVertically(fileA);
        fileA.getDocument().addDocumentListener(dl);
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
        hBox.add(Box.createHorizontalStrut(10));
        hBox.add(fileB = new JTextField());
        dontStretchVertically(fileB);
        fileB.getDocument().addDocumentListener(dl);
        hBox.add(browseB = new JButton(resources.getDlgString("Browse")));
        browseB.addActionListener(this);
        vBox.add(hBox);

        hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalStrut(10));
        hBox.add(countIdentical = new JCheckBox(resources
                .getString("Dialog.Count_Unchanged")));
        countIdentical.setEnabled(false);
        hBox.add(Box.createHorizontalGlue());
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

    public void recalculateEnablement() {
        String filenameA = fileA.getText().trim();
        String filenameB = fileB.getText().trim();
        countIdentical.setEnabled(
                filenameA.length() > 0 && filenameB.length() > 0
                && isDir(new File(filenameA)) && isDir(new File(filenameB)));
    }

    protected void beep(JTextField field) {
        java.awt.Toolkit.getDefaultToolkit().beep();
        if (field != null) {
            field.requestFocus();
            field.selectAll();
        }
    }

    public void compare() {
        if (validateInput()) {
            ProgressDialog workingDialog = new ProgressDialog
                (frame, resources.getString("Dialog.Comparing"),
                 resources.getString("Dialog.Comparing"));
            workingDialog.addTask(this);
            workingDialog.setCancelText(resources.getString("Cancel"));
            workingDialog.setCancellable(true);
            Dimension d = workingDialog.getSize();
            d.width *= 3;
            workingDialog.setSize(d);
            workingDialog.run();   // this will block until done.
        }
    }

    protected boolean validateInput() {

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

        File compareA = null;
        if (filenameA.length() != 0) {
            compareA = new File(filenameA);
            if (!compareA.exists()) {
                beep(fileA);
                return false;
            }
        }

        File compareB = new File(filenameB);
        if (!compareB.exists()) {
            beep(fileB);
            return false;
        }

        if (compareA != null &&
            (compareA.isDirectory() != compareB.isDirectory())) {
            beep(fileA);
            return false;
        }

        setCompareA(compareA);
        setCompareB(compareB);
        skipIdentical = (countIdentical.isSelected() == false);

        return true;
    }

    public void run() {
        try {
            File outFile = generateDiffs();
            outFile.deleteOnExit();
            Browser.launch(outFile.toURL().toString());

        } catch (IOException ioe) {
            beep(null);
            ioe.printStackTrace();
        } catch (ProgressDialog.CancelledException ce) {}
    }


    public static void main(String[] args) {

        if (args.length == 0) {
            new FileSystemLOCDiffDialog(HardcodedFilterLocator.getFilters())
                .showDialog();

        } else if (args.length == 1 || args.length == 2) {
            FileSystemLOCDiff.main(args);

        } else {
            printUsage();

        }
    }

    protected static void printUsage() {
        System.out.println("Usage: java " +
                FileSystemLOCDiffDialog.class.getName() + " [fileA] [fileB]");
    }

}
