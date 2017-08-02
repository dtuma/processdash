// Copyright (C) 2007-2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.border.Border;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.repository.RemoteException;
import net.sourceforge.processdash.data.util.TopDownBottomUpJanitor;
import net.sourceforge.processdash.ev.EVForecastDateCalculators;
import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.time.TimeLoggingModel;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.ui.lib.LevelIndicator;
import net.sourceforge.processdash.ui.lib.SwingWorker;
import net.sourceforge.processdash.ui.lib.ToolTipTimingCustomizer;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.ThreadThrottler;

public class PercentSpentIndicator extends JPanel implements DataListener,
        PropertyChangeListener, Runnable {

    private DashboardContext dashCtx;

    private TimeLoggingModel timingModel;

    private ActiveTaskModel activeTaskModel;

    private CardLayout layout;

    private LevelIndicator levelIndicator;

    private Border plainBorder, rolloverBorder;

    private String currentTaskPath = null;
    private boolean shouldBeVisible = true;

    private boolean isInEVSchedule = false;
    private String forecastDateHTML = null;

    private String estTimeDataName = null;
    private double estTime = Double.NaN;
    private boolean estTimeEditable = false;

    private String actTimeDataName = null;
    private double actTime = Double.NaN;

    private String actDateDateName = null;
    private SimpleData actDate = null;


    private static final Resources resources = Resources
            .getDashBundle("ProcessDashboard.PctSpent");

    // private static final Color[] UNDERSPENT = { new Color(0, 200, 0),
    //     new Color(128, 255, 128) }; // this creates a green bar
    private static final Color[] UNDERSPENT = { new Color(32, 180, 226),
        new Color(194, 224, 237) }; // this is an aqua-blue bar
    private static final Color[] OVERSPENT = { new Color(200, 0, 0),
        new Color(255, 60, 60) }; // this is a red bar

    private static final String LEVEL_INDICATOR_KEY = "level";
    private static final String MISSING_ESTIMATE_KEY = "noEst";


    public PercentSpentIndicator(DashboardContext dashCtx,
            TimeLoggingModel timingModel) {
        this.dashCtx = dashCtx;
        this.timingModel = timingModel;
        this.activeTaskModel = timingModel.getActiveTaskModel();

        buildUI();

        timingModel.addPropertyChangeListener(this);
        for (String s : ACTIVE_SETTINGS)
            InternalSettings.addPropertyChangeListener(s, this);
        setTaskPath(activeTaskModel.getPath());
    }


    private void buildUI() {
        layout = new CardLayout();
        setLayout(layout);

        levelIndicator = new LevelIndicator(true);
        levelIndicator.setPaintBarRect(false);
        add(levelIndicator, LEVEL_INDICATOR_KEY);

        Icon hourglassIcon = DashboardIconFactory.getHourglassIcon();
        JLabel label = new JLabel(hourglassIcon);
        add(label, MISSING_ESTIMATE_KEY);

        plainBorder = BorderFactory
                .createLineBorder(UIManager.getColor("controlDkShadow"));
        rolloverBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xB8CFE5)), plainBorder);
        setBorder(plainBorder);

        if (!Settings.isReadOnly()) {
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    setRollover(false);
                    showEditEstimateDialog();
                }
                public void mouseEntered(MouseEvent e) {
                    setRollover(true);
                }
                public void mouseExited(MouseEvent e) {
                    setRollover(false);
                }
            });
        }

        Dimension d = new Dimension(hourglassIcon.getIconWidth() + 4,
                hourglassIcon.getIconHeight() + 6);
        setMinimumSize(d);
        setPreferredSize(d);

        new ToolTipTimingCustomizer(750, 60000).install(this);
    }


    private void setRollover(boolean rollover) {
        setBorder(rollover ? rolloverBorder : plainBorder);
    }

    public void dataValuesChanged(Vector v) throws RemoteException {
        for (int i = v.size(); i-- > 0;)
            dataValueChanged((DataEvent) v.get(i));
    }

    public void dataValueChanged(DataEvent e) throws RemoteException {
        forecastDateHTML = null;
        String name = e.getName();
        SimpleData value = e.getValue();
        if (name.equals(estTimeDataName)) {
            estTime = getDouble(value, Double.NaN);
            estTimeEditable = (value == null || value.isEditable())
                    && Settings.isReadWrite();
            recalc();
        } else if (name.equals(actTimeDataName)) {
            actTime = getDouble(value, 0.0);
            recalc();
        } else if (name.equals(actDateDateName)) {
            actDate = value;
            recalc();
        }
    }

    private double getDouble(SimpleData value, double defVal) {
        if (value instanceof NumberData)
            return ((NumberData) value).getDouble();
        else
            return defVal;
    }

    private void recalc() {
        double pctSpent = actTime / estTime;
        String estTimeStr = FormatUtil.formatTime(estTime, true);
        String actTimeStr = FormatUtil.formatTime(actTime, true);
        StringBuffer tip = new StringBuffer();

        append(tip, "<html><body><b>${Estimated_Time_Label}</b> ");
        if (estTime > 0) {
            if (pctSpent <= 1.0) {
                levelIndicator.setLevel(1 - pctSpent);
                levelIndicator.setBarColors(UNDERSPENT[0], UNDERSPENT[1]);
            } else {
                double maxPct =
                    Settings.getInt("pctSpent.maxOverPct", 200) / 100.0;
                levelIndicator.setLevel((pctSpent - 1) / (maxPct - 1));
                levelIndicator.setBarColors(OVERSPENT[0], OVERSPENT[1]);
            }
            layout.show(this, LEVEL_INDICATOR_KEY);
            tip.append(estTimeStr);
        } else {
            layout.show(this, MISSING_ESTIMATE_KEY);
            append(tip, "<i>${None}</i>");
        }
        if (estTimeEditable)
            append(tip, " <i>${Click_To_Edit}</i>");

        append(tip, "<br><b>${Actual_Time_Label}</b> ");
        tip.append(actTimeStr);

        if (estTime > 0)
            append(tip, "<br><b>${Percent_Spent_Label}</b> ").append(
                    FormatUtil.formatPercent(pctSpent));

        if (forecastDateHTML != null)
            tip.append("<hr>").append(forecastDateHTML);
        else if (isInEVSchedule && actDate == null)
            append(tip, "<hr><i>${Calculating_Forecast_Date}</i>");

        tip.append("</body></html>");
        setToolTipText(tip.toString());

        // possibly update the title bar of the window, if user settings have
        // requested that.
        if (dashCtx instanceof JFrame) {
            JFrame f = (JFrame) dashCtx;
            String windowTitle = f.getTitle();
            int pos = windowTitle.indexOf(SPACER);
            if (pos != -1)
                windowTitle = windowTitle.substring(0, pos);
            if (Settings.getBool(SHOW_TIME_IN_TITLE, false)) {
                String resKey = (estTime > 0 ? "Window_Title.Time_Metrics_FMT"
                        : "Window_Title.Time_Metrics_Short_FMT");
                windowTitle += SPACER + resources.format(resKey,
                    actTimeStr, estTimeStr, pctSpent);
            }
            if (Settings.getBool(SHOW_PLAY_IN_TITLE, false)
                    && timingModel.isPaused() == false) {
                windowTitle += SPACER + resources.getString( //
                    "Window_Title.Timing_Indicator");
            }
            f.setTitle(windowTitle);
        }
    }

    private StringBuffer append(StringBuffer buf, String interpText) {
        return buf.append(resources.interpolate(interpText,
                HTMLUtils.ESC_ENTITIES));
    }


    public void propertyChange(PropertyChangeEvent evt) {
        setTaskPath(activeTaskModel.getPath());
        SwingUtilities.invokeLater(this);
    }

    public void run() {
        recalc();
    }


    private void setTaskPath(String newTaskPath) {
        if (newTaskPath != null && newTaskPath.equals(currentTaskPath))
            return;

        boolean newShouldBeVisible = shouldBeVisible(newTaskPath);

        String[] elemsToRemove = null;
        String[] elemsToAdd = null;

        // Use a synchronized block to make atomic changes to our internal state
        synchronized (this) {
            if (currentTaskPath != null)
                elemsToRemove = new String[] { estTimeDataName,
                        actTimeDataName, actDateDateName };

            estTimeDataName = actTimeDataName = actDateDateName = null;
            actTime = 0;
            estTime = Double.NaN;
            estTimeEditable = isInEVSchedule = false;
            forecastDateHTML = null;
            actDate = null;

            currentTaskPath = newTaskPath;
            shouldBeVisible = newShouldBeVisible;

            if (currentTaskPath != null && shouldBeVisible) {
                estTimeDataName = getDataName("Estimated Time");
                actTimeDataName = getDataName("Time");
                actDateDateName = getDataName("Completed");
                elemsToAdd = new String[] { estTimeDataName, actTimeDataName,
                        actDateDateName };
            }
        }

        // Outside of the synchronized block, interact with the data repository
        // to update our data listener registrations.
        List taskLists = null;
        if (shouldBeVisible)
            taskLists = EVTaskList.getTaskListNamesForPath(dashCtx.getData(),
                currentTaskPath);
        isInEVSchedule = (taskLists != null && !taskLists.isEmpty());

        if (elemsToRemove != null) {
            for (int i = 0; i < elemsToRemove.length; i++) {
                if (elemsToRemove[i] != null)
                    dashCtx.getData()
                            .removeDataListener(elemsToRemove[i], this);
            }
        }

        if (elemsToAdd != null) {
            for (int i = 0; i < elemsToAdd.length; i++) {
                if (elemsToAdd[i] != null)
                    dashCtx.getData().addDataListener(elemsToAdd[i], this);
            }
        }

        setVisible(shouldBeVisible);
    }


    private boolean shouldBeVisible(String path) {
        if (path == null)
            return false;

        SaveableData enabledVal = dashCtx.getData().getInheritableValue(path,
                ENABLED_DATA_NAME);
        if (enabledVal == null)
            return Settings.getBool(ENABLED_SETTING_NAME, true);

        SimpleData enabled = enabledVal.getSimpleValue();
        return (enabled != null && enabled.test());
    }


    private String getDataName(String elemName) {
        return DataRepository.createDataName(currentTaskPath, elemName);
    }


    MouseEvent lastMouseEvent = null;

    public Point getToolTipLocation(MouseEvent event) {
        lastMouseEvent = event;
        return super.getToolTipLocation(event);
    }


    public JToolTip createToolTip() {
        JToolTip result = super.createToolTip();
        if (isInEVSchedule && actDate == null && forecastDateHTML == null)
            new EVDateRecalculator(result).start();
        return result;
    }


    private void showEditEstimateDialog() {
        if (currentTaskPath == null || estTimeDataName == null
                || estTimeEditable == false)
            return;

        ToolTipManager.sharedInstance().mouseExited(
                new MouseEvent(this, MouseEvent.MOUSE_EXITED, System
                        .currentTimeMillis(), 0, 0, -1, 0, false));

        final JTextField estimate = new JTextField();
        if (estTime > 0 && !Double.isInfinite(estTime))
            estimate.setText(FormatUtil.formatTime(estTime));

        String prompt = resources.format("Edit_Dialog.Prompt_FMT",
                currentTaskPath);

        JPanel p = new JPanel(new GridLayout(2, 2, 10, 2));
        p.add(new JLabel(resources.getString("Actual_Time_Label"),
                JLabel.TRAILING));
        p.add(new JLabel(FormatUtil.formatTime(actTime)));
        p.add(new JLabel(resources.getString("Estimated_Time_Label"),
                JLabel.TRAILING));
        p.add(estimate);

        String title = resources.getString("Edit_Dialog.Title");
        Object message = new Object[] { prompt, p,
                new JOptionPaneTweaker.GrabFocus(estimate) };

        while (true) {
            if (JOptionPane.showConfirmDialog(this, message, title,
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
                return;
            SimpleData newEstimate;
            String userInput = estimate.getText();
            if (userInput == null || userInput.trim().length() == 0) {
                newEstimate = null;
            } else {
                long l = FormatUtil.parseTime(userInput.trim());
                if (l < 0) {
                    estimate.setBackground(new Color(255, 200, 200));
                    estimate.setToolTipText(resources
                            .getString("Edit_Dialog.Invalid_Time"));
                    continue;
                }
                newEstimate = new DoubleData(l, true);
            }
            dashCtx.getData().userPutValue(estTimeDataName, newEstimate);
            EST_TIME_JANITOR.cleanup(dashCtx);
            return;
        }
    }

    private class EVDateRecalculator extends SwingWorker {
        private JToolTip toolTip;

        public EVDateRecalculator(JToolTip toolTip) {
            this.toolTip = toolTip;
        }

        public Object construct() {
            ThreadThrottler.beginThrottling();
            long startTime = System.currentTimeMillis();
            forecastDateHTML = resources.interpolate(
                    "<i>${Calculating_Forecast_Date}</i>",
                    HTMLUtils.ESC_ENTITIES);
            Map dates = EVForecastDateCalculators.getForecastDates(dashCtx,
                    currentTaskPath);
            if (dates == null) {
                isInEVSchedule = false;
                forecastDateHTML = null;
            } else {
                forecastDateHTML = buildForecastDateHTML(dates);
            }

            // If the calculation happens very quickly, the tooltip flashes
            // and the user would think, "Wait - was did it just say? I missed
            // it."  Give them a fraction of a second to read the initial text
            // before we change it.
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed < MIN_DELAY)
                try {
                    Thread.sleep(MIN_DELAY - elapsed);
                } catch (InterruptedException e) {}

            return null;
        }

        private static final int MIN_DELAY = 500;

        private String buildForecastDateHTML(Map dates) {
            StringBuffer result = new StringBuffer();
            Set uniqueDates = new HashSet(dates.values());
            if (uniqueDates.isEmpty()) {
                append(result, "<b>${Forecast_Date_Label}</b> <i>${None}</i>");

            } else if (uniqueDates.size() == 1) {
                Date forecast = (Date) uniqueDates.iterator().next();
                append(result, "<b>${Forecast_Date_Label}</b> ").append(
                        EVSchedule.formatDate(forecast));
            } else {
                // calculate the values to display.
                Map html = new TreeMap();
                for (Iterator i = dates.entrySet().iterator(); i.hasNext();) {
                    Map.Entry e = (Map.Entry) i.next();
                    String taskListName = (String) e.getKey();
                    String taskListHtml = HTMLUtils.escapeEntities(EVTaskList
                            .cleanupName(taskListName));
                    Date forecast = (Date) e.getValue();
                    String forecastHtml = EVSchedule.formatDate(forecast);
                    html.put(taskListHtml, forecastHtml);
                }

                // now build the HTML with those values
                append(result, "<b>${Forecast_Date_Label}</b>");
                for (Iterator i = html.entrySet().iterator(); i.hasNext();) {
                    Map.Entry e = (Map.Entry) i.next();
                    result.append("<br>&nbsp;&nbsp;&nbsp;<b>").append(
                            e.getKey()).append(":</b> ").append(e.getValue());
                }
            }
            return result.toString();
        }


        public void finished() {
            // Update the tooltip text, and refresh the JToolTip.
            recalc();
            if (toolTip.isShowing() && lastMouseEvent != null)
                // This is an ugly way to get the tooltip to update itself.
                // It would be nice if there was a real API.
                ToolTipManager.sharedInstance().mouseMoved(lastMouseEvent);
        }

    }


    private static final String SPACER = "   ";

    private static final String SHOW_TIME_IN_TITLE =
            "userPref.window.title.includeTime";

    private static final String SHOW_PLAY_IN_TITLE =
            "userPref.window.title.includePlay";

    private static final String[] ACTIVE_SETTINGS = {
            ProcessDashboard.WINDOW_TITLE_SETTING, SHOW_TIME_IN_TITLE,
            SHOW_PLAY_IN_TITLE };

    private static final String ENABLED_DATA_NAME =
        "Show_Percent_Spent_Indicator";

    private static final String ENABLED_SETTING_NAME =
        "percentSpentIndicator.visible";

    private static final TopDownBottomUpJanitor EST_TIME_JANITOR =
        new TopDownBottomUpJanitor("Estimated Time");
}
