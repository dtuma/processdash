package teamdash.wbs.columns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import teamdash.TeamMember;
import teamdash.TeamMemberList;
import teamdash.wbs.DataColumn;
import teamdash.wbs.DataTableModel;

public class TeamMemberColumnManager
    implements TeamMemberList.InitialListener, TableModelListener {

    private DataTableModel dataModel;
    private TeamMemberList teamList;
    private List columnList;
    private List columnModels;

    public TeamMemberColumnManager(DataTableModel dataModel,
                                   TeamMemberList teamList)
    {
        this.dataModel = dataModel;
        this.teamList = teamList;
        this.columnList = new ArrayList();
        this.columnModels = new ArrayList();

        createColumns();

        teamList.addInitialListener(this);
        teamList.addTableModelListener(this);
    }

    private void createColumns() {
        List oldColumns = columnList;

        columnList = new ArrayList();
        List teamMembers = teamList.getTeamMembers();
        int teamSize = teamMembers.size();
        for (int i = 0;   i < teamMembers.size();   i++) {
            TeamMember m = (TeamMember) teamMembers.get(i);
            TeamMemberTimeColumn col =
                getExistingValidTimeColumn(oldColumns, m);
            if (col != null)
                oldColumns.remove(col);
            else {
                col = new TeamMemberTimeColumn(dataModel, m);
                dataModel.addDataColumn(col);
            }
            columnList.add(col);
        }
        Iterator i = oldColumns.iterator();
        while (i.hasNext())
            dataModel.removeDataColumn((DataColumn) i.next());
    }

    private TeamMemberTimeColumn getExistingValidTimeColumn
        (List existingColumns, TeamMember m)
    {
        Iterator i = existingColumns.iterator();
        while (i.hasNext()) {
            TeamMemberTimeColumn col = (TeamMemberTimeColumn) i.next();
            if (col.validFor(m)) {
                col.setTeamMember(m);
                return col;
            }
        }
        return null;
    }

    private class TableColumnModelSettings {
        TableColumnModel model;
        TableColumn afterColumn;
        List tableColumns;
        int destPos;
        public TableColumnModelSettings(TableColumnModel model) {
            this.model = model;
            destPos = model.getColumnCount();
            if (destPos == 0)
                afterColumn = null;
            else
                afterColumn = model.getColumn(destPos - 1);
            tableColumns = new ArrayList();
        }
        public void addColumn(TeamMemberTimeColumn column) {
            int columnIndex = dataModel.findColumn(column.getColumnID());
            TableColumn tableColumn = new TableColumn(columnIndex);
            tableColumn.setHeaderValue(column.getColumnName());
            tableColumn.setIdentifier(column.getColumnID());
            model.addColumn(tableColumn);
            int pos = model.getColumnIndex(tableColumn.getIdentifier());
            if (pos != destPos)
                model.moveColumn(pos, destPos);
            destPos++;
            tableColumns.add(tableColumn);
        }
        public void clearColumns() {
            Iterator i = tableColumns.iterator();
            while (i.hasNext())
                model.removeColumn((TableColumn) i.next());
            if (afterColumn == null)
                destPos = 0;
            else
                destPos = model.getColumnIndex(afterColumn.getIdentifier()) + 1;
        }
    }

    public void addToColumnModel(TableColumnModel columnModel) {
        TableColumnModelSettings settings =
            new TableColumnModelSettings(columnModel);
        columnModels.add(settings);
        addToColumnModel(settings);
    }

    private void addToColumnModel(TableColumnModelSettings settings) {
        Iterator i = columnList.iterator();
        while (i.hasNext())
            settings.addColumn((TeamMemberTimeColumn) i.next());
    }

    private HashMap initialsToChange = null;

    public void initialsChanged(String oldInitials, String newInitials) {
        if (initialsToChange == null) initialsToChange = new HashMap();
        initialsToChange.put(oldInitials, newInitials);
    }
    public void tableChanged(TableModelEvent e) {
        // update time data for any team members whose initials have changed.
        if (initialsToChange != null) {
            Iterator i = initialsToChange.keySet().iterator();
            while (i.hasNext()) {
                String oldInitials = (String) i.next();
                TeamMemberTimeColumn.changeInitials
                    (dataModel, oldInitials, oldInitials + " ");
            }

            i = initialsToChange.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry me = (Map.Entry) i.next();
                String oldInitials = (String) me.getKey();
                String newInitials = (String) me.getValue();
                TeamMemberTimeColumn.changeInitials
                    (dataModel, oldInitials + " ", newInitials);
            }

            initialsToChange = null;
        }

        // resynchronize all the column models with the new list of columns.
        createColumns();
        Iterator i = columnModels.iterator();
        while (i.hasNext()) {
            TableColumnModelSettings settings =
                (TableColumnModelSettings) i.next();
            settings.clearColumns();
            addToColumnModel(settings);
        }
    }
}
