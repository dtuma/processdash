// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.process.ui;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;


import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.process.*;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.DropDownButton;

// The scriptButton class activates a browser loaded with the current phase's
// script when it is pressed.  A right click can activate a popup menu with
// a selection of scripts.
public class ScriptButton extends DropDownButton implements PropertyChangeListener {
    ProcessDashboard parent = null;
    Icon enabled_icon = null;
    Icon disabled_icon = null;
    String scriptFilename = null;
    String path  = null;
    Vector paths = null; // A list of valid script paths for the popup menu
    JMenuItem moreItem = null;

    public static final String URL_PREFIX = "/";

    public ScriptButton(ProcessDashboard dash) {
        super();
        PCSH.enableHelp(this, "AccessingScripts");
        PCSH.enableHelpKey(getMenu(), "AccessingScripts");
        try {
            enabled_icon = DashboardIconFactory.getScriptIcon();
            disabled_icon = DashboardIconFactory.getDisabledScriptIcon();
            getButton().setIcon(enabled_icon);
            getButton().setDisabledIcon(disabled_icon);
        } catch (Exception e) {
            getButton().setText("Script");
        }
        getButton().setMargin (new Insets (1,2,1,2));
        getButton().setFocusPainted(false);
        parent = dash;

        moreItem = new JMenuItem(Resources.getGlobalBundle().getDlgString("More"));
        moreItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new ScriptBrowser(ScriptButton.this.parent, true); } } );

        dash.getActiveTaskModel().addPropertyChangeListener(this);
        dash.getContentPane().add(this);
    }

    // setPaths populates the popup menu with valid script paths.
    // However, it does not command the menu to be displayed.
    public void setPaths(Vector v) {
        paths = v;
        getMenu().removeAll();
        ScriptID id;
        String dataPath = null;
        boolean useSubmenus = useSubmenus(paths);
        JMenu destMenu = getMenu();

        // populate the popup menu with items for each script.
        if (paths != null && paths.size() > 0) {

            // add the current (default) menu.
            destMenu.add(new ScriptMenuItem((ScriptID) paths.elementAt (0)));
            destMenu.addSeparator();

            // add menu items for the rest of the scripts
            for (int i = 1;  i < paths.size();  i++) {
                id = (ScriptID) paths.elementAt (i);
                if (id.getDataPath() == null || id.getScript() == null)
                    continue;
                if (!id.getDataPath().equals(dataPath)) {
                    dataPath = id.getDataPath();
                    if (useSubmenus) {
                        int pos = dataPath.lastIndexOf('/');
                        destMenu = new JMenu(dataPath.substring(pos+1));
                        getMenu().add(destMenu);
                    } else if (i > 1) {
                        destMenu.addSeparator();
                    }
                }
                destMenu.add(new ScriptMenuItem(id));
            }
            getMenu().addSeparator();
        }

        getMenu().add(moreItem);
    }
    private boolean useSubmenus(Vector v) {
        if (v == null || v.size() < 2) return false;
        String dataPath = ((ScriptID) paths.elementAt (0)).getDataPath();
        String newDataPath;
        for (int i = v.size();   i-- > 1; ) {
            newDataPath = ((ScriptID) paths.elementAt (i)).getDataPath();
            if (dataPath != null && !dataPath.equals(newDataPath))
                return true;
            dataPath = newDataPath;
        }
        return false;
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

    public void propertyChange(PropertyChangeEvent evt) {
        PropertyKey currentPhase = parent.getActiveTaskModel().getNode();
        setPaths(parent.getHierarchy().getScriptIDs(currentPhase));
    }

}
