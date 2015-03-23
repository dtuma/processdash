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
import java.awt.Dimension;
import java.beans.EventHandler;
import java.io.File;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.hier.ui.SelectableHierarchyTree;
import net.sourceforge.processdash.tool.export.DataImporter;
import net.sourceforge.processdash.tool.export.mgr.ExportManager;
import net.sourceforge.processdash.tool.export.mgr.ExportMetricsFileInstruction;
import net.sourceforge.processdash.ui.lib.ExampleFileFilter;
import net.sourceforge.processdash.ui.lib.WrappingText;

public class EditExportMetricsFilePanel extends WizardPanel {

    private ExportMetricsFileInstruction origInstr;

    private ExportMetricsFileInstruction instr;

    private boolean fromManagePanel;

    private FileChooser file;

    private SelectableHierarchyTree paths;

    private ButtonGroup makeAutomatic;

    private WrappingText error;

    public EditExportMetricsFilePanel(Wizard wizard,
            ExportMetricsFileInstruction instr, boolean fromManagePanel) {
        super(wizard, "Export.Metrics_File");

        this.origInstr = instr;
        if (instr == null) {
            this.instr = new ExportMetricsFileInstruction();
        } else {
            this.instr = (ExportMetricsFileInstruction) instr.clone();
        }
        this.fromManagePanel = fromManagePanel;

        buildUserInterface();
        recalculateEnablement();
    }

    protected void buildMainPanelContents() {
        String chooseFilePrompt = getString("Choose_File");
        add(indentedComponent(2, new WrappingText(chooseFilePrompt)));
        add(verticalSpace(1));
        file = new FileChooser();
        file.getDocument().addDocumentListener(
                (DocumentListener) EventHandler.create(DocumentListener.class,
                        this, "updateInstruction"));
        add(indentedComponent(4, file));

        add(verticalSpace(2));
        String choosePathsPrompt = getString("Choose_Paths");
        add(indentedComponent(2, new WrappingText(choosePathsPrompt)));
        add(verticalSpace(1));
        ProcessDashboard dashboard = ExportManager.getInstance()
                .getProcessDashboard();
        paths = new SelectableHierarchyTree(dashboard.getHierarchy(), instr
                .getPaths());
        paths.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting())
                    updateInstruction();
            }});
        JScrollPane scrollPane = new JScrollPane(paths);
        scrollPane.setPreferredSize(new Dimension(999, 300));
        add(indentedComponent(4, scrollPane));

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
        error = new WrappingText("X");
        error.setMinimumSize(error.getPreferredSize());
        error.setText("");
        error.setForeground(Color.red);
        add(error);
    }

    protected void addBottomPadding(Box verticalBox) {
        // do nothing.
    }

    private JRadioButton createAutomaticButton(String key) {
        JRadioButton result = new JRadioButton(getString(key));
        result.setActionCommand(key);
        makeAutomatic.add(result);
        if (makeAutomatic.getButtonCount() == 2)
            result.setSelected(true);
        return result;
    }

    private void setError(String text) {
        error.setText(text);
        nextButton.setEnabled(text == null);
    }

    public void updateInstruction() {
        instr.setFile(file.getSelectedFile());
        instr.setPaths(paths.getBriefSelectedPaths());
        recalculateEnablement();
    }

    private void recalculateEnablement() {
        String filename = instr.getFile();
        if (filename == null || filename.trim().length() == 0) {
            setError(getString("Choose_File.Error_Missing"));
            return;
        }

        File file = new File(filename);
        if (file.exists() && !file.canWrite()) {
            setError(Wizard.resources.format(
                    "Export.Metrics_File.Choose_File.Error_Cannot_Write_FMT",
                    file.getPath()));
            return;
        }

        File dir = file.getParentFile();
        if (dir == null)
            dir = file.getAbsoluteFile().getParentFile();
        if (!dir.isDirectory()) {
            setError(Wizard.resources.format(
                    "Export.Metrics_File.Choose_File.Error_Nonexistent_FMT",
                    dir.getPath()));
            return;
        }

        Vector paths = instr.getPaths();
        if (paths == null || paths.size() == 0) {
            setError(getString("Choose_Paths.Error_Missing"));
            return;
        }

        setError(null);
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
                ExportManager.getInstance().addInstruction(instr);
            else
                ExportManager.getInstance().runOneTimeInstruction(instr);
        } else
            ExportManager.getInstance().changeInstruction(origInstr, instr);

        if (fromManagePanel)
            wizard.goBackward();
        else {
            wizard.goForward(new SuccessPanel(wizard, "Export.Success"));
        }
    }

    private class FileChooser extends FileChooserComponent {

        private ExampleFileFilter pdashFilter;
        private ExampleFileFilter txtFilter;

        public FileChooser() {
            super(instr.getFile());
        }

        protected JFileChooser createFileChooser() {
            JFileChooser result = super.createFileChooser();
            result.setDialogTitle(getString("Choose_File_Short"));

            pdashFilter = getFilter(".pdash", DataImporter.EXPORT_FILE_SUFFIX);
            txtFilter = getFilter(".txt", DataImporter.EXPORT_FILE_OLD_SUFFIX);
            result.addChoosableFileFilter(pdashFilter);
            result.addChoosableFileFilter(txtFilter);
            result.setFileFilter(pdashFilter);

            return result;
        }

        private ExampleFileFilter getFilter(String key, String suffix) {
            String description = getString("Choose_File.Description" + key);
            return new ExampleFileFilter(suffix.substring(1), description);
        }

        protected String maybeFrobPath(String path) {
            FileFilter filter = fileChooser.getFileFilter();
            File file = new File(path);
            if (!filter.accept(file)) {
                if (filter == pdashFilter)
                    path = path + DataImporter.EXPORT_FILE_SUFFIX;
                else if (filter == txtFilter)
                    path = path + DataImporter.EXPORT_FILE_OLD_SUFFIX;
            }
            return path;
        }


    }

}
