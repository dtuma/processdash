// Copyright (C) 2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib.autocomplete;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;

public class AssignedToEditList extends ArrayList<AssignedToEditList.Change> {

    public static class Change {

        public String origInitials, origTime, newInitials, newTime;

        public boolean isAdd() {
            return origInitials == null;
        }

        public boolean isDelete() {
            return newInitials == null;
        }

        public boolean isInitialsChange() {
            return origInitials != null && newInitials != null
                    && !origInitials.equals(newInitials);
        }

        public boolean isTimeChange() {
            return !eq(origTime, newTime);
        }

        public boolean isChange() {
            return !isAdd() && !isDelete() && !isNoop();
        }

        public boolean isNoop() {
            return eq(origInitials, newInitials) && eq(origTime, newTime);
        }

        public double getTimeRatio(double defaultTime) {
            if (eq(origTime, newTime))
                return 1.0;
            double origVal = getTimeOrDefault(origTime, defaultTime);
            double newVal = getTimeOrDefault(newTime, defaultTime);
            return newVal / origVal;
        }

        private double getTimeOrDefault(String time, double defaultTime) {
            if (time == null)
                return defaultTime;

            else if ("".equals(time))
                return 0;

            try {
                return FORMATTER.parse(time.toString()).doubleValue();
            } catch (ParseException nfe) {
            }

            return Double.NaN;
        }

    }


    public boolean isNoop() {
        for (Change c : this)
            if (!c.isNoop())
                return false;
        return true;
    }

    @Override
    public String toString() {
        if (isEmpty())
            return "";

        StringBuilder result = new StringBuilder();
        for (Change change : this) {
            if (!change.isDelete()) {
                result.append(AssignedToDocument.SEPARATOR_SPACE);
                result.append(change.newInitials);
                if (change.newTime != null)
                    result.append("(").append(change.newTime).append(")");
            }
        }
        return result.length() == 0 ? "" : result.substring(2);
    }

    private static boolean eq(String a, String b) {
        if (a == b)
            return true;
        if (a == null)
            return false;
        return a.equals(b);
    }

    protected static final NumberFormat FORMATTER = NumberFormat
            .getNumberInstance();

}
