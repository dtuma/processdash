// Copyright (C) 2013 Tuma Solutions, LLC
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

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class PleaseWaitDialog extends JDialog {

    private volatile boolean disposed;

    public PleaseWaitDialog(Frame parent, String title, String message,
            int displayDelay) {
        super(parent, title, true);

        JPanel dialogContents = new JPanel(new BorderLayout(5, 5));
        dialogContents.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        if (message == null)
            message = "Please wait...";
        JLabel messageLabel = new JLabel(message);
        dialogContents.add(messageLabel, BorderLayout.NORTH);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        dialogContents.add(progressBar, BorderLayout.CENTER);

        getContentPane().add(dialogContents);
        pack();
        setLocationRelativeTo(parent);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        this.disposed = false;
        Timer t = new Timer(displayDelay, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                maybeShow();
            }
        });
        t.setRepeats(false);
        t.start();
    }

    private void maybeShow() {
        if (!disposed)
            setVisible(true);
    }

    @Override
    public void dispose() {
        Runnable r = new Runnable() {
            public void run() {
                disposed = true;
                PleaseWaitDialog.super.dispose();
            }
        };

        if (SwingUtilities.isEventDispatchThread())
            r.run();
        else
            SwingUtilities.invokeLater(r);
    }

}
