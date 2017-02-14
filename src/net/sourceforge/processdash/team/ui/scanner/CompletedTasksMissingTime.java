// Copyright (C) 2017 Tuma Solutions, LLC
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.processdash.tool.db.PersonFilter;

public class CompletedTasksMissingTime extends GenericScanItemList {

    private static final String PERMISSION = "pdash.reports.scanner";

    @Override
    protected List<Object[]> getItems() throws IOException {
        // ensure the user has permission to view tasks with missing time
        PersonFilter privacyFilter = new PersonFilter(PERMISSION, //
                getPdash().getQuery());
        if (privacyFilter.isBlock())
            return Collections.EMPTY_LIST;

        List<Object[]> items = super.getItems();
        for (Iterator<Object[]> i = items.iterator(); i.hasNext();) {
            Object[] row = i.next();
            Object personKey = row[7];
            if (!privacyFilter.include(personKey))
                i.remove();
        }

        return items;
    }

}
