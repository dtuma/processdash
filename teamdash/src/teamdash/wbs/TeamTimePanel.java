package teamdash.wbs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.BevelBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import teamdash.TeamMember;
import teamdash.TeamMemberList;

/** Displays a panel containing dynamic bar charts for each team member. The
 * bars indicate the approximate bottom-up duration of the schedule for each
 * team member.  A separate indicator shows the approximate duration of the
 * balanced team schedule.
 */
public class TeamTimePanel extends JPanel implements TableModelListener {

    /** The list of team members on this project. */
    private TeamMemberList teamList;
    /** The data model containing time data. */
    private DataTableModel dataModel;
    /** The layout object managing this panel */
    private GridBagLayout layout;
    /** A list of the bar charts for each individual (each is a
     * TeamMemberBar object). */
    private List teamMemberBars;
    /** The index in the data model of the "team time" column */
    private int teamColumnNum;
    /** The number of weeks in the longest schedule */
    private double maxNumWeeks;
    /** The number of weeks in the balanced team schedule */
    private double balancedWeeks = Double.NaN;
    /** The indicator for the balanced team duration */
    private JPanel balancedBar;

    /** Create a team time panel.
     * @param teamList the list of team members to display.
     * @param dataModel the data model containing time data.
     */
    public TeamTimePanel(TeamMemberList teamList, DataTableModel dataModel) {
        this.teamList = teamList;
        this.dataModel = dataModel;
        this.teamMemberBars = new ArrayList();
        this.teamColumnNum = dataModel.findColumn("Time");

        setLayout(layout = new GridBagLayout());
        rebuildPanelContents();
        recalc();

        dataModel.addTableModelListener(this);
        teamList.addTableModelListener(this);
    }

    private void rebuildPanelContents() {
        removeAll();  // remove all components from this container.
        teamMemberBars.clear();

        // create the indicator for the balanced team duration. It is
        // added to the container first, so it will display on top of
        // other components.
        balancedBar = new JPanel();
        balancedBar.setBorder
            (BorderFactory.createBevelBorder(BevelBorder.RAISED));
        balancedBar.setBackground(Color.darkGray);
        add(balancedBar);
        // give the balanced bar a max/min/pref size of 0,0 so it will
        // not influence panel layout.
        Dimension d = new Dimension(0, 0);
        balancedBar.setMaximumSize(d);
        balancedBar.setMinimumSize(d);
        balancedBar.setPreferredSize(d);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 2; c.gridy = 0;
        layout.setConstraints(balancedBar, c);

        List teamMembers = teamList.getTeamMembers();
        // create a constraints object for the name labels.
        GridBagConstraints nc = new GridBagConstraints();
        nc.gridx = 0;
        nc.anchor = GridBagConstraints.WEST;
        nc.insets.left = nc.insets.right = 5;
        nc.insets.top = nc.insets.bottom = 0;
        // create a constraints object for the horizontal bars.
        GridBagConstraints bc = new GridBagConstraints();
        bc.gridx = 1;
        bc.fill = GridBagConstraints.BOTH;
        bc.insets.left = bc.insets.right = 5;
        bc.insets.top = bc.insets.bottom = 0;
        bc.weightx = bc.weighty = 1;
        for (int i = 0;   i < teamMembers.size();   i++) {
            // for each team member, create a name label and a horizontal
            // progress bar.
            TeamMember m = (TeamMember) teamMembers.get(i);

            JLabel name = new JLabel(m.getName());
            nc.gridy = i;
            add(name);
            layout.setConstraints(name, nc);

            TeamMemberBar bar = new TeamMemberBar(m);
            teamMemberBars.add(bar);
            bc.gridy = i;
            add(bar);
            layout.setConstraints(bar, bc);

            nc.insets.top = bc.insets.top = 0;
        }
    }

    public void doLayout() {
        super.doLayout();

        // we override doLayout so we can set the position of the
        // balanced bar.  The call to super.doLayout() will position
        // it somewhere meaningless and allocate it no space.  We
        // resize it to be as high as this panel, and reposition it to
        // properly indicate the calculated team duration.
        if (balancedWeeks <= maxNumWeeks && teamMemberBars.size() > 0) {
            Rectangle r = ((TeamMemberBar) teamMemberBars.get(0)).getBounds();
            int pos = r.x + (int) (r.width * balancedWeeks / maxNumWeeks);
            balancedBar.setBounds(pos - BALANCED_BAR_WIDTH/2, 1,
                                  BALANCED_BAR_WIDTH, getHeight());
        }
    }



