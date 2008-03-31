package teamdash.wbs.columns;

import java.awt.Color;
import java.util.HashSet;

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

    private MilestonesWBSModel milestones;


    public MilestoneColorColumn(MilestonesWBSModel milestones) {
        this.milestones = milestones;
        this.columnName = this.columnID = COLUMN_ID;
        this.preferredWidth = 65;
    }

    public Class getColumnClass() {
        return Color.class;
    }

    public Object getValueAt(WBSNode node) {
        if (node == milestones.getRoot())
            return Color.white;

        Color result = getColor(node);
        if (result == null) {
            String defaultVal = getFirstUnusedColor();
            Color defaultColor = Color.decode(defaultVal);
            node.setAttribute(VALUE_ATTR, defaultVal);
            node.setAttribute(CACHED_COLOR_ATTR, defaultColor);
            result = defaultColor;
        }
        return result;
    }

    public static Color getColor(WBSNode node) {
        Color result = (Color) node.getAttribute(CACHED_COLOR_ATTR);
        if (result == null) {
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

    private String getFirstUnusedColor() {
        HashSet usedColors = new HashSet();
        for (WBSNode node : milestones.getDescendants(milestones.getRoot()))
            usedColors.add(node.getAttribute(VALUE_ATTR));

        for (int j = 0; j < DEFAULT_COLORS.length; j++) {
            if (!usedColors.contains(DEFAULT_COLORS[j]))
                return DEFAULT_COLORS[j];
        }

         return "#ffffff";
    }
    private static final String[] DEFAULT_COLORS = {
        "#00ffff", // aqua
        "#e2931d", // dark orange
        "#b400b4", // dark magenta
        "#e7e730", // yellow
        "#63d62a", // grass green
        "#ff69b4", // hot pink
        "#989797", // grey
    };

    private static final TableCellEditor CELL_EDITOR = new ColorCellEditor(
            new JButton());

}
