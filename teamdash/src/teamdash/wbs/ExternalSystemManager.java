// Copyright (C) 2018 Tuma Solutions, LLC
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

import org.w3c.dom.Element;

import net.sourceforge.processdash.util.XMLUtils;

import teamdash.sync.ExtSyncUtil;
import teamdash.wbs.columns.ExternalNodeIDColumn;

public class ExternalSystemManager {

    public static void createDataColumns(File wbsDir, DataTableModel data) {
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
                if ("extSync".equals(ext.getTagName())) {
                    // read the specification of this external system
                    String type = ext.getAttribute("type");
                    String sysID = XMLUtils.getAttribute(ext, "id", type);
                    String sysName = XMLUtils.getAttribute(ext, "name", type);

                    // create a data column to display the external node ID
                    DataColumn col = new ExternalNodeIDColumn(sysID, sysName);
                    data.addDataColumn(col);
                }
            }
        } catch (Exception e) {
        }
    }

}
