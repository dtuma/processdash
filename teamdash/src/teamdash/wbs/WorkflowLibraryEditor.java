
package teamdash.wbs;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
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


public class WorkflowLibraryEditor {


    /** The team project that these workflows belong to. */
    TeamProject teamProject;

    /** Are we importing or exporting workflows? */
    boolean export;

    /** The WBS model for the team project workflows */
    WorkflowWBSModel workflows;
    /** The data model for the team project workflows */
    WorkflowModel workflowModel;
    /** The table to display the team project workflows in */
    WBSJTable workflowTable;

    /** The file containing the library workflows */
    WorkflowLibrary libraryFile;
    /** The WBS model for the library workflows */
    WorkflowWBSModel library;
    /** The data model for the library workflows */
    WorkflowModel libraryModel;
    /** The table to display the library workflows in */
    WBSJTable libraryTable;

    /** An editor whose value is a Set containing the names of all currently
     * selected workflows. */
    PropertyEditor selectedWorkflowNames;

    /** The dialog containing this workflow editor */
    JDialog dialog;

    JButton addButton;
    JButton addAllButton;
    JButton openLibraryButton;
    JButton okButton;
    JButton cancelButton;

    boolean dirtyFlag;

    private static JFileChooser fileChooser;


    public class UserCancelledException extends Exception {}

    public WorkflowLibraryEditor(TeamProject teamProject, JFrame parent, boolean export) throws UserCancelledException {
        this.teamProject = teamProject;
        TeamProcess process = teamProject.getTeamProcess();
        this.export = export;

        openWorkflowLibrary(parent, export);
        libraryModel =  new WorkflowModel(library, process);
        libraryModel.setEditingEnabled(false);

        workflows = new WorkflowWBSModel();
        workflows.copyFrom(teamProject.getWorkflows());
        workflowModel = new WorkflowModel(this.workflows, process);
        workflowModel.setEditingEnabled(false);

        dirtyFlag = false;

        String title = teamProject.getProjectName() +
                           " - " + (export ? "Export" : "Import") +
                           " Team Workflows";
        dialog = new JDialog(parent, title, true);
        buildContents();
        dialog.setSize(800, 600);
        dialog.show();
    }


    private void buildContents() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        JPanel panel = new JPanel(layout);
        panel.setBorder(new EmptyBorder(5,5,5,5));

        Insets insets0 = new Insets(0, 0, 0, 0);
        Insets insets10 = new Insets(10, 10, 10, 10);

        workflowTable = WorkflowEditor.createWorkflowJTable
            (workflowModel, teamProject.getTeamProcess());
        workflowTable.setEditingEnabled(false);
        JScrollPane sp = new JScrollPane(workflowTable);
        initConstraints(c, 0, 1, 2, 2, GridBagConstraints.BOTH, 2, 2, GridBagConstraints.CENTER);
        layout.setConstraints(sp, c);
        panel.add(sp);

        libraryTable = WorkflowEditor.createWorkflowJTable
            (libraryModel, teamProject.getTeamProcess());
        libraryTable.setEditingEnabled(false);
        libraryTable.setBackground(LIGHT_SEPIA);
        sp = new JScrollPane(libraryTable);
        initConstraints(c, 3, 1, 2, 2, GridBagConstraints.BOTH, 2, 2, GridBagConstraints.CENTER);
        layout.setConstraints(sp, c);
        panel.add(sp);

        selectedWorkflowNames = new WorkflowNameSetPropertyEditor();
        WorkflowSelectionModel wsm = new WorkflowSelectionModel
            (workflowTable, selectedWorkflowNames);
        workflowTable.setSelectionModel(wsm);
        WorkflowSelectionModel lsm = new WorkflowSelectionModel
            (libraryTable, selectedWorkflowNames);
        libraryTable.setSelectionModel(lsm);
        new SelectionListener(wsm, lsm, export);

        addButton = new JButton("Overwrite");
        addButton.setIcon(export ? IconFactory.getRightArrowIcon() : IconFactory.getLeftArrowIcon());
        addButton.setHorizontalTextPosition(export ? SwingConstants.LEFT : SwingConstants.RIGHT);
        addButton.setMinimumSize(addButton.getPreferredSize());
        addButton.setText("Add");
        addButton.setMnemonic('A');
        addButton.setEnabled(false);
        addButton.addActionListener
            ((ActionListener) EventHandler.create
                (ActionListener.class, this, "addWorkflowAction"));
        initConstraints(c, 2, 1, 1, 1, GridBagConstraints.HORIZONTAL, 0, 1, GridBagConstraints.SOUTH);
        c.insets = insets10;
        layout.setConstraints(addButton, c);
        panel.add(addButton);

