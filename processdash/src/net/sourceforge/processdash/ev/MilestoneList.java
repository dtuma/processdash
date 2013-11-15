// Copyright (C) 2013 Tuma Solutions, LLC
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

import java.util.ArrayList;
import java.util.Collection;

import net.sourceforge.processdash.util.StringUtils;

public class MilestoneList extends ArrayList<Milestone> {

    private Milestone missedMilestone;

    public MilestoneList(Collection data, Milestone missedMilestone) {
        super(data);
        this.missedMilestone = missedMilestone;
    }

    public boolean isMissedMilestone() {
        return missedMilestone != null;
    }

    public Milestone getMissedMilestone() {
        return missedMilestone;
    }

    public String getMissedMilestoneMessage() {
        if (missedMilestone == null)
            return null;
        else
            return EVTaskList.resources.format(
                "Task.Milestone_Date.Single_Error_Msg_FMT",
                missedMilestone.getCommitDate(), missedMilestone.getName());
    }

    public int getMinSortOrdinal() {
        int result = 99999;
        for (Milestone m : this)
            result = Math.min(result, m.getSortOrdinal());
        return result;
    }

    @Override
    public String toString() {
        if (isEmpty())
            return "";
        else
            return StringUtils.join(this, ", ");
    }

}
