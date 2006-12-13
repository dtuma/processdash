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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.DefaultListModel;

public class StatusObserver extends DefaultListModel {

    private int port;

    private int id = 0;

    public StatusObserver() throws IOException {
        NotificationListener nl = new NotificationListener();
        this.port = nl.getListeningPort();
        nl.start();
    }

    public int getPort() {
        return port;
    }

    public void addEntry(InstanceLauncher launcher) {
        int nextId;
        synchronized (this) {
            nextId = ++this.id;
        }
        launcher.setId(nextId);
        addElement(new Entry(launcher));
    }

    public boolean containsEntry(InstanceLauncher l) {
        for (int i = getSize(); i-- > 0;) {
            Entry e = (Entry) getElementAt(i);
            if (e.launcher.equals(l))
                return true;
        }
        return false;
    }

    public void removeEntry(InstanceLauncher l) {
        removeEntry(l.getId());
    }

    public void removeEntry(int id) {
        for (int i = getSize(); i-- > 0;) {
            Entry e = (Entry) getElementAt(i);
            if (e.launcher.getId() == id) {
                removeElement(e);
                return;
            }
        }
    }


    private void gotNotification(String idStr) {
        removeEntry(Integer.parseInt(idStr));
    }

    private class Entry {
        private InstanceLauncher launcher;

        public Entry(InstanceLauncher launcher) {
            this.launcher = launcher;
        }

        public String toString() {
            return QuickLauncher.resources.format("Status_Message_FMT",
                    launcher.getDisplay());
        }

    }

    private class NotificationListener extends Thread {
        private ServerSocket socket;

        public NotificationListener() throws IOException {
            setDaemon(true);
            socket = new ServerSocket(0, 50, InetAddress.getLocalHost());
        }

        public int getListeningPort() {
            return socket.getLocalPort();
        }

        public void run() {
            while (true) {
                try {
                    handleNotification();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleNotification() throws Exception {
            Socket s = socket.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(s
                    .getInputStream(), "UTF-8"));
            String id = in.readLine();
            gotNotification(id);
            in.close();
            s.close();
        }

    }
}
