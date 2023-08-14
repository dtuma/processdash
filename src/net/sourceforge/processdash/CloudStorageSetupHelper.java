// Copyright (C) 2023 Tuma Solutions, LLC
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

package net.sourceforge.processdash;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;

import net.sourceforge.processdash.team.setup.move.CloudStorageDashboardWorker;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleMigrator;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleMode;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleUtils;
import net.sourceforge.processdash.tool.bridge.client.LocalWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.tool.bridge.impl.DashboardInstanceStrategy;
import net.sourceforge.processdash.tool.bridge.impl.SyncClientMappings;
import net.sourceforge.processdash.tool.export.mgr.FolderMappingManager;
import net.sourceforge.processdash.tool.quicklauncher.CompressedInstanceLauncher;
import net.sourceforge.processdash.ui.LookAndFeelUtil;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.StringUtils;

class CloudStorageSetupHelper {

    private static Boolean IS_CLOUD_STORAGE;

    static void runNewTeamDashboardCheck(Component parent, String location) {
        // abort if we don't have an editable session
        if (!isEditableSession())
            return;

        // abort if the target directory doesn't look like a cloud dir
        if (!StringUtils.hasValue(location))
            location = System.getProperty("user.dir");
        File dir = new File(location);
        if (!mightBeCloudDir(dir))
            return;

        // abort the the target directory isn't a new team dashboard
        if (!isEmptyTeamDashboard(dir))
            return;

        // ask the user if this dashboard is being synced to cloud storage
        LookAndFeelUtil.setDefaultLAF();
        IS_CLOUD_STORAGE = askIfCloudStorage(parent, true,
            dir.getAbsolutePath());

        // if the user said yes, bundle the directory
        if (Boolean.TRUE.equals(IS_CLOUD_STORAGE)) {
            try {
                System.out.println("Bundling new cloud-based Team Dashboard");
                FileBundleMigrator.migrate(dir,
                    DashboardInstanceStrategy.INSTANCE, FileBundleMode.Sync);
                CloudStorageDashboardWorker.getShareableDataDirectoryPath(dir);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    static void runPostStartupChecks(ProcessDashboard dash) {
        // abort if we don't have an editable session
        if (!isEditableSession())
            return;

        // if there is already a setting for the cloud storage flag, abort
        if (Settings.getVal(CLOUD_SETTING) != null)
            return;

        // if this is a personal dashboard whose working dir might be cloud,
        WorkingDirectory wd = dash.getWorkingDirectory();
        File dir = wd.getTargetDirectory();
        if (Settings.isPersonalMode() //
                && wd instanceof LocalWorkingDirectory //
                && mightBeCloudDir(dir)) {
            // ask the user if this personal dashboard is using cloud storage
            IS_CLOUD_STORAGE = askIfCloudStorage(dash, false,
                dir.getAbsolutePath());
        }

        // save the user's response to the cloud storage question
        if (IS_CLOUD_STORAGE != null)
            InternalSettings.set(CLOUD_SETTING, IS_CLOUD_STORAGE.toString());
    }


    private static boolean isEditableSession() {
        // if we cannot make any changes, abort
        if (Settings.isReadOnly())
            return false;

        // if we are in quick launch mode, abort
        if (CompressedInstanceLauncher.isRunningFromCompressedData())
            return false;

        return true;
    }

    private static boolean mightBeCloudDir(File dir) {
        // if the directory doesn't exist, abort
        if (dir == null || !dir.isDirectory())
            return false;

        // if the directory is already bundled, abort
        if (FileBundleUtils.isBundledDir(dir))
            return false;

        // test whether the directory looks like a cloud storage dir
        SyncClientMappings.initialize(dir);
        String path = dir.getAbsolutePath();
        String enc = FolderMappingManager.getInstance().encodePath(path);
        return FolderMappingManager.isEncodedPath(enc);
    }

    private static boolean isEmptyTeamDashboard(File dir) {
        return true // return true if:

                // this is a team dashboard
                && fileContains(dir, "pspdash.ini", "datasetMode=team")

                // with no projects listed in the state file
                && !fileContains(dir, "state", "templateID")

                // and no value for the cloud setting flag
                && !fileContains(dir, "pspdash.ini", CLOUD_SETTING);
    }

    private static boolean fileContains(File dir, String filename,
            String text) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(
                    new FileInputStream(new File(dir, filename)), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains(text))
                    return true;
            }
            return false;

        } catch (Exception e) {
            return false;
        } finally {
            FileUtils.safelyClose(in);
        }
    }

    private static Boolean askIfCloudStorage(Component parent, boolean isTeam,
            String path) {
        ResourceBundle res = ResourceBundle
                .getBundle("Templates.resources.ProcessDashboard");
        ResourceBundle res2 = ResourceBundle
                .getBundle("Templates.resources.(Resources)");
        String title = res.getString("Cloud.Query.Title");
        String header = res.getString("Cloud.Query.Header." //
                + (isTeam ? "Team" : "Personal"));
        Object footer = res.getString("Cloud.Query.Footer").split("\n");
        Object message = new Object[] { header, "      " + path, " ", footer,
                new JOptionPaneTweaker.ToFront() };
        Object[] options = new Object[] { //
                res2.getString("Yes"), //
                res2.getString("No"), //
                res.getString("Cloud.Query.Not_Sure") };

        int userChoice = JOptionPane.showOptionDialog(parent, message, title,
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
            options, options[2]);
        switch (userChoice) {
            case 0: return Boolean.TRUE;
            case 1: return Boolean.FALSE;
            default: return null;
        }
    }

    private static final String CLOUD_SETTING = Settings.CLOUD_STORAGE_FLAG;

}
