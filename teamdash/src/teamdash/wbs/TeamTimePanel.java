package teamdash.wbs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
    /** The point in time when the first person is starting */
    private long teamStartTime;
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
        int row = 0;
        for (int i = 0;   i < teamMembers.size();   i++) {
            // for each team member, create a name label and a horizontal
            // progress bar.
            TeamMember m = (TeamMember) teamMembers.get(i);
            TeamMemberBar bar = new TeamMemberBar(m);
            teamMemberBars.add(bar);

            if (m.getHoursPerWeek().intValue() == 0)
                continue;

            JLabel name = new JLabel(m.getName());
            nc.gridy = row;
            add(name);
            layout.setConstraints(name, nc);

            bc.gridy = row;
            add(bar);
            layout.setConstraints(bar, bc);

            row++;
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
        // find out how much time the team plans to spend per week, and when
        // the overall team is starting work.
        double teamHoursPerWeek = 0;
        teamStartTime = Long.MAX_VALUE;
        for (Iterator i = teamMemberBars.iterator(); i.hasNext();) {
            TeamMemberBar bar = (TeamMemberBar) i.next();
            if (bar.getHoursPerWeek() > 0) {
                teamHoursPerWeek += bar.getHoursPerWeek();
                teamStartTime = Math.min(teamStartTime,
                        bar.getStartTime(Long.MAX_VALUE));
            }
        }

        // check to see if we have NO start dates in our schedule.  If not,
        // revert back to old-style behavior.
        if (teamStartTime == Long.MAX_VALUE)
            teamStartTime = 0;

        // retrieve the total amount of planned time in the work
        // breakdown structure for the entire team.
        NumericDataValue teamTotal =
            (NumericDataValue) dataModel.getValueAt(0, teamColumnNum);
        double totalHours = teamTotal.value;

        // Adjust the team total to include the lag time at the beginning
        // of each team member's schedule.
        for (Iterator i = teamMemberBars.iterator(); i.hasNext();) {
            TeamMemberBar bar = (TeamMemberBar) i.next();
            if (bar.getHoursPerWeek() == 0) {
                totalHours -= bar.getTotalAssignedHours();
            } else {
                double lagTime = bar.getStartTime(teamStartTime) - teamStartTime;
                double lagWeeks = Math.max(lagTime, 0) / MILLIS_PER_WEEK;
                double lagHours = lagWeeks * bar.getHoursPerWeek();
                totalHours += lagHours;
            }
        }

        // calculate the optimal finish time
        balancedWeeks = totalHours / teamHoursPerWeek;
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
    private class TeamMemberBar extends JPanel {

        private TeamMember teamMember;
        private int columnNumber;
        private double lagWeeks;
        private double numWeeks;

        public TeamMemberBar(TeamMember teamMember) {
            this.teamMember = teamMember;
            this.columnNumber = findTimeColumn();

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

        /** Returns the point in time when a team member plans to start */
        public long getStartTime(long unknownValue) {
            Date result = teamMember.getStartDate();
            if (result == null)
                return unknownValue;
            else
                return result.getTime();
        }

        /** Recalculate the schedule duration for this team member, and
         * return the number of weeks in the schedule.
         */
        public double recalc() {
            lagWeeks = numWeeks = 0;
            double time = getTotalAssignedHours();
            double hoursPerWeek = getHoursPerWeek();
            if (time == 0 || hoursPerWeek == 0) {
                String hoursString = NumericDataValue.format(time + 0.049);
                setToolTipText(teamMember.getName()+" - "+ hoursString +
                        " total hours");
                return 0;
            }

            // calculate the number of weeks in this team member's schedule.
            double startTime = getStartTime(teamStartTime);
            lagWeeks = (startTime - teamStartTime) / MILLIS_PER_WEEK;
            lagWeeks = Math.max(lagWeeks, 0);

            double workWeeks = time / hoursPerWeek;
            numWeeks = lagWeeks + workWeeks;

            setToolTipText(teamMember.getName()+" - "+formatWeeks(numWeeks));
            return numWeeks;
        }

        public double getTotalAssignedHours() {
            if (columnNumber == -1) {
                columnNumber = findTimeColumn();
                // if we can't find a column for this team member, return
                // a total time of 0 hours.
                if (columnNumber == -1) return 0;
            }
            // retrieve the total planned time for all tasks assigned to
            // this team member.
            NumericDataValue totalTime =
                (NumericDataValue) dataModel.getValueAt(0, columnNumber);
            if (totalTime != null)
                return totalTime.value;
            else
                return 0;
        }

        /** Look up the time column for this team member. */
        private int findTimeColumn() {
            return dataModel.findColumn(teamMember.getInitials()+"-Time");
        }

        /** Alter the horizontal position of this bar.
         *
         * It should depict the percentage obtained by dividing this team
         * member's schedule by the longest existing schedule.
         */
        public void update() {
            repaint();
        }

        public void paint(Graphics g) {
            // this will paint the background and the insets.
            super.paint(g);

            // now paint the bar.
            double leftPos = lagWeeks / maxNumWeeks;
            double rightPos = numWeeks / maxNumWeeks;
            if (!badDouble(leftPos) && !badDouble(rightPos)) {
                Rectangle bounds = getBounds();
                Insets insets = getInsets();
                int totalWidth = bounds.width - insets.left - insets.right;
                int barHeight = bounds.height - insets.top - insets.bottom;
                int barLeft = (int) (totalWidth * leftPos) + insets.left;
                int barWidth = (int) (totalWidth * (rightPos - leftPos));
                g.setColor(getForeground());
                g.fillRect(barLeft, insets.top, barWidth, barHeight);
            }
        }

        private boolean badDouble(double d) {
            return Double.isInfinite(d) || Double.isNaN(d);
        }


    }

    /** Format a real number of weeks for display.
     */
    private final String formatWeeks(double weeks) {
        if (teamStartTime > 0) {
            long when = teamStartTime + (long) (weeks * MILLIS_PER_WEEK);
            return dateFormat.format(new Date(when));
        } else {
            String num = NumericDataValue.format(weeks + 0.049);
            if ("1".equals(num) || "1.0".equals(num))
                return "1 week";
            else
                return num + " weeks";
        }
    }

    private DateFormat dateFormat = DateFormat.getDateInstance();

    private static final int BALANCED_BAR_WIDTH = 8;
    private static final long MILLIS_PER_WEEK = 7L /*days*/ * 24 /*hours*/
            * 60 /*min*/ * 60 /*sec*/ * 1000 /*millis*/;

}
