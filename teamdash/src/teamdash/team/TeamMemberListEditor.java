package teamdash.team;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import net.sourceforge.processdash.ui.macosx.MacGUIUtils;

import teamdash.SaveListener;


/** A graphical user interface for editing the list of team members.
 */
public class TeamMemberListEditor implements WindowListener, TableModelListener {

    /** The list of team members we are editing. */
    private TeamMemberList teamMemberList;
    /** The original list of team members, before we edited it. */
    private TeamMemberList orig;
    /** The table displaying the team member list */
    private JTable table;
    /** The frame containing/displaying the editor  */
    private JFrame frame;
    /** The Save button */
    private JButton saveButton;


    public TeamMemberListEditor(String projectName, TeamMemberList teamList) {
        teamMemberList = new TeamMemberList(orig = teamList);
        teamMemberList.addTableModelListener(this);
        teamMemberList.maybeAddEmptyRow();
        table = new TeamMemberListTable(teamMemberList);
        JPanel buttons = buildButtons();

        frame = new JFrame(projectName + " - Team Members");
        frame.getContentPane().add(makeScrollPane(table));
        frame.getContentPane().add(buttons, BorderLayout.SOUTH);

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        frame.addWindowListener(this);
        frame.setSize(670, 200);
        frame.setVisible(true);
    }

    public void show() {
        teamMemberList.maybeAddEmptyRow();
        frame.setExtendedState(JFrame.NORMAL);
        frame.setVisible(true);
        frame.repaint();
        frame.toFront();
    }

    public void hide() {
        MacGUIUtils.setDirty(frame, teamMemberList.isDirty());
        saveButton.setEnabled(teamMemberList.isDirty());
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
        if (teamMemberList.isReadOnly())
            return true;

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

        teamMemberList.setDirty(false);
        return true;
    }

    public void cancel() {
        if (table.isEditing())
            table.getCellEditor().stopCellEditing();
        // revert back to the original version of the team member list.
        teamMemberList.copyFrom(orig);

        teamMemberList.setDirty(false);

        // notify any listeners
        fireItemCancelled();
    }

    public void confirmClose() {
        if (teamMemberList.isReadOnly()) {
            hide();
            return;
        }

        boolean shouldHide = false;

        if (teamMemberList.isDirty()) {
            int choice =
                JOptionPane.showConfirmDialog(
                    frame,
                    "Would you like to save your changes to the Team Member List?",
                    "Save Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                shouldHide = save();
            }
            else if (choice == JOptionPane.NO_OPTION) {
                cancel();
                shouldHide = true;
            }
        }
        else {
            shouldHide = true;
        }

        if (shouldHide)
            hide();
    }

    private JPanel buildButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));

        JButton button = new JButton("Cancel");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel(); hide(); } });
        buttons.add(button);

        saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (save()) hide(); } });
        saveButton.setEnabled(false);
        buttons.add(saveButton);
        return buttons;
    }

    private JScrollPane makeScrollPane(JTable table) {
        JScrollPane result = new JScrollPane(table);
        JPanel corner = new JPanel();
        corner.setBorder(BorderFactory.createMatteBorder(0, 1, 1, 0,
            table.getGridColor()));
        result.setCorner(JScrollPane.UPPER_RIGHT_CORNER, corner);
        return result;
    }

    public void windowOpened(WindowEvent e) {}
    public void windowClosing(WindowEvent e) { confirmClose(); }
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
        if (saveListeners != null) {
        Iterator i = saveListeners.iterator();
            while (i.hasNext())
                ((SaveListener) i.next()).itemSaved(this);
        }
    }
    protected void fireItemCancelled() {
        if (saveListeners != null) {
            Iterator i = saveListeners.iterator();
            while (i.hasNext())
                ((SaveListener) i.next()).itemCancelled(this);
        }
    }

    public void tableChanged(TableModelEvent e) {
        if (!teamMemberList.isReadOnly() && saveButton != null) {
            MacGUIUtils.setDirty(frame, teamMemberList.isDirty());
            saveButton.setEnabled(teamMemberList.isDirty());
        }
    }

}
