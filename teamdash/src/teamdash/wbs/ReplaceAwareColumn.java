// Copyright (C) 2017 Tuma Solutions, LLC
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

package teamdash.wbs;

public interface ReplaceAwareColumn extends DataColumn {

    public Object REPLACEMENT_REJECTED = new Object();

    /**
     * Calculate the value to use for a find/replace operation in this column
     * 
     * @param find
     *            a string to search for
     * @param replace
     *            the replacement value to use
     * @param node
     *            the node where the replacement should occur
     * @return an appropriate replacement value that can be used in a subsequent
     *         {@link #setValueAt(Object, WBSNode)} call. If the replacement was
     *         rejected because it would result in an illegal value, returns
     *         {@link #REPLACEMENT_REJECTED}.
     */
    public Object getReplacementValueAt(String find, String replace,
            WBSNode node);

}
