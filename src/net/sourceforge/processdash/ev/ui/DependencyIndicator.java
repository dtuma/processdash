// Copyright (C) 2006-2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.ev.EVDependencyCalculator;
import net.sourceforge.processdash.ev.EVTask;
import net.sourceforge.processdash.ev.EVTaskDependency;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.ui.lib.SwingWorker;
import net.sourceforge.processdash.ui.lib.ToolTipTimingCustomizer;
import net.sourceforge.processdash.util.ThreadThrottler;

public class DependencyIndicator extends JLabel implements
        PropertyChangeListener {

    ProcessDashboard dash;

    DashboardContext context;

    ActiveTaskModel taskModel;

    private Worker currentWorker = null;

    public DependencyIndicator(ProcessDashboard dash, ActiveTaskModel taskModel) {
        this.dash = dash;
        this.context = dash;
        this.taskModel = taskModel;

        setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 2));
        new ToolTipTimingCustomizer().install(this);
        taskModel.addPropertyChangeListener(this);
    }

    public void update() {
        setIcon(null);
        setText(null);
        setToolTipText(null);
        setVisible(false);

        String taskPath = taskModel.getPath();
        currentWorker = null;
        if (shouldBeVisible(taskPath))
            new Worker(taskPath).start();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        update();
    }

    private boolean shouldBeVisible(String path) {
        if (path == null)
            return false;

        SaveableData enabledVal =  context.getData().getInheritableValue(path,
                ENABLED_DATA_NAME);
        if (enabledVal == null)
            return Settings.getBool(ENABLED_SETTING_NAME, true);

        SimpleData enabled = enabledVal.getSimpleValue();
        return (enabled != null && enabled.test());
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible != isVisible()) {
            super.setVisible(visible);
            dash.windowSizeRequirementsChanged();
        }
    }


    private class Worker extends SwingWorker {

        String taskPath;

        long requestTime;

        public Worker(String taskPath) {
            this.taskPath = taskPath;
            this.requestTime = System.currentTimeMillis();

            if (currentWorker != null)
                currentWorker.interrupt();
            currentWorker = this;
        }

        public Object construct() {
            ThreadThrottler.beginThrottling();

            // Find and recalc the dependencies associated with this task.
            EVDependencyCalculator calc = new EVDependencyCalculator(context
                    .getData(), context.getHierarchy(), context.getCache());
            String owner = ProcessDashboard.getOwnerName(context.getData());
            List dependencies = EVTaskDependency.getAllDependencies(context
                    .getData(), taskPath, owner);
            if (dependencies == null || dependencies.isEmpty())
                return dependencies;
            calc.recalculate(dependencies);

            // if there is only one EV task list containing this task, or if
            // there is a preferred task list, find the active task in that
            // task list, and compute the dependency target date.
            List taskListNames = EVTaskList.getPreferredTaskListsForPath(
                context.getData(), taskPath);
            if (taskListNames != null && taskListNames.size() == 1) {
                String taskListName = (String) taskListNames.get(0);
                EVTaskList tl = EVTaskList.openExisting(taskListName,
                    context.getData(), context.getHierarchy(),
                    context.getCache(), false);
                tl.recalc();
                List l = tl.findTasksByFullName(taskPath);
                if (l != null && l.size() == 1) {
                    EVTask t = (EVTask) l.get(0);
                    for (Iterator i = dependencies.iterator(); i.hasNext();) {
                        EVTaskDependency d = (EVTaskDependency) i.next();
                        d.loadParentDate(t);
                    }
                }
            }

            // an immediate change to the dashboard will cause the window to
            // apparently "jump", which the user might find disconcerting.
            // wait a moment before setting the icon.
            long now = System.currentTimeMillis();
            long elapsed = now - requestTime;
            if (elapsed < 1000) {
                try {
                    Thread.sleep(1000 - elapsed);
                } catch (InterruptedException ie) {}
            }

            return dependencies;
        }


        public void finished() {
            if (currentWorker != this)
                return;

            List dependencies = (List) get();

            TaskDependencyAnalyzer.GUI a = new TaskDependencyAnalyzer.GUI(
                    dependencies);
            switch (a.getStatus()) {

            case TaskDependencyAnalyzer.NO_DEPENDENCIES:
            case TaskDependencyAnalyzer.HAS_COMPLETED_REVERSE:
            case TaskDependencyAnalyzer.ALL_COMPLETE:
                break;

            case TaskDependencyAnalyzer.HAS_ERROR:
            case TaskDependencyAnalyzer.HAS_INCOMPLETE:
            case TaskDependencyAnalyzer.HAS_MISORDERED_INCOMPLETE:
            case TaskDependencyAnalyzer.HAS_REVERSE:
            case TaskDependencyAnalyzer.HAS_MISORDERED_REVERSE:
            case TaskDependencyAnalyzer.HAS_COLLAB:
                a.syncLabel(DependencyIndicator.this);
                setVisible(true);

                break;
            }
        }

    }

    private static final String ENABLED_DATA_NAME =
        "Show_Task_Dependency_Indicator";

    private static final String ENABLED_SETTING_NAME =
        "taskDependencyIndicator.visible";

}
