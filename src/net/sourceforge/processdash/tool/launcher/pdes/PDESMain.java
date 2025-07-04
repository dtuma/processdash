// Copyright (C) 2018-2025 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.launcher.pdes;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

import net.sourceforge.processdash.RecentDatasets;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.bridge.impl.HttpAuthenticator;
import net.sourceforge.processdash.tool.launcher.LaunchableDataset;
import net.sourceforge.processdash.tool.launcher.jnlp.JnlpDatasetLauncher;
import net.sourceforge.processdash.tool.launcher.jnlp.JnlpUtil;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RuntimeUtils;

public class PDESMain {

    public static void main(String[] args) {
        // if a single arg was provided, pass it to the launcher.
        if (JnlpUtil.isSingleJnlpArg(args)) {
            JnlpDatasetLauncher.launch(args[0]);
            return;
        }

        // initialize the authentication logic
        Resources r = Resources.getDashBundle("Launcher");
        String title = r.getString("Process_Dashboard");
        HttpAuthenticator.maybeInitialize(title);

        // get a list of PDES servers we know about
        List<String> servers = getKnownServers();

        // build an object for selecting datasets
        PDESDatasetChooser chooser = new PDESDatasetChooser(servers,
                System.getProperty(FORCED_SERVER_URL) == null);

        // if the user cancelled one of the initial prompts, abort
        if (chooser.userCancelledStartup())
            return;

        // if we found exactly one server and one dataset, launch that dataset.
        // this is a convenience for the average user, who doesn't need to
        // choose between various datasets and/or various servers.
        LaunchableDataset single = chooser.getSingleResult();
        if (single != null) {
            JnlpDatasetLauncher.launch(single.getLocation());
            return;
        }

        // display the chooser
        title = r.getString("Window_Title");
        int userChoice = JOptionPane.showConfirmDialog(null, chooser, title,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        // if the user clicked OK, launch their selected dataset
        if (userChoice == JOptionPane.OK_OPTION) {
            LaunchableDataset d = chooser.getSelectedDataset();
            if (d != null)
                JnlpDatasetLauncher.launch(d.getLocation());
        }
    }

    private static List<String> getKnownServers() {
        // if a forced URL is configured, use it only
        String forcedUrl = System.getProperty(FORCED_SERVER_URL);
        if (forcedUrl != null)
            return Collections.singletonList(forcedUrl);

        LinkedHashSet<String> result = new LinkedHashSet<String>();

        // if a default server is configured, add it to the result
        String defaultUrl = PDESUtil.getPdesPrefs()
                .get(PDESUtil.DEFAULT_SERVER_PREF, null);
        if (defaultUrl != null)
            result.add(PDESUtil.getBaseUrl(defaultUrl));

        // scan recent datasets and add them to the list
        List<Preferences> recent = RecentDatasets.list().getEntries(
            RecentDatasets.LOC_TYPE, RecentDatasets.LOC_TYPE_HTTP);
        for (Preferences dataset : recent) {
            String url = dataset.get(RecentDatasets.LOCATION, null);
            if (url != null && url.startsWith("http"))
                result.add(PDESUtil.getBaseUrl(url));
        }

        // if a URL has been configured in a bundled file, use it
        String bundleUrl = getUrlFromMacApplicationBundle();
        if (bundleUrl != null)
            result.add(PDESUtil.getBaseUrl(bundleUrl));

        // build a list of the resulting server URLs
        return new ArrayList<String>(result);
    }

    private static String getUrlFromMacApplicationBundle() {
        if (!MacGUIUtils.isMacOSX())
            return null;

        try {
            // find the JAR file containing these classes
            File self = RuntimeUtils.getClasspathFile(PDESMain.class);
            if (self == null)
                return null;

            // look in the grandparent dir for a server pref file
            File dir = self.getParentFile().getParentFile();
            File f = new File(dir, PDESUtil.DEFAULT_SERVER_PREF + ".txt");
            if (!f.isFile())
                return null;

            // read the data from the file
            byte[] data = FileUtils.slurpContents(new FileInputStream(f), true);
            String url = new String(data, "UTF-8").trim();
            if (url.startsWith("http"))
                return url;

        } catch (Exception e) {
        }

        // abort if there were problems or if the file did not contain a URL
        return null;
    }

    public static final String FORCED_SERVER_URL = PDESMain.class.getPackage()
            .getName() + ".forcedUrl";

}
