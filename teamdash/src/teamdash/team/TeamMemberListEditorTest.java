// Copyright (C) 2002-2018 Tuma Solutions, LLC
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


package teamdash.team;

import java.beans.EventHandler;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import net.sourceforge.processdash.ui.lib.GuiPrefs;

import teamdash.SaveListener;
import teamdash.XMLUtils;

/** Simple test editor for the team member list
 */
public class TeamMemberListEditorTest implements TableModelListener {

    private TeamMemberList teamList;

    public static void main(String args[]) {
        new TeamMemberListEditorTest();
    }

    public TeamMemberListEditorTest() {
        try {
            teamList = new TeamMemberList(XMLUtils.parse(
                new FileInputStream("team.xml")).getDocumentElement());
        } catch (FileNotFoundException fnfe) {
            teamList = new TeamMemberList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        teamList.addTableModelListener(this);
        TeamMemberListEditor editor = new TeamMemberListEditor("Team Project",
                teamList, null, new GuiPrefs("teamListTest"));
        editor.addSaveListener((SaveListener) EventHandler.create(
            SaveListener.class, this, "quit"));
        editor.setCommitButtonIsSave(true);
        editor.show();
    }

    public void quit() {
        System.exit(0);
    }

    public void tableChanged(TableModelEvent e) {
        try {
            FileWriter out = new FileWriter("team.xml");
            teamList.getAsXML(out);
            out.close();
        } catch (IOException ioe) {}
    }

}
