// Copyright (C) 2007 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.log.ui.importer;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionListener;
import java.beans.EventHandler;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import net.sourceforge.processdash.ui.lib.ToolTipTableCellRendererProxy;
import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.ui.lib.binding.ErrorData;

import org.w3c.dom.Element;

public class BoundDefectTable extends JPanel implements TableModelListener {

    BoundMap map;

    BoundDefectData data;

    JTable table;

    JLabel messageLabel;

    JCheckBox selectAll;

    CardLayout cardLayout;

    TableColumn[] tableColumns;

    public BoundDefectTable(BoundMap map, Element xml) {
        this(map);
    }

    public BoundDefectTable(BoundMap map) {
        this.map = map;

        data = BoundDefectData.getDefectData(map);
        data.addTableModelListener(this);

        table = new JTable(data);
        ToolTipTableCellRendererProxy.installHeaderToolTips(table,
                BoundDefectData.COLUMN_TOOLTIPS);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        tableColumns = new TableColumn[table.getColumnCount()];
        for (int i = 0; i < tableColumns.length; i++) {
            tableColumns[i] = table.getColumnModel().getColumn(i);
            tableColumns[i].setPreferredWidth(BoundDefectData.COLUMN_WIDTHS[i]);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane
                .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane
                .setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        selectAll = new JCheckBox(DefectImportForm.resources
                .getString("Toggle_Included_Defects"));
        selectAll.setHorizontalAlignment(SwingConstants.LEFT);
        selectAll.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "toggleSelected"));

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.add(BorderLayout.CENTER, scrollPane);
        tablePanel.add(BorderLayout.SOUTH, selectAll);

        JPanel labelPanel = new JPanel(new BorderLayout());
        messageLabel = new JLabel("", SwingConstants.LEFT);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        labelPanel.add(BorderLayout.NORTH, messageLabel);
        int selectAllHeight = selectAll.getPreferredSize().height;
        labelPanel.setBorder(BorderFactory.createCompoundBorder( //
                BorderFactory.createEmptyBorder(0,0, selectAllHeight, 0),
                BorderFactory.createEtchedBorder()));

        cardLayout = new CardLayout();
        setLayout(cardLayout);
        add("label", labelPanel);
        add("table", tablePanel);

        resyncTable();
    }

    public void toggleSelected() {
        data.selectUnselectAll(selectAll.isSelected());
    }

    public void tableChanged(TableModelEvent e) {
        if (e.getLastRow() > Short.MAX_VALUE
                && e.getColumn() == TableModelEvent.ALL_COLUMNS)
            resyncTable();
    }

    private void resyncTable() {
        selectAll.setSelected(true);

        TableColumnModel tcm = new DefaultTableColumnModel();
        for (int col = 0;  col < data.getColumnCount();  col++)
            if (data.hasData(col))
                tcm.addColumn(tableColumns[col]);
        table.setColumnModel(tcm);

        ErrorData error = data.getErrorData();
        if (error != null) {
            messageLabel.setText(error.getError());
            messageLabel.setForeground(map.getErrorColor(error));
            cardLayout.first(this);
        } else {
            messageLabel.setText(null);
            cardLayout.last(this);
        }

    }

}
