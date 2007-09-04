package teamdash.wbs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
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
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import teamdash.team.TeamMember;
import teamdash.team.TeamMemberList;
import teamdash.wbs.columns.TeamMemberTimeColumn;

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
    /** The number of milliseconds between the team start date and the
     * latest finish date */
    private double maxScheduleLength;
    /** The number of milliseconds between the team start date and the
     * balanced completion date.  If no balanced date can be computed, -1 */
    private long balancedLength = -1;
    /** The indicator for the balanced team duration */
    private JPanel balancedBar;
    /** Should the balanced bar be shown, or hidden */
    private boolean showBalancedBar;
    /** The position of the balanced bar, in pixels from the left edge of the
     * area where colored bars are drawn */
    private int balancedBarPos;
    /** The font to use when drawing labels on colored bars */
    private Font labelFont;
    /** The color to use for depicting overtasked time in a colored bar */
    private Color overtaskedColor = Color.red;

    /** Create a team time panel.
     * @param teamList the list of team members to display.
     * @param dataModel the data model containing time data.
     */
    public TeamTimePanel(TeamMemberList teamList, DataTableModel dataModel) {
        this.teamList = teamList;
        this.dataModel = dataModel;
        this.teamMemberBars = new ArrayList();
        this.teamColumnNum = dataModel.findColumn("Time");
        this.showBalancedBar = true;

        setLayout(layout = new GridBagLayout());
        rebuildPanelContents();
        recalc();

        dataModel.addTableModelListener(this);
        teamList.addTableModelListener(this);
    }

    public boolean isShowBalancedBar() {
        return showBalancedBar;
    }

    public void setShowBalancedBar(boolean showBalancedBar) {
        this.showBalancedBar = showBalancedBar;
    }

    private void rebuildPanelContents() {
        removeAll();  // remove all components from this container.
        teamMemberBars.clear();
        labelFont = null;

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
        if (showBalancedBar
                && maxScheduleLength > 0
                && balancedLength <= maxScheduleLength
                && teamMemberBars.size() > 0) {
            Rectangle r = ((TeamMemberBar) teamMemberBars.get(0)).getBounds();
            balancedBarPos = (int) (r.width * balancedLength / maxScheduleLength);
            int pos = r.x + balancedBarPos;
            balancedBar.setBounds(pos - BALANCED_BAR_WIDTH/2, 1,
                                  BALANCED_BAR_WIDTH, getHeight());
        } else {
            balancedBarPos = -100;
        }
    }



    public void recalc() {
        recalcTeam();
        recalcIndividuals();
        revalidate();
        repaintIndividuals();
    }


    /** Recalculate the duration of a balanced team schedule.
     */
    protected void recalcTeam() {
        // find out how when the overall team is starting work.
        Date teamStartDate = teamList.getDateForEffort(0);
        teamStartTime = teamStartDate.getTime();

        // retrieve the total amount of planned time in the work
        // breakdown structure for the entire team.
        NumericDataValue teamTotal =
            (NumericDataValue) dataModel.getValueAt(0, teamColumnNum);
        double totalHours = teamTotal.value;

        // calculate the optimal finish time
        Date balancedDate = teamList.getDateForEffort(totalHours);
        if (balancedDate == null) {
            balancedLength = -1;
            maxScheduleLength = 0;
        } else {
            balancedLength = balancedDate.getTime() - teamStartTime;
            maxScheduleLength = balancedLength;
            balancedBar.setToolTipText("Balanced Team Duration - " +
                dateFormat.format(balancedDate));
        }
    }


    /** Recalculate the horizontal bars for each team member.
     */
    protected void recalcIndividuals() {
        // first, recalculate each team member's schedule, and keep track
        // of the longest duration we've seen so far.
        Iterator i = teamMemberBars.iterator();
        while (i.hasNext())
            maxScheduleLength = Math.max(maxScheduleLength,
                ((TeamMemberBar) i.next()).recalc());
    }

    /** Repaint the horizontal bars for each team member.
     */
    protected void repaintIndividuals() {
        // Now, go back and adjust the bar for each individual based upon
        // their schedule duration and the longest duration
        Iterator i = teamMemberBars.iterator();
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

        /** The TeamMember we are displaying data for */
        private TeamMember teamMember;

        /** The column in the data model holding time for our team member */
        private int columnNumber;

        /** True if our colored bar has a dark color */
        private boolean barIsDark;

        /** True if we should paint the label with a light color */
        private boolean labelIsLight;

        /** Millis between the team start and the start date for this person */
        private long lagTime;

        /** Millis between the team start and the finish date for this person */
        private long finishTime;

        /** Millis between the team start and the date this person is leaving
         * the project. (-1 if they aren't leaving the project) */
        private long endTime;

        /** The label to display on the bar */
        private String label;


        public TeamMemberBar(TeamMember teamMember) {
            this.teamMember = teamMember;
            this.columnNumber = findTimeColumn();

            setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            // use the color associated with the given team member.
            setForeground(teamMember.getColor());

            int rgb = teamMember.getColor().getRGB();
            int gray = (int) (0.30 * ((rgb >> 16) & 0xff) +
                    0.59 * ((rgb >> 8) & 0xff) +
                    0.11 * (rgb & 0xff));
            barIsDark = (gray < 128);
        }

        /**
         * Recalculate the schedule duration for this team member, and return
         * the number of milliseconds in their schedule (including lag time at
         * the beginning of the schedule)
         */
        public long recalc() {
            Date startDate = teamMember.getStartDate();
            lagTime = Math.max(0, startDate.getTime() - teamStartTime);

            Date endDate = teamMember.getEndDate();
            if (endDate == null)
                endTime = -1;
            else
                endTime = endDate.getTime() - teamStartTime;

            double hours = getTotalAssignedHours();
            Date finishDate = teamMember.getSchedule().getDateForEffort(hours);

            if (finishDate == null) {
                finishTime = -1;
                String hoursString = NumericDataValue.format(hours + 0.049);
                setLabel(hoursString + " total hours");
                return 0;
            } else {
                finishTime = finishDate.getTime() - teamStartTime;
                String dateString = dateFormat.format(finishDate);
                if (endTime > 0 && finishTime > endTime)
                    dateString = dateString + " - OVERTASKED";
                setLabel(dateString);
                return finishTime;
            }
        }

        private void setLabel(String message) {
            this.label = message;
            setToolTipText(teamMember.getName() + " - " + message);
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
            String columnID = TeamMemberTimeColumn.getColumnID(teamMember);
            return dataModel.findColumn(columnID);
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

            if (finishTime > 0 && maxScheduleLength > 0) {
                // now paint the bar.
                double leftPos = lagTime / maxScheduleLength;
                double rightPos = finishTime / maxScheduleLength;

                Rectangle bounds = getBounds();
                Insets insets = getInsets();
                int totalWidth = bounds.width - insets.left - insets.right;
                int barHeight = bounds.height - insets.top - insets.bottom;
                int barLeft = (int) (totalWidth * leftPos) + insets.left;
                int barRight = (int) (totalWidth * rightPos) + insets.left;
                int barWidth = barRight - barLeft;
                g.setColor(getForeground());
                g.fillRect(barLeft, insets.top, barWidth, barHeight);

                if (endTime > 0 && finishTime > endTime) {
                    double endPos = endTime / maxScheduleLength;
                    int overageLeft = (int) (totalWidth * endPos) + insets.left;
                    int overageWidth = barRight - overageLeft;
                    g.setColor(overtaskedColor);
                    g.fillRect(overageLeft, insets.top, overageWidth, barHeight);
                }

                if (label != null && label.length() > 0) {
                    if (labelFont == null)
                        labelFont = createPlainFont(barHeight - 2);
                    int labelWidth = SwingUtilities.computeStringWidth(
                        getFontMetrics(labelFont), label);
                    int labelPos = calcLabelPos(barLeft, barRight, barWidth,
                        labelWidth, totalWidth);
                    g.setFont(labelFont);
                    g.setColor(labelIsLight ? Color.white : Color.black);
                    g.drawString(label, labelPos, barHeight + insets.top - 2);
                }
            }
        }

        public Font createPlainFont(float height) {
            return UIManager.getFont("Table.font").deriveFont(Font.BOLD).deriveFont(height);
        }

        private int calcLabelPos(int barLeft, int barRight, int barWidth,
                int labelWidth, int totalWidth) {
            // first preference: right aligned inside the colored bar
            if (labelWidth + 2 * PAD < barWidth) {
                int labelPos = barRight - labelWidth - PAD;
                if (!collidesWithBalancedBar(labelPos, labelWidth)) {
                    labelIsLight = barIsDark;
                    return labelPos;
                }
            }

            // second preference: left aligned to the right of the colored bar
            if (barRight + 2 * PAD + labelWidth < totalWidth) {
                int labelPos = barRight + PAD;
                if (!collidesWithBalancedBar(labelPos, labelWidth)) {
                    labelIsLight = false;
                    return labelPos;
                }
            }

            // third preference: inside colored bar, to the left of balanced bar
            if (barLeft + 2 * PAD + labelWidth + BBHW < balancedBarPos) {
                int labelPos = balancedBarPos - BBHW - PAD - labelWidth;
                if (labelPos + labelWidth + PAD < barRight) {
                    labelIsLight = barIsDark;
                    return labelPos;
                }
            }

            // abort: draw at the left of the team member bar.
            if (barLeft < labelWidth / 2)
                labelIsLight = barIsDark;
            else
                labelIsLight = false;
            return barLeft;
        }

        private boolean collidesWithBalancedBar(int labelPos, int labelWidth) {
            if (balancedBarPos < 0)
                return false;
            int leftEdge = labelPos - PAD - BBHW;
            int rightEdge = labelPos + labelWidth + PAD + BBHW;
            return (leftEdge < balancedBarPos && balancedBarPos < rightEdge);
        }

    }

    private DateFormat dateFormat = DateFormat.getDateInstance();

    private static final int BALANCED_BAR_WIDTH = 8;
    private static final int BBHW = BALANCED_BAR_WIDTH / 2;
    private static final int PAD = 3;

}
