// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.Border;


public class DropDownButton extends Box {

    private JButton mainButton;
    private JButton dropDownButton;
    private boolean dropDownEnabled = false;
    private boolean mainRunsDefaultMenuOption = true;
    private ImageIcon enabledDownArrow, disDownArrow;
    private DropDownMenu menu;
    private MainButtonListener mainButtonListener = new MainButtonListener();


    public DropDownButton(JButton mainButton) {
        super(BoxLayout.X_AXIS);

        menu = new DropDownMenu();
        menu.getPopupMenu().addContainerListener(new MenuContainerListener());
        JMenuBar bar = new JMenuBar();
        bar.add(menu);
        bar.setMaximumSize(new Dimension(0,100));
        bar.setMinimumSize(new Dimension(0,1));
        bar.setPreferredSize(new Dimension(0,1));
        add(bar);

        this.mainButton = mainButton;
        mainButton.addActionListener(mainButtonListener);
        mainButton.setBorder
            (new RightChoppedBorder(mainButton.getBorder(), 2));
        add(mainButton);

        enabledDownArrow = new ImageIcon
            (getClass().getResource("down-arrow.gif"));
        disDownArrow = new ImageIcon
            (getClass().getResource("down-arrow-dis.gif"));
        dropDownButton = new JButton(disDownArrow);
        dropDownButton.setDisabledIcon(disDownArrow);
        dropDownButton.addMouseListener(new DropDownListener());
        dropDownButton.setMaximumSize(new Dimension(11,100));
        dropDownButton.setMinimumSize(new Dimension(11,10));
        dropDownButton.setPreferredSize(new Dimension(11,10));
        dropDownButton.setFocusPainted(false);
        add(dropDownButton);

        setEnabled(false);
    }

    public DropDownButton()                 { this(new JButton());     }
    public DropDownButton(Action a)         { this(new JButton(a));    }
    public DropDownButton(Icon icon)        { this(new JButton(icon)); }
    public DropDownButton(String text)      { this(new JButton(text)); }
    public DropDownButton(String t, Icon i) { this(new JButton(t, i)); }

    public JButton getButton()         { return mainButton;     }
    //public JButton getDropDownButton() { return dropDownButton; }
    public JMenu   getMenu()           { return menu;           }

    public void setEnabled(boolean enable) {
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
        mainButton.removeActionListener(mainButtonListener);
        mainRunsDefaultMenuOption = enable;
        setEnabled(mainRunsDefaultMenuOption == false ||
                   isEmpty() == false);
        if (mainRunsDefaultMenuOption)
            mainButton.addActionListener(mainButtonListener);
    }

    /** @return true if a click on the main button will trigger an
     *    actionPerformed() on the first item in the popup menu.
     */
    public boolean getRunFirstMenuOption() {
        return mainRunsDefaultMenuOption;
    }

    private void setDropDownEnabled(boolean enable) {
        dropDownEnabled = enable;
        dropDownButton.setIcon(enable ? enabledDownArrow : disDownArrow);
        if (mainRunsDefaultMenuOption)
            setEnabled(enable);
    }

    /** This object responds to events on the main button. */
    private class MainButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (mainRunsDefaultMenuOption && !isEmpty()) {
                JMenuItem defaultItem = menu.getItem(0);
                if (defaultItem != null)
                    defaultItem.doClick(0);
            }
        }
    }

    /** This object responds to events on the drop-down button. */
    private class DropDownListener extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            if (dropDownEnabled) menu.doClick(0);
        }
        public void mousePressed(MouseEvent e) {
            if (dropDownEnabled) menu.dispatchMouseEvent(e);
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
}
