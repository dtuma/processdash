// Copyright (C) 2014-2015 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.reports;

import java.awt.Color;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;

public class DashboardChartDefaults {

    public static void initialize() {
        // install the legacy theme for chart colors
        ChartFactory.setChartTheme(StandardChartTheme.createLegacyTheme());
        // turn off shadows on bar charts by default
        BarRenderer.setDefaultShadowsVisible(false);
        XYBarRenderer.setDefaultShadowsVisible(false);
        // the standard set of colors includes yellow, which is nearly
        // impossible to see on a white background. Replace those yellows with
        // variations on orange.
        DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE[3] = Color.orange;
        DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE[18] = new Color(215, 170, 0);
        DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE[31] = new Color(255, 200, 128);
    }

}
