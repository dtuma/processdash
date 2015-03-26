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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.sourceforge.processdash.ui.lib.binding.BoundComboBox;
import net.sourceforge.processdash.ui.lib.binding.BoundForm;
import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.ui.lib.binding.BoundMap.Disposable;

import org.w3c.dom.Element;

public class TabularDataColumnSelector extends JPanel implements Disposable {

    ColumnComboBox combo;

    public TabularDataColumnSelector(BoundMap map, Element xml) {
        combo = new ColumnComboBox(map, xml);

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(combo);

        if (map instanceof BoundForm) {
            JPanel childPanel = new InsetOverridingJPanel(
                    new Insets(0, 5, 0, 5));
            add(Box.createHorizontalStrut(15));
            add(childPanel);

            ((BoundForm) map).addFormElements(childPanel, xml);
        }
    }

    public void disposeBoundItem() {
        // when the import form shuts down, save the current value of this
        // widget to user settings.
        combo.saveHistoricalSelections();
    }

    @Override
    public void setToolTipText(String text) {
        super.setToolTipText(text);

        // The BoundForm will set a tooltip on this component (the JPanel).
        // We want the tooltip to be set on our combo box as well.
        combo.setToolTipText(text);
    }


    public class ColumnComboBox extends BoundComboBox implements Runnable,
            ClipboardDataIDs {

        HistoricalSelectionHelper hsh;

        /**
         * A list of column names that have historically been selected in this
         * widget during an import operation. The most recent selections will be
         * at the front of the list.
         */
        private List<String> historicalSelections;


        public ColumnComboBox(BoundMap map, Element xml) {
            super(map, xml.getAttribute("id"), COLUMN_CHOICES);

            Dimension d = getPreferredSize();
            d.width = Integer.MAX_VALUE;
            setMaximumSize(d);

            this.hsh = new HistoricalSelectionHelper(map, propertyName);
            this.historicalSelections = hsh
                    .loadHistoricalSelections(new ArrayList<String>());
            map.addPropertyChangeListener(HAS_HEADER, this,
                "maybeSelectHistoricalColumn");
        }

        /**
         * If this selector is currently pointing to a column with a header, add
         * that column name to the list of recognized values and save the user
         * setting.
         */
        private void saveHistoricalSelections() {
            if (hasHeaderRow() == false)
                return;

            TabularDataColumn val = (TabularDataColumn) map.get(propertyName);
            if (val != null && val.getPos() != -1) {
                // the user has selected a column in this widget. Retrieve
                // the column name and place it at the front of the list of
                // historical selections.
                String name = val.getName().toLowerCase();
                historicalSelections.remove(name);
                historicalSelections.add(0, name);

            } else {
                // the user has selected "None" for this widget. Take a look
                // at the columns they *could* have selected but intentionally
                // chose not to; and make certain none of those are in the
                // list of historical preferences. (Otherwise, their presence
                // in the list will cause them to be autoselected in the future,
                // possibly causing the user to have to manually deselect them
                // every time.)
                Set<String> ignoredColumns = hsh.getColumnNames(COLUMNS);
                historicalSelections.removeAll(ignoredColumns);
            }

            hsh.saveHistoricalSelections(historicalSelections);
        }

        @Override
        public void updateListFromMap() {
            super.updateListFromMap();
            SwingUtilities.invokeLater(this);
        }

        @Override
        public void updateMapFromValue() {
            super.updateMapFromValue();

            // Whenever we change the value we're writing into the map, also
            // write a flag indicating whether our value is the "-None-"
            // element. (This flag can be used for enablement calculations.)
            boolean isActive = !selectedItemIsNone();
            map.put(propertyName + ".selected", isActive);
        }

        public void maybeSelectHistoricalColumn() {
            SwingUtilities.invokeLater(this);
        }

        public void run() {
            // This method runs a few moments after our dependent data elements
            // change (e.g., the list of columns, or the presence of a header).

            if (selectedItemIsImplicit())
                // If the current value for this column did not appear in the
                // list of available choices, the BoundComboBox class will
                // have created an "implicit" option to display it. We don't
                // want that. Instead, try to select the best column.
                autoselectBestColumn();

            else if (selectedItemIsNone())
                // If the current value is the "-None-" option and we get new
                // clipboard data, try looking at the clipboard columns to see
                // if one is a good match, and automatically select it.
                autoselectBestColumn();

            else
                // When the clipboard data changes, the BoundComboBox class
                // will reload the options. If the value currently held in
                // the combo box has the same *name* as one of the new options,
                // the BoundComboBox class will automatically select that new
                // option. However, it is possible that the column *positions*
                // have changed. So we retrieve the newly selected object
                // and store it into the map to make certain our clients see
                // the correct column position.
                updateMapFromValue();
        }

        private void autoselectBestColumn() {
            map.put(propertyName, findHistoricallySelectedColumn());
        }

        /**
         * Look through the current column list, and see if one of the options
         * matches an entry in the list of historically selected values for this
         * field.
         * 
         * @return a TabularDataColumn whose name matches a historically
         *         selected value for this field, or the "-None-" option if no
         *         match is found.
         */
        private TabularDataColumn findHistoricallySelectedColumn() {
            // if the current data does not have a header row, don't even
            // try performing a match.
            if (hasHeaderRow() == false)
                return TabularDataColumn.NONE_SELECTED;

            // Retrieve the list of columns for this selector.
            List<TabularDataColumn> columns = (List<TabularDataColumn>) map
                    .get(COLUMNS);

            // Look through the list of historically selected column names,
            // in the order they were used (from most recent to oldest)
            for (String name : historicalSelections) {
                // Now look through the list of columns to see if one has this
                // name. If we find a match, return it.
                for (TabularDataColumn tdc : columns) {
                    if (name.equalsIgnoreCase(tdc.getName()))
                        return tdc;
                }
            }

            // No match was found. Return the "-None-" option.
            return TabularDataColumn.NONE_SELECTED;
        }

        /** Return true if the currently selected item is the "-None-" option */
        private boolean selectedItemIsNone() {
            Object val = map.get(propertyName);
            return (val == TabularDataColumn.NONE_SELECTED);
        }

        /** @return true if the clipboard data has a header row */
        private boolean hasHeaderRow() {
            return (map.get(HAS_HEADER) == Boolean.TRUE);
        }

    }

    private class InsetOverridingJPanel extends JPanel {

        private Insets insets;

        public InsetOverridingJPanel(Insets insets) {
            super(new GridBagLayout());
            this.insets = insets;
        }

        @Override
        public void add(Component comp, Object constraints) {
            if (constraints instanceof GridBagConstraints) {
                GridBagConstraints gbc = (GridBagConstraints) constraints;
                gbc.insets = this.insets;
            }
            super.add(comp, constraints);
        }
    }

}
