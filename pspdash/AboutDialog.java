//This class shared from the Giant Java Tree, http://www.gjt.org
//originally in package org.gjt.fredde.util.gui;

/*  AboutDialog - Displays text in a JEditorPane
 *  Copyright (C) 1999 Fredrik Ehnbom
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package pspdash;

import java.io.IOException;
import java.awt.Frame;
import javax.swing.*;
import javax.swing.event.*;

/**
 * Displays information about an application in a JEditorPane.
 */
public class AboutDialog extends JDialog implements HyperlinkListener {

    /**
     * Creates a new AboutDialog.
     * @param text The text to display. If it begins with &lt;html&gt; it'll
     *         display directly what you wrote or else it asumes that
     *        you wanted it to display a location.
     */
    public AboutDialog(Frame parent, String text) {
        super(parent, "About");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JEditorPane jep = new JEditorPane();
        jep.setContentType("text/html");
        jep.setEditable(false);
        jep.addHyperlinkListener(this);

        if (text.toLowerCase().startsWith("<html>")) {
            jep.setText(text);
        } else {
            try {
                jep.setPage(text);
            } catch (IOException ioe) {
                System.err.println(ioe);
            }
        }

        getContentPane().add(new JScrollPane(jep));
        show();
    }

    /**
     * Launch a browser when you click on a link
     */
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
            Browser.launch(e.getURL().toString());
    }
}
