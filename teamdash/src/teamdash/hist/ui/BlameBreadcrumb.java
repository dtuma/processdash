// Copyright (C) 2015 Tuma Solutions, LLC
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

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import net.sourceforge.processdash.util.StringUtils;

import teamdash.wbs.IconFactory;

public class BlameBreadcrumb extends JLabel {

    private boolean isPainting = false;

    private String altTextForPainting = null;

    private int lastWidth = -1;


    public BlameBreadcrumb() {
        setAlignmentX(0f);
        setHorizontalAlignment(LEFT);
        setFont(getFont().deriveFont(Font.PLAIN));
        clear();
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

    @Override
    public String getText() {
        if (isPainting && altTextForPainting != null)
            return altTextForPainting;
        else
            return super.getText();
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (width != lastWidth) {
            recalcTextTruncation();
            lastWidth = width;
        }
    }

    public void clear() {
        setPath(null, null);
    }

    public void setPath(String fullPath, String columnName) {
        if (fullPath == null) {
            altTextForPainting = null;
            setIcon(null);
            setText(" ");

        } else {
            fullPath = StringUtils.findAndReplace(fullPath.substring(1), "/",
                PATH_SEP);
            setIcon(IconFactory.getProjectIcon());
            setText(fullPath + (columnName == null ? "" : BULLET + columnName));
            recalcTextTruncation();
        }
    }

    private void recalcTextTruncation() {
        String text = super.getText();
        if (text == null || text.length() < 4
                || getPreferredSize().width <= getWidth()) {
            altTextForPainting = null;
            setToolTipText(null);
            return;
        }

        Rectangle viewRect = new Rectangle();
        getBounds(viewRect);
        Insets insets = getInsets();
        viewRect.width -= insets.left + insets.right + 2 * getIconTextGap();

        List parts = Arrays.asList(text.split(PATH_SEP));
        Collections.reverse(parts);
        String reversedString = StringUtils.join(parts, PATH_SEP);

        String fitText = SwingUtilities.layoutCompoundLabel(
            getFontMetrics(getFont()), reversedString, getIcon(),
            getVerticalAlignment(), getHorizontalAlignment(),
            getVerticalTextPosition(), getHorizontalTextPosition(), viewRect,
            new Rectangle(), new Rectangle(), 0);
        if (fitText.equals(reversedString)) {
            altTextForPainting = null;
            setToolTipText(null);
            return;
        }

        int len = fitText.length();
        if (fitText.endsWith(" /..."))
            fitText = fitText.substring(0, len - 4) + "...";
        else if (fitText.endsWith(" / ..."))
            fitText = fitText.substring(0, len - 5) + "...";

        int pos = fitText.lastIndexOf(PATH_SEP);
        if (pos == -1) {
            setToolTipText(text);
            altTextForPainting = null;
            return;
        }

        String truncPart = fitText.substring(pos + 3);
        String finalPart = text.substring(text.length() - pos);
        setToolTipText(text);
        altTextForPainting = truncPart + " / " + finalPart;
    }

    private static final String PATH_SEP = " / ";

    private static final String BULLET = " \u2022 ";

}
