
package teamdash.process;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
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
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.ui.web.TinyCGIBase;



public class CustomProcessEditor extends TinyCGIBase {

    public CustomProcessEditor() { super(); }

    /** Generate CGI script output. */
    protected void writeContents() throws IOException {
        DashController.checkIP(env.get("REMOTE_ADDR"));
        new CustomProcessEditor(getPrefix(), getTinyWebServer());
        DashController.printNullDocument(out);
    }


    JFrame frame;
    JTextField processName, processVersion;
    String origProcessName = null, origProcessVersion = null;
    CustomProcess process;
    JTable table;
    JButton insertButton, deleteButton, upButton, downButton;
    JMenuItem newMenuItem, openMenuItem, saveMenuItem, closeMenuItem;
    boolean isDirty = false;
    WebServer webServer;
    File openedFileDir = null;

    public CustomProcessEditor(String prefix, WebServer webServer) {
        // ignore the prefix for now.
        this.webServer = webServer;
        process = new CustomProcess();
        origProcessName = process.getName();
        origProcessVersion = process.getVersion();

        frame = new JFrame("Custom Process Editor");
        frame.setJMenuBar(buildMenuBar());
        frame.getContentPane().add(buildHeader(), BorderLayout.NORTH);
        frame.getContentPane().add(buildTable(), BorderLayout.CENTER);
        frame.getContentPane().add(buildButtons(), BorderLayout.SOUTH);

        frame.addWindowListener( new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    confirmClose(true); }});
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        frame.setSize(new Dimension(400, 500));
        frame.setVisible(true);
    }

    private Component buildHeader() {
        Box header = Box.createHorizontalBox();
        header.add(Box.createHorizontalStrut(2));
        header.add(new JLabel("Process Name: "));
        header.add(processName = new JTextField(20));
        header.add(Box.createHorizontalStrut(10));
        header.add(new JLabel("Process Version: "));
        header.add(processVersion = new JTextField(5));
        header.add(Box.createHorizontalStrut(2));

        // Notify the process of changes to the name and version
        FocusListener l = new FocusAdapter() {
                public void focusLost(FocusEvent e) {
                    process.setName(processName.getText());
                    processName.setText(process.getName());
                    process.setVersion(processVersion.getText());
                    processVersion.setText(process.getVersion());
                } };
        processName.addFocusListener(l);
        processVersion.addFocusListener(l);

        Box result = Box.createVerticalBox();
        result.add(Box.createVerticalStrut(2));
        result.add(header);
        result.add(Box.createVerticalStrut(2));
        return result;
    }

    private Component buildTable() {
        table = new JTable(process);

        // adjust column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(75);

        // draw read-only phases with a different appearance
        table.setDefaultRenderer(String.class, new PhaseTableCellRenderer());

        // install a combo box as the editor for the "phase type" column
        TableColumn typeColumn = table.getColumnModel().getColumn(2);
        JComboBox phaseTypeEditor = new JComboBox(CustomProcess.PHASE_TYPES);
        phaseTypeEditor.setFont
            (phaseTypeEditor.getFont().deriveFont(Font.PLAIN));
        typeColumn.setCellEditor(new DefaultCellEditor(phaseTypeEditor));

        // listen to changes in the row selection, and enable/disable
        // buttons accordingly
        table.getSelectionModel().addListSelectionListener
            (new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        enableButtons(); }});

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createTitledBorder
                     (BorderFactory.createLoweredBevelBorder(),
                      "List of Process Phases"));
        return sp;
    }

    private class PhaseTableCellRenderer extends DefaultTableCellRenderer {
        private Font regular, bold;
        public PhaseTableCellRenderer() {
            Font f = getFont();
            regular = f.deriveFont(Font.PLAIN);
            bold    = f.deriveFont(Font.BOLD);
        }

        public Component getTableCellRendererComponent
            (JTable table, Object value, boolean isSelected,
             boolean hasFocus, int row, int col)
        {
            Component result = super.getTableCellRendererComponent
                (table, value, isSelected, hasFocus, row, col);
            result.setFont
                (table.getModel().isCellEditable(row, col) ? regular : bold );
            return result;
        }
    }


    private Component buildButtons() {
        Box buttons = Box.createHorizontalBox();
        buttons.add(Box.createHorizontalGlue());
        buttons.add(insertButton = new JButton("Insert"));
        insertButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    insert(); }});

        buttons.add(Box.createHorizontalGlue());
        buttons.add(deleteButton = new JButton("Delete"));
        deleteButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    delete(); }});

        buttons.add(Box.createHorizontalGlue());
        buttons.add(upButton = new JButton("Move Up"));
        upButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    moveUp(); }});

        buttons.add(Box.createHorizontalGlue());
        buttons.add(downButton = new JButton("Move Down"));
        downButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    moveDown(); }});
        buttons.add(Box.createHorizontalGlue());

        enableButtons();

        Box result = Box.createVerticalBox();
        result.add(Box.createVerticalStrut(2));
        result.add(buttons);
        result.add(Box.createVerticalStrut(2));
        return result;
    }

    private int getSelectedRow() {
        return table.getSelectionModel().getMinSelectionIndex();
    }
    private void selectRow(int row) {
        table.getSelectionModel().setSelectionInterval(row, row);
    }

    private void enableButtons() {
        int row = getSelectedRow();
        boolean editable = ((row >= 0) && !(process.get(row).readOnly));
        deleteButton.setEnabled(editable && row >= 0);
        upButton    .setEnabled(editable && row > 0);
        downButton  .setEnabled(editable &&
                              row >= 0 && row+1 < table.getRowCount());
    }

    protected void insert() {
        int row = getSelectedRow();
        if (row < 0) row = 0;
        process.insertPhase(row);
        selectRow(row);
    }
    protected void delete() {
        int row = getSelectedRow();
        if (row >= 0) process.deletePhase(row);
    }
    protected void moveUp()   {
        int row = getSelectedRow();
        process.movePhaseUp(row--);
        selectRow(row);
    }
    protected void moveDown() {
        int row = getSelectedRow()+1;
        process.movePhaseUp(row);
        selectRow(row);
    }
    /*
    private class RowSelectionTask implements Runnable {
        private int row;
        public RowSelectionTask(int row) { this.row = row; }
        public void run() {
            if (row != -1) {
                treeTable.getSelectionModel().setSelectionInterval(row, row);
                enableTaskButtons();
            }
        }
    }
    */

    protected void setProcess(CustomProcess proc) {
        process = proc;
        processName.setText(origProcessName = process.getName());
        processVersion.setText(origProcessVersion = process.getVersion());

        // remember the current widths of each table column.
        int[] width = new int[table.getColumnCount()];
        for (int i = width.length;   i-- > 0; )
            width[i] = table.getColumnModel().getColumn(i).getWidth();
        table.setModel(process);
        for (int i = width.length;   i-- > 0; )
            table.getColumnModel().getColumn(i).setPreferredWidth(width[i]);

        enableButtons();
    }

    protected void newProcess() {
        if (saveOrCancel(true)) {
            setProcess(new CustomProcess());
            openedFileDir = null;
        }
    }
    protected void openProcess() {
        if (!saveOrCancel(true))
            return;

        if (getFileChooser(true).showOpenDialog(frame) !=
            JFileChooser.APPROVE_OPTION)
            return;

        File f = fileChooser.getSelectedFile();
        CustomProcess newProcess = CustomProcessPublisher.open(f);
        if (newProcess == null)
            JOptionPane.showMessageDialog
                (frame,
                 "The file '" + f + "' is not a valid Custom Process file.",
                 "Invalid File", JOptionPane.ERROR_MESSAGE);
        else {
            openedFileDir = f.getParentFile();
            setProcess(newProcess);
        }
    }

    private class TemplateFileFilter extends FileFilter {
        public String getDescription() {
            return "Template files (*.jar, *.zip)"; }
        public boolean accept(File f) {
            if (f.isDirectory()) return true;
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
        }

        if (open) {
            fileChooser.setDialogTitle("Open Custom Process");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setApproveButtonText("Open");
            fileChooser.setApproveButtonToolTipText("Open selected file");
        } else {
            fileChooser.setDialogTitle("Choose Save Directory");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (openedFileDir != null)
                fileChooser.setSelectedFile(openedFileDir);
            fileChooser.setApproveButtonText("Save");
            fileChooser.setApproveButtonToolTipText
                ("Save to selected directory");
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
        if (!validate()) return false;

        try {
            // ask the user for the destination directory.
            if (getFileChooser(false).showOpenDialog(frame) !=
                JFileChooser.APPROVE_OPTION)
                return false;

            File dir = fileChooser.getSelectedFile();

            File file = new File(dir, process.getJarName());
            CustomProcessPublisher.publish(process, file, webServer);
            isDirty = process.isDirty = false;
            openedFileDir = dir;

        } catch (IOException ioe) {
            System.out.println("Caught " + ioe);
            ioe.printStackTrace();
        }

        return true;
    }

    protected boolean validate() {
        Collection errors = process.getErrors();
        if (errors != null && errors.size() > 0) {
            JList message = new JList(errors.toArray());
            JOptionPane.showMessageDialog(frame,
                                          message,
                                          "Invalid Process",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }

                // if the structure of the process has changed,
        if (process.structureChanged &&
                // and it had a real name when it was opened,
            origProcessName != null && origProcessName.length() != 0 &&
                // and the name has not been changed,
            process.getName().equalsIgnoreCase(origProcessName) &&
                // and the version number has not been changed,
            process.getVersion().equalsIgnoreCase(origProcessVersion))
                // warn the user that they may invalidate historical data.
            switch (JOptionPane.showConfirmDialog
                    (frame, STRUCTURE_CHANGED_MESSAGE,
                     "Process Structure Changed",
                     JOptionPane.YES_NO_CANCEL_OPTION,
                     JOptionPane.WARNING_MESSAGE)) {
            case JOptionPane.CLOSED_OPTION:
            case JOptionPane.CANCEL_OPTION:
                return false;                 // do nothing and abort.

            case JOptionPane.NO_OPTION:
                return true;

            case JOptionPane.YES_OPTION:
                incrementVersionNumber();
                return true;
            }
        return true;
    }
    private static final String[] STRUCTURE_CHANGED_MESSAGE = {
        "You have made structural changes to this process.  If",
        "you have ever used this process in the past, saving",
        "these changes will render your historical data unusable.",
        "Would you like to increment the version number, saving",
        "this as a newer version of the process?",
        "- Choosing Yes will increment the process version number.",
        "- Choosing No will overwrite the existing process.  This",
        "   choice is recommended only if the process has never",
        "   been used before.",
        "- Choosing Cancel will abort (not saving changes), and",
        "    return you to the editor to make additional changes",
        "    (such as choosing a new process name)." };

    public void close() { frame.dispose(); }

    public void confirmClose(boolean showCancel) {
        if (saveOrCancel(showCancel))
            close();
    }
    public boolean saveOrCancel(boolean showCancel) {
        if (isDirty || process.isDirty)
            switch (JOptionPane.showConfirmDialog
                    (frame, CONFIRM_CLOSE_MSG, "Save Changes?",
                     showCancel ? JOptionPane.YES_NO_CANCEL_OPTION
                                : JOptionPane.YES_NO_OPTION)) {
            case JOptionPane.CLOSED_OPTION:
            case JOptionPane.CANCEL_OPTION:
                return false;                 // do nothing and abort.

            case JOptionPane.NO_OPTION:
                return true;

            case JOptionPane.YES_OPTION:
                return save();                 // save changes.
            }
        return true;
    }
    private static final Object CONFIRM_CLOSE_MSG =
        "Do you want to save the changes you made to this " +
        "custom process?";



    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu menu = new JMenu("File");
        menuBar.add(menu);

        menu.add(newMenuItem = new JMenuItem("New"));
        newMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    newProcess(); }});

        menu.add(openMenuItem = new JMenuItem("Open..."));
        openMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    openProcess(); }});

        menu.add(saveMenuItem = new JMenuItem("Save..."));
        saveMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    save(); }});

        menu.add(closeMenuItem = new JMenuItem("Close"));
        closeMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    confirmClose(true); }});

        return menuBar;
    }

}
