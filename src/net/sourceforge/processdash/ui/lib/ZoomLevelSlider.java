// Copyright (C) 2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.text.NumberFormat;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Timer;
import javax.swing.event.ChangeListener;

public class ZoomLevelSlider extends JPanel {

    private ZoomManager zoom;

    private JLabel pctLabel;

    private JSlider slider;

    private ChangeListener zoomListener;

    private Timer snapTimer;

    private volatile boolean currentlyPropagatingChange;


    public ZoomLevelSlider(ZoomManager zoom) {
        this.zoom = zoom;

        pctLabel = new JLabel(PCT.format(zoom.getMaxZoom()));
        Dimension d = pctLabel.getPreferredSize();
        pctLabel.setMinimumSize(d);
        pctLabel.setPreferredSize(d);
        pctLabel.setMaximumSize(d);

        slider = new JSlider(pctToVal(0.5), pctToVal(zoom.getMaxZoom()));
        slider.setMajorTickSpacing(pctToVal(0.5));
        slider.setMinorTickSpacing(pctToVal(0.1));
        slider.setPaintTicks(true);
        slider.setFocusable(false);
        d = slider.getPreferredSize();
        d.width = 400;
        slider.setPreferredSize(d);
        slider.addChangeListener(
            EventHandler.create(ChangeListener.class, this, "sliderChanged"));

        zoomListener = EventHandler.create(ChangeListener.class, this,
            "zoomLevelChanged");

        snapTimer = new Timer(50,
                EventHandler.create(ActionListener.class, this, "maybeSnap"));

        setLayout(new BorderLayout(5, 0));
        add(pctLabel, BorderLayout.WEST);
        add(slider, BorderLayout.CENTER);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        zoom.addChangeListener(zoomListener);
        zoomLevelChanged();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        zoom.removeChangeListener(zoomListener);
    }

    public void zoomLevelChanged() {
        if (currentlyPropagatingChange == false) {
            double zoomLevel = zoom.getZoomLevel();
            pctLabel.setText(PCT.format(zoomLevel));
            try {
                currentlyPropagatingChange = true;
                slider.setValue(pctToVal(zoomLevel));
            } finally {
                currentlyPropagatingChange = false;
            }
        }
    }

    public void sliderChanged() {
        if (currentlyPropagatingChange || !isShowing())
            return;

        int value = slider.getValue();
        if (value != 100 && value > 90 && value < 110) {
            value = 100;
            snapTimer.restart();
        } else {
            snapTimer.stop();
        }

        int current = pctToVal(zoom.getZoomLevel());
        if (value != current) {
            double pct = value / 100.0;
            pctLabel.setText(PCT.format(pct));
            try {
                currentlyPropagatingChange = true;
                zoom.setZoomLevel(pct);
            } finally {
                currentlyPropagatingChange = false;
            }
        }
    }

    public void maybeSnap() {
        if (slider.getModel().getValueIsAdjusting() == false) {
            slider.setValue(pctToVal(1.0));
            snapTimer.stop();
        }
    }

    private int pctToVal(double pct) {
        return (int) (pct * 100);
    }

    private NumberFormat PCT = NumberFormat.getPercentInstance();

}
