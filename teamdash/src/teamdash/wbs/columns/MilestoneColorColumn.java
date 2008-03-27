package teamdash.wbs.columns;

import java.awt.Color;

import javax.swing.JButton;
import javax.swing.table.TableCellEditor;

import teamdash.team.ColorCellEditor;
import teamdash.wbs.CustomEditedColumn;
import teamdash.wbs.MilestonesWBSModel;
import teamdash.wbs.WBSNode;

public class MilestoneColorColumn extends AbstractDataColumn implements
        CustomEditedColumn {

    public static final String COLUMN_ID = "Color";

    private static final String VALUE_ATTR = "Color";

    private static final String CACHED_COLOR_ATTR = "Color_Object";


    public MilestoneColorColumn() {
        this.columnName = this.columnID = COLUMN_ID;
        this.preferredWidth = 65;
    }

    public Class getColumnClass() {
        return Color.class;
    }

    public Object getValueAt(WBSNode node) {
        return getColor(node);
    }

    public static Color getColor(WBSNode node) {
        Color result = (Color) node.getAttribute(CACHED_COLOR_ATTR);
        if (result == null) {
            result = Color.white;
            String value = (String) node.getAttribute(VALUE_ATTR);
            if (value != null)
                try {
                    result = Color.decode(value);
                } catch (Exception e) {
                }
            node.setAttribute(CACHED_COLOR_ATTR, result);
        }
        return result;
    }

    public boolean isCellEditable(WBSNode node) {
        return MilestonesWBSModel.MILESTONE_TYPE.equals(node.getType());
    }

    public void setValueAt(Object value, WBSNode node) {
        String storageVal;
        Color colorVal;
        if (value instanceof Color) {
            colorVal = (Color) value;
            storageVal = ColorCellEditor.encodeColor(colorVal);
        } else {
            colorVal = null;
            storageVal = null;
        }
        node.setAttribute(VALUE_ATTR, storageVal);
        node.setAttribute(CACHED_COLOR_ATTR, colorVal);
    }

    public TableCellEditor getCellEditor() {
        return CELL_EDITOR;
    }


    private static final TableCellEditor CELL_EDITOR = new ColorCellEditor(
            new JButton());

}
