// Copyright (C) 2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This class provides a mapping from a sparse set of unique IDs to another,
 * more compact set of unique IDs.
 * 
 * The input numbers are assumed to be sparsely scattered over the entire range
 * of Long values. However, this class will optimize for the following
 * situations:
 * <ul>
 * <li>The input numbers often contain clusters of near-sequential numbers. To
 * take advantage of this optimization, clients should make an initial call to
 * {@link #preassign(Collection)} with all the initially known input numbers.</li>
 * <li>Once in use, new input numbers will often arrive sequentially.</li>
 * </ul>
 * 
 * Output numbers will be generated as positive integers, starting at 1. The
 * {@link #maxGap} property can be used to control the acceptable size of gaps
 * that appear in the output number sequence.
 */
public class UidRemapper {

    private int maxGap;

    private List<Mapping> mappings;

    private Mapping growingEdge;

    private Set<Long> idsToPreassign;

    private boolean hasChanged;


    public UidRemapper() {
        maxGap = 200;
        mappings = new ArrayList<Mapping>();
        growingEdge = null;
        idsToPreassign = null;
        hasChanged = false;
    }

    public UidRemapper(String savedString) {
        this();
        if (savedString != null && savedString.length() > 0) {
            String[] mapSpecs = savedString.split(";");
            for (int i = 0; i < mapSpecs.length - 1; i++)
                mappings.add(new Mapping(mapSpecs[i]));
            growingEdge = new Mapping(mapSpecs[mapSpecs.length - 1]);
        }
        hasChanged = false;
    }

    public int getMaxGap() {
        return maxGap;
    }

    public void setMaxGap(int maxGap) {
        this.maxGap = maxGap;
    }

    public boolean hasChanged() {
        return hasChanged;
    }


    public void preassign(long input) {
        if (idsToPreassign == null)
            idsToPreassign = new HashSet<Long>();
        idsToPreassign.add(input);
    }

    public int remap(long input) {
        performPreassignments();

        Integer result = findInGrowingEdge(input);
        if (result != null)
            return result;

        result = findInExistingMappings(input);
        if (result != null)
            return result;

        return makeNewGrowingEdge(input);
    }

    private void performPreassignments() {
        if (idsToPreassign == null)
            return;

        for (Iterator<Long> i = idsToPreassign.iterator(); i.hasNext();) {
            long oneID = i.next();
            if (findInExistingMappings(oneID) != null)
                i.remove();
        }
        ArrayList<Long> ids = new ArrayList<Long>(idsToPreassign);
        Collections.sort(ids);
        for (long oneID : ids)
            if (findInGrowingEdge(oneID) == null)
                makeNewGrowingEdge(oneID);
        idsToPreassign = null;
    }

    private Integer findInGrowingEdge(long input) {
        return (growingEdge == null ? null : growingEdge.remap(input, maxGap));
    }

    private Integer findInExistingMappings(long input) {
        for (Mapping m : mappings) {
            Integer result = m.remap(input, 0);
            if (result != null)
                return result;
        }
        return null;
    }

    private int makeNewGrowingEdge(long input) {
        int nextOutput;
        if (growingEdge == null)
            nextOutput = 1;
        else
            nextOutput = growingEdge.getLastOutputNumber() + 1;

        mappings.add(growingEdge);
        growingEdge = new Mapping(input, nextOutput);
        hasChanged = true;
        return nextOutput;
    }

    public String saveAsString() {
        StringBuilder result = new StringBuilder();
        for (Mapping m : mappings) {
            m.saveAsString(result);
            result.append(";");
        }
        if (growingEdge != null)
            growingEdge.saveAsString(result);
        return super.toString();
    }


    private class Mapping {
        long firstInputNumber;

        long lastInputNumber;

        int firstOutputNumber;

        public Mapping(long input, int output) {
            firstInputNumber = lastInputNumber = input;
            firstOutputNumber = output;
        }

        public Mapping(String s) {
            int tildePos = s.indexOf('~');
            int colonPos = s.indexOf(':');
            if (tildePos == -1) {
                firstInputNumber = lastInputNumber = Long.parseLong(s
                        .substring(0, colonPos));
            } else {
                firstInputNumber = Long.parseLong(s.substring(0, tildePos));
                lastInputNumber = Long.parseLong(s.substring(tildePos + 1,
                    colonPos));
            }
            firstOutputNumber = Integer.parseInt(s.substring(colonPos + 1));
        }

        public Integer remap(long input, int allowableGrowth) {
            // check to see if the input number is out of bounds below
            if (input < firstInputNumber)
                return null;

            // check to see if the input number is out of bounds above
            if (input > lastInputNumber + allowableGrowth)
                return null;

            // the number is within bounds. Compute the remapped value, and
            // possibly update our lastInputNumber.
            if (input > lastInputNumber) {
                lastInputNumber = input;
                hasChanged = true;
            }
            return (int) (input - firstInputNumber + firstOutputNumber);
        }

        public int getLastOutputNumber() {
            return remap(lastInputNumber, 0);
        }

        public void saveAsString(StringBuilder result) {
            result.append(Long.toString(firstInputNumber));
            if (firstInputNumber != lastInputNumber)
                result.append('~').append(Long.toString(lastInputNumber));
            result.append(':').append(Integer.toString(firstOutputNumber));
        }

    }

}
