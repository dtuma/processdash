package teamdash;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;


/** A graphical user interface for editing the list of team members.
 */
public class TeamMemberListEditor implements WindowListener {

    /** The list of team members we are editing. */
    private TeamMemberList teamMemberList;
    /** The original list of team members, before we edited it. */
    private TeamMemberList orig;
    /** The table displaying the team member list */
    private JTable table;
    /** The frame containing/displaying the editor  */
    private JFrame frame;


    public TeamMemberListEditor(String projectName, TeamMemberList teamList) {
        teamMemberList = new TeamMemberList(orig = teamList);
        teamMemberList.maybeAddEmptyRow();
        buildTable();
        JPanel buttons = buildButtons();

        frame = new JFrame(projectName + " - Team Members");
        frame.getContentPane().add(new JScrollPane(table));
        frame.getContentPane().add(buttons, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(this);
        frame.setSize(380, 200);
        frame.show();
    }

    public void show() {
        teamMemberList.maybeAddEmptyRow();
        frame.show();
        frame.toFront();
    }

    public void hide() {
        frame.setVisible(false);
    }

    private boolean checkForErrors() {
        Object[] errors = teamMemberList.getErrors();
        if (errors == null) return true;

        Object[] message = new Object[] {
            "The team list cannot be saved because it contains errors:",
            new JList(errors) };
        JOptionPane.showMessageDialog
            (frame, message, "Problems with Team List",
             JOptionPane.ERROR_MESSAGE);
        return false;
    }

    private boolean confirmDestructiveChanges(Object[] changes) {
        if (changes == null) return true;

        Object[] message = new Object[] {
            "Your changes to the team list will require altering data in",
            "the work breakdown structure:",
            new JList(changes),
            "You will not be able to undo these changes. Do you still",
            "want to save?" };
        return (JOptionPane.YES_OPTION ==
                JOptionPane.showConfirmDialog
                    (frame, message, "Verify changes to Team List",
                     JOptionPane.YES_NO_OPTION));
    }

    public boolean save() {
        if (table.isEditing())
            // stop editing the current table cell.
            table.getCellEditor().stopCellEditing();

        // don't save if there are errors.
        if (!checkForErrors()) return false;

        // confirm that the user wants to perform the changes.
        TeamMemberList.Delta[] irreversibleChanges =
            orig.calculateDelta(teamMemberList);
        if (!confirmDestructiveChanges(irreversibleChanges)) return false;

        // commit and save the changes
        orig.publishChanges(irreversibleChanges);
        orig.copyFrom(teamMemberList);

        // now refresh the table with the newly saved data. (This will discard
        // empty rows in the middle of the list.)
        teamMemberList.copyFrom(orig);
        teamMemberList.maybeAddEmptyRow();

        // notify any listeners
        fireItemSaved();

        return true;
    }

    public void cancel() {
        if (table.isEditing())
            table.getCellEditor().stopCellEditing();
        // revert back to the original version of the team member list.
        teamMemberList.copyFrom(orig);
    }

    private void buildTable() {
        table = new JTable(teamMemberList);

        //Set up renderer and editor for the Color column.
        ColorCellRenderer.setUpColorRenderer(table);
        ColorCellEditor.setUpColorEditor(table);

        // set preferred sizes for each column
        table.getColumn("Name").setPreferredWidth(150);
        table.getColumn("Initials").setPreferredWidth(55);
        table.getColumn("Color").setPreferredWidth(55);
        table.getColumn("Est Hours/Week").setPreferredWidth(100);
    }

    private JPanel buildButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));

        JButton button = new JButton("Cancel");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel(); hide(); } });
        buttons.add(button);

        button = new JButton("Save");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (save()) hide(); } });
        buttons.add(button);
        return buttons;
    }

    public void windowOpened(WindowEvent e) {}
    public void windowClosing(WindowEvent e) { cancel(); }
    public void windowClosed(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}

    private Set saveListeners = null;
    public void addSaveListener(SaveListener l) {
        if (saveListeners == null)
            saveListeners = new HashSet();
        saveListeners.add(l);
    }
    public void removeSaveListener(SaveListener l) {
        if (saveListeners != null)
            saveListeners.remove(l);
    }
    protected void fireItemSaved() {
        Iterator i = saveListeners.iterator();
        while (i.hasNext())
            ((SaveListener) i.next()).itemSaved(this);
    }
}
