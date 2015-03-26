// Copyright (C) 2003-2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.process;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.StringUtils;


public class ProcessUtil {

    public static final String DEFAULT_SIZE_UNITS = "LOC";

    static Resources resources = Resources.getGlobalBundle();

    private DataContext data;
    private String processID = null;

    public ProcessUtil(DataRepository data, String prefix) {
        this(data.getSubcontext(prefix));
    }

    public ProcessUtil(DataContext data) {
        this.data = data;
    }

    private SimpleData getValue(String dataName) {
        return data.getSimpleValue(dataName);
    }

    private String getGlobalName(String dataName) {
        return "/" + getProcessID() + "/" + dataName;
    }

    public ListData getProcessList(String listName) {
        SimpleData val = getValue(listName);
        if (val == null)
            val = getValue(getGlobalName(listName));

        if (val instanceof ListData)
            return (ListData) val;
        else if (val instanceof StringData)
            return ((StringData) val).asList();
        else
            return ListData.EMPTY_LIST;
    }

    public List getProcessListPlain(String name) {
        return new ArrayList(getProcessList(name).asList());
    }

    public List filterPhaseList(List phases) {
        List filter = getProcessListPlain("Phase_Display_Filter_List");
        if (filter.isEmpty())
            return phases;

        List result = new ArrayList(phases);
        result.retainAll(filter);
        return result;
    }

    public String getProcessString(String stringName) {
        return getProcessString(stringName, true);
    }

    private String getProcessString(String stringName, boolean tryGlobal) {
        SimpleData val = getValue(stringName);
        if (val == null && tryGlobal)
            val = getValue(getGlobalName(stringName));

        return val == null ? "" : val.format();
    }

    public String getSizeUnits() {
        String result = getProcessString("Size Units", false).trim();
        if (result.length() > 0)
            return result;
        else
            return DEFAULT_SIZE_UNITS;
    }

    public String getSizeMetric() {
        return getProcessString("SIZE_METRIC_NAME");
    }

    public String getAggrSizeMetric() {
        return getProcessString("AGGR_SIZE_METRIC_NAME");
    }


    public String getSizeAbbr() {
        return getProcessString("SIZE_METRIC_NAME_ABBR");
    }

    public String getSizeAbbrLabel() {
        String result = getProcessString("SIZE_METRIC_LABEL");
        if (StringUtils.hasValue(result))
            return result;
        else
            return Translator.translate(getSizeAbbr());
    }

    public String getAggrSizeLabel() {
        return Translator.translate
            (getProcessString("AGGR_SIZE_METRIC_NAME_ABBR"));
    }

    public String getProductivityLabel() {
        return resources.format("Productivity_Units_FMT", getSizeAbbrLabel());
    }

    public String formatProductivity(double prod) {
        return resources.format
            ("Productivity_FMT", getSizeAbbrLabel(),
                FormatUtil.formatNumber(prod));
    }

    public String getDefectDensityLabel() {
        return resources.format
            ("Defect_Density_Units_FMT", getAggrSizeLabel());
    }

    public String formatDefectDensity(double density) {
        return resources.format
            ("Defect_Density_FMT", new Double(density), getAggrSizeLabel());
    }

    public String getRollupID() {
        String result = getProcessString("Use_Rollup");
        if (result == null)
            result = getProcessString("Process_ID", false);
        return result;
    }

    public String getProcessID() {
        if (processID == null)
            processID = getProcessString("Process_ID", false);
        return processID;
    }

    public String getEffectivePhase(String path, boolean guessFromPath) {
        String dataName = path + "/Effective_Phase";
        String result = getProcessString(dataName, false);
        if (result != null && result.length() > 0)
            return result;

        if (!guessFromPath)
            return null;

        int slashPos = path.lastIndexOf('/');
        return path.substring(slashPos + 1);
    }
}
