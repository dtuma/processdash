package com.izforge.izpack.pdash;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;

import com.izforge.izpack.event.SimpleInstallerListener;
import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.ScriptParser;
import com.izforge.izpack.installer.UninstallData;
import com.izforge.izpack.util.AbstractUIProgressHandler;
import com.izforge.izpack.util.OsVersion;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.ui.macosx.MacDataCleanup;

public class DashboardPostInstallAction extends SimpleInstallerListener
        implements DashboardInstallConstants {

    @Override
    public void afterPacks(AutomatedInstallData idata,
            AbstractUIProgressHandler handler) throws Exception {
        saveUserValues();
        filterUninstallableFiles();
        maybeCleanupMacData();
        mergeSettings();
    }



    /**
     * Write user-selected values into the Java preferences, so future
     * installers can display the remembered default.
     */
    private void saveUserValues() {
        Preferences prefs = Preferences.userRoot().node(USER_VALUES_PREFS_NODE);
        for (int i = 0; i < USER_VALUE_ITEMS.length; i++) {
            maybeSaveUserValue(prefs, USER_VALUE_ITEMS[i]);
        }
    }

    private void maybeSaveUserValue(Preferences prefs, String key) {
        String value = getInstalldata().getVariable(key);
        if (value != null)
            prefs.put(key, value);
    }

    private static final String[] USER_VALUE_ITEMS = {
            ScriptParser.INSTALL_PATH, DATA_PATH, TEAM_DATA_PATH };



    /**
     * The installer may write files into the user's data directory, or the
     * team data directory.  These files should NOT be uninstalled, so we
     * remove them from the uninstallable files list.
     */
    private void filterUninstallableFiles() {
        File dataDir = makeWritable(getUserDirectoryForVariable(DATA_PATH));
        File teamDataDir = null;
        if ("true".equals(getInstalldata().getVariable(TEAM_DATA_LOCAL_FLAG)))
            teamDataDir = makeWritable(getUserDirectoryForVariable(TEAM_DATA_PATH));

        List<String> uninstallableFiles = UninstallData.getInstance()
                .getUninstalableFilesList();
        for (Iterator i = uninstallableFiles.iterator(); i.hasNext();) {
            File oneFile = new File((String) i.next());
            if (fileIsInUserDataArea(oneFile, dataDir, teamDataDir)) {
                // remove the file from the list of uninstallable files.
                i.remove();
                // also, make the file globally writable;  if the user ran
                // the installer as an administrator, this will make it
                // possible for the dashboard to edit the file when it runs
                // as a regular user.
                makeWritable(oneFile);
            }
        }
    }

    private File getUserDirectoryForVariable(String varName) {
        String value = getInstalldata().getVariable(varName);
        if (value == null || value.trim().length() == 0)
            return null;

        return new File(value.trim());
    }

    private boolean fileIsInUserDataArea(File f, File... dataDirs) {
        if (UNIX_STARTUP_SCRIPT.equals(f.getName()))
            return true;

        File dir = f.getParentFile();
        while (dir != null) {
            for (File oneDataDir : dataDirs) {
                if (dirEq(dir, oneDataDir))
                    return true;
            }
            dir = dir.getParentFile();
        }

        return false;
    }

    private boolean dirEq(File a, File b) {
        if (a == null || b == null)
            return false;
        return a.equals(b);
    }

    private File makeWritable(File f) {
        try {
            f.setWritable(true, false);
        } catch (Throwable t) {
            // the File.setWritable method was added in Java 1.6.  If this
            // installer is run on Java 1.5, it will throw an exception.
            // ignore it, and continue on with other installation tasks.
        }
        return f;
    }


    /**
     * On Mac OS X, relocate data files that were placed in a bad location
     */
    private void maybeCleanupMacData() {
        if (OsVersion.IS_MAC) {
            String dataPath = getInstalldata().getVariable(DATA_PATH);
            MacDataCleanup.main(new String[] { dataPath });
        }
    }



    /**
     * Look in the custom-install.ini files to see if they request changes
     * to user settings, and apply those settings to the user's pspdash.ini
     * file.
     */
    private void mergeSettings() {
        String dataPath = getInstalldata().getVariable(DATA_PATH);
        String url1 = ExternalConfiguration.getURL1();
        String url2 = ExternalConfiguration.getURL2();
        InternalSettings.main(new String[] {dataPath, url1, url2});
    }

}
