// Copyright (C) 2002-2020 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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


package teamdash.wbs;

import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;

import net.sourceforge.processdash.i18n.Resources;

public class ProxyLibraryEditor extends AbstractLibraryEditor {

    public static final String ORG_PROXIES_SYS_PROP = //
            "teamdash.wbs.orgProxyURLs";

    private static final Resources RESOURCES = Resources
            .getDashBundle("WBSEditor.Proxy_Library");

    private static final String FILENAME_EXTENSION = ".estxml";

    public ProxyLibraryEditor(TeamProject teamProject, JFrame parent,
            Mode mode) throws UserCancelledException {
        super(teamProject, parent, mode, RESOURCES, FILENAME_EXTENSION,
                ORG_PROXIES_SYS_PROP);
    }

    @Override
    protected void openModels() {
        SizeMetricsWBSModel.removeMetricIDAttrs(library);
        SizeMetricsWBSModel sizeMetrics = new SizeMetricsWBSModel();
        libraryModel = new ProxyDataModel((ProxyWBSModel) library, sizeMetrics);

        projectWbs = new ProxyWBSModel();
        projectWbs.copyFrom(teamProject.getProxies());
        SizeMetricsWBSModel.removeMetricIDAttrs(projectWbs);
        projectModel = new ProxyDataModel((ProxyWBSModel) this.projectWbs,
            sizeMetrics);
    }

    @Override
    protected WBSJTable buildJTable(DataTableModel model) {
        return new ProxyJTable((ProxyDataModel) model);
    }

    @Override
    protected WBSLibrary openLibraryFile(File file) throws IOException {
        return new WBSLibrary.Proxies(file);
    }

    @Override
    protected WBSLibrary openOrgLibrary(String[] urls) {
        return new WBSLibrary.Proxies(urls, teamProject.getTeamProcess());
    }

    @Override
    protected WBSLibrary openNewLibrary(File file) throws IOException {
        return new WBSLibrary.Proxies(file, teamProject.getTeamProcess());
    }

    @Override
    public boolean doImport() {
        SizeMetricsWBSModel.removeMetricIDAttrs(projectWbs);
        teamProject.getProxies().copyFrom(projectWbs);
        return true;
    }

    @Override
    public boolean doExport() {
        SizeMetricsWBSModel.removeMetricIDAttrs(library);
        return super.doExport();
    }

    public static boolean orgAssetsAreAvailable(TeamProcess process) {
        try {
            String sysprop = System.getProperty(ORG_PROXIES_SYS_PROP);
            if (sysprop == null)
                return false;
            String[] urls = sysprop.trim().split("\\s+");
            return new WBSLibrary.Proxies(urls, process).isNotEmpty();
        } catch (Exception e) {
            return false;
        }
    }

}
