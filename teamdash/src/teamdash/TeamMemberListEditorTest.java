
package teamdash;

import java.io.FileWriter;
import java.io.IOException;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/** Simple test editor for the team member list
 */
public class TeamMemberListEditorTest implements TableModelListener {

    private TeamMemberList teamList;

    public static void main(String args[]) {
        new TeamMemberListEditorTest();
    }

    public TeamMemberListEditorTest() {
        teamList = new TeamMemberList();
        teamList.addTableModelListener(this);
        new TeamMemberListEditor("Team Project", teamList);
    }

    public void tableChanged(TableModelEvent e) {
        try {
            FileWriter out = new FileWriter("team.xml");
            teamList.getAsXML(out);
            out.close();
        } catch (IOException ioe) {}
    }

}
