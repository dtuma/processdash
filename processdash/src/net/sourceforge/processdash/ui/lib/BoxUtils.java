// Copyright (C) 2009 Tuma Solutions, LLC
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

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;

public abstract class BoxUtils extends Box {

    public static final Object GLUE = new Object();

    private BoxUtils(int axis, Object... contents) {
        super(axis);
        addItems(contents);
    }

    public BoxUtils addItems(Object... items) {
        for (Object item : items)
            addItem(item);
        return this;
    }

    public BoxUtils addItem(Object item) {
        if (item instanceof String) {
            add(new JLabel((String) item));
        } else if (item instanceof Component) {
            add((Component) item);
        } else if (item instanceof Integer) {
            add(createStrutItem((Integer) item));
        } else if (item instanceof Dimension) {
            add(Box.createRigidArea((Dimension) item));
        } else if (item == GLUE) {
            add(createGlueItem());
        }
        return this;
    }

    protected abstract Component createStrutItem(int size);

    protected abstract Component createGlueItem();

    public static class HBox extends BoxUtils {
        public HBox(Object... contents) {
            super(BoxLayout.X_AXIS, contents);
        }

        @Override
        protected Component createGlueItem() {
            return Box.createHorizontalGlue();
        }

        @Override
        protected Component createStrutItem(int size) {
            return Box.createHorizontalStrut(size);
        }
    }

    public static BoxUtils hbox(Object... contents) {
        return new HBox(contents);
    }

    public static class VBox extends BoxUtils {
        public VBox(Object... contents) {
            super(BoxLayout.Y_AXIS, contents);
        }

        @Override
        protected Component createGlueItem() {
            return Box.createVerticalGlue();
        }

        @Override
        protected Component createStrutItem(int size) {
            return Box.createVerticalStrut(size);
        }
    }

    public static BoxUtils vbox(Object... contents) {
        return new VBox(contents);
    }

}
