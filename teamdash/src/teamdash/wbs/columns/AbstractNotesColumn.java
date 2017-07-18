// Copyright (C) 2002-2017 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.wbs.columns;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Date;

import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

import teamdash.wbs.CustomEditedColumn;
import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.WBSNode;

public abstract class AbstractNotesColumn extends AbstractDataColumn implements
        CustomRenderedColumn, CustomEditedColumn {

    /** The attribute suffix used to store the time a note was edited */
    protected static final String TIMESTAMP_SUFFIX = " Timestamp";

    /** The attribute suffix used to store the last author of a note */
    protected static final String AUTHOR_SUFFIX = " Author";

    /** The attribute this column uses to store notes for a WBS node */
    private String valueAttr;

    /** The name of the person editing the WBS */
    private String authorName;


    protected AbstractNotesColumn(String valueAttr, String authorName) {
        this.preferredWidth = 400;
        this.valueAttr = valueAttr;
        this.authorName = authorName;
        setConflictAttributeName(valueAttr);
    }


    public TableCellRenderer getCellRenderer() {
        return new NotesCellRenderer();
    }

    public TableCellEditor getCellEditor() {
        return new NotesCellEditor(getEditDialogTitle());
    }

    protected abstract String getEditDialogTitle();

    protected Object getEditDialogHeader(WBSNode node) {
        return null;
    }

    public Object getValueAt(WBSNode node) {
        return getTextAt(node, valueAttr);
    }

    public boolean isCellEditable(WBSNode node) {
        return !node.isReadOnly();
    }

    public void setValueAt(Object value, WBSNode node) {
        Object oldValue = getValueAt(node);
        if (!eq(oldValue, value)) {
            node.setAttribute(valueAttr, scrub(value));
            node.setAttribute(valueAttr + AUTHOR_SUFFIX, authorName);
            node.setAttribute(valueAttr + TIMESTAMP_SUFFIX,
                System.currentTimeMillis());
        }
    }

    private String scrub(Object value) {
        if (value == null)
            return null;

        String text = value.toString();
        StringBuilder buf = null;
        for (int i = text.length(); i-- > 0;) {
            char c = text.charAt(i);
            if (c < ' ' && c != '\t' && c != '\r' && c != '\n') {
                if (buf == null)
                    buf = new StringBuilder(text);
                buf.setCharAt(i, ' ');
            }
        }
        return buf == null ? text : buf.toString();
    }

    protected static String getTextAt(WBSNode node, String valueAttr) {
        return (String) node.getAttribute(valueAttr);
    }

    protected static String getAuthorAt(WBSNode node, String valueAttr) {
        Object result = node.getAttribute(valueAttr + AUTHOR_SUFFIX);
        return (result == null ? null : result.toString());
    }

    protected static Date getTimestampAt(WBSNode node, String valueAttr) {
        double val = node.getNumericAttribute(valueAttr + TIMESTAMP_SUFFIX);
        if (Double.isNaN(val))
            return null;
        else
            return new Date((long) val);
    }

    protected static String getByLineAt(WBSNode node, String valueAttr) {
        Date when = getTimestampAt(node, valueAttr);
        String who = getAuthorAt(node, valueAttr);
        if (when == null || who == null)
            return null;
        else
            return resources.format("Notes.Byline_FMT", when, who);
    }

    protected static String getTooltipAt(WBSNode node, boolean includeByline,
            String valueAttr) {
        if (node == null)
            return null;

        String text = getTextAt(node, valueAttr);
        if (text == null || text.trim().length() == 0)
            return null;

        StringBuffer html = new StringBuffer();
        html.append("<html><body><div width='300'>");
        text = HTMLUtils.escapeEntities(text);
        text = StringUtils.findAndReplace(text, "\n", "<br>");
        text = StringUtils.findAndReplace(text, "  ", "&nbsp;&nbsp;");
        html.append(text);
        html.append("</div>");
        String byLine = (includeByline ? getByLineAt(node, valueAttr) : null);
        if (byLine != null) {
            html.append("<hr><div " + BYLINE_CSS + ">");
            html.append(HTMLUtils.escapeEntities(byLine));
            html.append("</div>");
        }
        html.append("</body></html>");
        return html.toString();
    }

    public static String[] getMetadataAttrs(String valueAttr) {
        return new String[] { valueAttr + AUTHOR_SUFFIX,
                valueAttr + TIMESTAMP_SUFFIX };
    }


    @Override
    public Object getConflictDisplayValue(String text, WBSNode node) {
        if (text == null || text.trim().length() == 0)
            return null;
        if (text.length() > 50)
            text = text.substring(0, 47) + "...";
        return "\"" + text + "\"";
    }


    private static String asString(Object o) {
        return (o == null ? "" : o.toString());
    }

    private static boolean eq(Object a, Object b) {
        if (a == b)
            return true;
        if (a == null)
            return false;
        return a.equals(b);
    }

    private class NotesCellRenderer extends DefaultTableCellRenderer {

        private WBSNode node;

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            DataTableModel dtm = (DataTableModel) table.getModel();
            node = (WBSNode) dtm.getValueAt(row, DataTableModel.WBS_NODE_COLUMN);

            String text = asString(value).replace('\n', ' ');
            return super.getTableCellRendererComponent(table, text, isSelected,
                hasFocus, row, column);
        }

        @Override
        public String getToolTipText() {
            return getTooltipAt(node, true, valueAttr);
        }

    }

    private class NotesCellEditor extends DefaultCellEditor implements
            ActionListener {

        private JTable parentTable;

        private String dialogTitle;

        private JButton button;

        private JTextArea textArea;

        private Object dialogHeader;

        private String nodeName;

        private String byLineText;

        private int dialogWidth = 300;

        private int dialogHeight = 300;

        private int dialogX = -1;

        private int dialogY = -1;


        public NotesCellEditor(String dialogTitle) {
            // we have to supply a bogus, temporary component to make our
            // superclass happy.  We'll change it below.
            super(new JTextField());
            this.dialogTitle = dialogTitle;

            button = new JButton("");
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setBackground(Color.white);
            button.setBorderPainted(false);
            button.setMargin(new Insets(0, 0, 0, 0));
            button.addActionListener(this);
            button.addKeyListener(new KeyAdapter() {
                public void keyReleased(KeyEvent e) {
                    button.doClick();
                }});

            editorComponent = button;
            setClickCountToStart(2);

            textArea = new JTextArea();
            textArea.setFont(UIManager.getFont("Table.font"));
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);

            delegate = new EditorDelegate() {
                public void setValue(Object value) {
                    textArea.setText(asString(value));
                }

                public Object getCellEditorValue() {
                    String text = textArea.getText();
                    if (text != null) text = text.trim();
                    return text;
                }
            };

        }

        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int column) {
            super.getTableCellEditorComponent(table, value, isSelected, row,
                column);
            parentTable = table;

            DataTableModel dtm = (DataTableModel) table.getModel();
            WBSNode node = (WBSNode) dtm.getValueAt(row,
                DataTableModel.WBS_NODE_COLUMN);

            dialogHeader = getEditDialogHeader(node);
            nodeName = dtm.getWBSModel().getFullName(node);
            byLineText = getByLineAt(node, valueAttr);

            button.setText(asString(value).replace('\n', ' '));
            button.setFont(table.getFont());
            return button;
        }

        public void actionPerformed(ActionEvent e) {
            JScrollPane sp = new JScrollPane(textArea,
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            Box byLine = null;
            if (byLineText != null) {
                JLabel l = new JLabel(byLineText);
                l.setForeground(Color.GRAY);
                Font f = l.getFont();
                f = f.deriveFont(Font.ITALIC);
                f = f.deriveFont(f.getSize() * 0.8f);
                l.setFont(f);
                byLine = Box.createHorizontalBox();
                byLine.add(Box.createHorizontalGlue());
                byLine.add(l);
            }
            Object message = new Object[] { dialogHeader, nodeName, sp, byLine,
                    new JOptionPaneTweaker.GrabFocus(textArea) };
            JOptionPane pane = new JOptionPane(message,
                    JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
            JDialog dialog = pane.createDialog(button.getParent(), dialogTitle);
            dialog.setResizable(true);
            dialog.setSize(dialogWidth, dialogHeight);
            if (dialogX != -1)
                dialog.setLocation(dialogX, dialogY);
            else
                dialog.setLocationRelativeTo(SwingUtilities
                        .getWindowAncestor(button));

            dialog.setVisible(true);

            dialogWidth = dialog.getWidth();
            dialogHeight = dialog.getHeight();
            dialogX = dialog.getX();
            dialogY = dialog.getY();

            if (new Integer(JOptionPane.OK_OPTION).equals(pane.getValue()))
                stopCellEditing();
            else
                cancelCellEditing();

            Timer t = new Timer(100, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    parentTable.grabFocus();
                }});
            t.setRepeats(false);
            t.start();
        }
    }

    private static final String BYLINE_CSS =
        "style='text-align:right; color:#808080; font-style:italic'";

}
