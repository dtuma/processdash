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

package net.sourceforge.processdash.team.setup;

import java.io.IOException;
import java.util.Date;

import org.xmlpull.v1.XmlSerializer;

public interface TeamSettingsDataWriter {

    /**
     * @return the version number of the dashboard when this writer last changed
     *         its output format
     */
    public String getFormatVersion();

    /**
     * @return the date when the source data for this writer last changed, or
     *         null if this writer does not have any data to write
     */
    public Date getDataTimestamp();

    /**
     * Write a well-formed XML fragment into a team settings file.
     */
    public void writeTeamSettings(String projectID, XmlSerializer xml)
            throws IOException;

}
