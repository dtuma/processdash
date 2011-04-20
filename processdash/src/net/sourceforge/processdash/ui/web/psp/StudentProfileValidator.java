// Copyright (C) 2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.psp;

import java.io.IOException;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.ui.web.TinyCGIBase;

public class StudentProfileValidator extends TinyCGIBase {

    @Override
    protected void doPost() throws IOException {
        String redirect;
        if (checkValues()) {
            redirect = "student-profile-success.htm";
        } else {
            redirect = "student-profile.shtm?missingData";
        }

        out.write("Location: ");
        out.write(redirect);
        out.write("\r\n\r\n");
    }

    private boolean checkValues() {
        String owner = getOwner();
        if (owner == null || owner.trim().length() == 0)
            return false;

        DataContext c = getDataContext();
        for (String name : NAMES_TO_CHECK) {
            SimpleData sd = c.getSimpleValue(name);
            if (sd == null || !sd.test() || sd.format().trim().length() == 0)
                return false;
        }

        c.putValue("../Student_Profile_Complete", ImmutableDoubleData.TRUE);
        if (c.getSimpleValue("Completed") == null)
            c.putValue("Completed", new DateData());

        return true;
    }

    private static final String[] NAMES_TO_CHECK = { "Profile_Initials",
            "Profile_Organization", "Profile_Instructor", "ProjectLang" };

}
