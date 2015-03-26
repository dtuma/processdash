// Copyright (C) 2014-2015 Tuma Solutions, LLC
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.xy.AbstractIntervalXYDataset;
import org.jfree.data.xy.XYDataset;

import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVSchedule.Period;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListData;
import net.sourceforge.processdash.ev.EVTaskListRollup;
import net.sourceforge.processdash.ui.lib.BoxUtils;
import net.sourceforge.processdash.ui.lib.DecimalField;
import net.sourceforge.processdash.ui.lib.JDialogCellEditor;
import net.sourceforge.processdash.util.ComparableValue;
import net.sourceforge.processdash.util.DateUtils;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.TimeNumberFormat;

public class ScheduleBalancingDialog extends JDialogCellEditor {

    private TaskScheduleDialog parent;

    private EVTaskListRollup rollupTaskList;

    private Date targetDate;

    private List<ScheduleTableRow> scheduleRows;

    boolean rowsAreEditable;

    private TotalTableRow totalRow;

    private double originalTotalTime;

    private ChartData chartData;


    public ScheduleBalancingDialog(TaskScheduleDialog parent,
            EVTaskListRollup taskList) {
        this.parent = parent;
        this.rollupTaskList = taskList;
        super.button.setHorizontalAlignment(SwingConstants.RIGHT);
        setClickCountToStart(2);
    }

    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {
        this.targetDate = rollupTaskList.getSchedule().get(row + 1)
                .getEndDate(true);

        return super.getTableCellEditorComponent(table, value, isSelected, row,
            column);
    }


    @Override
    protected Object showEditorDialog(Object value) throws EditingCancelled {
        collectScheduleRows();
        if (!scheduleRows.isEmpty()) {
            buildAndShowGUI();
        }
        return null;
    }

    private void collectScheduleRows() {
        rowsAreEditable = false;
        scheduleRows = new ArrayList();
        totalRow = new TotalTableRow();
        collectScheduleRows(rollupTaskList);
    }

    private void collectScheduleRows(EVTaskList tl) {
        if (tl instanceof EVTaskListRollup) {
            EVTaskListRollup rollup = (EVTaskListRollup) tl;
            for (int i = 0; i < rollup.getSubScheduleCount(); i++)
                collectScheduleRows(rollup.getSubSchedule(i));

        } else {
            ScheduleTableRow oneRow = new ScheduleTableRow(tl);
            if (oneRow.isDisplayable())
                scheduleRows.add(oneRow);
        }
    }

    private void buildAndShowGUI() {
        chartData = null;
        int numScheduleRows = scheduleRows.size();

        sumUpTotalTime();
        originalTotalTime = totalRow.time;
        if (originalTotalTime == 0)
            // if the rows added up to zero, choose a nominal target total time
            // corresponding to 10 hours per included schedule
            originalTotalTime = numScheduleRows * 60 * 10;

        JPanel panel = new JPanel(new GridBagLayout());
        if (numScheduleRows == 1) {
            totalRow.rowLabel = scheduleRows.get(0).rowLabel;
        } else {
            for (int i = 0; i < numScheduleRows; i++)
                scheduleRows.get(i).addToPanel(panel, i);
            scheduleRows.get(numScheduleRows - 1).showPercentageTickMarks();
            addChartToPanel(panel, numScheduleRows + 1);
        }
        totalRow.addToPanel(panel, numScheduleRows);

        if (rowsAreEditable == false) {
            String title = TaskScheduleDialog.resources
                    .getString("Balance.Read_Only_Title");
            JOptionPane.showMessageDialog(parent.frame, panel, title,
                JOptionPane.PLAIN_MESSAGE);

        } else {
            String title = TaskScheduleDialog.resources
                    .getString("Balance.Editable_Title");
            JDialog dialog = new JDialog(parent.frame, title, true);
            addButtons(dialog, panel, numScheduleRows + 2);
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            dialog.getContentPane().add(panel);
            dialog.pack();
            dialog.setLocationRelativeTo(parent.frame);
            dialog.setResizable(true);
            dialog.setVisible(true);
        }
    }

