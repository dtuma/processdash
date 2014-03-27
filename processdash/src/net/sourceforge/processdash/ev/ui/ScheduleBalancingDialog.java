// Copyright (C) 2014 Tuma Solutions, LLC
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVSchedule.Period;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListData;
import net.sourceforge.processdash.ev.EVTaskListRollup;
import net.sourceforge.processdash.ui.lib.DecimalField;
import net.sourceforge.processdash.ui.lib.JDialogCellEditor;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.TimeNumberFormat;

public class ScheduleBalancingDialog extends JDialogCellEditor {

    private EVTaskListRollup rollupTaskList;

    private Date targetDate;

    private Component parentComponent;

    private List<ScheduleTableRow> scheduleRows;

    boolean rowsAreEditable;

    private TotalTableRow totalRow;

    private double originalTotalTime;


    public ScheduleBalancingDialog(EVTaskListRollup taskList) {
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
        this.parentComponent = table.getTopLevelAncestor();

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
        }
        totalRow.addToPanel(panel, numScheduleRows);

        String dialogTitle = TaskScheduleDialog.resources.getString( //
                rowsAreEditable ? "Balance.Editable_Title"
                        : "Balance.Read_Only_Title");
        JOptionPane.showMessageDialog(parentComponent, panel, dialogTitle,
            JOptionPane.PLAIN_MESSAGE);
    }

    private void sumUpTotalTime() {
        double newTotal = 0;
        for (ScheduleTableRow oneRow : scheduleRows)
            newTotal += oneRow.time;
        totalRow.setTime(newTotal, TimeChangeSource.Other);
        for (ScheduleTableRow oneRow : scheduleRows)
            oneRow.updateSliderPos();
    }

    private void redistributeTime(ScheduleTableRow forRow) {
        double timeToDistribute = totalRow.time;
        if (forRow != null)
            timeToDistribute -= forRow.time;
        timeToDistribute = Math.max(0, timeToDistribute);

        double totalWeight = 0;
        int numMatchingRows = 0;
        for (ScheduleTableRow oneRow : scheduleRows) {
            if (oneRow.isRedistTarget(forRow)) {
                totalWeight += oneRow.time;
                numMatchingRows++;
            }
        }

        if (totalWeight == 0) {
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
    }


    private enum TimeChangeSource {
        TextField, Slider, Other
    }

    private abstract class TableRow implements FocusListener, ActionListener,
            ChangeListener {

        String rowLabel;

        double time;

        JTextField timeField;

        JLabel timeLabel = new JLabel();

        JSlider timeSlider;

        boolean programmaticallyChangingSlider;

        boolean isEditable() {
            return timeField != null;
        }

        void createEditingControls() {
            // create the text field for directly editing time
            timeField = new DecimalField(0, 4, new TimeNumberFormat());
            timeField.addFocusListener(this);
            timeField.addActionListener(this);

            // create a slider for visually adjusting time
            timeSlider = new JSlider(0, SLIDER_MAX);
            timeSlider.addChangeListener(this);
        }

        void addToPanel(JPanel panel, int row) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = row;
            c.anchor = GridBagConstraints.WEST;
            panel.add(new JLabel(rowLabel + "  "), c);

            c.gridx = 1;
            c.anchor = GridBagConstraints.EAST;
            panel.add(timeField != null ? timeField : timeLabel, c);

            c.gridx = 2;
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

    private class ScheduleTableRow extends TableRow {

        private EVTaskList targetTaskList;

        private Period targetPeriod;

        public ScheduleTableRow(EVTaskList tl) {
            this.targetTaskList = tl;
            this.rowLabel = tl.getDisplayName();

            boolean isEditable = tl instanceof EVTaskListData;
            if (isEditable)
                // this will ensure that the schedule is extended far enough
                // to include the date in question
                tl.getSchedule().saveActualIndirectTime(targetDate, 0);

            Period p = tl.getSchedule().get(targetDate);
            if (p != null && p.getBeginDate() != EVSchedule.A_LONG_TIME_AGO
                    && p.getEndDate(false).after(targetDate)) {
                this.targetPeriod = p;
                if (isEditable) {
                    rowsAreEditable = true;
                    createEditingControls();
                }
                setTime(p.getPlanDirectTime(), TimeChangeSource.Other);
            }
        }

        boolean isDisplayable() {
            return targetPeriod != null;
        }

        boolean isRedistTarget(ScheduleTableRow fromRow) {
            return isEditable() && this != fromRow;
        }

        @Override
        int getSliderPosForTime(double time) {
            if (totalRow.time == 0)
                return 0;

            // use an exponential function that positions the average time
            // at the 50% mark on the slider
            double percent = time / totalRow.time;
            double fraction = Math.exp(Math.log(percent) / exponent());
            return (int) (fraction * SLIDER_MAX);
        }

        @Override
        double getTimeForSliderPos(double sliderPos) {
            double fraction = sliderPos / SLIDER_MAX;
            double percent = Math.pow(fraction, exponent());
            return totalRow.time * percent;
        }

        private double exponent() {
            return Math.log(scheduleRows.size()) / Math.log(2);
        }

        @Override
        void setTime(double newTime, TimeChangeSource src) {
            super.setTime(newTime, src);
            if (src == TimeChangeSource.TextField)
                sumUpTotalTime();
            else if (src == TimeChangeSource.Slider)
                redistributeTime(this);
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
            return originalTotalTime * ratio;
        }

        @Override
        void setTime(double newTime, TimeChangeSource src) {
            super.setTime(newTime, src);
            if (src != TimeChangeSource.Other)
                redistributeTime(null);
        }

    }

    private static final int SLIDER_MAX = 1000;

}
