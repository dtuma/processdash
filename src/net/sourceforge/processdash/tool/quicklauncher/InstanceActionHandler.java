// Copyright (C) 2006-2019 Tuma Solutions, LLC
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

import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.EventHandler;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;

import net.sourceforge.processdash.util.FileUtils;

public class InstanceActionHandler implements MouseListener {

    JTable table;

    JPopupMenu popup;

    InstanceList instanceList;

    public InstanceActionHandler(JTable table, InstanceList instanceList) {
        this.table = table;
        this.instanceList = instanceList;

        // Create the popup menu.
        popup = new JPopupMenu();
        for (int i = 0; i < ACTION_KEYS.length; i++)
            popup.add(createMenuItem(ACTION_KEYS[i], ACTION_METHODS[i]));

        table.addMouseListener(this);
    }

    public void bringToFront() {
        bringToFront(table.getSelectedRow());
    }

    public void bringToFront(int pos) {
        makeHttpRequest(pos, "/control/raiseWindow.class");
    }

    public void showConsole() {
        makeHttpRequest(table.getSelectedRow(), "/control/showConsole.class");
    }

    public void refreshAllImports(int pos) {
        makeHttpRequest(pos, "/control/importNow.class");
    }

    private void makeHttpRequest(int pos, String uri) {
        if (pos == -1)
            return;
        try {
            InputStream in = getHttpResponse(pos, uri);
            if (in != null)
                FileUtils.slurpContents(in, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private InputStream getHttpResponse(int pos, String uri) throws IOException {
        DashboardInstance inst = instanceList.getInstance(pos);
        if (inst == null)
            return null;
        if (inst.getStatus() != DashboardInstance.RUNNING)
            return null;
        int httpPort = inst.getPort();
        if (httpPort < 0)
            return null;
        URL u = new URL("http", "localhost", httpPort, uri);
        URLConnection conn = u.openConnection();
        return conn.getInputStream();
    }



    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
            bringToFront(table.getSelectedRow());
        }
    }

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    public void mousePressed(MouseEvent e) {
        int row = table.rowAtPoint(e.getPoint());
        if (row != -1)
            table.getSelectionModel().setSelectionInterval(row, row);

        maybeShowPopup(e);
    }

    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
        if (!e.isPopupTrigger()) return;

        int selRow = table.getSelectedRow();
        if (selRow == -1) return;

        DashboardInstance inst = instanceList.getInstance(selRow);
        if (inst.getStatus() != DashboardInstance.RUNNING) return;
        if (inst.getPort() < 0) return;

        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    private JMenuItem createMenuItem(String resKey, String methodName) {
        String display = QuickLauncher.resources.getString("Actions."+resKey);
        JMenuItem result = new JMenuItem(display);
        result.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, methodName));
        return result;
    }

    private static final String[] ACTION_KEYS = { "Bring_To_Front",
            "Show_Console" };
    private static final String[] ACTION_METHODS = { "bringToFront",
            "showConsole" };
}
