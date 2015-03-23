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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import teamdash.merge.ContentMerger.ErrorReporter;
import teamdash.merge.MergeWarning.Severity;

public class DependentAttributeMerger<ID, Type> extends
        DefaultAttributeMerger<ID, Type> implements
        AttributeConflictMapMerger<ID, Type> {

    private Map<String, List<String>> dependentAttrs;

    /**
     * When attribute conflicts occur, choose the value from main and log a
     * merge warning with the given severity.
     */
    public DependentAttributeMerger(Severity severity) {
        super(severity);
        dependentAttrs = new HashMap();
    }

    /**
     * When a conflict occurs on the named attribute, arrange for all of the
     * named dependent attributes to be chosen from the main map too.
     */
    public DependentAttributeMerger<ID, Type> setDependentAttrs(
            String conflictAttrName, String... dependentAttrNames) {
        this.dependentAttrs.put(conflictAttrName,
            Arrays.asList(dependentAttrNames));
        return this;
    }

    public void mergeMapAfterAttrConflict(ID destNodeID,
            String conflictAttrName, Map base, Map main, Map incoming,
            Map merged, ErrorReporter<ID> err) {
        List<String> attrs = dependentAttrs.get(conflictAttrName);
        if (attrs != null)
            for (String depAttr : attrs)
                merged.put(depAttr, main.get(depAttr));
    }

}
