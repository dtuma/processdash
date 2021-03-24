// Copyright (C) 2002-2021 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

/**
 * 
 */
package teamdash.wbs.excel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined;

public class StyleKey {

    private static final short BLACK = HSSFColorPredefined.BLACK.getIndex();

    private static final short WHITE = HSSFColorPredefined.WHITE.getIndex();

    private static final short RED = HSSFColorPredefined.RED.getIndex();

    private static final short BLUE = HSSFColorPredefined.BLUE.getIndex();


    short color = BLACK;

    boolean bold = false;

    boolean italic = false;

    short indent = 0;

    short format = 0;

    public void configure(HSSFFont font) {
        if (color != BLACK)
            font.setColor(color);
        if (bold)
            font.setBold(true);
        if (italic)
            font.setItalic(true);
    }

    public void loadFrom(Component comp) {
        setColor(comp.getForeground());
        setFont(comp.getFont());
    }

    public void setColor(Color c) {
        if (c == Color.RED)
            color = RED;
        else if (c == Color.BLUE)
            color = BLUE;
        else if (c == Color.WHITE)
            color = WHITE;
    }

    public void setFont(Font f) {
        bold = (f != null && f.isBold());
        italic = (f != null && f.isItalic());
    }

    public void configure(HSSFCellStyle style) {
        if (indent > 0)
            style.setIndention(indent);
        if (format > 0)
            style.setDataFormat(format);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StyleKey) {
            StyleKey that = (StyleKey) obj;
            return this.color == that.color && this.bold == that.bold
                    && this.italic == that.italic
                    && this.indent == that.indent
                    && this.format == that.format;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = color;
        result = result << 1 + (bold ? 1 : 0);
        result = result << 1 + (italic ? 1 : 0);
        result = result << 4 + indent;
        result = result << 7 + format;
        return result;
    }

}
