// Copyright (C) 2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.lib;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class HTMLSplashScreen extends JWindow implements ActionListener {

    private boolean waitingForTimer;
    private boolean waitingForOK;

    public HTMLSplashScreen(Icon image, String html) {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        int contentWidth = 400;

        if (image != null) {
            contentWidth = image.getIconWidth();

            JLabel label = new JLabel(image, JLabel.CENTER);
            label.setAlignmentX(0);
            content.add(label);
            content.add(Box.createVerticalStrut(5));
        }

        JEditorPane editor = new JEditorPane();
        editor.setContentType("text/html");
        editor.setEditable(false);
        editor.setText(html);
        editor.setSize(contentWidth, 10);
        Dimension d = editor.getPreferredSize();
        d.width = contentWidth;
        editor.setMinimumSize(d);
        editor.setPreferredSize(d);
        editor.setMaximumSize(d);
        editor.setAlignmentX(0);
        content.add(editor);

        getContentPane().add(content);
        pack();
        setLocationRelativeTo(null);
    }

    public void displayFor(int millis) {
        waitingForOK = waitingForTimer = true;
        setVisible(true);

        Timer t = new Timer(millis, this);
        t.setRepeats(false);
        t.start();
    }

    private Object syncLock = new Object();

    public void okayToDispose() {
        synchronized (syncLock) {
            waitingForOK = false;
            maybeDispose();
        }
    }

    public void actionPerformed(ActionEvent e) {
        synchronized (syncLock) {
            waitingForTimer = false;
            maybeDispose();
        }
    }

    private void maybeDispose() {
        if (!waitingForOK && !waitingForTimer)
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    dispose();
                }});
    }

}
