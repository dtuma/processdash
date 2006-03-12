package net.sourceforge.processdash.util;

import java.io.StringWriter;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import net.sourceforge.processdash.ui.lib.HTMLTableWriter;

import junit.framework.TestCase;

public class HTMLTableWriterTest extends TestCase {

    private TableModel data = new AbstractTableModel() {

        public int getColumnCount() {
            return 3;
        }

        public int getRowCount() {
            return 2;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return rowIndex + "<" + columnIndex;
        }

    };

    public void testDefaultRendering() throws Exception {
        StringWriter out = new StringWriter();
        HTMLTableWriter writer = new HTMLTableWriter();
        writer.writeTable(out, data);
        assertEquals(EXPECTED_DEFAULT_RENDERING, out.toString());
    }

    private static String CRLF = "\r\n";

    private static String HEADER_STYLE = "style='font-weight:bold'";

    private static final String EXPECTED_DEFAULT_RENDERING = "<table>" + CRLF
            + "<tr>" + CRLF + //
            "<th " + HEADER_STYLE + ">A</th>" + CRLF + //
            "<th " + HEADER_STYLE + ">B</th>" + CRLF + //
            "<th " + HEADER_STYLE + ">C</th>" + CRLF + //
            "</tr>" + CRLF + //
            "<tr>" + CRLF + //
            "<td>0&lt;0</td>" + CRLF + //
            "<td>0&lt;1</td>" + CRLF + //
            "<td>0&lt;2</td>" + CRLF + //
            "</tr>" + CRLF + //
            "<tr>" + CRLF + //
            "<td>1&lt;0</td>" + CRLF + //
            "<td>1&lt;1</td>" + CRLF + //
            "<td>1&lt;2</td>" + CRLF + //
            "</tr>" + CRLF + "</table>" + CRLF;


    public void testFancyRendering() throws Exception {
        StringWriter out = new StringWriter();
        HTMLTableWriter writer = new HTMLTableWriter();

        String[] toolTips = new String[] { null, "bb" };
        HTMLTableWriter.DefaultHTMLHeaderCellRenderer headerRenderer = //
            new HTMLTableWriter.DefaultHTMLHeaderCellRenderer(toolTips);
        writer.setHeaderRenderer(headerRenderer);

        HTMLTableWriter.CellRenderer cellRend =
            new HTMLTableWriter.DefaultHTMLTableCellRenderer() {
                public String getAttributes(Object value, int row, int column) {
                    return (((row + column) & 1) == 0 ? null : CLASS_ODD);
                }
            };
        writer.setCellRenderer(cellRend);

        writer.setTableAttributes("border=1");
        writer.setTableName("this&that");
        writer.setWhitespace(HTMLTableWriter.WHITESPACE_ROWS);

        writer.writeTable(out, data);
        assertEquals(EXPECTED_FANCY_RENDERING, out.toString());
    }

    private static final String CLASS_ODD = "class='odd'";
    private static final String EXPECTED_FANCY_RENDERING =
            "<table name='this&amp;that' border=1>" + CRLF +
            "<tr>" + //
            "<th " + HEADER_STYLE + ">A</th>" + //
            "<th " + HEADER_STYLE + " title='bb'>B</th>" + //
            "<th " + HEADER_STYLE + ">C</th>" + //
            "</tr>" + CRLF + //
            "<tr>" + //
            "<td>0&lt;0</td>" + //
            "<td " + CLASS_ODD + ">0&lt;1</td>" + //
            "<td>0&lt;2</td>" + //
            "</tr>" + CRLF + //
            "<tr>" + //
            "<td " + CLASS_ODD + ">1&lt;0</td>" + //
            "<td>1&lt;1</td>" + //
            "<td " + CLASS_ODD + ">1&lt;2</td>" + //
            "</tr>" + CRLF + "</table>" + CRLF;
}
