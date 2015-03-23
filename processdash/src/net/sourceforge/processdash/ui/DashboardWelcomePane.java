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

package net.sourceforge.processdash.ui;

import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.help.PCSH;

public class DashboardWelcomePane extends JDialog implements HyperlinkListener {

    private static final String FIRST_TIME_HELP_URL = "/help/first-use.htm";

    private static final Resources resources = Resources
            .getDashBundle("ProcessDashboard");

    public DashboardWelcomePane(JFrame parent) {
        super(parent, resources.getString("Welcome_Dialog_Title"), false);

        JEditorPane pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.addHyperlinkListener(this);
        try {
            pane.setPage(Browser.mapURL(FIRST_TIME_HELP_URL));
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return;
        }
        getContentPane().add(new JScrollPane(pane));

        setLocation(parent.getLocation().x + 80, parent.getLocation().y + 20);
        setSize(475, 350);
        setVisible(true);
    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            String url = e.getURL().toString();
            if (url.startsWith("http://help/"))
                PCSH.displayHelpTopic(url.substring(12));
            else
                Browser.launch(url);
        }
    }

}
