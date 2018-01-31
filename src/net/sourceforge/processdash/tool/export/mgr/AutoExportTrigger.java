// Copyright (C) 2014-2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.mgr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import javax.swing.Timer;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.DashHierarchy.Event;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.util.XMLUtils;

public class AutoExportTrigger implements Runnable, DashHierarchy.Listener,
        DataListener, ActionListener {

    private DashboardContext ctx;

    private String requiredTag;

    private Set<String> valuesToWatch;

    private Set<String> listeningToDataNames;

    private volatile boolean currentlyRegisteringListeners;

    private Set<String> exportsNeeded;

    private Timer exportTimer;

    public AutoExportTrigger() {
        listeningToDataNames = Collections.EMPTY_SET;
        currentlyRegisteringListeners = false;
        exportsNeeded = new HashSet<String>();
        exportTimer = new Timer(300, this);
        exportTimer.setRepeats(false);
    }

    public void setDashboardContext(DashboardContext dashboardContext) {
        this.ctx = dashboardContext;
        ctx.getHierarchy().addHierarchyListener(this);
    }

    public void setConfigElement(Element xml, String attrName) {
        this.requiredTag = xml.getAttribute("forTag");
        if (!XMLUtils.hasValue(requiredTag))
            throw new IllegalArgumentException("no 'forTag' attribute provided");

        this.valuesToWatch = new HashSet<String>();
        NodeList nl = xml.getElementsByTagName("onDataChange");
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            valuesToWatch.add(e.getTextContent());
        }
        if (valuesToWatch.isEmpty())
            throw new IllegalArgumentException(
                    "no 'onDataChange' elements provided");
    }

    // called by the background task thread to initialize this object
    public void run() {
        scanHierarchy();
    }

    // messaged when the hierarchy has been modified
    public void hierarchyChanged(Event e) {
        if (!e.isAdjusting())
            scanHierarchy();
    }

    private void scanHierarchy() {
        Set<String> newDataNamesToWatch = new HashSet();
        scanHierarchy(newDataNamesToWatch, PropertyKey.ROOT, false);
        try {
            currentlyRegisteringListeners = true;
            updateDataRegistrations(newDataNamesToWatch);
        } finally {
            currentlyRegisteringListeners = false;
        }
    }

    private void scanHierarchy(Set<String> dataNamesToWatch, PropertyKey node,
            boolean pathHasActiveExport) {
        String path = node.path();
        pathHasActiveExport = pathHasActiveExport
                || ExportManager.getInstance().hasEnabledExportTask(path);

        if (pathHasActiveExport && test(path, requiredTag)) {
            for (String watch : valuesToWatch) {
                String dataName = DataRepository.createDataName(path, watch);
                dataNamesToWatch.add(dataName);
            }
        }

        int numChildren = ctx.getHierarchy().getNumChildren(node);
        for (int i = numChildren; i-- > 0;) {
            PropertyKey child = ctx.getHierarchy().getChildKey(node, i);
            scanHierarchy(dataNamesToWatch, child, pathHasActiveExport);
        }
    }

    private boolean test(String path, String name) {
        String dataName = DataRepository.createDataName(path, name);
        SimpleData sd = ctx.getData().getSimpleValue(dataName);
        return (sd != null && sd.test());
    }

    private void updateDataRegistrations(Set<String> newNamesToWatch) {
        // make a list of the registrations that have changed
        Set<String> unneededNames;
        Set<String> newNames;
        synchronized (this) {
            unneededNames = setDiff(listeningToDataNames, newNamesToWatch);
            newNames = setDiff(newNamesToWatch, listeningToDataNames);
            this.listeningToDataNames = newNamesToWatch;
        }

        // stop listening to any elements we no longer care about.
        DataRepository data = ctx.getData();
        for (String oneName : unneededNames)
            data.removeDataListener(oneName, this);

        // begin listening to any newly identified data elements.
        for (String oneName : newNames)
            data.addDataListener(oneName, this, true);
    }

    private Set<String> setDiff(Set a, Set b) {
        Set<String> result = new HashSet<String>(a);
        result.removeAll(b);
        return result;
    }

    // messaged by the data repository when a registered value changes
    public void dataValuesChanged(Vector v) {
        for (DataEvent e : (Vector<DataEvent>) v)
            dataValueChanged(e);
    }

    public void dataValueChanged(DataEvent e) {
        if (!currentlyRegisteringListeners)
            addExportPath(e.getName());
    }

    private void addExportPath(String dataName) {
        StringBuffer prefix = new StringBuffer(dataName);
        if (ctx.getData().getInheritableValue(prefix,
            ExportManager.EXPORT_DATANAME) != null) {
            exportsNeeded.add(prefix.toString());
            exportTimer.restart();
        }
    }

    // messaged by the export timer when the delay has expired
    public synchronized void actionPerformed(ActionEvent e) {
        new Exporter().start();
    }

    private class Exporter extends Thread {
        Set<String> pathsToExport;

        public Exporter() {
            this.pathsToExport = exportsNeeded;
            exportsNeeded = new HashSet<String>();
        }

        @Override
        public void run() {
            for (String path : pathsToExport) {
                System.out.println("Exporting " + path);
                DashController.exportData(path);
            }
        }
    }

}
