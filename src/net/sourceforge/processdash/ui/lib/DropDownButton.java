// Copyright (C) 2001-2017 Tuma Solutions, LLC
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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.Border;

import net.sourceforge.processdash.ui.macosx.MacGUIUtils;
import net.sourceforge.processdash.util.StringUtils;

public class DropDownButton extends JPanel {

    public static final int NO_ACTION = 0;
    public static final int RUN_FIRST_MENU_OPTION = 1;
    public static final int OPEN_DROP_DOWN_MENU = 2;

    private JMenuBar menuBar;
    private JComponent leftWidget;
    private AbstractButton mainButton;
    private JButton dropDownButton;
    private boolean dropDownEnabled = false;
    private int mainButtonDefaultAction = RUN_FIRST_MENU_OPTION;
    private Icon enabledDownArrow, disDownArrow;
    private DropDownMenu menu;
    private MainButtonListener mainButtonListener = new MainButtonListener();

    public DropDownButton()                 { this(new JButton());     }
    public DropDownButton(Action a)         { this(new JButton(a));    }
    public DropDownButton(String text)      { this(new JButton(text)); }
    public DropDownButton(boolean toggle)   { this(maybeMakeToggle(toggle)); }

    private DropDownButton(AbstractButton main_button) {
        // create the drop down menu and register listeners
        menu = new DropDownMenu();
        menu.getPopupMenu().addContainerListener(new MenuContainerListener());
        menuBar = new JMenuBar();
        menuBar.add(menu);

        // save the main button and register listeners
        mainButton = main_button;
        mainButton.addActionListener(mainButtonListener);

        // create the drop-down button and register listeners
        enabledDownArrow = new SmallDownArrow();
        disDownArrow = new SmallDisabledDownArrow();
        dropDownButton = new JButton(disDownArrow);
        dropDownButton.setDisabledIcon(disDownArrow);
        dropDownButton.addMouseListener(new DropDownListener());
        dropDownButton.setFocusPainted(false);

        // perform operating-system-specific tweaks on our components
        configureGUI();

        // add our elements to the component hierarchy
        add(dropDownButton);
        add(mainButton);
        add(menuBar);

        // configure the enclosing JPanel
        this.setBackground(null);
        this.setOpaque(false);

        setEnabled(false);
    }

    private void configureGUI() {
        if (MacGUIUtils.isMacOSX())
            configureMacGUI();
        else
            configureNormalGUI();
    }

    private void configureNormalGUI() {
        setLayout(new NormalDropDownButtonLayout());

        mainButton.setBorder(new RightChoppedBorder(mainButton.getBorder(), 1));
        dropDownButton.setMargin(new Insets(1, 1, 1, 1));
    }

    private void configureMacGUI() {
        setLayout(new MacDropDownButtonLayout());

        String buttonType = getMacButtonType();
        mainButton.putClientProperty(MAC_BUTTON_TYPE, buttonType);
        mainButton.putClientProperty(MAC_SEGMENT_POSITION, MAC_FIRST_SEGMENT);
        dropDownButton.putClientProperty(MAC_BUTTON_TYPE, buttonType);
        dropDownButton.putClientProperty(MAC_SEGMENT_POSITION, MAC_LAST_SEGMENT);
    }

    private String getMacButtonType() {
        return (isTextButton() ? MAC_TEXT_BUTTON_TYPE : MAC_ICON_BUTTON_TYPE);
    }

    /** Return true if this button is displaying text, false if it is
     * displaying an icon.  (Note that displaying BOTH is not supported
     * by this class at this time.)
     */
    private boolean isTextButton() {
        return StringUtils.hasValue(mainButton.getText());
    }

    /** Request a specific margin around the button contents.
     * 
     * This is programmed to be a no-op on the Mac, because it causes very
     * bad behavior for some Mac releases of Java.  Clients of DropDownButton
     * should be aware of this, and refrain from setting margins on the main
     * button or the left widget.
     */
    protected void setMainButtonMargin(Insets i) {
        if (!MacGUIUtils.isMacOSX())
            mainButton.setMargin(i);
    }

