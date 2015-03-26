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

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;

import org.jfree.chart.ChartColor;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.DrawingSupplier;

public class DrawingSupplierFactory {

    /** A modified color palette that does not include reddish colors. */
    public static final Color[] PALETTE1 = {
        new Color(0x99, 0x33, 0xff), // lavender
        new Color(0xff, 0xcc, 0x66), // pale orange
        new Color(0x66, 0xff, 0x99), // pale green
        new Color(0x66, 0x66, 0xff), // pale blue
        new Color(0x00, 0xcc, 0xff), // pale cyan
        new Color(0xff, 0xff, 0x66), // pale yellow
        new Color(0x66, 0x00, 0x66), // dark purple
        new Color(0x00, 0x00, 0x66), // navy
        new Color(0x99, 0x66, 0x33), // light brown
        new Color(0x00, 0xff, 0x00), // green
        new Color(0x00, 0x00, 0xff), // blue
        new Color(0xcc, 0xcc, 0x00), // mustard
        new Color(0x99, 0x33, 0x66), // purple
        new Color(0x00, 0x33, 0x66), // slate blue
        new Color(0x00, 0x66, 0x66), // green-blue
        new Color(0x33, 0x99, 0x33), // medium green
        new Color(0x00, 0x33, 0x00), // dark green
        new Color(0x66, 0x33, 0x00), // brown
        new Color(0x55, 0x55, 0xFF),
        new Color(0x55, 0xFF, 0x55),
        new Color(0xFF, 0xFF, 0x55),
        new Color(0x55, 0xFF, 0xFF),
        Color.gray,
        ChartColor.DARK_BLUE,
        ChartColor.DARK_GREEN,
        ChartColor.DARK_YELLOW,
        ChartColor.DARK_MAGENTA,
        ChartColor.DARK_CYAN,
        Color.darkGray,
        ChartColor.LIGHT_BLUE,
        ChartColor.LIGHT_GREEN,
        ChartColor.LIGHT_YELLOW,
        ChartColor.LIGHT_CYAN,
        Color.lightGray,
        ChartColor.VERY_DARK_BLUE,
        ChartColor.VERY_DARK_GREEN,
        ChartColor.VERY_DARK_YELLOW,
        ChartColor.VERY_DARK_MAGENTA,
        ChartColor.VERY_DARK_CYAN,
        ChartColor.VERY_LIGHT_BLUE,
        ChartColor.VERY_LIGHT_GREEN,
        ChartColor.VERY_LIGHT_YELLOW,
        ChartColor.VERY_LIGHT_CYAN
    };


    /** The paint sequence. */
    private Paint[] paintSequence;

    /** The outline paint sequence. */
    private Paint[] outlinePaintSequence;

    /** The fill paint sequence. */
    private transient Paint[] fillPaintSequence;

    /** The stroke sequence. */
    private transient Stroke[] strokeSequence;

    /** The outline stroke sequence. */
    private transient Stroke[] outlineStrokeSequence;

    /** The shape sequence. */
    private transient Shape[] shapeSequence;

    public DrawingSupplierFactory() {
        this.paintSequence = DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE;
        this.fillPaintSequence = DefaultDrawingSupplier.DEFAULT_FILL_PAINT_SEQUENCE;
        this.outlinePaintSequence = DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE;
        this.strokeSequence = DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE;
        this.outlineStrokeSequence = DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE;
        this.shapeSequence = DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE;
    }

    /**
     * @return Returns the paintSequence.
     */
    public Paint[] getPaintSequence() {
        return paintSequence;
    }

    /**
     * @param paintSequence
     *                The paintSequence to set.
     */
    public DrawingSupplierFactory setPaintSequence(Paint[] paintSequence) {
        this.paintSequence = paintSequence;
        return this;
    }

    /**
     * @return Returns the outlinePaintSequence.
     */
    public Paint[] getOutlinePaintSequence() {
        return outlinePaintSequence;
    }

    /**
     * @param outlinePaintSequence
     *                The outlinePaintSequence to set.
     */
    public DrawingSupplierFactory setOutlinePaintSequence(
            Paint[] outlinePaintSequence) {
        this.outlinePaintSequence = outlinePaintSequence;
        return this;
    }

    /**
     * @return Returns the fillPaintSequence.
     */
    public Paint[] getFillPaintSequence() {
        return fillPaintSequence;
    }

    /**
     * @param fillPaintSequence
     *                The fillPaintSequence to set.
     */
    public DrawingSupplierFactory setFillPaintSequence(Paint[] fillPaintSequence) {
        this.fillPaintSequence = fillPaintSequence;
        return this;
    }

    /**
     * @return Returns the strokeSequence.
     */
    public Stroke[] getStrokeSequence() {
        return strokeSequence;
    }

    /**
     * @param strokeSequence
     *                The strokeSequence to set.
     */
    public DrawingSupplierFactory setStrokeSequence(Stroke[] strokeSequence) {
        this.strokeSequence = strokeSequence;
        return this;
    }

    /**
     * @return Returns the outlineStrokeSequence.
     */
    public Stroke[] getOutlineStrokeSequence() {
        return outlineStrokeSequence;
    }

    /**
     * @param outlineStrokeSequence
     *                The outlineStrokeSequence to set.
     */
    public DrawingSupplierFactory setOutlineStrokeSequence(
            Stroke[] outlineStrokeSequence) {
        this.outlineStrokeSequence = outlineStrokeSequence;
        return this;
    }

    /**
     * @return Returns the shapeSequence.
     */
    public Shape[] getShapeSequence() {
        return shapeSequence;
    }

    /**
     * @param shapeSequence
     *                The shapeSequence to set.
     */
    public DrawingSupplierFactory setShapeSequence(Shape[] shapeSequence) {
        this.shapeSequence = shapeSequence;
        return this;
    }

    /**
     * Create a new drawing supplier using the current factory settings.
     * 
     * @return
     */
    public DrawingSupplier newDrawingSupplier() {
        return new DefaultDrawingSupplier(paintSequence, fillPaintSequence,
                outlinePaintSequence, strokeSequence, outlineStrokeSequence,
                shapeSequence);
    }

}
