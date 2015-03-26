// Copyright (C) 2006 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.quicklauncher;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class InstanceList extends AbstractTableModel {

    private List instances;

    private int notificationPort;

    private int id = 0;

    public InstanceList() throws IOException {
        this.instances = Collections.synchronizedList(new ArrayList());

        NotificationListener nl = new NotificationListener();
        this.notificationPort = nl.getListeningPort();
        nl.start();
    }

    public int getNotificationPort() {
        return notificationPort;
    }

    public synchronized void addInstance(DashboardInstance newInstance) {
        int nextId = ++this.id;
        newInstance.setId(nextId);
        newInstance.setObserver(this);
        instances.add(0, newInstance);
        fireTableRowsInserted(0, 0);
    }

    protected DashboardInstance getInstance(int pos) {
        if (pos < 0 || pos >= instances.size())
            return null;
        else
            return (DashboardInstance) instances.get(pos);
    }

    protected synchronized int indexOf(int id) {
        for (int i = 0; i < instances.size(); i++) {
            if (getInstance(i).getId() == id)
                return i;
        }
        return -1;
    }

    protected synchronized int indexOfSimilar(DashboardInstance inst) {
        for (int i = 0; i < instances.size(); i++) {
            if (getInstance(i).equals(inst))
                return i;
        }
        return -1;
    }

    public void removeInstance(DashboardInstance inst) {
        removeInstance(inst.getId());
    }

    public synchronized void removeInstance(int id) {
        int pos = indexOf(id);
        if (pos != -1) {
            instances.remove(pos);
            fireTableRowsDeleted(pos, pos);
        }
    }

    public boolean hasLaunching() {
        for (Iterator i = instances.iterator(); i.hasNext();) {
            DashboardInstance inst = (DashboardInstance) i.next();
            if (inst.getStatus() == DashboardInstance.LAUNCHING)
                return true;
        }
        return false;
    }

    private static final Resources resources = QuickLauncher.resources;

    private static final String[] COLUMN_KEYS = { "Label", "Status" };

    private static final int COL_LABEL = 0;

    private static final int COL_STATUS = 1;

    private static final String[] COLUMN_NAMES = resources.getStrings(
            "Instances.", COLUMN_KEYS, ".Name");

    static final int[] COLUMN_WIDTHS = resources.getInts("Instances.",
            COLUMN_KEYS, ".Width_");



    public int getColumnCount() {
        return COLUMN_KEYS.length;
    }

    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    public int getRowCount() {
        return instances.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= getRowCount())
            return null;
        DashboardInstance inst = getInstance(rowIndex);
        switch (columnIndex) {
        case COL_LABEL: return inst.getDisplay();
        case COL_STATUS: return inst.getStatusDisplay();
        default: return null;
        }
    }


    private void gotNotification(Element notification) {
        int id = XMLUtils.getXMLInt(notification, "instanceId");
        int pos = indexOf(id);
        if (pos != -1)
            getInstance(pos).handleNotification(notification);
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
            InputStream in = s.getInputStream();
            Document doc = XMLUtils.parse(in);
            gotNotification(doc.getDocumentElement());
            s.close();
        }

    }

    public void instanceChanged(DashboardInstance instance) {
        int pos = instances.indexOf(instance);
        if (pos != -1)
            fireTableRowsUpdated(pos, pos);
    }

}
