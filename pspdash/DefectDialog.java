// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import java.util.*;

public class DefectDialog extends JDialog implements ActionListener {
    PSPDashboard parent;
    DefectLog defectLog = null;
    Timer stopwatch = new Timer(false);
    JButton defectTimerButton, OKButton, CancelButton;
    Date date = null;
    String defectNumber = null;
    JLabel number;
    JTextField fix_time, fix_defect;
    JTextArea description;
    JComboBox defect_type, phase_injected, phase_removed;


    DefectDialog(PSPDashboard dash, String defectFilename,
                 PropertyKey defectPath) {
        super(dash);

        parent = dash;
        defectLog = new DefectLog(defectFilename, defectPath.path(),
                                  dash.data, dash);
        date = new Date();

        JPanel panel = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints g = new GridBagConstraints();
        JComponent c;
        Insets bottom_margin = new Insets(1, 1, 8, 1);
        Insets small_margin = new Insets(1, 1, 1, 1);
        panel.setBorder(new EmptyBorder(5,5,5,5));
        panel.setLayout(layout);

                                // first row
        g.gridy = 0;   g.insets = small_margin; g.anchor = g.WEST;

        number = new JLabel();
        g.gridx = 0;   layout.setConstraints(number, g);
        panel.add(number);

        c = new JLabel("Fix Defect");
        g.gridx = 1;   layout.setConstraints(c, g);
        panel.add(c);

                                // second row
        g.gridy = 1;   g.insets = bottom_margin;   g.anchor = g.NORTHWEST;
        defect_type = new JComboBox();
        ToolTipCellRenderer renderer = new ToolTipCellRenderer();
        defect_type.setRenderer(renderer);

        defect_type.addItem("Documentation");
        renderer.setToolTip("Documentation", "comments, messages");
        defect_type.addItem("Syntax");
        renderer.setToolTip("Syntax", "spelling, punctuation, typos, instruction formats");
        defect_type.addItem("Build, package");
        renderer.setToolTip("Build, package", "change management, library, version control");
        defect_type.addItem("Assignment");
        renderer.setToolTip("Assignment", "declaration, duplicate names, scope, limits");
        defect_type.addItem("Interface");
        renderer.setToolTip("Interface", "procedure calls and reference, I/O, user formats");
        defect_type.addItem("Checking");
        renderer.setToolTip("Checking", "error messages, inadequate checks");
        defect_type.addItem("Data");
        renderer.setToolTip("Data", "structure, content");
        defect_type.addItem("Function");
        renderer.setToolTip("Function", "logic, pointers, loops, recursion, computation, function defects");
        defect_type.addItem("System");
        renderer.setToolTip("System", "configuration, timing, memory");
        defect_type.addItem("Environment");
        renderer.setToolTip("Environment", "design, compile, test, or other support system problems");

        g.gridx = 0;   layout.setConstraints(defect_type, g);
        panel.add(defect_type);

        fix_defect = new JTextField(5);
        g.gridx = 1;   layout.setConstraints(fix_defect, g);
        panel.add(fix_defect);

                                // third row
        g.gridy = 2;   g.insets = small_margin;   g.anchor = g.WEST;

        c = new JLabel("Injected");
        g.gridx = 0;   layout.setConstraints(c, g);
        panel.add(c);

        c = new JLabel("Removed");
        g.gridx = 1;   layout.setConstraints(c, g);
        panel.add(c);

                                // fourth row
        g.gridy = 3;   g.insets = bottom_margin;   g.anchor = g.NORTHWEST;

        phase_injected = phaseComboBox(defectPath);
        phase_injected.insertItemAt("Before Development", 0);
        g.gridx = 0;   layout.setConstraints(phase_injected, g);
        panel.add(phase_injected);

        phase_removed = phaseComboBox(defectPath);
        phase_removed.addItem("After Development");
        g.gridx = 1;   layout.setConstraints(phase_removed, g);
        panel.add(phase_removed);

                                // fifth row
        g.gridy = 4;   g.insets = small_margin;   g.anchor = g.WEST;

        c = new JLabel("Fix Time");
        g.gridx = 0;   layout.setConstraints(c, g);
        panel.add(c);

                                // sixth row
        g.gridy = 5;   g.insets = bottom_margin;

        fix_time = new JTextField(10);
        g.anchor = g.NORTHWEST;   layout.setConstraints(fix_time, g);
        panel.add(fix_time);

        defectTimerButton = new JButton("Start Fixing");
        defectTimerButton.addActionListener(this);
        g.gridx = 1;   g.anchor = g.NORTH;
        layout.setConstraints(defectTimerButton, g);
        panel.add(defectTimerButton);

                                // seventh row
        g.gridy = 6;   g.insets = small_margin;   g.anchor = g.WEST;
        c = new JLabel("Description");
        g.gridx = 0;   layout.setConstraints(c, g);
        panel.add(c);

                                // eighth row
        g.gridy = 7;   g.insets = bottom_margin;  g.fill = g.BOTH;
        description = new JTextArea();

        JScrollPane scroller = new
            JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {
                public Dimension getPreferredSize() {
                    return new Dimension(100, 100);
                }
            };
        scroller.getViewport().add(description);

        JPanel textWrapper = new JPanel(new BorderLayout());
//	textWrapper.setAlignmentX(LEFT_ALIGNMENT);
        textWrapper.setBorder(new BevelBorder(BevelBorder.LOWERED));
        textWrapper.add("Center", scroller);

        g.gridwidth = 2;   layout.setConstraints(textWrapper, g);
        panel.add(textWrapper);
        g.gridwidth = 1;

                                // ninth row
        g.gridy = 8;  g.insets = small_margin;
        g.anchor = g.CENTER;   g.fill = g.NONE;

        OKButton = new JButton("OK");
        OKButton.addActionListener(this);
        g.gridx = 0;   layout.setConstraints(OKButton, g);
        panel.add(OKButton);

        CancelButton = new JButton("Cancel");
        CancelButton.addActionListener(this);
        g.gridx = 1;   layout.setConstraints(CancelButton, g);
        panel.add(CancelButton);

        getContentPane().add(panel);
        pack();
        show();

        if ("true".equalsIgnoreCase(Settings.getVal("defectDialog.autostart")))
            startTimingDefect();
    }

