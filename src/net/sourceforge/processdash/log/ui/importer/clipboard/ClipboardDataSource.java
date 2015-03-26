// Copyright (C) 2010 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.ui.importer.clipboard;

import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;

import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.TabularClipboardDataHelper;

import org.w3c.dom.Element;

public class ClipboardDataSource extends JButton implements ClipboardDataIDs {

    BoundMap map;

    String id;

    public ClipboardDataSource(BoundMap map, Element xml) {
        this.map = map;
        this.id = RAW_DATA;

        String label = map.getAttrOrResource(xml, null, "Button_Label", null);
        if (StringUtils.hasValue(label)) {
            setText(label);
            addActionListener(EventHandler.create(ActionListener.class, this,
                "loadDataFromClipboard"));
        } else {
            setVisible(false);
        }

        loadDataFromClipboard();
    }

    public void loadDataFromClipboard() {
        List<List<String>> data = TabularClipboardDataHelper
                .getTabularDataFromClipboard();
        deleteEmptyRows(data);

        if (data == null || data.isEmpty()) {
            map.put(RAW_DATA, null);

            if (map.get(COLUMN_CHOICES) == null) {
                map.put(COLUMN_CHOICES, Collections
                        .singletonList(TabularDataColumn.NONE_SELECTED));
                map.put(COLUMNS, Collections.EMPTY_LIST);
            }

        } else {
            List<String> columnData = new ArrayList<String>(data.get(0));
            map.put(HAS_HEADER, looksLikeHeaderRow(columnData));

            List<TabularDataColumn> columnsPlain = TabularDataColumn
                    .buildColumns(columnData);
            map.put(COLUMNS, columnsPlain);

            List<TabularDataColumn> columnChoices = new ArrayList<TabularDataColumn>(
                    columnsPlain);
            columnChoices.add(0, TabularDataColumn.NONE_SELECTED);
            map.put(COLUMN_CHOICES, columnChoices);

            map.put(RAW_DATA, data);
        }
    }

    /** Delete rows that have no data */
    private void deleteEmptyRows(List<List<String>> data) {
        if (data != null) {
            Iterator<List<String>> i = data.iterator();
            while (i.hasNext()) {
                if (isEmptyRow(i.next()))
                    i.remove();
            }
        }
    }

    private boolean isEmptyRow(List<String> row) {
        if (row != null)
            for (String item : row)
                if (StringUtils.hasValue(item))
                    return false;
        return true;
    }

    /**
     * Make an educated guess as to whether a row of data could be a header row,
     * based on its contents
     */
    private boolean looksLikeHeaderRow(List<String> row) {
        for (String item : row)
            if (looksLikeDataElement(item))
                return false;
        return true;
    }

    /**
     * @return true if the item looks like a data value, false if it could
     *         possibly be a column header label
     */
    private boolean looksLikeDataElement(String item) {
        // column headings are typically short. If we see a long value,
        // chances are it is data and not a column header label.
        if (item.length() > 30)
            return true;

        // data elements will often have numeric digits inside, while headers
        // typically will not. If we see a digit, treat this as data.
        for (int c = '0'; c <= '9'; c++)
            if (item.indexOf(c) != -1)
                return true;

        // no obvious problems - consider it to be a potential header label.
        return false;
    }

}
