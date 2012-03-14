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

import static net.sourceforge.processdash.util.NullSafeObjectUtils.EQ;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.sourceforge.processdash.util.PatternList;

import teamdash.merge.MergeWarning.Severity;


public class MapContentMerger<ID> implements ContentMerger<ID, Map> {

    private AttributeMerger<ID, Object> defaultHandler;

    private Map<PatternList, AttributeMerger<ID, Object>> handlers;

    public MapContentMerger() {
        handlers = new LinkedHashMap();
    }

    public void setDefaultHandler(AttributeMerger handler) {
        defaultHandler = handler;
    }

    public void addHandler(String attrName, AttributeMerger handler) {
        addHandler(new PatternList().addRegexp("^" + attrName + "$"), handler);
    }

    public void addHandler(PatternList pat, AttributeMerger handler) {
        if (pat == null)
            throw new NullPointerException();
        handlers.put(pat, handler);
    }

    public boolean isEqual(Map a, Map b) {
        return EQ(a, b);
    }

    public Map mergeContent(TreeNode<ID, Map> destNode, Map base, Map main,
            Map incoming, ContentMerger.ErrorReporter<ID> err) {

        // short-circuit: if one of the branches has a null map, return the map
        // from the other branch.
        if (main == null)
            return incoming;
        else if (incoming == null)
            return main;

        // if we have no base map, treat it as empty.
        if (base == null)
            base = Collections.EMPTY_MAP;

        // get a list of the keys used in all of the maps
        Set allKeys = new HashSet();
        allKeys.addAll(base.keySet());
        allKeys.addAll(main.keySet());
        allKeys.addAll(incoming.keySet());

        // iterate over the keys, and create the merged result
        Map result = createMergedMap();
        for (Object key : allKeys) {
            if (key instanceof String) {
                String attrName = (String) key;
                Object baseValue = base.get(attrName);
                Object mainValue = main.get(attrName);
                Object incomingValue = incoming.get(attrName);
                Object mergedValue = mergeAttribute(destNode.getID(), attrName,
                    baseValue, mainValue, incomingValue, err);
                result.put(attrName, mergedValue);
            }
        }

        return result;
    }

    protected HashMap createMergedMap() {
        return new HashMap();
    }

    protected Object mergeAttribute(ID nodeID, String attrName, Object base,
            Object main, Object incoming, ErrorReporter<ID> err) {

        // If the value agrees in both branches, return it.
        if (EQ(main, incoming))
            return main;

        // if the incoming branch didn't change the value, keep the main value
        if (EQ(base, incoming))
            return main;

        // if the main branch didn't change the value, keep the incoming value
        if (EQ(base, main))
            return incoming;

        // Get an appropriate handler, and ask it to merge this conflicting
        // attribute.
        AttributeMerger<ID, Object> handler = getHandler(attrName);
        Object merged = handler.mergeAttribute(nodeID, attrName, base, main,
            incoming, err);
        return merged;
    }

    protected AttributeMerger<ID, Object> getHandler(String key) {
        for (Entry<PatternList, AttributeMerger<ID, Object>> e : handlers
                .entrySet()) {
            if (e.getKey().matches(String.valueOf(key)))
                return e.getValue();
        }
        if (defaultHandler != null)
            return defaultHandler;
        else
            return GLOBAL_DEFAULT_HANDLER;
    }

    private static DefaultAttributeMerger GLOBAL_DEFAULT_HANDLER =
        new DefaultAttributeMerger(Severity.CONFLICT);

}