    DefectDialog(PSPDashboard dash, String defectFilename,
                 PropertyKey defectPath, Defect defect) {
        this(dash, defectFilename, defectPath);
        stopTimingDefect();
        setValues(defect);
    }

    public void save() {
        stopTimingDefect();

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
    }

    public void startTimingDefect() {
        stopwatch.start();
        defectTimerButton.setText("Stop Fixing");
    }

    public void stopTimingDefect() {
        stopwatch.stop();
        defectTimerButton.setText("Start Fixing");

        double time;
        try {
            time = Double.valueOf(fix_time.getText()).doubleValue();
        } catch (NumberFormatException e) { time = 0.0; }
        time += stopwatch.minutesElapsedDouble();
        String timeStr = Double.toString(time);

        fix_time.setText(timeStr.substring(0, timeStr.indexOf('.')+2));

        stopwatch.reset();
    }

    private void toggleDefect() {
        boolean is_now_running = stopwatch.toggle();

        if (is_now_running)
            startTimingDefect();
        else
            stopTimingDefect();
    }

    private void comboSelect(JComboBox cb, String item) {
        int i = cb.getItemCount();
        while (i != 0)
            if (item.equals(cb.getItemAt(--i))) {
                cb.setSelectedIndex(i);
                break;
            }
    }

    private JComboBox phaseComboBox(PropertyKey defectPath) {
        JComboBox result = new JComboBox();

        int prefixLength = defectPath.path().length() + 1;
        String item = null,
            selectedChild = parent.currentPhase.path().substring(prefixLength);
        Enumeration leafNames = parent.getProperties().getLeafNames(defectPath);

        while (leafNames.hasMoreElements()) {
            item = ((String)leafNames.nextElement()).substring(prefixLength);
            result.addItem(item);
            if (item.equals(selectedChild))
                result.setSelectedItem(item);
        }

        return result;
    }

    private void hide_popups() {
        defect_type.hidePopup();
        phase_injected.hidePopup();
        phase_removed.hidePopup();
    }

    public void setValues(Defect d) {
        date = d.date;
        defectNumber = d.number;
        number.setText("Defect #" + d.number);
        comboSelect(defect_type, d.defect_type);
        comboSelect(phase_injected, d.phase_injected);
        comboSelect(phase_removed, d.phase_removed);
        fix_time.setText(d.fix_time);
        fix_defect.setText(d.fix_defect);
        description.setText(d.description);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == OKButton) {
            save(); hide_popups(); dispose();
        } else if (e.getSource() == CancelButton) {
            hide_popups(); dispose();
        } else if (e.getSource() == defectTimerButton) {
            toggleDefect();
        }
    }

}
