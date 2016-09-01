// Copyright (C) 2002-2016 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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


package teamdash.wbs;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.EventHandler;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.PatternList;


public abstract class AbstractLibraryEditor {

    public enum Mode { Export, Import, ImportOrg }

    /** The team project that we are operating against. */
    TeamProject teamProject;
    /** Are we importing or exporting definitions? */
    boolean export;
    /** Resources for localization */
    Resources resources;
    /** The extension to use for library filenames */
    String filenameExtension;

    /** The WBS model for the definitions we are importing/exporting */
    WBSModel projectWbs;
    /** The data model associated with the definitions */
    DataTableModel projectModel;
    /** The table to display the team project definitions in */
    WBSJTable projectTable;

    /** The file containing the library definitions */
    WBSLibrary libraryFile;
    /** The WBS model for the library definitions */
    WBSModel library;
    /** The data model for the library definitions */
    DataTableModel libraryModel;
    /** The table to display the library definitions in */
    WBSJTable libraryTable;

    /** An editor whose value is a Set containing the names of all currently
     * selected definitions. */
    PropertyEditor selectedDefinitionNames;

    /** The dialog containing this library editor */
    JDialog dialog;


    JButton addButton;
    JButton addAllButton;
    JButton openLibraryButton;
    JButton okButton;
    JButton cancelButton;

    boolean dirtyFlag;

    private static JFileChooser fileChooser;


    public class UserCancelledException extends Exception {}

    public AbstractLibraryEditor(TeamProject teamProject, JFrame parent,
            Mode mode, Resources resources, String filenameExtension,
            String orgAssetsSystemProperty) throws UserCancelledException {
        this.teamProject = teamProject;
        this.export = (mode == Mode.Export);
        this.resources = resources;
        this.filenameExtension = filenameExtension;

        if (mode == Mode.ImportOrg)
            openOrgLibrary(orgAssetsSystemProperty);
        else
            openLibrary(parent, export);
        openModels();
        if (export)
            setExportSourceIDs();
        libraryModel.setEditingEnabled(false);
        projectModel.setEditingEnabled(false);

        dirtyFlag = false;

        String title = resources.format(resKey("Window_Title_FMT"),
            teamProject.getProjectName());
        dialog = new JDialog(parent, title, true);
        buildContents();
        dialog.setSize(800, 600);
        dialog.setVisible(true);
    }

    protected abstract void openModels();


    private void setExportSourceIDs() {
        String projectID = teamProject.getProjectID();
        for (WBSNode node : projectWbs.getWbsNodes()) {
            String sourceID = projectID + ":" + node.getUniqueID();
            node.setAttribute("exportSourceID", sourceID);
            node.removeAttribute("importSourceID");
        }
    }

    private void buildContents() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        JPanel panel = new JPanel(layout);
        panel.setBorder(new EmptyBorder(10,10,10,10));

        Insets insets0 = new Insets(0, 0, 0, 0);
        Insets insets10 = new Insets(10, 10, 10, 10);

        projectTable = buildJTable(projectModel);
        projectTable.setEditingEnabled(false);
        JScrollPane sp = new JScrollPane(projectTable);
        initConstraints(c, 0, 1, 2, 2, GridBagConstraints.BOTH, 2, 2, GridBagConstraints.CENTER, insets0);
        layout.setConstraints(sp, c);
        panel.add(sp);

        libraryTable = buildJTable(libraryModel);
        libraryTable.setEditingEnabled(false);
        libraryTable.setBackground(LIGHT_SEPIA);
        sp = new JScrollPane(libraryTable);
        initConstraints(c, 3, 1, 2, 2, GridBagConstraints.BOTH, 2, 2, GridBagConstraints.CENTER, insets0);
        layout.setConstraints(sp, c);
        panel.add(sp);

        selectedDefinitionNames = new NameSetPropertyEditor();
        WBSNameSelectionModel wsm = new WBSNameSelectionModel
            (projectTable, selectedDefinitionNames);
        projectTable.setSelectionModel(wsm);
        WBSNameSelectionModel lsm = new WBSNameSelectionModel
            (libraryTable, selectedDefinitionNames);
        libraryTable.setSelectionModel(lsm);
        new SelectionListener(wsm, lsm, export);

