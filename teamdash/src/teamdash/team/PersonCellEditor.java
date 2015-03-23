// Copyright (C) 2010-2013 Tuma Solutions, LLC
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

import static net.sourceforge.processdash.util.NullSafeObjectUtils.EQ;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.EventHandler;
import java.util.EventObject;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.sourceforge.processdash.ui.macosx.MacGUIUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

import teamdash.team.TeamMemberList.InitialsPolicy;

public class PersonCellEditor extends DefaultCellEditor {

    private static final String SEARCH_KEY = "userSearch";

    private Icon personIcon;

    private Box editorPanel;

    private JButton lookupButton;

    private JTextField textField;

    private Timer dispatchKeyEvent;

    private KeyEvent startingKey;

    private JFrame parentFrame;

    private TeamMemberList teamList;

    private int row;

    private int col;

    public PersonCellEditor() {
        this(new JTextField());
    }

    private PersonCellEditor(JTextField f) {
        super(f);
        this.textField = f;

        personIcon = new ImageIcon(PersonCellRenderer.class
                .getResource("person.png"));
        lookupButton = new JButton(personIcon);
        lookupButton.setBorder(BorderFactory.createEtchedBorder());
        lookupButton.addActionListener(EventHandler.create(
            ActionListener.class, this, "openLookupDialog"));

        InputMap m = f.getInputMap();
        int modifier = MacGUIUtils.getCtrlModifier();
        m.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, modifier), SEARCH_KEY);
        m.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, modifier), SEARCH_KEY);
        f.getActionMap().put(SEARCH_KEY, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                lookupButton.doClick();
            }});

        this.dispatchKeyEvent = new Timer(30, EventHandler.create(
            ActionListener.class, this, "dispatchKeyEvent"));
        this.dispatchKeyEvent.setRepeats(false);

        editorPanel = Box.createHorizontalBox();
        editorPanel.add(lookupButton);
        editorPanel.add(f);
    }


    @Override
    public boolean isCellEditable(EventObject anEvent) {
        this.startingKey = null;

        // when the user single-clicks on the person icon, we want to display
        // information about the user in question, or open the user lookup
        // dialog.  So give the OK to start editing if we detect a click on
        // the far left side of the cell (where the person icon is).
        if (anEvent instanceof MouseEvent) {
            MouseEvent me = (MouseEvent) anEvent;
            if (me.getX() < personIcon.getIconWidth())
                return true;
        }

        // If the user types a key in this cell, we probably want to start
        // editing.  However, Swing by default will direct the key event to
        // our Box panel, which isn't helpful.  Instead, keep the key event
        // around so we can do something with it after editing starts.
        if (anEvent instanceof KeyEvent) {
            KeyEvent key = (KeyEvent) anEvent;
            if (super.isCellEditable(anEvent)) {
                this.startingKey = key;
                dispatchKeyEvent.restart();
                return true;
            }
        }

        return super.isCellEditable(anEvent);
    }

    /** if editing was started via a key event, this method will get called
     * shortly afterward.
     */
    public void dispatchKeyEvent() {
        // if the Ctrl or Meta key was down during the start editing key,
        // respond by auto-clicking our lookup button.  This allows key
        // sequences like Ctrl-Enter to jump directly into the user lookup
        // dialog.
        if (startingKey != null
                && (startingKey.isControlDown() || startingKey.isMetaDown())) {
            lookupButton.doClick();
            return;
        }

        // by default, Swing will try to assign focus to the Box that contains
        // our button and text field.  That isn't helpful. Instead, transfer
        // the focus to the text field.  Then, if editing was started via the
        // press of a letter or digit, immediately place that into the text
        // field (overwriting the contents that are already there).
        textField.grabFocus();
        if (startingKey != null) {
            char c = startingKey.getKeyChar();
            if (Character.isLetterOrDigit(c))
                textField.setText(String.valueOf(c));
        }
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int col) {

        this.teamList = (TeamMemberList) table.getModel();
        this.parentFrame = (JFrame) SwingUtilities.getWindowAncestor(table);
        this.row = row;
        this.col = col;

        super.getTableCellEditorComponent(table, value, isSelected, row, col);

        return editorPanel;
    }

    /**
     * This method gets called when the user clicks the lookup button.
     */
    public void openLookupDialog() {
        // stop the cell editing operation.  This way, if they have typed
        // something into the text field, it will be available in the
        // TeamMember object's "name" field for the Person Lookup Dialog to use.
        stopCellEditing();

        // retrieve the team member and make a note of the starting values.
        TeamMember m = teamList.get(row);
        String nameBefore = m.getName();
        String serverStringBefore = m.getServerIdentityInfo();

        // open a PersonLookupDialog.  This method will block until the user
        // finishes interacting with the dialog.
        new PersonLookupDialog(parentFrame, m);

        // retrieve the final values of the name & identity fields.  If they
        // have changed, mark the team member list as dirty.
        String nameAfter = m.getName();
        String serverStringAfter = m.getServerIdentityInfo();
        if (!EQ(nameBefore, nameAfter)
                || !EQ(serverStringBefore, serverStringAfter)) {
            maybeAutoSetInitials(m);
            teamList.setDirty(true);
        }

        // fire the appropriate events
        teamList.fireTableCellUpdated(row, col);
        teamList.maybeAddEmptyRow();
    }

    private void maybeAutoSetInitials(TeamMember m) {
        if (teamList.getInitialsPolicy() != InitialsPolicy.Username)
            return;

        String serverInfoStr = m.getServerIdentityInfo();
        if (!StringUtils.hasValue(serverInfoStr))
            return;

        Map serverInfo = HTMLUtils.parseQuery(serverInfoStr);
        String username = (String) serverInfo.get("username");
        if (StringUtils.hasValue(username)) {
            m.setInitials(username);
            teamList.fireTableCellUpdated(row, TeamMemberList.INITIALS_COLUMN);
        }
    }

}
