// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.repository.RemoteException;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.ui.lib.LevelIndicator;
import net.sourceforge.processdash.util.FormatUtil;

public class PercentSpentIndicator extends JPanel implements DataListener,
        PropertyChangeListener {

    private DashboardContext dashCtx;

    private ActiveTaskModel activeTaskModel;

    private CardLayout layout;

    private LevelIndicator levelIndicator;

    private String currentTaskPath = null;

    private String estTimeDataName = null;
    private double estTime = Double.NaN;
    private boolean estTimeEditable = false;

    private String actTimeDataName = null;
    private double actTime = Double.NaN;


    private static final Resources resources = Resources
            .getDashBundle("ProcessDashboard.PctSpent");

    private static final Color GREEN_BAR = new Color(0, 200, 0);
    private static final Color GREEN_HIGHLIGHT = new Color(128, 255, 128);
    private static final Color RED_BAR = new Color(200, 0, 0);
    private static final Color RED_HIGHLIGHT = new Color(255, 60, 60);

    private static final String LEVEL_INDICATOR_KEY = "level";
    private static final String MISSING_ESTIMATE_KEY = "noEst";


    public PercentSpentIndicator(DashboardContext dashCtx,
            ActiveTaskModel activeTaskModel) {
        this.dashCtx = dashCtx;
        this.activeTaskModel = activeTaskModel;

        buildUI();

        activeTaskModel.addPropertyChangeListener(this);
        setTaskPath(activeTaskModel.getPath());
    }


    private void buildUI() {
        layout = new CardLayout();
        setLayout(layout);

        levelIndicator = new LevelIndicator(true);
        levelIndicator.setPaintBarRect(false);
        add(levelIndicator, LEVEL_INDICATOR_KEY);

        JLabel icon = new JLabel(new ImageIcon(getClass().getResource(
                "hourglass.png")));
        add(icon, MISSING_ESTIMATE_KEY);

        if (!Settings.isReadOnly()) {
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    showEditEstimateDialog();
                }
            });
        }

        setBorder(BorderFactory.createLineBorder(UIManager
                .getColor("controlDkShadow")));
        setPreferredSize(new Dimension(11, 21));
    }


    public void dataValuesChanged(Vector v) throws RemoteException {
        for (int i = v.size(); i-- > 0;)
            dataValueChanged((DataEvent) v.get(i));
    }

    public void dataValueChanged(DataEvent e) throws RemoteException {
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
        String toolTipResKey = null;
        if (Double.isNaN(estTime) || estTime == 0) {
            layout.show(this, MISSING_ESTIMATE_KEY);
            toolTipResKey = "No_Estimate_HTML_FMT";
        } else {
            if (pctSpent <= 1.0) {
                levelIndicator.setLevel(1 - pctSpent);
                levelIndicator.setBarColors(GREEN_BAR, GREEN_HIGHLIGHT);
            } else {
                levelIndicator.setLevel(pctSpent - 1);
                levelIndicator.setBarColors(RED_BAR, RED_HIGHLIGHT);
            }
            layout.show(this, LEVEL_INDICATOR_KEY);
            toolTipResKey = "Message_HTML_FMT";
        }
        String est = FormatUtil.formatTime(estTime, true);
        String act = FormatUtil.formatTime(actTime, true);
        String pct = FormatUtil.formatPercent(pctSpent);
        String clickToEdit = (estTimeEditable ? resources
                .getString("Click_To_Edit_HTML") : "");
        String toolTip = "<html><body>"
                + resources.format(toolTipResKey, est, act, pct, clickToEdit)
                + "</body></html>";
        setToolTipText(toolTip);
    }


    public void propertyChange(PropertyChangeEvent evt) {
        setTaskPath(activeTaskModel.getPath());
    }


    private void setTaskPath(String newTaskPath) {
        if (newTaskPath != null && newTaskPath.equals(currentTaskPath))
            return;

        String[] elemsToRemove = null;
        String[] elemsToAdd = null;

        // Use a synchronized block to make atomic changes to our internal state
        synchronized (this) {
            if (currentTaskPath != null)
                elemsToRemove = new String[] { estTimeDataName, actTimeDataName };

            estTimeDataName = actTimeDataName = null;
            actTime = 0;
            estTime = Double.NaN;
            estTimeEditable = false;

            currentTaskPath = newTaskPath;

            if (currentTaskPath != null) {
                estTimeDataName = getDataName("Estimated Time");
                actTimeDataName = getDataName("Time");
                elemsToAdd = new String[] { estTimeDataName, actTimeDataName };
            }
        }

        // Outside of the synchronized block, interact with the data repository
        // to update our data listener registrations.
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
    }


    private String getDataName(String elemName) {
        return DataRepository.createDataName(currentTaskPath, elemName);
    }



    private void showEditEstimateDialog() {
        if (currentTaskPath == null || estTimeDataName == null
                || estTimeEditable == false)
            return;

        final JTextField estimate = new JTextField();
        if (estTime > 0 && !Double.isInfinite(estTime))
            estimate.setText(FormatUtil.formatTime(estTime));

        String prompt = resources.format("Edit_Dialog.Prompt_FMT",
                currentTaskPath);

        JPanel p = new JPanel(new GridLayout(2, 2));
        p.add(new JLabel(resources.getString("Edit_Dialog.Actual_Time"),
                JLabel.TRAILING));
        p.add(new JLabel(FormatUtil.formatTime(actTime)));
        p.add(new JLabel(resources.getString("Edit_Dialog.Estimated_Time"),
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
            return;
        }
    }


}
