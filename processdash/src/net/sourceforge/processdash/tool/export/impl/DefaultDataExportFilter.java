// Copyright (C) 2005-2013 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.impl;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.tool.export.mgr.ExportManager;
import net.sourceforge.processdash.util.DateUtils;
import net.sourceforge.processdash.util.IteratorFilter;
import net.sourceforge.processdash.util.PatternList;

public class DefaultDataExportFilter extends IteratorFilter {

    private static Logger logger = Logger.getLogger(
            DefaultDataExportFilter.class.getName());

    private boolean skipToDateData = true;

    private boolean skipZero = true;

    private boolean skipInfNaN = true;

    private boolean skipNodesAndLeaves = true;

    private boolean skipProcessAutoData = true;

    private List includes = null;

    private List excludes = null;

    private PatternList includePatterns = null;

    private PatternList excludePatterns = null;

    private Date maxDate = null;

    public DefaultDataExportFilter(Iterator dataElements) {
        super(dataElements);
        // don't call init yet, though. Let our kids call it.
    }

    public boolean isSkipNodesAndLeaves() {
        return skipNodesAndLeaves;
    }

    public void setSkipNodesAndLeaves(boolean skipNodesAndLeaves) {
        this.skipNodesAndLeaves = skipNodesAndLeaves;
    }

    public boolean isSkipProcessAutoData() {
        return skipProcessAutoData;
    }

    public void setSkipProcessAutoData(boolean skipProcessAutoData) {
        this.skipProcessAutoData = skipProcessAutoData;
    }

    public boolean isSkipToDateData() {
        return skipToDateData;
    }

    public void setSkipToDateData(boolean skipToDateData) {
        this.skipToDateData = skipToDateData;
    }

    public boolean isSkipZero() {
        return skipZero;
    }

    public void setSkipZero(boolean skipZeroInfNaN) {
        this.skipZero = skipZeroInfNaN;
    }

    public boolean isSkipInfNaN() {
        return skipInfNaN;
    }

    public void setSkipInfNaN(boolean skipInfNaN) {
        // disabled for now. Infinity and NaN get unusual treatment by the
        // number formatter we're using.
        // this.skipInfNaN = skipInfNaN;
        this.skipInfNaN = false;
    }

    public List getIncludes() {
        return includes;
    }

    public void setIncludes(List includes) {
        this.includes = includes;
    }

    public List getExcludes() {
        return excludes;
    }

    public void setExcludes(List excludes) {
        this.excludes = excludes;
    }

    public Date getMaxDate() {
        return maxDate;
    }

    public void init() {
        includePatterns = setupPatterns(includes);
        excludePatterns = setupPatterns(excludes);
        super.init();
    }

    protected boolean includeInResults(Object o) {
        ExportedDataValue v = (ExportedDataValue) o;
        processMaxDate(v);
        if (isExcluded(v) || isNotIncluded(v) || isExportInstruction(v)
                || isSkippableToDateData(v) || isSkippableNodeLeaf(v)
                || isSkippableProcessAutoData(v) || isSkippableDoubleData(v))
            return false;
        else
            return true;
    }

    private void processMaxDate(ExportedDataValue v) {
        String name = v.getName();
        if (name.endsWith("/Started") || name.endsWith("/Completed")) {
            SimpleData value = v.getSimpleValue();
            if (value instanceof DateData) {
                Date thisDate = ((DateData) value).getValue();
                maxDate = DateUtils.maxDate(maxDate, thisDate);
            }
        }
    }

    private boolean isExcluded(ExportedDataValue v) {
        return (excludePatterns != null
                && excludePatterns.matches(v.getName()));
    }

    private boolean isNotIncluded(ExportedDataValue v) {
        return (includePatterns != null
                && !includePatterns.matches(v.getName()));
    }

    private boolean isExportInstruction(ExportedDataValue v) {
        return (v.getName().indexOf(ExportManager.EXPORT_DATANAME) != -1);
    }

    private boolean isSkippableToDateData(ExportedDataValue v) {
        return skipToDateData && v.getName().endsWith(" To Date");
    }

    private boolean isSkippableNodeLeaf(ExportedDataValue v) {
        return skipNodesAndLeaves && (v.getName().endsWith("/node") //
                || v.getName().endsWith("/leaf"));
    }

    private boolean isSkippableProcessAutoData(ExportedDataValue v) {
        if (skipProcessAutoData) {
            Matcher m = PROCESS_AUTO_DATA_PATTERN.matcher(v.getName());
            return m.find();
        } else
            return false;
    }

    private static Pattern PROCESS_AUTO_DATA_PATTERN;

    private static final String[] IGNORABLE_AUTO_DATA = { "_METRIC_NAME",
            "Last_Failure_Phase", "Use_Rollup", "FILES_XML", "Child_List",
            "Phase_List", "PROBE_NO_EDIT_INPUT", "_Rollup_List", "Normalized_",
            "Prototypical PSP Data" };
    static {
        StringBuffer pat = new StringBuffer();
        for (int i = 0; i < IGNORABLE_AUTO_DATA.length; i++)
            pat.append("|\\Q").append(IGNORABLE_AUTO_DATA[i]).append("\\E");
        PROCESS_AUTO_DATA_PATTERN = Pattern.compile(pat.substring(1));
    }

    private boolean isSkippableDoubleData(ExportedDataValue v) {
        if (skipZero || skipInfNaN) {
            SimpleData value = v.getSimpleValue();
            if (value instanceof DoubleData) {
                double d = ((DoubleData) value).getDouble();
                return ((skipZero && d == 0) || (skipInfNaN && (Double.isNaN(d) || Double
                        .isInfinite(d))));
            }
        }
        return false;
    }

    private PatternList setupPatterns(List regexps) {
        if (regexps == null || regexps.isEmpty())
            return null;
        else {
            PatternList result = new PatternList();
            for (Iterator iter = regexps.iterator(); iter.hasNext();) {
                String re = (String) iter.next();
                int bracePos = re.indexOf('}');
                if (bracePos != -1 && re.startsWith("{"))
                    re = re.substring(bracePos + 1);
                try {
                    result.addRegexp(re);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Bad regular expression ''{0}''",
                            re);
                }
            }
            return result;
        }
    }
}
