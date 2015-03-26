// Copyright (C) 2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.quicklauncher;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;

import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.templates.TemplateLoader;

public class WbsZipInstanceLauncher extends DashboardInstance {

    private File wbsZipFile;

    public WbsZipInstanceLauncher(File wbsZipFile) {
        this.wbsZipFile = wbsZipFile;
        setDisplay(wbsZipFile.getAbsolutePath());
    }

    @Override
    public void launch(DashboardProcessFactory processFactory) {
        launchApp(processFactory, getJvmArgs(), wbsZipFile);
    }

    private List getJvmArgs() {
        String customColumns = getCustomColumnSpecURLs();
        if (customColumns == null)
            return null;
        else
            return Collections.singletonList("-D" + CUSTOM_COL_PROP + "="
                    + customColumns);
    }

    /**
     * Retrieve a list of custom column specifications to pass along to the WBS
     * Editor process.
     * 
     * This method has been copied from the OpenWBSEditor class, and must be
     * kept in sync with the logic there.
     * 
     * Note that this method will only return a value when running within the
     * Process Dashboard. If the Quick Launcher was run in standalone mode, this
     * method will return null. The implication is that, for now, people will
     * need to use the "C > Tools > Open Dataset" feature to open WBS files if
     * they would like to view/edit custom column values in a WBS ZIP.
     */
    private String getCustomColumnSpecURLs() {
        List<Element> configElements = ExtensionManager
                .getXmlConfigurationElements("customWbsColumns");
        if (configElements == null || configElements.isEmpty())
            return null;

        StringBuffer result = new StringBuffer();
        for (Element xml : configElements) {
            String uri = xml.getAttribute("specFile");
            URL url = TemplateLoader.resolveURL(uri);
            if (url != null)
                result.append(" ").append(url.toString());
        }
        if (result.length() > 0)
            return result.substring(1);
        else
            return null;
    }

    // this constant must be kept in sync with the constant
    // teamdash.wbs.columns.CustomColumnManager.SYS_PROP_NAME
    private static final String CUSTOM_COL_PROP = "teamdash.wbs.customColumnURLs";


    @Override
    protected Process createProcess(DashboardProcessFactory processFactory,
            File launchTarget, List extraVmArgs, List extraArgs)
            throws Exception {
        return processFactory.launchWBS(launchTarget, extraVmArgs, extraArgs);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WbsZipInstanceLauncher) {
            WbsZipInstanceLauncher that = (WbsZipInstanceLauncher) obj;
            return this.wbsZipFile.equals(that.wbsZipFile);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return wbsZipFile.hashCode();
    }

}
