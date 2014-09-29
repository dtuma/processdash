// Copyright (C) 2006-2014 Tuma Solutions, LLC
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

import java.util.EventObject;

public class EVTaskListMerged extends EVTaskList {

    private EVTaskListMerger merger;

    private EVTaskList origTaskList;

    private Recalculator mergeRecalcTrigger;

    public EVTaskListMerged(EVTaskList tl, boolean simplify,
            boolean preserveLeaves, EVTaskFilter filter) {
        super(tl.taskListName, tl.getDisplayName(), false);
        merger = new EVTaskListMerger(tl, simplify, preserveLeaves, filter);
        this.root = merger.getMergedTaskRoot();
        this.schedule = tl.schedule;
        this.taskListID = tl.taskListID;
        this.calculator = tl.calculator;
        this.origTaskList = tl;
        tl.addRecalcListener(mergeRecalcTrigger = new Recalculator());
    }

    public void unregisterListeners() {
        origTaskList.removeRecalcListener(mergeRecalcTrigger);
    }

    private class Recalculator implements RecalcListener {

        public void evRecalculated(EventObject e) {
            merger.recalculate();
            fireTreeStructureChanged(this, new Object[] { root }, new int[0],
                    null);
        }

    }
}
