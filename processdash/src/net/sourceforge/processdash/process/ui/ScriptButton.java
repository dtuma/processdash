// Copyright (C) 2000-2015 Tuma Solutions, LLC
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

package net.sourceforge.processdash.process.ui;

import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.process.ScriptEnumerator;
import net.sourceforge.processdash.process.ScriptEnumeratorEvent;
import net.sourceforge.processdash.process.ScriptEnumeratorListener;
import net.sourceforge.processdash.process.ScriptID;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.DropDownButton;
import net.sourceforge.processdash.ui.lib.PaddedIcon;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;

/** The scriptButton class activates a browser loaded with the current phase's
 * script when it is pressed.  A right click can activate a popup menu with
 * a selection of scripts.
 */
public class ScriptButton extends DropDownButton implements
        PropertyChangeListener, DashHierarchy.Listener,
        ScriptEnumeratorListener {

    private static final Resources resources = Resources.getDashBundle(
            "ProcessDashboard.ScriptButton");

    private static final String DROP_DOWN_BEHAVIOR_PREF = Settings.PREFS_PREFIX
              + "scriptButton.clickOpensDefault";

    ProcessDashboard parent = null;
    Icon enabled_icon = null;
    String scriptFilename = null;
    String path  = null;
    boolean clickOpensDefault = true;
    List<ScriptID> paths = null; // A list of valid script paths for the popup menu
    JMenuItem moreItem = null;

    public static final String URL_PREFIX = "/";

    public ScriptButton(ProcessDashboard dash) {
        super();
        PCSH.enableHelp(this, "AccessingScripts");
        PCSH.enableHelpKey(getMenu(), "AccessingScripts");
        setMainButtonMargin(new Insets (1,2,1,2));
        enabled_icon = DashboardIconFactory.getScriptIcon();
        getButton().setIcon(padIcon(enabled_icon));
        getButton().setToolTipText(resources.getString("Tooltip"));
        getButton().setFocusPainted(false);
        parent = dash;

        moreItem = new JMenuItem(resources.getString("More_Menu"));
        moreItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new ScriptBrowser(ScriptButton.this.parent, true); } } );

        dash.getActiveTaskModel().addPropertyChangeListener(this);
        dash.getHierarchy().addHierarchyListener(this);
        ScriptEnumerator.addListener(this);
        InternalSettings.addPropertyChangeListener(DROP_DOWN_BEHAVIOR_PREF, this);
        updateAll();
    }

    private Icon padIcon(Icon icon) {
        if (MacGUIUtils.isMacOSX())
            return new PaddedIcon(icon, 0, 2, 0, 2);
        else
            return icon;
    }

    private void updateAll() {
        String clickPref = Settings.getVal(DROP_DOWN_BEHAVIOR_PREF);
        clickOpensDefault = "true".equals(clickPref);
        setMainButtonBehavior(clickOpensDefault ? RUN_FIRST_MENU_OPTION
                : OPEN_DROP_DOWN_MENU);
        PropertyKey currentPhase = parent.getActiveTaskModel().getNode();
        path = (currentPhase == null ? null : currentPhase.path());
        setPaths(ScriptEnumerator.getScripts(parent, currentPhase));
    }

    // setPaths populates the popup menu with valid script paths.
    // However, it does not command the menu to be displayed.
    private void setPaths(List<ScriptID> v) {
        paths = v;
        getMenu().removeAll();
        JMenu destMenu = getMenu();

        // populate the popup menu with items for each script.
        if (paths != null && paths.size() > 0) {

            // add the current (default) menu.
            destMenu.add(new ScriptMenuItem(paths.get(0)));

            ScriptMenuBuilder b = new ScriptMenuBuilder(paths);
            addMenuItems(destMenu, b.isMultiLevel(), b.getMenuItems());
            getMenu().addSeparator();
        }

        getMenu().add(moreItem);
    }

    private void addMenuItems(JMenu destMenu, boolean showSeparatorLabels,
              List menuItems) {
        for (Object item : menuItems) {
            if (item instanceof String) {
                String dataPath = (String) item;
                if (showSeparatorLabels)
                    destMenu.add(new ScriptMenuSeparator(dataPath));
                else
                    destMenu.addSeparator();

            } else if (item instanceof ScriptID) {
                ScriptID script = (ScriptID) item;
                destMenu.add(new ScriptMenuItem(script));

            } else if (item instanceof List) {
                JMenu submenu = new JMenu(resources.getDlgString("More"));
                destMenu.add(submenu);
                addMenuItems(submenu, showSeparatorLabels, (List) item);

            } else {
                System.out.println("Warning! Unrecognized menu item type "
                          + item);
            }
        }
    }

    /** ScriptMenuItem is an extended JMenuItem class with built-in
     * logic for displaying a script item. */
    public class ScriptMenuItem extends JMenuItem implements ActionListener,
              ScriptID.DisplayNameListener {
        ScriptID id;

        public ScriptMenuItem(ScriptID id) {
            this.id = id;
            setText(id.getDisplayName(this));
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            id.display();
        }

        public void displayNameChanged(ScriptID s, String displayName) {
            setText(displayName);
        }
    }

    private class ScriptMenuSeparator extends JPanel {
        public ScriptMenuSeparator(String path) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(null);
            setOpaque(false);

            add(new JSeparator());

            int slashPos = path.lastIndexOf('/');
            String title = path.substring(slashPos+1);
            JLabel l = new JLabel(title);
            Font f = l.getFont();
            l.setFont(f.deriveFont(f.getSize2D() * 0.8f));
            l.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 10));
            add(l);
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        updateAll();
    }

    public void hierarchyChanged(DashHierarchy.Event e) {
        updateAll();
    }

    public void scriptChanged(ScriptEnumeratorEvent e) {
        if (Filter.pathMatches(path, e.getAffectedPath()))
            updateAll();
    }

}