    public void recalc() {
        recalcTeam();
        recalcIndividuals();
        revalidate();
    }


    /** Recalculate the duration of a balanced team schedule.
     */
    protected void recalcTeam() {
        // find out how much time the team plans to spend per week.
        double teamHoursPerWeek = 0;
        Iterator i = teamMemberBars.iterator();
        while (i.hasNext())
            teamHoursPerWeek += ((TeamMemberBar) i.next()).getHoursPerWeek();

        // retrieve the total amount of planned time in the work
        // breakdown structure for the entire team.
        NumericDataValue teamTotal =
            (NumericDataValue) dataModel.getValueAt(0, teamColumnNum);

        balancedWeeks = teamTotal.value / teamHoursPerWeek;
        balancedBar.setToolTipText("Balanced Team Duration - " +
                                   formatWeeks(balancedWeeks));
        if (Double.isInfinite(balancedWeeks) || Double.isNaN(balancedWeeks))
            maxNumWeeks = 0;
        else
            maxNumWeeks = balancedWeeks;
    }


    /** Recalculate the horizontal bars for each team member.
     */
    protected void recalcIndividuals() {
        // first, recalculate each team member's schedule, and keep track
        // of the longest duration we've seen so far.
        Iterator i = teamMemberBars.iterator();
        while (i.hasNext())
            maxNumWeeks =
                Math.max(maxNumWeeks, ((TeamMemberBar) i.next()).recalc());

        // Now, go back and adjust the bar for each individual based upon
        // their schedule duration and the longest duration
        i = teamMemberBars.iterator();
        while (i.hasNext())
            ((TeamMemberBar) i.next()).update();
    }


    /** Listen for and respond to changes in the data or the team list.
     */
    public void tableChanged(TableModelEvent e) {
        if (e.getSource() == teamList)
            // if the list of team members changed, we need to discard
            // and rebuild the contents of this panel from scratch.
            rebuildPanelContents();

        // whenever data changes, recalculate and redisplay.
        recalc();
        repaint();
    }


    /** This class performs the calculations and the display of a
     * horizontal bar for a single team member.
     */
    private class TeamMemberBar extends JProgressBar {

        private TeamMember teamMember;
        private int columnNumber;
        private double numWeeks;

        public TeamMemberBar(TeamMember teamMember) {
            // JProgressBar does exactly what we need, but uses integers to
            // express min, max, and current values of the horizontal bar.
            // We want to display fractional numbers, so we must normalize
            // those numbers into a large integer value (NORMALIZED_MAX).
            super(0, NORMALIZED_MAX);
            this.teamMember = teamMember;
            this.columnNumber =
                dataModel.findColumn(teamMember.getInitials()+"-Time");

            setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            // use the color associated with the given team member.
            setForeground(teamMember.getColor());
        }

        /** Returns the estimated number of hours this team member plans to
         * spend per week.
         */
        public double getHoursPerWeek() {
            return teamMember.getHoursPerWeek().doubleValue();
        }

        /** Recalculate the schedule duration for this team member, and
         * return the number of weeks in the schedule.
         */
        public double recalc() {
            if (columnNumber == -1) {
                // look up the time column for this team member.
                columnNumber = dataModel.findColumn
                    (teamMember.getInitials()+"-Time");
                // if we can't find a column for this team member, return
                // a duration of 0 weeks.
                if (columnNumber == -1) return 0;
            }
            // retrieve the total planned time for all tasks assigned to
            // this team member.
            NumericDataValue totalTime =
                (NumericDataValue) dataModel.getValueAt(0, columnNumber);
            double time = 0;
            if (totalTime != null) time = totalTime.value;
            // calculate the number of weeks in this team member's schedule.
            numWeeks = time / getHoursPerWeek();
            setToolTipText(teamMember.getName()+" - "+formatWeeks(numWeeks));
            return numWeeks;
        }

        /** Alter the horizontal position of this bar.
         *
         * It should depict the percentage obtained by dividing this team
         * member's schedule by the longest existing schedule.
         */
        public void update() {
            if (maxNumWeeks == 0)
                setValue(0);
            else
                setValue((int) (NORMALIZED_MAX * numWeeks / maxNumWeeks));
        }
    }

    /** Format a real number of weeks for display.
     */
    private static final String formatWeeks(double weeks) {
        String num = NumericDataValue.format(weeks + 0.049);
        if ("1".equals(num) || "1.0".equals(num))
            return "1 week";
        else
            return num + " weeks";
    }

    private static final int NORMALIZED_MAX = 5000;
    private static final int BALANCED_BAR_WIDTH = 8;

}