        addButton = new JButton(resources.getString("Overwrite"));
        addButton.setIcon(export ? IconFactory.getRightArrowIcon() : IconFactory.getLeftArrowIcon());
        addButton.setHorizontalTextPosition(export ? SwingConstants.LEFT : SwingConstants.RIGHT);
        addButton.setMinimumSize(addButton.getPreferredSize());
        addButton.setText(resources.getString("Add"));
        addButton.setMnemonic('A');
        addButton.setEnabled(false);
        addButton.addActionListener(createActionListener("addDefinitionAction"));
        initConstraints(c, 2, 1, 1, 1, GridBagConstraints.HORIZONTAL, 0, 1, GridBagConstraints.SOUTH, insets10);
        layout.setConstraints(addButton, c);
        panel.add(addButton);

        addAllButton = new JButton(resources.getString("Add_All"));
        addAllButton.setMnemonic('L');
        addAllButton.setIcon(export ? IconFactory.getRightArrowIcon() : IconFactory.getLeftArrowIcon());
        addAllButton.setHorizontalTextPosition(export ? SwingConstants.LEFT : SwingConstants.RIGHT);
        addAllButton.addActionListener(createActionListener("addAllDefinitionsAction"));
        initConstraints(c, 2, 2, 1, 1, GridBagConstraints.HORIZONTAL, 0, 1, GridBagConstraints.NORTH, insets10);
        layout.setConstraints(addAllButton, c);
        panel.add(addAllButton);

        JLabel label = new JLabel(resources.format("Project_Heading_FMT",
            teamProject.getProjectName()));
        initConstraints(c, 0, 0, 1, 1, GridBagConstraints.NONE, 0, 0, GridBagConstraints.WEST, insets0);
        layout.setConstraints(label, c);
        panel.add(label);

        String libraryFileName = libraryFile.getFileName();
        String header = (libraryFileName != null
                ? resources.format("Library_Heading_FMT", libraryFileName)
                : resources.getString("Org_Library_Heading"));
        label = new JLabel(header);
        initConstraints(c, 3, 0, 1, 1, GridBagConstraints.NONE, 0, 0, GridBagConstraints.WEST, insets0);
        layout.setConstraints(label, c);
        panel.add(label);

        Box buttonPanel = Box.createHorizontalBox();
        buttonPanel.add(Box.createHorizontalGlue());

        okButton = new JButton(resources.getString(export ? "Export" : "Import"));
        okButton.addActionListener(createActionListener("okAction"));
        buttonPanel.add(okButton);

        cancelButton = new JButton(resources.getString("Cancel"));
        cancelButton.addActionListener(createActionListener("cancelAction"));
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(cancelButton);

        initConstraints(c, 0, 3, 5, 1, GridBagConstraints.BOTH, 0, 0, GridBagConstraints.NORTH, new Insets(10, 0, 0, 0));
        layout.setConstraints(buttonPanel, c);
        panel.add(buttonPanel);

