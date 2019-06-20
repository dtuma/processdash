// Copyright (C) 2001-2019 Tuma Solutions, LLC
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import net.sourceforge.processdash.ApplicationEventListener;
import net.sourceforge.processdash.ApplicationEventSource;
import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVHierarchicalFilter;
import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVScheduleFiltered;
import net.sourceforge.processdash.ev.EVTaskFilter;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListRollup;
import net.sourceforge.processdash.ev.ui.TaskScheduleChartSettings.PersistenceException;
import net.sourceforge.processdash.ev.ui.TaskScheduleChartUtil.ChartListPurpose;
import net.sourceforge.processdash.ev.ui.chart.EVCharts;
import net.sourceforge.processdash.ev.ui.chart.HelpAwareEvChart;
import net.sourceforge.processdash.ev.ui.icons.TaskScheduleIcons;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.team.group.ui.GroupFilterMenu;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.BoxUtils;
import net.sourceforge.processdash.ui.lib.JOptionPaneActionHandler;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.ui.lib.SwingWorker;
import net.sourceforge.processdash.ui.lib.WrappingText;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;
import net.sourceforge.processdash.ui.snippet.ConfigurableSnippetWidget;
import net.sourceforge.processdash.ui.snippet.SnippetDefinition;
import net.sourceforge.processdash.ui.snippet.SnippetWidget;
import net.sourceforge.processdash.util.Disposable;
import net.sourceforge.processdash.util.StringUtils;


