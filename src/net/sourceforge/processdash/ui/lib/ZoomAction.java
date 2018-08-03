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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Event;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;


public class ZoomAction extends AbstractAction {

    private ZoomManager zoom;

    private Component parent;

    private ResourceBundle resources;

    private ChildAction[] childActions;

    private ZoomLevelSlider slider;

    private JDialog dialog;


    public ZoomAction(ZoomManager zoom, Component parent,
            ResourceBundle resources, Icon icon) {
        super(resources.getString("Menu_Text") + "...", icon);
        this.zoom = zoom;
        this.parent = parent;
        this.resources = resources;
        this.childActions = new ChildAction[] { new Larger(), new Smaller(),
                new Default() };
    }

    public JMenu getZoomMenu(boolean enableKeyBindings) {
        JMenu result = new JMenu(resources.getString("Menu_Text"));
        result.add(this);
        for (ChildAction a : childActions) {
            result.add(a);
            if (enableKeyBindings)
                a.installAcceleratorKeys(result);
        }
        return result;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // create the user interface if it hasn't been built yet
        if (dialog == null)
            buildUI();

        // display the dialog
        dialog.setVisible(true);
    }

    private void buildUI() {
        slider = new ZoomLevelSlider(zoom);
        for (ChildAction a : childActions)
            a.installAcceleratorKeys(slider);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(slider);

        String title = resources.getString("Window_Title");
        Window w = SwingUtilities.getWindowAncestor(parent);
        if (w instanceof Frame)
            dialog = new JDialog((Frame) w, title, false);
        else
            dialog = new JDialog((Dialog) w, title, false);
        dialog.getContentPane().add(panel);
        dialog.pack();

        Rectangle r = w.getBounds();
        dialog.setLocation(r.x + r.width - dialog.getWidth() - 50, //
            r.y + r.height - dialog.getHeight() - 50);
    }



    private abstract class ChildAction extends AbstractAction {

        ChildAction(String resourceKey) {
            super(resources.getString(resourceKey));
        }

        protected abstract void installAcceleratorKeys(JComponent c);

    }


    private class Larger extends ChildAction {

        private static final String ACTION_KEY = "Zoom Larger";

        Larger() {
            super("Zoom_Larger");
            putValue(ACCELERATOR_KEY, getKey(KeyEvent.VK_PLUS, CTRL));
        }

        @Override
        protected void installAcceleratorKeys(JComponent c) {
            // Register additional accelerator keys based on other places
            // where the plus appears on the keyboard
            InputMap map = c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            map.put(getKey(KeyEvent.VK_PLUS, CTRL), ACTION_KEY);
            map.put(getKey(KeyEvent.VK_ADD, CTRL), ACTION_KEY);
            map.put(getKey(KeyEvent.VK_EQUALS, CTRL), ACTION_KEY);
            map.put(getKey(KeyEvent.VK_EQUALS, CTRL | SHIFT), ACTION_KEY);
            c.getActionMap().put(ACTION_KEY, this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            zoom.setZoomLevel(zoom.getZoomLevel() + 0.1);
        }

    }


    private class Smaller extends ChildAction {

        private static final String ACTION_KEY = "Zoom Smaller";

        Smaller() {
            super("Zoom_Smaller");
            putValue(ACCELERATOR_KEY, getKey(KeyEvent.VK_MINUS, CTRL));
        }

        @Override
        protected void installAcceleratorKeys(JComponent c) {
            // Register additional accelerator keys based on other places
            // where the minus appears on the keyboard
            InputMap map = c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            map.put(getKey(KeyEvent.VK_MINUS, CTRL), ACTION_KEY);
            map.put(getKey(KeyEvent.VK_SUBTRACT, CTRL), ACTION_KEY);
            c.getActionMap().put(ACTION_KEY, this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            zoom.setZoomLevel(zoom.getZoomLevel() - 0.1);
        }

    }


    private class Default extends ChildAction {

        private static final String ACTION_KEY = "Zoom Default";

        Default() {
            super("Zoom_Default");
            putValue(ACCELERATOR_KEY, getKey(KeyEvent.VK_0, CTRL));
        }

        @Override
        protected void installAcceleratorKeys(JComponent c) {
            // Register the accelerator key on the given component
            InputMap map = c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            map.put(getKey(KeyEvent.VK_0, CTRL), ACTION_KEY);
            c.getActionMap().put(ACTION_KEY, this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            zoom.setZoomLevel(1.0);
        }

    }


    private static final int SHIFT = Event.SHIFT_MASK;

    private static final int CTRL = InputEvent.CTRL_DOWN_MASK;

    private static KeyStroke getKey(int key, int modifier) {
        return KeyStroke.getKeyStroke(key, modifier);
    }

}
