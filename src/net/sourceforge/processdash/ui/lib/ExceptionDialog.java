// Copyright (C) 2009-2018 Tuma Solutions, LLC
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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;

import net.sourceforge.processdash.util.StringUtils;

public class ExceptionDialog {

    /**
     * Display a JOptionPane with information about a Throwable.
     * 
     * @param parentComponent
     *            the owner of the JOptionPane; can be null
     * @param title
     *            the title to display for the JOptionPane
     * @param contents
     *            the items to display within the JOptionPane. Each item can be
     *            <ul>
     *            <li>A Throwable, which will be displayed in a subdialog as a
     *            text area showing the stack trace.</li>
     * 
     *            <li>A string containing a hyperlink in &lt;a&gt;some
     *            text&lt;/a&gt; format. The embedded text will become a
     *            hyperlink to display a subdialog with information about
     *            the Throwable.</li>
     * 
     *            <li>A regular String or any other object, which will be passed
     *            along to the JOptionPane unchanged.</li>
     * 
     *            <li>A Collection or array of the above items</li>
     *            </ul>
     * 
     * @throws IllegalArgumentException
     *             if the contents parameter did not contain a copy link and at
     *             least one Throwable
     * @since 2.0.1
     */
    public static void showWithSubdialog(Component parentComponent, String title,
            Object... contents) throws IllegalArgumentException {
        show(parentComponent, title, new Object[] { USE_SUBDIALOG, contents });
    }


    /**
     * Display a JOptionPane with information about a Throwable.
     * 
     * @param parentComponent
     *            the owner of the JOptionPane; can be null
     * @param title
     *            the title to display for the JOptionPane
     * @param contents
     *            the items to display within the JOptionPane. Each item can be
     *            <ul>
     *            <li>A Throwable, which will be displayed in the dialog as a
     *            text area showing the stack trace. (Note: if a copy link is
     *            present, the contents array <b>must</b> contain at least one
     *            Throwable. If no copy link is present, the contents array can
     *            contain zero or more Throwables.)</li>
     * 
     *            <li>A string containing a hyperlink in &lt;a&gt;some
     *            text&lt;/a&gt; format. The embedded text will become a
     *            hyperlink to copy the stack trace to the clipboard.</li>
     * 
     *            <li>A regular String or any other object, which will be passed
     *            along to the JOptionPane unchanged.</li>
     * 
     *            <li>A Collection or array of the above items</li>
     *            </ul>
     * 
     * @throws IllegalArgumentException
     *             if the contents parameter contained a copy link but did not
     *             contain any Throwables
     */
    public static void show(Component parentComponent, String title,
            Object... contents) throws IllegalArgumentException {

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(false);
        textArea.setFont(textArea.getFont().deriveFont(
            textArea.getFont().getSize2D() * 0.8f));

        CopyToClipboardHandler copyHandler = new CopyToClipboardHandler(textArea);
        textArea.addFocusListener(copyHandler);
        ShowSubdialogHandler subdialogHandler = null;

        JScrollPane scrollPane = new JScrollPane(textArea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(400, 100));

        List items = new ArrayList();
        boolean sawThrowable = false;
        boolean sawCopyLink = false;
        for (Object o : flattenContents(contents)) {
            if (o instanceof Throwable) {
                if (sawThrowable)
                    textArea.append(DELIMITER);
                else if (subdialogHandler == null)
                    items.add(scrollPane);

                textArea.append(getStackTrace((Throwable) o));
                textArea.setCaretPosition(0);
                sawThrowable = true;

            } else if (o == USE_SUBDIALOG) {
                subdialogHandler = new ShowSubdialogHandler(title, scrollPane);

            } else if (isCopyLink(o)) {
                JLinkLabel errorTraceLabel = new JLinkLabel((String) o);
                errorTraceLabel.addActionListener(subdialogHandler == null //
                        ? copyHandler : subdialogHandler);
                items.add(errorTraceLabel);
                sawCopyLink = true;

            } else if (o != null) {
                items.add(o);
            }
        }

        if (sawCopyLink && !sawThrowable)
            throw new IllegalArgumentException(
                    "A Throwable must be included in the argument list");
        if (subdialogHandler != null && sawThrowable && !sawCopyLink)
            throw new IllegalArgumentException(
                    "A copy link must be included in the argument list");

        JOptionPane.showMessageDialog(parentComponent, items.toArray(), title,
            JOptionPane.ERROR_MESSAGE);
    }

    /** If the given array contains subarrays or collections, replace those
     * entries with the contents of the subarray/subcollection. */
    private static List flattenContents(Object[] contents) {
        List result = new ArrayList();
        for (Object o : contents) {
            if (o instanceof Object[])
                result.addAll(flattenContents((Object[]) o));
            else if (o instanceof Collection)
                result.addAll(flattenContents(((Collection) o).toArray()));
            else
                result.add(o);
        }
        return result;
    }

    private static String getStackTrace(Throwable t) {
        StringWriter buf = new StringWriter();
        PrintWriter pw = new PrintWriter(buf);
        t.printStackTrace(pw);
        pw.flush();
        return StringUtils.findAndReplace(buf.toString(), "\t", "        ");
    }

    private static boolean isCopyLink(Object o) {
        if (o instanceof String)
            return ((String) o).contains("</a>");
        else
            return false;
    }

    private static class ShowSubdialogHandler implements ActionListener {

        private String title;
        private JScrollPane throwableInfo;

        protected ShowSubdialogHandler(String title, JScrollPane sp) {
            this.title = title;
            this.throwableInfo = sp;
        }

        public void actionPerformed(ActionEvent e) {
            ExceptionDialog.show(null, title, throwableInfo);
        }

    }

    private static class CopyToClipboardHandler implements ActionListener, FocusListener {

        private JTextArea textArea;
        private Color bgColor;
        private Timer timer;

        private CopyToClipboardHandler(JTextArea textArea) {
            this.textArea = textArea;
            this.bgColor = textArea.getBackground();
            this.timer = new Timer(100, this);
            this.timer.setRepeats(false);
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == timer) {
                textArea.setBackground(bgColor);
            } else {
                performCopy();
            }
        }

        public void focusGained(FocusEvent e) {
            performCopy();
        }

        public void focusLost(FocusEvent e) {}

        private void performCopy() {
            textArea.setCaretPosition(textArea.getDocument().getLength());
            textArea.moveCaretPosition(0);
            textArea.copy();
            textArea.setBackground(textArea.getSelectionColor());
            timer.start();
        }

    }

    private static final String DELIMITER = "==========================================\n";

    private static final Object USE_SUBDIALOG = new Object();

}
