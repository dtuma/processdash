// Copyright (C) 2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.redact.filter;

import net.sourceforge.processdash.tool.redact.EnabledFor;
import net.sourceforge.processdash.tool.redact.LocationMapper;

public class FilterDataContainingLocations extends AbstractDataStringFilter {

    @EnabledFor("^Team_Directory(_UNC)?$")
    public String filterTeamDataDir(String dataDir) {
        if (dataDir.trim().length() == 0)
            return null;
        else
            return LocationMapper.FAKE_TEAM_DIR;
    }

    @EnabledFor("^Team_Data_Directory_URL$")
    public String filter(String url) {
        return LocationMapper.mapURL(url);
    }

}
