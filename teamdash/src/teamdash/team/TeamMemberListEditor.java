// Copyright (C) 2002-2020 Tuma Solutions, LLC
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

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.GuiPrefs;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;

import teamdash.SaveListener;
import teamdash.merge.ui.MergeConflictHyperlinkHandler;
import teamdash.wbs.WBSZoom;
import teamdash.wbs.WBSWindowTitle;
import teamdash.wbs.icons.WBSEditorIcon;


/** A graphical user interface for editing the list of team members.
 */
public class TeamMemberListEditor implements WindowListener,
        TableModelListener, MergeConflictHyperlinkHandler {

    /** The list of team members we are editing. */
    private TeamMemberList teamMemberList;
    /** The original list of team members, before we edited it. */
    private TeamMemberList orig;
    /** A list of the IDs of team members who have been added in this session */
    private Set<Integer> addedTeamMemberIDs;
    /** The table displaying the team member list */
    private JTable table;
    /** The frame containing/displaying the editor  */
    private JFrame frame;
    /** The Save button */
    private JButton saveButton;

    private static final Resources resources = TeamMember.resources;


    public TeamMemberListEditor(TeamMemberList teamList, String initialsPolicy,
            WBSWindowTitle title, GuiPrefs guiPrefs) {
        teamMemberList = new TeamMemberList(orig = teamList);
        teamMemberList.setInitialsPolicyName(initialsPolicy);
        teamMemberList.addTableModelListener(this);
        teamMemberList.setSchedulePrivacyFlags(null);
        teamMemberList.maybeAddEmptyRow();
        table = new TeamMemberListTable(teamMemberList);
        addedTeamMemberIDs = new HashSet();
        JPanel buttons = buildButtons();

        frame = title.register(new JFrame(resources.getString("Window_Title")));
        WBSEditorIcon.setWindowIcon(frame);
        frame.getContentPane().add(makeScrollPane(table));
        frame.getContentPane().add(buttons, BorderLayout.SOUTH);

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        frame.addWindowListener(this);
        frame.setSize(700, 200);
        WBSZoom.get().manage(frame, "size~");
        guiPrefs.load("teamListWindow", frame);
    }

    public void show() {
        teamMemberList.maybeAddEmptyRow();
        teamMemberList.setSchedulePrivacyFlags(addedTeamMemberIDs);
        frame.setExtendedState(JFrame.NORMAL);
        frame.setVisible(true);
        frame.repaint();
        frame.toFront();
    }

    public void hide() {
        MacGUIUtils.setDirty(frame, isDirty());
        saveButton.setEnabled(isDirty());
        frame.setVisible(false);
    }

    public boolean isVisible() {
        return frame.isVisible();
    }

    public boolean isDirty() {
        return teamMemberList.isDirty();
    }

    public void origListWasReplaced() {
        // revert back to the contents of the "orig" list, and hide this window
        cancel();
        hide();

        // scroll the week offset to the start of the new schedule
        teamMemberList.resetWeekOffset();
        // notify the customization hyperlink so it can update its date
        ((TeamMemberListTable) table).updateCustomizationHyperlinkText();
        // reset privacy flags on the orig list
        orig.setSchedulePrivacyFlags(addedTeamMemberIDs);
    }

    public boolean displayHyperlinkedItem(String item) {
        int id = Integer.parseInt(item);
        int pos = teamMemberList.findTeamMemberPosByID(id);
        if (pos == -1)
            return false;

        table.setRowSelectionInterval(pos, pos);
        table.setColumnSelectionInterval(0, 0);
        table.scrollRectToVisible(table.getCellRect(pos, 0, true));

        show();
        return true;
    }

    private boolean checkForErrors() {
        Object[] errors = teamMemberList.getErrors();
        if (errors == null) return true;

        Object[] message = new Object[] {
            resources.getString("Errors.Header"),
            new JList(errors) };
        JOptionPane.showMessageDialog
            (frame, message, resources.getString("Errors.Title"),
             JOptionPane.ERROR_MESSAGE);
        return false;
    }

    private boolean confirmDestructiveChanges(Object[] changes) {
        if (changes == null) return true;

        Object[] message = new Object[] {
            resources.getStrings("Warning.Header"),
            new JList(changes),
            resources.getStrings("Warning.Footer") };
        return (JOptionPane.YES_OPTION ==
                JOptionPane.showConfirmDialog
                    (frame, message, resources.getString("Warning.Title"),
                     JOptionPane.YES_NO_OPTION));
    }

    public boolean save() {
        if (teamMemberList.isReadOnly())
            return true;

        stopEditing();

        // don't save if there are errors.
        if (!checkForErrors()) return false;

        // confirm that the user wants to perform the changes.
        TeamMemberList.Delta[] irreversibleChanges =
            orig.calculateDelta(teamMemberList);
        if (!confirmDestructiveChanges(irreversibleChanges)) return false;

        // Assign IDs to any newly added team members.
        teamMemberList.eraseDeletedTeamMemberIDs(irreversibleChanges);
        teamMemberList.assignMissingUniqueIDs(addedTeamMemberIDs);

        // commit and save the changes
        orig.publishChanges(irreversibleChanges);
        orig.copyFrom(teamMemberList);
        orig.setSchedulePrivacyFlags(addedTeamMemberIDs);

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
        stopEditing();
        // revert back to the original version of the team member list.
        teamMemberList.copyFrom(orig);
        ((TeamMemberListTable) table).updateCustomizationHyperlinkText();

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

        if (isDirty()) {
            int choice =
                JOptionPane.showConfirmDialog(
                    frame,
                    resources.getString("Save.Prompt"),
                    resources.getString("Save.Title"),
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

    public void stopEditing() {
        if (table.isEditing())
            // stop editing the current table cell.
            table.getCellEditor().stopCellEditing();
    }

    private JPanel buildButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));

        JButton button = new JButton(resources.getString("Cancel"));
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel(); hide(); } });
        buttons.add(button);

        saveButton = new JButton(resources.getString("OK"));
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (save()) hide(); } });
        saveButton.setEnabled(false);
        buttons.add(saveButton);
        return buttons;
    }

    /**
     * When used in the context of the WBS Editor, the Team Member List has
     * an "OK" button.  When used as a standalone application, label the
     * button "Save" instead.
     * 
     * @param showSave true if the commit button should display the word
     *     "Save", false if it should display the word "OK".
     */
    public void setCommitButtonIsSave(boolean showSave) {
        saveButton.setText(resources.getString(showSave ? "Save" : "OK"));
    }

    public void setOnlyEditableFor(String initials) {
        teamMemberList.setOnlyEditableFor(initials);
        orig.setOnlyEditableFor(initials);
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
            MacGUIUtils.setDirty(frame, isDirty());
            saveButton.setEnabled(isDirty());
        }
    }

}
