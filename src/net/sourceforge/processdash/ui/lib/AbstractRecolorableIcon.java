// Copyright (C) 2017-2018 Tuma Solutions, LLC
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

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_STROKE_CONTROL;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.image.RGBImageFilter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;


public abstract class AbstractRecolorableIcon
        implements RecolorableIcon, ZoomLevelCapable, Cloneable {

    protected int width, height;

    protected boolean antialias, strokePure;

    protected ZoomLevel zoom;

    protected AbstractRecolorableIcon() {
        width = height = 16;
        antialias = true;
        strokePure = false;
    }

    public void setZoom(ZoomLevel zoom) {
        this.zoom = zoom;
    }

    @Override
    public int getIconWidth() {
        if (zoom == null)
            return width;
        else
            return (int) (width * zoom.getZoomLevel() + 0.5);
    }

    @Override
    public int getIconHeight() {
        if (zoom == null)
            return height;
        else
            return (int) (height * zoom.getZoomLevel() + 0.5);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();

        if (antialias)
            g2.setRenderingHint(KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        if (strokePure)
            g2.setRenderingHint(KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
        g2.translate(x, y);
        if (zoom != null && zoom.getZoomLevel() != 1.0)
            g2.scale(zoom.getZoomLevel(), zoom.getZoomLevel());

        paintIcon(g2, g2.getClip(), (float) g2.getTransform().getScaleX());
    }

    protected void paintIcon(Graphics2D g2, Shape clip, float scale) {}

    protected Shape shape(float... coords) {
        Path2D result = new Path2D.Float();
        result.moveTo(coords[0], coords[1]);
        for (int i = 2; i < coords.length; i += 2)
            result.lineTo(coords[i], coords[i + 1]);
        result.closePath();
        return result;
    }

    protected void clip(Graphics2D g2, Shape baseClip, Shape extraClip) {
        g2.setClip(baseClip);
        g2.clip(extraClip);
    }

    protected Color highlight(Color fill) {
        return PaintUtils.mixColors(fill, Color.white, 0.3f);
    }

    protected Color shadow(Color fill) {
        return PaintUtils.mixColors(fill, Color.black, 0.7f);
    }

    @Override
    public RecolorableIcon recolor(final RGBImageFilter filter) {
        try {
            // make a copy of this icon
            final AbstractRecolorableIcon result = clone();

            // scan the fields in this class, looking for values to recolor
            AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    Class clazz = result.getClass();
                    do {
                        for (Field f : clazz.getDeclaredFields()) {
                            if (Modifier.isStatic(f.getModifiers()))
                                continue;

                            Class t = f.getType();
                            if (Color.class.equals(t)) {
                                // if we find a field of type Color, tweak it
                                f.setAccessible(true);
                                Color c = (Color) f.get(result);
                                if (c != null) {
                                    int rgb = c.getRGB();
                                    int newRgb = filter.filterRGB(0, 0, rgb);
                                    Color r = new Color(newRgb, true);
                                    f.set(result, r);
                                }
    
                            } else if (RecolorableIcon.class.isAssignableFrom(t)) {
                                // if we find a RecolorableIcon field, tweak it
                                f.setAccessible(true);
                                RecolorableIcon i = (RecolorableIcon) f.get(result);
                                if (i != null) {
                                    RecolorableIcon r = i.recolor(filter);
                                    f.set(result, r);
                                }
                            }
                        }
                        clazz = clazz.getSuperclass();
                    } while (clazz != null && clazz != Object.class);
                    return null;
                }
            });

            // allow the object to perform any extra steps needed, then return
            result.finalizeColors();
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return this;
        }
    }

    @Override
    protected AbstractRecolorableIcon clone() {
        try {
            return (AbstractRecolorableIcon) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Perform any additional changes needed after the colors in this icon have
     * been altered.
     * 
     * Default is no-op; classes may override (for example, to recreate a
     * gradient from constituent colors)
     */
    protected void finalizeColors() {}

}
