// Copyright (C) 2008-2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev;

import java.util.HashMap;
import java.util.Map;


public class EVLeafTaskFilter implements EVTaskFilter {

    protected EVTaskFilterCondition[] leafTests;

    protected EVTaskFilter nextFilter;

    protected Map<String, String> attributes;

    public EVLeafTaskFilter(EVTaskFilterCondition... tests) {
        this.leafTests = tests;
    }

    public EVTaskFilter appendFilter(EVTaskFilter filter) {
        this.nextFilter = filter;
        return this;
    }

    public EVTaskFilter putAttribute(String name, String value) {
        if (attributes == null)
            attributes = new HashMap<String, String>();
        attributes.put(name, value);
        return this;
    }

    public boolean include(EVTask t) {
        if (!isIncludedByLeafTests(t))
            return false;
        else if (nextFilter != null)
            return nextFilter.include(t);
        else
            return true;
    }

    protected boolean isIncludedByLeafTests(EVTask t) {
        if (t.isLeaf()) {
            // iterate over all of the leaf tests we're checking.
            for (EVTaskFilterCondition oneTest : leafTests) {
                // All tests must pass for the leaf to be included; so if
                // any of them fails, the leaf should be excluded.
                if (oneTest.include(t) == false)
                    return false;
            }
            return true;
        }

        for (int i = t.getNumChildren(); i-- > 0;) {
            EVTask child = t.getChild(i);
            if (isIncludedByLeafTests(child))
                return true;
        }
        return false;
    }

    public String getAttribute(String name) {
        if (attributes != null && attributes.containsKey(name))
            return attributes.get(name);
        else if (nextFilter != null)
            return nextFilter.getAttribute(name);
        else
            return null;
    }

    public static final EVTaskFilterCondition COMPLETED = new EVTaskFilterCondition() {
        public boolean include(EVTask task) {
            return task.getDateCompleted() != null;
        }
    };

    public static final EVTaskFilterCondition IN_PROGRESS = new EVTaskFilterCondition() {
        public boolean include(EVTask task) {
            return task.getActualDirectTime() > 0 &&
                    task.getDateCompleted() == null;
        }
    };

    public static final EVTaskFilterCondition HAS_PLAN_TIME = new EVTaskFilterCondition() {
        public boolean include(EVTask task) {
            return task.getPlanTime() > 0;
        }
    };

    public static final EVTaskFilterCondition HAS_ACTUAL_TIME = new EVTaskFilterCondition() {
        public boolean include(EVTask task) {
            return task.getActualTime() > 0;
        }
    };

}
