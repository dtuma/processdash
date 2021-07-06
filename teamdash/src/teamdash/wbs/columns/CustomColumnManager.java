// Copyright (C) 2011-2021 Tuma Solutions, LLC
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

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.table.TableColumnModel;

import org.w3c.dom.Element;

import net.sourceforge.processdash.util.RobustFileWriter;
import net.sourceforge.processdash.util.XMLUtils;

import teamdash.wbs.DataColumn;
import teamdash.wbs.DataTableColumn;
import teamdash.wbs.DataTableModel;

public class CustomColumnManager {

    public static final String SYS_PROP_NAME = "teamdash.wbs.customColumnURLs";

    private DataTableModel dataModel;

    private CustomColumnSpecs projectColumnSpecs;

    private String processID;

    private List<CustomColumn> customColumns;

    private List<CustomColumnListener> listeners;

    public CustomColumnManager(DataTableModel dataModel,
            CustomColumnSpecs projectColumnSpecs, String processID) {
        this.dataModel = dataModel;
        this.projectColumnSpecs = projectColumnSpecs;
        this.processID = processID;
        this.customColumns = createColumns(processID);
        this.listeners = new ArrayList<CustomColumnListener>();
        dataModel.addRemoveDataColumns(customColumns, null);
    }

    public void addColumnsToColumnModel(TableColumnModel columnModel) {
        // add columns for each external system
        for (int i = 0; i < dataModel.getColumnCount(); i++) {
            DataColumn col = dataModel.getColumn(i);
            if (col instanceof ExternalSystemPrimaryColumn)
                columnModel.addColumn(new DataTableColumn(dataModel, col));
        }
        // add the custom columns registered to this WBS
        for (CustomColumn col : customColumns)
            columnModel.addColumn(new DataTableColumn(dataModel, col));
    }

    public List<CustomColumn> getProjectSpecificColumns() {
        List<CustomColumn> result = new ArrayList<CustomColumn>();
        for (CustomColumn column : customColumns)
            if (projectColumnSpecs.containsKey(column.getColumnID()))
                result.add(column);
        return result;
    }

    public Set<String> getProjectSpecificColumnIDs() {
        return new LinkedHashSet<String>(projectColumnSpecs.keySet());
    }

    public void replaceProjectSpecificColumns(CustomColumnSpecs newSpecs) {
        // make a set of all custom column IDs, starting with the old ones
        Set<String> columnIDs = getCurrentCustomColumnIDs();

        // replace the specs and rebuild the columns
        projectColumnSpecs.clear();
        projectColumnSpecs.putAll(newSpecs);
        List<CustomColumn> oldColumns = customColumns;
        customColumns = createColumns(processID);
        dataModel.addRemoveDataColumns(customColumns, oldColumns);

        // add all new column IDs to our custom column ID set
        columnIDs.addAll(getCurrentCustomColumnIDs());

        // send events to our registered listener
        for (String oneID : columnIDs) {
            CustomColumn oldColumn = findColumnById(oneID, oldColumns);
            CustomColumn newColumn = findColumnById(oneID, customColumns);
            fireColumnEvent(oldColumn, newColumn);
        }
    }

    private Set<String> getCurrentCustomColumnIDs() {
        Set<String> result = new HashSet<String>();
        for (CustomColumn col : customColumns)
            result.add(col.getColumnID());
        return result;
    }

    public void setOrderOfProjectSpecificColumns(List<String> columnOrder) {
        // make a copy of the custom column specs for this project.
        CustomColumnSpecs specs = new CustomColumnSpecs();
        specs.putAll(projectColumnSpecs);

        // clear out the custom column specs for this project. Then add them
        // back in the requested order.
        projectColumnSpecs.clear();
        for (String oneID : columnOrder) {
            Element oneSpec = specs.remove(oneID);
            if (oneSpec != null)
                projectColumnSpecs.put(oneID, oneSpec);
        }

        // if the requested order overlooked any preexisting columns, add them
        // back in to the model at the end of the list.
        projectColumnSpecs.putAll(specs);
    }

    public void addCustomColumnListener(CustomColumnListener l) {
        listeners.add(l);
    }

    public void removeCustomColumnListener(CustomColumnListener l) {
        listeners.remove(l);
    }

    public void changeColumn(CustomColumn oldColumn, CustomColumn newColumn) {
        // update our data structures
        removeOldColumn(oldColumn);
        addNewColumn(newColumn);

        // propagate column changes into the data model
        dataModel.addRemoveDataColumns(
            newColumn == null ? null : Collections.singletonList(newColumn),
            oldColumn == null ? null : Collections.singletonList(oldColumn));

        // notify our registered listeners
        fireColumnEvent(oldColumn, newColumn);
    }

