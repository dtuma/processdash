// Copyright (C) 2005-2009 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.ui.wizard;

import java.awt.Color;
import java.beans.EventHandler;
import java.io.File;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentListener;

import net.sourceforge.processdash.tool.bridge.client.TeamServerSelector;
import net.sourceforge.processdash.tool.export.mgr.ImportDirectoryInstruction;
import net.sourceforge.processdash.tool.export.mgr.ImportManager;
import net.sourceforge.processdash.ui.lib.SwingWorker;
import net.sourceforge.processdash.ui.lib.WrappingText;

public class EditImportDirectoryPanel extends WizardPanel {

    private ImportDirectoryInstruction origInstr;

    private ImportDirectoryInstruction instr;

    private boolean fromManagePanel;

    private DirectoryChooser directory;

    private JTextField url;

    private JTextField prefix;

    private ButtonGroup makeAutomatic;

    private WrappingText error;

    public EditImportDirectoryPanel(Wizard wizard,
            ImportDirectoryInstruction instr, boolean fromManagePanel) {
        super(wizard, "Import.Directory");

        this.origInstr = instr;
        if (instr == null) {
            this.instr = new ImportDirectoryInstruction();
        } else {
            this.instr = (ImportDirectoryInstruction) instr.clone();
        }
        this.fromManagePanel = fromManagePanel;

        buildUserInterface();
        recalculateEnablement();
    }

    protected void buildMainPanelContents() {
        DocumentListener listener = (DocumentListener) EventHandler.create(
                DocumentListener.class, this, "updateInstruction");

        if (instr.isUrlOnly()) {
            String chooseUrlPrompt = getString("Choose_URL");
            add(indentedComponent(2, new WrappingText(chooseUrlPrompt)));
            add(verticalSpace(1));
            url = new JTextField(instr.getURL());
            url.getDocument().addDocumentListener(listener);
            add(indentedComponent(4, url));

        } else {
            String chooseDirectoryPrompt = getString("Choose_Directory");
            add(indentedComponent(2, new WrappingText(chooseDirectoryPrompt)));
            add(verticalSpace(1));
            directory = new DirectoryChooser();
            directory.getDocument().addDocumentListener(listener);
            add(indentedComponent(4, directory));
        }

        add(verticalSpace(2));
        String choosePrefixPrompt = getString("Choose_Prefix");
        add(indentedComponent(2, new WrappingText(choosePrefixPrompt)));
        add(verticalSpace(1));
        prefix = new JTextField(instr.getPrefix());
        prefix.getDocument().addDocumentListener(listener);
        add(indentedComponent(4, prefix));

        if (fromManagePanel == false) {
            add(verticalSpace(2));
            String makeAutomaticPrompt = getString("Make_Automatic");
            add(indentedComponent(2, new WrappingText(makeAutomaticPrompt)));
            makeAutomatic = new ButtonGroup();

            Box autoButtonBox = Box.createHorizontalBox();
            autoButtonBox.add(createAutomaticButton("Yes"));
            autoButtonBox.add(createAutomaticButton("No"));
            add(indentedComponent(4, autoButtonBox));
        }

        add(verticalSpace(4));
        error = new WrappingText("");
        error.setForeground(Color.red);
        add(error);
    }

    private JRadioButton createAutomaticButton(String key) {
        JRadioButton result = new JRadioButton(getString(key));
        result.setActionCommand(key);
        makeAutomatic.add(result);
        if (makeAutomatic.getButtonCount() == 1)
            result.setSelected(true);
        return result;
    }

    private void setError(String text) {
        error.setText(text);
        nextButton.setEnabled(text == null);
    }

    public void updateInstruction() {
        if (directory != null) {
            String location = directory.getSelectedFile();
            if (TeamServerSelector.isUrlFormat(location)) {
                instr.setDirectory(null);
                instr.setURL(location);
            } else {
                instr.setDirectory(location);
                instr.setURL(null);
            }
        } else {
            instr.setDirectory(null);
            instr.setURL(url.getText());
        }
        instr.setPrefix(prefix.getText());
        recalculateEnablement();
    }

    private void recalculateEnablement() {
        nextButton.setEnabled(false);
        synchronized (this) {
            recalcWorker = new RecalculateEnablement();
            recalcWorker.start();
        }
    }

    private RecalculateEnablement recalcWorker = null;

    private class RecalculateEnablement extends SwingWorker {

        @Override
        public Object construct() {
            String locationError = getLocationError();
            if (locationError != null)
                return locationError;

            String pre = prefix.getText();
            if (pre == null || pre.trim().length() == 0) {
                return getString("Choose_Prefix.Error_Missing");
            }

            return null;
        }

        private String getLocationError() {
            if (instr.isUrlOnly() || url != null)
                return getUrlLocationError();
            else
                return getFileLocationError();
        }

        private String getUrlLocationError() {
            String url = instr.getURL();
            if (!hasValue(url))
                return getString("Choose_URL.Error_Missing");

            if (shouldPerformExpensiveCheck() == false)
                return null;

            if (TeamServerSelector.testServerURL(url) == null) {
                return Wizard.resources.format(
                    "Import.Directory.Choose_URL.Error_Invalid_FMT", url);
            }

            return null;
        }

        private String getFileLocationError() {
            String dir = instr.getDirectory();
            if (!hasValue(dir))
                return getString("Choose_Directory.Error_Missing");

            if (shouldPerformExpensiveCheck() == false)
                return null;

            File dirFile = new File(dir);
            if (!dirFile.isDirectory()) {
                return Wizard.resources.format(
                    "Import.Directory.Choose_Directory.Error_Nonexistent_FMT",
                    dir);
            }

            return null;
        }

        private boolean shouldPerformExpensiveCheck() {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {}

            synchronized (EditImportDirectoryPanel.this) {
                return (recalcWorker == this);
            }
        }

        @Override
        public void finished() {
            synchronized (EditImportDirectoryPanel.this) {
                if (recalcWorker == this) {
                    setError((String) getValue());
                    recalcWorker = null;
                }
            }
        }

    }

    public void doCancel() {
        if (fromManagePanel)
            wizard.goBackward();
        else
            super.doCancel();
    }

    private boolean shouldBeAutomatic() {
        if (fromManagePanel)
            return true;
        if (makeAutomatic == null)
            return false;
        return "Yes".equals(makeAutomatic.getSelection().getActionCommand());
    }

    public void doNext() {
        boolean auto = shouldBeAutomatic();

        if (origInstr == null) {
            if (auto)
                ImportManager.getInstance().addInstruction(instr);
            else
                ImportManager.getInstance().handleAddedInstruction(instr);
        } else
            ImportManager.getInstance().changeInstruction(origInstr, instr);

        if (fromManagePanel)
            wizard.goBackward();
        else {
            wizard.goForward(new SuccessPanel(wizard, "Import.Success"));
        }
    }

    private class DirectoryChooser extends FileChooserComponent {

        public DirectoryChooser() {
            super(instr.getDirectory());
        }

        protected JFileChooser createFileChooser() {
            JFileChooser result = super.createFileChooser();
            result.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            result.setDialogTitle(getString("Choose_Directory_Short"));
            return result;
        }
    }

    private static boolean hasValue(String s) {
        return s != null && s.trim().length() > 0;
    }

}