    private void addChartToPanel(JPanel panel, int gridY) {
        // create a dataset for displaying schedule data
        chartData = new ChartData();
        updateChart();

        // customize a renderer for displaying schedules
        XYBarRenderer renderer = new XYBarRenderer();
        renderer.setUseYInterval(true);
        renderer.setBaseToolTipGenerator(chartData);
        renderer.setDrawBarOutline(true);

        // use an inverted, unadorned numeric Y-axis
        NumberAxis yAxis = new NumberAxis();
        yAxis.setInverted(true);
        yAxis.setTickLabelsVisible(false);
        yAxis.setTickMarksVisible(false);
        yAxis.setUpperMargin(0);

        // use a Date-based X-axis
        DateAxis xAxis = new DateAxis();

        // create an XY plot to display the data
        XYPlot plot = new XYPlot(chartData, xAxis, yAxis, renderer);
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setRangeGridlinesVisible(false);
        plot.setNoDataMessage(TaskScheduleDialog.resources
                .getString("Chart.No_Data_Message"));

        // create a chart and a chart panel
        JFreeChart chart = new JFreeChart(plot);
        chart.removeLegend();
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setInitialDelay(50);
        chartPanel.setDismissDelay(60000);
        chartPanel.setMinimumDrawHeight(40);
        chartPanel.setMinimumDrawWidth(40);
        chartPanel.setMaximumDrawHeight(3000);
        chartPanel.setMaximumDrawWidth(3000);
        chartPanel.setPreferredSize(new Dimension(300, gridY * 25));

        // add the chart to the dialog content pane
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = gridY;
        c.gridwidth = 4;
        c.weightx = 2;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(10, 0, 0, 0);
        panel.add(chartPanel, c);

        // retrieve the colors used for each chart bar, and register those
        // colors with the schedule rows so they can act as a legend
        for (int i = scheduleRows.size(); i-- > 0;) {
            ScheduleTableRow oneRow = scheduleRows.get(i);
            oneRow.addColoredIcon(renderer.lookupSeriesPaint(i));
        }
    }

    private void addButtons(JDialog dialog, JPanel panel, int gridY) {
        Box buttonBox = BoxUtils.hbox( //
            new JButton(new OKAction(dialog)), 5, //
            new JButton(new RevertTimesAction()), 5, //
            new JButton(new CancelAction(dialog)));

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = gridY;
        c.gridwidth = 4;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(10, 10, 0, 10);
        panel.add(buttonBox, c);
    }

    private void sumUpTotalTime() {
        double newTotal = 0;
        for (ScheduleTableRow oneRow : scheduleRows)
            newTotal += oneRow.time;
        totalRow.setTime(newTotal, TimeChangeSource.Other);
        for (ScheduleTableRow oneRow : scheduleRows)
            oneRow.updateSliderPos();
        updateChart();
    }

    private void redistributeTime(ScheduleTableRow forRow, boolean clearLocks) {
        double timeToDistribute = totalRow.time;
        double totalWeight = 0;
        int numMatchingRows = 0;
        for (ScheduleTableRow oneRow : scheduleRows) {
            if (clearLocks && oneRow != forRow)
                oneRow.setLocked(false);
            if (oneRow.isRedistTarget(forRow)) {
                totalWeight += oneRow.time;
                numMatchingRows++;
            } else {
                timeToDistribute -= oneRow.time;
            }
        }
        timeToDistribute = Math.max(0, timeToDistribute);

        if (numMatchingRows == 0) {
            // if all of the rows are locked, don't redistribute time. Just
            // calculate a new sum based on the time that has been assigned to
            // the given row.
            sumUpTotalTime();

        } else if (timeToDistribute == 0 && totalWeight == 0) {
            // if we have no (or negative) time to distribute, and the editable
            // rows already have zero time (so they can't be reduced any
            // further), just calculate a new total time sum.
            sumUpTotalTime();

        } else if (totalWeight == 0) {
            double newTime = timeToDistribute / numMatchingRows;
            for (ScheduleTableRow oneRow : scheduleRows) {
                if (oneRow.isRedistTarget(forRow))
                    oneRow.setTime(newTime, TimeChangeSource.Other);
            }

        } else {
            for (ScheduleTableRow oneRow : scheduleRows) {
                if (oneRow.isRedistTarget(forRow)) {
                    double newTime = timeToDistribute * oneRow.time
                            / totalWeight;
                    oneRow.setTime(newTime, TimeChangeSource.Other);
                }
            }
        }

        updateChart();
    }

    private void updateChart() {
        if (chartData != null) {
            double cumTime = 0;
            for (int i = 0; i < scheduleRows.size(); i++) {
                ScheduleTableRow oneRow = scheduleRows.get(i);
                cumTime += oneRow.time / oneRow.duration;
                oneRow.cumTime = cumTime;
            }
            chartData.fireDatasetChanged();
        }
    }


    private void saveChanges() {
        boolean madeChange = false;
        for (ScheduleTableRow oneRow : scheduleRows)
            if (oneRow.saveChanges())
                madeChange = true;
        if (madeChange)
            parent.recalcAll();
    }