    protected void fireColumnEvent(DataColumn oldColumn, DataColumn newColumn) {
        for (CustomColumnListener listener : listeners) {
            if (newColumn == null)
                listener.columnDeleted(oldColumn.getColumnID(), oldColumn);
            else if (oldColumn == null)
                listener.columnAdded(newColumn.getColumnID(), newColumn);
            else
                listener.columnChanged(newColumn.getColumnID(), oldColumn,
                    newColumn);
        }
    }

    protected void fireColumnsRenamed() {
        for (CustomColumnListener listener : listeners)
            listener.columnsRenamed();
    }

    protected void removeOldColumn(CustomColumn oldColumn) {
        if (oldColumn != null) {
            customColumns.remove(oldColumn);
            projectColumnSpecs.remove(oldColumn.getColumnID());
        }
    }

    protected void addNewColumn(CustomColumn newColumn) {
        if (newColumn != null) {
            try {
                StringWriter buf = new StringWriter();
                PrintWriter out = new PrintWriter(buf);
                newColumn.getAsXml(out);
                out.flush();
                Element spec = XMLUtils.parse(buf.toString())
                        .getDocumentElement();

                customColumns.add(newColumn);
                projectColumnSpecs.put(newColumn.getColumnID(), spec);
            } catch (Exception e) {
                System.out.println("Unable to add new custom WBS column");
                e.printStackTrace();
            }
        }
    }

    private List<CustomColumn> createColumns(String processID) {
        // load column specs from any registered process assets.
        CustomColumnSpecs columnSpecs = loadColumnSpecsFromProcessAssets();

        // add project-specific columns, replacing any asset-provided columns
        // that have conflicting IDs.
        for (Entry<String, Element> e : projectColumnSpecs.entrySet()) {
            columnSpecs.remove(e.getKey());
            columnSpecs.put(e.getKey(), e.getValue());
        }

        // Now, iterate over the column specs that we found, and create the
        // actual column objects for each one.
        List<CustomColumn> result = new ArrayList<CustomColumn>();
        for (Element e : columnSpecs.values()) {
            CustomColumn col = createColumn(e, processID);
            if (col != null)
                result.add(col);
        }
        return result;
    }

    private CustomColumnSpecs loadColumnSpecsFromProcessAssets() {
        // Find all of the column specs, and index them by ID.  If two
        // definitions are found for the same column ID, take the earlier one.
        CustomColumnSpecs result = new CustomColumnSpecs();
        String locations = System.getProperty(SYS_PROP_NAME);
        if (locations != null) {
            for (String url : locations.trim().split("\\s+"))
                loadColumnSpecs(result, url);
        }
        return result;
    }

    private void loadColumnSpecs(CustomColumnSpecs result, String url) {
        try {
            Element doc = XMLUtils.parse(new URL(url).openStream())
                    .getDocumentElement();
            result.load(doc, false);
        } catch (Exception e) {
            System.out.println("Could not open custom column spec from " + url);
            e.printStackTrace();
        }
    }

    private CustomColumn createColumn(Element e, String processID) {
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

        if (AncestorSelectionColumn.TYPE.equals(type))
            return new AncestorSelectionColumn(dataModel, id, name, e);

        else if (CustomTextColumn.TYPE.equals(type))
            return new CustomTextColumn(dataModel, id, name, e);

        System.out.println("Unrecognized type '" + type
                + "' for custom column with id '" + id + "' - ignoring");
        return null;
    }

    private boolean hasValue(String s) {
        return XMLUtils.hasValue(s);
    }


    public void exportColumns(File dest) throws IOException {
        BufferedWriter out = new BufferedWriter(new RobustFileWriter(dest,
                "UTF-8"));
        projectColumnSpecs.getAsXML(out);
        out.close();
    }


    public void importColumns(File src) throws Exception {
        CustomColumnSpecs specs = new CustomColumnSpecs();
        specs.load(XMLUtils.parse(new BufferedInputStream(
            new FileInputStream(src))).getDocumentElement(), false);
        if (specs.isEmpty())
            throw new IOException("Not a custom columns file");

        for (Entry<String, Element> e : specs.entrySet()) {
            String id = e.getKey();
            CustomColumn oldColumn = findColumnById(id, customColumns);
            CustomColumn newColumn = createColumn(e.getValue(), null);
            if (newColumn != null)
                changeColumn(oldColumn, newColumn);
        }
    }

    private CustomColumn findColumnById(String id, List<CustomColumn> columns) {
        for (CustomColumn col : columns)
            if (col.getColumnID().equals(id))
                return col;
        return null;
    }


    static final String COLUMN_TAG = "column";

    static final String COLUMN_ID_ATTR = "id";

    static final String COLUMN_NAME_ATTR = "name";

    static final String COLUMN_TYPE_ATTR = "type";

    private static final String COLUMN_PROCESS_ATTR = "forProcess";

    static final String COL_WIDTH = "width";

    static final String SYNC_AS_LABEL = "syncAsLabel";

}
