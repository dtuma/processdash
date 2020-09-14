// Copyright (C) 2018-2020 Tuma Solutions, LLC
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

package teamdash.wbs;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Element;

import net.sourceforge.processdash.util.XMLUtils;

import teamdash.sync.ExtSyncUtil;
import teamdash.wbs.columns.ExternalNodeIDColumn;
import teamdash.wbs.columns.ExternalNodeOwnerColumn;
import teamdash.wbs.columns.ExternalNodeTypeColumn;

public class ExternalSystemManager {

    private File wbsDir;

    private Map<String, ExtSystem> extSystems;

    public ExternalSystemManager(File wbsDir) {
        this.wbsDir = wbsDir;
        loadExtSystems();
    }

    private void loadExtSystems() {
        this.extSystems = new LinkedHashMap();

        // check for an external spec file in the WBS storage directory.
        File externalSpecFile = new File(wbsDir, ExtSyncUtil.EXT_SPEC_FILE);
        if (!externalSpecFile.isFile())
            return;

        // read the XML in the file, and create columns for each ext sync
        // specification found inside
        try {
            Element xml = XMLUtils.parse(new FileInputStream(externalSpecFile))
                    .getDocumentElement();
            for (Element ext : XMLUtils.getChildElements(xml)) {
                if ("extSync".equals(ext.getTagName()))
                    new ExtSystem(ext);
            }
        } catch (Exception e) {
        }
    }


    public void createDataColumns(WBSDataModel data) {
        for (ExtSystem ext : extSystems.values()) {
            // create a data column to display the external node ID
            DataColumn col = new ExternalNodeIDColumn(ext.sysID, ext.sysName);
            data.addDataColumn(col);

            // create a data column to display the external node type
            col = new ExternalNodeTypeColumn(ext.sysID, ext.sysName);
            data.addDataColumn(col);

            // create a data column to display the external node owner
            col = new ExternalNodeOwnerColumn(ext.sysID, ext.sysName);
            data.addDataColumn(col);
        }
    }



    private class ExtSystem {

        private String type, sysID, sysName;

        ExtSystem(Element ext) {
            // read the specification of an external system from the XML tag
            this.type = ext.getAttribute("type");
            this.sysID = XMLUtils.getAttribute(ext, "id", type);
            this.sysName = XMLUtils.getAttribute(ext, "name", type);
            extSystems.put(sysID, this);
        }
    }

}
