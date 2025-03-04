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

package teamdash.wbs;

import static teamdash.sync.ExtSyncDaemon.EXT_SYSTEM_NAME;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
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


    public List<ExtSystem> getExternalSystems() {
        return new ArrayList<ExtSystem>(extSystems.values());
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
            String createKeyList = metadata.getStr(ExtSyncUtil.NODE_TYPE_PREFIX,
                ExtSyncUtil.CREATABLE_ATTR);
            List<String> creatableKeys = (createKeyList == null
                    ? Collections.EMPTY_LIST
                    : Arrays.asList(createKeyList.split(",")));
            for (Entry<String, String> e : types.entrySet()) {
                String typeID = e.getKey();
                String typeName = e.getValue();
                Icon typeIcon = getIcon(ext.syncData, metadata, entries,
                    ExtSyncUtil.NODE_TYPE_ICON, typeID);
                boolean creatable = creatableKeys.contains(typeID);
                result.add(new ExtNodeType(ext, typeID, typeName, typeIcon,
                        creatable));
            }
            ext.syncData.dispose();
        } catch (IOException e) {
        }

        // save the node types and return them
        ext.nodeTypes = result;
        return result;
    }

    private Icon getIcon(SyncDataFile syncData, SyncMetadata metadata,
            List<ExportFileEntry> entries, String entryType, String typeID) {
        String namePat = "/" + typeID + ".";
        for (ExportFileEntry e : entries) {
            String entryName = e.getFilename();
            if (e.getType().equals(entryType) && entryName.contains(namePat)) {
                try {
                    return WBSZoom.icon(readIcon(entryName, //
                        syncData.openEntry(entryName), //
                        getIconPadding(metadata, typeID)));
                } catch (Exception ex) {
                }
            }
        }
        return DEFAULT_NODE_TYPE_ICON;
    }

    private int[] getIconPadding(SyncMetadata metadata, String typeID) {
        // retrieve the padding; first for this specific icon, then global
        String padding = metadata.getStr(ExtSyncUtil.NODE_TYPE_PREFIX, typeID,
            ExtSyncUtil.NODE_TYPE_ICON_PADDING);
        if (padding == null)
            padding = metadata.getStr(ExtSyncUtil.NODE_TYPE_PREFIX,
                ExtSyncUtil.NODE_TYPE_ICON_PADDING);

        // if no padding is specified, assume 1 pixel padding all around
        if (padding == null)
            return STD_ICON_PADDING;

        // parse the padding like CSS, with 1-4 padding components
        try {
            String[] p = padding.trim().split("\\D+");
            int[] result = new int[4];
            result[0] = Integer.parseInt(p[0]);
            result[1] = (p.length > 1 ? Integer.parseInt(p[1]) : result[0]);
            result[2] = (p.length > 2 ? Integer.parseInt(p[2]) : result[0]);
            result[3] = (p.length > 3 ? Integer.parseInt(p[3]) : result[1]);
            return result;
        } catch (Exception e) {
            return STD_ICON_PADDING;
        }
    }

    private static final int[] STD_ICON_PADDING = { 1, 1, 1, 1 };

    private Icon readIcon(String entryName, InputStream in, int[] padding)
            throws Exception {
        // in the future, we may honor and apply all padding values; but for
        // now we just subtract top and bottom padding from the icon height,
        // then let rendering code center the resulting icon
        int height = 16 - padding[0] - padding[2];
        byte[] data = FileUtils.slurpContents(in, true);
        if (entryName.endsWith(".svg"))
            return new ScalableSvgIcon(height, data);
        else
            return new ScalableImageIcon(height, new ImageIcon(data));
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
            this.sysName = ext.getAttribute("name");
            this.syncData = new SyncDataFile(wbsDir, sysID + "-sync.pdash");
            if (!XMLUtils.hasValue(this.sysName)) {
                try {
                    sysName = syncData.getMetadata().getStr(EXT_SYSTEM_NAME);
                } catch (IOException ioe) {
                    sysName = sysID;
                }
            }
            extSystems.put(sysID, this);
        }

        public final String getID() {
            return sysID;
        }

        public final String getName() {
            return sysName;
        }

        public List<ExtNodeType> getNodeTypes() {
            return ExternalSystemManager.this.getNodeTypes(this);
        }

    }



    public class ExtNodeType {

        private ExtSystem extSystem;

        private String id, name;

        private Icon icon;

        private boolean creatable;

        private ExtNodeType(ExtSystem extSystem, String id, String name,
                Icon icon, boolean creatable) {
            this.extSystem = extSystem;
            this.id = id;
            this.name = name;
            this.icon = icon;
            this.creatable = creatable;
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

        public final boolean isCreatable() {
            return creatable;
        }

        @Override
        public String toString() {
            return name + "@" + id;
        }

    }

}
