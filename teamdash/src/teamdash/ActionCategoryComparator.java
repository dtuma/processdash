// Copyright (C) 2002-2010 Tuma Solutions, LLC
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

package teamdash;

import java.util.Comparator;
import java.util.List;

import javax.swing.Action;

/**
 * Class used to compare 2 Actions. The property that is used to compare the Actions is
 *  their category. The category is simply the value under the ACTION_CATEGORY key,
 *  in the Actions' key/value list.
 */
public class ActionCategoryComparator implements Comparator<Action> {

    /** The Action category key, used in the Action's key/value list */
    public static final String ACTION_CATEGORY = "actionCategory";

    /** A array that contains all categories in the order that the comparator
     * should sort them */
    private List<String> categoryOrder = null;

    public ActionCategoryComparator(List<String> categoryOrder) {
        this.categoryOrder = categoryOrder;
    }

    public int compare(Action a1, Action a2) {
        int categoryPos1 = categoryOrder.indexOf((String) a1.getValue(ACTION_CATEGORY));
        int categoryPos2 = categoryOrder.indexOf((String) a2.getValue(ACTION_CATEGORY));

        // If an Action's category is not in the list, it should be considered as
        //  "greater than" the other action.
        if (categoryPos1 == -1)
            categoryPos1 = Integer.MAX_VALUE;
        if (categoryPos2 == -1)
            categoryPos2 = Integer.MAX_VALUE;

        return categoryPos1 - categoryPos2;
    }

}
