// Copyright (C) 2008-2011 Tuma Solutions, LLC
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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class JLinkLabel extends JPanel {

    public JLinkLabel(String label) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        while (label.length() > 0) {
            Matcher m = LINK_PAT.matcher(label);
            if (m.lookingAt()) {
                String preText = m.group(1);
                String attrs = m.group(2);
                String linkText = m.group(3);

                if (preText.length() > 0)
                    add(new JLabel(preText));
                add(new Link(linkText, getHref(attrs)));

                label = label.substring(m.end());

            } else {
                add(new JLabel(label));
                label = "";
            }
        }
    }

    public JLinkLabel(String label, ActionListener handler) {
        this(label);
        addActionListener(handler);
    }

    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    protected void fireActionPerformed(ActionEvent e) {
        for (ActionListener l : listenerList.getListeners(ActionListener.class))
            l.actionPerformed(e);
    }

    private class Link extends JLabel {
        public Link(String text, final String href) {
            super("<html><a href='#'>" + text + "</a></html>");
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setForeground(Color.blue);
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    fireActionPerformed(new ActionEvent(this,
                            ActionEvent.ACTION_PERFORMED, href));
                }
            });
        }
        @Override
        public Dimension getMaximumSize() {
            return super.getPreferredSize();
        }
    }

    private static final String getHref(String tagContents) {
        Matcher m = HREF_PAT.matcher(tagContents);
        if (m.find())
            return m.group(1);
        else
            return null;
    }

    private static final Pattern LINK_PAT = Pattern.compile(
        "([^<]*)<a([^>]*)>([^<]+)</a>", Pattern.CASE_INSENSITIVE);

    private static final Pattern HREF_PAT = Pattern.compile(
        "href=['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);

}
