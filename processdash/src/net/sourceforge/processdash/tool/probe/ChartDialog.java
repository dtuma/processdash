// Copyright (C) 2000-2003 Tuma Solutions, LLC
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

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import net.sourceforge.processdash.data.util.DataCorrelator;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.ui.help.PCSH;


public class ChartDialog extends JDialog {

    DataCorrelator corr;
    Vector names, points;
    ChartCanvas chart;
    JTable table;
    Vector titles;
    DefaultTableModel tableModel;

    Resources resources = Resources.getDashBundle("PROBE");


    public ChartDialog (Frame parent,
                        DataCorrelator c,
                        String labelX,
                        String labelY,
                        boolean showRegression,
                        boolean showAverage) {
        super (parent);
        setTitle(resources.getString("Chart_Window_Title"));
        PCSH.enableHelpKey(this, "UsingProbeTool.chart");
        corr = c;

        names  = (corr == null) ? null : corr.getDataNames();
        points = (corr == null) ? null : corr.getDataPoints();
        labelX = Translator.translate(labelX);
        labelY = Translator.translate(labelY);

        chart = new ChartCanvas(c, labelX, labelY, showRegression, showAverage);


        if (labelX == null) labelX = resources.getString("Empty_Label");
        if (labelY == null) labelY = resources.getString("Empty_Label");

        titles = new Vector();
        titles.add(resources.getString("Task_Project"));
        titles.add(labelX);
        titles.add(labelY);

        tableModel = new DefaultTableModel(dataArray(names, points), titles);
        table = new JTable (tableModel);
        table.setMinimumSize (new Dimension (0,0));

        JScrollPane scrollpane = new JScrollPane(table);
        scrollpane.setMinimumSize (new Dimension (0,0));
        scrollpane.setPreferredSize(new Dimension(200, 200));

        JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                        true,
                                        chart,
                                        scrollpane);
        jsp.setResizeWeight(0.5);

        getContentPane().add(jsp, "Center");

