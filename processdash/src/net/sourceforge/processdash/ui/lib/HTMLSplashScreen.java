// Copyright (C) 2006-2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

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

    // We switch text block every two seconds (2000 ms)
    private static final int TIME_PER_TEXT_BLOCK = 2000;

    private boolean waitingForTimer;
    private boolean waitingForOK;

    // The JPanel which contains all HTML blocks of text to display
    //  on the splash screen.
    private JPanel textCards = new JPanel(new CardLayout());

    // The Timer which shows a new HTML block every second.
    private Timer textSwitcher;

    private int nbOfBlockToShow;

    public HTMLSplashScreen(Icon image, List<String> htmlBlocks) {
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

        nbOfBlockToShow = 0;
        int maxPreferedHeight = 0;

        for (String text : htmlBlocks) {
            JEditorPane editor = new JEditorPane();
            editor.setContentType("text/html");
            editor.setEditable(false);
            editor.setText(text);
            editor.setSize(contentWidth, 10);

            Dimension editorPreferredSize = editor.getPreferredSize();

            if (editorPreferredSize.height > maxPreferedHeight)
                maxPreferedHeight = editorPreferredSize.height;

            textCards.add(editor, String.valueOf(nbOfBlockToShow++));
        }

        Dimension textDimension = new Dimension(contentWidth, maxPreferedHeight);
        textCards.setMinimumSize(textDimension);
        textCards.setPreferredSize(textDimension);
        textCards.setMaximumSize(textDimension);
        textCards.setAlignmentX(0);

        content.add(textCards);

        getContentPane().add(content);
        pack();
        setLocationRelativeTo(null);
    }

    public void displayFor(int timeToShowSplash) {
        waitingForOK = waitingForTimer = true;
        setVisible(true);

        textSwitcher = new Timer(TIME_PER_TEXT_BLOCK, new CardFlipper());
        textSwitcher.start();

        int minTimeToShowSplash = nbOfBlockToShow * TIME_PER_TEXT_BLOCK;
        timeToShowSplash = timeToShowSplash < minTimeToShowSplash ?
                               minTimeToShowSplash : timeToShowSplash;

        Timer displayTimeTimer = new Timer(timeToShowSplash, this);
        displayTimeTimer.setRepeats(false);
        displayTimeTimer.start();
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
                    textSwitcher.stop();
                    dispose();
                }});
    }

    private class CardFlipper implements ActionListener {
        // The CardLayout of the JPanel containing all html blocks to display
        private CardLayout layout = null;

        private int nbOfBlockShown;

        public CardFlipper() {
            layout = (CardLayout) textCards.getLayout();

            // We were showing block 0 before this CardFlippers got called
            //  for the first time so we start counting at 1.
            nbOfBlockShown = 1;
        }

        public void actionPerformed(ActionEvent e) {
            if (nbOfBlockShown < nbOfBlockToShow)
                layout.show(textCards, String.valueOf(nbOfBlockShown++));
        }

    }

}
