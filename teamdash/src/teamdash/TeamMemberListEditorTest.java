
package teamdash;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.*;

public class TeamMemberListEditorTest implements WindowListener {

    private TeamMemberListEditor editor;

    public static void main(String args[]) {
        new TeamMemberListEditorTest();
    }

    public TeamMemberListEditorTest() {
        editor = new TeamMemberListEditor(new TeamMemberList());
        editor.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        editor.frame.addWindowListener(this);
    }

    public void windowOpened(WindowEvent e) {}
    public void windowClosing(WindowEvent e) {
        try {
            FileWriter out = new FileWriter("team.xml");
            editor.teamMemberList.getAsXML(out);
            out.close();
        } catch (IOException ioe) {}
    }
    public void windowClosed(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}

}