        Box buttonBox = new Box(BoxLayout.X_AXIS);
        buttonBox.add (Box.createGlue());
        JButton button = new JButton (resources.getString("Close"));
        button.setActionCommand("close");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ChartDialog.this.setVisible(false);
            } });
        buttonBox.add (button);
        buttonBox.add (Box.createGlue());
        getContentPane().add(buttonBox, "South");

        setSize(new Dimension(450, 600));
        show();
    }

    public void updateDialog (DataCorrelator c,
                              String labelX, String labelY,
                              boolean showRegression, boolean showAverage) {
        corr = c;
        names  = (corr == null) ? null : corr.getDataNames();
        points = (corr == null) ? null : corr.getDataPoints();
        labelX = Translator.translate(labelX);
        labelY = Translator.translate(labelY);

        // set the column names
        titles.setElementAt(labelX, 1);
        titles.setElementAt(labelY, 2);

        //set column data
        double db[];
        int rr;
        for (rr = table.getRowCount() - 1; rr >= 0; rr--)
            tableModel.removeRow (rr);
        if (names != null)
            for (rr = 0; rr < names.size(); rr++) {
                db = (double[])points.elementAt(rr);
                tableModel.addRow (new Object[]
                            {names.elementAt(rr),
                             new Double (db[0]),
                             new Double (db[1])});
            }
        table.tableChanged(null);
        chart.updateChart (corr, labelX, labelY, showRegression, showAverage);
    }


    protected Vector dataArray(Vector names, Vector points) {
        Vector data = new Vector();
        Vector row;

        if (names == null) {
            String blank = resources.getString("Blank");
            row = new Vector();
            row.addElement(blank); row.addElement(blank); row.addElement(blank);
            data.addElement(row);

        } else {
            double db[];
            for (int rr = 0; rr < names.size(); rr++) {
                row = new Vector();
                db = (double[])points.elementAt(rr);
                row.addElement(names.elementAt(rr));
                row.addElement(new Double (db[0]));
                row.addElement(new Double (db[1]));
                data.addElement(row);
            }
        }
        return data;
    }


    public class ChartCanvas extends Canvas {
        final int TICK_DIM = 20;

        DataCorrelator c;
        String labelX, labelY;
        Vector points;
        double minX, maxX, minY, maxY, pointSpanX, pointSpanY;
        int xTickSpacing, yTickSpacing;
        Dimension minSize  = new Dimension (0, 0);
        Dimension prefSize = new Dimension (200, 200);
        Dimension maxSize  = new Dimension (10000, 10000);
        boolean showRegression, showAverage;

        public ChartCanvas (DataCorrelator c,
                            String labelX,
                            String labelY,
                            boolean showRegression,
                            boolean showAverage) {
            updateChart (c, labelX, labelY, showRegression, showAverage);
        }

        public Dimension getPreferredSize() { return prefSize; }

        public Dimension getMinimumSize() { return minSize; }

        public Dimension getMaximumSize() { return maxSize; }

        public void updateChart (DataCorrelator c,
                                 String labelX,
                                 String labelY,
                                 boolean showRegression,
                                 boolean showAverage) {
            this.c = c;
            this.labelX = labelX;
            this.labelY = labelY;
            this.showRegression = showRegression;
            this.showAverage = showAverage;
            points = (c == null) ? null : c.getDataPoints();

                                      // calc data point spread
            double x, y;
            if ((points != null) && (points.size() > 0)) {
                double[] pt = (double[])points.elementAt(0);
                minX = maxX = pt[0]; minY = maxY = pt[1];
                for (int ii = 1; ii < points.size(); ii++) {
                    pt = (double[])points.elementAt(ii);
                    x = pt[0]; y = pt[1];
                    if (x < minX)    minX = x;
                    if (x > maxX)    maxX = x;
                    if (y < minY)    minY = y;
                    if (y > maxY)    maxY = y;
                }
            } else {
                minX = maxX = minY = maxY = 0;
            }
            minX = calcLowLim (minX, maxX - minX);
            maxX = calcHighLim (maxX, maxX - minX);
            minY = calcLowLim (minY, maxY - minY);
            maxY = calcHighLim (maxY, maxY - minY);
            pointSpanX = 1.0 + maxX - minX;
            pointSpanY = 1.0 + maxY - minY;
            xTickSpacing = calcTicks (minX, maxX);
            yTickSpacing = calcTicks (minY, maxY);

                                      // refresh visible part of screen
            Rectangle r = ChartCanvas.this.getBounds();
            ChartCanvas.this.repaint(0, 0, r.width, r.height);
        }

        int calcTicks (double min, double max) {
            int span = (int)(max-min);
            int iter = 5;
            int lastIter = iter = 1;
            boolean byFive = true;
            while (((span % iter) == 0) && ((span / lastIter) > 20)) {
                lastIter = iter;
                iter *= ((byFive) ? 5 : 2);
                byFive = ! byFive;
            }
            return lastIter;
        }

        double calcLowLim (double x, double span) {
            if (x >= 0.0)
                return 0.0;
            return -calcHighLim (-x, span);
        }

        double calcHighLim (double x, double span) {
            if (x <= 0.0)
                return 0.0;

            int low  = (int)(x + (span / 10.0)); // 1/10 min margin
            int high = (int)(x + (span / 2.0));  // 1/2 max margin
            int test = low;
            int lastTest = test;
            int iter = 5;
            boolean byFive = false;
            while (test < high) {
                lastTest = test;
                test = ((low + iter) / iter) * iter;
                iter *= ((byFive) ? 5 : 2);
                byFive = ! byFive;
            }
            return (double)lastTest;
        }

        private void paintVString (Graphics g, String s, int x, int y, int shift) {
            for (int ii = 0; ii < s.length(); ii++) {
                g.drawString (s.substring(ii, ii + 1), x, y);
                y += shift;
            }
        }


        /**
         * A utility method for drawing text vertically.
         */
        protected void drawVerticalString(String text, Graphics2D g2, float x, float y) {

            AffineTransform saved = g2.getTransform();

            // apply a 90 degree rotation
            AffineTransform rotate = AffineTransform.getRotateInstance(-Math.PI/2, x, y);
            g2.transform(rotate);
            g2.drawString(text, x, y);

            g2.setTransform(saved);

        }

        public int xPoint (double x, int left, double xMult) {
            return left + (int)((x - minX) * xMult);
        }

        public int yPoint (double y, int bottom, double yMult) {
            return bottom - (int)((y - minY) * yMult);
        }

        public void paint(Graphics g) {
            int ii;
            Rectangle cBounds = ChartCanvas.this.getBounds();
            cBounds.x = 0; cBounds.y = 0;
            FontMetrics fm = g.getFontMetrics();
            int cWide = fm.getMaxAdvance() + 2; // 2 == margin
            int cHigh = fm.getMaxAscent() + fm.getMaxDescent() + 2; // 2 == margin

                                      // clear rect & draw outline
            g.setColor (Color.white);
            g.fillRect (cBounds.x, cBounds.y, cBounds.width, cBounds.height);
            g.setColor (Color.black);
            g.drawRect (cBounds.x, cBounds.y, cBounds.width, cBounds.height);
                                      // and adjust size for outline
            cBounds.x++; cBounds.width  -= 2;
            cBounds.y++; cBounds.height -= 2;

                                      // draw legend
            g.setColor (showRegression ? Color.blue : Color.lightGray);
            String l1 = resources.getString("Linear_Regression");
            g.drawString (l1, 5,
                          cBounds.y - 1 + cBounds.height - fm.getMaxDescent());
            g.setColor (showAverage ? Color.red : Color.lightGray);
            g.drawString (resources.getString("Average"), 15 + fm.stringWidth (l1),
                          cBounds.y - 1 + cBounds.height - fm.getMaxDescent());
            cBounds.height -= cHigh + 2;

                                      // draw labels
            g.setColor (Color.black);
            if (labelX != null) {
                g.drawString (labelX, cWide * 2,
                              cBounds.y - 1 + cBounds.height - fm.getMaxDescent());
                cBounds.height -= cHigh + 2;
            }
            if (labelY != null)
                if (g instanceof Graphics2D) {
                    drawVerticalString(labelY, (Graphics2D) g,
                                       cBounds.x + 1 + cHigh, cBounds.height - 20);
                    cBounds.x += cHigh + 2;  cBounds.width -= (cHigh + 2);
                } else {
                    paintVString (g, labelY, cBounds.x + 1, cHigh + 2, cHigh);
                    cBounds.x += cWide + 2;  cBounds.width -= cWide + 2;
                }

                                        // calc tick boundaries
            Rectangle xTicks, yTicks;
            xTicks = new Rectangle (cBounds.x, cBounds.y + cBounds.height - TICK_DIM,
                                    cBounds.width, TICK_DIM);
            yTicks = new Rectangle (cBounds.x, cBounds.y,
                                    TICK_DIM, cBounds.height);
            cBounds.x += TICK_DIM;  cBounds.width -= TICK_DIM;
            cBounds.height -= TICK_DIM;

                                      // calc coordinate conversion constants
            double xMult = ((double)cBounds.width)  / pointSpanX;
            double yMult = ((double)cBounds.height) / pointSpanY;

                                      // draw X ticks & labels
            int textSize;
            String label;
            int x0 = xTicks.x + TICK_DIM, xx = x0;
            for (ii = 0; xx <= xTicks.x + xTicks.width; ii++) {
                xx = x0 + (int)(((double)(xTickSpacing * ii)) * xMult);
                g.drawLine (xx, xTicks.y, xx, xTicks.y + (xTicks.height / 2));
            }
            label = String.valueOf((int)minX);
            g.drawString(label, x0, xTicks.y + xTicks.height - fm.getMaxDescent());
            label = String.valueOf((int)maxX);
            textSize = fm.stringWidth (label);
            g.drawString(label,
                         xTicks.x + xTicks.width - textSize,
                         xTicks.y + xTicks.height - fm.getMaxDescent());

                                      // draw Y ticks & labels
            int y0 = yTicks.y + yTicks.height - TICK_DIM, yy = y0;
            for (ii = 0; yy >= yTicks.y; ii++) {
                yy = y0 - (int)(((double)(yTickSpacing * ii)) * yMult);
                g.drawLine (yTicks.x + (yTicks.width / 2), yy,
                            yTicks.x + yTicks.width, yy);
            }
            label = String.valueOf((int)minY);
            paintVString (g, label,
                          yTicks.x + 2,
                          yTicks.y + yTicks.height - ((cHigh * label.length())),
                          cHigh);
            label = String.valueOf((int)maxY);
            paintVString (g, label,
                          yTicks.x + 2, yTicks.y + fm.getMaxAscent(),
                          cHigh);

                                      // draw axes
            g.setColor (Color.darkGray);
            g.drawLine(cBounds.x, cBounds.y,
                       cBounds.x, cBounds.y + cBounds.height);
            g.drawLine(cBounds.x,                 cBounds.y + cBounds.height,
                       cBounds.x + cBounds.width, cBounds.y + cBounds.height);

                                      // sanity check
            if ((c == null) || (points == null) || (points.size() < 1)) {
                return;
            }

                                      // draw points
            double[] pt;
            g.setColor (Color.black);
            for (ii = 0; ii < points.size(); ii++) {
                pt = (double[])points.elementAt(ii);
                g.fillRect(xPoint (pt[0], cBounds.x, xMult) - 1,
                           yPoint (pt[1], cBounds.y + cBounds.height, yMult) - 1,
                           3, 3);
            }

                                      // draw lines
                                      // Linear Regression
            g.setColor (showRegression ? Color.blue : Color.lightGray);
            drawLineInRect (g, cBounds, c.r.beta0, c.r.beta1,
                            minX, maxX, minY, maxY, xMult, yMult);
                                      // Average
            g.setColor (showAverage ? Color.red : Color.lightGray);
            drawLineInRect (g, cBounds, 0, c.r.y_avg / c.r.x_avg,
                            minX, maxX, minY, maxY, xMult, yMult);
        }

        void drawLineInRect (Graphics g,  Rectangle r,
                             double b0,   double b1,
                             double minX, double maxX,
                             double minY, double maxY,
                             double xMult, double yMult) {
            int index = 0;
            int x[] = new int[2];
            int y[] = new int[2];
            double intercept;
            int bottom = r.y + r.height;

            intercept = b0 + (b1 * minX);
            if (intercept <= maxY && intercept >= minY) {
                x[index]   = xPoint (minX, r.x, xMult);
                y[index++] = yPoint (intercept, bottom, yMult);
            }
            intercept = b0 + (b1 * maxX);
            if (intercept <= maxY && intercept >= minY) {
                x[index]   = xPoint (maxX, r.x, xMult);
                y[index++] = yPoint (intercept, bottom, yMult);
            }
            if (index < 2) {
                intercept = (minY - b0) / b1;
                if (intercept < maxX && intercept > minX) {
                    x[index]   = xPoint (intercept, r.x, xMult);
                    y[index++] = yPoint (minY, bottom, yMult);
                }
            }
            if (index < 2) {
                intercept = (maxY - b0) / b1;
                if (intercept < maxX && intercept > minX) {
                    x[index]   = xPoint (intercept, r.x, xMult);
                    y[index++] = yPoint (maxY, bottom, yMult);
                }
            }
            if (index > 1)
                g.drawLine (x[0], y[0], x[1], y[1]);
        }
    }
}