        addAllButton = new JButton("Add All");
        addAllButton.setMnemonic('L');
        addAllButton.setIcon(export ? IconFactory.getRightArrowIcon() : IconFactory.getLeftArrowIcon());
        addAllButton.setHorizontalTextPosition(export ? SwingConstants.LEFT : SwingConstants.RIGHT);
        addAllButton.addActionListener
            ((ActionListener) EventHandler.create
                (ActionListener.class, this, "addAllWorkflowsAction"));
        initConstraints(c, 2, 2, 1, 1, GridBagConstraints.HORIZONTAL, 0, 1, GridBagConstraints.NORTH);
        c.insets = insets10;
        layout.setConstraints(addAllButton, c);
        panel.add(addAllButton);

        openLibraryButton = new JButton(IconFactory.getOpenIcon());
        openLibraryButton.setToolTipText("Open Other Workflow Library...");
        initConstraints(c, 4, 0, 1, 1, GridBagConstraints.NONE, 0, 0, GridBagConstraints.EAST);
        c.insets = insets0;
        layout.setConstraints(openLibraryButton, c);
        panel.add(openLibraryButton);

        JLabel label = new JLabel("Workflows in the project:");
        initConstraints(c, 0, 0, 1, 1, GridBagConstraints.NONE, 0, 0, GridBagConstraints.WEST);
        layout.setConstraints(label, c);
        panel.add(label);

        label = new JLabel("Workflows in the library:");
        initConstraints(c, 3, 0, 1, 1, GridBagConstraints.NONE, 0, 0, GridBagConstraints.WEST);
        layout.setConstraints(label, c);
        panel.add(label);

        Box buttonPanel = Box.createHorizontalBox();
        buttonPanel.add(Box.createHorizontalGlue());

        cancelButton = new JButton("Cancel");
        buttonPanel.add(cancelButton);
        buttonPanel.add(Box.createHorizontalStrut(10));

        okButton = new JButton("OK");
        buttonPanel.add(okButton);

        initConstraints(c, 0, 3, 5, 1, GridBagConstraints.BOTH, 0, 0, GridBagConstraints.NORTH);
        c.insets = new Insets(10, 0, 0, 0);
        layout.setConstraints(buttonPanel, c);
        panel.add(buttonPanel);

