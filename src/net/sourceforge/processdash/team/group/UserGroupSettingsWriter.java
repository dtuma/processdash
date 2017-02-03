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

package net.sourceforge.processdash.team.group;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.Timer;

import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.team.setup.TeamSettingsDataWriter;
import net.sourceforge.processdash.team.setup.TeamSettingsRepublisher;

public class UserGroupSettingsWriter implements TeamSettingsDataWriter,
        UserGroupEditListener, ActionListener {

    private Timer timer;

    public UserGroupSettingsWriter() {
        timer = new Timer(1000, this);
        timer.setRepeats(false);
        UserGroupManager.getInstance().addUserGroupEditListener(this);
    }

    @Override
    public void userGroupEdited(UserGroupEditEvent e) {
        if (e.getGroup().isCustom() == false)
            timer.restart();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        TeamSettingsRepublisher.getInstance().requestRepublish();
    }


    @Override
    public String getFormatVersion() {
        return "2.3.1";
    }

    @Override
    public Date getDataTimestamp() {
        return UserGroupManager.getInstance().getSharedGroupsTimestamp();
    }

    @Override
    public void writeTeamSettings(String projectID, XmlSerializer xml)
            throws IOException {
        // write an opening <groups> tag
        xml.startTag(null, GROUPS_TAG);

        // write data for each group
        List<UserGroup> groups = new ArrayList<UserGroup>(
                UserGroupManager.getInstance().getGroups().values());
        Collections.sort(groups);
        for (UserGroup oneGroup : groups) {
            if (oneGroup.isCustom() == false)
                oneGroup.getAsXml(xml);
        }

        // close the </groups> tag
        xml.endTag(null, GROUPS_TAG);
    }

    private static final String GROUPS_TAG = "groups";

}
