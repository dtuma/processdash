// Copyright (C) 2002-2021 Tuma Solutions, LLC
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

package teamdash.wbs.columns;

import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSNode;

/** Abstract implementation of DataColumn interface for a numeric column.
 */
public abstract class AbstractNumericColumn extends AbstractDataColumn {

    protected double fuzzFactor = 0.05;


    public Class getColumnClass() { return NumericDataValue.class; }


    protected double getValueForNode(WBSNode node) { return 0; }


    public Object getValueAt(WBSNode node) {
        return new NumericDataValue(getValueForNode(node));
    }


    protected void setValueForNode(double value, WBSNode node) { }

    public void setValueAt(Object aValue, WBSNode node) {
        if (isNoOpEdit(aValue, node))
            // if the user started an editing session but ultimately made no
            // changes, do nothing
            ;
        else
            setValueForNode(NumericDataValue.parse(aValue), node);
    }

    protected boolean isNoOpEdit(Object newValue, WBSNode node) {
        if (newValue instanceof String) {
            // When a user enters a new value into a table cell, the setValueAt
            // method is called with a String containing the value they typed.
            // If that string is identical to the string we get when we format
            // the current cell value, then no changes have been made. This
            // pattern would occur if a user double-clicked on a cell, then
            // pressed enter without making any changes.
            String oldStr = String.valueOf(getValueAt(node));
            return newValue.equals(oldStr);
        } else {
            // if setValueAt is called with anything other than a string, the
            // method call came from within other code rather than from a
            // user's editing session.
            return false;
        }
    }

    protected boolean equal(double a, double b) {
        if (a == 0 || b == 0)
            return a == b;
        else
            return equal(a, b, fuzzFactor);
    }

    protected static boolean equal(double a, double b, double fuzzFactor) {
        if (Double.isNaN(a)) return Double.isNaN(b);
        if (Double.isInfinite(a)) return Double.isInfinite(b);
        return Math.abs(a - b) < fuzzFactor;
    }

}
