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

import javax.swing.*;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Vector;

// The scriptButton class activates a browser loaded with the current phase's
// script when it is pressed.  A right click can activate a popup menu with
// a selection of scripts.
class ScriptButton extends JButton {
    PSPDashboard parent = null;
    ImageIcon enabled_icon = null;
    ImageIcon disabled_icon = null;
    String browserCommand = null;
    String scriptFilename = null;
    String path  = null;
    Vector paths = null; // A list of valid script paths for the popup menu
    JPopupMenu jPopup = null;
    Rectangle rv = new Rectangle();
    Point popupOrigin;
    int menuIndex;  // keeps track of the last menu item selected, -1 if none.

    public static final String URL_PREFIX = "/";

    ScriptButton(PSPDashboard dash) {
        super();
        try {
            enabled_icon = new ImageIcon(getClass().getResource("script.gif"));
            disabled_icon = new ImageIcon(getClass().getResource("scriptd.gif"));
            setIcon(enabled_icon);
            setDisabledIcon(disabled_icon);
        } catch (Exception e) {
            setText("Script");
        }
        setMargin (new Insets (1,2,1,2));
        parent = dash;
        setEnabled(false);    // Add event listeners for the button itself.
        addActionListener(scriptButtonActionListener);
        addMouseListener(scriptButtonMouseListener);

        browserCommand = Settings.getVal("browser.command");
        dash.getContentPane().add(this);
    }

    // setPaths creates a new JPopupMenu, and populates it with valid script paths.
    // However, it does not command the menu to be displayed.
    public void setPaths(Vector v) {
        paths = v;
        if (paths == null)
            setEnabled(false);
        else {
            setEnabled(paths.size() != 0);
            ScriptID id;      /* debug code
            for (int i = 0; i < paths.size(); i++) {
                id = (ScriptID) paths.elementAt (i);
                System.err.println(id.toString());
            }*/

            // create the popup menu
            jPopup = new JPopupMenu("Templates");
            jPopup.setInvoker (this);

            // initialize the menuIndex to "none selected".
            menuIndex = -1;
            JMenuIndexItem m;

            // populate the popup menu with items for each script.
            for (int i = 0; i < paths.size(); i++) {
                id = (ScriptID) paths.elementAt (i);
                m = (JMenuIndexItem)jPopup.add (new JMenuIndexItem (id.getUserName(),i));

                // add event listeners for each popup menu item.
                m.addActionListener (scriptMenuActionListener);
                m.addMouseListener (scriptMenuMouseListener);
            }
        }
    }

    public void hidePopup() {
        if (menuIndex == -1) {
            jPopup.setVisible (false);
        }
    }

    protected void viewScript (String theScript, String thePath) {
        if (theScript != null) {
            String url = encode(thePath) + "//" + theScript;
            Browser.launch(url);
        }
    }

    protected String encode(String path) {
        String result = URLEncoder.encode(path);
        result = StringUtils.findAndReplace(result, "%2f", "/");
        result = StringUtils.findAndReplace(result, "%2F", "/");
        return result;
    }

    // JMenuIndexItem is an extended JMenuItem class that adds a data member
    // to keep track of which item in the menu it is.
    public class JMenuIndexItem extends JMenuItem {
        int index;

        public JMenuIndexItem(String text, int index) {
            super(text);
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

    }

    // This data member of ScriptButton handles action events for ScriptButton.
    ActionListener scriptButtonActionListener = new ActionListener () {

        // If a click & release is performed on the script button, view the script
        // for the current phase, and hide the popup if it is visible.
        public void actionPerformed(ActionEvent e) {
            if (jPopup != null && jPopup.isVisible())
                jPopup.setVisible (false);
            if (paths != null) {
                ScriptID id = (ScriptID)paths.elementAt (0);
                viewScript (id.getScript(), id.getDataPath());
            }
        }

    };

    // This data member of ScriptButton handles mouse events for ScriptButton.
    MouseAdapter scriptButtonMouseListener = new MouseAdapter () {

        // If the right mouse button is pressed on the script button, display the
        // popup menu.
        public void mousePressed(MouseEvent e) {

            if (e.getModifiers() == e.BUTTON3_MASK) {
                popupOrigin = e.getPoint();
                Point p = getLocationOnScreen();
                jPopup.setLocation (p.x + e.getX(), p.y + e.getY());
                jPopup.setVisible (true);
            }
        }

        // If the left mouse button is released on the script button, hide the
        // popup menu if it is visible.
        public void mouseReleased(MouseEvent e) {
            if (jPopup != null && jPopup.isVisible()) {
                if (e.getModifiers() == e.BUTTON1_MASK) {
                    jPopup.setVisible (false);
                }
            }
        }

    };

    // This data member of ScriptButton handles action events for menu items.
    ActionListener scriptMenuActionListener = new ActionListener () {

        // If a click & release is performed on a menu item, view the script
        // for the menu item.
        public void actionPerformed(ActionEvent e) {
            if (menuIndex >= 0) {
                ScriptID id = (ScriptID)paths.elementAt (menuIndex);
                viewScript (id.getScript(), id.getDataPath());
            }
        }

    };

    // This data member of ScriptButton handles mouse events for menu items.
    MouseAdapter scriptMenuMouseListener = new MouseAdapter() {

        // When the mouse enters a menu item, set the menuIndex to that menu item.
        public synchronized void mouseEntered(MouseEvent e) {
            if(e.getSource() instanceof JMenuIndexItem) {
                menuIndex = ((JMenuIndexItem)e.getSource()).getIndex();
            }
        }

        // When the mouse exits a menu item, set the menuIndex to "none selected".
        public synchronized void mouseExited(MouseEvent e) {
            menuIndex = -1;
        }

    };

}
