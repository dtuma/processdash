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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.quicklauncher;

import java.awt.Cursor;
import java.beans.EventHandler;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListDataListener;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.DashboardIconFactory;


public class QuickLauncher {

    static Resources resources = Resources.getDashBundle("QuickLauncher");

    public static void main(String[] args) {
        new QuickLauncher();
    }

    DashboardProcessFactory processFactory;

    StatusObserver statusObserver;

    JFrame frame;

    public QuickLauncher() {
        try {
            processFactory = new DashboardProcessFactory();

            processFactory.addVmArg("-Dbackup.enabled=false");

            statusObserver = new StatusObserver();
            processFactory.addVmArg("-D"
                    + ProcessDashboard.NOTIFY_ON_OPEN_PORT_PROPERTY + "="
                    + statusObserver.getPort());

            buildUI();
        } catch (Exception e) {
            abortWithError(e.getMessage());
        }
    }

    private void buildUI() throws Exception {
        frame = new JFrame(resources.getString("Window_Title"));
        frame.setIconImage(DashboardIconFactory.getWindowIconImage());

        JPanel contents = new JPanel();
        contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
        LaunchDropZone th = new LaunchDropZone(this);
        contents.setTransferHandler(th);

        contents.add(new JLabel(resources.getString("Window_Prompt")));

        contents.add(new OptionCheckbox("Read_Only", "-DreadOnly=true", null,
                processFactory, false));
        contents.add(new OptionCheckbox("Disable_Export", "-D"
                + Settings.SYS_PROP_PREFIX + "export.disableAutoExport=true",
                null, processFactory, true));

        JList list = new JList(statusObserver);
        list.setVisibleRowCount(4);
        list.setTransferHandler(th);
        contents.add(new JScrollPane(list));

        statusObserver.addListDataListener((ListDataListener) EventHandler
                .create(ListDataListener.class, this, "updateCursor"));

        frame.getContentPane().add(contents);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.show();
    }

    public void updateCursor() {
        frame.setCursor(statusObserver.isEmpty() ? null : Cursor
                .getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    static void showError(Object message) {
        JOptionPane.showMessageDialog(null, message, resources
                .getString("Errors.Dialog_Title"), JOptionPane.ERROR_MESSAGE);
    }

    private void abortWithError(Object message) {
        showError(message);
        System.exit(1);
    }

    public void launchInstances(Collection launchers) {
        for (Iterator i = launchers.iterator(); i.hasNext();) {
            InstanceLauncher l = (InstanceLauncher) i.next();
            if (!statusObserver.containsEntry(l))
                new LaunchThread(l).start();
        }
    }

    private class LaunchThread extends Thread {

        InstanceLauncher launcher;

        public LaunchThread(InstanceLauncher launcher) {
            super(launcher);
            this.launcher = launcher;
            launcher.setProcessFactory(processFactory);
        }

        public void run() {
            try {
                String display = launcher.getDisplay();
                if (display == null)
                    launcher.setDisplay(resources.getString("Launcher.Display"));

                statusObserver.addEntry(launcher);

                super.run();

                launcher.waitForCompletion();
            } catch (LaunchException le) {
                showError(le.getMessage());
            } catch (InterruptedException ie) {
            } finally {
                statusObserver.removeEntry(launcher);
            }
        }

    }
}
