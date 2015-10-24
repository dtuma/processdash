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

package net.sourceforge.processdash.ui.lib;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.JTextField;

public class JHintTextField extends JTextField {

    private String hint;

    private Font italic;

    public JHintTextField(String hint) {
        this(hint, 0);
    }

    public JHintTextField(String hint, int columns) {
        super(columns);
        setHint(hint);
        italic = getFont().deriveFont(Font.ITALIC);
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = ("".equals(hint) ? null : hint);
        setToolTipText(this.hint);
    }

    @Override
    public String getToolTipText() {
        if (isHintVisible())
            return null;
        else
            return super.getToolTipText();
    }

    @Override
    public void setFont(Font f) {
        super.setFont(f);
        italic = getFont().deriveFont(Font.ITALIC);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        if (isHintVisible()) {
            ((Graphics2D) g).setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color fg = getForeground();
            Color bg = getBackground();
            g.setColor(PaintUtils.mixColors(fg, bg, 0.5));
            g.setFont(italic);

            int h = getHeight();
            Insets ins = getInsets();
            FontMetrics fm = g.getFontMetrics();
            g.drawString(hint, ins.left, h / 2 + fm.getAscent() / 2 - 2);
        }
    }

    private boolean isHintVisible() {
        return getText().trim().length() == 0 && hint != null;
    }

}