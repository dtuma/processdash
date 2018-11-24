// Copyright (C) 2018 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.diff.impl.git.ui;

import static net.sourceforge.processdash.tool.diff.impl.git.ui.GitLOCDiffPanel.resources;
import static org.eclipse.jgit.diff.DiffEntry.Side.NEW;
import static org.eclipse.jgit.diff.DiffEntry.Side.OLD;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.eclipse.jgit.diff.DiffEntry;

import net.sourceforge.processdash.tool.diff.impl.git.GitFileSet;


public class GitIDSelector {

    private DiffEntry.Side side;

    private GitIDSelector downstreamSelector;

    private String selectedID;

    private JTextField textField;

    private Font plain, bold;

    private JMenu menu;

    private JMenuBar menubar;

    private JButton button;

    public GitIDSelector(DiffEntry.Side side, GitIDSelector downstream) {
        this.side = side;

        buildTextField();
        buildMenu();

        this.downstreamSelector = downstream;
    }

    public List getUIControls() {
        return Arrays.asList(textField, 2, menubar, button);
    }

    public String getSelectedID() {
        if (selectedID != null) {
            return selectedID;
        } else {
            String text = textField.getText().trim();
            return (text.length() > 0 ? text : null);
        }
    }

    public void selectMenuItem(int pos) {
        JMenuItem menuItem = (JMenuItem) menu.getMenuComponent(pos);
        menuItem.doClick();
    }

    private void buildTextField() {
        // create an empty text field
        this.textField = new JTextField();
        textField.setToolTipText(resources.getString("ID_Tooltip"));
        this.plain = textField.getFont();
        this.bold = plain.deriveFont(Font.BOLD);

        // when the user types in the text field, delete our stored ID
        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void removeUpdate(DocumentEvent e) {
                handleChange();
            }

            public void insertUpdate(DocumentEvent e) {
                handleChange();
            }

            public void changedUpdate(DocumentEvent e) {
                handleChange();
            }

            private void handleChange() {
                textField.setFont(plain);
                selectedID = null;
            }
        });

        // select all text when the field gains focus
        textField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                int len = textField.getDocument().getLength();
                textField.setCaretPosition(len);
                textField.moveCaretPosition(0);
            }
        });
    }

    private void buildMenu() {
        // create a menu that takes up no space
        this.menu = new JMenu(" ");
        menu.setMinimumSize(new Dimension(0, 5));
        menu.setPreferredSize(new Dimension(0, 5));
        menu.setMaximumSize(new Dimension(0, 100));

        // add items to the menu for selecting comparison targets
        if (side == NEW)
            menu.add(new IDMenuItem("Working_Dir", GitFileSet.WORKING_DIR));
        menu.add(new IDMenuItem("Index", GitFileSet.INDEX));
        menu.add(new IDMenuItem("HEAD_", "HEAD"));
        if (side == OLD)
            menu.add(new IDMenuItem("Parent", GitFileSet.PARENT));

        // add the menu to a menubar
        menubar = new JMenuBar();
        menubar.add(menu);

        // create a button that opens the menu
        this.button = new JButton(new ShowMenuAction());
    }

    private class IDMenuItem extends AbstractAction {

        private String id;

        private int pos;

        IDMenuItem(String resKey, String id) {
            super(resources.getString(resKey));
            this.id = id;
            this.pos = menu.getMenuComponentCount();
            if (pos == 0)
                actionPerformed(null);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            textField.setText((String) getValue(NAME));
            textField.setFont(bold);
            selectedID = id;

            if (downstreamSelector != null)
                downstreamSelector.selectMenuItem(pos);
        }

    }

    private class ShowMenuAction extends AbstractAction {

        private Timer timer;

        ShowMenuAction() {
            super("...");
            timer = new Timer(10, this);
            timer.setRepeats(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == timer)
                menu.doClick(0);
            else
                timer.restart();
        }

    }

}
