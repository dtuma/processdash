// Copyright (C) 2005-2011 Tuma Solutions, LLC
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.io.File;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentListener;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.diff.engine.DiffEngine;
import net.sourceforge.processdash.tool.diff.engine.FileAnalysisSet;
import net.sourceforge.processdash.tool.diff.impl.file.FileSystemDiffException;
import net.sourceforge.processdash.tool.diff.impl.file.FileSystemFileSet;
import net.sourceforge.processdash.tool.diff.ui.LOCDiffDialog.PanelInvalidException;
import net.sourceforge.processdash.ui.lib.BoxUtils;


public class FileSystemLOCDiffPanel implements LOCDiffDialog.Panel,
        ActionListener {

    private Component configPanel;
    private JRadioButton countOneOption, compareOption;
    private JLabel fileAPrompt;
    private JTextField fileA, fileB;
    private JButton browseA, browseB;
    private Box fileBBox;
    private JCheckBox countIdentical;
    private static JFileChooser fileChooser = null;

    public FileSystemLOCDiffPanel() {}

    public String getId() {
        return "file";
    }

    public String getShortName() {
        return resources.getString("Dialog.File.Name");
    }

    public Component getConfigPanel() {
        if (configPanel == null)
            configPanel = buildConfigPanel();
        return configPanel;
    }

    private Component buildConfigPanel() {
        DocumentListener dl = (DocumentListener) EventHandler.create(
                DocumentListener.class, this, "recalculateEnablement");


        compareOption = new JRadioButton(getRes("Compare_Two"));
        countOneOption = new JRadioButton(getRes("Count_One"));
        compareOption.setSelected(true);
        new OptionCoordinator();

        fileAPrompt = new JLabel(getRes("Compare_A"));

        fileA = new JTextField();
        fileA.getDocument().addDocumentListener(dl);
        browseA = new JButton(resources.getDlgString("Browse"));
        browseA.addActionListener(this);

        fileB = new JTextField();
        fileB.getDocument().addDocumentListener(dl);
        browseB = new JButton(resources.getDlgString("Browse"));
        browseB.addActionListener(this);

        countIdentical = new JCheckBox(resources
            .getString("Dialog.Count_Unchanged"));
        countIdentical.setEnabled(false);

        fileBBox = BoxUtils.vbox(
            hbox(getRes("Compare_B"), GLUE), 5,
            hbox(PAD, fileB, 5, browseB), GLUE,
            hbox(PAD, countIdentical, GLUE), GLUE
        );

        return BoxUtils.vbox(
            hbox(GLUE, compareOption, GLUE, countOneOption, GLUE), GLUE,
            hbox(fileAPrompt, 150, GLUE), 5,
            hbox(PAD, fileA, 5, browseA), GLUE,
            fileBBox
        );
    }

    private class OptionCoordinator extends ButtonGroup implements
            ChangeListener {
        private boolean isCompare;
        private OptionCoordinator() {
            add(compareOption);
            add(countOneOption);
            isCompare = true;
            compareOption.setSelected(true);
            compareOption.addChangeListener(this);
        }

        public void stateChanged(ChangeEvent e) {
            boolean isCompare = compareOption.isSelected();
            if (this.isCompare == isCompare)
                return;

            fileBBox.setVisible(isCompare);
            fileAPrompt.setText(getRes(isCompare ? "Compare_A" : "Count_Prompt"));
            this.isCompare = isCompare;
        }
    }

    private Component hbox(Object... contents) {
        BoxUtils result = BoxUtils.hbox(contents);
        Dimension d = result.getPreferredSize();
        d.width = 3000;
        result.setMaximumSize(d);
        return result;
    }
    private Object GLUE = BoxUtils.GLUE;
    private static final int PAD = 30;

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == browseA)
            browseFile(fileA);
        else if (e.getSource() == browseB)
            browseFile(fileB);
    }

    protected void browseFile(JTextField dest) {
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode
                (JFileChooser.FILES_AND_DIRECTORIES);
        }

        if (fileChooser.showOpenDialog(configPanel) ==
            JFileChooser.APPROVE_OPTION) {
            File f = fileChooser.getSelectedFile();
            if (f != null)
                dest.setText(f.getPath());
        }
    }

    public void recalculateEnablement() {
        String filenameA = fileA.getText().trim();
        String filenameB = fileB.getText().trim();
        countIdentical.setEnabled(compareOption.isSelected()
                && filenameA.length() > 0 && filenameB.length() > 0
                && isDir(new File(filenameA)) && isDir(new File(filenameB)));
    }

    private boolean isDir(File f)  { return (f != null && f.isDirectory()); }

    public FileAnalysisSet getFileAnalysisSet(DiffEngine engine)
            throws PanelInvalidException {
        recalculateEnablement();

        FileSystemFileSet fileSet = new FileSystemFileSet();
        if (compareOption.isSelected()) {
            fileSet.addFile(getFileForField(fileA));
            fileSet.addFile(getFileForField(fileB));
            boolean shouldCountIdentical = countIdentical.isEnabled()
                    && countIdentical.isSelected();
            engine.setSkipIdenticalFiles(shouldCountIdentical == false);
        } else {
            fileSet.addFile(getFileForField(fileA));
            engine.setSkipIdenticalFiles(false);
        }

        try {
            fileSet.validate();
        } catch (FileSystemDiffException.FileNotFound e) {
            throw new PanelInvalidException(resources.format(
                "Dialog.File.File_Not_Found_FMT", e.getMissingFile().getPath()));
        } catch (FileSystemDiffException.TypeMismatch e) {
            throw new PanelInvalidException(resources.getStrings(
                "Dialog.File.Type_Mismatch"));
        }

        return fileSet;
    }

    private File getFileForField(JTextField field) {
        String filename = field.getText().trim();
        if (filename.length() == 0)
            throwInvalidField(field, getRes("File_Missing"));

        return new File(filename);
    }

    private void throwInvalidField(JTextField field, Object errorMessage) {
        field.grabFocus();
        field.selectAll();
        throw new PanelInvalidException(errorMessage);
    }

    private static String getRes(String key) {
        return resources.getString("Dialog.File." + key);
    }

    private static Resources resources = Resources.getDashBundle("LOCDiff");


}
