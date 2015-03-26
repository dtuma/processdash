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

package net.sourceforge.processdash.log.time;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.util.EnumerIterator;
import net.sourceforge.processdash.util.IteratorConcatenator;
import net.sourceforge.processdash.util.StringMapper;

/**
 * A time log which consolidates entries from several sources, optionally
 * rewriting their paths.
 */
public abstract class RolledUpTimeLog implements TimeLog {

    /** Constant to indicate that paths should not be altered */
    public static final int REMAP_NONE = 0;

    /**
     * Constant to indicate that source prefixes should be replaced with the
     * base prefix supplied for the rolled up time log
     */
    public static final int REMAP_MERGE = 1;



    protected DashboardContext context;

    protected String basePrefix;



    /**
     * Use a Result Set to determine the list of time log prefixes that should
     * be rolled up. The result set will be reevaluated each time the time log's
     * filter() method is called, allowing for semi-dynamic behavior.
     */
    public static class FromResultSet extends RolledUpTimeLog {

        public static final String MERGE_PREFIXES_PARAM = "mergePrefixes";

        Map parameters;

        public FromResultSet(DashboardContext context, String prefix, Map params) {
            super(context, prefix);
            this.parameters = new HashMap(params);
        }

        protected String[] getPrefixes() {
            return ResultSet.getPrefixList(context.getData(), parameters,
                    basePrefix);
        }

        protected int getPathRemappingStyle() {
            if (parameters.containsKey(MERGE_PREFIXES_PARAM))
                return REMAP_MERGE;
            else
                return REMAP_NONE;
        }

    }



    /**
     * Roll up time log data from a fixed set of prefixes.
     */
    public static class FromPrefixList extends RolledUpTimeLog {

        String[] prefixList;

        private int mappingStyle;

        public FromPrefixList(DashboardContext context, String prefix,
                String[] prefixList, int mappingStyle) {
            super(context, prefix);
            this.prefixList = prefixList;
            this.mappingStyle = mappingStyle;
        }

        protected String[] getPrefixes() {
            return prefixList;
        }

        protected int getPathRemappingStyle() {
            return mappingStyle;
        }
    }



    protected RolledUpTimeLog(DashboardContext context, String basePrefix) {
        this.context = context;
        this.basePrefix = basePrefix;
    }

    protected abstract String[] getPrefixes();

    protected abstract int getPathRemappingStyle();

    protected StringMapper getPathRemapper(String[] prefixes) {
        switch (getPathRemappingStyle()) {
        case REMAP_MERGE:
            return new ReplacePrefixes(prefixes, basePrefix);

        case REMAP_NONE:
        default:
            return null;
        }
    }

    public EnumerIterator filter(String path, Date from, Date to)
            throws IOException {
        List entries = new ArrayList();
        String[] prefixes = getPrefixes();
        StringMapper pathRemapper = getPathRemapper(prefixes);

        for (int i = 0; i < prefixes.length; i++) {
            String onePrefix = prefixes[i];

            // find any imported entries for the given prefix
            Iterator importedEntries = ImportedTimeLogManager.getInstance()
                    .getImportedTimeLogEntries(onePrefix);
            maybeAddEntries(entries, importedEntries, pathRemapper);

            // find any regular entries for the given prefix
            Iterator regularEntries = context.getTimeLog().filter(onePrefix,
                    from, to);
            maybeAddEntries(entries, regularEntries, pathRemapper);
        }

        EnumerIterator result = new IteratorConcatenator(entries);
        result = new TimeLogIteratorFilter(result, path, from, to);

        return result;
    }

    private void maybeAddEntries(List entries, Iterator i,
            StringMapper pathRemapper) {
        if (i != null && i.hasNext()) {
            if (pathRemapper != null)
                i = new TimeLogEntryVOPathFilter(i, pathRemapper);
            entries.add(i);
        }
    }


    /**
     * Simple StringMapper that replaces multiple prefixes with a target prefix.
     */
    private static class ReplacePrefixes implements StringMapper {

        String[] srcPrefixes;

        String destPrefix;

        public ReplacePrefixes(String[] prefixesToReplace, String targetPrefix) {
            this.srcPrefixes = (String[]) prefixesToReplace.clone();
            Arrays.sort(this.srcPrefixes);
            this.destPrefix = targetPrefix;
        }

        public String getString(String path) {
            for (int i = srcPrefixes.length; i-- > 0;) {
                String prefix = srcPrefixes[i];
                if (Filter.pathMatches(path, prefix))
                    return destPrefix + path.substring(prefix.length());
            }
            return path;
        }

    }

}
