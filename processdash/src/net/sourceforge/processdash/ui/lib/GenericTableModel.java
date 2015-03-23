// Copyright (C) 2009 Tuma Solutions, LLC
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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

public class GenericTableModel<Row> extends AbstractTableModel {

    public interface Column<Row, Type> {
        public Class getColumnClass();

        public String getColumnName();

        public Type getValueForRow(Row row);
    }

    public interface EditableColumn<Row, Type> extends Column<Row, Type> {
        public boolean isRowEditable(Row row);

        public void setValueForRow(Row row, Type value);
    }

    public static class GenericColumn<Row, Type> implements
            EditableColumn<Row, Type> {

        private Class clazz;

        private String name;

        private boolean isEditable;

        public GenericColumn(Class<Type> clazz, String name, boolean isEditable) {
            this.clazz = clazz;
            this.name = name;
            this.isEditable = isEditable;
        }

        protected void setType(Class clazz) {
            this.clazz = clazz;
        }

        protected void setName(String name) {
            this.name = name;
        }

        public GenericColumn<Row, Type> setEditable(boolean e) {
            this.isEditable = e;
            return this;
        }

        public Class getColumnClass() {
            return clazz;
        }

        public String getColumnName() {
            return name;
        }

        public Type getValueForRow(Row row) {
            return null;
        }

        public boolean isRowEditable(Row row) {
            return isEditable;
        }

        public void setValueForRow(Row row, Type value) {}

    }

    public static class GenericMappedColumn<Type> extends
            GenericColumn<Map, Type> {

        private Object mapKey;

        public GenericMappedColumn(Class<Type> clazz, String name,
                Object mapKey, boolean isEditable) {
            super(clazz, name, isEditable);
            this.mapKey = mapKey;
        }

        @Override
        public Type getValueForRow(Map row) {
            return (Type) row.get(mapKey);
        }

        @Override
        public void setValueForRow(Map row, Type value) {
            row.put(mapKey, value);
        }

    }

    public static class GenericReflectiveColumn<Row, Type> extends
            GenericColumn<Row, Type> {

        Method readMethod;

        Method writeMethod;

        public GenericReflectiveColumn(Class beanType, String propertyName,
                String colName) {
            super(null, colName, true);
            try {
                PropertyDescriptor prop = new PropertyDescriptor(propertyName,
                        beanType);
                this.readMethod = prop.getReadMethod();
                this.writeMethod = prop.getWriteMethod();
                setType(readMethod.getReturnType());
                setEditable(writeMethod != null);
            } catch (IntrospectionException e) {
                throw new IllegalArgumentException("Invalid bean property", e);
            }
        }

        @Override
        public Type getValueForRow(Row row) {
            try {
                return (Type) readMethod.invoke(row);
            } catch (Exception e) {
                throw new RuntimeException(
                        "could not read bean property for column "
                                + getColumnName(), e);
            }
        }

        @Override
        public void setValueForRow(Row row, Type value) {
            try {
                if (writeMethod != null)
                    writeMethod.invoke(row, value);
            } catch (Exception e) {
                throw new RuntimeException(
                        "could not read bean property for column "
                                + getColumnName(), e);
            }
        }

    }


    public List<Row> rows;

    public List<Column<Row, ?>> columns;

    public GenericTableModel() {
        this(new ArrayList<Column<Row, ?>>());
    }

    public GenericTableModel(Column<Row, ?>... columns) {
        this(Arrays.asList(columns));
    }

    public GenericTableModel(List<Column<Row, ?>> columns) {
        this(new ArrayList<Row>(), columns);
    }

    public GenericTableModel(List<Row> rows, Column<Row, ?>... columns) {
        this(rows, Arrays.asList(columns));
    }

    public GenericTableModel(List<Row> rows, List<Column<Row, ?>> columns) {
        this.rows = rows;
        this.columns = columns;
    }

    public List<Row> getRows() {
        return rows;
    }

    public void setRows(List<Row> rows) {
        if (rows == null)
            throw new NullPointerException("row list cannot be null");
        this.rows = rows;
        fireTableDataChanged();
    }

    public List<Column<Row, ?>> getColumns() {
        return columns;
    }

    public void setColumns(List<Column<Row, ?>> columns) {
        if (columns == null)
            throw new NullPointerException("column list cannot be null");
        this.columns = columns;
        fireTableStructureChanged();
    }


    public int getRowCount() {
        return rows.size();
    }

    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return getColumn(columnIndex).getColumnClass();
    }

    @Override
    public String getColumnName(int columnIndex) {
        return getColumn(columnIndex).getColumnName();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        Column<Row, ?> column = getColumn(columnIndex);
        if (column instanceof EditableColumn) {
            EditableColumn ec = (EditableColumn) column;
            Row row = getRow(rowIndex);
            return (row == null ? false : ec.isRowEditable(row));
        } else {
            return false;
        }
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        Row row = getRow(rowIndex);
        if (row == null)
            return null;
        return getColumn(columnIndex).getValueForRow(row);
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        Row row = getRow(rowIndex);
        if (row == null)
            return;

        Column<Row, ?> column = getColumn(columnIndex);
        if (column instanceof EditableColumn) {
            EditableColumn ec = (EditableColumn) column;
            if (value == null
                    || ec.getColumnClass().isAssignableFrom(value.getClass()))
                if (ec.isRowEditable(row))
                    ec.setValueForRow(row, value);
        }
    }

    protected Row getRow(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rows.size())
            return null;
        else
            return rows.get(rowIndex);
    }

    protected Column<Row, ?> getColumn(int columnIndex) {
        if (columnIndex < 0 || columnIndex >= columns.size())
            return NULL_COLUMN;
        else
            return columns.get(columnIndex);
    }


    private static final GenericColumn NULL_COLUMN = new GenericColumn(
            Object.class, "", false);

}
