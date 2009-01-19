// Copyright (C) 2009 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

package net.sourceforge.processdash.tool.prefs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.SortedSet;
import java.util.TreeSet;

import net.sourceforge.processdash.i18n.Resources;

public class PreferencesCategory implements Comparable<PreferencesCategory> {
    /** The place we look for categories display string */
    private static final String DEFAULT_DISPLAY_STRING_LOCATION = "Tools.Prefs.Category";

    /** The category id */
    private String categoryID = null;

    /** The category display name */
    private String displayName = null;

    /** The PreferencesPanes defining this PreferenceCategory. The panes are ordered
     *   according to their priority. */
    private SortedSet<PreferencesPane> panes = null;

    public PreferencesCategory(String categoryID) {
        this.categoryID = categoryID;
        this.panes = new TreeSet<PreferencesPane>();
    }

    public void addPane(PreferencesPane pane) {
        displayName = null;
        panes.add(pane);
    }

    @Override
    public String toString() {
        if (displayName == null) {
            // We fetch the display string for the category. We start by looking
            //  in the "Tools.Prefs.Category.<categoryID>.Label" bundle. If no such
            //  key exist, we iterate through the panes (which are ordered according
            //  to their priority) and use the first display name we find. If none
            //  is found, we use the categoryID.
            Resources resources = Resources.getDashBundle(DEFAULT_DISPLAY_STRING_LOCATION);
            try {
                displayName = resources.getString(categoryID + ".Label");
            } catch (MissingResourceException e) {}

            if (displayName == null && panes.size() > 0) {
                Iterator<PreferencesPane> it = panes.iterator();

                while (it.hasNext() && displayName == null) {

                    try {
                        displayName = it.next().getCategoryDisplayName();
                    } catch (MissingResourceException e) {
                        displayName = null;
                    }
                }

                if (displayName == null) {
                    displayName = this.categoryID;
                }
            }
        }

        return displayName;
    }

    public SortedSet<PreferencesPane> getPanes() {
        return panes;
    }

    public String getCategoryID() {
        return categoryID;
    }

    /**
     * Returns 0 if both categories have the same priority, a positive
     *  number if this category's priority is less than the argument and
     *  a negative number if its higher than the argument. To determine the
     *  priority of a category, we look at the highest priority
     *  preferences-pane that is contained in the in the category.
     */
    public int compareTo(PreferencesCategory o) {
        return o.panes.first().getPriority() - this.panes.first().getPriority();
    }

}
