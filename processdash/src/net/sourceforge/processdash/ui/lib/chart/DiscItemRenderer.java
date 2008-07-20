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

package net.sourceforge.processdash.ui.lib.chart;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.data.general.PieDataset;

public interface DiscItemRenderer {

    /**
     * Initialises the renderer. This method will be called before the first
     * item is rendered, giving the renderer an opportunity to initialise any
     * state information it wants to maintain. The renderer can do nothing if it
     * chooses.
     * 
     * @param g2
     *                the graphics device.
     * @param dataArea
     *                the area inside the axes.
     * @param plot
     *                the plot.
     * @param dataset
     *                the dataset.
     * @param info
     *                an optional info collection object to return data back to
     *                the caller.
     * 
     * @return The number of passes the renderer requires.
     */
    public DiscItemRendererState initialise(Graphics2D g2,
            Rectangle2D dataArea, DiscPlot plot, PieDataset dataset,
            PlotRenderingInfo info);

    /**
     * Called for each item to be plotted.
     * 
     * @param g2
     *                the graphics device.
     * @param state
     *                the renderer state.
     * @param dataArea
     *                the area within which the data is being rendered.
     * @param discLocation
     *                the area within which this data point should be drawn.
     * @param info
     *                collects drawing info.
     * @param plot
     *                the plot (can be used to obtain standard color information
     *                etc).
     * @param dataset
     *                the dataset.
     * @param item
     *                the item index (zero-based).
     */
    public void drawItem(Graphics2D g2, DiscItemRendererState state,
            Rectangle2D dataArea, Ellipse2D discLocation,
            PlotRenderingInfo info, DiscPlot plot, PieDataset dataset, int item);

}
