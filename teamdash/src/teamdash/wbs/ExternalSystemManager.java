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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.w3c.dom.Element;

import net.sourceforge.processdash.tool.export.mgr.ExportFileEntry;
import net.sourceforge.processdash.ui.lib.ScalableImageIcon;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.XMLUtils;

import teamdash.sync.ExtSyncUtil;
import teamdash.sync.SyncDataFile;
import teamdash.sync.SyncMetadata;
import teamdash.wbs.columns.ExternalNodeIDColumn;
import teamdash.wbs.columns.ExternalNodeOwnerColumn;
import teamdash.wbs.columns.ExternalNodeTypeColumn;
import teamdash.wbs.icons.ScalableSvgIcon;


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
            col = new ExternalNodeTypeColumn(data, ext.sysID, ext.sysName,
                    getNodeTypes(ext));
            data.addDataColumn(col);

            // create a data column to display the external node owner
            col = new ExternalNodeOwnerColumn(ext.sysID, ext.sysName);
            data.addDataColumn(col);
        }
    }


    public Icon getExtNodeTypeIcon(WBSNode node) {
        // identify which external system this node is using
        String sysID = (String) node.getAttribute(ExtSyncUtil.EXT_SYSTEM_ID_ATTR);
        if (sysID == null)
            return null;

        // get the node type ID, and use it to look up a node type
        Object typeID = node.getAttribute(ExtSyncUtil.EXT_NODE_TYPE_ID_ATTR);
        for (ExtNodeType type : getNodeTypes(sysID)) {
            if (type.getId().equals(typeID))
                return type.getIcon();
        }

        // no matching type was found. Return a default icon
        return DEFAULT_NODE_TYPE_ICON;
    }


    public List<ExtNodeType> getNodeTypes(String extSystemID) {
        ExtSystem ext = extSystems.get(extSystemID);
        if (ext == null)
            return Collections.EMPTY_LIST;
        else
            return getNodeTypes(ext);
    }

    private List<ExtNodeType> getNodeTypes(ExtSystem ext) {
        // return the node types we loaded in the past, if available
        if (ext.nodeTypes != null)
            return ext.nodeTypes;

        // read node type info from the external metadata file
        List<ExtNodeType> result = new ArrayList<ExtNodeType>();
        try {
            SyncMetadata metadata = ext.syncData.getMetadata();
            List<ExportFileEntry> entries = ext.syncData
                    .readExistingManifestEntries();
            Map<String, String> types = metadata.getKeyedItems(
                ExtSyncUtil.NODE_TYPE_PREFIX, ExtSyncUtil.NAME_ATTR);
            for (Entry<String, String> e : types.entrySet()) {
                String typeID = e.getKey();
                String typeName = e.getValue();
                Icon typeIcon = getIcon(ext.syncData, entries,
                    ExtSyncUtil.NODE_TYPE_ICON, typeID);
                result.add(new ExtNodeType(ext, typeID, typeName, typeIcon));
            }
            ext.syncData.dispose();
        } catch (IOException e) {
        }

        // save the node types and return them
        ext.nodeTypes = result;
        return result;
    }

    private Icon getIcon(SyncDataFile syncData, List<ExportFileEntry> entries,
            String entryType, String typeID) {
        String namePat = "/" + typeID + ".";
        for (ExportFileEntry e : entries) {
            String entryName = e.getFilename();
            if (e.getType().equals(entryType) && entryName.contains(namePat)) {
                try {
                    return WBSZoom.icon(
                        readIcon(entryName, syncData.openEntry(entryName)));
                } catch (Exception ex) {
                }
            }
        }
        return DEFAULT_NODE_TYPE_ICON;
    }

    private Icon readIcon(String entryName, InputStream in) throws Exception {
        byte[] data = FileUtils.slurpContents(in, true);
        if (entryName.endsWith(".svg"))
            return new ScalableSvgIcon(16, data);
        else
            return new ScalableImageIcon(16, new ImageIcon(data));
    }

    private static final Icon DEFAULT_NODE_TYPE_ICON = WBSZoom
            .icon(IconFactory.getExtNodeIcon());



    public class ExtSystem {

        private String type, sysID, sysName;

        private SyncDataFile syncData;

        private List<ExtNodeType> nodeTypes;

        private ExtSystem(Element ext) {
            // read the specification of an external system from the XML tag
            this.type = ext.getAttribute("type");
            this.sysID = XMLUtils.getAttribute(ext, "id", type);
            this.sysName = XMLUtils.getAttribute(ext, "name", type);
            this.syncData = new SyncDataFile(wbsDir, sysID + "-sync.pdash");
            extSystems.put(sysID, this);
        }

        public final String getID() {
            return sysID;
        }

        public final String getName() {
            return sysName;
        }

    }



    public class ExtNodeType {

        private ExtSystem extSystem;

        private String id, name;

        private Icon icon;

        private ExtNodeType(ExtSystem extSystem, String id, String name,
                Icon icon) {
            this.extSystem = extSystem;
            this.id = id;
            this.name = name;
            this.icon = icon;
        }

        public final ExtSystem getExtSystem() {
            return extSystem;
        }

        public final String getId() {
            return id;
        }

        public final String getName() {
            return name;
        }

        public final Icon getIcon() {
            return icon;
        }

        @Override
        public String toString() {
            return name + "@" + id;
        }

    }

}
