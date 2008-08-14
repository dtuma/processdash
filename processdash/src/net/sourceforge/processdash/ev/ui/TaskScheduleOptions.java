// Copyright (C) 2008 Tuma Solutions, LLC
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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.ev.EVMetadata;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.ci.EVConfidenceIntervalUtils;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.CheckboxList;
import net.sourceforge.processdash.ui.lib.JLinkLabel;

public class TaskScheduleOptions {

    private TaskScheduleDialog parent;

    private EVTaskList taskList;

    private JDialog dialog;

    private OptionHyperlinkCheckbox histDataOption;

//    private OptionHyperlinkCheckbox estErrorsOption;

    private static final Resources resources = Resources
            .getDashBundle("EV.Options");

    public TaskScheduleOptions(TaskScheduleDialog parent) {
        this.parent = parent;
        this.taskList = parent.model;

        String taskListName = taskList.getDisplayName();
        String windowTitle = resources.format("Window_Title_FMT", taskListName);
        this.dialog = new JDialog(parent.frame, windowTitle, false);
        dialog.setLocationRelativeTo(parent.frame);

        Box content = Box.createVerticalBox();
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        content.add(makeRow(resources.getString("Forecast_Ranges.Prompt")));

        content.add(makeRow(INDENT, new OptionCheckbox(
                "Forecast_Ranges.Current_Plan",
                EVMetadata.Forecast.Ranges.USE_CURRENT_PLAN, true)));

        content.add(makeRow(INDENT,
            histDataOption = new OptionHyperlinkCheckbox(
                    "Forecast_Ranges.Historical_Data",
                    EVMetadata.Forecast.Ranges.USE_HIST_DATA, false,
                    "editHistoricalData")));

        // content.add(makeRow(INDENT,
        // estErrorsOption = new OptionHyperlinkCheckbox(
        // "Forecast_Ranges.Estimating_Errors",
        // EVMetadata.Forecast.Ranges.USE_EST_ERRORS, false,
        // "editEstimatingErrors")));

        content.add(Box.createVerticalStrut(5));

        JButton closeButton = new JButton(resources.getString("Close"));
        closeButton.addActionListener(EventHandler.create(ActionListener.class,
            dialog, "dispose"));
        content.add(makeRow(GLUE, closeButton));


        this.dialog.getContentPane().add(content);
        this.dialog.pack();
        this.dialog.setVisible(true);
    }

    public void show() {
        dialog.setVisible(true);
        dialog.toFront();
    }

    public void editHistoricalData() {
        String currentHistData = taskList
                .getMetadata(EVMetadata.Forecast.Ranges.SAVED_HIST_DATA);
        List<String> histTaskLists = EVConfidenceIntervalUtils
                .getHistoricalTaskListNames(currentHistData);
        final List<String> newTaskLists = selectTaskLists(dialog, parent.dash,
            taskList.getTaskListName(), histTaskLists);
        if (newTaskLists != null)
            new HistoricalDataAnalyzer(newTaskLists).execute();
    }

    private class HistoricalDataAnalyzer extends SwingWorker<String, Object> {

        List<String> newTaskLists;

        private HistoricalDataAnalyzer(List<String> newTaskLists) {
            this.newTaskLists = newTaskLists;
            dialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }

        protected String doInBackground() throws Exception {
            return EVConfidenceIntervalUtils.makeHistoricalConfidenceIntervals(
                parent.dash, newTaskLists);
        }

        protected void done() {
            try {
                String newHistData = get();
                taskList.setMetadata(
                    EVMetadata.Forecast.Ranges.SAVED_HIST_DATA, newHistData);
                parent.setDirty(true);

                histDataOption.checkbox.setSelected(newHistData != null);
                histDataOption.checkbox.saveSetting();
            } catch (Exception ignore) {
            }
            dialog.setCursor(null);
        }
    }

// public void editEstimatingErrors() {
//        System.out.println("editEstimatingErrors");
//    }

