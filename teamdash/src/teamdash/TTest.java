
package teamdash;

import javax.swing.*;

public class TTest {

    public static void main(String argv[]) {
        new TTest();
    }

    public TTest() {
        JTable table = new JTable(30, 1);
        JScrollPane sp = new JScrollPane(table);

        JFrame frame = new JFrame("TTest");
        frame.getContentPane().add(sp);
        frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
        frame.pack();
        frame.show();
    }


}
