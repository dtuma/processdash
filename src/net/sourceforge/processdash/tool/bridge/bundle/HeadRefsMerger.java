// Copyright (C) 2021 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.bundle;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.util.PatternList;

/**
 * An implementation of {@link HeadRefs} that merges data from several
 * independent {@link HeadRefs} objects.
 * 
 * Each child {@link HeadRefs} object is registered with a pattern, and is only
 * called upon to store/retrieve refs for bundles whose name matches that
 * pattern.
 */
public class HeadRefsMerger implements HeadRefs {

    private List<PatternedRefs> patternedRefs;

    public HeadRefsMerger() {
        this.patternedRefs = new LinkedList<PatternedRefs>();
    }


    /**
     * Add a {@link HeadRefs} object that should be used to store refs for
     * bundles whose name matches a given pattern.
     * 
     * This method should be called multiple times to register different
     * {@link HeadRefs} that should be merged. The patterns provided should
     * describe distinct sets of bundles. It's almost certainly a mistake for a
     * bundle to match multiple patterns, but if one does, the <b>first</b>
     * {@link HeadRefs} registered via this method will take precedence.
     * 
     * @param bundleNamePattern
     *            a {@link PatternList} matching the names of bundles that
     *            should be stored
     * @param headRefs
     *            the {@link HeadRefs} object that should store refs for those
     *            bundles
     * @return this object
     */
    public HeadRefsMerger addPatternedRefs(PatternList bundleNamePattern,
            HeadRefs headRefs) {
        PatternedRefs pr = new PatternedRefs();
        pr.bundleNamePattern = bundleNamePattern;
        pr.headRefs = headRefs;
        patternedRefs.add(pr);
        return this;
    }


    /**
     * Add a {@link HeadRefs} object that should be used to store refs for
     * bundles whose name doesn't match any previously registered pattern.
     * 
     * This method should only be called after all desired calls to the
     * {@link #addPatternedRefs(PatternList, HeadRefs)} have been made. This
     * method will add a "match-all" pattern to the list, so it will shadow any
     * subsequent pattern registrations.
     * 
     * If this method is not called, this object will silently ignore requests
     * to store refs for bundles whose names don't match any registered
     * patterns.
     * 
     * @param defaultRefs
     *            a set of {@link HeadRefs} that will store/retrieve refs for
     *            bundles whose names do match any previously registered
     *            patterns.
     * @return this object
     */
    public HeadRefsMerger addDefaultRefs(HeadRefs defaultRefs) {
        return addPatternedRefs(new PatternList(".*"), defaultRefs);
    }


    @Override
    public FileBundleID getHeadRef(String bundleName) throws IOException {
        for (PatternedRefs pr : patternedRefs) {
            if (pr.matches(bundleName))
                return pr.headRefs.getHeadRef(bundleName);
        }
        return null;
    }


    @Override
    public Map<String, FileBundleID> getHeadRefs() throws IOException {
        // create an empty Map to hold our merged results
        Map<String, FileBundleID> result = new HashMap<String, FileBundleID>();

        // iterate over our HeadRefs in reverse order
        for (int i = patternedRefs.size(); i-- > 0;) {
            PatternedRefs pr = patternedRefs.get(i);

            // if the result we've built so far contains any bundles that match
            // this pattern, those entries are bogus. The logic in getHeadRef
            // would shadow those entries; so we remove them
            for (Iterator j = result.keySet().iterator(); j.hasNext();) {
                String bundleName = (String) j.next();
                if (pr.matches(bundleName))
                    j.remove();
            }

            // add all of the bundle refs from this collection; but only if
            // they match this collection's registered pattern
            for (FileBundleID bundleID : pr.headRefs.getHeadRefs().values()) {
                if (pr.matches(bundleID))
                    result.put(bundleID.getBundleName(), bundleID);
            }
        }

        // return the merged list of results we've built
        return result;
    }


    @Override
    public void storeHeadRef(FileBundleID bundleID) throws IOException {
        for (PatternedRefs pr : patternedRefs) {
            if (pr.matches(bundleID)) {
                pr.headRefs.storeHeadRef(bundleID);
                break;
            }
        }
    }


    @Override
    public void storeHeadRefs(Collection<FileBundleID> headRefs)
            throws IOException {
        // make a copy of the refs we need to set, so we can modify it without
        // affecting our caller
        List<FileBundleID> refsToSet = new LinkedList<FileBundleID>(headRefs);

        // iterate over each of our patterned ref collections
        for (PatternedRefs pr : patternedRefs) {

            // find incoming refs that match the pattern for this collection.
            // remove those matches from "refsToSet" and place in separate list
            List<FileBundleID> matchingRefs = pr.extractMatches(refsToSet);

            // if any were found, ask this head collection to store them
            if (matchingRefs != null) {
                pr.headRefs.storeHeadRefs(matchingRefs);
            }
        }
    }



    @Override
    public void deleteHeadRef(String bundleName) throws IOException {
        // iterate over each of our patterned ref collections
        for (PatternedRefs pr : patternedRefs) {

            // if this head collection claims the named bundle, ask it to
            // delete the ref
            if (pr.matches(bundleName))
                pr.headRefs.deleteHeadRef(bundleName);
        }
    }



    private class PatternedRefs {

        private PatternList bundleNamePattern;

        private HeadRefs headRefs;

        private boolean matches(FileBundleID bundleID) {
            return matches(bundleID.getBundleName());
        }

        private boolean matches(String bundleName) {
            return bundleNamePattern.matches(bundleName);
        }

        private List<FileBundleID> extractMatches(List<FileBundleID> refs) {
            if (refs == null || refs.isEmpty())
                return null;

            List<FileBundleID> matchingRefs = null;
            for (Iterator i = refs.iterator(); i.hasNext();) {
                FileBundleID oneRef = (FileBundleID) i.next();
                if (matches(oneRef)) {
                    if (matchingRefs == null)
                        matchingRefs = new LinkedList<FileBundleID>();
                    matchingRefs.add(oneRef);
                    i.remove();
                }
            }

            return matchingRefs;
        }

    }

}
