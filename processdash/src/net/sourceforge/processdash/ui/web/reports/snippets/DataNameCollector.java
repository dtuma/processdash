// Copyright (C) 2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

package net.sourceforge.processdash.ui.web.reports.snippets;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.compiler.CompiledScript;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ui.DataComboBox;

public class DataNameCollector {

    public static void run(DataRepository data, String prefix, Pattern regexp,
            String replacement, boolean numbersOnly, boolean discardHidden,
            Collection dest) {
        Map dataElements = data.getDefaultDataElementsFor(prefix);
        if (dataElements == null)
            return;

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

            if (numbersOnly) {
                Object val = e.getValue();
                if ((val instanceof DoubleData == false)
                        && (val instanceof CompiledScript == false))
                    continue;
            }

            String name = (String) e.getKey();

            if (hiddenDataPattern != null
                    && hiddenDataPattern.matcher(name).find())
                continue;

            if (regexp == null) {
                dest.add(name);
            } else {
                Matcher m = regexp.matcher(name);
                if (m.matches()) {
                    if (replacement == null)
                        dest.add(name);
                    else
                        dest.add(m.replaceAll(replacement));
                }
            }
        }
    }

    private static String SETTING_NAME = DataComboBox.settingName;
}
