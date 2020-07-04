// Copyright (C) 2017-2020 Tuma Solutions, LLC
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
import java.util.List;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;

public class RepublishTeamSettings extends TinyCGIBase {

    @Override
    protected void writeContents() throws IOException {
        // the republisher isn't normally initialized for personal dashboards,
        // so if we're in a personal dashboard, make sure it's been initialized
        if (!Settings.isTeamMode())
            TeamSettingsRepublisher.init(getDashboardContext());

        // perform the requested republish operation
        List<String> errors = TeamSettingsRepublisher.getInstance()
                .republish(true);

        // write the results
        out.write(
            "<html><head><title>Republish Project Settings</title></head>\n");
        out.write("<body><h1>Republish Project Settings</h1>\n");
        out.write("Project settings files republished at " + new Date());

        // if any errors were encountered, mention them
        if (!errors.isEmpty()) {
            out.write("<p>Settings for the following projects could "
                    + "not be published:<ul>");
            for (String project : errors)
                out.write("<li>" + HTMLUtils.escapeEntities(project) + "</li>");
            out.write("</ul>The dashboard's <a href='showConsole.class'>debug "
                    + "log</a> may have more information.</p>");
        }

        // end the document
        out.write("</body></html>\n");
    }

}