    private void revertTimes() {
        for (ScheduleTableRow oneRow : scheduleRows)
            oneRow.revertTime();
        sumUpTotalTime();
    }


    private enum TimeChangeSource {
        TextField, Slider, Other
    }

    private abstract class TableRow implements FocusListener, ActionListener,
            ChangeListener {

        String rowLabel;

        JLabel rowJLabel;

        double time;

        double cumTime;

        JTextField timeField;

        JLabel timeLabel = new JLabel();

        JLabel lockedLabel;

        JSlider timeSlider;

        ComparableValue rowKey;

        boolean programmaticallyChangingSlider;

        boolean isEditable() {
            return timeField != null;
        }

        void createEditingControls() {
            // create the text field for directly editing time
            timeField = new DecimalField(0, 4, new TimeNumberFormat());
            timeField.setMinimumSize(timeField.getPreferredSize());
            timeField.setHorizontalAlignment(JTextField.RIGHT);
            timeField.addFocusListener(this);
            timeField.addActionListener(this);

            // create a label to indicate whether this row has been modified
            lockedLabel = new JLabel(UNLOCKED_ICON);

            // create a slider for visually adjusting time
            timeSlider = new JSlider(0, SLIDER_MAX);
            timeSlider.addChangeListener(this);
        }

        public void addColoredIcon(Paint paint) {
            rowJLabel.setIcon(new ColoredIcon(paint));
        }

        void addToPanel(JPanel panel, int row) {
            rowKey = new ComparableValue(rowLabel, row);

            rowJLabel = new JLabel(rowLabel + "  ");
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = row;
            c.anchor = GridBagConstraints.NORTHWEST;
            panel.add(rowJLabel, c);

            c.gridx = 1;
            c.anchor = GridBagConstraints.NORTHEAST;
            panel.add(timeField != null ? timeField : timeLabel, c);

            c.gridx = 2;
            if (lockedLabel != null)
                panel.add(lockedLabel, c);

            c.gridx = 3;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            if (timeSlider != null)
                panel.add(timeSlider, c);
        }

        void setTime(double newTime, TimeChangeSource src) {
            time = newTime;
            updateTimeText();
            if (src != TimeChangeSource.Slider)
                updateSliderPos();
        }

        void updateTimeText() {
            String timeStr = FormatUtil.formatTime(time);
            if (timeField != null)
                timeField.setText(timeStr);
            else
                timeLabel.setText(timeStr);
        }

        void updateTimeFromField() {
            double newTime = FormatUtil.parseTime(timeField.getText());
            if (newTime >= 0 && Math.abs(newTime - time) > 0.1)
                setTime(newTime, TimeChangeSource.TextField);
            else
                updateTimeText();
        }

        void updateTimeFromSlider() {
            if (timeSlider != null) {
                int sliderPos = timeSlider.getValue();
                double newTime = getTimeForSliderPos(sliderPos);
                setTime(newTime, TimeChangeSource.Slider);
            }
        }

        void updateSliderPos() {
            if (timeSlider != null) {
                programmaticallyChangingSlider = true;
                timeSlider.setValue(getSliderPosForTime(time));
                programmaticallyChangingSlider = false;
            }
        }

        double roundTime(double time) {
            return 5 * Math.round(time / 5);
        }

        abstract int getSliderPosForTime(double time);

        abstract double getTimeForSliderPos(double sliderPos);

        // Handle events from the time field
        public void focusGained(FocusEvent e) {}

        public void focusLost(FocusEvent e) {
            updateTimeFromField();
        }

        public void actionPerformed(ActionEvent e) {
            updateTimeFromField();
        }

        // Handle events from the slider
        public void stateChanged(ChangeEvent e) {
            if (!programmaticallyChangingSlider)
                updateTimeFromSlider();
        }

    }

    private class ScheduleTableRow extends TableRow implements MouseListener {

        private EVTaskList targetTaskList;

        private Period targetPeriod;

        private boolean locked;

        private double duration;

        public ScheduleTableRow(EVTaskList tl) {
            this.targetTaskList = tl;
            this.rowLabel = tl.getDisplayName();
            this.locked = false;

            boolean isEditable = tl instanceof EVTaskListData;
            if (isEditable)
                // this will ensure that the schedule is extended far enough
                // to include the date in question
                tl.getSchedule().saveActualIndirectTime(targetDate, 0);

            Period p = tl.getSchedule().get(targetDate);
            if (p != null && p.getBeginDate() != EVSchedule.A_LONG_TIME_AGO
                    && p.getEndDate(false).after(targetDate)) {
                this.targetPeriod = p;
                this.duration = (p.getEndDate().getTime() - p.getBeginDate()
                        .getTime()) / (double) DateUtils.DAYS;
                if (isEditable) {
                    rowsAreEditable = true;
                    createEditingControls();
                    lockedLabel.addMouseListener(this);
                }
                setTime(p.getPlanDirectTime(), TimeChangeSource.Other);
            }
        }