        dialog.setContentPane(panel);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                cancelAction();
            }});
    }

    protected abstract WBSJTable buildJTable(DataTableModel model);

    private static Color LIGHT_SEPIA = new Color(232, 224, 205);

    private void initConstraints(GridBagConstraints c, int gridx, int gridy, int gridwidth, int gridheight, int fill, double weightx, double weighty, int anchor, Insets insets) {
        c.gridx = gridx;
        c.gridy = gridy;
        c.gridwidth = gridwidth;
        c.gridheight = gridheight;
        c.fill = fill;
        c.weightx = weightx;
        c.weighty = weighty;
        c.anchor = anchor;
        c.insets = insets;
    }

    private ActionListener createActionListener(String methodName) {
        return (ActionListener) EventHandler.create
            (ActionListener.class, this, methodName);
    }

    private WBSModel getSourceWBSModel() {
        return (export ? projectWbs : library);
    }

    private WBSModel getDestWBSModel() {
        return (export ? library : projectWbs);
    }

    public void addDefinitionAction() {
        List selectedNames = (List) selectedDefinitionNames.getValue();
        if (selectedNames != null && !selectedNames.isEmpty()) {
            getDestWBSModel().replaceBaseItems(getSourceWBSModel(),
                selectedNames);
            selectedDefinitionNames.setValue(selectedNames);
            dirtyFlag = true;
        }
    }

    public void addAllDefinitionsAction() {
        List selectedNames = (List) selectedDefinitionNames.getValue();
        List allNames = WBSLibrary.getAllDefinitionNames(getSourceWBSModel());
        if (allNames != null && !allNames.isEmpty()) {
            getDestWBSModel().replaceBaseItems(getSourceWBSModel(), allNames);
            selectedDefinitionNames.setValue(selectedNames);
            dirtyFlag = true;
        }
    }

    public void cancelAction() {
        if (dirtyFlag == false || confirmCancel())
            dialog.dispose();
    }

    private boolean confirmCancel() {
        String title = resources.getString("Discard.Window_Title");
        String message = resources.getString(resKey("Discard_Prompt"));
        int response = JOptionPane.showConfirmDialog
            (dialog, message, title, JOptionPane.YES_NO_OPTION);
        return (response == JOptionPane.YES_OPTION);
    }



    public void okAction() {
        if (export ? doExport() : doImport())
            dialog.dispose();
    }

    public boolean doExport() {
        try {
            libraryFile.save();
            return true;
        } catch (IOException e) {
            String title = resources.getString("Errors.Unexpected_Error");
            String message = resources.getString("Errors.Export");
            JOptionPane.showMessageDialog
                (dialog, message, title, JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }


    public abstract boolean doImport();


    private void openOrgLibrary(String systemProperty)
            throws UserCancelledException {
        String propVal = System.getProperty(systemProperty);
        if (propVal == null)
            throw new UserCancelledException();

        libraryFile = openOrgLibrary(propVal.trim().split("\\s+"));
        library = libraryFile.getWbs();
        library.getRoot().setName(resources.getString("Org_Library_Root_Name"));
    }

    protected abstract WBSLibrary openOrgLibrary(String[] urls);


    private void openLibrary(Component parent, boolean export)
            throws UserCancelledException {
        WBSLibrary result = null;
        while (true) {
            File file = selectFile(parent, export);
            if (file == null)
                throw new UserCancelledException();
            result = openLibrary(parent, export, file);
            if (result != null)
                break;
        }
        libraryFile = result;
        library = result.getWbs();
        library.getRoot().setName(resources.getString("Library_Root_Name"));
        if (export == false)
            library.removeAttributes(new PatternList("^exportSourceID$"));
    }

    private WBSLibrary openLibrary(Component parent, boolean export,
            File file) throws UserCancelledException {
        if (fileSelectionIsInvalid(file, parent))
            return null;

        if (!file.exists())
            return openNonexistentLibrary(parent, export, file);

        if (!checkReadWrite(parent, export, file))
            return null;

        WBSLibrary result = null;
        try {
            result = openLibraryFile(file);
        } catch (Exception e) {
        }

        if (libraryIsAcceptable(parent, export, file, result))
            return result;
        else
            return null;
    }

    protected abstract WBSLibrary openLibraryFile(File file)
            throws IOException;

    private boolean fileSelectionIsInvalid(File file, Component parent)
            throws UserCancelledException {
        if (file == null)
            throw new UserCancelledException();
        else if (file.isDirectory()) {
            showFileError(parent, "Errors.Invalid_File", file);
            return true;

        } else
            return false;
    }

    private WBSLibrary openNonexistentLibrary(Component parent, boolean export, File file) throws UserCancelledException {
        WBSLibrary result = null;

        if (export) {
            try {
                result = openNewLibrary(file);
            } catch (IOException e1) {}

        } else {
            showFileError(parent, "Errors.File_Not_Found", file);
        }

        return result;
    }

    protected abstract WBSLibrary openNewLibrary(File file)
            throws IOException;

    private boolean checkReadWrite(Component parent, boolean export, File file) throws UserCancelledException {
        if (!file.canRead()) {
            showFileError(parent, "Errors.Cannot_Read_File", file);
            return false;
        }

        if (export && !file.canWrite()) {
            showFileError(parent, "Errors.Cannot_Write_File", file);
            return false;
        }

        return true;
    }

    private boolean libraryIsAcceptable(Component parent, boolean export,
            File file, WBSLibrary result) throws UserCancelledException {

        if (result == null || !result.isValid()) {
            showFileError(parent, "Errors.Invalid_File", file);
            return false;
        }

        if (!result.compatible(teamProject.getTeamProcess())) {
            String title = resources.getString("Errors.Process_Mismatch");
            String message = resources.format(
                "Errors.Process_Mismatch_Msg_FMT", file.getPath()).trim()
                + "\n \n" + resources.getString(resKey("Process_Mismatch"));

            int response = JOptionPane.showConfirmDialog(parent,
                message.split("\n"), title, JOptionPane.YES_NO_OPTION);
            return (response == JOptionPane.YES_OPTION);
        }

        return true;
    }

    private void showFileError(Component parent, String resKey, File file) throws UserCancelledException {
        String title = resources.getString(resKey);
        String message = resources.format(resKey + "_Msg_FMT", file.getPath());
        showError(parent, title, message);
    }

    private void showError(Component parent, String title, String message) throws UserCancelledException {
        int response = JOptionPane.showConfirmDialog(parent, message.split("\n"), title, JOptionPane.OK_CANCEL_OPTION);
        if (response != JOptionPane.OK_OPTION)
            throw new UserCancelledException();
    }

    private File selectFile(Component parent, boolean export) {
        String title = resources.getString(resKey("Select_File_Title"));
        String buttonLabel = resources.getString(export ? "Export" : "Import")
                + "...";
        File result = selectFile(parent, title, buttonLabel);

        // maybe add filename extension, if the user didn't specify it.
        if (export && result != null && result.getName().indexOf('.') == -1)
            result = new File(result.getParentFile(), result.getName()
                    + filenameExtension);

        return result;
    }

    private File selectFile(Component parent, String title, String buttonLabel) {
        JFileChooser chooser = getFileChooser();
        chooser.setDialogTitle(title);
        chooser.setApproveButtonText(buttonLabel);
        chooser.setApproveButtonMnemonic(buttonLabel.charAt(0));
        chooser.setApproveButtonToolTipText(title);

        int userChoice = chooser.showDialog(parent, buttonLabel);
        if (userChoice == JFileChooser.APPROVE_OPTION)
            return chooser.getSelectedFile();
        else
            return null;
    }

    private JFileChooser getFileChooser() {
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new LibraryFileFilter());
            fileChooser.setMultiSelectionEnabled(false);
        }
        return fileChooser;
    }


    private class LibraryFileFilter extends FileFilter {

        public boolean accept(File f) {
            return (f.isDirectory() ||
                f.getName().endsWith(filenameExtension));
        }

        public String getDescription() {
            return resources.getString("File_Type");
        }

    }

    private class NameSetPropertyEditor extends PropertyEditorSupport {
        public NameSetPropertyEditor() {}
    }

    private class SelectionListener implements ListSelectionListener {

        private ListSelectionModel source;
        private ListSelectionModel dest;

        public SelectionListener(ListSelectionModel a, ListSelectionModel b, boolean forward) {
            this.source = forward ? a : b;
            this.dest   = forward ? b : a;
            a.addListSelectionListener(this);
            b.addListSelectionListener(this);
        }

        public void valueChanged(ListSelectionEvent e) {
            addButton.setEnabled(!source.isSelectionEmpty());
            String label = resources.getString(dest.isSelectionEmpty()
                ? "Add" : "Overwrite");
            addButton.setText(label);
            addButton.setMnemonic(label.charAt(0));
        }

    }

    protected String resKey(String base) {
        return (export ? "Export." : "Import.") + base;
    }

}
