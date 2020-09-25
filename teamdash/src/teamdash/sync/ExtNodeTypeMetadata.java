// Copyright (C) 2020 Tuma Solutions, LLC
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

package teamdash.sync;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.tool.export.mgr.ExportFileEntry;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.StringUtils;

public class ExtNodeTypeMetadata {

    public String id;

    public String name;

    public boolean creatable;

    public String iconUrl;

    public String iconPadding;


    public static void storeNodeTypeMetadata(SyncDataFile syncData,
            List<ExtNodeTypeMetadata> allNodeTypes) throws IOException {

        // discard any node type icon files previously in the sync data file
        for (ExportFileEntry efe : syncData.readExistingManifestEntries()) {
            if (efe.getFilename().startsWith(ExtSyncUtil.NODE_TYPE_ICON))
                syncData.addEntry(efe, null);
        }

        // iterate over the node types we were given
        Map<String, String> issueTypes = new LinkedHashMap<String, String>();
        List<String> creatableTypes = new ArrayList();
        for (ExtNodeTypeMetadata type : allNodeTypes) {
            // build a mapping of ID to name
            issueTypes.put(type.id, type.name);

            // make an ID list of types that are creatable
            if (type.creatable)
                creatableTypes.add(type.id);

            // store the type icon in the data file, if one was given
            if (StringUtils.hasValue(type.iconUrl))
                downloadIconUrl(syncData, type);
        }

        // save the node type metadata
        SyncMetadata metadata = syncData.getMetadata();
        metadata.discardAttrs(ExtSyncUtil.NODE_TYPE_PREFIX);
        metadata.storeKeyedItems(issueTypes, ExtSyncUtil.NODE_TYPE_PREFIX,
            ExtSyncUtil.NAME_ATTR);
        metadata.setStr(StringUtils.join(creatableTypes, ","),
            ExtSyncUtil.NODE_TYPE_PREFIX, ExtSyncUtil.CREATABLE_ATTR);

    }

    private static void downloadIconUrl(SyncDataFile syncData,
            ExtNodeTypeMetadata type) {
        try {
            // open a connection to the icon URL and download the info
            URL url = new URL(type.iconUrl);
            URLConnection conn = url.openConnection();
            String contentType = conn.getContentType();
            byte[] data = FileUtils.slurpContents(conn.getInputStream(), true);

            // discard the icon if we don't recognize its content type
            String fileSuffix = getIconFileSuffix(contentType);
            if (fileSuffix == null)
                return;

            // store the icon data in the sync data file
            String iconEntryName = ExtSyncUtil.NODE_TYPE_ICON + "/" + type.id
                    + fileSuffix;
            ExportFileEntry efe = new ExportFileEntry(iconEntryName,
                    ExtSyncUtil.NODE_TYPE_ICON, "1", null);
            syncData.addEntry(efe, data);

            // store any icon padding value that was specified
            syncData.getMetadata().setStr(type.iconPadding,
                ExtSyncUtil.NODE_TYPE_PREFIX, type.id,
                ExtSyncUtil.NODE_TYPE_ICON_PADDING);

        } catch (IOException ioe) {
            // missing icons are not fatal
        }
    }

    private static String getIconFileSuffix(String iconContentType) {
        if (iconContentType != null) {
            for (int i = 0; i < ICON_FILE_TYPES.length; i += 2) {
                if (iconContentType.contains(ICON_FILE_TYPES[i]))
                    return ICON_FILE_TYPES[i + 1];
            }
        }
        return null;
    }

    private static final String[] ICON_FILE_TYPES = { "/svg", ".svg", //
            "/png", ".png", "/jpeg", ".jpg", "/gif", ".gif" };

}
