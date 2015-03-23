// Copyright (C) 2007-2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.systray;

import java.awt.Font;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.UIManager;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.process.ScriptEnumerator;
import net.sourceforge.processdash.process.ScriptEnumeratorEvent;
import net.sourceforge.processdash.process.ScriptEnumeratorListener;
import net.sourceforge.processdash.process.ScriptID;
import net.sourceforge.processdash.process.ui.ScriptMenuBuilder;

public class ScriptMenuReplicator implements ScriptEnumeratorListener {

    private DashboardContext ctx;

    private DashHierarchy hierarchy;

    private ActiveTaskModel activeTaskModel;

    private PopupMenu popupMenu;

    private String currentPath;

    public static void replicate(ProcessDashboard pdash, PopupMenu dest) {
        new ScriptMenuReplicator(pdash, dest);
    }

    public ScriptMenuReplicator(ProcessDashboard pdash, PopupMenu popupMenu) {
        this.ctx = pdash;
        this.popupMenu = popupMenu;
        popupMenu.add(new ScriptMenuSeparator());

        // listen to application state & mirror the contents of the script menu
        hierarchy = pdash.getHierarchy();
        hierarchy.addHierarchyListener(EventHandler.create(
            DashHierarchy.Listener.class, this, "updateScriptMenuItems"));
        activeTaskModel = pdash.getActiveTaskModel();
        activeTaskModel.addPropertyChangeListener(EventHandler.create(
            PropertyChangeListener.class, this, "updateScriptMenuItems"));
        ScriptEnumerator.addListener(this);
        updateScriptMenuItems();
    }

    public void scriptChanged(ScriptEnumeratorEvent e) {
        if (Filter.pathMatches(currentPath, e.getAffectedPath()))
            updateScriptMenuItems();
    }

    public void updateScriptMenuItems() {
        // look for the last "script menu separator" item to determine where
        // the script menu entries appear in our popup menu.
        int pos = popupMenu.getItemCount() - 1;
        while (!(popupMenu.getItem(pos) instanceof ScriptMenuSeparator)) {
            pos--;
        }

        // now, discard any script menu items that are currently present.
        while (pos-- > 0) {
            if (popupMenu.getItem(pos) instanceof ScriptItemTag)
                popupMenu.remove(pos);
            else
                break;
        }

        // retrieve the current list of script menu items from the dashboard
        PropertyKey currentPhase = activeTaskModel.getNode();
        currentPath = (currentPhase == null ? null : currentPhase.path());
        List<ScriptID> scriptItems = ScriptEnumerator.getScripts(ctx,
            currentPhase);

        if (scriptItems != null && scriptItems.size() > 1) {
            ScriptMenuBuilder b = new ScriptMenuBuilder(scriptItems);
            addMenuItems(popupMenu, b.getMenuItems(), pos);
        }
    }

    private void addMenuItems(Menu destMenu, List menuItems, int pos) {
        for (Object item : menuItems) {
            if (item instanceof String) {
                String dataPath = (String) item;
                destMenu.insert(new ScriptMenuSeparator(), ++pos);
                destMenu.insert(new ScriptMenuHeader(dataPath), ++pos);

            } else if (item instanceof ScriptID) {
                ScriptID script = (ScriptID) item;
                destMenu.insert(new ScriptMenuItem(script), ++pos);

            } else if (item instanceof List) {
                Menu submenu = new ScriptMenuSubmenu();
                destMenu.insert(submenu, ++pos);
                addMenuItems(submenu, (List) item, -1);

            } else {
                System.out.println("Warning! Unrecognized menu item type "
                          + item);
            }
        }
    }

    private interface ScriptItemTag {
    }

    private static class ScriptMenuItem extends MenuItem implements
            ScriptItemTag, ScriptID.DisplayNameListener {
        private ScriptID target;

        public ScriptMenuItem(ScriptID target) {
            this.target = target;
            setLabel(target.getDisplayName(this));
            addActionListener(DISPATCHER);
        }

        public void display() {
            target.display();
        }

        private static final ActionListener DISPATCHER = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((ScriptMenuItem) e.getSource()).display();
            }
        };

        public void displayNameChanged(ScriptID s, String displayName) {
            setLabel(displayName);
        }

    }

    private static class ScriptMenuHeader extends MenuItem implements
            ScriptItemTag {

        public ScriptMenuHeader(String path) {
            int slashPos = path.lastIndexOf('/');
            String label = path.substring(slashPos+1);
            setLabel(label);

            Font f = UIManager.getFont("MenuItem.font");
            setFont(f.deriveFont(Font.BOLD));
        }
    }

    private static class ScriptMenuSeparator extends MenuItem implements
            ScriptItemTag {
        public ScriptMenuSeparator() {
            super("-");
        }
    }

    private static class ScriptMenuSubmenu extends Menu implements ScriptItemTag {
        public ScriptMenuSubmenu() {
            super(MORE_TEXT);
        }
    }

    private static final String MORE_TEXT = Resources.getGlobalBundle()
            .getDlgString("More");

}
