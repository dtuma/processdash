// Copyright (C) 2002-2022 Tuma Solutions, LLC
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

package teamdash.wbs.excel;

import java.awt.Color;
import java.awt.Component;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import net.sourceforge.processdash.util.HTMLUtils;

import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSModelValidator;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WrappedValue;

public class WBSExcelWriter {

    private HSSFWorkbook xls;

    private StyleCache styleCache;

    private Set<String> tabNames;

    private short[] decimalFormats;


    public WBSExcelWriter() {
        this.xls = new HSSFWorkbook();
        this.styleCache = new StyleCache(xls);
        this.tabNames = new HashSet();
        HSSFDataFormat df = xls.getCreationHelper().createDataFormat();
        this.decimalFormats = new short[] { df.getFormat("0"),
                df.getFormat("0.0"), df.getFormat("0.00"),
                df.getFormat("0.000") };
    }

    public void save(File f) throws IOException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(f));
        xls.write(out);
        out.close();
    }

    public void addData(String tabName, JTable table) {
        addData(tabName, table, table.getColumnModel());
    }

    public void addData(String tabName, JTable table,
            TableColumnModel columns) {
        addData(tabName, table, filterColumns(table.getModel(), columns,
            Arrays.asList(WBSNode.class, Color.class)));
    }

    public static List<TableColumn> filterColumns(TableModel model,
            TableColumnModel columns, List thingsToSkip) {
        List<TableColumn> result = new ArrayList<TableColumn>();
        for (int i = 0; i < columns.getColumnCount(); i++) {
            TableColumn col = columns.getColumn(i);
            Class clz = model.getColumnClass(col.getModelIndex());
            if (!thingsToSkip.contains(i) && !thingsToSkip.contains(clz))
                result.add(col);
        }
        return result;
    }

    public void addData(String tabName, JTable table,
            List<TableColumn> columns) {
        String safeTabName = cleanupTabName(tabName);
        HSSFSheet sheet = xls.createSheet(safeTabName);

        // WORKAROUND, for POI bug
        // ------ http://issues.apache.org/bugzilla/show_bug.cgi?id=30714
        // We should have been able to use the next line...
        // sheet.setRowSumsBelow(false);
        sheet.setAlternativeExpression(false);

        if (table.getModel() instanceof DataTableModel)
            addWbsTableData(sheet, table, (DataTableModel) table.getModel(),
                columns);
        else
            addTableData(sheet, table, table.getModel(), columns);
    }

    private void addWbsTableData(HSSFSheet sheet, JTable table,
            DataTableModel data, List<TableColumn> columns) {
        WBSModel wbs = data.getWBSModel();
        createHeaderRow(sheet, data, 1, columns);
        writeDataForNodes(sheet, 1, wbs, wbs.getRoot(), data, table, 1, columns);
        autoSizeColumns(sheet, 1 + columns.size());
        sheet.createFreezePane(1, 1);
    }

    private void addTableData(HSSFSheet sheet, JTable table, TableModel data,
            List<TableColumn> columns) {
        createHeaderRow(sheet, data, 0, columns);
        writeDataForTableRows(sheet, data, table, columns);
        autoSizeColumns(sheet, columns.size());
        sheet.createFreezePane(1, 1);
    }

    private String cleanupTabName(String tabName) {
        // Excel forbids certain characters from appearing in the names of
        // worksheets.  If any illegal characters are present, replace them.
        for (int i = ILLEGAL_TAB_CHARS.length();  i-- > 0; )
            tabName = tabName.replace(ILLEGAL_TAB_CHARS.charAt(i),
                REPLACE_TAB_CHARS.charAt(i));

        // ensure the name is not too long - Excel allows 31 characters
        tabName = trim(tabName, 30);

        // ensure tab names are unique and nonempty
        if (tabNames.contains(tabName) || tabName.length() == 0) {
            // trim again if necessary to make space for the numeric suffix
            tabName = trim(tabName, 25);

            // try appending different numbers to the tab name until we
            // find something unique
            int num = 1;
            String uniqueName = "";
            do {
                num++;
                uniqueName = tabName + " (" + num + ")";
            } while (tabNames.contains(uniqueName));
            tabName = uniqueName;
        }

        tabNames.add(tabName);
        return tabName;
    }
    private static final String ILLEGAL_TAB_CHARS = ":?*[]/\\";
    private static final String REPLACE_TAB_CHARS = ";$+{}||";
    private static String trim(String s, int len) {
        s = s.trim();
        if (s.length() > len)
            s = s.substring(0, len).trim();
        return s;
    }

    private void createHeaderRow(HSSFSheet sheet, TableModel data,
            int leftColPad, List<TableColumn> columns) {
        HSSFRow row = sheet.createRow(0);
        StyleKey style = new StyleKey();
        style.bold = true;
        for (int i = 0; i < columns.size(); i++) {
            TableColumn col = columns.get(i);
            String columnName = data.getColumnName(col.getModelIndex());
            HSSFCell cell = row.createCell(s(i + leftColPad));
            cell.setCellValue(new HSSFRichTextString(columnName));
            styleCache.applyStyle(cell, style);
        }
    }

    private int writeDataForNodes(HSSFSheet sheet, int rowNum, WBSModel wbs,
            WBSNode node, DataTableModel data, JTable table, int leftColPad,
            List<TableColumn> columns) {
        HSSFRow row = sheet.createRow(rowNum);
        writeCellForNodeName(node, row);
        writeCellsForRowData(row, node, data, table, leftColPad, columns);

        WBSNode[] children = wbs.getChildren(node);
        if (children.length == 0)
            return rowNum;

        int childRowPos = rowNum;
        for (WBSNode child : children) {
            if (!child.isHidden())
                childRowPos = writeDataForNodes(sheet, childRowPos + 1, wbs,
                    child, data, table, leftColPad, columns);
        }

        sheet.groupRow(rowNum + 1, childRowPos);
        if (!node.isExpanded())
            sheet.setRowGroupCollapsed(rowNum + 1, true);

        return childRowPos;
    }

    private void writeCellForNodeName(WBSNode node, HSSFRow row) {
        HSSFCell cell = row.createCell(s(0));
        String name = node.getName();
        if (name == null || name.trim().length() == 0)
            name = "(empty)";
        cell.setCellValue(new HSSFRichTextString(name));
        StyleKey style = new StyleKey();
        style.indent = s(node.getIndentLevel());
        if (WBSModelValidator.hasNodeError(node)) {
            style.setColor(Color.RED);
            style.bold = true;
        }
        styleCache.applyStyle(cell, style);
    }

    private void writeDataForTableRows(HSSFSheet sheet, TableModel data,
            JTable table, List<TableColumn> columns) {
        for (int rowNum = 0; rowNum < data.getRowCount(); rowNum++) {
            HSSFRow row = sheet.createRow(rowNum + 1);
            writeCellsForRowData(row, rowNum, data, table, 0, columns);
        }
    }

    private void writeCellsForRowData(HSSFRow row, Object target,
            TableModel data, JTable table, int leftColPad,
            List<TableColumn> columns) {
        for (int c = 0; c < columns.size(); c++) {
            TableColumn col = columns.get(c);
            int columnIndex = col.getModelIndex(), rowIndex;
            Object value;
            if (data instanceof DataTableModel) {
                rowIndex = 99;
                value = ((DataTableModel) data).getValueAt((WBSNode) target,
                    columnIndex);
            } else {
                rowIndex = (Integer) target;
                value = data.getValueAt(rowIndex, columnIndex);
            }

            TableCellRenderer rend = col.getCellRenderer();
            if (rend == null)
                rend = table.getDefaultRenderer(data
                        .getColumnClass(columnIndex));

            Component comp = rend.getTableCellRendererComponent(table, value,
                false, false, rowIndex, columnIndex);

            HSSFCell cell = row.createCell(s(c + leftColPad));
            copyCellData(cell, rend, comp, value);
        }

    }

    private void copyCellData(HSSFCell cell, TableCellRenderer rend,
            Component comp, Object value) {
        StyleKey style = new StyleKey();

        String text = null;
        if (comp instanceof JLabel) {
            JLabel label = (JLabel) comp;
            text = label.getText();
            text = stripHtml(text);
        }

        if (value instanceof ExcelExportableValue)
            value = ((ExcelExportableValue) value).getValueForExcelExport();
        if (rend instanceof ExcelValueExporter)
            value = ((ExcelValueExporter) rend).getValueForExcelExport(value);
        Object unwrapped = WrappedValue.unwrap(value);

        if (unwrapped instanceof Date) {
            Date date = (Date) unwrapped;
            cell.setCellValue(date);
            style.format = DATE_FORMAT;

        } else if (unwrapped instanceof NumericDataValue) {
            NumericDataValue ndv = (NumericDataValue) unwrapped;
            cell.setCellValue(ndv.value);
            if (text == null || text.trim().length() == 0) {
                style.setColor(Color.WHITE);
            } else if (text.indexOf('%') != -1) {
                style.format = PERCENT_FORMAT;
            } else {
                setDecimalFormat(style, text);
            }

        } else if (unwrapped instanceof Boolean) {
            if ((Boolean) unwrapped)
                cell.setCellValue(new HSSFRichTextString("X"));

        } else if (text == null || text.trim().length() == 0) {
            return;

        } else {
            cell.setCellValue(new HSSFRichTextString(text));
        }

        style.loadFrom(comp);
        styleCache.applyStyle(cell, style);
    }

    private String stripHtml(String str) {
        if (str == null || !str.startsWith("<html"))
            return str;

        while (true) {
            int beg = str.indexOf('<');
            if (beg == -1)
                break;
            int end = str.indexOf('>', beg+1);
            if (end == -1)
                break;
            str = str.substring(0, beg) + str.substring(end+1);
        }
        return HTMLUtils.unescapeEntities(str);
    }

    private void setDecimalFormat(StyleKey style, String numText) {
        // instruct Excel to use the same number of digits we used
        int fractionLen = getNumFractionDigits(numText);
        fractionLen = Math.max(fractionLen, 0);
        fractionLen = Math.min(fractionLen, decimalFormats.length - 1);
        style.format = decimalFormats[fractionLen];
    }

    private int getNumFractionDigits(String numText) {
        // look for the decimal point in this number
        int pos = numText.lastIndexOf(DECIMAL_POINT);

        // for integers with no fraction, return 0
        if (pos == -1)
            return 0;

        // return the number of digits that appear after the decimal
        int fractionLen = numText.length() - pos - 1;
        return fractionLen;
    }

    private void autoSizeColumns(HSSFSheet sheet, int numColumns) {
        for (int i = 0; i < numColumns; i++)
            sheet.autoSizeColumn(s(i));
    }


    private static short s(int i) {
        return (short) i;
    }

    private static final short PERCENT_FORMAT = HSSFDataFormat
            .getBuiltinFormat("0%");

    private static final char DECIMAL_POINT = NumericDataValue.format(1.5)
            .charAt(1);

    private static final short DATE_FORMAT =  HSSFDataFormat
            .getBuiltinFormat("m/d/yy");

}
