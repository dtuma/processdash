// Copyright (C) 2009-2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.launcher.jnlp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.ExceptionDialog;
import net.sourceforge.processdash.util.RuntimeUtils;

import com.tuma_solutions.teamserver.jnlp.client.JnlpClientConstants;

public class JnlpDatasetLauncher implements JnlpClientConstants {

    static Resources res = Resources.getDashBundle("Launcher.Jnlp");

    public static void main(String[] args) {
        launch(args[0]);
    }

    public static void launch(String source) {
        try {
            // launch the dataset for the given source
            new JnlpDatasetLauncher(source);

        } catch (Exception e) {
            // display a dialog for unexpected exceptions
            ExceptionDialog.showWithSubdialog(null,
                getRes("Launch_Error.Title"),
                getResArr("Launch_Error.Other_Error"), e);
            System.exit(1);
        }
    }


    private Element xml;

    private AssetManager assetManager;

    private File distrDir;

    private String dataLocation;

    private String appName;

    private Map<String, String> systemProperties;

    private List<String> argv;

    public JnlpDatasetLauncher(String source) throws Exception {
        getJnlpXml(source);
        initializeFields();
        parseArguments();
        retrieveAssetsAndLaunchProfile();
        finalizePropertiesAndCommandLine();
        launchApp();
    }


    /**
     * Retrieve the JNLP file named by the given source, which could be a URL or
     * a filename.
     */
    private void getJnlpXml(String source) throws IOException {
        if (source.toLowerCase().startsWith("http")) {
            getJnlpXmlFromURL(source);
        } else {
            getJnlpXmlFromFile(source);
        }
    }

    private void getJnlpXmlFromURL(String url) throws IOException {
        try {
            xml = JnlpFileRetriever.open(url);

        } catch (FileNotFoundException fnfe) {
            showStartupErrorAndExit(getRes("Launch_Error.Cannot_Reach_Server"),
                "      " + url, " ", getResArr("Network_Error_Footer"));
        }
    }

    private void getJnlpXmlFromFile(String file) throws IOException {
        try {
            xml = JnlpFileRetriever.open(new File(file));

        } catch (FileNotFoundException fnfe) {
            showStartupErrorAndExit(getRes("Launch_Error.Cannot_Read_File"),
                "      " + file);
        }
    }


    /**
     * set starting values for fields in this object
     */
    private void initializeFields() throws IOException {
        assetManager = new AssetManager();
        dataLocation = "unspecified";
        systemProperties = new HashMap<String, String>();
        argv = new ArrayList<String>();
    }


    /**
     * Read the command line arguments in the JNLP file
     */
    private void parseArguments() {
        List<Element> args = getTags(true, "application-desc", "argument");
        for (Element argTag : args) {
            String arg = argTag.getTextContent();

            if (arg.startsWith(JNLP_ARG_PREFIX)) {
                handleJnlpArg(arg);

            } else if (arg.startsWith(MEMORY_ARG_PREFIX)) {
                // discard; we are running in the same process

            } else if (JAR_ARG.equals(arg)) {
                // discard; we are creating our own classloader

            } else if (arg.contains(SECURITY_MANAGER_SETTING_NAME)) {
                // discard; don't let JNLP authors disable the security manager

            } else if (arg.startsWith("-D")) {
                handleSystemProperty(arg);

            } else {
                handleCommandLineArg(arg);
            }
        }
    }

    private void handleJnlpArg(String arg) {
        if (USE_INSTALLED_DISTRIB_FLAG.equals(arg)) {
            distrDir = DistributionManager.getInstalledDistributionDir();
            if (distrDir == null)
                showStartupErrorAndExitT(getRes("Not_Installed.Title"),
                    getResArr("Not_Installed.Message"));

        } else if (arg.startsWith(NAME_PREFIX)) {
            appName = arg.substring(NAME_PREFIX.length());

        } else if (arg.startsWith(PROCESS_ASSET_PREFIX)) {
            String assetSpec = arg.substring(PROCESS_ASSET_PREFIX.length());
            assetManager.addAsset(assetSpec);

        } else if (arg.startsWith(HIST_TIME_PREFIX)) {
            String histTime = arg.substring(HIST_TIME_PREFIX.length());
            setupHistTime(histTime);

        } else {
            // unrecognized or deprecated jnlp arg - ignore
        }
    }

    private void setupHistTime(String histTime) {
        try {
            Date d = new SimpleDateFormat("yyyyMMddHHmm").parse(histTime);
            systemProperties.put(HIST_TIME_PROP, Long.toString(d.getTime()));
        } catch (Exception e) {
        }
    }

    private void handleSystemProperty(String arg) {
        int eqPos = arg.indexOf('=');
        String key = arg.substring(2, eqPos);
        String value = arg.substring(eqPos + 1);
        systemProperties.put(key, value);
    }

    private void handleCommandLineArg(String arg) {
        if (arg.startsWith(DATA_LOCATION_PREFIX))
            dataLocation = arg.substring(DATA_LOCATION_PREFIX.length());

        argv.add(arg);
    }


