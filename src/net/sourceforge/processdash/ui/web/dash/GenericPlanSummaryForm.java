// Copyright (C) 2003-2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.dash;


import java.io.IOException;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;



public class GenericPlanSummaryForm extends TinyCGIBase {

    private static Resources resources =
        Resources.getDashBundle("Templates.Generic");

    private static final String UNITS_NAME = "Units";


    protected void writeContents() throws IOException {
        StringBuffer uri = new StringBuffer((String) env.get("SCRIPT_PATH"));
        uri.setLength(uri.length()-6);
        uri.append(".shtm");

        String unit, units;
        DataRepository data = getDataRepository();
        String prefix = getPrefix();
        SimpleData d = data.getSimpleValue(prefix + "/" + UNITS_NAME);
        units = (d != null ? d.format() : null);
        if (units == null || units.trim().length() == 0)
            units = resources.getString("Default_Units");

        int semicolonPos = units.indexOf(';');
        if (semicolonPos > -1) {
            unit  = units.substring(0, semicolonPos);
            units = units.substring(semicolonPos+1);
        } else if (units.endsWith("s")) {
            unit = units.substring(0, units.length() - 1);
        } else {
            unit = units;
        }
        HTMLUtils.appendQuery(uri, "Unit", unit);
        HTMLUtils.appendQuery(uri, "Units", units);

        String text = getRequestAsString(uri.toString());
        out.write(text);
    }

}
