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

import pspdash.data.DoubleData;
import pspdash.data.DataRepository;
import pspdash.data.DataCorrelator;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import java.util.*;

public class ProbeDialog extends JFrame implements
    ActionListener, FocusListener {
        PSPDashboard parent;
        DataRepository data;

        FilterDialog filterDlg = null;
        ChartDialog  chartDlg  = null;

        JButton filterButton, chartButton, closeButton;
        JComboBox method, yName, xName;
        JTextField estimate, percent;
        JLabel label, beta0, beta1, rSquared, significance, variance, stddev;
        JLabel estimateLabel, percentLabel, projection, range, upi, lpi;
        JLabel Cbeta0, Cbeta1, Cprojection;
        String initialValue;
        JPanel regressionBox, averageBox;

        DataCorrelator  corr;
        Vector          theFilter = null;
        boolean showRegression, showAverage;


        ProbeDialog(PSPDashboard dash) {
            super("PROBE");
            PCSH.enableHelpKey(this, "UsingProbeTool");
            setIconImage(dash.getIconImage());

            parent = dash;
            data   = parent.data;

            JPanel panel = new JPanel();
            GridBagLayout layout = new GridBagLayout();
            GridBagConstraints g = new GridBagConstraints();
            JComponent c;
            panel.setBorder(new EmptyBorder(5,5,5,5));
            panel.setLayout(layout);


            Insets left_margin = new Insets(0, 20, 0, 0);
            Insets no_margin = new Insets(0, 0, 0, 0);


            g.gridy = 0;   g.insets = no_margin;

            c = label = new JLabel("Method:");   g.gridx = 0;   g.gridwidth = 1;
            g.weightx = 0;   g.anchor = g.CENTER;   g.fill = g.NONE;
            layout.setConstraints(c, g);   panel.add(c);

            method = new JComboBox(PROBE_METHODS);   g.gridx = 1;  g.gridwidth = 3;
            g.weightx = 0;   g.fill = g.HORIZONTAL;
            layout.setConstraints(method, g);   panel.add(method);
            method.addActionListener(this);
            method.setMaximumRowCount(PROBE_METHODS.length);


            g.gridy++;              // next row

            c = new JLabel("Correlate");    g.gridx = 0;   g.gridwidth = 1;
            g.weightx = 0;   g.fill = g.NONE;
            layout.setConstraints(c, g);   panel.add(c);

            xName = new DataComboBox(data);
            g.gridx = 1;  g.gridwidth = 3;  g.weightx = 0;  g.fill = g.HORIZONTAL;
            layout.setConstraints(xName, g);   panel.add(xName);
            xName.setToolTipText
                ("The independent variable in the correlation, i.e. x");
            xName.addActionListener(this);
            xName.addFocusListener(this);


            g.gridy++;              // new row

            c = new JLabel("with");    g.gridx = 0;   g.gridwidth = 1;
            g.weightx = 0;   g.fill = g.NONE;
            layout.setConstraints(c, g);   panel.add(c);

            yName = new DataComboBox(data);
            g.gridx = 1;   g.gridwidth = 3;
            g.weightx = 0;   g.anchor = g.WEST;   g.fill = g.HORIZONTAL;
            layout.setConstraints(yName, g);   panel.add(yName);
            yName.setToolTipText
                ("The dependent variable in the correlation, i.e. y");
            yName.addActionListener(this);
            yName.addFocusListener(this);


            g.gridy++;              // new row: blank space

            c = new JLabel(" ");   g.gridx = 0;   g.gridwidth = 4;
            layout.setConstraints(c, g);   panel.add(c);


            g.gridy++;              // new row

            estimateLabel = new JLabel("Estimate");   g.gridx = 0;
            g.gridwidth = 1;   g.weightx = 0;   g.anchor = g.EAST;
            g.fill = g.NONE;   g.insets = no_margin;
            layout.setConstraints(estimateLabel, g);   panel.add(estimateLabel);

            estimate = new JTextField(7);   g.gridx = 1;   g.gridwidth = 1;
            g.anchor = g.WEST;
            estimate.setMinimumSize(estimate.getPreferredSize());
            layout.setConstraints(estimate, g);   panel.add(estimate);
            estimate.addActionListener(this);
            estimate.addFocusListener(this);

            percentLabel = new JLabel("% range");   g.gridx = 2;
            g.gridwidth = 1;   g.anchor = g.EAST;
            layout.setConstraints(percentLabel, g);   panel.add(percentLabel);

            percent = new JTextField("70", 7);   g.gridx = 3;   g.gridwidth = 1;
            g.anchor = g.WEST;
            percent.setMinimumSize(percent.getPreferredSize());
            layout.setConstraints(percent, g);   panel.add(percent);
            percent.addActionListener(this);
            percent.addFocusListener(this);


            g.gridy++;              // new row: blank space
            c = new JLabel(" ");   g.gridx = 0;   g.gridwidth = 4;
            layout.setConstraints(c, g);   panel.add(c);


            g.gridy++;
            g.fill = g.HORIZONTAL;

            JPanel box = new JPanel();
            box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
            box.setBorder(BorderFactory.createTitledBorder("L. Regression"));
            box.add(projection = new JLabel());
            box.add(beta0 = new JLabel());
            box.add(beta1 = new JLabel());
            box.add(rSquared = new JLabel());
            box.add(significance = new JLabel());
            box.add(variance = new JLabel());
            box.add(stddev = new JLabel());
            box.add(range = new JLabel());
            box.add(upi = new JLabel());
            box.add(lpi = new JLabel());
            g.gridx = 0;   g.gridwidth = 2;   g.gridheight = 5;
            g.insets = left_margin;
            layout.setConstraints(box, g);   panel.add(box);
            regressionBox = box;

            rSquared.setToolTipText
                ("The measure of how well these data elements correlate.");
            significance.setToolTipText
                ("the probability that this correlation could occur by chance.");

            box = new JPanel();
            box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
            box.setBorder(BorderFactory.createTitledBorder("Average"));
            box.add(Cprojection = new JLabel());
            box.add(Cbeta0 = new JLabel());
            box.add(Cbeta1 = new JLabel());
            g.gridx = 2;  g.gridwidth = 2;   g.gridheight = 1;
            layout.setConstraints(box, g);   panel.add(box);
            averageBox = box;


            g.gridy++;              //new row: blank space
            c = new JLabel(" ");   g.gridx = 2;   g.gridwidth = 2;
            g.weighty = 1;
            layout.setConstraints(c, g);   panel.add(c);

            g.gridy++;              //new row: filter button
            g.fill = g.HORIZONTAL;  g.weighty = 0;

            filterButton = new JButton ("Filter...");
            filterButton.setActionCommand("filter");
            filterButton.addActionListener(this);
            layout.setConstraints(filterButton, g);   panel.add(filterButton);

            g.gridy++;              //new row: chart button

            chartButton = new JButton ("Chart...");
            chartButton.setActionCommand("chart");
            chartButton.addActionListener(this);
            layout.setConstraints(chartButton, g);   panel.add(chartButton);

            g.gridy++;              //new row: close button

            closeButton = new JButton ("Close");
            closeButton.setActionCommand("close");
            closeButton.addActionListener(this);
            layout.setConstraints(closeButton, g);   panel.add(closeButton);


            getContentPane().add(panel);
            addWindowListener(new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                        ProbeDialog.this.close();
                    } });

            // This will cause all the labels to be populated
            setAFields (false, null, null, null, null, null, null);
            setARanges (false, null, null, null, null);
            setCFields (false, null, null);
            pack();
            show();

            // This will trigger a correlation in a background thread.
            method.setSelectedIndex(0);
        }

        private String format(double value, int numDecimalPoints) {
            return DoubleData.formatNumber(value, numDecimalPoints);
        }

        private static final String EST_OBJ_LOC = "Estimated Object LOC";
        private static final String EST_NC_LOC  = "Estimated New & Changed LOC";
        private static final String ACT_NC_LOC  = "New & Changed LOC";
        private static final String ACT_TIME    = "Time";

        private static final String[] PROBE_METHODS = {
            "PROBE Method A for Size", "PROBE Method A for Time",
            "PROBE Method B for Size", "PROBE Method B for Time",
            "PROBE Method C1 for Size",
            "PROBE Method C2 for Size",
            "PROBE Method C1 for Time",
            "PROBE Method C2 for Time",
            "PROBE Method C3 for Time",
            "-Custom-" };
        private static final boolean [] PROBE_SHOW_REGRESSION = {
            true, true, true, true, false, false, false, false, false };
        private static final String[] PROBE_X = {
            EST_OBJ_LOC, EST_OBJ_LOC,               // method A
            EST_NC_LOC,  EST_NC_LOC,                // method B
            EST_OBJ_LOC, EST_NC_LOC,                // method C for size
            EST_OBJ_LOC, EST_NC_LOC, ACT_NC_LOC };  // method C for time
        private static final String[] PROBE_Y = {
            ACT_NC_LOC, ACT_TIME,                   // method A
            ACT_NC_LOC, ACT_TIME,                   // method B
            ACT_NC_LOC, ACT_NC_LOC,                 // method C for size
            ACT_TIME, ACT_TIME, ACT_TIME };         // method C for time


        private void setAFields (boolean enableFields,
                                 String  b0Text,
                                 String  b1Text,
                                 String  rsText,
                                 String  sigText,
                                 String  varText,
                                 String  sdText) {
            beta0.setText        ((b0Text != null)  ? b0Text  : "Beta0 = ????");
            beta1.setText        ((b1Text != null)  ? b1Text  : "Beta1 = ????");
            rSquared.setText     ((rsText != null)  ? rsText  : "r\u00B2 = ????");
            significance.setText ((sigText != null) ? sigText : "p = ????");
            variance.setText     ((varText != null) ? varText : "Variance = ????");
            stddev.setText       ((sdText != null)  ? sdText  : "StdDev = ???? ");

            beta0.setEnabled(enableFields);           beta0.invalidate();
            beta1.setEnabled(enableFields);           beta1.invalidate();
            stddev.setEnabled(enableFields);          stddev.invalidate();
            variance.setEnabled(enableFields);        variance.invalidate();
            rSquared.setEnabled(enableFields);        rSquared.invalidate();
            significance.setEnabled(enableFields);    significance.invalidate();
        }

        private void setARanges (boolean enableFields,
                                 String  proText,
                                 String  ranText,
                                 String  upiText,
                                 String  lpiText) {
            projection.setText ((proText != null)  ? proText : "Projection = ????");
            range.setText      ((ranText != null)  ? ranText : "Range = ????");
            upi.setText        ((upiText != null)  ? upiText : "UPI = ????");
            lpi.setText        ((lpiText != null)  ? lpiText : "LPI = ????");

            projection.setEnabled(enableFields);      projection.invalidate();
            range.setEnabled(enableFields);           range.invalidate();
            upi.setEnabled(enableFields);             upi.invalidate();
            lpi.setEnabled(enableFields);             lpi.invalidate();
        }

        private void setCFields (boolean enableFields,
                                 String  b0Text,
                                 String  b1Text) {
            Cbeta0.setText        ((b0Text != null) ? b0Text  : "Beta0 = ????");
            Cbeta1.setText        ((b1Text != null) ? b1Text  : "Beta1 = ????");

            Cbeta0.setEnabled(enableFields);          Cbeta0.invalidate();
            Cbeta1.setEnabled(enableFields);          Cbeta1.invalidate();
        }

        private void setCRanges (boolean enableFields,
                                 String  proText) {
            Cprojection.setText ((proText != null)  ? proText : "Projection = ????");

            Cprojection.setEnabled(enableFields);     Cprojection.invalidate();
        }

        private void doDisable(JComponent c) {
            for (int i = c.getComponentCount();  i-- > 0; ) {
                c.getComponent(i).setEnabled(true);
                c.getComponent(i).setForeground(Color.lightGray);
            }
        }
        private void doEnable(JComponent c) {
            for (int i = c.getComponentCount();  i-- > 0; )
                c.getComponent(i).setForeground(label.getForeground());
        }

        private volatile Thread correlationThread = null;

        private void correlate() {
            synchronized (this) {
                correlationThread = new Thread() {
                        public void run() { doCorrelate(this); } };
                correlationThread.start();
            }
        }

        private void doCorrelate(Thread t) {
            String x_name = (String)xName.getSelectedItem();
            String y_name = (String)yName.getSelectedItem();
            boolean enableFields = false;
            if (showRegression) doEnable(regressionBox);
            if (showAverage)    doEnable(averageBox);

            if (x_name == null || x_name.length() == 0 ||
                y_name == null || y_name.length() == 0) {

                                      // for invalid input, blank all output fields
                corr = null;
                enableFields = false;
                setAFields (enableFields, null, null, null, null, null, null);

                rSquared.setForeground(Color.black);
                significance.setForeground(Color.black);

                setCFields (enableFields, null, null);

            } else {
                getContentPane().setCursor
                    (Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                                        // calculate and update output fields
                corr = new DataCorrelator(data, x_name, y_name, theFilter);
                synchronized (ProbeDialog.this) {
                    if (correlationThread != t) return;
                }
                enableFields = true;
                double var = corr.r.stddev * corr.r.stddev;
                double r_squared = corr.c.r * corr.c.r;

                setAFields (enableFields,
                            "Beta0 = " + format(corr.r.beta0, 4),
                            "Beta1 = " + format(corr.r.beta1, 4),
                            "r\u00B2 = " + format(r_squared, 4),
                            "p = " + format(corr.c.p * 100, 4) + "%",
                            "Variance = " + format(var, 2),
                            "StdDev = " + format(corr.r.stddev, 3));

                if (r_squared >= 0.9) rSquared.setForeground(Color.black);
                else if (r_squared >= 0.7) rSquared.setForeground(Color.blue);
                else if (r_squared >= 0.5) rSquared.setForeground(Color.orange.darker());
                else rSquared.setForeground(Color.red);

                if (corr.c.p <= 0.05) significance.setForeground(Color.black);
                else if (corr.c.p <= 0.12) significance.setForeground(Color.blue);
                else if (corr.c.p < 0.20) significance.setForeground(Color.orange.darker());
                else significance.setForeground(Color.red);

                setCFields (enableFields,
                            "Beta0 = " + format(0, 4),
                            "Beta1 = " + format(corr.r.y_avg / corr.r.x_avg, 4));
            }

            estimate.setEditable(enableFields);
            percent.setEditable(enableFields);

            estimateLabel.setText("Estimate");
            percentLabel.setText("% range");
            estimateLabel.setEnabled(enableFields);   estimateLabel.invalidate();
            estimate.setEnabled(enableFields);        estimate.invalidate();
            percentLabel.setEnabled(enableFields);    percentLabel.invalidate();
            percent.setEnabled(enableFields);         percent.invalidate();

            findRange();

            if (chartDlg != null)
                chartDlg.updateDialog (corr, x_name, y_name,
                                       showRegression, showAverage);

            if (!showRegression) doDisable(regressionBox);
            if (!showAverage)    doDisable(averageBox);

            getContentPane().setCursor(Cursor.getDefaultCursor());
        }

        private void findRange() {
            double est = Double.NaN;
            double prob = Double.NaN;
            boolean enableFields = false;

            if (corr != null) {
                try {
                    est = Long.valueOf(estimate.getText()).doubleValue();
                } catch (NumberFormatException nfe) { try {
                    est = Double.valueOf(estimate.getText()).doubleValue();
                } catch (NumberFormatException nfe2) {} }

                try {
                    prob = Integer.valueOf(percent.getText()).doubleValue() / 100.0;
                } catch (NumberFormatException nfe) { try {
                    prob = Double.valueOf(percent.getText()).doubleValue() / 100.0;
                } catch (NumberFormatException nfe2) {} }
            }

            if (corr == null || Double.isNaN(est) || Double.isNaN(prob)) {

                                      // for invalid input, blank all output fields
                enableFields = false;
                setARanges (enableFields, null, null, null, null);
                setCRanges (enableFields, null);

            } else {
                                      // calculate and update output fields
                corr.r.project(est, prob);
                enableFields = true;

                setARanges (enableFields,
                            "Projection = " + format(corr.r.projection, 2),
                            "Range = " + format(corr.r.range, 2),
                            "UPI = " + format(corr.r.UPI, 2),
                            "LPI = " + format(corr.r.LPI, 2));

                setCRanges (enableFields,
                            "Projection = " +
                            format((corr.r.y_avg / corr.r.x_avg) * est, 2));
            }

            if (!showRegression) doDisable(regressionBox);
            if (!showAverage)    doDisable(averageBox);

            pack();
        }

        private boolean programaticallyChangingXY = false;
        public void close() {
            if (filterDlg != null) filterDlg.setVisible(false);
            if (chartDlg != null) chartDlg.setVisible(false);
            setVisible(false);
        }

        public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
            if (cmd.equals("filter")) {
                //Bring up a dialog here that allows filtering of data used.
                //(at least by template'd node)
                if (filterDlg == null)
                    filterDlg = new FilterDialog (parent, this, this);
                else
                    filterDlg.show();
            } else if (cmd.equals("chart")) {
                //Bring up a dialog here that shows the data used.
                if (chartDlg == null)
                    chartDlg = new ChartDialog (this,
                                                corr,
                                                (String)xName.getSelectedItem(),
                                                (String)yName.getSelectedItem(),
                                                showRegression, showAverage);
                else
                    chartDlg.show();
            } else if (cmd.equals("close")) {
                close();
            } else if (cmd.equals("applyFilter")) { // response from filter dlg
                theFilter = (Vector)e.getSource();
                correlate();
            } else if (e.getSource() == method) {
                int index = method.getSelectedIndex();
                if (index < PROBE_X.length) {
                    programaticallyChangingXY = true;
                    xName.setSelectedItem(PROBE_X[index]);
                    yName.setSelectedItem(PROBE_Y[index]);
                    programaticallyChangingXY = false;
                    showRegression = PROBE_SHOW_REGRESSION[index];
                    showAverage = !showRegression;
                } else
                    showRegression = showAverage = true;
                correlate();
            } else if (e.getSource() == estimate || e.getSource() == percent) {
                findRange();
            } else if (!programaticallyChangingXY &&
                       (e.getSource() == xName || e.getSource() == yName)) {
                method.setSelectedIndex(PROBE_METHODS.length - 1);
                showRegression = showAverage = true;
                correlate();
            }
        }

        public void focusGained(FocusEvent e) {
            initialValue = ((JTextField)e.getSource()).getText();
        }

        public void focusLost(FocusEvent e) {
            if (!initialValue.equals(((JTextField)e.getSource()).getText()))
                if (e.getSource() == xName || e.getSource() == yName) {
                    correlate();
                } else if (e.getSource() == estimate || e.getSource() == percent) {
                    findRange();
                }
        }

}
