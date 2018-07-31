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

package teamdash.wbs;

import java.awt.Font;

import javax.swing.JTable;

import net.sourceforge.processdash.ui.lib.ZoomManager;

public class TableFontHandler extends ZoomManager.FontType {

    private static final String BOLD = TableFontHandler.class.getName()
            + ".BOLD";

    private static final String ITALIC = TableFontHandler.class.getName()
            + ".ITALIC";

    @Override
    public boolean matches(Object target, String propertyName, Object value) {
        return (target instanceof JTable && value instanceof Font);
    }

    @Override
    public Font zoom(Object target, String propertyName, Font baseValue,
            double zoom) {
        // discard the cached bold/italic fonts
        JTable table = (JTable) target;
        table.putClientProperty(BOLD, null);
        table.putClientProperty(ITALIC, null);

        // scale the font based on the zoom level
        return super.zoom(target, propertyName, baseValue, zoom);
    }

    public static Font getBold(JTable t) {
        return getCachedFont(t, BOLD, Font.BOLD);
    }

    public static Font getItalic(JTable t) {
        return getCachedFont(t, ITALIC, Font.ITALIC);
    }

    private static Font getCachedFont(JTable t, String prop, int style) {
        Font result = (Font) t.getClientProperty(prop);
        if (result == null) {
            result = t.getFont().deriveFont(style);
            t.putClientProperty(prop, result);
        }
        return result;
    }

}
