// Copyright (C) 2001-2022 Tuma Solutions, LLC
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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


/** This simple class can capture the debugging output that was previously
 * sent to <code>System.out</code> and <code>System.err</code>, and display
 * it in a small dialog box instead.
 */
public class ConsoleWindow extends JFrame {

    private static ConsoleWindow INSTALLED_CONSOLE_WINDOW = null;
    JTextArea textArea;
    ConsoleOutputStream outputStream = null;
    PrintStream printStream = null;
    OutputStream copy = null;

    public ConsoleWindow() { this(true); }

    public ConsoleWindow(boolean install) {
        super("Process Dashboard Debug Log");
        JMenuBar menubar = new JMenuBar();
        setJMenuBar(menubar);

        JMenuItem item = new JMenuItem("Copy");
        item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    textArea.selectAll(); textArea.copy(); } });
        menubar.add(item);

        item = new JMenuItem("Clear");
        item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    textArea.setText(null); } });
        menubar.add(item);

        item = new JMenuItem("Close");
        item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setVisible(false); } });
        menubar.add(item);

        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setCaretColor(textArea.getBackground());
        getContentPane().add(new JScrollPane(textArea));
        setSize(new Dimension(700, 350));
        if (install) install();
    }

    public void setCopyOutputStream(OutputStream c) {
        if (copy == null && c != null) {
            // if the copy output stream is changing from null to a value,
            // copy any characters that were missed to the new output stream
            try {
                c.write(textArea.getText().getBytes());
            } catch (Exception e) {}
        }

        copy = c;
    }
    public OutputStream getOutputStream() {
        if (outputStream == null)
            outputStream = new ConsoleOutputStream(System.out);
        return outputStream;
    }
    public PrintStream getPrintStream() {
        if (printStream == null)
            printStream = new PrintStream(getOutputStream(), true);
        return printStream;
    }
    public void install() {
        System.setOut(getPrintStream());
        System.setErr(getPrintStream());
        INSTALLED_CONSOLE_WINDOW = this;
    }
    public static ConsoleWindow getInstalledConsole() {
        return INSTALLED_CONSOLE_WINDOW;
    }
    public static void showInstalledConsole() {
        if (INSTALLED_CONSOLE_WINDOW != null)
            INSTALLED_CONSOLE_WINDOW.setVisible(true);
    }

    // WARNING - doesn't correctly translate bytes to chars.
    private class ConsoleOutputStream extends OutputStream {
        OutputStream orig;
        public ConsoleOutputStream(OutputStream o) { orig = o; }
        public void write(int b) throws IOException {
            orig.write(b);
            if (copy != null) copy.write(b);
            byte[] buf = new byte[1];
            buf[0] = (byte) b;
            textArea.append(new String(buf));
        }
        public void write(byte[] b, int off, int len) throws IOException {
            orig.write(b, off, len);
            if (copy != null) copy.write(b, off, len);
            textArea.append(new String(b, off, len));
        }
    }
}
