// Copyright (C) 2021 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.bundle.ui;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.templates.DataVersionChecker;
import net.sourceforge.processdash.templates.DataVersionChecker.Incompatibility;
import net.sourceforge.processdash.templates.ExtensionManager.DisabledExtensionException;
import net.sourceforge.processdash.tool.bridge.bundle.DatasetBundleMigrator;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleMode;
import net.sourceforge.processdash.ui.lib.ProgressDialog;

public class BundleMigrationAction extends AbstractAction {

    private DashboardContext ctx;

    private static Resources resources = Resources
            .getDashBundle("Bundler.Migrate");


    public BundleMigrationAction() {
        super(res("Menu_Text"));
    }


    public void setDashboardContext(DashboardContext ctx) {
        this.ctx = ctx;

        // don't show this item unless the user has enabled it
        if (!Settings.getBool("userPref.bundle.migrator.enabled", false))
            throw new DisabledExtensionException();

        // if this dataset is already bundled, don't show this menu item
        DatasetBundleMigrator dbm = new DatasetBundleMigrator(ctx, null);
        if (dbm.getDatasetMigrationTask() == null
                && dbm.getProjectMigrationTasks().isEmpty())
            throw new DisabledExtensionException();
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (!userConfirm("Confirm", JOptionPane.YES_NO_OPTION))
            return;

        // identify the target bundle mode for the directory
        FileBundleMode bundleMode = FileBundleMode.Local;

        // ensure the user has the minimum required versions of software
        if (checkVersions(bundleMode) == false)
            return;

        // if any projects need migrating, do that first
        ProcessDashboard parent = (ProcessDashboard) ctx;
        DatasetBundleMigrator dbm = new DatasetBundleMigrator(ctx, bundleMode);
        List<Runnable> projectMigrationTasks = dbm.getProjectMigrationTasks();
        if (!projectMigrationTasks.isEmpty()) {
            ProgressDialog dialog = new ProgressDialog(parent,
                    res("Projects.Title"), res("Projects.Message"));
            dialog.setCompletionMessage(res("Projects.Complete"));
            for (Runnable task : projectMigrationTasks)
                dialog.addTask(task);
            dialog.run();
        }

        // if the dashboard dir is already bundled, no further work is needed
        Runnable datasetMigrationTask = dbm.getDatasetMigrationTask();
        if (datasetMigrationTask == null)
            return;

        // ask the user for permission to shut down. Then close the dashboard,
        // and migrate the dashboard directory after shutdown is complete
        if (userConfirm("Restart", JOptionPane.OK_CANCEL_OPTION))
            parent.exitProgram(new MinTimeRunnable(datasetMigrationTask, 2000));
    }

    private boolean checkVersions(FileBundleMode bundleMode) {
        // check to see if the required packages are all satisfied
        Map<String, String> reqts = bundleMode.getMinVersions();
        List<Incompatibility> missingPackages = DataVersionChecker
                .checkPackageVersions(reqts);
        if (missingPackages.isEmpty()) {
            // register the versions so future clients know they are required
            DataVersionChecker.registerDataRequirements(reqts);
            return true;
        }

        // some packages need updating. Display a message and abort
        Object[] listItems = new Object[missingPackages.size()];
        for (int i = listItems.length; i-- > 0;)
            listItems[i] = missingPackages.get(i).getBullet();
        userConfirm("VersionCheck", JOptionPane.DEFAULT_OPTION, listItems);
        return false;
    }

    private boolean userConfirm(String resPrefix, int optionType,
            Object... extraLines) {
        String title = res(resPrefix + ".Title");
        Object message = resources.getStrings(resPrefix + ".Message");
        if (extraLines.length > 0)
            message = new Object[] { message, extraLines };
        return JOptionPane.showConfirmDialog((ProcessDashboard) ctx, message,
            title, optionType) == JOptionPane.OK_OPTION;
    }

    private static String res(String resKey) {
        return resources.getString(resKey);
    }

    private class MinTimeRunnable implements Runnable {

        private Runnable task;

        private long minTime;

        public MinTimeRunnable(Runnable task, long minTime) {
            this.task = task;
            this.minTime = minTime;
        }

        @Override
        public void run() {
            long start = System.currentTimeMillis();
            task.run();
            long end = System.currentTimeMillis();
            long elapsed = end - start;
            long remaining = minTime - elapsed;
            if (remaining > 0) {
                try {
                    Thread.sleep(remaining);
                } catch (InterruptedException e) {
                }
            }
        }

        @Override
        public String toString() {
            return task.toString();
        }
    }

}
