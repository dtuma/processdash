// Copyright (C) 2020 Tuma Solutions, LLC
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
import java.util.Arrays;
import java.util.Date;

import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

public class TeamDirPermissionSettingsWriter implements TeamSettingsDataWriter {

    private DashboardContext ctx;

    public TeamDirPermissionSettingsWriter(DashboardContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String getFormatVersion() {
        return "2.5.6";
    }

    @Override
    public Date getDataTimestamp(String projectID) {
        SimpleData sd = getProjectValue(projectID, TS_DATA_NAME);
        if (sd instanceof DateData)
            return ((DateData) sd).getValue();
        else
            return null;
    }

    @Override
    public void writeTeamSettings(String projectID, XmlSerializer xml)
            throws IOException {
        SimpleData sd = getProjectValue(projectID, PUB_DATA_NAME);
        writeTeamSettings(sd, xml);
    }

    private static void writeTeamSettings(SimpleData sd, XmlSerializer xml)
            throws IOException {
        if (sd != null && sd.test()) {
            String[] usernames = sd.format().split("[, \t]+");
            if (usernames.length > 0) {
                xml.startTag(null, ACCESS_TAG);
                xml.attribute(null, "type", "read-write");
                xml.attribute(null, USERS_ATTR,
                    StringUtils.join(Arrays.asList(usernames), ","));
                xml.endTag(null, ACCESS_TAG);
            }
        }
    }

    private SimpleData getProjectValue(String projectID, String name) {
        String path = getProjectPath(projectID);
        if (path == null)
            return null;

        String dataName = DataRepository.createDataName(path, name);
        return ctx.getData().getSimpleValue(dataName);
    }

    private String getProjectPath(String projectID) {
        if (projectID == null)
            return null;
        return getProjectPath(projectID, ctx.getHierarchy(), PropertyKey.ROOT);
    }

    private String getProjectPath(String projectID, DashHierarchy hier,
            PropertyKey key) {
        if (TeamSettingsRepublisher.isTeamProjectRoot(hier.getID(key))) {
            String projectPath = key.path();
            String dataName = DataRepository.createDataName(projectPath,
                TeamDataConstants.PROJECT_ID);
            SimpleData sd = ctx.getData().getSimpleValue(dataName);
            if (sd != null && projectID.equals(sd.format()))
                return projectPath;

        } else {
            for (int i = hier.getNumChildren(key); i-- > 0;) {
                PropertyKey child = hier.getChildKey(key, i);
                String childResult = getProjectPath(projectID, hier, child);
                if (childResult != null)
                    return childResult;
            }
        }
        return null;
    }



    public static class ExportTrigger extends TinyCGIBase {

        public ExportTrigger() {
            charset = "UTF-8";
        }

        protected void writeContents() throws IOException {
            // get the current and "last published" values of our setting
            DataContext data = getDataContext();
            SimpleData users = data.getSimpleValue(USER_DATA_NAME);
            SimpleData published = data.getSimpleValue(PUB_DATA_NAME);

            // if the value has changed, try republishing the settings file
            if (!eq(users, published)) {
                data.putValue(PUB_DATA_NAME, users);
                data.putValue(TS_DATA_NAME, new DateData());
                try {
                    TeamSettingsRepublisher.getInstance()
                            .republishProject(getPrefix(), true);
                } catch (NullPointerException npe) {
                    // if no republisher is operating (indiv mode), do nothing
                } catch (Exception e) {
                    // if the republish fails, log the error but proceed
                    // normally. Our updated timestamp will trigger the
                    // republisher to rewrite the setting on its next run.
                    e.printStackTrace();
                }
            }

            // write some nominal data to our PDASH file
            XmlSerializer xml = XMLUtils.getXmlSerializer(true);
            xml.setOutput(out);
            xml.startDocument(charset, null);
            xml.startTag(null, "accessPermissions");
            writeTeamSettings(users, xml);
            xml.endTag(null, "accessPermissions");
            xml.endDocument();
            out.flush();
        }

        private boolean eq(SimpleData a, SimpleData b) {
            if (a == b)
                return true;
            else if (a == null || b == null)
                return false;
            else
                return a.format().equals(b.format());
        }
    }



    private static final String USER_DATA_NAME = "Team_Data_Directory_Users";

    private static final String PUB_DATA_NAME = USER_DATA_NAME + "_Published";

    private static final String TS_DATA_NAME = USER_DATA_NAME + "_Timestamp";

    private static final String ACCESS_TAG = "access";

    private static final String USERS_ATTR = "users";

}
