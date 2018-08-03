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

package net.sourceforge.processdash.ui.lib;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.beans.PropertyDescriptor;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;


/**
 * Utility for managing a global zoom/scaling level, and for automatically
 * applying that scaling level to various user interface objects.
 */
public class ZoomManager implements ZoomLevel {


    /**
     * Base class for scaling a property of a given type.
     */
    public static abstract class Type<T> {

        private Class clazz;

        protected Type(Class<T> clazz) {
            this.clazz = clazz;
        }

        /**
         * Return true if this object can scale the given value.
         * 
         * The default implementation checks to see if the value is not null and
         * is an instanceof the class we are handling. Subclasses can override
         * if they use a different test.
         */
        public boolean matches(Object target, String propertyName,
                Object value) {
            return clazz.isInstance(value);
        }

        /**
         * Compute a scaled version of the given value.
         */
        public abstract T zoom(Object target, String propertyName, T baseValue,
                double zoom);

    }



    private double zoom, minZoom, maxZoom;

    private EventListenerList listeners;

    private List<Type> types;

    private List<ManagedProperty> properties;


    public ZoomManager() {
        this.zoom = 1.0;
        this.minZoom = 0.01;
        this.maxZoom = 100.0;
        this.listeners = new EventListenerList();
        this.types = Collections.synchronizedList(new ArrayList( //
                Arrays.asList(DEFAULT_TYPES)));
        this.properties = Collections.synchronizedList(new ArrayList());
    }


    public double getMinZoom() {
        return minZoom;
    }

    public void setMinZoom(double minZoom) {
        this.minZoom = minZoom;
    }

    public double getMaxZoom() {
        return maxZoom;
    }

    public void setMaxZoom(double maxZoom) {
        this.maxZoom = maxZoom;
    }

    /**
     * Get the current zoom level. 1.0 == 100%
     */
    public double getZoomLevel() {
        return zoom;
    }

    /**
     * Change the current zoom level.
     * 
     * If the new zoom level is the same as the current level, or if it is
     * outside the bounds of minZoom...maxZoom, this call will be a no-op.
     * 
     * Otherwise, if any ChangeListeners have been registered, they will be
     * notified about the change first.
     * 
     * Next, managed object properties will be zoomed in the order they were
     * added to the manager. So if one object was managed before another, it
     * will be zoomed first; and if several properties are being managed for an
     * object, they will be zoomed in the order they were listed in the
     * {@link #manage(Object, String...)} call.
     * 
     * @param zoom
     *            the new zoom level. 1.0 == 100%
     */
    public void setZoomLevel(double zoom) {
        if (zoom != this.zoom && zoom >= minZoom && zoom <= maxZoom) {
            this.zoom = zoom;

            ArrayList<ManagedProperty> propsToZoom;
            synchronized (properties) {
                propsToZoom = new ArrayList(properties);
            }

            ChangeEvent event = new ChangeEvent(this);
            ChangeListener[] list = listeners
                    .getListeners(ChangeListener.class);
            for (ChangeListener cl : list) {
                cl.stateChanged(event);
            }

            for (ManagedProperty mp : propsToZoom)
                mp.zoom(zoom);
        }
    }


    /**
     * Adds the specified listener to receive notifications when the zoom level
     * changes
     */
    public void addChangeListener(ChangeListener l) {
        listeners.add(ChangeListener.class, l);
    }

    /**
     * Removes a change listener from the zoom manager.
     */
    public void removeChangeListener(ChangeListener l) {
        listeners.remove(ChangeListener.class, l);
    }


    /**
     * Add a new property type that this object can automatically scale.
     * 
     * By default, this class knows how to scale properties of type int, float,
     * double, Font, and Dimension. Calling this method can add support for a
     * new property type.
     * 
     * Types are evaluated by the {@link #manage(Object, String...)} method, so
     * any relevant types must be added first. Types added later have higher
     * precedence, so the built-in types can be overridden by types added via
     * this method.
     */
    public void addType(Type t) {
        types.add(t);
    }


    /**
     * Register an object that should receive scaling when the zoom level
     * changes.
     * 
     * @param obj
     *            the object that should be scaled
     * @param properties
     *            a list of the properties of the object that should receive
     *            scaling. Properties with a null value will be silently
     *            ignored. If a property is under independent control by the
     *            end-user (for example, a window size), append a "~" to the end
     *            of the property name; this will arrange for user changes to be
     *            incorporated into subsequent scaling operations. Type handlers
     *            will be automatically selected based on the list of Types that
     *            have been installed.
     * @throws IllegalArgumentException
     *             if no type handler can be found for one of the properties, or
     *             if reflection exceptions prevent interacting with one of the
     *             named properties
     */
    public void manage(Object obj, String... properties)
            throws IllegalArgumentException {
        manage(obj, null, properties);
    }


