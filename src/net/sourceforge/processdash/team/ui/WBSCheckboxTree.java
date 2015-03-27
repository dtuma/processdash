// Copyright (C) 2002-2010 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.templates.setup;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.util.HTMLUtils;

public class filterWBS extends selectWBS {

    String wbsID;

    protected void initialize(DashHierarchy properties, PropertyKey key,
            String rootId, int i, String rootPrefix) {
        super.initialize(properties, key, rootId, i, rootPrefix);

        String dataName = DataRepository.createDataName(rootPrefix,
                "Project_WBS_ID");
        SimpleData wbsIdVal = getDataRepository().getSimpleValue(dataName);
        wbsID = (wbsIdVal == null ? "NULL" : wbsIdVal.format());
    }

    protected String getScript() {
        return "";
    }

    protected void printLink(String rootPath, String relPath) {
        out.print("<input type='checkbox' class='notData' name='");
        out.print(HTMLUtils.escapeEntities(wbsID));
        if (relPath != null && relPath.length() > 0) {
            out.print("/");
            out.print(HTMLUtils.escapeEntities(relPath));
        }
        out.print("' checked onclick='updateSelected(this);' />&nbsp;");
        out.print("<a href='#' onclick='toggleSelected(this); return false;'>");
    }


}
