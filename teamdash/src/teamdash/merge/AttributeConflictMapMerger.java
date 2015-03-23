// Copyright (C) 2012 Tuma Solutions, LLC
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

package teamdash.merge;

import java.util.Map;

public interface AttributeConflictMapMerger<ID, Type> extends
        AttributeMerger<ID, Type> {

    /**
     * If this merger was called upon to resolve an attribute conflict within
     * the map for a particular node, this method will be called after all
     * attribute merging has been completed. In this method, this merger can
     * perform any necessary cleanup or processing on the maps themselves.
     */
    public void mergeMapAfterAttrConflict(ID destNodeID,
            String conflictAttrName, Map base, Map main, Map incoming,
            Map merged, ContentMerger.ErrorReporter<ID> err);

}
