package teamdash.wbs.columns;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import teamdash.TeamMember;
import teamdash.TeamMemberList;
import teamdash.wbs.DataTableModel;

public class TeamMemberColumnManager {

    private DataTableModel dataModel;
    private TeamMemberList teamList;
    private List columnList;

    public TeamMemberColumnManager(DataTableModel dataModel,
                                   TeamMemberList teamList)
    {
        this.dataModel = dataModel;
        this.teamList = teamList;
        this.columnList = new ArrayList();

        createColumns();
        // TODO: listen for changes in the team list and update!
    }

    private void createColumns() {
        List teamMembers = teamList.getTeamMembers();
        int teamSize = teamMembers.size();
        for (int i = 0;   i < teamMembers.size();   i++) {
            TeamMember m = (TeamMember) teamMembers.get(i);
            TeamMemberTimeColumn col = new TeamMemberTimeColumn(dataModel, m);
            columnList.add(col);
            dataModel.addDataColumn(col);
        }
    }

    public void addToColumnModel(TableColumnModel columnModel) {
        Iterator i = columnList.iterator();
        while (i.hasNext())
            addToColumnModel(columnModel, (TeamMemberTimeColumn) i.next());
    }

    private void addToColumnModel(TableColumnModel columnModel,
                                  TeamMemberTimeColumn column)
    {
        int columnIndex = dataModel.findColumn(column.getColumnID());
        TableColumn tableColumn = new TableColumn(columnIndex);
        tableColumn.setHeaderValue(column.getColumnName());
        tableColumn.setIdentifier(column.getColumnID());
        columnModel.addColumn(tableColumn);
    }
}
