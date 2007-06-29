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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.log.ui.importer;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.log.ui.DefectLogEditor;
import net.sourceforge.processdash.util.StringUtils;

import org.w3c.dom.Element;

public class FormImporter extends AbstractAction {

    private DashboardContext dashboardContext;

    private Element configElement;

    public FormImporter() {
        setEnabled(false);
    }

    public void setDashboardContext(DashboardContext dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    public void setConfigElement(Element xml, String attrName) {
        this.configElement = xml;

        String name = xml.getAttribute("displayName");
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
        String selectedPath = (String) getValue(DefectLogEditor.IMPORT_ACTION_SEL_PATH);
        if (!StringUtils.hasValue(selectedPath))
            AbortImport.showErrorAndAbort("Select_Hierarchy");

        String defectLogPath = (String) getValue(DefectLogEditor.IMPORT_ACTION_DEF_PATH);
        if (!StringUtils.hasValue(defectLogPath))
            AbortImport.showErrorAndAbort("No_Defect_Log", selectedPath);

        String displayName = (String) getValue(NAME);

        new DefectImportForm(dashboardContext, configElement, selectedPath,
                defectLogPath, displayName);
    }

}
