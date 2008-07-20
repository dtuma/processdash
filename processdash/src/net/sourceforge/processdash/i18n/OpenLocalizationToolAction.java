// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.util.RuntimeUtils;

public class OpenLocalizationToolAction extends AbstractAction {

    // The name l10n tool main class
    // Would this be the right place to specify the jar name or should I put this in some
    // configuration file ?
    public static final String MAIN_CLASS = "net.sourceforge.processdash.i18n.Main";

    // The property that indicates which resource to translate
    // Warning : This constant must be syschronized with the
    // one in net.sourceforge.processdash.i18n.Main
    public static final String PROPERTY_RESOURCE_TO_TRANSLATE = "translate.resource";

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

        cmd.add("-D" + PROPERTY_RESOURCE_TO_TRANSLATE + "=" + dashboardJar);

        // The url that is used to accesss the help topics
        String helpURL = Browser.mapURL(HELP_URL);
        cmd.add("-D" + PROPERTY_HELP_URL + "=" + helpURL);

        // set a reasonable application menu name on Mac OS X
        if ("Mac OS X".equalsIgnoreCase(System.getProperty("os.name")))
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

}
