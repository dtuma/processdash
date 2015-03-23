// Copyright (C) 2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.reports.snippets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.compiler.CompiledScript;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ui.DataComboBox;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.StringUtils;

public class DataNameCollector extends TinyCGIBase {



    protected void writeContents() throws IOException {
        boolean includeNonNumeric = parameters.containsKey("all");
        boolean unqualified = parameters.containsKey("unqualified");
        boolean phasesOnly = parameters.containsKey("phases");

        Set metrics = new HashSet();
        run(getDataRepository(), getPrefix(), includeNonNumeric == false,
                unqualified, phasesOnly, metrics);

        for (Iterator i = metrics.iterator(); i.hasNext();) {
            out.write((String) i.next());
            out.write('\n');
        }
    }



    public static Collection run(DataRepository data, String prefix,
            boolean numbersOnly, boolean unqualified, boolean phasesOnly,
            Collection dest) {

        Collection metrics = new ArrayList();

        run(data, prefix, numbersOnly, true, metrics);

        if (phasesOnly)
            metrics = findPhaseMetrics(data, prefix, metrics);
        if (unqualified)
            metrics = removeQualifiers(metrics);

        dest.addAll(metrics);
        return dest;
    }

    private static Collection findPhaseMetrics(DataContext data,
            String prefix, Collection metrics) {
        List phases = getList(data, prefix, "Phase_List");
        String regex = "(\\Q" + StringUtils.join(phases, "\\E|\\Q")
                + "\\E)/(.*)";
        Pattern pat = Pattern.compile(regex);

        Set results = new HashSet();
        for (Iterator i = metrics.iterator(); i.hasNext();) {
            String dataName = (String) i.next();
            Matcher m = pat.matcher(dataName);
            if (m.matches())
                results.add(m.group(2));
        }
        return results;
    }

    private static List getList(DataContext data, String prefix, String name) {
        name = DataRepository.createDataName(prefix, name);
        SimpleData phaseDataElem = data.getSimpleValue(name);
        if (phaseDataElem instanceof ListData)
            return ((ListData) phaseDataElem).asList();
        else if (phaseDataElem instanceof StringData)
            return ((StringData) phaseDataElem).asList().asList();
        return Collections.EMPTY_LIST;
    }


    private static Collection removeQualifiers(Collection working) {
        Set results = new HashSet(working.size());
        for (Iterator i = working.iterator(); i.hasNext();) {
            String dataName = (String) i.next();
            results.add(REMOVE_QUALIFIERS.matcher(dataName).replaceAll(""));
        }
        return results;
    }

    public static final String[] PROBE_DATA = { "/Beta0", "/Beta1", "/Range",
            "/Interval Percent", "/R Squared", "/LPI", "/UPI" };

    private static final Pattern REMOVE_QUALIFIERS = Pattern
            .compile("(Estimated (?!.*("
                    + StringUtils.join(Arrays.asList(PROBE_DATA), "|")
                    + ")$)| To Date)");

    public static Collection run(DataRepository data, String prefix,
            boolean numbersOnly, boolean discardHidden, Collection dest) {
        Map dataElements = data.getDefaultDataElementsFor(prefix);
        if (dataElements == null)
            return dest;

        Pattern hiddenDataPattern = null;
        if (discardHidden)
            try {
                String hiddenRegexp = "(" + Settings.getVal(SETTING_NAME) + ")";
                hiddenDataPattern = Pattern.compile(hiddenRegexp,
                        Pattern.CASE_INSENSITIVE);
            } catch (Exception e) {
            }

        for (Iterator i = dataElements.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();

            String name = (String) e.getKey();

            if (numbersOnly) {
                Object val = e.getValue();
                if ((val instanceof DoubleData == false)
                        && (val instanceof CompiledScript == false))
                    continue;
                if (NONNUMERIC_DATA.contains(name))
                    continue;
            }

            if (hiddenDataPattern != null
                    && hiddenDataPattern.matcher(name).find())
                continue;

            dest.add(name);
        }

        return dest;
    }

    private static final String SETTING_NAME = DataComboBox.settingName;

    private static final Set NONNUMERIC_DATA = new HashSet(Arrays
            .asList(new String[] { "Started", "Completed" }));


}
