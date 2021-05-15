// Copyright (C) 2007-2021 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.log.ui.importer;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.ui.DefectLogEditor;
import net.sourceforge.processdash.util.StringUtils;

import org.w3c.dom.Element;

public class FormImporter extends AbstractAction {

    private DashboardContext dashboardContext;

    private Element configElement;

    private Resources resources;

    public FormImporter() {
        setEnabled(false);
    }

    public void setDashboardContext(DashboardContext dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    public void setConfigElement(Element xml, String attrName) {
        this.configElement = xml;

        // The legacy Code Collaborator implementation used by Intuit is no
        // longer relevant, and should be silently ignored.
        String id = xml.getAttribute("id");
        if ("Intuit.CodeCollaborator".equals(id)) {
            putValue(DefectLogEditor.IMPORT_ACTION_INVALID, "true");
            return;
        }

        this.resources = DefectImportForm.resources;
        String resourcesId = xml.getAttribute(RESOURCES_TAG);
        if (StringUtils.hasValue(resourcesId)) {
            this.resources = Resources.getDashBundle(resourcesId,
                this.resources);
        }

        String name = xml.getAttribute("displayName");
        if (!StringUtils.hasValue(name))
            name = resources.getString("Form.Label");
        putValue(NAME, name);
    }

    public void putValue(String key, Object newValue) {
        super.putValue(key, newValue);

        if (DefectLogEditor.IMPORT_ACTION_DEF_PATH.equals(key))
            setEnabled(StringUtils.hasValue((String) newValue));
    }

    public void actionPerformed(ActionEvent e) {
        try {
            doIt();
        } catch (AbortImport ai) {}
    }

    private void doIt() throws AbortImport {
        Component parentWindow = (Component) getValue(
            DefectLogEditor.IMPORT_ACTION_PARENT_WINDOW);

        String selectedPath = (String) getValue(DefectLogEditor.IMPORT_ACTION_SEL_PATH);
        if (!StringUtils.hasValue(selectedPath))
            AbortImport.showErrorAndAbort("Select_Hierarchy");

        String defectLogPath = (String) getValue(DefectLogEditor.IMPORT_ACTION_DEF_PATH);
        if (!StringUtils.hasValue(defectLogPath))
            AbortImport.showErrorAndAbort("No_Defect_Log", selectedPath);

        String displayName = (String) getValue(NAME);

        new DefectImportForm(dashboardContext, configElement, selectedPath,
                defectLogPath, displayName, resources, parentWindow);
    }

    private static final String RESOURCES_TAG = "resources";

}
