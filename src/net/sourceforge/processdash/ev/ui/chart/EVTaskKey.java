// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev.ui.chart;

import net.sourceforge.processdash.ev.EVTask;

public class EVTaskKey implements Comparable<EVTaskKey> {

    private int ordinal;
    private EVTask task;

    public EVTaskKey(int ordinal, EVTask task) {
        this.ordinal = ordinal;
        this.task = task;
    }

    @Override
    public String toString() {
        return task.getFullName();
    }

    public EVTask getTask() {
        return task;
    }

    public int compareTo(EVTaskKey o) {
        return this.ordinal - o.ordinal;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj instanceof EVTaskKey) {
            EVTaskKey that = (EVTaskKey) obj;
            return this.task.sameNode(that.task)
                    && this.task.getAssignedToText().equals(
                        that.task.getAssignedToText());
        }

        return false;
    }

    @Override
    public int hashCode() {
        int result = 0;

        String fullName = task.getFullName();
        if (fullName != null)
            result = fullName.hashCode();

        String assignedTo = task.getAssignedToText();
        if (assignedTo != null)
            result = result ^ assignedTo.hashCode();

        return result;
    }

}
