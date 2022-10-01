// Copyright (C) 2022 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.merge;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class SetMerger<T> extends MapMerger<T, Boolean> {

    public static final SetMerger INSTANCE = new SetMerger();

    public Set<T> mergeSets(Collection<T> parent, Collection<T> first,
            Collection<T> second) {
        Map parentMap = makeMapForSet(parent);
        Map firstMap = makeMapForSet(first);
        Map secondMap = makeMapForSet(second);

        LinkedHashMap<T, Boolean> mergedMap = new LinkedHashMap<T, Boolean>();
        mergeMaps(parentMap, firstMap, secondMap, mergedMap);

        return makeSetFromMap(mergedMap);
    }

    private Map<T, Boolean> makeMapForSet(Collection<T> source) {
        Map<T, Boolean> result = new LinkedHashMap<T, Boolean>();
        for (T item : source) {
            result.put(item, Boolean.TRUE);
        }
        return result;
    }

    @Override
    protected Boolean mergeConflictingChange(T key, Boolean parent,
            Boolean first, Boolean second) {
        // this method will never be called. Our only possible values are true
        // and null, so the three participants in the 3-way merge can't have
        // three different values. Provide a simple implementation to keep the
        // compiler happy
        return second;
    }

    private Set<T> makeSetFromMap(LinkedHashMap<T, Boolean> mergedMap) {
        Set<T> result = new LinkedHashSet<T>();
        for (Entry<T, Boolean> e : mergedMap.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue()))
                result.add(e.getKey());
        }
        return result;
    }

}
