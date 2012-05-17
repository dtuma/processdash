// Copyright (C) 2002-2012 Tuma Solutions, LLC
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
import java.text.DateFormat;
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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;
import teamdash.XMLUtils;
import teamdash.wbs.CustomEditedColumn;
import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.WBSNode;

public class NotesColumn extends AbstractDataColumn implements
        CustomRenderedColumn, CustomEditedColumn {

    /** The ID we use for this column in the data model */
    public static final String COLUMN_ID = "Notes";

    /** The name for this column */
    private static final String COLUMN_NAME = "Notes";

    /** The attribute this column uses to store task notes for a WBS node */
    public static final String VALUE_ATTR = "Notes";

    /** The attribute this column uses to store the time a note was edited */
    public static final String TIMESTAMP_ATTR = "Notes Timestamp";

    /** The attribute this column uses to store the last author of a note */
    public static final String AUTHOR_ATTR = "Notes Author";

    /** The name of the person editing the WBS */
    private String authorName;

    public NotesColumn(String authorName) {
        this.columnID = COLUMN_ID;
        this.columnName = COLUMN_NAME;
        this.preferredWidth = 400;
        this.authorName = authorName;
        setConflictAttributeName(VALUE_ATTR);
    }


    public TableCellRenderer getCellRenderer() {
        return new NotesCellRenderer();
    }

    public TableCellEditor getCellEditor() {
        return new NotesCellEditor();
    }

    public Object getValueAt(WBSNode node) {
        return getTextAt(node);
    }

    public boolean isCellEditable(WBSNode node) {
        return !node.isReadOnly();
    }

    public void setValueAt(Object value, WBSNode node) {
        Object oldValue = getValueAt(node);
        if (!eq(oldValue, value)) {
            node.setAttribute(VALUE_ATTR, value);
            node.setAttribute(AUTHOR_ATTR, authorName);
            node.setAttribute(TIMESTAMP_ATTR, System.currentTimeMillis());
        }
    }

    public static String getTextAt(WBSNode node) {
        return (String) node.getAttribute(VALUE_ATTR);
    }

    public static String getAuthorAt(WBSNode node) {
        Object result = node.getAttribute(AUTHOR_ATTR);
        return (result == null ? null : result.toString());
    }

    public static Date getTimestampAt(WBSNode node) {
        double val = node.getNumericAttribute(TIMESTAMP_ATTR);
        if (Double.isNaN(val))
            return null;
        else
            return new Date((long) val);
    }

    public static String getByLineAt(WBSNode node) {
        Date when = getTimestampAt(node);
        String who = getAuthorAt(node);
        if (when == null || who == null)
            return null;
        else
            return "- Last edited " + DATE_FMT.format(when) + " by " + who;
    }

    public static String getTooltipAt(WBSNode node, boolean includeByline) {
        if (node == null)
            return null;

        String text = getTextAt(node);
        if (text == null || text.trim().length() == 0)
            return null;

        StringBuffer html = new StringBuffer();
        html.append("<html><body><div width='300'>");
        text = HTMLUtils.escapeEntities(text);
        text = StringUtils.findAndReplace(text, "\n", "<br>");
        text = StringUtils.findAndReplace(text, "  ", "&nbsp;&nbsp;");
        html.append(text);
        html.append("</div>");
        String byLine = (includeByline ? getByLineAt(node) : null);
        if (byLine != null) {
            html.append("<hr><div " + BYLINE_CSS + ">");
            html.append(XMLUtils.escapeAttribute(byLine));
            html.append("</div>");
        }
        html.append("</body></html>");
        return html.toString();
    }


    @Override
    public Object getConflictDisplayValue(String text, WBSNode node) {
        if (text == null || text.trim().length() == 0)
            return null;
        if (text.length() > 50)
            text = text.substring(0, 47) + "...";
        return "\"" + text + "\"";
    }


    public static void saveSyncData(WBSNode node, String text, String author,
            Date timestamp) {
        node.setAttribute(VALUE_ATTR, text);
        node.setAttribute(AUTHOR_ATTR, author);
        Long ts = (timestamp == null ? null : new Long(timestamp.getTime()));
        node.setAttribute(TIMESTAMP_ATTR, ts);
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

    private static final DateFormat DATE_FMT = DateFormat.getDateInstance();

    private class NotesCellRenderer extends DefaultTableCellRenderer {

        private String tooltip;

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            this.tooltip = calculateTooltip(table, row);
            String text = asString(value).replace('\n', ' ');
            return super.getTableCellRendererComponent(table, text, isSelected,
                hasFocus, row, column);
        }

        @Override
        public String getToolTipText() {
            return tooltip;
        }

        private String calculateTooltip(JTable table, int row) {
            DataTableModel dtm = (DataTableModel) table.getModel();
            WBSNode node = (WBSNode) dtm.getValueAt(row,
                DataTableModel.WBS_NODE_COLUMN);
            return getTooltipAt(node, true);
        }

    }

    private class NotesCellEditor extends DefaultCellEditor implements
            ActionListener {

        private JButton button;

        private JTextArea textArea;

        private String nodeName;

        private String byLineText;

        private int dialogWidth = 300;

        private int dialogHeight = 200;

        private int dialogX = -1;

        private int dialogY = -1;


        public NotesCellEditor() {
            // we have to supply a bogus, temporary component to make our
            // superclass happy.  We'll change it below.
            super(new JTextField());

            button = new JButton("");
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setBackground(Color.white);
            button.setBorderPainted(false);
            button.setMargin(new Insets(0, 0, 0, 0));
            button.addActionListener(this);

            editorComponent = button;
            setClickCountToStart(2);

            textArea = new JTextArea();
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

            DataTableModel dtm = (DataTableModel) table.getModel();
            WBSNode node = (WBSNode) dtm.getValueAt(row,
                DataTableModel.WBS_NODE_COLUMN);

            nodeName = dtm.getWBSModel().getFullName(node);
            byLineText = getByLineAt(node);

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
            Object message = new Object[] { nodeName, sp, byLine,
                    new FocusTweaker() };
            JOptionPane pane = new JOptionPane(message,
                    JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
            JDialog dialog = pane.createDialog(button.getParent(), "Edit Notes");
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
        }

        private class FocusTweaker extends Component implements ActionListener {

            public void addNotify() {
                super.addNotify();
                Timer t = new Timer(100, this);
                t.setRepeats(false);
                t.start();
            }

            public void actionPerformed(ActionEvent e) {
                textArea.requestFocus();
            }
        }
    }

    private static final String BYLINE_CSS =
        "style='text-align:right; color:#808080; font-style:italic'";

}
