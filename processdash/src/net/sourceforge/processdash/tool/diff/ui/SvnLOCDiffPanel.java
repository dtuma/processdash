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

package net.sourceforge.processdash.tool.diff.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.diff.engine.DiffEngine;
import net.sourceforge.processdash.tool.diff.engine.FileAnalysisSet;
import net.sourceforge.processdash.tool.diff.impl.svn.SvnDiffException;
import net.sourceforge.processdash.tool.diff.impl.svn.SvnExecutor;
import net.sourceforge.processdash.tool.diff.impl.svn.SvnFileSet;
import net.sourceforge.processdash.tool.diff.ui.LOCDiffDialog.PanelInvalidException;
import net.sourceforge.processdash.ui.lib.BoxUtils;
import net.sourceforge.processdash.ui.lib.DecimalField;
import net.sourceforge.processdash.ui.lib.FileSelectionField;

public class SvnLOCDiffPanel implements LOCDiffDialog.Panel {

    private Component configPanel;

    private JCheckBox showLocalMods;

    private JCheckBox showRevisions;

    private JTextField revisionsToShow;

    private JCheckBox showToken;

    private JTextField tokenToShow;

    private JCheckBox tokenIsRegexp;

    private DecimalField numTokenDays;

    private FileSelectionField baseDirSelector;

    public SvnLOCDiffPanel() {}

    public String getId() {
        return "svn";
    }

    public String getShortName() {
        return getRes("Name");
    }

    public Component getConfigPanel() {
        if (configPanel == null)
            configPanel = buildConfigPanel();
        return configPanel;
    }

    private Component buildConfigPanel() {
        baseDirSelector = new FileSelectionField(LOCDiffDialog.PREFS,
                "recentSvnDirectories", JFileChooser.DIRECTORIES_ONLY,
                resources.getString("Browse"));

        showLocalMods = new JCheckBox(getRes("Local_Mods"), true);
        CheckBoxCoordinator coord = new CheckBoxCoordinator();
        Dimension linePad = showLocalMods.getPreferredSize();
        linePad.width = 10;

        showRevisions = new JCheckBox(getRes("Revisions"));
        showRevisions.addActionListener(coord);
        revisionsToShow = textField(showRevisions, coord);
        revisionsToShow.setToolTipText(getRes("Revisions_Tooltip"));

        showToken = new JCheckBox(getRes("Token"));
        showToken.addActionListener(coord);
        tokenToShow = textField(showToken, coord);
        tokenIsRegexp = new JCheckBox(getRes("Regular_Expression"));

        numTokenDays = new DecimalField(180, 3, NumberFormat
                .getIntegerInstance());
        numTokenDays.setMaximumSize(numTokenDays.getPreferredSize());
        Component tokenDaysLine = null;
        String tokenDaysText = getRes("Token_Limit_Days_FMT");
        int pos = tokenDaysText.indexOf("{0}");
        if (pos != -1)
            tokenDaysLine = hbox(PAD, tokenDaysText.substring(0, pos),
                numTokenDays, tokenDaysText.substring(pos + 3), GLUE);

        return BoxUtils.vbox(
            hbox(getRes("Base_Dir_Prompt"), GLUE), 5,
            hbox(PAD, baseDirSelector, 10, GLUE), SPACE, GLUE,
            hbox(showLocalMods, GLUE), SPACE, GLUE,
            hbox(showRevisions, linePad, GLUE),
            hbox(PAD, revisionsToShow, 10, GLUE), SPACE, GLUE,
            hbox(showToken, GLUE), 1,
            hbox(PAD, tokenToShow, 10, tokenIsRegexp, 10, GLUE), 3,
            tokenDaysLine, GLUE
        );
    }

