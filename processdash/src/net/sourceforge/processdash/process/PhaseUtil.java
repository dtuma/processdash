// Copyright (C) 2007 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.process;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PhaseUtil {

    public static boolean isAppraisalPhaseType(String phaseType) {
        return appraisalPhaseTypes.contains(phaseType);
    }

    public static boolean isFailurePhaseType(String phaseType) {
        return failurePhaseTypes.contains(phaseType);
    }

    public static boolean isDevelopmentPhaseType(String phaseType) {
        return developmentPhaseTypes.contains(phaseType);
    }

    public static boolean isOverheadPhaseType(String phaseType) {
        return overheadPhaseTypes.contains(phaseType);
    }

    static PhaseTypeSet appraisalPhaseTypes = new PhaseTypeSet(new String[] {
            "appraisal", "review", "insp", "reqinsp", "hldr", "hldrinsp",
            "dldr", "dldinsp", "cr", "codeinsp" });

    static PhaseTypeSet failurePhaseTypes = new PhaseTypeSet(new String[] {
            "failure", "comp", "ut", "it", "st", "at", "pl" });

    static PhaseTypeSet overheadPhaseTypes = new PhaseTypeSet(new String[] {
            "overhead", "mgmt", "strat", "plan", "pm" });

    static PhaseTypeSet developmentPhaseTypes = new PhaseTypeSet(new String[] {
            "develop", "req", "stp", "itp", "td", "hld", "dld", "code", "doc" });

    /**
     * A case-insensitive set containing phase types.
     */
    static class PhaseTypeSet {

        public final String[] phaseTypes;

        public final Set phaseTypeSet;

        /**
         * Construct a PhaseTypeSet from the list of types in the given array.
         * The array must contain strings which are all lower case.
         */
        public PhaseTypeSet(String[] phaseTypes) {
            this.phaseTypes = phaseTypes;
            this.phaseTypeSet = Collections.unmodifiableSet(new HashSet(Arrays
                    .asList(phaseTypes)));
        }

        /**
         * Returns true if a given phaseType is contained in this PhaseTypeSet.
         */
        public boolean contains(String phaseType) {
            return phaseTypeSet.contains(phaseType.toLowerCase());
        }
    }
}
