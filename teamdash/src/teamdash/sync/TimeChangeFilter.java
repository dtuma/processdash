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

package teamdash.sync;

import java.util.Map;

public class TimeChangeFilter implements ProjDumpFilter {

    private Map<String, String> timeChanges;

    public TimeChangeFilter(Map<String, String> timeChanges) {
        this.timeChanges = timeChanges;
    }

    @Override
    public String filterLine(String line, String nodeID) {
        String newTimeVal = timeChanges.get(nodeID);
        if (newTimeVal != null) {
            // the replaceXmlAttr method does nothing if the named attribute
            // is not present; thus, we can ask both time attributes to be
            // replaced, and one will be a no-op.
            line = FilterUtils.replaceXmlAttr(line, "time", newTimeVal);
            line = FilterUtils.replaceXmlAttr(line, "deferredTime", newTimeVal);
        }
        return line;
    }

}
