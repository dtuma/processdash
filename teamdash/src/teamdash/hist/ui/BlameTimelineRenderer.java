// Copyright (C) 2015-2017 Tuma Solutions, LLC
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

package teamdash.hist.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import net.sourceforge.processdash.util.FastDateFormat;

import teamdash.hist.BlamePoint;

public class BlameTimelineRenderer extends JComponent implements
        TableCellRenderer {

    private boolean isHeader;

    private boolean isSelected;

    private boolean isFirst;

    private boolean isLast;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        this.isHeader = (value == null);
        this.isSelected = isSelected;
        this.isFirst = (row == 0);
        int nextRow = row + 1;
        if (nextRow < table.getRowCount())
            this.isLast = (table.getValueAt(nextRow, column) == null);
        else
            this.isLast = true;
        setToolTipText(format((BlamePoint) value));
        return this;
    }

    @Override
    public void paint(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        if (!isHeader) {
            int midX = getWidth() / 2;
            int midY = getHeight() / 2;
            int dotRadius = getHeight() / 5;
            int dotDiam = dotRadius * 2 + 1;

            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke(new BasicStroke(1));
            g.setColor(Color.LIGHT_GRAY);
            if (!isFirst)
                g.drawLine(midX, 0, midX, midY);
            if (!isLast)
                g.drawLine(midX, midY, midX, getHeight());

            if (isSelected)
                g.setColor(Color.GRAY);

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            if (g2.getTransform().getScaleX() > 1.5)
                dotDiam--;
            g.fillOval(midX - dotRadius, midY - dotRadius, dotDiam, dotDiam);
        }
    }

    private static String format(BlamePoint blamePoint) {
        if (blamePoint == null)
            return null;
        else if (blamePoint == BlamePoint.INITIAL)
            return INITIAL_VALUE;
        else
            return blamePoint.getAuthor() + ", "
                    + DATE_FMT.format(blamePoint.getTimestamp());
    }

    private static final String INITIAL_VALUE = BlameHistoryDialog.resources
            .getString("Initial_Value");

    static final FastDateFormat DATE_FMT = FastDateFormat.getDateTimeInstance(
        FastDateFormat.SHORT, FastDateFormat.SHORT);

}
