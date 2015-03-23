package com.izforge.izpack.pdash;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

import com.izforge.izpack.Pack;
import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.PanelAction;
import com.izforge.izpack.installer.PanelActionConfiguration;
import com.izforge.izpack.installer.ScriptParser;
import com.izforge.izpack.util.AbstractUIHandler;
import com.izforge.izpack.util.os.RegistryDefaultHandler;
import com.izforge.izpack.util.os.RegistryHandler;

import net.sourceforge.processdash.util.RuntimeUtils;

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

        // Quickly test to ensure we can launch a JVM with the given heap size
        AtomicInteger heapSize = new AtomicInteger(maxMem);
        try {
            String[] cmd = new String[] { "-cp",
                    RuntimeUtils.getClasspathFile(getClass()).getPath(),
                    getClass().getName() };
            RuntimeUtils.execWithAdaptiveHeapSize(cmd, null, null, heapSize);
        } catch (Throwable t) {
            System.err.println("Unexpected problem when testing heap size:");
            t.printStackTrace();
            heapSize.set(-1);
        }

        // if the heap test fails for any reason, pick a fallback value.
        if (heapSize.intValue() > 0) {
            System.out.println("Using validated heap size of " + heapSize + "m");
        } else {
            heapSize.set(800);
            System.out.println("Could not validate heap size; using 800m");
        }

        // store the memory setting for later use.
        installdata.setVariable(MAX_MEMORY, heapSize.toString());
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

    public static void main(String[] args) {
        System.out.println(DashboardPreInstallAction.class.getName() + " OK");
    }

}
