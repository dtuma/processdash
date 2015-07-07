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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

public class BlameAnnotationBorder extends EmptyBorder {

    public static final String ANNOTATION_COLOR_KEY = BlameAnnotationBorder.class
            .getName() + ".annotationColor";

    private static final Border INSTANCE = new BlameAnnotationBorder();

    private static final Color DEFAULT_ANNOTATION_COLOR = new Color(0, 0, 128);

    private BlameAnnotationBorder() {
        super(0, 0, 0, 0);
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width,
            int height) {
        Color color = null;
        if (c instanceof JComponent)
            color = (Color) ((JComponent) c)
                    .getClientProperty(ANNOTATION_COLOR_KEY);

        g.setColor(color != null ? color : DEFAULT_ANNOTATION_COLOR);
        for (int i = height / 3; i-- > 0;) {
            g.drawLine(x, y + 5 - i, x + i, y + 5 - i);
        }
    }


    public static void annotate(Component c) {
        if (c instanceof JComponent)
            annotate((JComponent) c);
    }

    public static void annotate(JComponent comp) {
        Border currentBorder = comp.getBorder();
        Border annotated = annotate(currentBorder);
        comp.setBorder(annotated);
    }

    public static Border annotate(Border b) {
        if (b == null)
            return INSTANCE;
        Border result = ANNOTATED_BORDERS.get(b);
        if (result == null) {
            result = BorderFactory.createCompoundBorder(INSTANCE, b);
            ANNOTATED_BORDERS.put(b, result);
        }
        return result;
    }

    private static final Map<Border, Border> ANNOTATED_BORDERS = Collections
            .synchronizedMap(new HashMap());

}
