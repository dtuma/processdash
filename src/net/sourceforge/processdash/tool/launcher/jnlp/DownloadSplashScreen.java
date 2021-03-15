// Copyright (C) 2015-2021 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.launcher.jnlp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class DownloadSplashScreen extends JWindow {

    private JProgressBar progressBar;

    private JLabel activityLabel;

    public DownloadSplashScreen(int totalSize) {

        // create a panel to hold splash screen contents
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints g = new GridBagConstraints();
        JPanel content = new JPanel(layout);
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        // display a banner image with the Process Dashboard icon
        ImageIcon bannerImage = new ImageIcon(DownloadSplashScreen.class
                .getResource("/net/sourceforge/processdash/ui/splash.png"));
        JLabel banner = new JLabel(bannerImage);
        g.gridx = g.gridy = 0;
        content.add(banner);
        layout.setConstraints(banner, g);

        // create a label that can be used to display messages to the user
        activityLabel = new JLabel(" ", SwingConstants.CENTER);
        activityLabel.setFont(activityLabel.getFont().deriveFont(Font.PLAIN));
        Dimension d = activityLabel.getPreferredSize();
        d.width = bannerImage.getIconWidth();
        activityLabel.setPreferredSize(d);
        g.gridy = 2;
        content.add(activityLabel);
        layout.setConstraints(activityLabel, g);

        // create a horizontal bar to display download progress
        progressBar = new JProgressBar(0, totalSize);
        g.gridy = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(10, 0, 5, 0);
        content.add(progressBar);
        layout.setConstraints(progressBar, g);

        // size, center, and display the splash screen
        getContentPane().add(content);
    }

    public void display() {
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void setActivity(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                activityLabel.setText(text);
            }
        });
    }

    public void addProgress(final int delta) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int progress = progressBar.getValue();
                progressBar.setValue(progress + delta);
            }
        });
    }

}