    /**
     * Register an object that should receive scaling when the zoom level
     * changes, using a specific type handler.
     * 
     * @param obj
     *            the object that should be scaled
     * @param property
     *            a list of the properties of the object that should receive
     *            scaling. Properties with a null value will be silently
     *            ignored. If a property is under independent control by the
     *            end-user (for example, a window size), append a "~" to the end
     *            of the property name; this will arrange for user changes to be
     *            incorporated into subsequent scaling operations.
     * @param type
     *            the hardcoded type handler that will be used for <b>all</b> of
     *            the named properties. Using this method makes it possible to
     *            force the use of a specific type handler, rather than using
     *            the automatic type selection logic.
     * @throws IllegalArgumentException
     *             if the given type handler returns false from the matches()
     *             method for any of the named properties, or if reflection
     *             exceptions prevent interacting with one of the named
     *             properties
     */
    public void manage(Object obj, Type type, String... properties)
            throws IllegalArgumentException {
        // create property handlers for each named property
        List<ManagedProperty> newProperties = new ArrayList();
        for (String propName : properties) {
            try {
                ManagedProperty mp;
                if (propName.endsWith("~"))
                    mp = new AdaptiveManagedProperty(obj, type, propName);
                else
                    mp = new ManagedProperty(obj, type, propName);
                newProperties.add(mp);
            } catch (NullPointerException npe) {
                // property value was null. Ignore and don't scale it
            } catch (IllegalArgumentException iae) {
                // no scaler was found for this type
                throw iae;
            } catch (Exception e) {
                // other property or reflection error
                throw new IllegalArgumentException(e);
            }
        }

        // scale the given properties if needed
        if (zoom != 1.0) {
            for (ManagedProperty mp : newProperties)
                mp.zoom(zoom);
        }

        // add these properties to the list for future zoom changes
        this.properties.addAll(newProperties);
    }


    /**
     * Unregister an object from zoom scaling changes.
     * 
     * @param obj
     *            the object to unregister. All of its managed properties will
     *            be unregistered.
     */
    public void unmanage(Object obj) {
        synchronized (properties) {
            for (Iterator i = properties.iterator(); i.hasNext();) {
                ManagedProperty mp = (ManagedProperty) i.next();
                Object target = mp.target_.get();
                if (target == null || target == obj)
                    i.remove();
            }
        }
    }



    /**
     * A property of a managed object, which should be scaled when the zoom
     * factor changes
     */
    private class ManagedProperty {

        WeakReference target_;

        String propertyName;

        Method readMethod, writeMethod;

        Object baseValue;

        private Type type;

        public ManagedProperty(Object target, Type type, String propertyName)
                throws Exception {
            // save the target object and property name
            this.target_ = new WeakReference(target);
            this.propertyName = propertyName;

            // get the read/write methods, and the initial value
            PropertyDescriptor prop = new PropertyDescriptor(propertyName,
                    target.getClass());
            this.readMethod = prop.getReadMethod();
            this.writeMethod = prop.getWriteMethod();
            this.baseValue = readMethod.invoke(target);
            if (baseValue == null)
                throw new NullPointerException();

            // determine the appropriate handler for zooming this value
            List<Type> typesToTry = (type == null ? ZoomManager.this.types
                    : Collections.singletonList(type));
            for (Type t : typesToTry) {
                if (t.matches(target, propertyName, baseValue))
                    this.type = t;
            }
            if (this.type == null)
                throw new IllegalArgumentException(
                        "No matching type found for property '" + propertyName
                                + "' (type " + baseValue.getClass()
                                + ") in class " + target.getClass());
        }

        public final void zoom(double zoom) {
            try {
                Object target = target_.get();
                if (target == null) {
                    properties.remove(this);
                } else {
                    zoom_(target, zoom);
                }
            } catch (Exception e) {
            }
        }

        protected void zoom_(Object target, double zoom) throws Exception {
            Object newValue = type.zoom(target, propertyName, baseValue, zoom);
            writeMethod.invoke(target, newValue);
        }
    }


