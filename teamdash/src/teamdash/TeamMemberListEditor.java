package teamdash;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

public class TeamMemberListEditor {

    TeamMemberList teamMemberList, orig;
    JTable table;
    JPanel buttons;
    JFrame frame;

    public TeamMemberListEditor(TeamProject teamProject) {
        this(teamProject.getTeamMemberList());
    }

    public TeamMemberListEditor(TeamMemberList teamList) {
        teamMemberList = new TeamMemberList(orig = teamList);
        teamMemberList.maybeAddEmptyRow();
        buildTable();
        buildButtons();

        frame = new JFrame("Team Members");
        frame.getContentPane().add(new JScrollPane(table));
        frame.getContentPane().add(buttons, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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
            table.getCellEditor().stopCellEditing();

        if (!checkForErrors()) return false;

        TeamMemberList.Delta[] irreversibleChanges =
            orig.calculateDelta(teamMemberList);
        if (!confirmDestructiveChanges(irreversibleChanges)) return false;

        orig.publishChanges(irreversibleChanges);
        orig.copyFrom(teamMemberList);
        teamMemberList.copyFrom(orig);
        teamMemberList.maybeAddEmptyRow();
        return true;
    }

    public void cancel() {
        if (table.isEditing())
            table.getCellEditor().stopCellEditing();
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

    private void buildButtons() {
        buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));

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
    }
}
