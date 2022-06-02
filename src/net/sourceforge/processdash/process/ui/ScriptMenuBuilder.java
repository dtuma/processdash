// Copyright (C) 2009-2022 Tuma Solutions, LLC
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

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.process.ScriptID;

public class ScriptMenuBuilder {

    private boolean isMultiLevel;

    private List menuItems;

    public ScriptMenuBuilder(List<ScriptID> scripts) {
        this(scripts, Settings.getInt("hierarchyMenu.maxItems", 20));
    }

    public ScriptMenuBuilder(List<ScriptID> scripts, int maxItemsPerMenu) {
        scripts = scripts.subList(1, scripts.size());
        maxItemsPerMenu = Math.max(maxItemsPerMenu, 5);
        List<List<ScriptID>> scriptGroups = groupScriptsByDataPath(scripts);
        this.isMultiLevel = (scriptGroups.size() > 1);

        int maxItemsPerGroup;
        if (scripts.size() <= maxItemsPerMenu) {
            maxItemsPerGroup = Integer.MAX_VALUE;
        } else {
            maxItemsPerGroup = calcMaxItemsPerGroup(maxItemsPerMenu,
                scriptGroups);
        }

        List menuItems = new ArrayList();
        for (List<ScriptID> group : scriptGroups)
            addMenuItemsForGroup(menuItems, group, maxItemsPerGroup,
                maxItemsPerMenu);
        this.menuItems = menuItems;
    }

    public boolean isMultiLevel() {
        return isMultiLevel;
    }

    public List getMenuItems() {
        return menuItems;
    }

    private List<List<ScriptID>> groupScriptsByDataPath(List<ScriptID> scripts) {
        List<List<ScriptID>> result = new ArrayList<List<ScriptID>>();
        String currentPath = null;
        List<ScriptID> currentScripts = null;
        for (ScriptID s : scripts) {
            String newDataPath = s.getDataPath();
            if (newDataPath == null)
                continue;
            if (!newDataPath.equals(currentPath)) {
                if (currentScripts != null)
                    result.add(currentScripts);
                currentScripts = new ArrayList<ScriptID>();
            }
            currentPath = newDataPath;
            currentScripts.add(s);
        }
        if (currentScripts != null)
            result.add(currentScripts);
        return result;
    }

    /**
     * When menus get too large, they receive an overflow menu. In the case of a
     * single menu, this logic is simple.
     * 
     * But things can be more complicated for the script menu. When scripts are
     * present at multiple levels of the hierarchy, we display a group for each
     * distinct level; and <b>all</b> of those hierarchy groups must appear in
     * the script button's top-level menu. We never push an entire group into an
     * overflow menu; instead, we include all groups and provide overflows for
     * individual groups as needed.
     * 
     * This method calculates the optimal number of items to display for each
     * hierarchy group, to ensure (a) all groups fit, and (b) we include as much
     * detail on the main menu as possible.
     */
    private int calcMaxItemsPerGroup(int maxItemsPerMenu,
            List<List<ScriptID>> scriptGroups) {
        int numGroups = scriptGroups.size();
        if (numGroups < 2)
            return maxItemsPerMenu;

        int nominalGroupSize = Math.max(1, maxItemsPerMenu / numGroups);
        int largeGroupCount = 0;
        int smallGroupItemCount = 0;
        for (List<ScriptID> group : scriptGroups) {
            int groupSize = group.size();
            if (groupSize <= nominalGroupSize)
                smallGroupItemCount += groupSize;
            else
                largeGroupCount++;
        }

        if (largeGroupCount == 0)
            return nominalGroupSize;

        int spaceForLargeGroups = maxItemsPerMenu - smallGroupItemCount;
        return Math.max(nominalGroupSize,
            spaceForLargeGroups / largeGroupCount);
    }

    private void addMenuItemsForGroup(List menuItems, List<ScriptID> group,
            int maxItemsInFirstMenu, int maxItemsInOtherMenus) {
        String dataPath = group.get(0).getDataPath();
        menuItems.add(dataPath);

        List addToList = menuItems;
        int remainingCapacity = maxItemsInFirstMenu;
        for (int i = 0; i < group.size(); i++) {
            if (remainingCapacity == 1 && i != group.size() - 1) {
                List subMenu = new ArrayList();
                addToList.add(subMenu);
                addToList = subMenu;
                remainingCapacity = maxItemsInOtherMenus;
            }
            addToList.add(group.get(i));
            remainingCapacity--;
        }
    }

}