    private static AbstractButton maybeMakeToggle(boolean toggle) {
        if (toggle == false)
            return new JButton();
        else
            return new JToggleButton();
    }

    public AbstractButton getButton()  { return mainButton;     }
    //public JButton getDropDownButton() { return dropDownButton; }
    public JMenu   getMenu()           { return menu;           }

    public JComponent getLeftWidget() {
        return leftWidget;
    }

    public void setLeftWidget(JComponent newWidget) {
        if (leftWidget != null)
            remove(leftWidget);

        leftWidget = newWidget;

        if (leftWidget != null)
            add(leftWidget);

        if (leftWidget instanceof AbstractButton) {
            if (MacGUIUtils.isMacOSX()) {
                leftWidget.putClientProperty(MAC_BUTTON_TYPE, getMacButtonType());
                leftWidget.putClientProperty(MAC_SEGMENT_POSITION, MAC_FIRST_SEGMENT);
                mainButton.putClientProperty(MAC_SEGMENT_POSITION, MAC_MIDDLE_SEGMENT);
            } else {
                leftWidget.setBorder(new RightChoppedBorder(leftWidget
                        .getBorder(), 1));
            }
        } else {
            if (MacGUIUtils.isMacOSX()) {
                mainButton.putClientProperty(MAC_SEGMENT_POSITION, MAC_FIRST_SEGMENT);
            }
        }

        invalidate();
    }

    public void setEnabled(boolean enable) {
        if (leftWidget != null)
            leftWidget.setEnabled(enable);
        mainButton.setEnabled(enable);
        dropDownButton.setEnabled(enable);
    }
    public boolean isEnabled() {
        return mainButton.isEnabled();
    }
    public boolean isEmpty() {
        return (menu.getItemCount() == 0);
    }

    /** Set the behavior of the main button.
     *
     * @param enable if true, a click on the main button will trigger
     *    an actionPerformed() on the first item in the popup menu.
     */
    public void setRunFirstMenuOption(boolean enable) {
        setMainButtonBehavior(enable ? RUN_FIRST_MENU_OPTION : NO_ACTION);
    }

    /** @return true if a click on the main button will trigger an
     *    actionPerformed() on the first item in the popup menu.
     */
    public boolean getRunFirstMenuOption() {
        return mainButtonDefaultAction == RUN_FIRST_MENU_OPTION;
    }

    /** Set the behavior of the main button.
     *
     * @param enable if true, a click on the main button will cause the drop
     *    down menu to be displayed.
     */
    public void setOpenPopupMenu(boolean enable) {
        setMainButtonBehavior(enable ? OPEN_DROP_DOWN_MENU : NO_ACTION);
    }

    /** @return true if a click on the main button will cause the drop down
     *    menu to be displayed.
     */
    public boolean getOpenPopupMenu() {
        return mainButtonDefaultAction == OPEN_DROP_DOWN_MENU;
    }

    public void setMainButtonBehavior(int which) {
        mainButton.removeActionListener(mainButtonListener);
        mainButtonDefaultAction = which;
        setEnabled(which == NO_ACTION || isEmpty() == false);
        if (which != NO_ACTION)
            mainButton.addActionListener(mainButtonListener);
    }

    private void setDropDownEnabled(boolean enable) {
        dropDownEnabled = enable;
        dropDownButton.setIcon(enable ? enabledDownArrow : disDownArrow);
        if (mainButtonDefaultAction != NO_ACTION)
            setEnabled(enable);
    }

    @Override
    public Dimension getMaximumSize() {
        return super.getPreferredSize();
    }

