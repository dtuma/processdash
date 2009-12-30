// Copyright (C) 2001-2009 Tuma Solutions, LLC
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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.TagData;
import net.sourceforge.processdash.data.util.SimpleDataContext;
import net.sourceforge.processdash.ev.EVHierarchicalFilter;
import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVScheduleFiltered;
import net.sourceforge.processdash.ev.EVTaskFilter;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListRollup;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.http.TinyCGI;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.SwingWorker;
import net.sourceforge.processdash.ui.lib.WrappingText;
import net.sourceforge.processdash.ui.snippet.ConfigurableSnippetWidget;
import net.sourceforge.processdash.ui.snippet.SnippetDefinition;
import net.sourceforge.processdash.ui.snippet.SnippetDefinitionManager;
import net.sourceforge.processdash.ui.snippet.SnippetWidget;
import net.sourceforge.processdash.util.Disposable;


public class TaskScheduleChart extends JFrame
    implements EVTaskList.RecalcListener {

    EVTaskList taskList;
    EVSchedule schedule;
    EVTaskFilter filter;
    Map<String, SnippetChartItem> widgets;
    JList widgetList;
    JPanel displayArea;
    CardLayout cardLayout;
    JComponent configurationButton;
    ConfigurationButtonToggler configurationButtonToggler;
    JDialog configurationDialog;
    SnippetChartItem currentItem;
    DashboardContext ctx;

    static Resources resources = Resources.getDashBundle("EV.Chart");
    static Logger logger = Logger.getLogger(TaskScheduleChart.class.getName());

    public TaskScheduleChart(EVTaskList tl, EVTaskFilter filter,
            DashboardContext ctx) {
        super(formatWindowTitle(tl, filter));
        PCSH.enableHelpKey(this, "UsingTaskSchedule.chart");
        DashboardIconFactory.setWindowIcon(this);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.taskList = tl;
        this.filter = filter;
        if (filter == null)
            schedule = taskList.getSchedule();
        else {
            schedule = new EVScheduleFiltered(tl, filter);
            taskList.addRecalcListener(this);
        }
        this.ctx = ctx;

        boolean isFiltered = (filter != null);
        boolean isRollup = (tl instanceof EVTaskListRollup);
        widgets = getChartWidgets(isFiltered, isRollup);

        cardLayout = new CardLayout(0, 5);
        displayArea = new JPanel(cardLayout);
        displayArea.setMinimumSize(new Dimension(0, 0));
        displayArea.setPreferredSize(new Dimension(400, 300));
        displayArea.add(new JPanel(), " ");

        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            createChooserComponent(), displayArea);
        sp.setOneTouchExpandable(true);
        getContentPane().add(sp);

        createConfigurationButton();

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

    private Map<String, SnippetChartItem> getChartWidgets(
            boolean filterInEffect, boolean isRollup) {
        Map<String, SnippetChartItem> result = new HashMap<String, SnippetChartItem>();

        SimpleDataContext ctx = getContextTags(filterInEffect, isRollup);

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

    private SimpleDataContext getContextTags(boolean filterInEffect,
            boolean isRollup) {
        SimpleDataContext ctx = new SimpleDataContext();
        TagData tag = TagData.getInstance();

        ctx.put(EVSnippetEnvironment.EV_CONTEXT_KEY, tag);
        if (isRollup)
            ctx.put(EVSnippetEnvironment.ROLLUP_EV_CONTEXT_KEY, tag);

        if (filterInEffect) {
            ctx.put(EVSnippetEnvironment.FILTERED_EV_CONTEXT_KEY, tag);
            if (isRollup)
                ctx.put(EVSnippetEnvironment.FILTERED_ROLLUP_EV_CONTEXT_KEY,
                    tag);

        } else {
            ctx.put(EVSnippetEnvironment.UNFILTERED_EV_CONTEXT_KEY, tag);
            if (isRollup)
                ctx.put(EVSnippetEnvironment.UNFILTERED_ROLLUP_EV_CONTEXT_KEY,
                    tag);
        }

        return ctx;
    }

    private JComponent createChooserComponent() {
        ArrayList items = new ArrayList(widgets.values());
        Collections.sort(items);
        JList list = new JList(items.toArray());
        widgetList = list;
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new SnippetChartItemListRenderer());
        list.addListSelectionListener(new ChartSelectionHandler());
        list.setSelectedIndex(getDefaultChartIndex(items));

        return new JScrollPane(list);
    }

    private class ChartSelectionHandler implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            SnippetChartItem newItem = (SnippetChartItem) widgetList
                    .getSelectedValue();
            if (newItem != currentItem) {
                hideConfigurationDialog();
                currentItem = newItem;
                currentItem.display();
                maybeReopenConfigurationDialog();
            }
        }
    }

    private int getDefaultChartIndex(List items) {
        String defaultChartId = PREFS.get(DEFAULT_CHART_PREF, null);
        int fallbackItem = 0;
        for (int i = 0;  i < items.size();  i++) {
            SnippetChartItem item = (SnippetChartItem) items.get(i);
            if (item.id.equals(defaultChartId))
                return i;
            else if (item.id.equals(DEFAULT_CHART_ID))
                fallbackItem = i;
        }
        return fallbackItem;
    }

    private void createConfigurationButton() {
        configurationButton = new ConfigurationButton();
        configurationButtonToggler = new ConfigurationButtonToggler();

        JPanel glassPane = new GlassPane();
        SpringLayout l = new SpringLayout();
        glassPane.setLayout(l);

        glassPane.add(configurationButton);
        l.putConstraint(SpringLayout.NORTH, configurationButton, 10,
            SpringLayout.NORTH, glassPane);
        l.putConstraint(SpringLayout.EAST, configurationButton, -8,
            SpringLayout.EAST, glassPane);

        setGlassPane(glassPane);
    }

    private class GlassPane extends JPanel {

        public GlassPane() {
            setOpaque(false);
        }

        @Override
        public boolean contains(int x, int y) {
            x -= configurationButton.getX();
            y -= configurationButton.getY();
            return configurationButton.isVisible()
                && configurationButton.contains(x, y);
        }
    }

    private class ConfigurationButtonToggler extends MouseAdapter implements
            Runnable {
        private boolean armedToHide = false;
        public void mouseEntered(MouseEvent e) {
            if (configurationDialog == null || !configurationDialog.isVisible())
                getGlassPane().setVisible(true);
        }
        public void mouseExited(MouseEvent e) {
            armedToHide = true;
            SwingUtilities.invokeLater(this);
        }
        public void disarmHide() {
            armedToHide = false;
        }
        public void run() {
            if (armedToHide)
                getGlassPane().setVisible(false);
        }
    }

    private class ConfigurationButton extends JLabel implements MouseListener {

        private ImageIcon plainIcon, rolloverIcon;

        public ConfigurationButton() {
            plainIcon = new ImageIcon(TaskScheduleChart.class
                .getResource("chart-options.png"));
            rolloverIcon = new ImageIcon(TaskScheduleChart.class
                .getResource("chart-options-glow.png"));
            setIcon(plainIcon);
            setToolTipText(resources.getString("Configure.Icon_Tooltip"));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setOpaque(false);
            addMouseListener(this);
        }

        public void mouseClicked(MouseEvent e) {
            openConfigurationDialog();
        }

        public void mouseEntered(MouseEvent e) {
            configurationButtonToggler.disarmHide();
            setIcon(rolloverIcon);
        }
        public void mouseExited(MouseEvent e)  {
            setIcon(plainIcon);
        }
        public void mousePressed(MouseEvent e) {}
        public void mouseReleased(MouseEvent e) {}

    }

    private void openConfigurationDialog() {
        getGlassPane().setVisible(false);

        if (configurationDialog != null) {
            configurationDialog.setVisible(true);
            configurationDialog.toFront();
            return;
        }

        Component configPane = currentItem.getConfigurationPane();
        if (configPane == null)
            return;

        JDialog d = new JDialog(this, false);
        JPanel contents = new JPanel(new BorderLayout(10, 10));
        contents.setBorder(new EmptyBorder(10, 10, 10, 10));
        contents.add(configPane);

        d.getContentPane().add(contents);
        if (currentItem.dialogSize != null) {
            d.setSize(currentItem.dialogSize);
            d.setLocation(currentItem.dialogLocation);
        } else {
            d.pack();
        }
        d.setVisible(true);
        configurationDialog = d;
    }

    private void hideConfigurationDialog() {
        if (configurationDialog != null) {
            currentItem.dialogSize = configurationDialog.getSize();
            currentItem.dialogLocation = configurationDialog.getLocation();
            currentItem.isDialogVisible = configurationDialog.isVisible();

            configurationDialog.dispose();
            configurationDialog = null;
        }
    }

    protected void maybeReopenConfigurationDialog() {
        if (currentItem.isDialogVisible)
            openConfigurationDialog();
    }


    @Override
    public void dispose() {
        super.dispose();
        if (currentItem != null)
            PREFS.put(DEFAULT_CHART_PREF, currentItem.id);
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


    private enum ChartItemState { START, INITIALIZING, READY };

    private class SnippetChartItem implements Comparable<SnippetChartItem>,
            ChangeListener {

        private SnippetDefinition snip;

        private String id;

        private String name;

        private String description;

        private ChartItemState state;

        private SnippetWidget widget;

        private Component configurationPane;

        private boolean isDirty;

        private Dimension dialogSize;
        private Point dialogLocation;
        private boolean isDialogVisible;


        public SnippetChartItem(SnippetDefinition snip) {
            this.snip = snip;
            this.id = snip.getId();
            this.name = snip.getName();
            try {
                this.description = snip.getDescription();
            } catch (Exception e) {}
            this.state = ChartItemState.START;
            this.isDirty = false;
        }

        public void display() {
            if (currentItem != this) {
                return;
            } else if (state == ChartItemState.START) {
                new ComponentBuilder().start();
            } else if (state == ChartItemState.READY) {
                cardLayout.show(displayArea, id);
                getGlassPane().setVisible(false);
                TaskScheduleChart.this.setCursor(null);
            }
        }

        private Component getComponent() {
            try {
                SnippetWidget w = snip.getWidget("view", null);
                synchronized(this) {
                    this.widget = w;
                }

                Map environment = new HashMap();
                environment.put(EVSnippetEnvironment.TASK_LIST_KEY, taskList);
                environment.put(EVSnippetEnvironment.SCHEDULE_KEY, schedule);
                environment.put(EVSnippetEnvironment.TASK_FILTER_KEY, filter);
                environment.put(EVSnippetEnvironment.RESOURCES, snip
                        .getResources());
                environment.put(TinyCGI.DASHBOARD_CONTEXT, ctx);
                environment.put(TinyCGI.DATA_REPOSITORY, ctx.getData());
                environment.put(TinyCGI.PSP_PROPERTIES, ctx.getHierarchy());

                HashMap params = new HashMap();

                return w.getWidgetComponent(environment, params);

            } catch (Throwable e) {
                logger.log(Level.SEVERE,
                    "Unexpected error when displaying EV snippet widget with id '"
                            + id + "'", e);
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

        public Component getConfigurationPane() {
            if (configurationPane == null) {
                if (widget instanceof ConfigurableSnippetWidget) {
                    ConfigurableSnippetWidget csw = (ConfigurableSnippetWidget) widget;
                    try {
                        configurationPane = csw.getWidgetConfigurationPane();
                        csw.addChangeListener(this);

                    } catch (Throwable e) {
                        logger.log(Level.SEVERE,
                            "Unexpected error when retrieving configuration " +
                            "pane for widget with id '" + id + "'", e);
                    }
                }
            }
            return configurationPane;
        }

        public String getDescription() {
            return description;
        }

        public String toString() {
            if (isDirty)
                return "* " + name;
            else
                return name;
        }

        public int compareTo(SnippetChartItem that) {
            return this.name.compareTo(that.name);
        }

        private class ComponentBuilder extends SwingWorker {

            public ComponentBuilder() {
                state = ChartItemState.INITIALIZING;
                TaskScheduleChart.this.setCursor(Cursor
                        .getPredefinedCursor(Cursor.WAIT_CURSOR));
            }

            @Override
            public Object construct() {
                return getComponent();
            }

            @Override
            public void finished() {
                Component c = (Component) getValue();
                if (widget instanceof ConfigurableSnippetWidget)
                    c.addMouseListener(configurationButtonToggler);
                displayArea.add(c, id);
                state = ChartItemState.READY;
                display();
            }

        }

        public void stateChanged(ChangeEvent e) {
            setDirty(true);
        }

        public void setDirty(boolean d) {
            if (d != isDirty) {
                isDirty = d;
                widgetList.repaint();
            }
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

    private static final Preferences PREFS = Preferences
            .userNodeForPackage(TaskScheduleChart.class);
    private static final String DEFAULT_CHART_PREF = "TaskScheduleChart.DefaultChartId";
    private static final String DEFAULT_CHART_ID = "pdash.ev.cumValueChart";

}
