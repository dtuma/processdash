/*
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.izforge.izpack.panels;

import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.izforge.izpack.installer.InstallData;
import com.izforge.izpack.installer.InstallerFrame;
import com.izforge.izpack.installer.IzPanel;


public class ShutdownDashboardPanel extends IzPanel {

    /**  The layout. */
    private BoxLayout layout;


    public ShutdownDashboardPanel(InstallerFrame parent, InstallData idata) {
        super(parent, idata);

        // The 'super' layout
        GridBagLayout superLayout = new GridBagLayout();
        setLayout(superLayout);
        GridBagConstraints gbConstraints = new GridBagConstraints();
        gbConstraints.insets = new Insets(0, 0, 0, 0);
        gbConstraints.fill = GridBagConstraints.NONE;
        gbConstraints.anchor = GridBagConstraints.CENTER;

        // We initialize our 'real' layout
        JPanel centerPanel = new JPanel();
        layout = new BoxLayout(centerPanel, BoxLayout.Y_AXIS);
        centerPanel.setLayout(layout);
        superLayout.addLayoutComponent(centerPanel, gbConstraints);
        add(centerPanel);

        for (int i = 0; i < text.length; i++)
            centerPanel.add(new JLabel(text[i]));
    }

    private String[] text = {
        "If you are running the Process Dashboard right now,",
        "installing new files can cause unpredictable errors.  It is",
        "therefore imperative that you shut down the Dashboard if it",
        "is currently running.",
        " ",
        "If you are installing the Process Dashboard for the first",
        "time, or if you are not currently running the Process",
        "Dashboard, you can ignore this warning.",
        " ",
        "Otherwise, shut down any currently running instances of the",
        "Process Dashboard before you click the 'Next' button.",
    };


    public void panelActivate() {
        super.panelActivate();
        startScan();
    }


    public boolean isValidated() {
        if (isDashboardRunning()) {
            showErrorDialog();
            return false;
        } else {
            return true;
        }
    }


    private void showErrorDialog() {
        String[] message = new String[] {
            "This installer has detected that an instance",
            "of the Process Dashboard is currently running",
            "on this machine.  You must shut it down before",
            "you can proceed."
        };
        JOptionPane.showMessageDialog(
            this,
            message,
            "Process Dashboard Running",
            JOptionPane.ERROR_MESSAGE);
        needToRestart = true;
    }




    private static final int[] COMMON_PORTS = { 2468, 3000 };
    private static final int SCAN_WIDTH = 5;


    int numSearchers = 0;
    boolean foundDashboard;
    boolean needToRestart;

    private synchronized void startScan() {
        foundDashboard = needToRestart = false;
        for (int i = 0; i < COMMON_PORTS.length; i++)
            for (int j = 0; j < SCAN_WIDTH; j++) {
                (new Searcher(COMMON_PORTS[i]+j*2)).start();
                numSearchers++;
            }
    }

    private synchronized boolean isDashboardRunning() {
        if (needToRestart)
            startScan();

        if (!foundDashboard && numSearchers > 0) {
            getRootPane().setCursor
                (Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            try {
                wait(10000);
            } catch (InterruptedException e) { }

            getRootPane().setCursor(null);
        }

        return foundDashboard;
    }

    private synchronized void searcherFinished(boolean foundDash) {
        if (foundDash) foundDashboard = true;
        numSearchers--;
        if (foundDash || numSearchers == 0)
            notify();
    }

    private class Searcher extends Thread {
        int port;
        public Searcher(int port) { this.port = port; }
        public void run() {
            boolean result = isDashboardRunning(port);
            searcherFinished(result);
        }
    }

    private static final String RAISE_URL = "/control/raiseWindow.class";
    private static final String TIMESTAMP_HEADER = "Dash-Startup-Timestamp";

    private boolean isDashboardRunning(int port) {
        try {
            URL testUrl = new URL("http://localhost:"+port+RAISE_URL);
            HttpURLConnection conn =
                (HttpURLConnection) testUrl.openConnection();
            conn.connect();
            String timeStamp = conn.getHeaderField(TIMESTAMP_HEADER);
            return timeStamp != null;

        } catch (Exception e) {
        }
        return false;
    }
}
