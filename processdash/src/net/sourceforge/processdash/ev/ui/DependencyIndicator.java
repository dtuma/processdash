// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2006 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ev.ui;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.ev.EVDependencyCalculator;
import net.sourceforge.processdash.ev.EVTaskDependency;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.ui.lib.ToolTipTimingCustomizer;

public class DependencyIndicator extends JLabel implements
        PropertyChangeListener {

    Window window;

    DataContext data;

    Worker worker;

    ActiveTaskModel taskModel;

    public DependencyIndicator(ProcessDashboard dash, ActiveTaskModel taskModel) {
        this.window = dash;
        this.data = dash.getData();
        this.taskModel = taskModel;
        this.worker = new Worker(dash);

        new ToolTipTimingCustomizer().install(this);
        taskModel.addPropertyChangeListener(this);
    }

    public void update() {
        setIcon(null);
        setText(null);
        setToolTipText(null);

        String taskPath = taskModel.getPath();
        List dependencies = EVTaskDependency.getAllDependencies(data, taskPath);
        if (dependencies != null && !dependencies.isEmpty()) {
            setIcon(BLANK_ICON);
            window.pack();
            worker.doCalc(dependencies);
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        update();
    }


    private class Worker extends Thread {

        EVDependencyCalculator calc;

        List dependenciesToCalc = null;

        public Worker(DashboardContext context) {
            super(Worker.class.getName());
            calc = new EVDependencyCalculator(context.getData(), context
                    .getHierarchy(), context.getCache());
            setDaemon(true);
            start();
        }

        public synchronized void doCalc(List dependencies) {
            dependenciesToCalc = dependencies;
            notify();
        }

        public void run() {
            while (true) {
                List dependencies;
                synchronized (this) {
                    while (dependenciesToCalc == null)
                        try {
                            wait();
                        } catch (InterruptedException e) {
                        }
                    dependencies = dependenciesToCalc;
                    dependenciesToCalc = null;
                }

                calc.recalculate(dependencies);

                synchronized (this) {
                    if (dependenciesToCalc == null)
                        SwingUtilities.invokeLater(new Updater(dependencies));
                }
            }
        }
    }

    private class Updater implements Runnable {

        List dependencies;

        public Updater(List dependencies) {
            this.dependencies = dependencies;
        }

        public void run() {
            TaskDependencyAnalyzer.GUI a = new TaskDependencyAnalyzer.GUI(
                    dependencies);
            switch (a.getStatus()) {

            case TaskDependencyAnalyzer.NO_DEPENDENCIES:
            case TaskDependencyAnalyzer.ALL_COMPLETE:
                break;

            case TaskDependencyAnalyzer.HAS_ERROR:
            case TaskDependencyAnalyzer.HAS_INCOMPLETE:
                a.syncLabel(DependencyIndicator.this);
                window.pack();
                break;
            }
        }

    }

    private static final Icon BLANK_ICON = new Icon() {
        public int getIconHeight() { return 11; }
        public int getIconWidth() { return 11; }
        public void paintIcon(Component c, Graphics g, int x, int y) { }
    };

}
