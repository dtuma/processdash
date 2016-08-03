// Copyright (C) 2000-2016 Tuma Solutions, LLC
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


package net.sourceforge.processdash.log.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.toedter.calendar.JDateChooser;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.defects.Defect;
import net.sourceforge.processdash.log.defects.DefectLog;
import net.sourceforge.processdash.log.defects.DefectPhase;
import net.sourceforge.processdash.log.defects.DefectPhaseList;
import net.sourceforge.processdash.log.defects.DefectUtil;
import net.sourceforge.processdash.log.time.TimeLoggingModel;
import net.sourceforge.processdash.process.DefectTypeStandard;
import net.sourceforge.processdash.process.WorkflowInfo.Phase;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.BoxUtils;
import net.sourceforge.processdash.ui.lib.DecimalField;
import net.sourceforge.processdash.ui.lib.DropDownLabel;
import net.sourceforge.processdash.ui.lib.HTMLMarkup;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.NullSafeObjectUtils;
import net.sourceforge.processdash.util.Stopwatch;
import net.sourceforge.processdash.util.StringUtils;


public class DefectDialog extends JDialog
    implements ActionListener, DocumentListener, WindowListener
{
    ProcessDashboard parent;
    String defectFilename;
    PropertyKey defectPath;
    PropertyKey taskPath;
    DefectLog defectLog = null;
    Stopwatch stopwatch = null;
    StopwatchSynchronizer stopwatchSynchronizer;
    StartStopButtons defectTimerButton;
    Date date = null;
    JButton OKButton, CancelButton, fixDefectButton;
    String defectNumber = null;
    JLabel number;
    JDateChooser fix_date;
    JTextField fix_defect;
    DecimalField fix_time, fix_count;
    ExternalLinksButton linksButton;
    JTextArea description;
    DefectPhaseList workflowPhases, processPhases;
    JComboBox defect_type, phase_injected, phase_removed;
    PendingSelector pendingSelector;
    Map<String, String> extra_attrs;
    boolean isDirty = false, autoCreated = false;

    /** A stack of the defect dialogs that have been interrupted. */
    private static Stack interruptedDialogs = new Stack();
    /** The defect dialog which was timing most recently. */
    private static DefectDialog activeDialog = null;
    /** A list of all open defect dialogs. */
    private static Hashtable defectDialogs = new Hashtable();
    /** A timer object for refreshing the fix time field. */
    private javax.swing.Timer activeRefreshTimer = null;
    /** Objects representing "special" injection/removal phases */
    private static final DefectPhase //
        BEFORE_DEVELOPMENT = Defect.BEFORE_DEVELOPMENT_PHASE,
        AFTER_DEVELOPMENT = Defect.AFTER_DEVELOPMENT_PHASE;
    static Resources resources = Resources.getDashBundle("Defects.Editor");


    DefectDialog(ProcessDashboard dash, String defectFilename,
            PropertyKey defectPath, PropertyKey taskPath) {
        this(dash, defectFilename, defectPath, taskPath, true);
    }

    DefectDialog(ProcessDashboard dash, String defectFilename,
            PropertyKey defectPath, PropertyKey taskPath, boolean guessDefaults) {
        super(dash);
        setTitle(resources.getString("Window_Title"));
        PCSH.enableHelpKey(this, "EnteringDefects");

        parent = dash;
        this.defectFilename = defectFilename;
        this.defectPath = defectPath;
        this.taskPath = taskPath;
        defectLog = new DefectLog(defectFilename, defectPath.path(),
                                  dash.getData());
        date = new Date();
        stopwatch = new Stopwatch(false);
        stopwatch.setMultiplier(Settings.getVal("timer.multiplier"));
        stopwatchSynchronizer = new StopwatchSynchronizer(dash
                .getTimeLoggingModel());

        JPanel panel = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints g = new GridBagConstraints();
        JComponent c;
        Insets bottom_margin = new Insets(1, 1, 8, 1);
        Insets bottom_right_margin = new Insets(1, 1, 8, 10);
        Insets small_margin = new Insets(1, 1, 1, 1);
        panel.setBorder(new EmptyBorder(5,5,5,5));
        panel.setLayout(layout);

                                // first row
        g.gridy = 0;
        g.insets = small_margin;
        g.anchor = GridBagConstraints.WEST;

        number = new JLabel();
        g.gridx = 0;   layout.setConstraints(number, g);
        panel.add(number);

        c = new JLabel(resources.getString("Fix_Date_Label"));
        g.gridx = 1;   layout.setConstraints(c, g);
        panel.add(c);

                                // second row
        g.gridy = 1;
        g.insets = bottom_right_margin;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        defect_type = DefectTypeStandard.get
            (defectPath.path(), dash.getData()).getAsComboBox();
        defect_type.insertItemAt(resources.getString("Defect_Type_Prompt"), 0);
        defect_type.setMaximumRowCount(20);
        defect_type.setSelectedIndex(0);
        defect_type.addActionListener(this);

        g.gridx = 0;   layout.setConstraints(defect_type, g);
        panel.add(defect_type);

        fix_date = new JDateChooser(date);
        fix_date.getDateEditor().getUiComponent().setToolTipText(
            resources.getString("Fix_Date_Tooltip"));
        g.insets = bottom_margin;
        g.fill = GridBagConstraints.BOTH;
        g.gridx = 1;   layout.setConstraints(fix_date, g);
        panel.add(fix_date);

                                // third row
        g.gridy = 2;
        g.insets = small_margin;
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;

        c = new JLabel(resources.getString("Injected_Label"));
        g.gridx = 0;   layout.setConstraints(c, g);
        panel.add(c);

        c = pendingSelector = new PendingSelector();
        g.fill = GridBagConstraints.NONE;
        g.gridx = 1;   layout.setConstraints(c, g);
        panel.add(c);

                                // fourth row
        g.gridy = 3;
        g.insets = bottom_right_margin;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTHWEST;

        workflowPhases = DefectUtil.getWorkflowDefectPhases(taskPath.path(),
            parent);
        processPhases = DefectUtil.getDefectPhaseList(defectPath.path(),
            taskPath.path(), parent);

        DefectPhaseList defectPhaseList;
        if (workflowPhases != null && !workflowPhases.isEmpty())
            defectPhaseList = workflowPhases;
        else
            defectPhaseList = processPhases;
        stopwatchSynchronizer.workflowRoot = defectPhaseList.workflowRoot;

        phase_removed = phaseComboBox(defectPhaseList,
            guessDefaults ? defectPhaseList.defaultRemovalPhase : -1);
        phase_removed.setToolTipText(resources.getString("Removed_Tooltip"));

        phase_injected = phaseComboBox(defectPhaseList,
            guessDefaults ? defectPhaseList.defaultInjectionPhase : -1);
        phase_injected.setToolTipText(resources.getString("Injected_Tooltip"));

        phase_injected.insertItemAt(BEFORE_DEVELOPMENT, 0);
        phase_injected.addActionListener(this);
        g.gridx = 0;   layout.setConstraints(phase_injected, g);
        panel.add(phase_injected);

        phase_removed.addItem(AFTER_DEVELOPMENT);
        phase_removed.addActionListener(this);
        g.insets = bottom_margin;
        g.gridx = 1; layout.setConstraints(phase_removed, g);
        panel.add(phase_removed);

        if (workflowPhases != null && !workflowPhases.workflowInfo.isEmpty()) {
            new MorePhaseOptionsHandler(phase_injected, workflowPhases,
                    processPhases, true);
            new MorePhaseOptionsHandler(phase_removed, workflowPhases,
                    processPhases, false);
        }

                                // fifth row

        // create a subpanel to allow more even spacing for the three
        // elements that appear on these two rows
        GridBagLayout fixLayout = new GridBagLayout();
        JPanel fixPanel = new JPanel(fixLayout);

        g.gridy = 0;
        g.insets = small_margin;
        g.fill = GridBagConstraints.VERTICAL;
        g.anchor = GridBagConstraints.WEST;

        c = new JLabel(resources.getString("Fix_Time_Label"));
        g.gridx = 0;   fixLayout.setConstraints(c, g);
        fixPanel.add(c);

        c = new JLabel(resources.getString("Fix_Count_Label"));
        g.gridx = 1;   fixLayout.setConstraints(c, g);
        fixPanel.add(c);

        c = new JLabel(resources.getString("Fix_Defect_Label"));
        g.gridx = 2;   fixLayout.setConstraints(c, g);
        fixPanel.add(c);

                                // sixth row
        g.gridy = 1;
        g.insets = bottom_right_margin;

        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(1);
        fix_time = new DecimalField(0.0, 4, nf);
        setTextFieldMinSize(fix_time);
        fix_time.setToolTipText(resources.getString("Fix_Time_Tooltip"));
        defectTimerButton = new StartStopButtons();
        c = BoxUtils.hbox(fix_time, 1, defectTimerButton.stopButton, 1,
            defectTimerButton.startButton);
        g.fill = GridBagConstraints.VERTICAL;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.gridx = 0;   fixLayout.setConstraints(c, g);
        fixPanel.add(c);
        fix_time.getDocument().addDocumentListener(this);

        nf = NumberFormat.getIntegerInstance();
        fix_count = new DecimalField(1, 4, nf);
        setTextFieldMinSize(fix_count);
        fix_count.setToolTipText(resources.getString("Fix_Count_Tooltip"));
        g.gridx = 1;   fixLayout.setConstraints(fix_count, g);
        fixPanel.add(fix_count);
        fix_count.getDocument().addDocumentListener(this);

        fix_defect = new JTextField(3);
        fix_defect.setToolTipText(resources.getString("Fix_Defect_Tooltip"));
        fix_defect.getDocument().addDocumentListener(this);
        setTextFieldMinSize(fix_defect);

        fixDefectButton = new JButton();
        fixDefectButton.setIcon(DashboardIconFactory.getDefectIcon());
        fixDefectButton.setMargin(new Insets(1, 2, 1, 2));
        fixDefectButton.setToolTipText(resources
                .getString("Fix_Defect_Button_Tooltip"));
        fixDefectButton.addActionListener(this);

        c = BoxUtils.hbox(fix_defect, 1, fixDefectButton);
        g.insets = bottom_margin;
        g.fill = GridBagConstraints.BOTH;
        g.gridx = 2;   fixLayout.setConstraints(c, g);
        fixPanel.add(c);

        g.gridx = 0;   g.gridy = 4;  g.gridwidth = 2;
        g.insets = new Insets(0, 0, 0, 0);
        layout.setConstraints(fixPanel, g);
        panel.add(fixPanel);

                                // seventh row
        g.gridy = 6;
        g.insets = small_margin;
        g.anchor = GridBagConstraints.WEST;
        g.gridwidth = 1;
        linksButton = new ExternalLinksButton();
        c = BoxUtils.hbox(new JLabel(resources.getString("Description_Label")),
            linksButton);
        g.gridx = 0;   layout.setConstraints(c, g);
        panel.add(c);

                                // eighth row
        g.gridy = 7;
        g.insets = bottom_margin;
        g.fill = GridBagConstraints.BOTH;
        description = new JTextArea();
        description.getDocument().addDocumentListener(this);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);

        JScrollPane scroller = new
            JScrollPane(description, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setPreferredSize(new Dimension(100, 100));

        JPanel textWrapper = new JPanel(new BorderLayout());
//      textWrapper.setAlignmentX(LEFT_ALIGNMENT);
        textWrapper.setBorder(new BevelBorder(BevelBorder.LOWERED));
        textWrapper.add("Center", scroller);

        g.weighty = 100;
        g.gridwidth = 2;   layout.setConstraints(textWrapper, g);
        panel.add(textWrapper);
        g.gridwidth = 1;
        g.weighty = 0;

                                // ninth row
        g.gridy = 8;
        g.insets = small_margin;
        g.anchor = GridBagConstraints.CENTER;
        g.fill = GridBagConstraints.NONE;

        Action okAction = new OKButtonAction();
        OKButton = new JButton(okAction);
        g.gridx = 0;   layout.setConstraints(OKButton, g);
        panel.add(OKButton);

        Action cancelAction = new CancelButtonAction();
        CancelButton = new JButton(cancelAction);
        g.gridx = 1; layout.setConstraints(CancelButton, g);
        panel.add(CancelButton);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(this);

        InputMap inputMap = panel
                .getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, MacGUIUtils
                .getCtrlModifier()), "okButtonAction");
        panel.getActionMap().put("okButtonAction", okAction);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            "cancelButtonAction");
        panel.getActionMap().put("cancelButtonAction", cancelAction);

        getContentPane().add(panel);
        pack();
        panel.setMinimumSize(panel.getPreferredSize());
        setVisible(true);

        if ("true".equalsIgnoreCase(Settings.getVal("defectDialog.autostart")))
            startTimingDefect();
        setDirty(false);
    }

    private void setTextFieldMinSize(JTextField tf) {
        Dimension d = tf.getPreferredSize();
        d.width /= 2;
        tf.setMinimumSize(d);
    }

    private DefectDialog(ProcessDashboard dash, String defectFilename,
                         PropertyKey defectPath, Defect defect) {
        this(dash, defectFilename, defectPath, defectPath, false);
        stopTimingDefect();
        setValues(defect);
        setDirty(false);
    }

    public static DefectDialog getDialogForDefect
        (ProcessDashboard dash, String defectFilename,
         PropertyKey defectPath, Defect defect, boolean create)
    {
        DefectDialog result = null;

        String comparisonKey = defectFilename + defect.number;
        result = (DefectDialog) defectDialogs.get(comparisonKey);
        if (result != null && result.isDisplayable()) return result;
        if (!create) return null;

        result = new DefectDialog(dash, defectFilename, defectPath, defect);
        result.saveDialogInCache();

        return result;
    }

    private String comparisonKey() {
        return defectLog.getDefectLogFilename() + defectNumber;
    }
    private void saveDialogInCache() {
        defectDialogs.put(comparisonKey(), this);
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
        MacGUIUtils.setDirty(this, isDirty);
        //OKButton.setEnabled(dirty);
    }

    public void save() {
        refreshFixTimeFromStopwatch();

        Defect d = new Defect();
        d.date = fix_date.getDate();
        if (d.date == null)
            d.date = date;
        d.number = defectNumber;
        d.defect_type = (String)defect_type.getSelectedItem();
        d.injected = (DefectPhase) phase_injected.getSelectedItem();
        d.phase_injected = d.injected.legacyPhase;
        d.removed = (DefectPhase) phase_removed.getSelectedItem();
        d.phase_removed = d.removed.legacyPhase;
        d.fix_time = fix_time.getText();
        try {
            d.fix_count = (int) FormatUtil.parseNumber(fix_count.getText());
            d.fix_count = Math.max(0, d.fix_count);
        } catch (ParseException nfe) {
            d.fix_count = 1;
        }
        d.fix_defect = fix_defect.getText();
        d.fix_pending = pendingSelector.isPending();
        d.description = description.getText();
        d.extra_attrs = extra_attrs;

        defectLog.writeDefect(d);

        defectNumber = d.number;
        number.setText(formatDefectNum(d.number));
        setDirty(false);
    }
    private String formatDefectNum(String number) {
        return resources.format("Defect_Number_FMT", number);
    }

    public void startTimingDefect() {
        stopwatch.start();
        defectTimerButton.setRunning(true);
        if (activeRefreshTimer == null)
            activeRefreshTimer = new javax.swing.Timer(6000, this);
        activeRefreshTimer.start();

        if (activeDialog != this) synchronized (interruptedDialogs) {
            if (activeDialog != null && activeDialog.stopwatch.isRunning()) {
                interruptedDialogs.push(activeDialog);
                activeDialog.stopTimingDefect();
            }

            interruptedDialogs.remove(this); // it might not be there, that's OK
            activeDialog = this;
        }
    }

    public void stopTimingDefect() {
        stopwatch.stop();
        defectTimerButton.setRunning(false);
        if (activeRefreshTimer != null)
            activeRefreshTimer.stop();

        refreshFixTimeFromStopwatch();
    }

    private void maybePopDialog() {
        if (activeDialog == this)
            if (interruptedDialogs.empty())
                activeDialog = null;
            else {
                activeDialog = (DefectDialog) interruptedDialogs.pop();
                activeDialog.toFront();

                if (stopwatch.isRunning())
                    activeDialog.startTimingDefect();
            }
    }

    private void maybeDeleteAutocreatedDefect() {
        if (autoCreated)
            defectLog.deleteDefect(defectNumber);
    }

    private void comboSelect(JComboBox cb, String item) {
        int i = cb.getItemCount();
        while (i != 0)
            if (item.equals(cb.getItemAt(--i))) {
                cb.setSelectedIndex(i);
                return;
            }
        if (StringUtils.hasValue(item)) {
            cb.addItem(item);
            cb.setSelectedItem(item);
        }
    }

    public static void phaseComboSelect(JComboBox cb, DefectPhase target) {
        if (target == null)
            return;

        if (target.phaseID != null) {
            // try to match the phase by exact ID
            for (int i = cb.getItemCount(); i-- > 0; ) {
                DefectPhase onePhase = (DefectPhase) cb.getItemAt(i);
                if (onePhase.phaseID != null
                        && onePhase.phaseID.equals(target.phaseID)) {
                    cb.setSelectedIndex(i);
                    return;
                }
            }
            // next try to match the phase by trailing ID
            for (int i = cb.getItemCount(); i-- > 0; ) {
                DefectPhase onePhase = (DefectPhase) cb.getItemAt(i);
                if (onePhase.phaseID != null
                        && onePhase.phaseID.endsWith(target.phaseID)) {
                    cb.setSelectedIndex(i);
                    return;
                }
            }
            // next try to match by workflow and phase name
            for (int i = cb.getItemCount(); i-- > 0; ) {
                DefectPhase onePhase = (DefectPhase) cb.getItemAt(i);
                if (onePhase.phaseID != null && onePhase.processName != null
                        && onePhase.phaseName != null
                        && onePhase.processName.equals(target.processName)
                        && onePhase.phaseName.equals(target.phaseName)) {
                    cb.setSelectedIndex(i);
                    return;
                }
            }

        } else {
            // try to match by legacy phase
            for (int i = cb.getItemCount(); i-- > 0; ) {
                DefectPhase onePhase = (DefectPhase) cb.getItemAt(i);
                if (onePhase.phaseID == null
                        && onePhase.legacyPhase.equals(target.legacyPhase)) {
                    cb.setSelectedIndex(i);
                    return;
                }
            }
        }

        // fallback: add the item to the list and select it.
        int insPos = 0;
        if (cb.getItemCount() > 0)
            if (cb.getItemAt(0) == BEFORE_DEVELOPMENT
                    || cb.getItemAt(0) == Defect.UNSPECIFIED_PHASE)
                insPos = 1;
        cb.insertItemAt(target, insPos);
        cb.setSelectedItem(target);
    }

    private JComboBox phaseComboBox(DefectPhaseList phases, int selectedPos) {
        JComboBox result = new JComboBox();
        result.setRenderer(new DefectPhaseItemRenderer());
        for (DefectPhase p : phases)
            result.addItem(p);
        if (selectedPos != -1)
            result.setSelectedIndex(selectedPos);
        return result;
    }


    private void hide_popups() {
        defect_type.hidePopup();
        phase_injected.hidePopup();
        phase_removed.hidePopup();
    }

    private volatile boolean programmaticallyChangingFixTime = false;

    private void fixTimeChanged() {
        if (programmaticallyChangingFixTime) return;
        setDirty(true);
        stopwatch.setElapsed((long) (fix_time.getValue() * 60.0));
    }

    private void refreshFixTimeFromStopwatch() {
        programmaticallyChangingFixTime = true;
        fix_time.setValue(stopwatch.minutesElapsedDouble());
        programmaticallyChangingFixTime = false;
    }

    private void openFixDefectDialog() {
        if (defectNumber == null) {
            save();
            setDirty(true);
            saveDialogInCache();
            autoCreated = true;
        }
        DefectDialog d = new DefectDialog(parent, defectFilename, defectPath,
                taskPath);
        d.fix_defect.setText(defectNumber);
        DefectPhase p = (DefectPhase) phase_removed.getSelectedItem();
        phaseComboSelect(d.phase_injected, p);
        phaseComboSelect(d.phase_removed, p);
        d.setDirty(false);
    }

    public void setValues(Defect d) {
        date = d.date;
        fix_date.setDate(date);
        defectNumber = d.number;
        number.setText(formatDefectNum(d.number));
        comboSelect(defect_type, d.defect_type);
        phaseComboSelect(phase_injected, d.injected);
        phaseComboSelect(phase_removed, d.removed);
        fix_time.setText(d.getLocalizedFixTime()); // will trigger fixTimeChanged
        fix_count.setText(FormatUtil.formatNumber(d.fix_count));
        fix_defect.setText(d.fix_defect);
        pendingSelector.setPending(d.fix_pending);
        description.setText(d.description);
        description.setCaretPosition(0);
        linksButton.scanForLinks();
        extra_attrs = d.extra_attrs;
    }

    public void dispose() {
        hide_popups();
        if (activeRefreshTimer != null) {
            activeRefreshTimer.stop();
            activeRefreshTimer.removeActionListener(this);
            activeRefreshTimer = null;
        }
        stopwatchSynchronizer.dispose();
        interruptedDialogs.remove(this); // it might not be there, that's OK
        defectDialogs.remove(comparisonKey());
        super.dispose();
    }


    /** Check to see if the removal phase is before the injection phase.
     *
     * If they are out of order, display an error message to the user and
     * return false; otherwise return true.
     */
    private boolean checkSequence() {
        if ("false".equalsIgnoreCase
            (Settings.getVal("defectDialog.restrictSequence")))
            return true;

        // retrieve the phases the defect was injected and removed.
        DefectPhase injected = (DefectPhase) phase_injected.getSelectedItem();
        DefectPhase removed  = (DefectPhase) phase_removed.getSelectedItem();
        if (injected == removed)
            return true;

        // if the phases are from different workflows, we can't compare them
        // in any meaningful way.
        if (!NullSafeObjectUtils.EQ(injected.processName, removed.processName))
            return true;

        if (injected.phaseID != null) {
            // if the phases came from a workflow, retrieve the rich phase data
            // from the WorkflowInfo object.
            Phase injPhase = getWorkflowInfoPhase(injected);
            Phase remPhase = getWorkflowInfoPhase(removed);
            if (injPhase == null || remPhase == null)
                return true;

            // compare the positions of the phases within the workflow
            List<Phase> phases = injPhase.getWorkflow().getPhases();
            int injPos = phases.indexOf(injPhase);
            int remPos = phases.indexOf(remPhase);

            // if the pos is -1, these phases weren't from the same workflow
            // after all, and we can't compare them in any meaningful way.
            if (injPos == -1 || remPos == -1)
                return true;

            // if the injection phase precedes the removal phase, it's good
            if (injPos <= remPos)
                return true;

            // if the removal phase is part of a PSP task but the injection
            // phase isn't, this might be a case where a PSP task is immediately
            // followed by one or more related inspections. (The Task & Schedule
            // logic inserts those phases in between the related PSP steps, so
            // we should respect the same ordering logic.) Handle this special
            // case by comparing the legacy process phases.
            if (!injPhase.isPspPhase() && remPhase.isPspPhase()) {
                injPos = getLegacyPhasePos(injPhase);
                remPos = getLegacyPhasePos(remPhase);
                if (injPos <= remPos || injPos == -1 || remPos == -1)
                    return true;
            }

        } else {
            // if the phases are legacy process phases, compare their positions
            // within the process.
            int injPos = processPhases.indexOf(injected);
            int remPos = processPhases.indexOf(removed);
            if (injPos == -1 || remPos == -1)
                return true;

            // if the injection phase precedes the removal phase, it's good
            if (injPos <= remPos)
                return true;
        }

        JOptionPane.showMessageDialog(this,
            resources.getStrings("Sequence_Error_Message"),
            resources.getString("Sequence_Error_Title"),
            JOptionPane.ERROR_MESSAGE);
        return false;
    }
    
    private Phase getWorkflowInfoPhase(DefectPhase p) {
        if (workflowPhases == null || workflowPhases.workflowInfo == null)
            return null;
        else
            return workflowPhases.workflowInfo.getPhase(p.getTerminalPhaseID());
    }

    private int getLegacyPhasePos(Phase workflowPhase) {
        String legacyPhase = workflowPhase.getMcfPhase();
        for (int i = processPhases.size(); i-- > 0;) {
            if (processPhases.get(i).legacyPhase.equals(legacyPhase))
                return i;
        }
        return -1;
    }


    /** Check to ensure that the user has selected a defect type.
     *
     * If they have not, display an error message to the user and
     * return false; otherwise return true.
     */
    private boolean checkValidType() {
        if (defect_type.getSelectedIndex() > 0)
            return true;

        JOptionPane.showMessageDialog
            (this, resources.getString("Choose_Defect_Type_Message"),
             resources.getString("Choose_Defect_Type_Title"),
             JOptionPane.ERROR_MESSAGE);

        return false;
    }


    /** Check to see if the defect has been modified, prior to a "Cancel"
     * operation.
     *
     * If the defect has not been modified, return true.  If the
     * defect HAS been modified, ask the user if they really want to
     * discard their changes.  If they do, return true; otherwise
     * returns false.
     */
    private boolean checkDirty() {
        return (!isDirty ||
                JOptionPane.showConfirmDialog
                (this, resources.getString("Confirm_Cancel_Message"),
                 resources.getString("Confirm_Cancel_Title"),
                 JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION);
    }

    /** Logic supporting a click of the OK button */
    private void okButtonAction() {
        if (checkSequence() && checkValidType()) {
            maybePopDialog();
            save();
            dispose();
        }
    }

    /** Logic supporting a click of the Cancel button */
    private void cancelButtonAction() {
        if (checkDirty()) {
            maybeDeleteAutocreatedDefect();
            maybePopDialog();
            dispose();
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == activeRefreshTimer)
            refreshFixTimeFromStopwatch();
        else if (e.getSource() == fixDefectButton)
            openFixDefectDialog();
        else
            // this event must be a notification of a change to one of the
            // JComboBoxes on the form.
            setDirty(true);
    }

    // Implementation of the DocumentListener interface

    private void handleDocumentEvent(DocumentEvent e) {
        if (e.getDocument() == fix_time.getDocument()) {
            // If the user edited the "Fix Time" field, perform the
            // necessary recalculations.
            fixTimeChanged();

        } else {
            // The user changed one of the other text fields on the form
            // (for example, the Fix Defect or the Description).
            setDirty(true);

            // if the change was in the description, scan for external links
            if (e.getDocument() == description.getDocument())
                linksButton.defectDescriptionChanged();
        }
    }

    public void changedUpdate(DocumentEvent e) { handleDocumentEvent(e); }
    public void insertUpdate(DocumentEvent e)  { handleDocumentEvent(e); }
    public void removeUpdate(DocumentEvent e)  { handleDocumentEvent(e); }

    // Implementation of the WindowListener interface

    public void windowOpened(WindowEvent e) {}
    public void windowClosing(WindowEvent e) { cancelButtonAction(); }
    public void windowClosed(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}

    private class StopwatchSynchronizer implements PropertyChangeListener {

        TimeLoggingModel timeLoggingModel;
        PropertyKey workflowRoot;
        boolean pausedByTimeLoggingModel = false;

        public StopwatchSynchronizer(TimeLoggingModel timeLoggingModel) {
            this.timeLoggingModel = timeLoggingModel;
            timeLoggingModel.addPropertyChangeListener(this);
        }

        public void dispose() {
            timeLoggingModel.removePropertyChangeListener(this);
        }

        public void userToggledDefectTimer() {
            pausedByTimeLoggingModel = false;
        }

        public void propertyChange(PropertyChangeEvent evt) {
            String propName = evt.getPropertyName();
            if (TimeLoggingModel.PAUSED_PROPERTY.equals(propName)) {
                boolean mainTimerIsPaused = timeLoggingModel.isPaused();
                if (mainTimerIsPaused && stopwatch.isRunning()) {
                    stopTimingDefect();
                    pausedByTimeLoggingModel = true;
                }
                if (!mainTimerIsPaused && pausedByTimeLoggingModel
                        && activeDialog == DefectDialog.this) {
                    startTimingDefect();
                    pausedByTimeLoggingModel = false;
                }

            } else if (TimeLoggingModel.ACTIVE_TASK_PROPERTY.equals(propName)
                    && !activeTaskMatches()) {
                // the global active task just changed, to something that
                // doesn't match the path we are logging a defect against.
                // stop the timer if it is running.
                if (stopwatch.isRunning())
                    stopTimingDefect();

                // don't implicitly restart the timer in the future
                pausedByTimeLoggingModel = false;
            }
        }

        private boolean activeTaskMatches() {
            // get the user's preference about how strict matching should be
            String userPref = getTaskMatchPref();
            if ("none".equalsIgnoreCase(userPref))
                return true;

            // retrieve the active task (the one selected on the main dashboard
            // toolbar). Is it the same as the task we are logging a defect
            // against?
            PropertyKey activeTask = timeLoggingModel.getActiveTaskModel()
                    .getNode();
            if (taskPath.equals(activeTask))
                return true;
            else if (activeTask == null || "exact".equalsIgnoreCase(userPref))
                return false;

            // is the active task underneath the node that owns our defect log?
            if (under(activeTask, defectPath))
                return true;
            else if ("defectLog".equalsIgnoreCase(userPref))
                return false;

            // is the active task underneath the current workflow root?
            if (under(activeTask, workflowRoot))
                return true;
            else if ("workflow".equals(userPref))
                return false;

            // is the active task is under the same component as the task we
            // are logging a defect against?
            if (under(activeTask, getEnclosingComponent(taskPath)))
                return true;
            else
                return false;
        }

        private String getTaskMatchPref() {
            String result = Settings.getVal("defectDialog.taskMatch");
            if (StringUtils.hasValue(result))
                return result;

            SimpleData sd = parent.getData().getSimpleValue(
                "/Defect_Timer_Task_Match_Policy");
            if (sd != null && sd.test())
                return sd.format();

            return "component";
        }

        private boolean under(PropertyKey node, PropertyKey parent) {
            return (node != null && parent != null
                    && (node.equals(parent) || node.isChildOf(parent)));
        }

        private PropertyKey getEnclosingComponent(PropertyKey node) {
            DashHierarchy hier = parent.getHierarchy();
            while (node != null) {
                String templateID = hier.getID(node);
                if (templateID != null && templateID.endsWith("ReadOnlyNode"))
                    return node;
                node = node.getParent();
            }
            return null;
        }
    }

    private class PendingSelector extends DropDownLabel {
        private boolean pending;
        private String removedLabel, pendingLabel;
        public PendingSelector() {
            removedLabel = resources.getString("Removed_Label");
            pendingLabel = resources.getString("Found_Label");
            setPending(false);
            getMenu().add(new PendingMenuOption(false));
            getMenu().add(new PendingMenuOption(true));
        }
        private boolean isPending() {
            return pending;
        }
        private void setPending(boolean fix_pending) {
            this.pending = fix_pending;
            setText(fix_pending ? pendingLabel : removedLabel);
        }
        private class PendingMenuOption extends AbstractAction {
            boolean pending;
            public PendingMenuOption(boolean pending) {
                this.pending = pending;
                String resKey = pending ? "Found_Option" : "Removed_Option";
                putValue(Action.NAME, resources.getString(resKey));
            }
            public void actionPerformed(ActionEvent e) {
                PendingSelector.this.setPending(pending);
            }
        }
    }

    private class StartStopButtons implements ActionListener {
        boolean running;
        JButton stopButton, startButton;
        public StartStopButtons() {
            stopButton = makeButton();
            startButton = makeButton();
            setRunning(false);
        }
        private JButton makeButton() {
            JButton result = new JButton();
            result.setMargin(new Insets(0,0,0,0));
            result.setFocusPainted(false);
            result.addActionListener(StartStopButtons.this);
            return result;
        }
        public void actionPerformed(ActionEvent e) {
            boolean shouldBeRunning = (e.getSource() == startButton);
            if (running != shouldBeRunning) {
                stopwatchSynchronizer.userToggledDefectTimer();
                if (shouldBeRunning)
                    startTimingDefect();
                else
                    stopTimingDefect();
                setDirty(true);
            }
        }
        private void setRunning(boolean running) {
            this.running = running;
            if (running) {
                startButton.setIcon(DashboardIconFactory.getPlayGlowingIcon());
                startButton.setToolTipText(resources.getString("Timing.Started"));
                stopButton.setIcon(DashboardIconFactory.getPauseBlackIcon());
                stopButton.setToolTipText(resources.getString("Timing.Pause"));
            } else {
                stopButton.setIcon(DashboardIconFactory.getPauseGlowingIcon());
                stopButton.setToolTipText(resources.getString("Timing.Paused"));
                startButton.setIcon(DashboardIconFactory.getPlayBlackIcon());
                startButton.setToolTipText(resources.getString("Timing.Start"));
            }
        }
    }

    private class ExternalLinksButton extends DropDownLabel implements
            PopupMenuListener, ActionListener {

        private Timer rescanTimer;

        private Map<String, String> links = Collections.EMPTY_MAP;

        public ExternalLinksButton() {
            super(" ");
            setIcon(DashboardIconFactory.getExternalLinkIcon());
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setVisible(false);

            rescanTimer = new Timer(1000, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    scanForLinks();
                }});

            getMenu().getPopupMenu().addPopupMenuListener(this);
        }

        // messaged when the user edits the defect description
        private void defectDescriptionChanged() {
            rescanTimer.restart();
        }

        private void scanForLinks() {
            rescanTimer.stop();
            String descrText = description.getText();
            Map newLinks = HTMLMarkup.getHyperlinks(descrText);
            if (!newLinks.equals(links)) {
                links = newLinks;
                getMenu().removeAll();
                for (String linkText : links.keySet()) {
                    JMenuItem menuItem = new JMenuItem(linkText);
                    menuItem.addActionListener(this);
                    getMenu().add(menuItem);
                }
                setToolTipText(calcToolTip());
            }
            setVisible(!links.isEmpty());
        }

        private String calcToolTip() {
            switch (links.size()) {
            case 0: return null;
            case 1: return links.keySet().iterator().next();
            default: return resources.getString("External_Link_Tooltip");
            }
        }

        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            // rescan to ensure that we have the most recent list of links
            scanForLinks();
            if (links.size() > 1)
                // if multiple links are present, let the menu appear normally
                return;

            else if (links.size() == 0)
                // if there are no links, hide the external links button
                setVisible(false);

            else if (links.size() == 1)
                // if there is just one link, open it.
                Browser.launch(links.values().iterator().next());

            // when 0 or 1 link is present, cancel display of the popup menu
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    getMenu().setPopupMenuVisible(false);
                }});
        }

        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
        public void popupMenuCanceled(PopupMenuEvent e) {}

        // called when the user clicks an option in the popup menu
        public void actionPerformed(ActionEvent e) {
            JMenuItem menuItem = (JMenuItem) e.getSource();
            String itemText = menuItem.getText();
            String url = links.get(itemText);
            if (url != null)
                Browser.launch(url);
        }

    }

    private class OKButtonAction extends AbstractAction {
        public OKButtonAction() {
            super(resources.getString("OK"));
        }
        public void actionPerformed(ActionEvent e) {
            okButtonAction();
        }
    }
    private class CancelButtonAction extends AbstractAction {
        public CancelButtonAction() {
            super(resources.getString("Cancel"));
        }
        public void actionPerformed(ActionEvent e) {
            cancelButtonAction();
        }
    }
}