    /**
     * Download any resources needed to launch the app
     */
    private void retrieveAssetsAndLaunchProfile() throws IOException {
        // if we are not using an installed distribution, check the JNLP data
        // for the launch profile we should use, and try finding a previously
        // downloaded copy of it.
        if (distrDir == null) {
            String launchProfileUrl = getTag(true, "resources", "extension")
                    .getAttribute("href");
            distrDir = DistributionManager
                    .findExistingDistribution(launchProfileUrl);
            if (distrDir == null)
                assetManager.addLaunchProfile(launchProfileUrl);
        }

        // retrieve any missing assets and launch profile as needed
        assetManager.downloadMissingAssets(dataLocation);

        // get the launch profile the asset manager retrieved for us
        if (distrDir == null) {
            distrDir = assetManager.getDownloadedLaunchProfileDir();
        }
    }


    /**
     * Store final system properties and command line args
     */
    private void finalizePropertiesAndCommandLine() {
        // record the application name
        if (appName != null)
            argv.add(appName);

        // record any process assets that are in use
        List<String> assetList = assetManager.getAssetDirectoryList();
        for (int i = assetList.size(); i-- > 0;) {
            systemProperties.put(TEMPLATE_DIR_PROP_PREFIX + (i + 1),
                assetList.get(i));
        }

        // record the "usage log file" setting
        File usageLog = new File(distrDir,
                DistributionManager.USAGE_LOG_FILENAME);
        systemProperties.put(USAGE_LOG_PROP,
            usageLog.getAbsolutePath() + assetManager.getUsageFileList());
    }


    /**
     * Launch the application, using the arguments and distribution we've set
     */
    private void launchApp() throws Exception {
        // store all of the system property settings
        for (Entry<String, String> e : systemProperties.entrySet()) {
            System.setProperty(e.getKey(), e.getValue());
        }

        // get the arguments that should be passed to the main() function
        String[] args = argv.toArray(new String[argv.size()]);

        // create a classloader and load the main ProcessDashboard class
        File targetJarFile = new File(distrDir,
                DistributionManager.TARGET_JARFILE);
        ClassLoader cl = getAppClassLoader(targetJarFile);
        Class clazz = Class.forName(PDASH_MAIN_CLASS, true, cl);
        Method main = clazz.getMethod("main", new Class[] { String[].class });

        // reflectively invoke main method to start the application
        main.invoke(clazz, new Object[] { args });
    }

    private ClassLoader getAppClassLoader(File targetJarFile)
            throws IOException {
        // see if we're already running from the indicated JAR. If so, there
        // is no need to create a new class loader.
        File selfClasspath = RuntimeUtils.getClasspathFile(getClass());
        if (targetJarFile.equals(selfClasspath))
            return getClass().getClassLoader();

        // make a new class loader to read data from the given JAR
        ClassLoader cl = new URLClassLoader(
                new URL[] { targetJarFile.toURI().toURL() },
                JnlpDatasetLauncher.class.getClassLoader().getParent());
        Thread.currentThread().setContextClassLoader(cl);
        return cl;
    }



    //
    // Utility methods
    //

    private void showStartupErrorAndExit(Object... message) {
        String title = getRes("Launch_Error.Title");
        showStartupErrorAndExitT(title, message);
    }

    private void showStartupErrorAndExitT(String title, Object... message) {
        JOptionPane.showMessageDialog(null, message, title,
            JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    private static String getRes(String key) {
        return res.getString(key);
    }

    private static Object[] getResArr(String key) {
        return res.getStrings(key);
    }

    private Element getTag(boolean required, String... tagNames) {
        List<Element> tags = getTags(required, tagNames);
        return (tags.isEmpty() ? null : tags.get(0));
    }

    private List<Element> getTags(boolean required, String... tagNames) {
        List<Element> result = Collections.singletonList(xml);
        for (String name : tagNames) {
            List<Element> newResult = new ArrayList<Element>();
            for (Element e : result) {
                NodeList nl = e.getElementsByTagName(name);
                for (int i = 0; i < nl.getLength(); i++)
                    newResult.add((Element) nl.item(i));
            }
            if (!newResult.isEmpty())
                result = newResult;
            else if (!required)
                return Collections.EMPTY_LIST;
            else
                throw new BadJnlpFile(
                        "JNLP XML file does not contain tag " + tagNames);
        }
        return result;
    }

    private static class BadJnlpFile extends RuntimeException {
        public BadJnlpFile(String message) {
            super(message);
        }
    }



    private static final String TEMPLATE_DIR_PROP_PREFIX = "net.sourceforge.processdash.templates.TemplateLoader.extraDir.";

    private static final String USAGE_LOG_PROP = "net.sourceforge.processdash.util.UsageLogger.fileList";

    private static final String HIST_TIME_PROP = "net.sourceforge.processdash.tool.bridge.client.TeamServerSelector.effectiveDate";

    private static final String SECURITY_MANAGER_SETTING_NAME = "net.sourceforge.processdash.disableSecurityManager";

    private static final String PDASH_MAIN_CLASS = "net.sourceforge.processdash.ProcessDashboard";

}