        public void showPercentageTickMarks() {
            if (timeSlider == null)
                return;

            // Create a series of labels to indicate 0, 25, 50, 75, and 100%
            Hashtable labels = new Hashtable();
            JLabel emptyLabel = new JLabel();
            Font font = emptyLabel.getFont();
            font = font.deriveFont(Font.PLAIN, 0.8f * font.getSize2D());
            Border border = BorderFactory.createEmptyBorder(0, 0, 10, 0);
            for (int i = 0; i <= 100; i += 25) {
                String txt = i + (i == 100 ? "%   " : (i == 75 ? "%  " : "%"));
                JLabel label = new JLabel(txt, TICK_MARK, SwingConstants.CENTER);
                label.setHorizontalTextPosition(SwingConstants.CENTER);
                label.setVerticalTextPosition(SwingConstants.BOTTOM);
                label.setIconTextGap(0);
                label.setFont(font);
                label.setBorder(border);

                double percent = i / 100.0;
                int pos = getSliderPosForPercent(percent);
                int key = Math.min(Math.max(pos, 1), SLIDER_MAX - 1);
                labels.put(key, label);
            }

            // the code above moves the 0% and 100% labels inward by a miniscule
            // amount. Now we attach a zero-width label to the slider positions
            // representing "true" 0% and 100%. This tricks the SliderUI so it
            // doesn't adjust the track margins inward to account for the
            // labels, so this slider has the same width as the other unlabeled
            // sliders.
            labels.put(0, emptyLabel);
            labels.put(SLIDER_MAX, emptyLabel);
            timeSlider.setLabelTable(labels);
            timeSlider.setPaintLabels(true);
        }

        boolean isDisplayable() {
            return targetPeriod != null;
        }

        boolean isRedistTarget(ScheduleTableRow fromRow) {
            return isEditable() && this != fromRow && !locked;
        }

        public void setLocked(boolean locked) {
            this.locked = locked;
            if (lockedLabel != null) {
                lockedLabel.setIcon(locked ? LOCKED_ICON : UNLOCKED_ICON);
                lockedLabel.setToolTipText(locked == false ? null
                        : TaskScheduleDialog.resources
                                .getString("Balance.Modified_Tooltip"));
            }
        }

        public void mouseClicked(MouseEvent e) {
            // when the user clicks on the "locked" label, toggle the flag
            setLocked(!locked);
        }
        public void mousePressed(MouseEvent e) {}
        public void mouseReleased(MouseEvent e) {}
        public void mouseEntered(MouseEvent e) {}
        public void mouseExited(MouseEvent e) {}

        @Override
        int getSliderPosForTime(double time) {
            if (totalRow.time == 0)
                return 0;

            double percent = time / totalRow.time;
            return getSliderPosForPercent(percent);
        }

        private int getSliderPosForPercent(double percent) {
            // use an exponential function that positions the average time
            // at the 50% mark on the slider
            double fraction = Math.exp(Math.log(percent) / exponent());
            return (int) (fraction * SLIDER_MAX);
        }

        @Override
        double getTimeForSliderPos(double sliderPos) {
            if (sliderPos == SLIDER_MAX)
                return totalRow.time;

            double fraction = sliderPos / SLIDER_MAX;
            double percent = Math.pow(fraction, exponent());
            return roundTime(totalRow.time * percent);
        }

        private double exponent() {
            return Math.log(scheduleRows.size()) / Math.log(2);
        }

        @Override
        void setTime(double newTime, TimeChangeSource src) {
            super.setTime(newTime, src);
            if (src != TimeChangeSource.Other)
                setLocked(true);
            if (src == TimeChangeSource.TextField)
                sumUpTotalTime();
            else if (src == TimeChangeSource.Slider)
                redistributeTime(this, false);
        }

        boolean saveChanges() {
            if (Math.abs(time - targetPeriod.getPlanDirectTime()) < 0.1) {
                return false;
            } else {
                targetPeriod.setPlanDirectTime(FormatUtil.formatTime(time),
                    true);
                parent.recordDirtySubschedule(targetTaskList);
                return true;
            }
        }

        void revertTime() {
            setLocked(false);
            setTime(targetPeriod.getPlanDirectTime(), TimeChangeSource.Other);
        }

    }

    private class TotalTableRow extends TableRow {

        public TotalTableRow() {
            rowLabel = TaskScheduleDialog.resources.getString("Total");
        }