    private class CheckBoxCoordinator implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (showRevisions.isSelected() || showToken.isSelected()) {
                showLocalMods.setSelected(false);
            } else {
                showLocalMods.setSelected(true);
            }
        }
    }

    private JTextField textField(final JCheckBox associatedCheckBox,
            final CheckBoxCoordinator coord) {
        final JTextField result = new JTextField();
        result.getDocument().addDocumentListener(new DocumentListener() {
            public void removeUpdate(DocumentEvent e) { update(); }
            public void insertUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
            private void update() {
                boolean hasValue = result.getText().trim().length() > 0;
                associatedCheckBox.setSelected(hasValue);
                coord.actionPerformed(null);
            }});
        return result;
    }

    private Component hbox(Object... contents) {
        BoxUtils result = BoxUtils.hbox(contents);
        Dimension d = result.getPreferredSize();
        d.width = 3000;
        result.setMaximumSize(d);
        return result;
    }
    private static final Object GLUE = BoxUtils.GLUE;
    private static final int PAD = 30;
    private static final int SPACE = 10;

    public FileAnalysisSet getFileAnalysisSet(DiffEngine engine)
            throws PanelInvalidException {
        File dir = getSelectedDirectory();
        SvnExecutor svn = createSvnExecutor(dir);
        FileAnalysisSet fileSet = createFileSet(svn);
        return fileSet;
    }

    private File getSelectedDirectory() {
        File dir = baseDirSelector.getSelectedFile();
        if (dir == null)
            throwInvalidField(baseDirSelector.getTextField(), resources
                    .getString("Dialog.SVN.Directory_Missing"));

        else if (!dir.isDirectory())
            throwInvalidField(baseDirSelector.getTextField(), resources.format(
                "Dialog.SVN.No_Such_Directory_FMT", dir.getPath()));

        return dir;
    }

    private SvnExecutor createSvnExecutor(File baseDir) {
        try {
            SvnExecutor svn = new SvnExecutor();
            svn.setBaseDirectory(baseDir);
            svn.validate();
            baseDirSelector.savePreferences();
            return svn;
        } catch (SvnDiffException.NotWorkingCopy e) {
            throwInvalidField(baseDirSelector.getTextField(), resources.format(
                "Dialog.SVN.Not_Working_Directory_FMT", baseDir.getPath()));
        } catch (SvnDiffException.AppNotFound e) {
            throwInvalidField(baseDirSelector.getTextField(), resources
                    .getStrings("Dialog.SVN.Executable_Not_Found"));
        }
        // this line is not reached, because the catch clauses above will
        // always throw an exception.  But it must be present to keep the
        // compiler happy.
        return null;
    }

    private FileAnalysisSet createFileSet(SvnExecutor svn) {
        SvnFileSet files = new SvnFileSet(svn);

        boolean sawChanges = false;
        if (configureLocalMods(files)) sawChanges = true;
        if (configureRevisions(files)) sawChanges = true;
        if (configureLogToken(files)) sawChanges = true;

        if (!sawChanges)
            throw new PanelInvalidException(getRes("No_Option_Selected"));

        return files;
    }

    private boolean configureLocalMods(SvnFileSet files) {
        if (showLocalMods.isSelected()) {
            files.setIncludeLocalMods(true);
            return true;
        } else {
            return false;
        }
    }

    private boolean configureRevisions(SvnFileSet files) {
        if (!showRevisions.isSelected())
            return false;

        List<String> revisions = new ArrayList<String>();
        Matcher m = REVISON_PATTERN.matcher(revisionsToShow.getText());
        while (m.find()) {
            String revA = m.group(1);
            String revB = m.group(3);
            if (revB == null) {
                revisions.add(revA);
            } else {
                int aNum = Integer.parseInt(revA);
                int bNum = Integer.parseInt(revB);
                for (int r = aNum; r <= bNum; r++)
                    revisions.add(Integer.toString(r));
            }
        }
        if (revisions.isEmpty()) {
            throwInvalidField(revisionsToShow, new String[] {
                    getRes("Revisions_Error"), getRes("Revisions_Tooltip") });
        }

        files.addRevisionsToTrack(revisions);
        return true;
    }
    private static final Pattern REVISON_PATTERN = Pattern
            .compile("(\\d+)(-(\\d+))?");

    private boolean configureLogToken(SvnFileSet files) {
        if (!showToken.isSelected())
            return false;

        String token = tokenToShow.getText().trim();
        if (token.length() == 0)
            throwInvalidField(tokenToShow, getRes("Token_Missing"));

        if (tokenIsRegexp.isSelected()) {
            try {
                files.setLogMessageTokenRegexp(token);
            } catch (Exception e) {
                throwInvalidField(tokenToShow, getRes("Invalid_Regexp"));
            }
        } else {
            files.setLogMessageToken(token);
        }

        int numDays = (int) numTokenDays.getValue();
        if (numDays > 0)
            files.setLogMessageTokenLimit(numDays);
        return true;
    }

    private static String getRes(String key) {
        return resources.getString("Dialog.SVN." + key);
    }

    private void throwInvalidField(JTextField field, Object errorMessage) {
        field.grabFocus();
        field.selectAll();
        throw new PanelInvalidException(errorMessage);
    }

    private static final Resources resources = LOCDiffDialog.resources;

}
