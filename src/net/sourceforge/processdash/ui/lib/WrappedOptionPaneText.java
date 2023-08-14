// Copyright (C) 2023 Tuma Solutions, LLC
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

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import net.sourceforge.processdash.util.StringUtils;

public class WrappedOptionPaneText extends JTextArea {

    /**
     * Create a label that will wrap lines in each paragraph in a JOptionPane
     * 
     * When you add one or more of these items to a JOptionPane's message, you
     * must also add one {@link Initializer} to the message as well. That object
     * controls the paragraph wrap width. Failure to add the initializer will
     * result in truncation of the contents of the option pane.
     * 
     * @param text
     *            the text to wrap and display. Doubled newlines (optionally
     *            with space between) represent empty lines to separate
     *            paragraphs; these separations will be retained. Other newlines
     *            will be converted to spaces and the resulting paragraphs will
     *            be wrapped.
     */
    public WrappedOptionPaneText(String text) {
        // configure appearance for option pane display
        setFont(UIManager.getFont("OptionPane.font"));
        setEditable(false);
        setLineWrap(true);
        setWrapStyleWord(true);
        setBackground(null);

        // if the incoming text already contained line breaks, remove them
        // (except for empty lines which are used to separate paragraphs)
        String unwrapped = text.replaceAll("\n\\s*\n", "<parSep>");
        unwrapped = StringUtils.findAndReplace(unwrapped, "\n", " ");
        unwrapped = StringUtils.findAndReplace(unwrapped, "<parSep>", "\n \n");
        setText(unwrapped);
    }


    public static class Initializer extends Box.Filler implements Runnable {

        public Initializer() {
            this(400);
        }

        public Initializer(int width) {
            this(new Dimension(width, 0));
        }

        private Initializer(Dimension d) {
            super(d, d, d);
        }

        @Override
        public void addNotify() {
            super.addNotify();
            SwingUtilities.invokeLater(this);
        }

        @Override
        public void run() {
            SwingUtilities.getWindowAncestor(this).pack();
        }

    }

}
