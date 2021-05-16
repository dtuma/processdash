// Copyright (C) 2000-2021 Tuma Solutions, LLC
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


package net.sourceforge.processdash.tool.probe;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.util.DataCorrelator;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.DataComboBox;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.SwingWorker;
import net.sourceforge.processdash.util.FormatUtil;


public class ProbeDialog extends JFrame implements
      ActionListener, DocumentListener {

    DashHierarchy hier;
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

    private javax.swing.Timer docChangeTimer;

    Resources resources = Resources.getDashBundle("PROBE");


    public ProbeDialog(DashHierarchy props, DataRepository data) {
        super();
        setTitle(resources.getString("PROBE_Window_Title"));
        PCSH.enableHelpKey(this, "UsingProbeTool");
        DashboardIconFactory.setWindowIcon(this);

        this.hier = props;
        this.data = data;

        docChangeTimer = new javax.swing.Timer(Integer.MAX_VALUE, this);
        docChangeTimer.setRepeats(false);
        docChangeTimer.setInitialDelay(500);

        JPanel panel = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints g = new GridBagConstraints();
        JComponent c;
        panel.setBorder(new EmptyBorder(5,5,5,5));
        panel.setLayout(layout);


        Insets left_margin = new Insets(0, 20, 0, 0);
        Insets no_margin = new Insets(0, 0, 0, 0);


        g.gridy = 0;   g.insets = no_margin;

        c = label = new JLabel(resources.getString("Method_Label"));
        g.gridx = 0;
        g.gridwidth = 1;
        g.weightx = 0;
        g.anchor = GridBagConstraints.CENTER;
        g.fill = GridBagConstraints.NONE;
        layout.setConstraints(c, g);
        panel.add(c);

        method = new JComboBox(PROBE_METHODS);
        g.gridx = 1;
        g.gridwidth = 3;
        g.weightx = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        layout.setConstraints(method, g);
        panel.add(method);
        method.addActionListener(this);
        method.setMaximumRowCount(PROBE_METHODS.length);


        g.gridy++;              // next row

        c = new JLabel(resources.getString("X_Label"));
        g.gridx = 0;
        g.gridwidth = 1;
        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        layout.setConstraints(c, g);
        panel.add(c);

        xName = new DataComboBox(data);
        g.gridx = 1;
        g.gridwidth = 3;
        g.weightx = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        layout.setConstraints(xName, g);
        panel.add(xName);
        xName.setToolTipText(resources.getString("X_Description"));
        xName.addActionListener(this);


        g.gridy++; // new row

        c = new JLabel(resources.getString("Y_Label"));
        g.gridx = 0;
        g.gridwidth = 1;
        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        layout.setConstraints(c, g);
        panel.add(c);

        yName = new DataComboBox(data);
        g.gridx = 1;
        g.gridwidth = 3;
        g.weightx = 0;
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        layout.setConstraints(yName, g);
        panel.add(yName);
        yName.setToolTipText(resources.getString("Y_Description"));
        yName.addActionListener(this);


        g.gridy++;              // new row: blank space

        c = new JLabel(" ");
        g.gridx = 0;
        g.gridwidth = 4;
        layout.setConstraints(c, g);
        panel.add(c);


        g.gridy++;              // new row

        estimateLabel = new JLabel(resources.getString("Estimate_Label"));
        g.gridx = 0;
        g.gridwidth = 1;
        g.weightx = 0;
        g.anchor = GridBagConstraints.EAST;
        g.fill = GridBagConstraints.NONE;
        g.insets = no_margin;
        layout.setConstraints(estimateLabel, g);
        panel.add(estimateLabel);

        estimate = new JTextField(7);
        g.gridx = 1;
        g.gridwidth = 1;
        g.anchor = GridBagConstraints.WEST;
        estimate.setMinimumSize(estimate.getPreferredSize());
        layout.setConstraints(estimate, g);
        panel.add(estimate);
        estimate.addActionListener(this);
        estimate.getDocument().addDocumentListener(this);

        percentLabel = new JLabel(resources.getString("Percent_Label"));
        g.gridx = 2;
        g.gridwidth = 1;
        g.anchor = GridBagConstraints.EAST;
        layout.setConstraints(percentLabel, g);
        panel.add(percentLabel);

        percent = new JTextField("70", 7);
        g.gridx = 3;
        g.gridwidth = 1;
        g.anchor = GridBagConstraints.WEST;
        percent.setMinimumSize(percent.getPreferredSize());
        layout.setConstraints(percent, g);
        panel.add(percent);
        percent.addActionListener(this);
        percent.getDocument().addDocumentListener(this);


        g.gridy++;              // new row: blank space
        c = new JLabel(" ");
        g.gridx = 0;
        g.gridwidth = 4;
        layout.setConstraints(c, g);
        panel.add(c);


        g.gridy++;
        g.fill = GridBagConstraints.HORIZONTAL;

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createTitledBorder
                      (resources.getString("L_Regression")));
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
        g.gridx = 0;
        g.gridwidth = 2;
        g.gridheight = 5;
        g.insets = left_margin;
        layout.setConstraints(box, g);
        panel.add(box);
        regressionBox = box;

        rSquared.setToolTipText(resources.getString("R_Squared_Description"));
        significance.setToolTipText
            (resources.getString("Significance_Description"));

        box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createTitledBorder
                      (resources.getString("Average")));
        box.add(Cprojection = new JLabel());
        box.add(Cbeta0 = new JLabel());
        box.add(Cbeta1 = new JLabel());
        g.gridx = 2;
        g.gridwidth = 2;
        g.gridheight = 1;
        layout.setConstraints(box, g);
        panel.add(box);
        averageBox = box;


        g.gridy++;              //new row: blank space
        c = new JLabel(" ");
        g.gridx = 2;
        g.gridwidth = 2;
        g.weighty = 1;
        layout.setConstraints(c, g);
        panel.add(c);

        g.gridy++;              //new row: filter button
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weighty = 0;

        filterButton = new JButton (resources.getString("Filter_Button"));
        filterButton.setActionCommand("filter");
        filterButton.addActionListener(this);
        layout.setConstraints(filterButton, g);
        panel.add(filterButton);

        g.gridy++;              //new row: chart button

        chartButton = new JButton(resources.getString("Chart_Button"));
        chartButton.setActionCommand("chart");
        chartButton.addActionListener(this);
        layout.setConstraints(chartButton, g);
        panel.add(chartButton);

        g.gridy++;              //new row: close button

        closeButton = new JButton(resources.getString("Close"));
        closeButton.setActionCommand("close");
        closeButton.addActionListener(this);
        layout.setConstraints(closeButton, g);
        panel.add(closeButton);


        getContentPane().add(panel);
        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    ProbeDialog.this.close();
                } });

        // This will cause all the labels to be populated
        setAFields (false, null, null, null, null, null, null);
        setARanges (false, null, null, null, null);
        setCFields (false, null, null);

        // In addition to synchronizing the "method", "x_name", and
        // "y_name" combo boxes, this will trigger a correlation in a
        // background thread.
        method.setSelectedIndex(0);

        // display the dialog
        pack();
        DashController.setRelativeLocation(this, 100, 100);
        setVisible(true);
    }

    private String notEnoughData = resources.getString("Not_Enough_Data");
    private NumberFormat percentFormat = NumberFormat.getPercentInstance();
    private String formatPercent(double value, int numDecimalPoints) {
        if (Double.isNaN(value) || Double.isInfinite(value))
            return notEnoughData;
        percentFormat.setMaximumFractionDigits(numDecimalPoints);
        return percentFormat.format(value);
    }

    private String format(double value, int numDecimalPoints) {
        if (Double.isInfinite(value) || Double.isNaN(value))
            return notEnoughData;
        else
            return FormatUtil.formatNumber(value, numDecimalPoints);
    }

    // these variables name elements in the data repository, and
    // therefore cannot currently be localized.
    private static final String EST_OBJ_LOC = "Estimated Object LOC";
    private static final String EST_NC_LOC  = "Estimated New & Changed LOC";
    private static final String ACT_NC_LOC  = "New & Changed LOC";
    private static final String ACT_TIME    = "Time";

    private String[] PROBE_METHODS = buildProbeMethods();
    private String[] buildProbeMethods() {
        String[] result = new String[10];
        String size = resources.getString("Size");
        String time = resources.getString("Time");
        result[0]  = resources.format("Method_FMT", "A", size);
        result[1]  = resources.format("Method_FMT", "A", time);
        result[2]  = resources.format("Method_FMT", "B", size);
        result[3]  = resources.format("Method_FMT", "B", time);
        result[4]  = resources.format("Method_FMT", "C1", size);
        result[5]  = resources.format("Method_FMT", "C2", size);
        result[6]  = resources.format("Method_FMT", "C1", time);
        result[7]  = resources.format("Method_FMT", "C2", time);
        result[8]  = resources.format("Method_FMT", "C3", time);
        result[9]  = resources.getString("Custom_Method");
        return result;
    }
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


    private String unknownValue = resources.getString("Unknown_Value");
    private String formatField(String key, String value) {
        String label = resources.getString(key);
        if (value == null)
            return label + " = " + unknownValue;
        else
            return label + " = " + value;
    }

    private void setAFields (boolean enableFields,
                             String  b0Text,
                             String  b1Text,
                             String  rsText,
                             String  sigText,
                             String  varText,
                             String  sdText) {
        beta0.setText        (formatField("Beta0", b0Text));
        beta1.setText        (formatField("Beta1", b1Text));
        rSquared.setText     (formatField("R_Squared", rsText));
        significance.setText (formatField("Significance", sigText));
        variance.setText     (formatField("Variance", varText));
        stddev.setText       (formatField("Standard_Deviation", sdText));

        beta0.setEnabled(enableFields);
        beta1.setEnabled(enableFields);
        stddev.setEnabled(enableFields);
        variance.setEnabled(enableFields);
        rSquared.setEnabled(enableFields);
        significance.setEnabled(enableFields);
    }

    private void setARanges (boolean enableFields,
                             String  proText,
                             String  ranText,
                             String  upiText,
                             String  lpiText) {
        projection.setText (formatField("Projection", proText));
        range.setText      (formatField("Range", ranText));
        upi.setText        (formatField("UPI", upiText));
        lpi.setText        (formatField("LPI", lpiText));

        projection.setEnabled(enableFields);
        range.setEnabled(enableFields);
        upi.setEnabled(enableFields);
        lpi.setEnabled(enableFields);
    }

    private void setCFields (boolean enableFields,
                             String  b0Text,
                             String  b1Text) {
        Cbeta0.setText        (formatField("Beta0", b0Text));
        Cbeta1.setText        (formatField("Beta1", b1Text));

        Cbeta0.setEnabled(enableFields);
        Cbeta1.setEnabled(enableFields);
    }

    private void setCRanges (boolean enableFields,
                             String  proText) {
        Cprojection.setText (formatField("Projection", proText));

        Cprojection.setEnabled(enableFields);
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

    private volatile CorrelationWorker correlationWorker = null;

    private void correlate() {
        //System.out.println("correlate");
        synchronized (this) {
            correlationWorker = new CorrelationWorker();
            correlationWorker.start();
        }
    }

    private class CorrelationWorker extends SwingWorker {
        String x_name, y_name;

        public CorrelationWorker() {
            x_name = (String)xName.getSelectedItem();
            y_name = (String)yName.getSelectedItem();

            // display a wait cursor
            getContentPane().setCursor
                (Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }

        public Object construct() {
            if (x_name == null || x_name.length() == 0 ||
                y_name == null || y_name.length() == 0)
                return null;

            return new DataCorrelator(data, x_name, y_name, theFilter);
        }

        public void finished() {
            DataCorrelator corr = (DataCorrelator) getValue();

            synchronized (ProbeDialog.this) {
                if (correlationWorker != this) return;
                ProbeDialog.this.corr = corr;
            }

            getContentPane().setCursor(Cursor.getDefaultCursor());

            updateValuesAfterCorrelation(x_name, y_name);
        }
    }

    private void updateValuesAfterCorrelation(String x_name, String y_name) {
        boolean enableFields = false;

        if (showRegression) doEnable(regressionBox);
        if (showAverage)    doEnable(averageBox);

        if (corr == null) {
                        // for invalid input, blank all output fields
            enableFields = false;
            setAFields(enableFields, null, null, null, null, null, null);

            rSquared.setForeground(Color.black);
            significance.setForeground(Color.black);

            setCFields(enableFields, null, null);

        } else {
                        // update output fields with calculated values
            enableFields = true;
            double var = corr.r.stddev * corr.r.stddev;
            double r_squared = corr.c.r * corr.c.r;

            setAFields (enableFields,
                        format(corr.r.beta0, 4),
                        format(corr.r.beta1, 4),
                        format(r_squared, 4),
                        formatPercent(corr.c.p, 2),
                        format(var, 2),
                        format(corr.r.stddev, 3));

            setColorFromRange(rSquared, r_squared, R_SQUARED_COLORS);
            setColorFromRange(significance, corr.c.p, SIGNIFICANCE_COLORS);

            setCFields (enableFields,
                        format(0, 4),
                        format(corr.r.y_avg / corr.r.x_avg, 4));
        }

        estimate.setEditable(enableFields);
        percent.setEditable(enableFields);

        //estimateLabel.setText("Estimate");
        //percentLabel.setText("% range");
        estimateLabel.setEnabled(enableFields);
        estimate.setEnabled(enableFields);
        percentLabel.setEnabled(enableFields);
        percent.setEnabled(enableFields);

        findRange();

        if (chartDlg != null)
            chartDlg.updateDialog (corr, x_name, y_name,
                                   showRegression, showAverage);

        if (!showRegression) doDisable(regressionBox);
        if (!showAverage)    doDisable(averageBox);
    }

    private Object[][] R_SQUARED_COLORS = {
        { null, Color.red },
        { new Double(0.5), Color.orange.darker() },
        { new Double(0.7), Color.blue },
        { new Double(0.9), Color.black } };
    private Object[][] SIGNIFICANCE_COLORS = {
        { null, Color.black },
        { new Double(0.0501), Color.blue },
        { new Double(0.1201), Color.orange.darker() },
        { new Double(0.2001), Color.red } };


    private void setColorFromRange(JComponent c, double val,
                                   Object[][] colorRanges) {
        for (int i = colorRanges.length; i-- > 1; ) {
            if (val >= ((Number) colorRanges[i][0]).doubleValue()) {
                c.setForeground((Color) colorRanges[i][1]);
                return;
            }
        }
        c.setForeground((Color) colorRanges[0][1]);
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
                        format(corr.r.projection, 2),
                        format(corr.r.range, 2),
                        format(corr.r.UPI, 2),
                        format(corr.r.LPI, 2));

            setCRanges (enableFields,
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
        if ("filter".equals(cmd)) {
            //Bring up a dialog here that allows filtering of data used.
            //(at least by template'd node)
            if (filterDlg == null)
                filterDlg = new FilterDialog (hier, this, this);
            else
                filterDlg.setVisible(true);
        } else if ("chart".equals(cmd)) {
            //Bring up a dialog here that shows the data used.
            if (chartDlg == null)
                chartDlg = new ChartDialog (this,
                                            corr,
                                            (String)xName.getSelectedItem(),
                                            (String)yName.getSelectedItem(),
                                            showRegression, showAverage);
            else
                chartDlg.setVisible(true);
        } else if ("close".equals(cmd)) {
            close();
        } else if ("applyFilter".equals(cmd)) { // response from filter dlg
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
            if (!programaticallyChangingXY)
                correlate();
        } else if (e.getSource() == estimate || e.getSource() == percent ||
                   e.getSource() == docChangeTimer) {
            findRange();
        } else if (!programaticallyChangingXY &&
                   (e.getSource() == xName || e.getSource() == yName)) {
            programaticallyChangingXY = true;
            method.setSelectedIndex(PROBE_METHODS.length - 1);
            programaticallyChangingXY = false;
            showRegression = showAverage = true;
            correlate();
        }
    }


    public void insertUpdate(DocumentEvent e) { docChangeTimer.restart(); }
    public void removeUpdate(DocumentEvent e) { docChangeTimer.restart(); }
    public void changedUpdate(DocumentEvent e) {}

}