    /** This object responds to events on the main button. */
    private class MainButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (getRunFirstMenuOption() && !isEmpty()) {
                JMenuItem defaultItem = menu.getItem(0);
                if (defaultItem != null)
                    defaultItem.doClick(0);
            } else if (getOpenPopupMenu() && !isEmpty()) {
                if (dropDownEnabled) menu.doClick(0);
            }
        }
    }

    /** This object responds to events on the drop-down button. */
    private class DropDownListener extends MouseAdapter {
        boolean pressHidPopup = false;
        public void mouseClicked(MouseEvent e) {
            if (dropDownEnabled && !pressHidPopup) menu.doClick(0);
        }
        public void mousePressed(MouseEvent e) {
            if (dropDownEnabled) menu.dispatchMouseEvent(e);
            if (menu.isPopupMenuVisible())
                pressHidPopup = false;
            else
                pressHidPopup = true;

        }
        public void mouseReleased(MouseEvent e) { }
    }

    /** This object watches for insertion/deletion of menu items in
     * the popup menu, and disables the drop-down button when the
     * popup menu becomes empty. */
    private class MenuContainerListener implements ContainerListener {
        public void componentAdded(ContainerEvent e) {
            setDropDownEnabled(true);
        }
        public void componentRemoved(ContainerEvent e) {
            setDropDownEnabled(!isEmpty());
        }
    }

    /** An adapter that wraps a border object, and chops some number of
     *  pixels off the right hand side of the border.
     */
    private class RightChoppedBorder implements Border {
        private Border b;
        private int w;

        public RightChoppedBorder(Border b, int width) {
            this.b = b;
            this.w = width;
        }

        public void paintBorder(Component c,
                                Graphics g,
                                int x,
                                int y,
                                int width,
                                int height) {
            Shape clipping = g.getClip();
            g.setClip(x, y, width, height);
            b.paintBorder(c, g, x, y, width + w, height);
            g.setClip(clipping);
        }

        public Insets getBorderInsets(Component c) {
            Insets i = b.getBorderInsets(c);
            return new Insets(i.top, i.left, i.bottom, i.right-w);
        }

        public boolean isBorderOpaque() {
            return b.isBorderOpaque();
        }
    }

    private class DropDownMenu extends JMenu {
        public void dispatchMouseEvent(MouseEvent e) {
            processMouseEvent(e);
        }
    }


    /** An icon to draw a small downward-pointing arrow.
     */
    static class SmallDownArrow implements Icon {

        Color arrowColor = Color.black;

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            double scale = g2.getTransform().getScaleX();
            g2.translate(x, y);
            g2.scale(1 / scale, 1 / scale);

            int pw = (int) (getIconWidth() * scale) - 1;
            int hw = pw / 2;
            int d = pw - 2 * hw;
            int[] xx = new int[] { 0, hw, hw + d, pw };
            int[] yy = new int[] { 0, hw, hw, 0 };

            g2.setColor(arrowColor);
            g2.fillPolygon(xx, yy, 4);
            g2.drawPolygon(xx, yy, 4);
        }

        public int getIconWidth() {
            return 5;
        }

        public int getIconHeight() {
            return 3;
        }

    }

    /** An icon to draw a disabled small downward-pointing arrow.
     */
    static class SmallDisabledDownArrow extends SmallDownArrow {

        public SmallDisabledDownArrow() {
            arrowColor = new Color(140, 140, 140);
        }

    }

    /** Custom layout manager to arrange our children on PC/Unix systems.
     */
    private class NormalDropDownButtonLayout implements LayoutManager {

        private Rectangle bounds = new Rectangle();

        public void layoutContainer(Container parent) {
            parent.getBounds(bounds);

            int lww = getLeftWidgetWidth();
            int ddw = getDropDownButtonWidth();
            int mainW = bounds.width - ddw - lww;

            if (leftWidget != null)
                leftWidget.setBounds(0, 0, lww, bounds.height);
            mainButton.setBounds(lww, 0, mainW, bounds.height);
            dropDownButton.setBounds(lww + mainW, 0, ddw, bounds.height);

            menuBar.setBounds(0, 0, 0, bounds.height);
        }

        public Dimension minimumLayoutSize(Container parent) {
            Dimension result = mainButton.getMinimumSize();
            result.width += getLeftWidgetWidth();
            result.width += getDropDownButtonWidth();
            return result;
        }

        public Dimension preferredLayoutSize(Container parent) {
            Dimension result = mainButton.getPreferredSize();
            result.width += getLeftWidgetWidth();
            result.width += getDropDownButtonWidth();
            return result;
        }

        private int getLeftWidgetWidth() {
            if (leftWidget == null)
                return 0;
            else
                return leftWidget.getPreferredSize().width;
        }

        private int getDropDownButtonWidth() {
            return dropDownButton.getMinimumSize().width;
        }

        public void addLayoutComponent(String name, Component comp) {}
        public void removeLayoutComponent(Component comp) {}

    }

    /** Custom layout manager to arrange our children on Mac OS X systems.
     */
    private class MacDropDownButtonLayout implements LayoutManager {

        private Rectangle bounds = new Rectangle();

        public void layoutContainer(Container parent) {
            parent.getBounds(bounds);

            int lww = getConstrainedWidth(leftWidget);
            int ddw = getConstrainedWidth(dropDownButton);
            int mainW = bounds.width - ddw - lww;

            if (leftWidget != null)
                leftWidget.setBounds(0, 0, lww, bounds.height);
            mainButton.setBounds(lww, 0, mainW, bounds.height);
            dropDownButton.setBounds(lww + mainW, 0, ddw, bounds.height);

            Insets leftInsets;
            int removedPadding;
            if (leftWidget == null) {
                leftInsets = mainButton.getBorder().getBorderInsets(mainButton);
                removedPadding = mainButton.getPreferredSize().width - mainW;
            } else {
                leftInsets = leftWidget.getBorder().getBorderInsets(leftWidget);
                removedPadding = leftWidget.getMinimumSize().width - lww;
            }
            if (removedPadding > 0)
                leftInsets.left -= removedPadding / 2;
            menuBar.setBounds(leftInsets.left, 0, 0, bounds.height
                    - leftInsets.bottom);
        }

        public Dimension minimumLayoutSize(Container parent) {
            Dimension result = mainButton.getMinimumSize();
            result.width = getConstrainedWidth(leftWidget)
                    + getConstrainedWidth(mainButton)
                    + getConstrainedWidth(dropDownButton);
            return result;
        }

        public Dimension preferredLayoutSize(Container parent) {
            Dimension result = mainButton.getPreferredSize();
            result.width = getConstrainedWidth(leftWidget)
                    + getConstrainedWidth(mainButton)
                    + getConstrainedWidth(dropDownButton);
            return result;
        }

        private int getConstrainedWidth(JComponent c) {
            if (c == null)
                return 0;

            int width = c.getMinimumSize().width;
            int widthLimit = getIconBasedWidthLimit(c);
            return Math.min(width, widthLimit);
        }

        private int getIconBasedWidthLimit(JComponent c) {
            if ((c instanceof AbstractButton) == false)
                return NO_LIMIT;

            Object buttonType = c.getClientProperty(MAC_BUTTON_TYPE);
            if (! MAC_ICON_BUTTON_TYPE.equals(buttonType))
                return NO_LIMIT;

            AbstractButton button = (AbstractButton) c;
            Icon i = button.getIcon();
            if (i == null)
                return NO_LIMIT;

            int iconWidth = i.getIconWidth();

            Object segmentPos = c.getClientProperty(MAC_SEGMENT_POSITION);
            if (MAC_LAST_SEGMENT.equals(segmentPos))
                return iconWidth + 10;
            else if (MAC_MIDDLE_SEGMENT.equals(segmentPos))
                return iconWidth + 4;
            else
                return iconWidth + 12;
        }
        private static final int NO_LIMIT = 9999;

        public void addLayoutComponent(String name, Component comp) {}
        public void removeLayoutComponent(Component comp) {}

    }

    private static final String MAC_BUTTON_TYPE = "JButton.buttonType";
    private static final String MAC_ICON_BUTTON_TYPE = "segmented";
    private static final String MAC_TEXT_BUTTON_TYPE = "segmentedRoundRect";

    private static final String MAC_SEGMENT_POSITION = "JButton.segmentPosition";
    private static final String MAC_FIRST_SEGMENT = "first";
    private static final String MAC_MIDDLE_SEGMENT = "middle";
    private static final String MAC_LAST_SEGMENT = "last";

}
