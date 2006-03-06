package net.sourceforge.processdash.util;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.table.TableModel;

public class HTMLTableWriter {

    public interface CellRenderer {
        public String getInnerHtml(Object value, int row, int column);

        public String getAttributes(Object value, int row, int column);
    }

    private static final String CRLF = "\r\n";

    public static int WHITESPACE_NONE = 0;

    public static int WHITESPACE_ROWS = 1;

    public static int WHITESPACE_CELLS = 2;

    private CellRenderer headerRenderer = DEFAULT_HEADER_RENDERER;

    private CellRenderer cellRenderer = DEFAULT_CELL_RENDERER;

    private String tableName;

    private String tableAttributes;

    private int whitespace = WHITESPACE_CELLS;

    private Set columnsToSkip;

    private Map specialCellRenderers;

    public CellRenderer getCellRenderer() {
        return cellRenderer;
    }

    public void setCellRenderer(CellRenderer cellRenderer) {
        this.cellRenderer = cellRenderer;
    }

    public CellRenderer getCellRenderer(int col) {
        CellRenderer result = null;
        if (specialCellRenderers != null)
            result = (CellRenderer) specialCellRenderers.get(new Integer(col));
        if (result != null)
            return result;
        else
            return getCellRenderer();
    }

    public void setCellRenderer(int col, CellRenderer cellRenderer) {
        if (specialCellRenderers == null)
            specialCellRenderers = new HashMap();
        specialCellRenderers.put(new Integer(col), cellRenderer);
    }

    public CellRenderer getHeaderRenderer() {
        return headerRenderer;
    }

    public void setHeaderRenderer(CellRenderer headerRenderer) {
        this.headerRenderer = headerRenderer;
    }

    public String getTableAttributes() {
        return tableAttributes;
    }

    public void setTableAttributes(String tableAttributes) {
        this.tableAttributes = tableAttributes;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public int getWhitespace() {
        return whitespace;
    }

    public void setWhitespace(int whitespace) {
        this.whitespace = whitespace;
    }

    public void setSkipColumn(int col, boolean skip) {
        if (columnsToSkip == null)
            columnsToSkip = new HashSet();
        if (skip)
            columnsToSkip.add(new Integer(col));
        else
            columnsToSkip.remove(new Integer(col));
    }

    public boolean getSkipColumn(int c) {
        return columnsToSkip != null && columnsToSkip.contains(new Integer(c));
    }

    public void writeTable(Writer out, TableModel t) throws IOException {

        int numCols = t.getColumnCount();
        int numRows = t.getRowCount();

        String rowNewline = (whitespace >= WHITESPACE_ROWS ? CRLF : "");
        String cellNewline = (whitespace >= WHITESPACE_CELLS ? CRLF : "");

        // print the initial table tag.
        out.write("<table");
        printAttr(out, "name", tableName);
        printAttr(out, tableAttributes);
        out.write(">");
        out.write(rowNewline);

        // print the header row for the table.
        out.write("<tr>");
        out.write(cellNewline);
        for (int c = 0; c < numCols; c++) {
            if (getSkipColumn(c) == true)
                continue;

            String columnName = t.getColumnName(c);
            printCell(out, "th", headerRenderer, columnName, -1, c);
            out.write(cellNewline);
        }
        out.write("</tr>");
        out.write(rowNewline);

        // print out each row in the table.
        for (int r = 0; r < numRows; r++) {
            out.write("<tr>");
            out.write(cellNewline);
            for (int c = 0; c < numCols; c++) {
                if (getSkipColumn(c) == true)
                    continue;

                Object value = t.getValueAt(r, c);
                printCell(out, "td", getCellRenderer(c), value, r, c);
                out.write(cellNewline);
            }
            out.write("</tr>");
            out.write(rowNewline);
        }

        out.write("</table>");
        out.write(rowNewline);

    }

    private void printAttr(Writer out, String attr) throws IOException {
        if (attr != null) {
            out.write(" ");
            out.write(attr);
        }
    }

    private void printAttr(Writer out, String attrName, String attrVal)
            throws IOException {
        if (attrVal != null) {
            out.write(" ");
            out.write(attrName);
            out.write("='");
            out.write(HTMLUtils.escapeEntities(attrVal));
            out.write("'");
        }
    }

    private void printCell(Writer out, String tag, CellRenderer renderer,
            Object value, int row, int col) throws IOException {
        out.write("<");
        out.write(tag);
        printAttr(out, renderer.getAttributes(value, row, col));
        out.write(">");
        String text = renderer.getInnerHtml(value, row, col);
        if (text != null)
            out.write(text);
        out.write("</");
        out.write(tag);
        out.write(">");
    }

    public static class DefaultHTMLTableCellRenderer implements CellRenderer {

        public String getInnerHtml(Object value, int row, int column) {
            if (value == null)
                return null;
            else
                return HTMLUtils.escapeEntities(value.toString());
        }

        public String getAttributes(Object value, int row, int column) {
            return null;
        }

    }

    private static final CellRenderer DEFAULT_CELL_RENDERER =
        new DefaultHTMLTableCellRenderer();

    public static class DefaultHTMLHeaderCellRenderer extends
            DefaultHTMLTableCellRenderer {
        private String[] tooltips;

        public DefaultHTMLHeaderCellRenderer() {
            this(null);
        }

        public DefaultHTMLHeaderCellRenderer(String[] tooltips) {
            this.tooltips = tooltips;
        }

        public String getAttributes(Object value, int row, int column) {
            if (tooltips != null && column < tooltips.length
                    && tooltips[column] != null) {
                return "style='font-weight:bold' title='"
                        + HTMLUtils.escapeEntities(tooltips[column]) + "'";
            } else {
                return "style='font-weight:bold'";
            }
        }

    }

    private static final CellRenderer DEFAULT_HEADER_RENDERER =
        new DefaultHTMLHeaderCellRenderer();
}