    /**
     * A managed property that is potentially modifiable by the end user. If the
     * user changes the value of the property between zoom operations, we should
     * incorporate their modification into our calculations moving forward
     * (rather than overwriting their change with a zoom based on the initial
     * value).
     */
    private class AdaptiveManagedProperty extends ManagedProperty {

        private Object lastValue;

        private double lastZoom, adaptiveFactor;

        public AdaptiveManagedProperty(Object target, Type type,
                String propName) throws Exception {
            super(target, type, propName.substring(0, propName.length() - 1));
            this.lastValue = this.baseValue;
            this.lastZoom = this.adaptiveFactor = 1.0;
        }

        @Override
        protected void zoom_(Object target, double zoom) throws Exception {
            Object currentValue = readMethod.invoke(target);
            if (currentValue != null && !currentValue.equals(lastValue)) {
                baseValue = currentValue;
                adaptiveFactor = lastZoom;
            }

            super.zoom_(target, zoom / adaptiveFactor);
            lastValue = readMethod.invoke(target);
            lastZoom = zoom;
        }

    }



    /**
     * The built-in property types that we know how to zoom
     */
    private static final Type[] DEFAULT_TYPES = { //
            new IntType(), //
            new FloatType(), //
            new DoubleType(), //
            new FontType(), //
            new DimensionType(1, 1), //
            new WindowSizeType(), //
            new TableRowHeightType(), //
    };



    /** Reusable class for bounded integers */
    public static class IntType extends Type<Integer> {

        private int min, max;

        public IntType() {
            this(1, Integer.MAX_VALUE);
        }

        public IntType(int min, int max) {
            super(Integer.class);
            this.min = min;
            this.max = max;
        }

        public Integer zoom(Object o, String p, Integer i, double zoom) {
            int result = (int) (i * zoom + 0.5);
            result = Math.min(result, max);
            result = Math.max(result, min);
            return Integer.valueOf(result);
        }
    }


    /** Reusable class for scaling floats */
    public static class FloatType extends Type<Float> {

        public FloatType() {
            super(Float.class);
        }

        public Float zoom(Object o, String p, Float f, double zoom) {
            return Float.valueOf((float) (f * zoom));
        }
    }


    /** Reusable class for scaling doubles */
    public static class DoubleType extends Type<Double> {

        public DoubleType() {
            super(Double.class);
        }

        public Double zoom(Object o, String p, Double d, double zoom) {
            return Double.valueOf(d * zoom);
        }
    }


    /** Default property handler for scaling fonts */
    public static class FontType extends Type<Font> {

        public FontType() {
            super(Font.class);
        }

        public Font zoom(Object o, String p, Font f, double zoom) {
            float newFontSize = (float) (f.getSize() * zoom);
            return f.deriveFont(Math.max(2, newFontSize));
        }
    }


    /** Default property handler for scaling dimensions */
    public static class DimensionType extends Type<Dimension> {

        private int minWidth, minHeight, maxWidth, maxHeight;

