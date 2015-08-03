// Copyright (C) 2015 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.ui.scanner;

import java.io.IOException;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.ui.web.TinyCGIBase;

public class ScanItemListBulkHandler extends TinyCGIBase {

    @Override
    protected void writeContents() throws IOException {
        parseFormData();
        String[] itemIDs = (String[]) parameters.get("id_ALL");
        boolean checked = "true".equals(parameters.get("checked"));
        DateData valueToStore = checked ? new DateData() : null;

        DataContext data = getDataContext();
        for (String itemID : itemIDs) {
            if (!checkValidID(itemID))
                throw new TinyCGIException(400, "Bad item ID");
            data.putValue(itemID, valueToStore);
        }

        out.write("OK");
    }

    private boolean checkValidID(String itemID) {
        if (itemID.startsWith("/") || itemID.contains(".."))
            return false;
        else if (itemID.indexOf('/') == -1)
            return false;
        else
            return true;
    }

}
