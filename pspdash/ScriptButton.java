// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import javax.swing.*;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Vector;

// The scriptButton class activates a browser loaded with the current phase's
// script when it is pressed.  A right click can activate a popup menu with
// a selection of scripts.
class ScriptButton extends DropDownButton {
    PSPDashboard parent = null;
    ImageIcon enabled_icon = null;
    ImageIcon disabled_icon = null;
    String scriptFilename = null;
    String path  = null;
    Vector paths = null; // A list of valid script paths for the popup menu
    JMenuItem moreItem = null;

    public static final String URL_PREFIX = "/";

    ScriptButton(PSPDashboard dash) {
        super();
        PCSH.enableHelp(this, "AccessingScripts");
        PCSH.enableHelpKey(getMenu(), "AccessingScripts");
        try {
            enabled_icon = new ImageIcon(getClass().getResource("script.gif"));
            disabled_icon = new ImageIcon(getClass().getResource("scriptd.gif"));
            getButton().setIcon(enabled_icon);
            getButton().setDisabledIcon(disabled_icon);
        } catch (Exception e) {
            getButton().setText("Script");
        }
        getButton().setMargin (new Insets (1,2,1,2));
        getButton().setFocusPainted(false);
        parent = dash;

        moreItem = new JMenuItem("More...");
        moreItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new ScriptBrowser(ScriptButton.this.parent, true); } } );

        dash.getContentPane().add(this);
    }

    // setPaths populates the popup menu with valid script paths.
    // However, it does not command the menu to be displayed.
    public void setPaths(Vector v) {
        paths = v;
        getMenu().removeAll();
        ScriptID id;

        // populate the popup menu with items for each script.
        if (paths != null) for (int i = 0;  i < paths.size();  i++) {
            id = (ScriptID) paths.elementAt (i);
            getMenu().add(new ScriptMenuItem(id));
            if (i == 0) getMenu().addSeparator();
        }

        if (paths.size() > 0)
            getMenu().addSeparator();
        getMenu().add(moreItem);
    }

    /** ScriptMenuItem is an extended JMenuItem class with built-in
     * logic for displaying a script item. */
    public class ScriptMenuItem extends JMenuItem implements ActionListener {
        ScriptID id;

        public ScriptMenuItem(ScriptID id) {
            super(id.getDisplayName());
            this.id = id;
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            id.display();
        }
    }

}
