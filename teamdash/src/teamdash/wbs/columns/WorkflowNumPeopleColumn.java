package teamdash.wbs.columns;

import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class WorkflowNumPeopleColumn extends AbstractDataColumn {

    private WBSModel wbsModel;

    public WorkflowNumPeopleColumn(WBSModel wbsModel) {
        this.wbsModel = wbsModel;
        this.columnName = this.columnID = "# People";
    }

    public boolean isCellEditable(WBSNode node) {
        return TeamTimeColumn.isLeafTask(wbsModel, node);
    }

    public Object getValueAt(WBSNode node) {
        if (!isCellEditable(node)) return "";

        double value = node.getNumericAttribute(ATTR_NAME);
        int numPeople = 1;
        if (value > 0) numPeople = (int) value;

        if (numPeople == 1)
            return "1 person";
        else
            return numPeople + " people";
    }


    public void setValueAt(Object aValue, WBSNode node) {
        if (aValue == null) return;
        String s = String.valueOf(aValue).trim();

        int pos = s.indexOf(' ');
        if (pos != -1) s = s.substring(0, pos).trim();
        pos = s.indexOf('p');
        if (pos != -1) s = s.substring(0, pos).trim();

        try {
            int numPeople = Integer.parseInt(s);
            node.setNumericAttribute(ATTR_NAME, numPeople);
        } catch (NumberFormatException nfe) { }
    }

    private static final String ATTR_NAME = TeamTimeColumn.NUM_PEOPLE_ATTR;
}
