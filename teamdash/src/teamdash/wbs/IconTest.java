
package teamdash.wbs;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

public class IconTest {

    static JLabel label;
    static JColorChooser chooser;

    public static void main(String argv[]) {
        JFrame frame = new JFrame();
        frame.getContentPane().add(new JLabel("              "),
                                   BorderLayout.NORTH);
        label = new JLabel();
        frame.getContentPane().add(label, BorderLayout.CENTER);
        frame.getContentPane().setBackground(Color.white);

        chooser = new JColorChooser();
        frame.getContentPane().add(chooser, BorderLayout.SOUTH);
        chooser.getSelectionModel().addChangeListener(new Listener());

        frame.pack();
        frame.show();
    }

    private static class Listener implements ChangeListener {

        public void stateChanged(ChangeEvent e) {
            Color c = chooser.getSelectionModel().getSelectedColor();
            label.setIcon(IconFactory.getDocumentIcon(c));
        }
    }

}
