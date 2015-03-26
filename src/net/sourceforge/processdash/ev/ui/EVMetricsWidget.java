// Copyright (C) 2003-2008 Tuma Solutions, LLC
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


package net.sourceforge.processdash.ev.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import net.sourceforge.processdash.ev.EVMetrics;
import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.ui.chart.AbstractEVChart;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.snippet.SnippetWidget;


public class EVMetricsWidget implements SnippetWidget {

    private static final Resources resources = AbstractEVChart.getResources();

    public Component getWidgetComponent(Map environment, Map parameters) {
        EVSchedule schedule = (EVSchedule) environment
                .get(EVSnippetEnvironment.SCHEDULE_KEY);
        EVMetrics m = schedule.getMetrics();

        JTable table = new JTable(m);
        table.removeColumn(table.getColumnModel().getColumn(3));
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(100, 100));

        DescriptionPane descr =
            new DescriptionPane(m, table.getSelectionModel());

        Box result = Box.createVerticalBox();
        result.add(scrollPane);
        result.add(Box.createVerticalStrut(2));
        result.add(descr);
        return result;
    }

    private class DescriptionPane extends JTextArea
        implements ListSelectionListener, TableModelListener
    {
        EVMetrics metrics;
        ListSelectionModel selectionModel;
        public DescriptionPane(EVMetrics m, ListSelectionModel sm) {
            super(resources.getString("Choose_Metric_Instruction"));
            setBackground(null);
            setLineWrap(true); setWrapStyleWord(true); setEditable(false);
            doResize();
            metrics = m;
            metrics.addTableModelListener(this);
            selectionModel = sm;
            selectionModel.addListSelectionListener(this);
        }

        public void valueChanged(ListSelectionEvent e) { refreshText(); }
        public void tableChanged(TableModelEvent e)    { refreshText(); }
        public void refreshText() {
            String descr = (String) metrics.getValueAt
                (selectionModel.getMinSelectionIndex(), EVMetrics.FULL);
            setText(descr);
            doResize();
        }
        private void doResize() {
            Dimension d = getPreferredSize();
            setMinimumSize(d);
            d.width = 10000;
            setMaximumSize(d);
        }
    }

}
