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

package net.sourceforge.processdash.tool.launcher.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.event.MouseInputAdapter;

import net.sf.image4j.codec.ico.ICODecoder;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.launcher.LaunchableDataset;
import net.sourceforge.processdash.ui.icons.ExternalLinkIcon;
import net.sourceforge.processdash.ui.lib.ScalableImageIcon;

public class LaunchableDatasetList extends JList {

    /** The row the mouse is currently over */
    private int mouseOverRow = -1;

    /** True if the mouse is over the link icon */
    private boolean mouseOverLinkIcon = false;

    private static final int PAD = 2;

    private static final Resources resources = Resources
            .getDashBundle("Launcher.List");


    public LaunchableDatasetList() {
        // add padding to keep the list from looking crowded
        setBorder(BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));

        // install a custom renderer for the cells
        setCellRenderer(new DatasetRenderer());

        // listen for mouse motion/click events
        new MouseHandler();
    }

    /**
     * Install a new list of datasets to be displayed
     */
    public void setData(List<LaunchableDataset> data) {
        setListData(new Vector(data));
    }



    /**
     * Track whether the mouse is over a row and/or the link icon, and update
     * the user interface accordingly.
     */
    private void setMouseOverState(int newRow, boolean overLink) {
        if (newRow != mouseOverRow) {
            int oldRow = mouseOverRow;
            mouseOverRow = newRow;
            repaintRow(oldRow);
            repaintRow(newRow);
        }

        if (mouseOverLinkIcon != overLink) {
            mouseOverLinkIcon = overLink;
            setCursor(overLink ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    : null);
            repaintRow(newRow);
        }
    }

    private void repaintRow(int row) {
        if (row != -1) {
            Rectangle r = getCellBounds(row, row);
            if (r != null)
                repaint(r);
        }
    }


    /**
     * Respond to a user click on a link icon
     * 
     * @param row
     *            the row whose link icon was clicked
     */
    private void userClickedLink(int row) {
        if (row != -1) {
            LaunchableDataset d = (LaunchableDataset) getModel()
                    .getElementAt(row);
            if (d.getDetailsUrl() != null) {
                showUrl(d.getDetailsUrl());
            }
        }
    }

    private void showUrl(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            e.printStackTrace();
            Toolkit.getDefaultToolkit().beep();
        }
    }



    /**
     * Event listener for mouse motion/click events
     */
    private class MouseHandler extends MouseInputAdapter {

        public MouseHandler() {
            LaunchableDatasetList.this.addMouseListener(this);
            LaunchableDatasetList.this.addMouseMotionListener(this);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            int rowHeight = getFixedCellHeight();
            int overRow = (e.getY() - PAD) / rowHeight;
            boolean overLink = e.getX() > getWidth() - rowHeight;
            setMouseOverState(overRow, overLink);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            setMouseOverState(-1, false);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (mouseOverLinkIcon)
                userClickedLink(mouseOverRow);
        }

    }



    /**
     * Renderer for LaunchableDataset list items
     */
    private class DatasetRenderer extends DefaultListCellRenderer {

        private Icon team, personal, link, linkLight;

        private Font font;

        private int currentlyPaintingRow;

        public DatasetRenderer() {
            int iconSize = new JLabel("X").getPreferredSize().height;
            team = getIcon("teamicon.ico", iconSize);
            personal = getIcon("dashicon.ico", iconSize);
            link = new ExternalLinkIcon();
            linkLight = new ExternalLinkIcon(new Color(200, 200, 200));
            font = LaunchableDatasetList.this.getFont().deriveFont(Font.PLAIN);
            LaunchableDatasetList.this.setFixedCellHeight(iconSize + 4);
        }

        @Override
        public Component getListCellRendererComponent(JList list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            this.currentlyPaintingRow = index;
            LaunchableDataset d = (LaunchableDataset) value;
            super.getListCellRendererComponent(list, d.getName(), index,
                isSelected, cellHasFocus);
            setIcon(d.isTeam() ? team : personal);
            setFont(font);
            return this;
        }

        private Icon getIcon(String name, int iconSize) {
            try {
                InputStream in = getClass().getResourceAsStream(
                    "/net/sourceforge/processdash/ui/" + name);
                List<BufferedImage> images = ICODecoder.read(in);
                ImageIcon[] icons = new ImageIcon[images.size()];
                for (int i = icons.length; i-- > 0;)
                    icons[i] = new ImageIcon(images.get(i));
                return new ScalableImageIcon(iconSize, icons);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);

            if (currentlyPaintingRow == mouseOverRow) {
                int linkPad = (getHeight() - link.getIconHeight()) / 2;
                Icon icon = (mouseOverLinkIcon ? link : linkLight);
                icon.paintIcon(this, g,
                    getWidth() - link.getIconWidth() - linkPad, linkPad);
            }
        }

        @Override
        public Dimension getPreferredSize() {
            // request additional width so we can display the link icon to the
            // right of the text
            Dimension size = super.getPreferredSize();
            size.width += size.height;
            return size;
        }

        @Override
        public String getToolTipText() {
            if (mouseOverRow == -1 || !mouseOverLinkIcon)
                return null;

            LaunchableDataset d = (LaunchableDataset) getModel()
                    .getElementAt(mouseOverRow);
            if (d.getDetailsUrl() != null)
                return resources.getString("Dataset_Details");
            else
                return d.getLocation();
        }
    }

}
