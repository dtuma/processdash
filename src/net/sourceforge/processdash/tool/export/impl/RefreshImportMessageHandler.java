// Copyright (C) 2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.impl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.Timer;

import net.sourceforge.processdash.msg.MessageEvent;
import net.sourceforge.processdash.msg.MessageHandler;
import net.sourceforge.processdash.tool.export.DataImporter;

public class RefreshImportMessageHandler extends Thread implements
        MessageHandler, ActionListener {

    private Set<String> locationsToRefresh;

    private Timer deferredRefreshTimer;

    private static final String[] MESSAGE_TYPES = { "pdash.refreshImport" };


    public RefreshImportMessageHandler() {
        super("RefreshImportMessageHandler");
        setDaemon(true);

        locationsToRefresh = new HashSet();

        deferredRefreshTimer = new Timer(750, this);
        deferredRefreshTimer.setRepeats(false);

        start();
    }

    public String[] getMessageTypes() {
        return MESSAGE_TYPES;
    }

    public synchronized void handle(MessageEvent message) {
        // find the string describing what collection we should refresh
        String location = message.getMessageXml().getAttribute("location");

        // add the location to our list, and start a timer to perform the
        // refresh a moment from now. This way, if we receive several identical
        // messages in quick succession, we will only perform the refresh once.
        locationsToRefresh.add(location);
        deferredRefreshTimer.restart();
    }

    public synchronized void actionPerformed(ActionEvent e) {
        notifyAll();
    }

    public void run() {
        while (true) {
            // wait until there is work for us to perform
            Set<String> thisRefreshBatch;
            synchronized (this) {
                while (locationsToRefresh.isEmpty()) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
                thisRefreshBatch = new HashSet<String>(locationsToRefresh);
                locationsToRefresh.clear();
            }

            // refresh data for all of the selected locations
            for (String location : thisRefreshBatch)
                DataImporter.refreshLocation(location);
        }
    }

}
