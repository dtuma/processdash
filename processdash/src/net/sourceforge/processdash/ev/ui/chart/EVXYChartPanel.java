// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev.ui.chart;

import javax.swing.ToolTipManager;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.ToolTipTimingCustomizer;
import net.sourceforge.processdash.util.Disposable;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.title.LegendTitle;

public class EVXYChartPanel extends ChartPanel implements Disposable {
    protected static Resources resources = Resources.getDashBundle("EV.Chart");

    // Used to indicate if the window has FULL width, MED width or short width.
    protected static final int FULL = 2;
    protected static final int MED = 1;
    protected static final int SHORT = 0;

    // Those are properties that are fetched in the dashboard resources bundle
    //  and indicate at what width a window should be considered "medium" or
    //  short
    private int medium_window_width;
    private int short_window_width;

    private LegendTitle legend;
    private int currentStyle;
    private String xLabel;
    private String yLabel;

    public EVXYChartPanel(JFreeChart chart) {
        super(chart);
        setMouseZoomable(true, false);

        this.legend = getChart().getLegend();
        this.xLabel = getChart().getXYPlot().getDomainAxis().getLabel();
        this.yLabel = getChart().getXYPlot().getRangeAxis().getLabel();

        try {
            medium_window_width =
                Integer.parseInt(resources.getString("Window_Width_Med_Name"));
        } catch (NumberFormatException nfe) {}
        try {
            short_window_width =
                Integer.parseInt(resources.getString("Window_Width_Short_Name"));
        } catch (NumberFormatException nfe) {}

        ToolTipManager.sharedInstance().registerComponent(this);
        new ToolTipTimingCustomizer().install(this);
    }

    public void dispose() {
        getChart().getXYPlot().setDataset(null);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        adjustStyle(width);
        super.setBounds(x, y, width, height);
    }

    private void adjustStyle(int width) {
        int style;
        if (width > medium_window_width)      style = FULL;
        else if (width > short_window_width)  style = MED;
        else                                  style = SHORT;

        if (style == currentStyle) return;
        currentStyle = style;

        JFreeChart chart = getChart();
        chart.removeLegend();
        if (style == FULL) chart.addLegend(legend);

        adjustAxis(chart.getXYPlot().getRangeAxis(),
                   style != FULL,
                   yLabel);

        adjustAxis(chart.getXYPlot().getDomainAxis(),
                   style == SHORT,
                   xLabel);
    }

    protected void adjustAxis(Axis a, boolean chromeless, String label) {
        // FIXME: to correctly respond to series changes, we should register
        // a DatasetChangeListener
        boolean chartContainsData = getChart().getXYPlot().getSeriesCount() > 0;
        boolean showAxisTickLabels = !chromeless && chartContainsData;

        a.setTickLabelsVisible(showAxisTickLabels);
        a.setTickMarksVisible(showAxisTickLabels);
        a.setLabel(chromeless ? null : label);
    }

}