        public DimensionType(int minWidth, int minHeight) {
            this(minWidth, minHeight, Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        public DimensionType(int minWidth, int minHeight, int maxWidth,
                int maxHeight) {
            super(Dimension.class);
            this.minWidth = minWidth;
            this.minHeight = minHeight;
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
        }

        public Dimension zoom(Object o, String p, Dimension d, double zoom) {
            int newWidth = (int) (d.width * zoom + 0.5);
            newWidth = Math.max(newWidth, minWidth);
            newWidth = Math.min(newWidth, maxWidth);

            int newHeight = (int) (d.height * zoom + 0.5);
            newHeight = Math.max(newHeight, minHeight);
            newHeight = Math.min(newHeight, maxHeight);

            return new Dimension(newWidth, newHeight);
        }
    }


    /** Specialized property handler for scaling window sizes */
    public static class WindowSizeType extends Type<Dimension> {

        private int minWidth, minHeight;

        public WindowSizeType() {
            this(30, 30);
        }

        public WindowSizeType(int minWidth, int minHeight) {
            super(Dimension.class);
            this.minWidth = minWidth;
            this.minHeight = minHeight;
        }

        public boolean matches(Object target, String propName, Object value) {
            return target instanceof Window && propName.equals("size");
        }

        public Dimension zoom(Object target, String prop, Dimension size,
                double zoom) {
            // find the size of the display containing this window
            Window window = (Window) target;
            Rectangle display = window.getGraphicsConfiguration().getBounds();

            // compute the new width, constraining it to the display
            int newWidth = (int) (size.width * zoom + 0.5);
            newWidth = Math.min(newWidth, display.width);
            newWidth = Math.max(newWidth, minWidth);

            // compute the new height, constraining it to the display
            int newHeight = (int) (size.height * zoom + 0.5);
            newHeight = Math.min(newHeight, display.height);
            newHeight = Math.max(newHeight, minHeight);

            // get a dimension containing the desired new size
            Dimension result = getDimension(newWidth, newHeight);

            // if the new size will cause the window to overflow the
            // right or bottom edges of the display, reposition it
            Point loc = window.getLocation();
            if (loc.x + result.width > display.x + display.width
                    || loc.y + result.height > display.y + display.height) {
                int newLeft = Math.min(loc.x,
                    display.x + display.width - result.width);
                int newTop = Math.min(loc.y,
                    display.y + display.height - result.height);
                window.setLocation(newLeft, newTop);
            }

            return result;
        }

        /**
         * Create a dimension for the new size. Subclasses may override to
         * adjust the new size based on specific requirements.
         */
        protected Dimension getDimension(int newWidth, int newHeight) {
            return new Dimension(newWidth, newHeight);
        }
    }


    /** Handler that manages table scrolling while changing row height */
    public static class TableRowHeightType extends IntType {

        public TableRowHeightType() {
            super(2, Integer.MAX_VALUE);
        }

        public boolean matches(Object target, String propName, Object value) {
            return target instanceof JTable && propName.equals("rowHeight");
        }

        public Integer zoom(Object target, String prop, Integer base,
                double zoom) {
            // ask the superclass what the new row height should be
            Integer newRowHeight = super.zoom(target, prop, base, zoom);

            // see if row scrolling is disabled for this table
            JTable table = (JTable) target;
            if (table.getClientProperty(DISABLE_TABLE_SCROLL_LOCK) != null)
                return newRowHeight;

            // look for an enclosing viewport that will handle scrolling
            JViewport viewport = (JViewport) SwingUtilities
                    .getAncestorOfClass(JViewport.class, table);
            if (viewport == null)
                return newRowHeight;

            // get an object to scroll the table in the near future (after the
            // row height changes)
            DeferredScroller deferredScroller = (DeferredScroller) table
                    .getClientProperty(DeferredScroller.class);
            if (deferredScroller == null) {
                table.putClientProperty(DeferredScroller.class,
                    deferredScroller = new DeferredScroller());
            }

            // see what part of the table is currently visible/showing
            deferredScroller.viewport = viewport;
            Rectangle viewRect = viewport.getViewRect();
            double height = viewRect.getHeight();

            // identify an "anchor" point on the screen that should remain
            // stationary. if the selected table row is currently visible on
            // screen, use that as the anchor point. Otherwise, keep the row
            // currently in the center of the view stationary.
            double anchorPercent = 0.5;
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                Rectangle selRect = table.getCellRect(selectedRow, 0, false);
                double selY = selRect.getCenterY();
                double selAnchorPercent = (selY - viewRect.getMinY()) / height;
                if (0 < selAnchorPercent && selAnchorPercent < 1)
                    anchorPercent = selAnchorPercent;
            }

            // compute the new scroll location of the viewport that will keep
            // the anchor point stationary
            double anchorDelta = height * anchorPercent;
            double anchorY = viewRect.getMinY() + anchorDelta;
            double currRowHeight = table.getRowHeight();
            double changeRatio = newRowHeight / currRowHeight;
            double newAnchorY = anchorY * changeRatio;
            deferredScroller.newTopY = (int) (newAnchorY - anchorDelta + 0.5);

            // set the new location immediately if possible; otherwise register
            // it to occur after the new row height has been installed
            if (deferredScroller.newTopY + height < table.getHeight()) {
                deferredScroller.run();
            } else {
                SwingUtilities.invokeLater(deferredScroller);
            }

            // return the result
            return newRowHeight;
        }

        private class DeferredScroller implements Runnable {

            private JViewport viewport;

            private int newTopY;

            public void run() {
                Point p = viewport.getViewPosition();
                p.y = newTopY;
                viewport.setViewPosition(p);
            }
        }

    }

    public static final String DISABLE_TABLE_SCROLL_LOCK = //
            TableRowHeightType.class.getName() + ".disableScrollLock";

}
