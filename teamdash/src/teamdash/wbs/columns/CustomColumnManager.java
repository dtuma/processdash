// Copyright (C) 2011-2012 Tuma Solutions, LLC
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

package teamdash.wbs.columns;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.TableColumnModel;

import org.w3c.dom.Element;

import net.sourceforge.processdash.util.XMLUtils;

import teamdash.wbs.DataColumn;
import teamdash.wbs.DataTableColumn;
import teamdash.wbs.DataTableModel;

public class CustomColumnManager {

    public static final String SYS_PROP_NAME = "teamdash.wbs.customColumnURLs";

    private DataTableModel dataModel;

    private List<DataColumn> customColumns;

    public CustomColumnManager(DataTableModel dataModel, String processID) {
        this.dataModel = dataModel;
        this.customColumns = createColumns(processID);
        dataModel.addRemoveDataColumns(customColumns, null);
    }

    public void addColumnsToColumnModel(TableColumnModel columnModel) {
        for (DataColumn col : customColumns)
            columnModel.addColumn(new DataTableColumn(dataModel, col));
    }

    private List<DataColumn> createColumns(String processID) {
        // Find all of the column specs, and index them by ID.  If two
        // definitions are found for the same column ID, take the earlier one.
        Map<String, Element> columnSpecs = new LinkedHashMap<String, Element>();
        for (Element e : loadColumnSpecs()) {
            String columnId = e.getAttribute(COLUMN_ID_ATTR);
            if (!columnSpecs.containsKey(columnId))
                columnSpecs.put(columnId, e);
        }

        // Now, iterate over the column specs that we found, and create the
        // actual column objects for each one.
        List<DataColumn> result = new ArrayList<DataColumn>();
        for (Element e : columnSpecs.values()) {
            DataColumn col = createColumn(e, processID);
            if (col != null)
                result.add(col);
        }
        return result;
    }

    private List<Element> loadColumnSpecs() {
        List<Element> result = new ArrayList<Element>();
        String locations = System.getProperty(SYS_PROP_NAME);
        if (locations != null) {
            for (String url : locations.trim().split("\\s+"))
                loadColumnSpecs(result, url);
        }
        return result;
    }

    private void loadColumnSpecs(List<Element> result, String url) {
        try {
            Element doc = XMLUtils.parse(new URL(url).openStream())
                    .getDocumentElement();
            result.addAll(XMLUtils.getChildElements(doc));
        } catch (Exception e) {
            System.out.println("Could not open custom column spec from " + url);
            e.printStackTrace();
        }
    }

    private DataColumn createColumn(Element e, String processID) {
        if (!COLUMN_TAG.equals(e.getTagName()))
            // this element is not a custom column tag. Ignore it.
            return null;

        String id = e.getAttribute(COLUMN_ID_ATTR);
        String name = e.getAttribute(COLUMN_NAME_ATTR);
        String type = e.getAttribute(COLUMN_TYPE_ATTR);
        if (!hasValue(id) || !hasValue(name) || !hasValue(type))
            // this element is missing a required attribute. Ignore it.
            return null;

        String forProcess = e.getAttribute(COLUMN_PROCESS_ATTR);
        if (hasValue(forProcess) && !forProcess.equals(processID))
            // this element is for a different process type than the one
            // that this WBS is using.
            return null;

        if (TYPE_ANCESTOR_SELECTION.equals(type))
            return new AncestorSelectionColumn(dataModel, id, name, e);

        else if (TYPE_TEXT.equals(type))
            return new CustomTextColumn(dataModel, id, name, e);

        System.out.println("Unrecognized type '" + type
                + "' for custom column with id '" + id + "' - ignoring");
        return null;
    }

    private boolean hasValue(String s) {
        return XMLUtils.hasValue(s);
    }

    private static final String COLUMN_TAG = "column";

    private static final String COLUMN_ID_ATTR = "id";

    private static final String COLUMN_NAME_ATTR = "name";

    private static final String COLUMN_TYPE_ATTR = "type";

    private static final String COLUMN_PROCESS_ATTR = "forProcess";

    private static final String TYPE_ANCESTOR_SELECTION = "AncestorSelectionColumn";

    private static final String TYPE_TEXT = "TextColumn";

}
