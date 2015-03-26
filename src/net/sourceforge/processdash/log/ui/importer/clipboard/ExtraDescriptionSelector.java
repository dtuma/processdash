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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.processdash.ui.lib.DropDownButton;
import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.ui.lib.binding.BoundMap.Disposable;

import org.w3c.dom.Element;

public class ExtraDescriptionSelector extends DropDownButton implements
        ClipboardDataIDs, Disposable {

    BoundMap map;

    String id;

    HistoricalSelectionHelper hsh;

    Set<String> historicalSelections;

    Set<String> currentSelections;

    public ExtraDescriptionSelector(BoundMap map, Element xml) {
        super(map.getAttrOrResource(xml, null, "Button_Label", null));
        setMainButtonBehavior(DropDownButton.OPEN_DROP_DOWN_MENU);

        this.map = map;
        this.id = xml.getAttribute("id");
        this.hsh = new HistoricalSelectionHelper(map, id);
        this.historicalSelections = hsh
                .loadHistoricalSelections(new TreeSet<String>());
        this.currentSelections = new HashSet<String>(historicalSelections);

        map.addPropertyChangeListener(COLUMNS, this,
            "updateMenuContentsFromMap");
        updateMenuContentsFromMap();
    }

    @Override
    public void setToolTipText(String text) {
        super.setToolTipText(text);

        // The BoundForm will set a tooltip on this component.
        // We want the tooltip to be set on our main button as well.
        getButton().setToolTipText(text);
    }

    public void disposeBoundItem() {
        saveHistoricalSelections();
    }

    /**
     * Make a note of the columns that are currently selected, and update the
     * user setting to reflect this information.
     */
    private void saveHistoricalSelections() {
        if (hsh.hasHeaderRow() == false)
            return;

        Set<String> allColumns = hsh.getColumnNames(COLUMNS);
        Set<String> selectedColumns = hsh.getColumnNames(id);

        Set<String> unselectedColumns = new HashSet<String>(allColumns);
        unselectedColumns.removeAll(selectedColumns);

        // any columns that were selected during this round should be
        // remembered for future sessions by adding them to the user setting.
        historicalSelections.addAll(selectedColumns);

        // if any columns were selected historically but NOT selected this
        // time around, remember that user decision by removing them from
        // the user setting too.
        historicalSelections.removeAll(unselectedColumns);

        hsh.saveHistoricalSelections(historicalSelections);
    }

    public void updateMenuContentsFromMap() {
        getMenu().removeAll();

        List columns = (List) map.get(COLUMNS);
        if (columns != null) {
            for (Iterator i = columns.iterator(); i.hasNext();) {
                TabularDataColumn tdc = (TabularDataColumn) i.next();
                getMenu().add(new ColumnMenuItem(tdc));
            }
        }

        updateMapFromMenuSelections();
    }

    public void updateMapFromMenuSelections() {
        List<TabularDataColumn> result = new ArrayList<TabularDataColumn>();
        for (int i = 0; i < getMenu().getItemCount(); i++) {
            Object item = getMenu().getItem(i);
            if (item instanceof ColumnMenuItem) {
                ColumnMenuItem co = (ColumnMenuItem) item;
                if (co.isSelected())
                    result.add(co.tdc);
            }
        }
        map.put(id, result);
    }

    private class ColumnMenuItem extends JCheckBoxMenuItem implements
            ChangeListener {

        private TabularDataColumn tdc;

        private String columnKey;

        public ColumnMenuItem(TabularDataColumn tdc) {
            super(tdc.toString());
            this.tdc = tdc;
            this.columnKey = tdc.getName().toLowerCase();

            setSelected(currentSelections.contains(columnKey));

            addChangeListener(this);
        }

        public void stateChanged(ChangeEvent e) {
            if (isSelected())
                currentSelections.add(columnKey);
            else
                currentSelections.remove(columnKey);

            updateMapFromMenuSelections();
        }

    }

}