public class TaskScheduleChart extends JFrame
    implements EVTaskList.RecalcListener, ApplicationEventListener {

    EVTaskList taskList;
    EVSchedule schedule;
    EVTaskFilter filter;
    JList chooser;
    WidgetListModel widgetList;
    JPanel displayArea;
    CardLayout cardLayout;
    GroupChangeListener groupChangeHandler;
    JComponent configurationButton;
    ConfigurationButtonToggler configurationButtonToggler;
    ConfigurationDialog configurationDialog;
    SnippetChartItem currentItem;
    DashboardContext ctx;

    static Resources resources = Resources.getDashBundle("EV.Chart");
    static Logger logger = Logger.getLogger(TaskScheduleChart.class.getName());

    public TaskScheduleChart(EVTaskList tl, EVTaskFilter filter,
            GroupFilterMenu groupMenu, DashboardContext ctx) {
        super(formatWindowTitle(tl, filter));
        DashboardIconFactory.setWindowIcon(this);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                confirmClose();
            }});
        this.taskList = tl;
        taskList.addRecalcListener(this);
        this.filter = filter;
        if (filter == null) {
            schedule = taskList.getSchedule();
        } else {
            schedule = new EVScheduleFiltered(tl, filter);
        }
        this.ctx = ctx;
        if (ctx instanceof ApplicationEventSource)
            ((ApplicationEventSource) ctx).addApplicationEventListener(this);

        boolean isFiltered = (filter != null);
        boolean isRollup = (tl instanceof EVTaskListRollup);
        Map<String, SnippetChartItem> widgets = getChartWidgets(tl, ctx
                .getData(), isFiltered, isRollup);

        cardLayout = new CardLayout(0, 0);
        displayArea = new JPanel(cardLayout);
        displayArea.setMinimumSize(new Dimension(0, 0));
        displayArea.setPreferredSize(new Dimension(400, 300));
        displayArea.add(new JPanel(), " ");

        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            createChooserComponent(widgets), displayArea);
        sp.setOneTouchExpandable(true);
        getContentPane().add(sp);

        if (isRollup && groupMenu != null)
            groupChangeHandler = new GroupChangeListener(groupMenu);

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

    private Map<String, SnippetChartItem> getChartWidgets(EVTaskList tl,
            DataRepository data, boolean filterInEffect, boolean isRollup) {

        Map<String, SnippetChartItem> result = new HashMap<String, SnippetChartItem>();

        List<TaskScheduleChartUtil.ChartItem> chartItems =
            TaskScheduleChartUtil.getChartsForTaskList(tl.getID(), data,
                    filterInEffect, isRollup, false, false, false, false,
                    ChartListPurpose.ChartWindow);
        for (TaskScheduleChartUtil.ChartItem oneChart : chartItems) {
            try {
                SnippetChartItem item = new SnippetChartItem(oneChart.snip);
                if (oneChart.settings != null) {
                    item.settings = oneChart.settings;
                    item.id = oneChart.settings.getSettingsIdentifier();
                    item.name = oneChart.name;
                }
                result.put(item.id, item);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Problem with EV Chart Snippet '"
                        + oneChart.snip.getId() + "'", e);
            }
        }

        return result;
    }

    private JComponent createChooserComponent(
            Map<String, SnippetChartItem> widgets) {
        ArrayList items = new ArrayList(widgets.values());
        Collections.sort(items);
        widgetList = new WidgetListModel(items);

        JList list = new JList(widgetList);
        chooser = list;
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new SnippetChartItemListRenderer());
        list.addListSelectionListener(new ChartSelectionHandler());
        list.setSelectedIndex(getDefaultChartIndex(items));

        list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
            "deleteChart");
        list.getActionMap().put("deleteChart", new DeleteChartAction());

        return new JScrollPane(list);
    }

    private void configureHelp(SnippetWidget widget) {
        String helpId = null;
        if (widget instanceof HelpAwareEvChart) {
            HelpAwareEvChart helpAware = (HelpAwareEvChart) widget;
            helpId = helpAware.getHelpId();
        }
        if (!StringUtils.hasValue(helpId))
            helpId = "TaskScheduleCharts";
        PCSH.enableHelpKey(this, helpId);
    }

    private class ChartSelectionHandler implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            SnippetChartItem newItem = (SnippetChartItem) chooser
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

    private class GroupChangeListener implements TableModelListener {

        private GroupFilterMenu groupMenu;

        private String windowBaseTitle;

        GroupChangeListener(GroupFilterMenu groupMenu) {
            this.groupMenu = groupMenu;
            this.windowBaseTitle = getTitle();
            schedule.addTableModelListener(this);
            tableChanged(null);
        }

        @Override
        public void tableChanged(TableModelEvent e) {
            setTitle(windowBaseTitle + " - " + groupMenu.getSelectedItem());
        }

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

        private Icon plainIcon, rolloverIcon;

        public ConfigurationButton() {
            plainIcon = TaskScheduleIcons.chartOptionsIcon();
            rolloverIcon = TaskScheduleIcons.chartOptionsGlowIcon();
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

        if (currentItem.getConfigurationPane() == null)
            return;

        configurationDialog = new ConfigurationDialog(currentItem);
        configurationDialog.setVisible(true);
    }

    private void hideConfigurationDialog() {
        if (configurationDialog != null) {
            configurationDialog.hideDialog();
            configurationDialog = null;
        }
    }

    protected void maybeReopenConfigurationDialog() {
        if (currentItem.isDialogVisible)
            openConfigurationDialog();
    }

    private class ConfigurationDialog extends JDialog {

        private SnippetChartItem item;
        private List<AbstractChartItemAction> actions;

        public ConfigurationDialog(SnippetChartItem item) {
            super(TaskScheduleChart.this, false);
            this.item = item;
            setupTitle();

            JPanel contents = new JPanel(new BorderLayout(10, 10));
            contents.setBorder(new EmptyBorder(10, 10, 10, 10));
            contents.add(item.getConfigurationPane());
            contents.add(buildButtons(), BorderLayout.SOUTH);
            getContentPane().add(contents);

            if (item.dialogSize != null) {
                setSize(item.dialogSize);
                setLocation(item.dialogLocation);
            } else {
                pack();
            }
        }

        public void setupTitle() {
            String title;
            if (item.settings != null && item.settings.isGlobal()) {
                title = resources.format("Configure.Dialog_Title.Global_FMT",
                    item.name);
            } else {
                title = resources.format("Configure.Dialog_Title.Local_FMT",
                    item.name, taskList.getDisplayName());
            }
            setTitle(title);
        }

        public void handleDirtyStateChange() {
            MacGUIUtils.setDirty(this, item.isDirty);
            for (AbstractChartItemAction action : actions)
                action.dirtyStateChanged();
        }

        public void hideDialog() {
            item.dialogSize = getSize();
            item.dialogLocation = getLocation();
            item.isDialogVisible = isVisible();
            dispose();
        }

        private Component buildButtons() {
            actions = new ArrayList<AbstractChartItemAction>();

            JButton revertButton = new JButton(new RevertSettingsAction(item,
                    actions));
            JButton saveButton = new JButton(new SaveSettingsAction(item,
                    actions));
            JButton saveAsButton = new JButton(new SaveSettingsAsAction(item,
                    actions));

            return BoxUtils.hbox(BoxUtils.GLUE, revertButton, BoxUtils.GLUE,
                saveButton, BoxUtils.GLUE, saveAsButton, BoxUtils.GLUE);
        }

    }

    private void handleDirtyStateChange(boolean oneKnownDirty) {
        if (configurationDialog != null)
            configurationDialog.handleDirtyStateChange();
        boolean anyDirty = oneKnownDirty || !getDirtyCharts().isEmpty();
        MacGUIUtils.setDirty(this, anyDirty);
    }

    public void handleApplicationEvent(ActionEvent e) {
        if (APP_EVENT_SAVE_ALL_DATA.equals(e.getActionCommand())) {
            saveRevertOrCancel(true);
        }
    }


    protected void confirmClose() {
        if (saveRevertOrCancel(false))
            dispose();
    }


    private boolean saveRevertOrCancel(boolean isAppEvent) {
        List<SnippetChartItem> dirtyCharts = getDirtyCharts();
        if (dirtyCharts.isEmpty())
            return true;

        String title = resources.getString("Configure.Confirm.Window_Title");
        String header = resources.getString("Configure.Confirm.Header_Msg");
        JList dirtyChartList = new JList(dirtyCharts.toArray());
        dirtyChartList.setBorder(new BevelBorder(BevelBorder.LOWERED));
        String footer = resources.getString("Configure.Confirm.Footer_Msg");
        Object[] message = { header, dirtyChartList, footer };
        String saveOption = resources.getString("Configure.Confirm.Save_All");
        String revertOption = resources.getString("Configure.Confirm.Discard_All");
        String cancelOption = resources.getString("Cancel");
        Object[] options;
        if (isAppEvent)
            options = new Object[] { saveOption, revertOption };
        else
            options = new Object[] { saveOption, revertOption, cancelOption };

        switch (JOptionPane.showOptionDialog(this, message, title, 0,
            JOptionPane.QUESTION_MESSAGE, null, options, saveOption)) {

        case 0: // save all
            for (SnippetChartItem chart : dirtyCharts)
                chart.saveSettings();
            return true;

        case 1: // discard
            if (isAppEvent)
                for (SnippetChartItem chart : dirtyCharts)
                    chart.revertSettings();
            return true;

        default:
            return false;
        }
    }

    private List<SnippetChartItem> getDirtyCharts() {
        List<SnippetChartItem> result = new ArrayList<SnippetChartItem>();
        for (int i = widgetList.getSize(); i-- > 0;) {
            SnippetChartItem chart = (SnippetChartItem) widgetList.get(i);
            if (chart.isDirty)
                result.add(chart);
        }
        return result;
    }

    private TaskScheduleChartSettings showSaveAsDialog(
            TaskScheduleChartSettings currentSettings, String chartID) {
        String title = resources.getString("Configure.Save_As.Window_Title");

        String namePrompt = resources
                .getString("Configure.Save_As.Name_Prompt");
        JTextField chartNameField = new JTextField();
        if (currentSettings != null)
            chartNameField.setText(currentSettings.getCustomName());
        new JOptionPaneActionHandler().install(chartNameField);

        String scopePrompt = resources
                .getString("Configure.Save_As.Scope_Prompt");
        ButtonGroup group = new ButtonGroup();
        JRadioButton localScopeButton = new JRadioButton(resources
                .getString("Configure.Save_As.Scope_Local"));
        group.add(localScopeButton);
        JRadioButton globalScopeButton = new JRadioButton(resources
                .getString("Configure.Save_As.Scope_Global"));
        group.add(globalScopeButton);

        boolean global = (currentSettings != null && currentSettings.isGlobal());
        (global ? globalScopeButton : localScopeButton).setSelected(true);

        String errorKey = null;

        while (true) {
            BoxUtils errorBox;
            if (errorKey != null) {
                JLabel errorLabel = new JLabel(resources.getString(errorKey));
                errorLabel.setForeground(Color.RED);
                errorBox = BoxUtils.hbox(20, errorLabel);
            } else {
                errorBox = BoxUtils.hbox();
            }

            Object[] message = {
                    namePrompt,
                    BoxUtils.hbox(20, chartNameField),
                    new JOptionPaneTweaker.GrabFocus(chartNameField),
                    errorBox,
                    scopePrompt,
                    BoxUtils.hbox(20, localScopeButton),
                    BoxUtils.hbox(20, globalScopeButton) };
            int userChoice = JOptionPane.showConfirmDialog(configurationDialog,
                message, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
            if (userChoice != JOptionPane.OK_OPTION)
                return null;

            String chartName = chartNameField.getText();
            chartName = (chartName == null ? "" : chartName.trim());
            if (chartName.length() == 0) {
                errorKey = "Configure.Save_As.Name_Missing";
                continue;
            }

            TaskScheduleChartSettings result = new TaskScheduleChartSettings();
            if (localScopeButton.isSelected())
                result.setTaskListID(taskList.getID());
            result.setChartID(chartID);
            result.setCustomName(chartName);

            if (result.hasSameNameAs(currentSettings) == false
                    && chartNameIsTaken(chartName)) {
                errorKey = "Configure.Save_As.Duplicate_Name";
                continue;
            }

            return result;
        }
    }

    private boolean chartNameIsTaken(String name) {
        for (int i = widgetList.getSize(); i-- > 0;) {
            SnippetChartItem chart = (SnippetChartItem) widgetList.get(i);
            if (chart.name.equalsIgnoreCase(name))
                return true;
        }
        return false;
    }


    @Override
    public void dispose() {
        super.dispose();
        if (currentItem != null && currentItem.id.indexOf('#') == -1)
            PREFS.put(DEFAULT_CHART_PREF, currentItem.id);
        for (int i = displayArea.getComponentCount();  i-- > 0; ) {
            Component c = displayArea.getComponent(i);
            if (c instanceof Disposable)
                ((Disposable) c).dispose();
        }
        widgetList.dispose();
        if (groupChangeHandler != null)
            schedule.removeTableModelListener(groupChangeHandler);
        taskList.removeRecalcListener(this);
        if (ctx instanceof ApplicationEventSource)
            ((ApplicationEventSource) ctx).removeApplicationEventListener(this);
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

    private void maybeDispose(Object obj) {
        if (obj instanceof Disposable)
            ((Disposable) obj).dispose();
    }

    private class WidgetListModel extends DefaultListModel {
        public WidgetListModel(List items) {
            for (Object item : items)
                addElement(item);
        }
        public void itemChanged(Object item) {
            int pos = indexOf(item);
            if (pos != -1)
                fireContentsChanged(this, pos, pos);
        }
        public void dispose() {
            for (Object item : toArray())
                maybeDispose(item);
        }
    }

    private enum ChartItemState { START, INITIALIZING, READY };

    private class SnippetChartItem implements Comparable<SnippetChartItem>,
            ChangeListener, Disposable {

        private SnippetDefinition snip;

        private String id;

        private String name;

        private String description;

        private TaskScheduleChartSettings settings;

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
                TaskScheduleChart.this.configureHelp(widget);
            }
        }

        private Component getComponent() {
            try {
                SnippetWidget w = snip.getWidget("view", null);
                synchronized(this) {
                    this.widget = w;
                }

                Map environment = TaskScheduleChartUtil.getEnvironment(
                    taskList, schedule, filter, snip, ctx);
                Map params = TaskScheduleChartUtil.getParameters(settings);

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
                TaskScheduleChart.this.configureHelp(null);
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
                if (c instanceof JComponent) {
                    JComponent jc = (JComponent) c;
                    if (jc.getBorder() == null)
                        jc.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
                }
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
                widgetList.itemChanged(this);
                handleDirtyStateChange(isDirty);
            }
        }

        public void revertSettings() {
            if (!(widget instanceof ConfigurableSnippetWidget))
                return;

            ConfigurableSnippetWidget csw = (ConfigurableSnippetWidget) widget;
            Map params = Collections.EMPTY_MAP ;
            try {
                if (settings != null)
                    params = settings.getParameters();
            } catch (PersistenceException e) {}
            csw.setConfigurationParameters(params);
            setDirty(false);
        }

        public void saveSettings() {
            if (!(widget instanceof ConfigurableSnippetWidget))
                return;

            ConfigurableSnippetWidget csw = (ConfigurableSnippetWidget) widget;
            if (settings == null) {
                settings = new TaskScheduleChartSettings();
                settings.setChartID(snip.getId());
                settings.setTaskListID(taskList.getID());
            }

            Map params = csw.getConfigurationParameters();
            settings.setParameters(params);
            settings.setChartVersion(snip.getVersion());
            settings.save(ctx.getData());
            setDirty(false);
        }

        public void doSaveSettingsAs() {
            if (!(widget instanceof ConfigurableSnippetWidget))
                return;

            TaskScheduleChartSettings dest = showSaveAsDialog(settings,
                snip.getId());
            if (dest == null)
                return;

            if (settings == null || !dest.hasSameNameAs(settings)) {
                // the name has changed.  Save this as a new chart.
                saveAsNewChart(dest);

            } else if (dest.hasSameScopeAs(settings) == false) {
                // the name is the same, but the scope has changed.
                saveAsNewScope(dest);

            } else {
                // the name and scope have not changed. Perform a plain "save"
                saveSettings();
            }
        }

        private void saveAsNewChart(TaskScheduleChartSettings dest) {
            SnippetChartItem orig = new SnippetChartItem(this.snip);
            orig.settings = this.settings;
            orig.name = this.name;
            orig.id = this.id + "#" + System.currentTimeMillis();

            int pos = widgetList.indexOf(this);
            widgetList.add(pos, orig);

            this.settings = dest;
            this.name = dest.getCustomName();
            widgetList.itemChanged(this);

            ConfigurableSnippetWidget csw = (ConfigurableSnippetWidget) widget;
            Map params = new HashMap(csw.getConfigurationParameters());
            params.put(EVSnippetEnvironment.EV_CUSTOM_SNIPPET_NAME_KEY, this.name);
            csw.setConfigurationParameters(params);

            saveSettings();

            if (currentItem == this && configurationDialog != null)
                configurationDialog.setupTitle();
        }

        private void saveAsNewScope(TaskScheduleChartSettings dest) {
            if (this.settings != null)
                this.settings.delete(ctx.getData());

            this.settings = dest;
            saveSettings();

            if (currentItem == this && configurationDialog != null)
                configurationDialog.setupTitle();
        }

        public void maybeDelete() {
            if (settings == null || settings.getCustomName() == null) {
                // do not allow the user to delete built-in charts.
                Toolkit.getDefaultToolkit().beep();
                return;
            }

            String title = resources.getString("Configure.Delete_Chart.Title");
            String prompt = resources.format(
                "Configure.Delete_Chart.Prompt_FMT", name);
            int userChoice = JOptionPane.showConfirmDialog(
                TaskScheduleChart.this, prompt, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (userChoice != JOptionPane.YES_OPTION)
                return;

            // delete this chart definition from the data repository.
            settings.delete(ctx.getData());

            // select a different chart.
            int pos = widgetList.indexOf(this);
            int posToSelect = (pos > 0 ? pos-1 : pos+1);
            chooser.setSelectedIndex(posToSelect);

            // remove this chart from the list.
            widgetList.removeElement(this);
        }

        public void dispose() {
            maybeDispose(widget);
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

    private abstract class AbstractChartItemAction extends AbstractAction {
        SnippetChartItem item;
        boolean enableIfDirty;
        protected AbstractChartItemAction(SnippetChartItem item,
                String nameResKey, boolean enableIfDirty, List storeTo) {
            super(resources.getString(nameResKey));
            this.item = item;
            this.enableIfDirty = enableIfDirty;
            if (storeTo != null)
                storeTo.add(this);
            dirtyStateChanged();
        }
        public void dirtyStateChanged() {
            if (enableIfDirty) {
                setEnabled(item.isDirty);
            }
        }
    }

    private class RevertSettingsAction extends AbstractChartItemAction {

        public RevertSettingsAction(SnippetChartItem item, List storeTo) {
            super(item, "Revert", true, storeTo);
        }

        public void actionPerformed(ActionEvent e) {
            item.revertSettings();
        }

    }

    private class SaveSettingsAction extends AbstractChartItemAction {

        public SaveSettingsAction(SnippetChartItem item, List storeTo) {
            super(item, "Save", true, storeTo);
        }

        public void actionPerformed(ActionEvent e) {
            item.saveSettings();
        }

    }

    private class SaveSettingsAsAction extends AbstractChartItemAction {

        public SaveSettingsAsAction(SnippetChartItem item, List storeTo) {
            super(item, "Configure.Save_As.Button", false, storeTo);
        }

        public void actionPerformed(ActionEvent e) {
            item.doSaveSettingsAs();
        }

    }

    private class DeleteChartAction extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            currentItem.maybeDelete();
        }

    }

    private static final Preferences PREFS = Preferences
            .userNodeForPackage(TaskScheduleChart.class);
    private static final String DEFAULT_CHART_PREF = "TaskScheduleChart.DefaultChartId";
    private static final String DEFAULT_CHART_ID = EVCharts.Value.ID;

}
