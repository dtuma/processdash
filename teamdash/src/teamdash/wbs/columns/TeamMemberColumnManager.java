package teamdash.wbs.columns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import teamdash.team.TeamMember;
import teamdash.team.TeamMemberList;
import teamdash.wbs.DataColumn;
import teamdash.wbs.DataTableColumn;
import teamdash.wbs.DataTableModel;

/** Creates and manages time columns for each team member.
 * 
 * When the team list changes, this class can recreate the columns and adjust
 * all affected TableColumnModel objects.
 */
public class TeamMemberColumnManager
    implements TeamMemberList.InitialsListener, TableModelListener {

    public interface TeamMemberColumn {
        public boolean validFor(TeamMember t);
        public void setTeamMember(TeamMember t);
    }

    /** The DataTableModel to create columns for */
    private DataTableModel dataModel;
    /** The list of team members */
    private TeamMemberList teamList;
    /** The list of TeamMemberTimeColumn objects for each team member */
    private List<TeamMemberTimeColumn> planTimeColumnList;
    /** The list of TeamMemberActualTimeColumn objects for each team member */
    private List<TeamMemberActualTimeColumn> actualTimeColumnList;
    /** A list of AffectedTableColumnModel objects for each affected
     * TableColumnModel */
    private List affectedColumnModels;



    public TeamMemberColumnManager(DataTableModel dataModel,
                                   TeamMemberList teamList) {
        this.dataModel = dataModel;
        this.teamList = teamList;
        this.planTimeColumnList = new ArrayList<TeamMemberTimeColumn>();
        this.actualTimeColumnList = new ArrayList<TeamMemberActualTimeColumn>();
        this.affectedColumnModels = new ArrayList();

        createColumns();

        teamList.addInitialsListener(this);
        teamList.addTableModelListener(this);
    }



    /** Get a list of the TeamMemberTimeColumn objects for each team member */
    public List<TeamMemberTimeColumn> getPlanTimeColumns() {
        return Collections.unmodifiableList(planTimeColumnList);
    }

    /** Get a list of the TeamMemberActualTimeColumn objects for each team member */
    public List<TeamMemberActualTimeColumn> getActualTimeColumns() {
        return Collections.unmodifiableList(actualTimeColumnList);
    }


    /** Create or recreate TeamMemberTimeColumn objects for each team member
     */
    private void createColumns() {
        List<TeamMemberTimeColumn> obsoletePlanTimeColumns = planTimeColumnList;
        List<TeamMemberActualTimeColumn> obsoleteActualTimeColumns = actualTimeColumnList;
        ArrayList newColumns = new ArrayList();

        planTimeColumnList = new ArrayList<TeamMemberTimeColumn>();
        actualTimeColumnList = new ArrayList<TeamMemberActualTimeColumn>();
        Iterator teamMembers = teamList.getTeamMembers().iterator();
        while (teamMembers.hasNext()) {
            // Loop through the list of team members.
            TeamMember m = (TeamMember) teamMembers.next();

            // try to find a preexisting planned time column for this team member.
            TeamMemberTimeColumn planTimeCol =
                getExistingValidTimeColumn(obsoletePlanTimeColumns, m);

            if (planTimeCol == null) {
                // create a new column for this team member.
                planTimeCol = new TeamMemberTimeColumn(dataModel, m);
                newColumns.add(planTimeCol);
            }
            // add the column to our master list.
            planTimeColumnList.add(planTimeCol);

            // try to find a preexisting actual time column for this team member.
            TeamMemberActualTimeColumn actualTimeCol =
                getExistingValidTimeColumn(obsoleteActualTimeColumns, m);

            if (actualTimeCol == null) {
                // create a new column for this team member.
                actualTimeCol = new TeamMemberActualTimeColumn(m);
                newColumns.add(actualTimeCol);
            }
            // add the column to our master list.
            actualTimeColumnList.add(actualTimeCol);
        }

        // make the changes to the columns in the data model.
        if (!newColumns.isEmpty() || !obsoletePlanTimeColumns.isEmpty()
                || !obsoleteActualTimeColumns.isEmpty()) {
            List obsoleteColumns = new ArrayList();
            obsoleteColumns.addAll(obsoletePlanTimeColumns);
            obsoleteColumns.addAll(obsoleteActualTimeColumns);
            dataModel.addRemoveDataColumns(newColumns, obsoleteColumns);
        }
    }



    /** Look through the given list of columns to see if one can be adapted
     * to work for the given team member.
     * 
     * @return a valid column, or null if none could be found.
     */
    private static <T extends TeamMemberColumn> T getExistingValidTimeColumn(
            List<T> existingColumns, TeamMember m) {
        Iterator<T> i = existingColumns.iterator();
        while (i.hasNext()) {
            T col = i.next();
            if (col.validFor(m)) {
                col.setTeamMember(m);
                i.remove();
                return col;
            }
        }
        return null;
    }



    /** Add columns for each team member plan time to the given TableColumnModel. */
    public void addPlanTimesToColumnModel(TableColumnModel columnModel) {
        addToColumnModel(columnModel, PLAN_TIME_COLS);
    }

    /** Add columns for each team member plan time to the given TableColumnModel. */
    public void addActualTimesToColumnModel(TableColumnModel columnModel) {
        addToColumnModel(columnModel, ACTUAL_TIME_COLS);
    }

    private void addToColumnModel(TableColumnModel columnModel, int colSet) {
        AffectedTableColumnModel settings =
            new AffectedTableColumnModel(columnModel, colSet);
        affectedColumnModels.add(settings);
        settings.addAllColumns();
    }


    /** A list of team member initials that are currently changing */
    private HashMap initialsToChange = null;

    /** Messaged by the TeamMemberList when it has been saved and a team
     * member's initials have changed. */
    public void initialsChanged(String oldInitials, String newInitials) {
        // don't make the change immediately - instead, batch up all of the
        // initials changes and apply them all at once when we receive the
        // subsequent "table data changed" event.
        if (initialsToChange == null)
            initialsToChange = new HashMap();
        initialsToChange.put(oldInitials, newInitials);
    }
    /** Messaged when the TeamMemberList has been saved with changes
     */
    public void tableChanged(TableModelEvent e) {

        // update time data for any team members whose initials have changed.
        // since it is possible for two team members to have swapped initials
        // (or for larger cycles to exist), we perform the changes in 2 steps:
        //   (1) Rename the data from the old initials to something which is
        //       not a legal initials value, and is thus safe to avoid
        //       collisions with real data.
        //   (2) Rename the data from its interim value to the new initials.
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
        Iterator i = affectedColumnModels.iterator();
        while (i.hasNext()) {
            AffectedTableColumnModel settings =
                (AffectedTableColumnModel) i.next();
            settings.clearColumns();
            settings.addAllColumns();
        }
    }

    private static final int PLAN_TIME_COLS = 0;
    private static final int ACTUAL_TIME_COLS = 1;

    /** Maintains information about a TableColumnModel object that contains
     * some of our columns.
     */
    private class AffectedTableColumnModel {

        /** The affected TableColumnModel */
        TableColumnModel model;
        /** The column that our columns should be inserted after */
        TableColumn afterColumn;
        /** Which set of columns we're adding */
        int whichColumnSet;
        /** The list of TableColumn objects we inserted */
        List<TableColumn> tableColumns;
        /** The position where the next TableColumn should be inserted */
        int destPos;

        public AffectedTableColumnModel(TableColumnModel model, int colSet) {
            this.model = model;
            this.whichColumnSet = colSet;
            this.tableColumns = new ArrayList<TableColumn>();

            // insert our columns at the end of this TableColumnModel
            destPos = model.getColumnCount();
            if (destPos == 0)
                afterColumn = null;
            else
                afterColumn = model.getColumn(destPos - 1);
        }

        /** Add all of the TeamMemberTimeColumn objects to this model. */
        public void addAllColumns() {
            for (DataColumn dataColumn : getColumnSet())
                addColumn(dataColumn);
        }

        private List<? extends DataColumn> getColumnSet() {
            switch (whichColumnSet) {
            case ACTUAL_TIME_COLS: return actualTimeColumnList;
            case PLAN_TIME_COLS: default: return planTimeColumnList;
            }
        }

        /** Add a new column to this TableColumnModel */
        private void addColumn(DataColumn column) {
            // create the new column and add it to the TableColumnModel.
            TableColumn tableColumn = new DataTableColumn(dataModel, column);
            model.addColumn(tableColumn);

            // move the column from its current location to destPos.
            int pos = model.getColumnIndex(tableColumn.getIdentifier());
            if (pos != destPos)
                model.moveColumn(pos, destPos);

            // update our internal data structures.
            destPos++;
            tableColumns.add(tableColumn);
        }

        /** Remove all our columns from this TableColumnModel. */
        public void clearColumns() {
            // remove each column
            for (TableColumn column : tableColumns)
                model.removeColumn(column);

            // reset the destPos location
            if (afterColumn == null)
                destPos = 0;
            else
                destPos =
                    model.getColumnIndex(afterColumn.getIdentifier()) + 1;
        }
    }
}
