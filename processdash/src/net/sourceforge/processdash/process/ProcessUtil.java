// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.process;

import java.util.LinkedList;
import java.util.List;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.util.FormatUtil;


public class ProcessUtil {

    static Resources resources = Resources.getGlobalBundle();

    DataRepository data;
    String prefix;

    public ProcessUtil(DataRepository data, String prefix) {
        this.data = data;
        this.prefix = prefix;
    }

    private String getPrefix() {
        return prefix;
    }
    private DataRepository getDataRepository() {
        return data;
    }

    public ListData getProcessList(String listName) {
        String dataName = DataRepository.createDataName(getPrefix(), listName);
        SimpleData val = getDataRepository().getSimpleValue(dataName);
        if (val instanceof ListData)
            return (ListData) val;
        else if (val instanceof StringData)
            return ((StringData) val).asList();
        else
            return new ListData();
    }

    public List getProcessListPlain(String name) {
        ListData list = getProcessList(name);
        List result = new LinkedList();
        for (int i = 0;  i < list.size();   i++)
            result.add(list.get(i));
        return result;
    }

    public String getProcessString(String stringName) {
        String dataName = DataRepository.createDataName(getPrefix(), stringName);
        SimpleData val = getDataRepository().getSimpleValue(dataName);
        return val == null ? "" : val.format();
    }

    public String getSizeMetric() {
        return getProcessString("SIZE_METRIC_NAME");
    }

    public String getAggrSizeMetric() {
        return getProcessString("SIZE_METRIC_NAME");
    }


    public String getSizeAbbr() {
        return getProcessString("SIZE_METRIC_NAME_ABBR");
    }

    public String getSizeAbbrLabel() {
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
            result = getProcessString("Process_ID");
        return result;
    }

    public String getProcessID() {
        return getProcessString("Process_ID");
    }

}
