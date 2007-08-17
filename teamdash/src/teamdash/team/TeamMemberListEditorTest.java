
package teamdash.team;

import java.beans.EventHandler;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

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
                teamList);
        editor.addSaveListener((SaveListener) EventHandler.create(
            SaveListener.class, this, "quit"));
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
