// Copyright (C) 2007-2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.i18n;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import net.sourceforge.processdash.templates.DashPackage;
import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;
import net.sourceforge.processdash.util.RuntimeUtils;
import net.sourceforge.processdash.util.StringUtils;

public class OpenLocalizationToolAction extends AbstractAction {

    // The name l10n tool main class
    public static final String MAIN_CLASS = "net.sourceforge.processdash.i18n.Main";

    // The property that indicates which resources to translate
    // Warning : This constant must be syschronized with the
    // one in net.sourceforge.processdash.i18n.Main
    public static final String PROPERTY_RESOURCES_TO_TRANSLATE = "translate.resource";

    // If there are multiple resources in PROPERTY_RESOURCES_TO_TRANSLATE, they are
    //  separated by a ';'
    // Warning : This constant must be syschronized with the
    // one in net.sourceforge.processdash.i18n.Main
    public static final String RESOURCE_SEPARATOR = ";";

    // The property that indicates the directory to place new files
    // Warning : This constant must be syschronized with the
    // one in net.sourceforge.processdash.i18n.Main
    public static final String PROPERTY_RESOURCES_DEST_DIR = "translate.destDir";

    // The property that indicates what URL to navigate to to access the help topic
    // Warning : This constant must be syschronized with the
    // one in net.sourceforge.processdash.i18n.Main
    public static final String PROPERTY_HELP_URL = "help.url";

    // This URL will be mapped to be a full URL used to access the help topic
    public static final String HELP_URL = "/control/showHelp.class?topicID=LocalizationTool";

    private static final Resources resources = Resources.getDashBundle("ProcessDashboard");

    String dashboardJar;


    public OpenLocalizationToolAction() {
        super(resources.getString("Menu.Localization_Tool"));

        // if the current locale is English, don't display the menu item
        // for the localization tool.
        if (Locale.getDefault().getLanguage().equals("en"))
            throw new ExtensionManager.DisabledExtensionException();

        // We want the dashboard jar name. To get it, we get the classpath of a
        // class in that jar (here, ProcessDashboard).
        Class processdashClass = net.sourceforge.processdash.ProcessDashboard.class;
        File cp = RuntimeUtils.getClasspathFile(processdashClass);

        // If the localization JAR file is unsigned and we are running in a
        // security manager, we will be unable to find a classpath.  Indicate
        // this with an appropriate exception
        if (cp == null)
            throw new SecurityException("Unable to calculate classpath");

        // The localization tool requires the dashboard to be running from a
        // JAR file.  If the dashboard is running from unpackaged class files
        // instead, disable the menu item.
        if (cp.isFile() && cp.getName().toLowerCase().endsWith(".jar"))
            dashboardJar = cp.getAbsolutePath();
        else
            throw new ExtensionManager.DisabledExtensionException();
    }

    public void actionPerformed(ActionEvent e) {
        List<String> cmd = new ArrayList<String>();

        cmd.add(RuntimeUtils.getJreExecutable());

        String[] propagatedArgs = RuntimeUtils.getPropagatedJvmArgs();
        cmd.addAll(Arrays.asList (propagatedArgs));

        String destDir = getDestDir();
        cmd.add("-D" + PROPERTY_RESOURCES_DEST_DIR + "=" + destDir);

        cmd.add("-D" + PROPERTY_RESOURCES_TO_TRANSLATE + "=" + getPackages(destDir));

        // The url that is used to accesss the help topics
        String helpURL = Browser.mapURL(HELP_URL);
        cmd.add("-D" + PROPERTY_HELP_URL + "=" + helpURL);

        // set a reasonable application menu name on Mac OS X
        if (MacGUIUtils.isMacOSX())
            cmd.add("-Xdock:name="
                    + resources.getString("Menu.Localization_Tool"));

        // Classpath configuration
        cmd.add("-cp");
        cmd.add(RuntimeUtils.getClasspathFile(getClass()).getAbsolutePath());

        // The actual class we want to launch.
        cmd.add(MAIN_CLASS);

        String[] cmdLine = (String[]) cmd.toArray(new String[cmd.size()]);

        try {
            final Process proc = Runtime.getRuntime().exec(cmdLine);

            // Set up a thread to consume output (copying it to our stdout/stderr)
            new Thread() {
                public void run() {
                    RuntimeUtils.consumeOutput(proc, System.out, System.err);
                }
            }.start();
        }
        catch (IOException ex) {
            String message = resources.format("Errors.Cant_Launch_L10n_Tool_Dialog_FMT",
                                              ex.getLocalizedMessage());
            String title = resources.getString("Errors.Cant_Launch_L10n_Tool_Dialog.Title");

            JOptionPane.showMessageDialog(null, message, title,
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Returns the directory where new translations should be saved
     */
    private String getDestDir() {
        File templatesDir = TemplateLoader.getApplicationTemplateDir();
        templatesDir.mkdirs();
        if (templatesDir.isDirectory())
            return templatesDir.getAbsolutePath();
        else
            return "";
    }

    /**
     * Returns a String containing all packages we want to translate,
     *  starting with the dashboardJar
     */
    private String getPackages(String appTemplateDirName) {
        // add the application JAR file to the list, in position zero.
        StringBuffer packageFilenames = new StringBuffer(dashboardJar);

        // scan other dashboard add-ons to see if they contain localizable
        // resources.
        List packages = TemplateLoader.getPackages();
        for (Object p : packages) {
            DashPackage pkg = (DashPackage) p;
            String filename = pkg.filename;

            if (filename != null && !filename.equals(dashboardJar) && pkg.localizable)
                packageFilenames.append(RESOURCE_SEPARATOR + filename);
        }

        // Previously saved translations are not packaged as "official" add-ons.
        // They do not contain a manifest, so they will not be picked up as
        // packages by the loop above.  Look for these files and include them.
        if (StringUtils.hasValue(appTemplateDirName)) {
            File destDir = new File(appTemplateDirName);
            File[] appTemplateFiles = destDir.listFiles();
            if (appTemplateFiles != null) {
                for (File f : appTemplateFiles) {
                    String name = f.getName().toLowerCase();
                    if (name.startsWith("pspdash_") && name.endsWith(".jar"))
                        packageFilenames.append(RESOURCE_SEPARATOR + f.getPath());
                }
            }
        }

        return packageFilenames.toString();
    }

}
