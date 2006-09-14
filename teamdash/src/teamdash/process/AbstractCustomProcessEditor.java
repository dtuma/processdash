package teamdash.process;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;

public abstract class AbstractCustomProcessEditor {

    JFrame frame;

    JTextField processName, processVersion;

    String origProcessName = null, origProcessVersion = null;

    CustomProcess process;

    ProcessPhaseTableModel phaseModel;

    SizeMetricsTableModel sizeModel;

    JMenuItem newMenuItem, openMenuItem, saveMenuItem, closeMenuItem;

    File defaultDir = null;

    File openedFileDir = null;

    public AbstractCustomProcessEditor(String prefix) {
        // ignore the prefix for now.
        process = new CustomProcess();
        origProcessName = process.getName();
        origProcessVersion = process.getVersion();

        frame = new JFrame("Custom Team Metrics Framework Editor");
        frame.setJMenuBar(buildMenuBar());
        frame.getContentPane().add(buildHeader(), BorderLayout.NORTH);
        frame.getContentPane().add(buildTabPanel(), BorderLayout.CENTER);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                confirmClose(true);
            }
        });
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        initNewProcess();

        frame.setSize(new Dimension(600, 600));
        frame.setVisible(true);
    }

    public void setDefaultDirectory(File dir) {
        defaultDir = dir;
    }

    public boolean isStructureChanged() {
        return sizeModel.isStructureChanged()
                || phaseModel.isStructureChanged();
    }

    public boolean isDirty() {
        return sizeModel.isDirty() || phaseModel.isDirty();
    }

    private Component buildHeader() {
        Box header = Box.createHorizontalBox();
        header.add(Box.createHorizontalStrut(2));
        header.add(new JLabel("Framework Name: "));
        header.add(processName = new JTextField(20));
        header.add(Box.createHorizontalStrut(10));
        header.add(new JLabel("Framework Version: "));
        header.add(processVersion = new JTextField(5));
        header.add(Box.createHorizontalStrut(2));

        // Notify the process of changes to the name and version
        FocusListener l = new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                process.setName(processName.getText());
                processName.setText(process.getName());
                process.setVersion(processVersion.getText());
                processVersion.setText(process.getVersion());
            }
        };
        processName.addFocusListener(l);
        processVersion.addFocusListener(l);

        Box result = Box.createVerticalBox();
        result.add(Box.createVerticalStrut(2));
        result.add(header);
        result.add(Box.createVerticalStrut(2));
        return result;
    }

    private Component buildTabPanel() {
        JTabbedPane tabPane = new JTabbedPane();
        tabPane.addTab("Size Metrics", buildSizeTable());
        tabPane.addTab("Process Phases", buildPhaseTable());
        return tabPane;
    }

    private Component buildSizeTable() {
        sizeModel = new SizeMetricsTableModel(process);
        return new ItemListEditor(sizeModel, "List of Process Size Metrics");

    }

    private Component buildPhaseTable() {
        phaseModel = new ProcessPhaseTableModel(process);
        return new ItemListEditor(phaseModel, "List of Process Phases");
    }

    protected void setProcess(CustomProcess proc) {
        process = proc;
        processName.setText(origProcessName = process.getName());
        processVersion.setText(origProcessVersion = process.getVersion());
        phaseModel.setProcess(process);
        sizeModel.setProcess(process);
    }

    protected void initNewProcess() {
        sizeModel.initNewProcess();
        phaseModel.initNewProcess();
    }

    protected void newProcess() {
        if (saveOrCancel(true)) {
            setProcess(new CustomProcess());
            initNewProcess();
            openedFileDir = defaultDir;
        }
    }

    protected void openProcess() {
        if (!saveOrCancel(true))
            return;

        if (getFileChooser(true).showOpenDialog(frame) != JFileChooser.APPROVE_OPTION)
            return;

        File f = fileChooser.getSelectedFile();
        CustomProcess newProcess = CustomProcess.open(f);
        if (newProcess == null)
            JOptionPane.showMessageDialog(frame, "The file '" + f
                    + "' is not a valid Custom Metrics Framework file.",
                    "Invalid File", JOptionPane.ERROR_MESSAGE);
        else {
            openedFileDir = f.getParentFile();
            setProcess(newProcess);
        }
    }

    private void stopEditing(Component c) {
        if (c instanceof JTable) {
            JTable table = (JTable) c;
            if (table.getCellEditor() != null)
                table.getCellEditor().stopCellEditing();
        } else if (c instanceof Container) {
            Container parent = (Container) c;
            for (int i = parent.getComponentCount(); i-- > 0;)
                stopEditing(parent.getComponent(i));
        }
    }

    private class TemplateFileFilter extends FileFilter {
        public String getDescription() {
            return "Template files (*.jar, *.zip)";
        }

        public boolean accept(File f) {
            if (f.isDirectory())
                return true;
            String name = f.getName().toLowerCase();
            return (name.endsWith(".zip") || name.endsWith(".jar"));
        }
    }

    private JFileChooser fileChooser = null;

    private JFileChooser getFileChooser(boolean open) {
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new TemplateFileFilter());
            fileChooser.setAcceptAllFileFilterUsed(false);
            if (defaultDir != null)
                fileChooser.setCurrentDirectory(defaultDir);
        }

        if (open) {
            fileChooser.setDialogTitle("Open Custom Metrics Framework");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setApproveButtonText("Open");
            fileChooser.setApproveButtonToolTipText("Open selected file");
        } else {
            fileChooser.setDialogTitle("Choose Save Directory");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (openedFileDir != null)
                fileChooser.setSelectedFile(openedFileDir);
            fileChooser.setApproveButtonText("Save");
            fileChooser
                    .setApproveButtonToolTipText("Save to selected directory");
        }

        return fileChooser;
    }

    protected void incrementVersionNumber() {
        String version = process.getVersion();
        if (version.length() == 0)
            version = "2";
        else {
            NumberFormat f = NumberFormat.getInstance();
            Number num = f.parse(version, new ParsePosition(0));
            version = Long.toString(num.longValue() + 1);
        }
        processVersion.setText(version);
        process.setVersion(version);
    }

    /** save the process */
    public boolean save() {
        if (!validate())
            return false;

        try {
            // ask the user for the destination directory.
            if (getFileChooser(false).showOpenDialog(frame) != JFileChooser.APPROVE_OPTION)
                return false;

            File dir = fileChooser.getSelectedFile();

            File file = new File(dir, process.getJarName());
            publishProcess(process, file);
            sizeModel.clearDirty();
            phaseModel.clearDirty();
            openedFileDir = dir;
        } catch (IOException ioe) {
            System.out.println("Caught " + ioe);
            ioe.printStackTrace();
        }

        return true;
    }

    protected boolean validate() {
        Set errors = new LinkedHashSet();
        process.checkForErrors(errors);
        sizeModel.checkForErrors(errors);
        phaseModel.checkForErrors(errors);
        if (errors != null && errors.size() > 0) {
            JList message = new JList(errors.toArray());
            JOptionPane.showMessageDialog(frame, message,
                    "Invalid Metrics Framework", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // if the structure of the process has changed,
        if (isStructureChanged() &&
        // and it had a real name when it was opened,
                origProcessName != null && origProcessName.length() != 0 &&
                // and the name has not been changed,
                process.getName().equalsIgnoreCase(origProcessName) &&
                // and the version number has not been changed,
                process.getVersion().equalsIgnoreCase(origProcessVersion))
            // warn the user that they may invalidate historical data.
            switch (JOptionPane.showConfirmDialog(frame,
                    STRUCTURE_CHANGED_MESSAGE,
                    "Metrics Framework Structure Has Changed",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE)) {
            case JOptionPane.CLOSED_OPTION:
            case JOptionPane.CANCEL_OPTION:
                return false; // do nothing and abort.

            case JOptionPane.NO_OPTION:
                return true;

            case JOptionPane.YES_OPTION:
                incrementVersionNumber();
                return true;
            }
        return true;
    }

    private static final String[] STRUCTURE_CHANGED_MESSAGE = {
            "You have made structural changes to this metrics framework.",
            "If you have ever used this framework in the past, saving",
            "these changes will render your historical data unusable.",
            "Would you like to increment the version number, saving",
            "this as a newer version of the framework?",
            "- Choosing Yes will increment the framework version number.",
            "- Choosing No will overwrite the existing framework.  This",
            "   choice is recommended only if the framework has never",
            "   been used before.",
            "- Choosing Cancel will abort (not saving changes), and",
            "    return you to the editor to make additional changes",
            "    (such as choosing a new framework name)." };

    public void close() {
        frame.dispose();
    }

    public void confirmClose(boolean showCancel) {
        if (saveOrCancel(showCancel))
            close();
    }

    public boolean saveOrCancel(boolean showCancel) {
        if (isDirty())
            switch (JOptionPane.showConfirmDialog(frame, CONFIRM_CLOSE_MSG,
                    "Save Changes?",
                    showCancel ? JOptionPane.YES_NO_CANCEL_OPTION
                            : JOptionPane.YES_NO_OPTION)) {
            case JOptionPane.CLOSED_OPTION:
            case JOptionPane.CANCEL_OPTION:
                return false; // do nothing and abort.

            case JOptionPane.NO_OPTION:
                return true;

            case JOptionPane.YES_OPTION:
                return save(); // save changes.
            }
        return true;
    }

    private static final Object CONFIRM_CLOSE_MSG = "Do you want to save the changes you made to this "
            + "custom metrics collection framework?";

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu menu = new JMenu("File");
        menuBar.add(menu);

        menu.add(newMenuItem = new JMenuItem("New"));
        newMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopEditing(frame);
                newProcess();
            }
        });

        menu.add(openMenuItem = new JMenuItem("Open..."));
        openMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopEditing(frame);
                openProcess();
            }
        });

        menu.add(saveMenuItem = new JMenuItem("Save..."));
        saveMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopEditing(frame);
                save();
            }
        });

        menu.add(closeMenuItem = new JMenuItem("Close"));
        closeMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopEditing(frame);
                confirmClose(true);
            }
        });

        return menuBar;
    }

    protected abstract void publishProcess(CustomProcess process, File destFile)
            throws IOException;
}