    private class OptionHyperlinkCheckbox extends JPanel implements
            ActionListener {

        private OptionCheckbox checkbox;

        private ActionListener hyperlinkHandler;

        public OptionHyperlinkCheckbox(String resourceKey, String metadataKey,
                boolean defaultValue, String actionMethod) {
            super();
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

            checkbox = new OptionCheckbox(null, metadataKey, defaultValue);
            add(checkbox);
            add(new JLinkLabel(resources.getString(resourceKey), this));

            hyperlinkHandler = EventHandler.create(ActionListener.class,
                TaskScheduleOptions.this, actionMethod);
            checkbox.addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() != checkbox || checkbox.isSelected())
                hyperlinkHandler.actionPerformed(e);
        }
    }

    private class OptionCheckbox extends JCheckBox implements ActionListener {

        private String metadataKey;

        public OptionCheckbox(String resourceKey, String metadataKey,
                boolean defaultValue) {
            super();
            if (resourceKey != null)
                setText(resources.getString(resourceKey));
            setFocusPainted(false);

            this.metadataKey = metadataKey;
            String value = taskList.getMetadata(metadataKey);
            if ("true".equalsIgnoreCase(value))
                setSelected(true);
            else if ("false".equalsIgnoreCase(value))
                setSelected(false);
            else
                setSelected(defaultValue);

            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            saveSetting();
        }

        public void saveSetting() {
            taskList.setMetadata(metadataKey, Boolean.toString(isSelected()));
            parent.setDirty(true);
        }



    }

    private Component makeRow(Object... items) {
        Box result = Box.createHorizontalBox();
        boolean sawGlue = false;
        for (Object o : items) {
            if (o == GLUE) {
                result.add(Box.createHorizontalGlue());
                sawGlue = true;
            } else if (o instanceof Component) {
                result.add((Component) o);
            } else if (o instanceof Integer) {
                result.add(Box.createHorizontalStrut((Integer) o));
            } else if (o instanceof String) {
                result.add(new JLabel((String) o));
            }
        }
        if (!sawGlue)
            result.add(Box.createHorizontalGlue());
        return result;
    }

    private static final int INDENT = 20;

    private static Object GLUE = new Object();


    private static List<String> selectTaskLists(Component parentComponent,
            DashboardContext ctx, String excludeTaskListName,
            Collection<String> preselectedNames) {

        List<TaskListSelection> taskLists = new ArrayList<TaskListSelection>();
        for (String oneName : EVTaskList.findTaskLists(ctx.getData())) {
            if (!oneName.equals(excludeTaskListName))
                taskLists.add(new TaskListSelection(oneName));
        }
        Collections.sort(taskLists);

        if (taskLists.isEmpty()) {
            String title = resources.getString(
                "Forecast_Ranges.Historical_Data.No_Data_Title");
            String[] message = resources.getStrings(
                "Forecast_Ranges.Historical_Data.No_Data_Error");
            JOptionPane.showMessageDialog(parentComponent, message, title,
                JOptionPane.ERROR_MESSAGE);
            return Collections.EMPTY_LIST;
        }

        CheckboxList cbl = new CheckboxList(taskLists.toArray());
        if (preselectedNames != null) {
            for (int i = 0;  i < taskLists.size();  i++) {
                if (preselectedNames.contains(taskLists.get(i).taskListName))
                    cbl.setChecked(i, true);
            }
        }

        JScrollPane sp = new JScrollPane(cbl);
        sp.setPreferredSize(new Dimension(200, 200));
        Object message = new Object[] {
            resources.getString("Forecast_Ranges.Historical_Data.Prompt"), sp };
        if (JOptionPane.showConfirmDialog(parentComponent, message,
            resources.getString("Forecast_Ranges.Historical_Data.Window_Title"),
            JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
            return null;

        Object[] selections = cbl.getCheckedItems();
        List<String> result = new ArrayList<String>(selections.length);
        for (int i = 0; i < selections.length; i++) {
            result.add(((TaskListSelection) selections[i]).taskListName);
        }
        return result;
    }

    private static class TaskListSelection implements
            Comparable<TaskListSelection> {
        String taskListName;

        String displayName;

        public TaskListSelection(String taskListName) {
            this.taskListName = taskListName;
            this.displayName = EVTaskList.cleanupName(taskListName);
        }

        public int compareTo(TaskListSelection that) {
            return this.displayName.compareTo(that.displayName);
        }

        public String toString() {
            return displayName;
        }

    }

}
