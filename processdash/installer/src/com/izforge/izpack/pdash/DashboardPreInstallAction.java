package com.izforge.izpack.pdash;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.prefs.Preferences;

import com.izforge.izpack.Pack;
import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.PanelAction;
import com.izforge.izpack.installer.PanelActionConfiguration;
import com.izforge.izpack.installer.ScriptParser;
import com.izforge.izpack.util.AbstractUIHandler;
import com.izforge.izpack.util.os.RegistryDefaultHandler;
import com.izforge.izpack.util.os.RegistryHandler;
import com.sun.management.OperatingSystemMXBean;

public class DashboardPreInstallAction implements PanelAction,
        DashboardInstallConstants {

    private AutomatedInstallData installdata;

    public void initialize(PanelActionConfiguration configuration) {}

    public void executeAction(AutomatedInstallData installdata,
            AbstractUIHandler handler) {
        this.installdata = installdata;

        setMaxMemory();
        setMacApplicationDir();
        setUninstallationRegistryName();

        // the handler will be null for an automated install. When the
        // installation is automated, we don't want to change the user's preset
        // values.
        if (handler != null) {
            hideAndPreselectPacks();
            setDefaultInstallDir();
            setShortcutPrefs();
        }
    }

    private void setMaxMemory() {
        // read the max memory preference from the external configuration, and
        // set it in the installation data
        String maxMemSetting = ExternalConfiguration.getConfig().getProperty(
            "max.memory");
        int maxMem;
        try {
            maxMem = Integer.parseInt(maxMemSetting);
        } catch (Exception exc) {
            maxMem = 800; // default value
        }
        maxMem = limitToHalfOfSystemMemory(maxMem);
        installdata.setVariable(MAX_MEMORY, Integer.toString(maxMem));
    }

    /**
     * If we specify an Xmx memory ceiling that is larger than the amount of
     * space available to the OS, the JVM will display an error dialog and
     * fail to launch. Perform a check to see how much space is available and
     * limit our application to half of available memory.
     */
    private int limitToHalfOfSystemMemory(int maxMem) {
        try {
            Object bean = ManagementFactory.getOperatingSystemMXBean();
            OperatingSystemMXBean osMxBean = (OperatingSystemMXBean) bean;
            long systemMemoryBytes = osMxBean.getTotalPhysicalMemorySize();
            long systemMemoryMegabytes = systemMemoryBytes >> 20;
            long halfOfMemory = systemMemoryMegabytes / 2;
            if (maxMem > halfOfMemory)
                maxMem = (int) halfOfMemory;

            // Systems today may have a lot of memory - 4 or 6GB - and half
            // of that value will still be excessive.  In addition, requesting
            // half of 4GB would result in a process that exceeds the memory
            // limitations of a 32-bit JVM.  Look at the type of system we are
            // running, and choose a more conservative limit as appropriate.
            if ("64".equals(System.getProperty("sun.arch.data.model")))
                maxMem = Math.min(maxMem, 2000);
            else
                maxMem = Math.min(maxMem, 1400);

            return maxMem;
        } catch (Throwable t) {
            // If we are not running in a Sun JVM, the code above will fail.
            // In that case, use a conservative threshhold.
            return Math.min(maxMem, 500);
        }
    }

    private void setMacApplicationDir() {
        // read the Mac OS X Application directory from the external
        // configuration,
        // or choose an appropriate default
        String macAppDir = ExternalConfiguration.getConfig().getProperty(
            "mac-os-x.applicationDir", "/Applications");
        if (macAppDir.startsWith("~"))
            macAppDir = System.getProperty("user.home")
                    + macAppDir.substring(1);
        installdata.setVariable(MAC_APPLICATION_DIR, macAppDir);
    }

    private void setUninstallationRegistryName() {
        // By default, the uninstaller will use the version number of the
        // application when registering for uninstallation.  But we allow
        // people to install the dashboard on top of itself over and over.
        // We wouldn't want the user to see multiple uninstallation options
        // in the "Add/Remove Programs" dialog (one for each version), because
        // they can only uninstall the application once anyway.
        RegistryHandler rh = RegistryDefaultHandler.getInstance();
        if (rh != null) {
            String name = installdata.getVariable(ScriptParser.APP_NAME);
            rh.setUninstallName(name);
        }
    }

    /**
     * Tweak the list of available and preselected packs based on the external
     * configuration, if it is present.
     */
    private void hideAndPreselectPacks() {

        List selPacks = installdata.selectedPacks;
        Iterator pack_it = installdata.availablePacks.iterator();
        while (pack_it.hasNext()) {
            Pack pack = (Pack) pack_it.next();
            String packID = pack.id;
            String hidePropName = "pack." + packID + ".hidden";
            if (isConfigTrue(hidePropName) && !pack.required) {
                selPacks.remove(pack);
                pack_it.remove();
                continue;
            }

            String preselPropName = "pack." + packID + ".preselected";
            if (isConfigTrue(preselPropName) && !selPacks.contains(pack)) {
                pack.preselected = true;
                selPacks.add(pack);
            } else if (isConfigFalse(preselPropName) && !pack.required) {
                pack.preselected = false;
                selPacks.remove(pack);
            }
        }
    }

    private boolean isConfigTrue(String flag) {
        String val = ExternalConfiguration.getConfig().getProperty(flag);
        return propMatches(val, "yes", "true")
                || (testInstallSpec(val) == Boolean.TRUE);
    }

    private boolean isConfigFalse(String flag) {
        String val = ExternalConfiguration.getConfig().getProperty(flag);
        return propMatches(val, "no", "false")
                || (testInstallSpec(val) == Boolean.FALSE);
    }

    private boolean propMatches(String prop, String valA, String valB) {
        return (valA.equalsIgnoreCase(prop) || valB.equalsIgnoreCase(prop));
    }

    private Boolean testInstallSpec(String spec) {
        if (spec == null || (spec = spec.trim()).length() == 0)
            return null;
        else if (spec.startsWith("if-installed"))
            return atLeastOneFileIsInstalled(spec.substring(12));
        else if (spec.startsWith("if-not-installed"))
            return atLeastOneFileIsInstalled(spec.substring(16)) == false;
        else
            return null;
    }

    private boolean atLeastOneFileIsInstalled(String spec) {
        String instDirName = getDefaultInstallDir();
        if (instDirName == null)
            return false;

        File instDir = new File(instDirName);
        if (!instDir.isDirectory())
            return false;

        for (String name : spec.trim().split(" +")) {
            File file = new File(instDir, name);
            if (file.isFile())
                return true;
        }
        return false;
    }


    private void setDefaultInstallDir() {
        String defaultDir = getDefaultInstallDir();
        if (defaultDir != null)
            installdata.setVariable(ScriptParser.INSTALL_PATH, defaultDir);
    }

    private String getDefaultInstallDir() {
        // We check to see if user settings exists for the default dir.
        Preferences prefs = Preferences.userRoot().node(USER_VALUES_PREFS_NODE);
        String userInstallPath = prefs.get(ScriptParser.INSTALL_PATH, null);
        if (userInstallPath != null)
            return userInstallPath;

        Properties p = ExternalConfiguration.getConfig();
        String extDefault = p.getProperty("dir.install.default");
        if (extDefault != null)
            return extDefault;

        return null;
    }

    private void setShortcutPrefs() {
        // create shortcuts on the desktop by default
        installdata.setVariable("DesktopShortcutCheckboxEnabled", "true");
    }

}