        dialog.setContentPane(panel);
    }
    private static Color SEPIA = new Color(159, 141, 114);
    private static Color LIGHT_SEPIA = new Color(232, 224, 205);

    private void initConstraints(GridBagConstraints c, int gridx, int gridy, int gridwidth, int gridheight, int fill, double weightx, double weighty, int anchor) {
        c.gridx = gridx;
        c.gridy = gridy;
        c.gridwidth = gridwidth;
        c.gridheight = gridheight;
        c.fill = fill;
        c.weightx = weightx;
        c.weighty = weighty;
        c.anchor = anchor;
    }

    private WorkflowWBSModel getSourceWBSModel() {
        return (export ? workflows : library);
    }

    private WorkflowWBSModel getDestWBSModel() {
        return (export ? library : workflows);
    }

    public void addWorkflowAction() {
        List selectedWorkflows = (List) selectedWorkflowNames.getValue();
        if (selectedWorkflows != null && !selectedWorkflows.isEmpty()) {
            getDestWBSModel().mergeWorkflows(getSourceWBSModel(), selectedWorkflows);
            selectedWorkflowNames.setValue(selectedWorkflows);
            dirtyFlag = true;
        }
    }

    public void addAllWorkflowsAction() {
        List selectedWorkflows = (List) selectedWorkflowNames.getValue();
        List allWorkflows = getSourceWBSModel().getWorkflowNames();
        if (allWorkflows != null && !allWorkflows.isEmpty()) {
            getDestWBSModel().mergeWorkflows(getSourceWBSModel(), allWorkflows);
            selectedWorkflowNames.setValue(selectedWorkflows);
            dirtyFlag = true;
        }
    }


    private void openWorkflowLibrary(Component parent, boolean export) throws UserCancelledException {
        WorkflowLibrary result = null;
        while (true) {
            File file = selectFile(parent, export);
            result = openWorkflowLibrary(parent, export, file);
            if (result != null)
                break;
        }
        this.libraryFile = result;
        this.library = result.getWorkflows();
    }

    private WorkflowLibrary openWorkflowLibrary(Component parent, boolean export, File file) throws UserCancelledException {
        if (fileSelectionIsInvalid(file, parent))
            return null;

        if (!file.exists())
            return openNonexistentLibrary(parent, export, file);

        if (!checkReadWrite(parent, export, file))
            return null;

        WorkflowLibrary result = null;
        try {
            result = new WorkflowLibrary(file);
        } catch (Exception e) {
        }

        if (libraryIsAcceptable(parent, export, file, result))
            return result;
        else
            return null;
    }

    private boolean fileSelectionIsInvalid(File file, Component parent) throws UserCancelledException {
        if (file == null)
            throw new UserCancelledException();
        else if (file.isDirectory()) {
            showError(parent, "Invalid File Selection",
                      "The file '"+file+"' is a directory.\n" +
                      "Please select the name of a workflow library file.");
            return true;

        } else
            return false;
    }

    private WorkflowLibrary openNonexistentLibrary(Component parent, boolean export, File file) throws UserCancelledException {
        WorkflowLibrary result = null;

        if (export) {
            try {
                result = new WorkflowLibrary(file, teamProject.getTeamProcess());
            } catch (IOException e1) {}

        } else {
            showError(parent, "File Not Found",
                      "The file '"+file+"' could not be found.\n" +
                      "Please select an existing workflow library to import from.");
        }

        return result;
    }

    private boolean checkReadWrite(Component parent, boolean export, File file) throws UserCancelledException {
        if (!file.canRead()) {
            showError(parent, "Cannot Read File",
                      "The file '"+file+"' cannot be read.\n" +
                      "Please select a different file.");
            return false;
        }

        if (export && !file.canWrite()) {
            showError(parent, "Cannot Write to File",
                    "The file '"+file+"' is read only.\n" +
                    "Please select a different file.");
            return false;
        }

        return true;
    }

    private boolean libraryIsAcceptable(Component parent, boolean export, File file, WorkflowLibrary result) throws UserCancelledException {

        if (result == null || !result.isValid()) {
            showError(parent, "Cannot Read File",
                    "The file '"+file+"' cannot be read, or is not a valid workflow library.\n" +
                    "Please select a different file.");
            return false;
        }

        if (result.compatible(teamProject.getTeamProcess())) {
            String message =
                "The workflow library contained in the file\n"+
                "     "+file+"\n"+
                "is based on a different process definition than the current project.\n";

            if (export)
                message = message +
                    "If you export workflows into this library, they may include phases\n"+
                    "which do not exist in the library's process definition.  This would\n"+
                    "result in future errors that you will need to resolve manually.\n"+
                    "\n" +
                    "Do you want to export workflows to this library anyway?";
            else
                message = message +
                    "If you import workflows from this library, they may include phases\n" +
                    "that do not exist in the current process definition.  You will need\n" +
                    "to resolve such errors manually.\n" +
                    "\n" +
                    "Do you want to import workflows from this library anyway?";

            int response = JOptionPane.showConfirmDialog(parent, message.split("\n"), "Process Mismatch", JOptionPane.YES_NO_OPTION);
            return (response == JOptionPane.YES_OPTION);
        }

        return true;
    }

    private void showError(Component parent, String title, String message) throws UserCancelledException {
        int response = JOptionPane.showConfirmDialog(parent, message.split("\n"), title, JOptionPane.OK_CANCEL_OPTION);
        if (response != JOptionPane.OK_OPTION)
            throw new UserCancelledException();
    }

    private File selectFile(Component parent, boolean export) {
        String title = (export ? "Export to Workflow Library" : "Import from Workflow Library");
        String buttonLabel = (export ? "Export..." : "Import...");
        return selectFile(parent, title, buttonLabel);
    }

    private File selectFile(Component parent, String title, String buttonLabel) {
        JFileChooser chooser = getFileChooser();
        chooser.setDialogTitle(title);
        chooser.setApproveButtonText(buttonLabel);
        chooser.setApproveButtonMnemonic(buttonLabel.charAt(0));
        chooser.setApproveButtonToolTipText(title);

        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
            return chooser.getSelectedFile();
        else
            return null;
    }

    private JFileChooser getFileChooser() {
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
            fileChooser.setFileFilter(FILE_FILTER);
            fileChooser.setMultiSelectionEnabled(false);
        }
        return fileChooser;
    }


    private static class WorkflowFileFilter extends FileFilter {

        public boolean accept(File f) {
            return (f.isDirectory() || f.getName().endsWith(".wfxml"));
        }

        public String getDescription() {
            return "Workflow Libraries (.wfxml)";
        }

    }
    static FileFilter FILE_FILTER = new WorkflowFileFilter();

    private class WorkflowNameSetPropertyEditor extends PropertyEditorSupport {
        public WorkflowNameSetPropertyEditor() {}
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
            addButton.setText(dest.isSelectionEmpty() ? "Add" : "Overwrite");
            addButton.setMnemonic(dest.isSelectionEmpty() ? 'A' : 'O');
        }

    }
}
