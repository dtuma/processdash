// Copyright (C) 2002-2010 Tuma Solutions, LLC
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

package teamdash.wbs.excel;

import java.util.HashMap;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class StyleCache {

    HSSFWorkbook xls;

    private Map<StyleKey, HSSFCellStyle> styleCache;

    public StyleCache(HSSFWorkbook xls) {
        this.xls = xls;
        this.styleCache = new HashMap<StyleKey, HSSFCellStyle>();
    }


    public void applyStyle(HSSFCell cell, StyleKey styleKey) {
        HSSFCellStyle style = getStyle(styleKey);
        if (style != null)
            cell.setCellStyle(style);
    }

    public HSSFCellStyle getStyle(StyleKey key) {
        if (DEFAULT_STYLE.equals(key))
            return null;

        HSSFCellStyle style = styleCache.get(key);
        if (style == null) {
            style = xls.createCellStyle();
            key.configure(style);
            HSSFFont font = xls.createFont();
            key.configure(font);
            style.setFont(font);
            styleCache.put(key, style);
        }

        return style;
    }

    private static final StyleKey DEFAULT_STYLE = new StyleKey();

}
