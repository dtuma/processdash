// Copyright (C) 2021 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.perm;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;

import net.sourceforge.processdash.tool.bridge.ResourceBridgeConstants;
import net.sourceforge.processdash.ui.lib.WrappingHtmlLabel;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.HttpException;

public class UserAccountFlagErrorMessage implements ResourceBridgeConstants {

    public static Component get(HttpException.Forbidden f, String footer) {
        // if this Forbidden error was not due to a user account flag, abort
        String extErrCode = f.getResponseHeader(EXTENDED_ERROR_HEADER);
        if (USER_ACCOUNT_FLAG_ERROR_VALUE.equals(extErrCode) == false)
            return null;

        // retrieve the flags present on the user's account
        List<UserAccountFlag> flags;
        try {
            String pdesUrl = f.getUrl().toExternalForm();
            flags = new WhoAmI(pdesUrl).getUserAccountFlags();
            if (flags.isEmpty())
                return null;
        } catch (Exception e) {
            // if we couldn't retrieve the flags, abort
            return null;
        }

        // build an HTML error message for the first flag found
        UserAccountFlag flag = flags.get(0);
        String html = "<html><div width='400'>" + flag.getHtml() + "<br/><br/>"
                + HTMLUtils.escapeEntities(footer) + "</div></html>";

        // create a label to display the HTML, with hyperlink handling
        WrappingHtmlLabel label = new WrappingHtmlLabel(html);
        label.setFont(UIManager.getFont("Label.font"));
        label.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,
            Boolean.TRUE);
        label.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (EventType.ACTIVATED.equals(e.getEventType())
                        && e.getURL() != null) {
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Toolkit.getDefaultToolkit().beep();
                    }
                }
            }
        });
        return label;
    }

}
