
package teamdash;

import javax.swing.*;

public class TeamMemberListEditorTest {

    public static void main(String args[]) {
        JFrame f = new JFrame("Test");
        f.getContentPane().add(new TeamMemberListEditor());
        f.setDefaultCloseOperation(f.EXIT_ON_CLOSE);
        f.pack();
        f.show();
    }

}
