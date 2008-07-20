// Copyright (C) 2001-2008 Tuma Solutions, LLC
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

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.processdash.data.TagData;
import net.sourceforge.processdash.data.util.SimpleDataContext;
import net.sourceforge.processdash.ev.EVHierarchicalFilter;
import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVScheduleFiltered;
import net.sourceforge.processdash.ev.EVTaskFilter;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.WrappingText;
import net.sourceforge.processdash.ui.snippet.SnippetDefinition;
import net.sourceforge.processdash.ui.snippet.SnippetDefinitionManager;
import net.sourceforge.processdash.ui.snippet.SnippetWidget;
import net.sourceforge.processdash.util.Disposable;


public class TaskScheduleChart extends JFrame
    implements EVTaskList.RecalcListener {

    EVTaskList taskList;
    EVSchedule schedule;
    Map<String, SnippetChartItem> widgets;
    JPanel displayArea;
    CardLayout cardLayout;

    static Resources resources = Resources.getDashBundle("EV.Chart");
    static Logger logger = Logger.getLogger(TaskScheduleChart.class.getName());

    public TaskScheduleChart(EVTaskList tl, EVTaskFilter filter) {
        super(formatWindowTitle(tl, filter));
        PCSH.enableHelpKey(this, "UsingTaskSchedule.chart");
        DashboardIconFactory.setWindowIcon(this);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        taskList = tl;
        if (filter == null)
            schedule = taskList.getSchedule();
        else {
            schedule = new EVScheduleFiltered(tl, filter);
            taskList.addRecalcListener(this);
        }

        widgets = getChartWidgets(filter != null);

        cardLayout = new CardLayout(0, 5);
        displayArea = new JPanel(cardLayout);
        displayArea.setMinimumSize(new Dimension(0, 0));
        displayArea.setPreferredSize(new Dimension(400, 300));

        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            createChooserComponent(), displayArea);
        sp.setOneTouchExpandable(true);
        getContentPane().add(sp);

        setSize(600, 300);
        sp.setDividerLocation(160);
        setVisible(true);
    }

    private static String formatWindowTitle(EVTaskList tl, EVTaskFilter filter) {
        if (filter == null)
            return resources.format("Window_Title_FMT", tl.getDisplayName());

        String filterDescription = filter
                .getAttribute(EVHierarchicalFilter.HIER_FILTER_ATTR);
        if (filterDescription == null)
            return resources.format("Window_Title_Filtered", tl.getDisplayName());
        else
            return resources.format("Window_Title_Filtered_FMT",
                    tl.getDisplayName(), filterDescription);
    }

    private Map<String, SnippetChartItem> getChartWidgets(boolean filterInEffect) {
        Map<String, SnippetChartItem> result = new HashMap<String, SnippetChartItem>();

        SimpleDataContext ctx = new SimpleDataContext();
        TagData tag = TagData.getInstance();
        ctx.put(EVSnippetEnvironment.EV_CONTEXT_KEY, tag);
        if (filterInEffect)
            ctx.put(EVSnippetEnvironment.FILTERED_EV_CONTEXT_KEY, tag);
        else
            ctx.put(EVSnippetEnvironment.UNFILTERED_EV_CONTEXT_KEY, tag);

        SnippetDefinitionManager.initialize();
        Set snippets = SnippetDefinitionManager.getAllSnippets();
        for (Iterator i = snippets.iterator(); i.hasNext();) {
            SnippetDefinition snip = (SnippetDefinition) i.next();
            if (snip.getCategory().startsWith("ev")
                    && snip.matchesContext(ctx)) {
                try {
                    SnippetChartItem item = new SnippetChartItem(snip);
                    result.put(snip.getId(), item);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Problem with EV Chart Snippet '"
                            + snip.getId() + "'", e);
                }
            }
        }

        return result;
    }

    private JComponent createChooserComponent() {
        ArrayList items = new ArrayList(widgets.values());
        Collections.sort(items);
        final JList list = new JList(items.toArray());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new SnippetChartItemListRenderer());
        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                SnippetChartItem item = (SnippetChartItem) list.getSelectedValue();
                item.display();
            }});
        list.setSelectedIndex(0);

        return new JScrollPane(list);
    }


    @Override
    public void dispose() {
        super.dispose();
        for (int i = displayArea.getComponentCount();  i-- > 0; ) {
            Component c = displayArea.getComponent(i);
            if (c instanceof Disposable)
                ((Disposable) c).dispose();
        }
        taskList.removeRecalcListener(this);
    }

    public void evRecalculated(EventObject e) {
        if (schedule instanceof EVScheduleFiltered) {
            EVScheduleFiltered filtSched = (EVScheduleFiltered) schedule;
            EVTaskFilter filter = filtSched.getFilter();
            String valid = filter.getAttribute(EVTaskFilter.IS_INVALID);
            if (valid == null)
                filtSched.recalculate();
            else
                dispose();
        }
    }


    private class SnippetChartItem implements Comparable<SnippetChartItem> {

        private SnippetDefinition snip;

        private String id;

        private String name;

        private String description;

        private boolean initialized;


        public SnippetChartItem(SnippetDefinition snip) {
            this.snip = snip;
            this.id = snip.getId();
            this.name = snip.getName();
            try {
                this.description = snip.getDescription();
            } catch (Exception e) {}
            this.initialized = false;
        }

        public void display() {
            if (!initialized) {
                Component comp = getComponent();
                displayArea.add(id, comp);
                initialized = true;
            }
            cardLayout.show(displayArea, id);
        }

        private Component getComponent() {
            try {
                SnippetWidget w = snip.getWidget("view", null);

                Map environment = new HashMap();
                environment.put(EVSnippetEnvironment.TASK_LIST_KEY, taskList);
                environment.put(EVSnippetEnvironment.SCHEDULE_KEY, schedule);
                environment.put(EVSnippetEnvironment.RESOURCES, snip
                        .getResources());

                HashMap params = new HashMap();

                return w.getWidgetComponent(environment, params);

            } catch (Exception e) {
                WrappingText label = new WrappingText(resources
                        .getString("Widget_Error"));
                label.setFont(label.getFont().deriveFont(Font.ITALIC));

                Box b = Box.createVerticalBox();
                b.add(Box.createVerticalGlue());
                b.add(label);
                b.add(Box.createVerticalGlue());
                b.setBorder(new EmptyBorder(30, 30, 30, 30));
                return b;
            }

        }

        public String getDescription() {
            return description;
        }

        public String toString() {
            return name;
        }

        public int compareTo(SnippetChartItem that) {
            return this.name.compareTo(that.name);
        }

    }

    private class SnippetChartItemListRenderer extends DefaultListCellRenderer {

        private SnippetChartItem item;

        @Override
        public Component getListCellRendererComponent(JList list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            this.item = (SnippetChartItem) value;
            return super.getListCellRendererComponent(list, value, index,
                isSelected, cellHasFocus);
        }

        @Override
        public String getToolTipText() {
            if (item != null)
                return item.getDescription();
            else
                return null;
        }

    }

}
