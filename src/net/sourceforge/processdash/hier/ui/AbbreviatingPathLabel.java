// Copyright (C) 2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.hier.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;

import net.sourceforge.processdash.ui.TaskNavigationSelector;

public class AbbreviatingPathLabel extends JLabel {

    public static final int MIN_WIDTH = 30;

    private Rectangle viewRect, textRect, iconRect;

    private int truncatedPathWidth;

    private int requestedWidth;

    private int lastWidth = -1;

    private String altTextForPainting = null;

    private boolean isPainting = false;

    public AbbreviatingPathLabel() {
        viewRect = new Rectangle();
        textRect = new Rectangle();
        iconRect = new Rectangle();
    }

    public AbbreviatingPathLabel(String path) {
        this();
        setPath(path);
    }

    public AbbreviatingPathLabel(String path, Icon icon) {
        this();
        setPath(path);
        setIcon(icon);
    }

    public void setPath(String path) {
        if (path.startsWith("/"))
            path = path.substring(1);
        setText(TaskNavigationSelector.prettifyPath(path));
    }

    public int getRequestedWidth() {
        return requestedWidth;
    }

    public void setRequestedWidth(int requestedWidth) {
        this.requestedWidth = requestedWidth;
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        recalcTextTruncation();
    }

    @Override
    public void setIcon(Icon icon) {
        super.setIcon(icon);
        recalcTextTruncation();
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        recalcTextTruncation();
    }

    @Override
    public String getText() {
        if (isPainting && altTextForPainting != null)
            return altTextForPainting;
        else
            return super.getText();
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        if (event.getX() < truncatedPathWidth)
            return super.getToolTipText(event);
        else
            return null;
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension result = super.getPreferredSize();
        result.width = MIN_WIDTH;
        return result;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension result = super.getPreferredSize();
        if (requestedWidth > MIN_WIDTH)
            result.width = requestedWidth;
        return result;
    }

    @Override
    public Point getToolTipLocation(MouseEvent event) {
        return TOOLTIP_LOCATION;
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (width != lastWidth) {
            recalcTextTruncation();
            lastWidth = width;
        }
    }

    @Override
    public void paint(Graphics g) {
        try {
            isPainting = true;
            super.paint(g);
        } finally {
            isPainting = false;
        }
    }

    protected void recalcTextTruncation() {
        String fullText = super.getText();
        if (fullText == null || fullText.length() < 4
                || super.getPreferredSize().width <= getWidth()) {
            altTextForPainting = null;
            setToolTipText(null);
            return;
        }

        getBounds(viewRect);
        Insets insets = getInsets();
        viewRect.width -= insets.left + insets.right + 2 * getIconTextGap();

        String layoutText = reversePathSegments(fullText);
        String fitLayoutText = SwingUtilities.layoutCompoundLabel(
            getFontMetrics(getFont()), layoutText, getIcon(),
            getVerticalAlignment(), getHorizontalAlignment(),
            getVerticalTextPosition(), getHorizontalTextPosition(), //
            viewRect, iconRect, textRect, getIconTextGap());

        if (fitLayoutText.equals(layoutText)) {
            altTextForPainting = null;
            setToolTipText(null);
        } else {
            String[] parts = getFitPathSplit(fullText, fitLayoutText);
            setToolTipText(parts[0]);
            JLabel l = new JLabel(parts[1]);
            l.setIcon(getIcon());
            l.setFont(getFont());
            truncatedPathWidth = l.getPreferredSize().width + 3;
            altTextForPainting = parts[2];
        }
    }

    private static String reversePathSegments(String fullPath) {
        String[] parts = fullPath.split(" / ");
        StringBuilder result = new StringBuilder();
        for (int i = parts.length; i-- > 0;)
            result.append(" / ").append(parts[i]);
        return result.substring(3);
    }

    private static String[] getFitPathSplit(String fullPath, String fitText) {
        int len = fitText.length();
        if (fitText.endsWith(" /..."))
            fitText = fitText.substring(0, len - 4) + "...";
        else if (fitText.endsWith(" / ..."))
            fitText = fitText.substring(0, len - 5) + "...";

        int pos = fitText.lastIndexOf(" / ");
        if (pos == -1)
            return new String[] { fullPath, fitText, fitText };

        String truncPart = fitText.substring(pos + 3);
        String finalPart = fullPath.substring(fullPath.length() - pos);
        String initialPart = fullPath.substring(0, fullPath.length() - pos - 3);
        return new String[] { initialPart, truncPart, //
                truncPart + " / " + finalPart };
    }

    private static final Point TOOLTIP_LOCATION = getTooltipOffset();

    private static Point getTooltipOffset() {
        JToolTip tip = new JToolTip();
        tip.setTipText("X");
        int delta = tip.getPreferredSize().height + 2;
        return new Point(0, -delta);
    }

}