        @Override
        void addToPanel(JPanel panel, int row) {
            if (rowsAreEditable)
                createEditingControls();
            setTime(time, TimeChangeSource.Other);
            super.addToPanel(panel, row);
            if (chartData != null)
                addColoredIcon(null);
        }

        @Override
        int getSliderPosForTime(double time) {
            // use a curved function that positions the original total time
            // at the 50% mark on the slider, and uses the right edge for
            // 20 times the original total time.
            double ratio = time / originalTotalTime;
            double x = 1 - 1 / (ratio + 1);
            int result = (int) (x * SLIDER_MAX * 21 / 20);
            return Math.min(result, SLIDER_MAX);
        }

        @Override
        double getTimeForSliderPos(double sliderPos) {
            double x = (sliderPos * 20) / (SLIDER_MAX * 21);
            double ratio = -1 + 1 / (1 - x);
            return roundTime(originalTotalTime * ratio);
        }

        @Override
        void setTime(double newTime, TimeChangeSource src) {
            super.setTime(newTime, src);
            if (src != TimeChangeSource.Other)
                redistributeTime(null, true);
        }

    }

    private static class ColoredIcon implements Icon {

        private Paint paint;

        protected ColoredIcon(Paint paint) {
            this.paint = paint;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (paint != null) {
                ((Graphics2D) g).setPaint(paint);
                g.fillRect(x + 2, y + 2, 8, 8);
            }
        }

        public int getIconWidth() {
            return 12;
        }

        public int getIconHeight() {
            return 12;
        }

    }

    private static final Icon UNLOCKED_ICON = new ColoredIcon(null);

    private static final Icon LOCKED_ICON = new ImageIcon(
            ScheduleBalancingDialog.class.getResource("modified.png"));

    private static class TickMarkIcon implements Icon {

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.black);
            g.drawLine(x, y, x, y + 4);
        }

        public int getIconWidth() {
            return 1;
        }

        public int getIconHeight() {
            return 4;
        }
    }

    private static final Icon TICK_MARK = new TickMarkIcon();

    private class ChartData extends AbstractIntervalXYDataset implements
            XYToolTipGenerator {

        public int getSeriesCount() {
            if (totalRow.time == 0)
                return 0;
            else
                return scheduleRows.size();
        }

        private ScheduleTableRow get(int series) {
            return scheduleRows.get(series);
        }

        public Comparable getSeriesKey(int series) {
            return get(series).rowKey;
        }

        public int indexOf(Comparable seriesKey) {
            return ((ComparableValue) seriesKey).getOrdinal();
        }

        public int getItemCount(int series) {
            return 1;
        }

        public Number getStartX(int series, int item) {
            return get(series).targetPeriod.getBeginDate().getTime();
        }

        public Number getEndX(int series, int item) {
            return get(series).targetPeriod.getEndDate().getTime();
        }

        public Number getStartY(int series, int item) {
            if (series == 0)
                return 0;
            else
                return getEndY(series - 1, item);
        }

        public Number getEndY(int series, int item) {
            return get(series).cumTime;
        }

        public Number getX(int series, int item) {
            return getStartX(series, item);
        }

        public Number getY(int series, int item) {
            return getStartY(series, item);
        }

        @Override
        protected void fireDatasetChanged() {
            super.fireDatasetChanged();
        }

        public String generateToolTip(XYDataset dataset, int series, int item) {
            ScheduleTableRow row = get(series);
            return TaskScheduleDialog.resources.format(
                "Balance.Chart_Tooltip_FMT", row.rowLabel,
                FormatUtil.formatTime(row.time),
                row.targetPeriod.getBeginDate(), //
                row.targetPeriod.getEndDate());
        }

    }


    private class OKAction extends AbstractAction {
        private JDialog dialog;

        public OKAction(JDialog dialog) {
            super(TaskScheduleDialog.resources.getString("OK"));
            this.dialog = dialog;
        }

        public void actionPerformed(ActionEvent e) {
            saveChanges();
            dialog.dispose();
        }

    }

    private class CancelAction extends AbstractAction {
        private JDialog dialog;

        public CancelAction(JDialog dialog) {
            super(TaskScheduleDialog.resources.getString("Cancel"));
            this.dialog = dialog;
        }

        public void actionPerformed(ActionEvent e) {
            dialog.dispose();
        }

    }

    private class RevertTimesAction extends AbstractAction {

        public RevertTimesAction() {
            super(TaskScheduleDialog.resources.getString("Revert"));
        }

        public void actionPerformed(ActionEvent e) {
            revertTimes();
        }

    }

    private static final int SLIDER_MAX = 1000;

}
