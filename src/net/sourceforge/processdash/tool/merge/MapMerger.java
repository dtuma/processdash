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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.sourceforge.processdash.util.NullSafeObjectUtils;

public abstract class MapMerger<K, V> {

    protected void mergeMaps(Map parent, Map first, Map second, Map dest) {
        // gather up all the known keys
        Set<K> keys = new LinkedHashSet<K>();
        keys.addAll(parent.keySet());
        keys.addAll(first.keySet());
        keys.addAll(second.keySet());

        // iterate over known keys, merging each value
        for (K oneKey : keys) {
            V parentVal = (V) parent.get(oneKey);
            V firstVal = (V) first.get(oneKey);
            V secondVal = (V) second.get(oneKey);
            V mergedVal = mergeValue(oneKey, parentVal, firstVal, secondVal);
            if (mergedVal != null)
                dest.put(oneKey, mergedVal);
        }
    }

    protected V mergeValue(K key, V parent, V first, V second) {
        // If the value agrees in both branches, return it.
        if (eq(first, second))
            return second;

        // if the first branch didn't change the value, keep the second value
        if (eq(parent, first))
            return second;

        // if the second branch didn't change the value, keep the first value
        if (eq(parent, second))
            return first;

        // the value has been edited in both branches. Request conflict
        // resolution
        return mergeConflictingChange(key, parent, first, second);
    }

    protected boolean eq(V a, V b) {
        return NullSafeObjectUtils.EQ(a, b);
    }

    protected abstract V mergeConflictingChange(K key, V parent, V first,
            V second);

}
