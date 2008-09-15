// Copyright (C) 2000-2008 Tuma Solutions, LLC
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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.defects.Defect;
import net.sourceforge.processdash.log.defects.DefectLog;
import net.sourceforge.processdash.log.defects.DefectUtil;
import net.sourceforge.processdash.process.DefectTypeStandard;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.DecimalField;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;
import net.sourceforge.processdash.util.Stopwatch;
import net.sourceforge.processdash.util.StringUtils;


public class DefectDialog extends JDialog
    implements ActionListener, DocumentListener, WindowListener
{
    ProcessDashboard parent;
    String defectFilename;
    PropertyKey defectPath;
    DefectLog defectLog = null;
    Stopwatch stopwatch = null;
    JButton defectTimerButton, OKButton, CancelButton, fixDefectButton;
    Date date = null;
    String defectNumber = null;
    JLabel number;
    JTextField fix_defect;
    DecimalField fix_time;
    JTextArea description;
    JComboBox defect_type, phase_injected, phase_removed;
    boolean isDirty = false, autoCreated = false;

    /** A stack of the defect dialogs that have been interrupted. */
    private static Stack interruptedDialogs = new Stack();
    /** The defect dialog which was timing most recently. */
    private static DefectDialog activeDialog = null;
    /** A list of all open defect dialogs. */
    private static Hashtable defectDialogs = new Hashtable();
    /** A timer object for refreshing the fix time field. */
    private javax.swing.Timer activeRefreshTimer = null;

    Resources resources = Resources.getDashBundle("Defects.Editor");


    DefectDialog(ProcessDashboard dash, String defectFilename,
                 PropertyKey defectPath) {
        this(dash, defectFilename, defectPath, true);
    }

    DefectDialog(ProcessDashboard dash, String defectFilename,
                 PropertyKey defectPath, boolean guessDefaults) {
        super(dash);
        setTitle(resources.getString("Window_Title"));
        PCSH.enableHelpKey(this, "EnteringDefects");

        parent = dash;
        this.defectFilename = defectFilename;
        this.defectPath = defectPath;
        defectLog = new DefectLog(defectFilename, defectPath.path(),
                                  dash.getData(), dash);
        date = new Date();
        stopwatch = new Stopwatch(false);
        stopwatch.setMultiplier(Settings.getVal("timer.multiplier"));

        JPanel panel = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints g = new GridBagConstraints();
        JComponent c;
        Insets bottom_margin = new Insets(1, 1, 8, 1);
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

        c = new JLabel(resources.getString("Fix_Defect_Label"));
        g.gridx = 1;   g.gridwidth = 2; layout.setConstraints(c, g);
        panel.add(c);

                                // second row
        g.gridy = 1;
        g.insets = bottom_margin;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridwidth = 1;
        defect_type = DefectTypeStandard.get
            (defectPath.path(), dash.getData()).getAsComboBox();
        defect_type.insertItemAt(resources.getString("Defect_Type_Prompt"), 0);
        defect_type.setMaximumRowCount(20);
        defect_type.setSelectedIndex(0);
        defect_type.addActionListener(this);

        g.gridx = 0;   layout.setConstraints(defect_type, g);
        panel.add(defect_type);

        fix_defect = new JTextField(5);
        fix_defect.getDocument().addDocumentListener(this);
        fix_defect.setMinimumSize(fix_defect.getPreferredSize());
        g.gridx = 1;   layout.setConstraints(fix_defect, g);
        panel.add(fix_defect);

        fixDefectButton = new JButton
            (DashboardIconFactory.getDefectIcon());
        fixDefectButton.addActionListener(this);
        g.fill = GridBagConstraints.NONE;
        g.gridx = 2;   layout.setConstraints(fixDefectButton, g);
        panel.add(fixDefectButton);

                                // third row
        g.gridy = 2;
        g.insets = small_margin;
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;

        c = new JLabel(resources.getString("Injected_Label"));
        g.gridx = 0;   layout.setConstraints(c, g);
        panel.add(c);

        c = new JLabel(resources.getString("Removed_Label"));
        g.gridx = 1;   g.gridwidth = 2;   layout.setConstraints(c, g);
        panel.add(c);

                                // fourth row
        g.gridy = 3;
        g.insets = bottom_margin;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.gridwidth = 1;

        List defectPhases = DefectUtil.getDefectPhases(defectPath.path(),
                parent);

        String defaultRemovalPhase = null;
        if (guessDefaults)
            defaultRemovalPhase = guessRemovalPhase(defectPath);
        phase_removed = phaseComboBox(defectPhases, defaultRemovalPhase);

        String defaultInjectionPhase = null;
        if (guessDefaults && defaultRemovalPhase != null)
            defaultInjectionPhase = DefectUtil.guessInjectionPhase(defectPhases,
                    defaultRemovalPhase);
        phase_injected = phaseComboBox(defectPhases, defaultInjectionPhase);

        phase_injected.insertItemAt("Before Development", 0);
        phase_injected.addActionListener(this);
        g.gridx = 0;   layout.setConstraints(phase_injected, g);
        panel.add(phase_injected);

        phase_removed.addItem("After Development");
        phase_removed.addActionListener(this);
        g.gridx = 1; g.gridwidth =2; layout.setConstraints(phase_removed, g);
        panel.add(phase_removed);

                                // fifth row
        g.gridy = 4;
        g.insets = small_margin;
        g.anchor = GridBagConstraints.WEST;
        g.gridwidth = 1;

        c = new JLabel(resources.getString("Fix_Time_Label"));
        g.gridx = 0;   layout.setConstraints(c, g);
        panel.add(c);

                                // sixth row
        g.gridy = 5;   g.insets = bottom_margin;

        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(1);
        fix_time = new DecimalField(0.0, 10, nf);
        fix_time.setMinimumSize(fix_time.getPreferredSize());
        g.anchor = GridBagConstraints.NORTHWEST;
        layout.setConstraints(fix_time, g);
        panel.add(fix_time);
        fix_time.getDocument().addDocumentListener(this);

        defectTimerButton = new JButton
            (resources.getString("Start_Fixing_Button"));
        defectTimerButton.addActionListener(this);
        g.gridx = 1;
        g.gridwidth = 2;
        g.fill = GridBagConstraints.NONE;
        g.anchor = GridBagConstraints.NORTHWEST;
        layout.setConstraints(defectTimerButton, g);
        panel.add(defectTimerButton);

                                // seventh row
        g.gridy = 6;
        g.insets = small_margin;
        g.anchor = GridBagConstraints.WEST;
        g.gridwidth = 1;
        c = new JLabel(resources.getString("Description_Label"));
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
        g.gridwidth = 3;   layout.setConstraints(textWrapper, g);
        panel.add(textWrapper);
        g.gridwidth = 1;
        g.weighty = 0;

                                // ninth row
        g.gridy = 8;
        g.insets = small_margin;
        g.anchor = GridBagConstraints.CENTER;
        g.fill = GridBagConstraints.NONE;

        OKButton = new JButton(resources.getString("OK"));
        OKButton.addActionListener(this);
        g.gridx = 0;   layout.setConstraints(OKButton, g);
        panel.add(OKButton);

        CancelButton = new JButton(resources.getString("Cancel"));
        CancelButton.addActionListener(this);
        g.gridx = 1; g.gridwidth = 2; layout.setConstraints(CancelButton, g);
        panel.add(CancelButton);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(this);

        getContentPane().add(panel);
        pack();
        panel.setMinimumSize(panel.getPreferredSize());
        setVisible(true);

        if ("true".equalsIgnoreCase(Settings.getVal("defectDialog.autostart")))
            startTimingDefect();
        setDirty(false);
    }

    private DefectDialog(ProcessDashboard dash, String defectFilename,
                         PropertyKey defectPath, Defect defect) {
        this(dash, defectFilename, defectPath, false);
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
        d.date = date;
        d.number = defectNumber;
        d.defect_type = (String)defect_type.getSelectedItem();
        d.phase_injected = (String)phase_injected.getSelectedItem();
        d.phase_removed = (String)phase_removed.getSelectedItem();
        d.fix_time = fix_time.getText();
        d.fix_defect = fix_defect.getText();
        d.description = description.getText();

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
        defectTimerButton.setText(resources.getString("Stop_Fixing_Button"));
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
        defectTimerButton.setText(resources.getString("Start_Fixing_Button"));
        if (activeRefreshTimer != null)
            activeRefreshTimer.stop();

        refreshFixTimeFromStopwatch();
    }

    private void toggleDefect() {
        boolean is_now_running = stopwatch.toggle();

        if (is_now_running)
            startTimingDefect();
        else
            stopTimingDefect();

        setDirty(true);
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

    private JComboBox phaseComboBox(List phases, String selectedChild) {
        JComboBox result = new JComboBox();

        for (Iterator i = phases.iterator(); i.hasNext();) {
            String phase = (String) i.next();
            result.addItem(phase);
            if (phase.equals(selectedChild))
                result.setSelectedItem(phase);
        }

        return result;
    }


    /** Make an educated guess about which removal phase might correspond to
     * the current dashboard state.
     */
    private String guessRemovalPhase(PropertyKey defectPath) {
        String phasePath = parent.getCurrentPhase().path();
        return DefectUtil.guessRemovalPhase(defectPath.path(), phasePath,
                parent);
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
        DefectDialog d = new DefectDialog(parent, defectFilename, defectPath);
        d.fix_defect.setText(defectNumber);
        comboSelect(d.phase_injected, (String)phase_removed.getSelectedItem());
        d.setDirty(false);
    }

    public void setValues(Defect d) {
        date = d.date;
        defectNumber = d.number;
        number.setText(formatDefectNum(d.number));
        comboSelect(defect_type, d.defect_type);
        comboSelect(phase_injected, d.phase_injected);
        comboSelect(phase_removed, d.phase_removed);
        fix_time.setText(d.fix_time); // will trigger fixTimeChanged
        fix_defect.setText(d.fix_defect);
        description.setText(d.description);
    }

    public void dispose() {
        hide_popups();
        if (activeRefreshTimer != null) {
            activeRefreshTimer.stop();
            activeRefreshTimer.removeActionListener(this);
            activeRefreshTimer = null;
        }
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

        // Ensure that the user isn't removing a defect before it is
        // injected.
        String injected = (String)phase_injected.getSelectedItem();
        String removed  = (String)phase_removed.getSelectedItem();

        int numOptions = phase_injected.getItemCount();
        String option;
        for (int i = 0;  i < numOptions;  i++) {
            option = (String) phase_injected.getItemAt(i);
            if (option.equalsIgnoreCase(injected))
                return true;
            if (option.equalsIgnoreCase(removed)) {
                JOptionPane.showMessageDialog
                    (this,
                     resources.getStrings("Sequence_Error_Message"),
                     resources.getString("Sequence_Error_Title"),
                     JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        // We shouldn't get here...
        return true;
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
        else if (e.getSource() == OKButton)
            okButtonAction();
        else if (e.getSource() == CancelButton)
            cancelButtonAction();
        else if (e.getSource() == defectTimerButton)
            toggleDefect();
        else if (e.getSource() == fixDefectButton)
            openFixDefectDialog();
        else
            // this event must be a notification of a change to one of the
            // JComboBoxes on the form.
            setDirty(true);
    }

    // Implementation of the DocumentListener interface

    private void handleDocumentEvent(DocumentEvent e) {
        if (e.getDocument() == fix_time.getDocument())
            // If the user edited the "Fix Time" field, perform the
            // necessary recalculations.
            fixTimeChanged();

        else
            // The user changed one of the other text fields on the form
            // (for example, the Fix Defect or the Description).
            setDirty(true);
    }

    public void changedUpdate(DocumentEvent e) {}
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
}
