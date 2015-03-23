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

package teamdash.wbs.columns;

import teamdash.wbs.WBSNode;

/** Interface used by {@link TopDownBottomUpColumn} to identify nodes
 * which should be excluded from bottom-up calculations.
 */
public interface Pruner {

    /** Returns true if the given node should not participate in the
     * bottom-up calculation. Nodes that are pruned from the calculation (and
     * all the children of pruned nodes) will have neither their "top down"
     * nor their "bottom up" attribute set.  Instead, they will have their
     * "inherited" attribute set to the value of their nearest ancestor.
     */
    public boolean shouldPrune(WBSNode node);

}
