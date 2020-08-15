// Copyright (C) 2002-2020 Tuma Solutions, LLC
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

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.PatternList;

import teamdash.merge.ui.MergeConflictNotification;
import teamdash.wbs.ConflictCapableDataColumn;
import teamdash.wbs.IndexAwareDataColumn;
import teamdash.wbs.WBSNode;

/** Abstract implementation of DataColumn interface.
 */
public abstract class AbstractDataColumn implements IndexAwareDataColumn,
        ConflictCapableDataColumn {

    public static final Resources resources = Resources
            .getDashBundle("WBSEditor.Columns");

    /** The value of this field will be returned as the column ID */
    protected String columnID;

    /** The value of this field will be returned as the column name */
    protected String columnName;

    /** The index of this column in the data model */
    private int dataColumnIndex = -1;

    /** For {@link teamdash.wbs.CalculatedDataColumn CalculatedDataColumn}
     * objects, the value of this field will be returned as the list
     * of dependent columns */
    protected String[] dependentColumns = null;

    /** For {@link teamdash.wbs.CalculatedDataColumn CalculatedDataColumn}
     * objects, the value of this field will be returned as the list
     * of affected columns */
    protected String[] affectedColumns = null;

    /** The value of this field will be returned as the preferred
     * column width */
    protected int preferredWidth = -1;

    protected PatternList conflictAttributeNamePattern = null;

    public String getColumnID()   { return columnID;     }
    public String getColumnName() { return columnName;   }
    public Class getColumnClass() { return String.class; }
    public int getColumnIndex()   { return dataColumnIndex; }
    public void setColumnIndex(int index) { dataColumnIndex = index; }
    public String[] getDependentColumnIDs() { return dependentColumns; }
    public String[] getAffectedColumnIDs() { return affectedColumns; }
    public void resetDependentColumns() {}
    public int getPreferredWidth() { return preferredWidth; }
    public PatternList getConflictAttributeNamePattern() {
        return conflictAttributeNamePattern;
    }
    protected void setConflictAttributeName(String name) {
        conflictAttributeNamePattern = new PatternList().addLiteralEquals(name);
    }
    public Object getConflictDisplayValue(String value, WBSNode node) {
        return getValueAt(node);
    }
    public void adjustConflictNotification(MergeConflictNotification mcn) {}
    public void storeConflictResolutionValue(Object value, WBSNode node) {
        setValueAt(value, node);
    }

}
