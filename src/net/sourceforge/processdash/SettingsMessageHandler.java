// Copyright (C) 2020 Tuma Solutions, LLC
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

package net.sourceforge.processdash;

import java.io.StringReader;
import java.util.Map.Entry;
import java.util.Properties;

import net.sourceforge.processdash.msg.MessageEvent;
import net.sourceforge.processdash.msg.MessageHandler;
import net.sourceforge.processdash.util.XMLUtils;

/**
 * @since 2.5.5.1
 */
public class SettingsMessageHandler implements MessageHandler {

    private static final String[] MESSAGE_TYPES = { "pdash.alterSettings" };

    public String[] getMessageTypes() {
        return MESSAGE_TYPES;
    }

    public void handle(MessageEvent message) {
        // parse the text contents of the XML tag as a Java Properties file
        Properties props = new Properties();
        try {
            String text = XMLUtils.getTextContents(message.getMessageXml());
            props.load(new StringReader(text));
        } catch (Exception e) {
            System.out.println("Could not load settings from message "
                    + message.getMessageId() + " - ignoring");
            e.printStackTrace();
            return;
        }

        // store the resulting properties into the settings file
        for (Entry<Object, Object> e : props.entrySet()) {
            String prop = (String) e.getKey();
            String val = (String) e.getValue();
            InternalSettings.set(prop, val);
        }
    }

}
