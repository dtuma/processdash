// Copyright (C) 2010 Tuma Solutions, LLC
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

import java.io.IOException;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.XMLUtils;

public class AddScheduleWriter extends TinyCGIBase {

    public AddScheduleWriter() {
        this.charset = "UTF-8";
    }

    @Override
    protected void writeHeader() {
        out.print("Content-type: text/xml; charset=UTF-8\r\n\r\n");
        out.flush();
    }

    @Override
    protected void writeContents() throws IOException {
        String projectID = getData(TeamDataConstants.PROJECT_ID);
        String scheduleName = getData(TeamDataConstants.PROJECT_SCHEDULE_NAME);
        String scheduleID = getData(TeamDataConstants.PROJECT_SCHEDULE_ID);

        out.write("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>\n");
        out.write("<messages>\n");

        if (hasValue(projectID, scheduleName, scheduleID))
            writeMessage(XMLUtils.escapeAttribute(projectID),
                    XMLUtils.escapeAttribute(scheduleID));

        out.write("</messages>\n");
    }

    private void writeMessage(String projectID, String taskListID) {
        out.write(I + "<message type='pdash.alterTaskList' msgId='" + projectID
                + ":" + taskListID + "'>\n");
        out.write(I + I + "<addToRollup forProjectID='" + projectID + "'>\n");
        out.write(I + I + I + "<addTaskList taskListID='" + taskListID + "' />\n");
        out.write(I + I + "</addToRollup>\n");
        out.write(I + "</message>\n");
    }

    private String getData(String name) {
        SimpleData sd = getDataContext().getSimpleValue(name);
        return (sd == null ? null : sd.format());
    }

    private boolean hasValue(String... values) {
        for (String s : values)
            if (s == null || s.trim().length() == 0)
                return false;
        return true;
    }

    private static final String I = "   ";

}
