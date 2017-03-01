// Copyright (C) 2002-2017 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.wbs;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.EventHandler;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import net.sourceforge.processdash.i18n.Resources;

import teamdash.team.PrivacyType;
import teamdash.team.TeamMember;
import teamdash.team.TeamMemberList;
import teamdash.wbs.columns.MilestoneColorColumn;
import teamdash.wbs.columns.MilestoneCommitDateColumn;
import teamdash.wbs.columns.MilestoneVisibilityColumn;
import teamdash.wbs.columns.TeamActualTimeColumn;
import teamdash.wbs.columns.UnassignedTimeColumn;

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
    /** The data model containing milestone information */
    private MilestonesDataModel milestonesModel;
    /** The layout object managing this panel */
    private GridBagLayout layout;
    /** A colored bar displaying milestone completion dates for the team */
    private TeamMilestoneBar teamMilestoneBar;
    /** A list of the bar charts for each individual (each is a
     * TeamMemberBar object). */
    private List<TeamMemberBar> teamMemberBars;
    /** The point in time when the first person is starting */
    private long teamStartTime;
    /** The team effective date for actual metrics collected so far */
    private Date teamEffectiveDate;
    /** The point in time represented by the left edge of this panel */
    private long leftTimeBoundary;
    /** The number of milliseconds between the left time boundary and the
     * latest finish date */
    private double maxScheduleLength;
    /** The amount of time in schedules for team members whose end date
     * precedes the team effective date */
    private double historicalTeamMemberCollateralTime;
    /** The number of milliseconds between the left time boundary and the
     * balanced completion date.  If no balanced date can be computed, -1 */
    private long balancedLength = -1;
    /** The indicator for the balanced team duration */
    private JPanel balancedBar;
    /** Should the balanced bar be shown, or hidden */
    private boolean showBalancedBar;
    /** Should the bars for each team member be shown, or hidden */
    private boolean showTeamMemberBars;
    /** IDs of individuals who should receive bars, or null for no filter */
    private Set<Integer> subteamFilter;
    /** The name to display for the filtered subteam */
    private String subteamName;
    /** The panel displaying milestone commit dates */
    private CommitDatePane commitDatePane;
    /** Should the bars show total project data, or just remaining project data */
    private boolean showRemainingWork;
    /** Should the balanced bar include unassigned work? */
    private boolean includeUnassigned;
    /** Should we show the number of hours each team member spends per week? */
    private boolean showHoursPerWeek;
    /** Should we show lines for commit dates? */
    private boolean showCommitDates;
    /** Should we show milestone markers on individual bars? */
    private boolean showMilestoneMarks;
    /** Should the bars be colored by milestone, rather than person? */
    private boolean colorByMilestone;
    /** An object to manage the highlighted milestone */
    private MilestoneHighlighter milestoneHighlighter;
    /** An object to manage highlighting of team member bars */
    private MemberBarHighlighter memberBarHighlighter;
    /** if not -1, the ID of a milestone to balance work through */
    private int balanceThroughMilestone;
    /** The name of the milestone we're balancing through, or null if we're
     * balancing the entire schedule */
    private String balanceThroughMilestoneName;
    /** The position of the balanced bar, in pixels from the left edge of the
     * area where colored bars are drawn */
    private int balancedBarPos;
    /** The font to use when drawing labels on colored bars */
    private Font labelFont;
    /** The number format for displaying hours per week information */
    private NumberFormat hoursPerWeekFormat;
    /** The color to use for depicting overtasked time in a colored bar */
    private Color overtaskedColor = Color.red;
    /** A timer used to recalc once after receiving multiple TableModelEvents */
    private Timer recalcTimer;

    private static final Resources resources = Resources
            .getDashBundle("WBSEditor.Team");


    /** Create a team time panel.
     * @param teamList the list of team members to display.
     * @param dataModel the data model containing time data.
     */
    public TeamTimePanel(TeamMemberList teamList, DataTableModel dataModel,
            MilestonesDataModel milestones) {
        this.teamList = teamList;
        this.dataModel = dataModel;
        this.milestonesModel = milestones;
        this.teamMemberBars = new ArrayList<TeamMemberBar>();
        this.showBalancedBar = true;
        this.showTeamMemberBars = true;
        this.showRemainingWork = false;
        this.includeUnassigned = true;
        this.showCommitDates = true;
        this.showMilestoneMarks = true;
        this.colorByMilestone = false;
        this.balanceThroughMilestone = -1;

        this.recalcTimer = new Timer(100, EventHandler.create(
            ActionListener.class, this, "recalc"));
        this.recalcTimer.setRepeats(false);
        this.milestoneHighlighter = new MilestoneHighlighter();
        this.memberBarHighlighter = new MemberBarHighlighter();

        this.hoursPerWeekFormat = NumberFormat.getInstance();
        this.hoursPerWeekFormat.setGroupingUsed(false);
        this.hoursPerWeekFormat.setMinimumFractionDigits(0);

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
        if (this.showBalancedBar != showBalancedBar) {
            this.showBalancedBar = showBalancedBar;
            if (!showBalancedBar)
                showTeamMemberBars = true;
            rebuildPanelContents();
            recalc();
        }
    }

    public boolean isShowTeamMemberBars() {
        return showTeamMemberBars;
    }

    public void setShowTeamMemberBars(boolean showTeamMemberBars) {
        // only allow this setting to be toggled if we are showing the balanced
        // team bar too.  Otherwise, we could end up with no bars to display.
        if (showBalancedBar) {
            if (this.showTeamMemberBars != showTeamMemberBars) {
                this.showTeamMemberBars = showTeamMemberBars;
                rebuildPanelContents();
                recalc();
            }
        }
    }

    public boolean isShowRemainingWork() {
        return showRemainingWork;
    }

    public void setShowRemainingWork(boolean showRemaining) {
        if (this.showRemainingWork != showRemaining) {
            this.showRemainingWork = showRemaining;
            rebuildPanelContents();
            recalc();
        }
    }

    public boolean isIncludeUnassigned() {
        return includeUnassigned;
    }

    public void setIncludeUnassigned(boolean includeUnassigned) {
        if (this.includeUnassigned != includeUnassigned) {
            this.includeUnassigned = includeUnassigned;
            recalc();
        }
    }

    public boolean isShowHoursPerWeek() {
        return showHoursPerWeek;
    }

    public void setShowHoursPerWeek(boolean showHoursPerWeek) {
        if (this.showHoursPerWeek != showHoursPerWeek) {
            this.showHoursPerWeek = showHoursPerWeek;
            rebuildPanelContents();
            recalc();
        }
    }

    public boolean isShowCommitDates() {
        return showCommitDates;
    }

    public void setShowCommitDates(boolean showCommitDates) {
        if (this.showCommitDates != showCommitDates) {
            this.showCommitDates = showCommitDates;
            commitDatePane.loadCommitDates();
            recalc();
        }
    }

    public boolean isShowMilestoneMarks() {
        return showMilestoneMarks;
    }

    public void setShowMilestoneMarks(boolean showMilestoneMarks) {
        if (this.showMilestoneMarks != showMilestoneMarks) {
            this.showMilestoneMarks = showMilestoneMarks;
            if (!showMilestoneMarks && !colorByMilestone)
                milestoneHighlighter.clear();
            recalc();
        }
    }

    public boolean isColorByMilestone() {
        return colorByMilestone;
    }

    public void setColorByMilestone(boolean colorByMilestone) {
        if (this.colorByMilestone != colorByMilestone) {
            this.colorByMilestone = colorByMilestone;
            if (!showMilestoneMarks && !colorByMilestone)
                milestoneHighlighter.clear();
            recalc();
        }
    }

    public int getBalanceThroughMilestone() {
        return balanceThroughMilestone;
    }

    public void setBalanceThroughMilestone(int milestoneID) {
        if (this.balanceThroughMilestone != milestoneID) {
            this.balanceThroughMilestone = milestoneID;
            commitDatePane.loadCommitDates();
            recalc();
        }
    }

    public void applySubteamFilter(Set<Integer> subteamFilter,
            String subteamName) {
        this.subteamFilter = subteamFilter;
        this.subteamName = subteamName;
        this.teamList.setSubteamFilter(subteamFilter);

        rebuildPanelContents();

        ChangeEvent evt = new ChangeEvent(this);
        for (ChangeListener l : listenerList.getListeners(ChangeListener.class))
            l.stateChanged(evt);
    }

    public Set<Integer> getSubteamFilter() {
        return subteamFilter;
    }

    public String getSubteamName() {
        return subteamName;
    }

    public boolean isSubteamFiltered() {
        return subteamFilter != null;
    }

    private String getTeamName() {
        if (subteamFilter == null)
            return "Team";
        else if (subteamName == null)
            return "Subteam";
        else
            return subteamName;
    }

    public void addSubteamFilterListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeSubteamFilterListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
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
        c.gridx = 3; c.gridy = 0;
        layout.setConstraints(balancedBar, c);

        // create and add the panel displaying milestone commit dates.
        commitDatePane = new CommitDatePane();
        add(commitDatePane);
        c = new GridBagConstraints();
        c.gridx = 2; c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.insets.left = c.insets.right = 0;
        c.insets.top = c.insets.bottom = 0;
        c.weightx = c.weighty = 1;
        layout.setConstraints(commitDatePane, c);

        teamEffectiveDate = (Date) dataModel.getWBSModel().getRoot()
            .getAttribute(WBSSynchronizer.EFFECTIVE_DATE_ATTR);
        if (teamEffectiveDate == null)
            teamEffectiveDate = A_LONG_TIME_AGO;
        historicalTeamMemberCollateralTime = 0;

        List teamMembers = teamList.getTeamMembers();
        // create a constraints object for the name labels.
        GridBagConstraints nc = new GridBagConstraints();
        nc.gridx = 0;
        nc.anchor = GridBagConstraints.WEST;
        nc.fill = GridBagConstraints.BOTH;
        nc.insets.left = COLORED_BAR_SIDE_INSET; nc.insets.right = 0;
        nc.insets.top = nc.insets.bottom = 0;
        // create a constraints object for hours per week
        GridBagConstraints hc = new GridBagConstraints();
        hc.gridx = 1;
        hc.anchor = GridBagConstraints.CENTER;
        hc.fill = GridBagConstraints.BOTH;
        hc.insets.left = hc.insets.right = 0;
        hc.insets.top = hc.insets.bottom = 0;
        // create a constraints object for the horizontal bars.
        GridBagConstraints bc = new GridBagConstraints();
        bc.gridx = 2;
        bc.fill = GridBagConstraints.BOTH;
        bc.insets.left = 0; bc.insets.right = COLORED_BAR_SIDE_INSET;
        bc.insets.top = bc.insets.bottom = 0;
        bc.weightx = bc.weighty = 1;
        int row = 1;

        // create a horizontal bar to show balanced team milestone dates
        teamMilestoneBar = new TeamMilestoneBar();
        teamMemberBars.add(teamMilestoneBar);

        // create horizontal progress bars for each team member
        for (int i = 0;   i < teamMembers.size();   i++) {
            TeamMember m = (TeamMember) teamMembers.get(i);

            // if a subteam filter is in place, and this individual is not
            // included, do not create a bar for them.
            if (subteamFilter != null && !subteamFilter.contains(m.getId()))
                continue;

            // if we're only showing remaining time, and this team member's
            // schedule ends before the effective date, don't show a bar for
            // this individual.
            if (showRemainingWork) {
                Date endDate = m.getSchedule().getEndDate();
                if (endDate != null && endDate.before(teamEffectiveDate)) {
                    historicalTeamMemberCollateralTime +=
                        m.getSchedule().getEffortForDate(teamEffectiveDate);
                    continue;
                }
            }

            TeamMemberBar bar = new TeamMemberBar(m);
            teamMemberBars.add(bar);

            teamMilestoneBar.effectivePastHours += bar.effectivePastHours;
        }
        teamMilestoneBar.effectivePastHours += historicalTeamMemberCollateralTime;

        // add all horizontal bars to our panel.
        for (TeamMemberBar bar : teamMemberBars) {
            // possibly skip over this row if requested
            if (bar instanceof TeamMilestoneBar) {
                if (!showBalancedBar) continue;
            } else {
                if (!showTeamMemberBars) continue;
            }

            JLabel name = bar.getNameLabel();
            nc.gridy = row;
            add(name);
            layout.setConstraints(name, nc);

            if (isShowHoursPerWeek()) {
                JLabel hours = bar.getHoursPerWeekLabel();
                hc.gridy = row;
                add(hours);
                layout.setConstraints(hours, hc);
            }

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
                && balancedLength > 0
                && balancedLength <= maxScheduleLength
                && teamMemberBars.size() > 0) {
            TeamMemberBar bar = teamMemberBars.get(0);
            Rectangle r = bar.getBounds();
            Insets i = bar.getInsets();
            int width = r.width - i.left - i.right;
            balancedBarPos = (int) (width * balancedLength / maxScheduleLength);
            int pos = r.x + i.left + balancedBarPos;
            int yOff = r.y * 2 / 3;
            balancedBar.setBounds(pos-BBHW, yOff, BALANCED_BAR_WIDTH,
                getHeight() - yOff);
        } else {
            balancedBarPos = -100;
        }

        // adjust the size of the commit date pane to be as tall as this panel.
        Rectangle r = commitDatePane.getBounds();
        commitDatePane.changeBounds(r.x, r.y, r.width, getHeight());
    }



    public void recalc() {
        recalcTimer.stop();
        recalcStartDate();
        teamMilestoneBar.reset();
        double totalTime = recalcIndividuals() + getUnassignedTime();
        recalcTeam(totalTime);
        revalidate();
        repaintIndividuals();
        repaint();
    }


    /** Recalculate the start dates for the team schedule.
     */
    protected void recalcStartDate() {
        // find out how when the overall team is starting work.
        Date teamStartDate = teamList.getDateForEffort(0);
        teamStartTime = teamStartDate.getTime();

        // select the time value to use for the left edge of the panel
        if (showRemainingWork)
            leftTimeBoundary = Math.max(teamStartTime,
                teamEffectiveDate.getTime());
        else
            leftTimeBoundary = teamStartTime;
    }


    /** Recalculate the horizontal bars for each team member.
     * @return
     */
    protected double recalcIndividuals() {
        double totalTime = historicalTeamMemberCollateralTime;
        long maxLen = 0;
        // recalculate each team member's schedule. Keep track of the longest
        // duration we've seen so far, and the total effective time.
        for (TeamMemberBar tmb : teamMemberBars) {
            if (tmb instanceof TeamMilestoneBar)
                continue;

            tmb.recalc();
            totalTime += tmb.getTotalHours();
            maxLen = Math.max(maxLen, tmb.getFinishTime());
        }
        maxScheduleLength = maxLen;
        return totalTime;
    }

    /** Retrieve the total amount of unassigned time in the WBS, if the user
     * wants it to be included in the calculation; otherwise return 0.
     */
    protected double getUnassignedTime() {
        if (includeUnassigned == false || subteamFilter != null)
            return 0;

        WBSNode rootNode = dataModel.getWBSModel().getRoot();
        Map<Integer, Double> unassignedMilestoneTime =
                (Map<Integer, Double>) rootNode.getAttribute(
                    UnassignedTimeColumn.MILESTONE_UNASSIGNED_TIME_ATTR);

        double result = 0;
        for (WBSNode milestone : getMilestones()) {
            int id = milestone.getUniqueID();
            Double time = unassignedMilestoneTime.get(id);
            if (time != null)
                result += time;
            if (id == balanceThroughMilestone)
                break;
        }

        if (balanceThroughMilestone <= 0) {
            Double time = unassignedMilestoneTime.get(-1);
            if (time != null)
                result += time;
        }

        return result;
    }

    /** Recalculate the duration of a balanced team schedule.
     */
    protected void recalcTeam(double totalHours) {
        if (showCommitDates)
            maxScheduleLength = Math.max(maxScheduleLength,
                commitDatePane.maxDate - leftTimeBoundary);
        // calculate the optimal finish time
        teamMilestoneBar.recalc(totalHours);
        Date balancedDate = teamList.getDateForEffort(totalHours);
        if (balancedDate == null) {
            balancedLength = -1;
        } else {
            balancedLength = balancedDate.getTime() - leftTimeBoundary;
            maxScheduleLength = Math.max(maxScheduleLength, balancedLength);
            String message = "Balanced " + getTeamName() + " "
                    + getDateQualifier() + ": "
                    + dateFormat.format(balancedDate);
            if (balanceThroughMilestoneName != null) {
                message = balanceThroughMilestoneName + " - Optimal " + message;
            }
            balancedBar.setToolTipText(message);
        }
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
        recalcTimer.restart();
    }

    private WBSNode[] getMilestones() {
        return milestonesModel.getWBSModel().getMilestones();
    }

    private String getDateQualifier() {
        return (showRemainingWork ? "Replan Date" : "Plan Date");
    }

    public static boolean colorIsDark(Color c) {
        return getGrayScale(c) < 128;
    }

    private static int getGrayScale(Color c) {
        int rgb = c.getRGB();
        int gray = (int) (0.30 * ((rgb >> 16) & 0xff) +
                0.59 * ((rgb >> 8) & 0xff) +
                0.11 * (rgb & 0xff));
        return gray;
    }




    /**
     * This panel displays arrows and dashed lines for milestone commit dates
     */
    private class CommitDatePane extends JPanel {

        private static final int GUTTER_HEIGHT = 10;
        private static final int ARROW_WIDTH = 16;
        private static final int ARROW_HEIGHT = 8;

        private List<CommitDate> commitDates;
        private long maxDate;
        private boolean isShowing = false;

        private CommitDatePane() {
            setOpaque(false);
            ToolTipManager.sharedInstance().registerComponent(this);

            milestonesModel.addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    loadCommitDates();
                }});
            loadCommitDates();
            addMouseListener(memberBarHighlighter);
        }

        public void changeBounds(int x, int y, int width, int height) {
            setBounds(x, y, width, height);

            int w = width - 2 * COLORED_BAR_SIDE_INSET;
            int l = COLORED_BAR_SIDE_INSET;
            if (teamMemberBars != null && !teamMemberBars.isEmpty()) {
                TeamMemberBar bar = (TeamMemberBar) teamMemberBars.get(0);
                Insets i = bar.getInsets();
                if (i != null) {
                    w = w - i.left - i.right + COLORED_BAR_SIDE_INSET;
                    l = i.left;
                }
            }

            for (CommitDate cd : commitDates) {
                cd.recalcXPos(w, l);
            }
        }

        public void loadCommitDates() {
            maxDate = 0;
            balanceThroughMilestoneName = null;
            boolean hasCommitDates = false;
            List<CommitDate> newDates = new ArrayList<CommitDate>();
            for (WBSNode node : getMilestones()) {
                if (MilestoneVisibilityColumn.isHidden(node))
                    continue;
                CommitDate commitDate = new CommitDate(node);
                if (commitDate.isEmpty() == false)
                    hasCommitDates = true;
                newDates.add(commitDate);
                if (node.getUniqueID() == balanceThroughMilestone) {
                    balanceThroughMilestoneName = node.getName();
                    if (balanceThroughMilestoneName != null
                            && balanceThroughMilestoneName.trim().length() == 0)
                        balanceThroughMilestoneName = null;
                    break;
                }
            }
            if (balanceThroughMilestoneName == null)
                balanceThroughMilestone = -1;

            isShowing = hasCommitDates && showCommitDates;
            int height = (isShowing ? GUTTER_HEIGHT : 0);
            setMinimumSize(new Dimension(0, height));
            setPreferredSize(new Dimension(10, height));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, height));

            commitDates = newDates;
            recalcTimer.restart();
        }

        @Override
        public void paint(Graphics g) {
            if (showCommitDates == false)
                return;

            Stroke plainStroke = null;
            Graphics2D g2 = null;
            if (g instanceof Graphics2D) {
                g2 = (Graphics2D) g;
                plainStroke = g2.getStroke();
            }

            int[] xPoints = new int[3];
            int[] yPoints = new int[3];
            for (CommitDate cd : commitDates) {
                if (cd.xPos == -1)
                    continue;

                g.setColor(cd.color);
                xPoints[0] = cd.xPos + 1 - ARROW_WIDTH/2;
                xPoints[1] = cd.xPos + ARROW_WIDTH/2;
                xPoints[2] = cd.xPos;
                yPoints[0] = yPoints[1] = GUTTER_HEIGHT - ARROW_HEIGHT;
                yPoints[2] = GUTTER_HEIGHT;
                g.fillPolygon(xPoints, yPoints, xPoints.length);

                g.setColor(Color.BLACK);
                if (g2 != null)
                    g2.setStroke(plainStroke);
                xPoints[1]--; /*xPoints[2]--;*/ yPoints[2]--;
                g.drawPolygon(xPoints, yPoints, xPoints.length);

                if (g2 != null)
                    g2.setStroke(milestoneHighlighter.isHighlighted(cd)
                            ? milestoneHighlighter.commitDateHighlightStroke
                            : COMMIT_DATE_LINE_STYLE);
                g.drawLine(cd.xPos, GUTTER_HEIGHT-1, cd.xPos, getHeight());
            }

        }

        @Override
        public boolean contains(int x, int y) {
            return (isShowing ? y < GUTTER_HEIGHT : false);
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            int xPos = event.getX();

            int closestDistance = 2 + ARROW_WIDTH/2;
            CommitDate closestDate = null;
            for (CommitDate cd : commitDates) {
                if (cd.xPos < 0 || cd.tooltip == null)
                    continue;

                int distance = Math.abs(xPos - cd.xPos);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestDate = cd;
                }
            }

            if (closestDate == null) {
                return null;
            } else {
                milestoneHighlighter.set(closestDate);
                return closestDate.tooltip;
            }
        }

        private class CommitDate implements MilestoneHighlightable {

            private int milestoneID;
            private String name;
            private Date date;
            private Color color;
            private String tooltip;
            private int xPos;

            private CommitDate(WBSNode milestone) {
                this.milestoneID = milestone.getUniqueID();
                this.name = milestone.getName();
                this.date = MilestoneCommitDateColumn.getCommitDate(milestone);
                if (this.date != null)
                    maxDate = Math.max(maxDate, this.date.getTime());
                this.color = MilestoneColorColumn.getColor(milestone);
                if (name != null && date != null)
                    this.tooltip = name + " - Commit Date: "
                            + dateFormat.format(date);
            }

            public int getMilestoneID() {
                return milestoneID;
            }

            public Color getMilestoneColor() {
                return color;
            }

            public boolean isEmpty() {
                return name == null || name.trim().length() == 0
                        || date == null;
            }

            private void recalcXPos(int width, int leftPad) {
                this.xPos = calcXPos(width, leftPad);
            }

            private int calcXPos(int width, int leftPad) {
                if (date == null || name == null || name.trim().length() == 0)
                    return -1;
                long xTime = date.getTime() - leftTimeBoundary;
                if (xTime < 0 || xTime > maxScheduleLength)
                    return -1;

                return (int) (width * xTime / maxScheduleLength) + leftPad;
            }
        }

    }

    private static final Stroke COMMIT_DATE_LINE_STYLE = new BasicStroke(1,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1,
            new float[] { 3, 3 }, 0);


    private interface MilestoneHighlightable {
        public int getMilestoneID();
        public Color getMilestoneColor();
    }

    private class MilestoneHighlighter extends Timer implements ActionListener {

        private int milestoneID;
        private Color color;
        private Color bgColor;
        private int i;
        private Paint milestoneHighlightPaint;
        private Stroke commitDateHighlightStroke;
        private boolean armedForClick;
        private boolean clickLocked;

        public MilestoneHighlighter() {
            super(50, null);
            commitDateHighlightStroke = COMMIT_DATE_LINE_STYLE;
            addActionListener(this);
        }

        public void clear() {
            armedForClick = clickLocked = false;
            set(null);
        }

        public void setArmedForClick(boolean armed) {
            armedForClick = armed;
        }

        public void set(MilestoneHighlightable h) {
            if (clickLocked && !armedForClick)
                return;

            if (h == null || (!showMilestoneMarks && !colorByMilestone)) {
                if (this.milestoneID != -1) {
                    this.milestoneID = -1;
                    stop();
                    repaint();
                }
                clickLocked = armedForClick = false;

            } else {
                if (isHighlighted(h)) {
                    // if the user re-clicks the click-locked milestone, clear it.
                    if (clickLocked && armedForClick)
                        set(null);
                } else {
                    // when highlighting a new milestone, calc new colors
                    this.milestoneID = h.getMilestoneID();
                    this.color = h.getMilestoneColor();
                    int gray = getGrayScale(color);
                    switch (gray >> 6) {
                    case 1: case 3: gray -= 64; break;
                    case 2: gray += 64; break;
                    case 0: default: gray = 128; break;
                    }
                    bgColor = new Color(gray, gray, gray);
                    start();
                }
                clickLocked = armedForClick;
                armedForClick = false;
            }
        }

        public boolean isHighlighted(MilestoneHighlightable h) {
            return h.getMilestoneID() == milestoneID;
        }

        public void actionPerformed(ActionEvent e) {
            i = (i + 1) % 40;
            milestoneHighlightPaint = new GradientPaint(i, 0, color, 10 + i,
                    10, bgColor, true);
            commitDateHighlightStroke = new BasicStroke(3, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_BEVEL, 1, new float[] { 5, 5 }, i);
            repaint();
        }

    }


    private class MemberBarHighlighter extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            Component c = e.getComponent();
            if (c instanceof JComponent) {
                milestoneHighlighter.setArmedForClick(true);
                ((JComponent) c).getToolTipText(e);
                milestoneHighlighter.setArmedForClick(false);
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            setBarHighlighted(e, true);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            setBarHighlighted(e, false);
        }

        private void setBarHighlighted(MouseEvent e, boolean highlighted) {
            if (e.getComponent() instanceof JComponent) {
                TeamMemberBar bar = (TeamMemberBar) ((JComponent) e
                        .getComponent()).getClientProperty(TeamMemberBar.class);
                if (bar != null) {
                    bar.setHighlighted(highlighted);
                    repaint();
                }
            }
        }

        private void listen(JComponent comp, TeamMemberBar bar) {
            comp.putClientProperty(TeamMemberBar.class, bar);
            comp.addMouseListener(this);
        }
    }


    /** This class performs the calculations and the display of a
     * horizontal bar for a single team member.
     */
    private class TeamMemberBar extends JPanel {

        /** The TeamMember we are displaying data for */
        private TeamMember teamMember;

        /** True if our colored bar has a dark color */
        private boolean barIsDark;

        /** True if the final milestone segment is a dark color */
        private boolean lastMilestoneIsDark;

        /** True if we should paint the label with a light color */
        private boolean labelIsLight;

        /** What is the effective date of the data for this team member */
        private Date indivEffectiveDate;

        /** When we're showing remaining time, how many hours should we add to
         * account for the portion of the schedule that has already passed? */
        protected double effectivePastHours;

        /** The total number of effective hours in this individual's schedule */
        private double totalHours;

        /** Millis between the team start and the start date for this person */
        private long lagTime;

        /** Millis between the team start and the finish date for this person */
        private long finishTime;

        /** Millis between the team start and the date this person is leaving
         * the project. (-1 if they aren't leaving the project) */
        private long endTime;

        /** The label to display on the bar */
        private String label;

        /** A tooltip to display for the start of the bar */
        private String startTooltip;

        /** The pixel position of the start of the bar */
        private int startPos;

        /** A label displaying the team member name */
        JLabel nameLabel;

        /** A label displaying hours per week */
        JLabel hoursPerWeekLabel;

        private List<MilestoneMark> milestoneMarks;

        protected boolean hideTerminalMilestoneMark;


        public TeamMemberBar(TeamMember teamMember) {
            this.teamMember = teamMember;

            this.indivEffectiveDate = getIndivEffectiveDate();
            effectivePastHours = teamMember.getSchedule().getEffortForDate(
                indivEffectiveDate);

            setBorder(COLORED_BAR_BORDER);
            // use the color associated with the given team member.
            setForeground(teamMember.getColor());
            barIsDark = colorIsDark(teamMember.getColor());

            nameLabel = new JLabel(teamMember.getName());
            nameLabel.setOpaque(true);
            nameLabel.setBorder(NAME_LABEL_BORDER);
            if (showBalancedBar)
                nameLabel.setIcon(SPACER_ICON);
            memberBarHighlighter.listen(nameLabel, this);

            PrivacyType privacy = teamMember.getSchedulePrivacy();
            boolean censorHours = privacy == PrivacyType.Censored
                    || privacy == PrivacyType.Uncertain;
            hoursPerWeekLabel = new JLabel(censorHours ? "*"
                    : hoursPerWeekFormat.format(teamMember.getHoursPerWeek()),
                    SwingConstants.CENTER);
            hoursPerWeekLabel.setOpaque(true);
            hoursPerWeekLabel.setToolTipText(resources.getString(
                censorHours ? "Hours_Censored" : "Hours_Nominal"));
            memberBarHighlighter.listen(hoursPerWeekLabel, this);

            memberBarHighlighter.listen(this, this);
        }

        public JLabel getNameLabel() {
            return nameLabel;
        }

        public JLabel getHoursPerWeekLabel() {
            return hoursPerWeekLabel;
        }

        /**
         * Recalculate the schedule duration for this team member, and return
         * the number of milliseconds in their schedule (including lag time at
         * the beginning of the schedule)
         */
        public void recalc() {
            Date startDate = getStartDate();
            lagTime = startDate.getTime() - leftTimeBoundary;
            if (lagTime < 0) {
                startTooltip = teamMember.getName()
                        + " - Schedule started previously on "
                        + dateFormat.format(startDate);
            } else {
                startTooltip = teamMember.getName() + " - Schedule Start Date "
                        + dateFormat.format(startDate);
            }

            Date endDate = teamMember.getEndDate();
            if (endDate == null)
                endTime = -1;
            else
                endTime = endDate.getTime() - leftTimeBoundary;

            milestoneMarks = new ArrayList<MilestoneMark>();
            Map<Integer, Double> milestoneEffort = getMilestoneEffort();
            double cumMilestoneEffort =
                    (showRemainingWork ? effectivePastHours : 0);
            for (WBSNode milestone : getMilestones()) {
                MilestoneMark mark = new MilestoneMark(milestone,
                        milestoneEffort, cumMilestoneEffort);
                if (mark.effort > 0) {
                    cumMilestoneEffort = mark.cumEffort;
                    if (mark.markTime > 0
                            && !MilestoneVisibilityColumn.isHidden(milestone))
                        milestoneMarks.add(mark);
                }
                if (milestone.getUniqueID() == balanceThroughMilestone)
                    break;
            }
            if (teamMilestoneBar != null)
                teamMilestoneBar.addHoursToTeamMilestoneEffort(milestoneEffort);

            recalcTotalHours(milestoneEffort, cumMilestoneEffort);
        }

        protected void recalcTotalHours(Map<Integer, Double> milestoneEffort,
                double cumMilestoneEffort) {
            if (balanceThroughMilestone > 0) {
                totalHours = cumMilestoneEffort;
            } else if (showRemainingWork) {
                totalHours = getEffectiveRemainingHours();
            } else {
                totalHours = cumMilestoneEffort;
                Double noMilestoneTime = milestoneEffort.get(-1);
                if (noMilestoneTime != null)
                    totalHours += noMilestoneTime;
            }
            recalcFinishDate();
        }

        protected void setTotalHours(double totalHours) {
            this.totalHours = totalHours;
            recalcFinishDate();
        }

        private void recalcFinishDate() {
            Date finishDate = getDateForEffort(totalHours);

            if (finishDate == null) {
                finishTime = -1;
                String hoursString = NumericDataValue.format(totalHours + 0.049);
                String message = hoursString + " total hours";
                String postQual = "";
                if (balanceThroughMilestoneName != null)
                    postQual = " through " + balanceThroughMilestoneName;
                setLabel("", message, postQual);
            } else {
                finishTime = finishDate.getTime() - leftTimeBoundary;
                String dateString = dateFormat.format(finishDate);
                if (endTime > 0 && finishTime > endTime)
                    dateString = dateString + " - OVERTASKED";
                String qualifier = getDateQualifier();
                if (balanceThroughMilestone > 0)
                    qualifier = balanceThroughMilestoneName + " - Optimal "
                            + qualifier;
                setLabel(qualifier + ": ", dateString, "");
            }
        }

        public double getTotalHours() {
            return totalHours;
        }

        public long getFinishTime() {
            return finishTime;
        }

        private void setLabel(String preQual, String message, String postQual) {
            this.label = message;
            setToolTipText(teamMember.getName() + " - " + preQual + message
                    + postQual);
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            int x = event.getX();
            if (Math.abs(x - startPos) < 5)
                return startTooltip;

            if (x > startPos && (showMilestoneMarks || colorByMilestone)) {
                for (MilestoneMark mark : milestoneMarks) {
                    if (x < mark.rightEdge) {
                        milestoneHighlighter.set(mark);
                        return mark.tooltip;
                    }
                }
            }

            milestoneHighlighter.set(null);
            return super.getToolTipText(event);
        }

        private Date getIndivEffectiveDate() {
            String initials = teamMember.getInitials();
            Date result = (Date) dataModel.getWBSModel().getRoot().getAttribute(
                WBSSynchronizer.getIndivEffectiveDateAttrName(initials));
            if (result == null) result = teamEffectiveDate;
            if (result == null) result = new Date();
            return result;
        }

        private double getEffectiveRemainingHours() {
            String attrName = TeamActualTimeColumn.getRemainingTimeAttr(
                teamMember);
            double remainingTime = dataModel.getWBSModel().getRoot()
                    .getNumericAttribute(attrName);
            return remainingTime + effectivePastHours;
        }

        protected Date getStartDate() {
            return teamMember.getStartDate();
        }

        protected Date getDateForEffort(double hours) {
            return teamMember.getSchedule().getDateForEffort(hours);
        }

        protected Map<Integer, Double> getMilestoneEffort() {
            String attrName;
            if (showRemainingWork)
                attrName = TeamActualTimeColumn
                        .getMilestoneRemainingTimeAttr(teamMember);
            else
                attrName = TeamActualTimeColumn
                        .getMilestonePlanTimeAttr(teamMember);
            return (Map<Integer, Double>) dataModel.getWBSModel().getRoot()
                    .getAttribute(attrName);
        }

        private boolean barIsDark() {
            return (colorByMilestone ? lastMilestoneIsDark : barIsDark);
        }

        private void setHighlighted(boolean highlight) {
            Color bgColor = (highlight ? BAR_HIGHLIGHT_COLOR : null);
            this.setBackground(bgColor);
            nameLabel.setBackground(bgColor);
            hoursPerWeekLabel.setBackground(bgColor);
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

            if (labelFont == null) {
                Insets i = getInsets();
                labelFont = createPlainFont(getHeight() - i.top - i.bottom - 2);
            }
            hoursPerWeekLabel.setFont(labelFont);

            if (finishTime > 0 && maxScheduleLength > 0) {
                // now paint the bar.
                long leftTime = Math.max(lagTime, 0);
                double leftPos = leftTime / maxScheduleLength;
                double rightPos = finishTime / maxScheduleLength;

                Rectangle bounds = getBounds();
                Insets insets = getInsets();
                int totalWidth = bounds.width - insets.left - insets.right;
                int barHeight = bounds.height - insets.top - insets.bottom;
                int barLeft = (int) (totalWidth * leftPos) + insets.left;
                int barRight = (int) (totalWidth * rightPos) + insets.left;
                int barWidth = barRight - barLeft;
                g.setColor(colorByMilestone ? Color.darkGray : getForeground());
                g.fillRect(barLeft, insets.top, barWidth, barHeight);

                if (endTime > 0 && finishTime > endTime && !colorByMilestone) {
                    double endPos = endTime / maxScheduleLength;
                    int overageLeft = (int) (totalWidth * endPos) + insets.left;
                    int overageWidth = barRight - overageLeft;
                    g.setColor(overtaskedColor);
                    g.fillRect(overageLeft, insets.top, overageWidth, barHeight);
                }

                // paint milestone marks from right to left, so earlier marks
                // cover later overlapping marks.
                if (showMilestoneMarks || colorByMilestone) {
                    lastMilestoneIsDark = true;
                    for (int i = milestoneMarks.size(); i-- > 0;) {
                        MilestoneMark mark = milestoneMarks.get(i);

                        long markLeftTime = Math.max(0, (i == 0 ? leftTime //
                                : milestoneMarks.get(i - 1).markTime));

                        mark.paint(g, insets.left, insets.top, markLeftTime,
                            totalWidth, barHeight, bounds.height);

                        if (mark.isTerminalMilestone())
                            lastMilestoneIsDark = mark.milestoneColorIsDark;
                    }
                }

                if (label != null && label.length() > 0) {
                    int labelWidth = SwingUtilities.computeStringWidth(
                        getFontMetrics(labelFont), label);
                    int labelPos = calcLabelPos(barLeft, barRight, barWidth,
                        labelWidth, totalWidth);
                    g.setFont(labelFont);
                    g.setColor(labelIsLight ? Color.white : Color.black);
                    g.drawString(label, labelPos, barHeight + insets.top - 2);
                }

                this.startPos = barLeft;

                // in "show remaining work" mode, if this team member started
                // before the effective date, draw a jagged left edge for their
                // colored bar to indicate that the full schedule continues
                // to the left
                if (lagTime < 0 && showRemainingWork) {
                    g.setColor(getBackground());
                    int ll = insets.left, tt = insets.top, d = barHeight/4;
                    int[] xx = new int[] { ll, ll + d, ll, ll+d, ll };
                    int[] yy = new int[] { tt, tt+d, tt+d*2, tt+d*3, tt+barHeight };
                    g.fillPolygon(xx, yy, xx.length);
                }
            }
        }

        public Font createPlainFont(float height) {
            return UIManager.getFont("Table.font").deriveFont(Font.BOLD).deriveFont(height);
        }

        private int calcLabelPos(int barLeft, int barRight, int barWidth,
                int labelWidth, int totalWidth) {
            // first preference: left aligned to the right of the colored bar
            if (barRight + 2 + 2 * PAD + labelWidth < totalWidth) {
                int labelPos = barRight + PAD + 2;
                if (!collidesWithBalancedBar(labelPos, labelWidth)) {
                    labelIsLight = false;
                    return labelPos;
                }
            }

            // second preference: right aligned inside the colored bar
            if (labelWidth + 2 * PAD < barWidth) {
                int labelPos = barRight - labelWidth - PAD;
                if (!collidesWithBalancedBar(labelPos, labelWidth)) {
                    labelIsLight = barIsDark();
                    return labelPos;
                }
            }

            // third preference: inside colored bar, to the left of balanced bar
            if (barLeft + 2 * PAD + labelWidth + BBHW < balancedBarPos) {
                int labelPos = balancedBarPos - BBHW - PAD - labelWidth;
                if (labelPos + labelWidth + PAD < barRight) {
                    labelIsLight = barIsDark();
                    return labelPos;
                }
            }

            // fourth preference: right aligned to the left of the colored bar
            if (barLeft > 2 * PAD + labelWidth) {
                int labelPos = barLeft - PAD - labelWidth;
                if (!collidesWithBalancedBar(labelPos, labelWidth)) {
                    labelIsLight = false;
                    return labelPos;
                }
            }

            // fifth preference: to the right of the balanced bar
            if (balancedBarPos > 0 && barRight < balancedBarPos + BBHW) {
                int labelPos = balancedBarPos + BBHW + PAD;
                if (labelPos + labelWidth + PAD < totalWidth) {
                    labelIsLight = false;
                    return labelPos;
                }
            }

            // abort: draw at the left of the team member bar.
            if (barLeft < labelWidth / 2)
                labelIsLight = barIsDark();
            else
                labelIsLight = false;
            return PAD;
        }

        private boolean collidesWithBalancedBar(int labelPos, int labelWidth) {
            if (balancedBarPos < 0)
                return false;
            int leftEdge = labelPos - PAD - BBHW;
            int rightEdge = labelPos + labelWidth + PAD + BBHW;
            return (leftEdge < balancedBarPos && balancedBarPos < rightEdge);
        }

        private class MilestoneMark implements MilestoneHighlightable {

            int milestoneID;
            double effort;
            double cumEffort;
            long markTime;
            Color color;
            boolean milestoneColorIsDark;
            String tooltip;
            int xPos, rightEdge, leftEdge;

            public MilestoneMark(WBSNode milestone,
                    Map<Integer, Double> milestoneEffort,
                    double startingEffort) {
                if (milestone == null || milestoneEffort == null)
                    return;

                milestoneID = milestone.getUniqueID();
                Double effortVal = milestoneEffort.get(milestoneID);
                if (effortVal == null) {
                    effort = 0;
                    return;
                }

                effort = effortVal.doubleValue();
                cumEffort = startingEffort + effort;
                Date when = getDateForEffort(cumEffort);
                if (when == null) {
                    markTime = -1;
                    return;
                }
                markTime = when.getTime() - leftTimeBoundary;

                color = MilestoneColorColumn.getColor(milestone);
                milestoneColorIsDark = colorIsDark(color);
                tooltip = teamMember.getName() + " - " + milestone.getName()
                        + " - Optimal " + getDateQualifier() + ": "
                        + dateFormat.format(when);
            }

            public int getMilestoneID() {
                return milestoneID;
            }

            public Color getMilestoneColor() {
                return color;
            }

            public boolean isTerminalMilestone() {
                return Math.abs(markTime - finishTime) < 60000;
            }

            public void paint(Graphics g, int leftInset, int topInset,
                    long leftTime, int totalWidth, int barHeight, int height) {
                if (markTime < 0)
                    return;

                double markPos = markTime / maxScheduleLength;
                rightEdge = xPos = (int) (totalWidth * markPos) + leftInset;

                boolean isHighlighted = milestoneHighlighter.isHighlighted(this);
                if (colorByMilestone || isHighlighted) {
                    ((Graphics2D) g).setPaint(isHighlighted //
                            ? milestoneHighlighter.milestoneHighlightPaint
                            : this.color);
                    double leftPos = leftTime / maxScheduleLength;
                    int leftX = (int) (totalWidth * leftPos) + leftInset;
                    g.fillRect(leftX, topInset, xPos - leftX, barHeight);
                }

                if ((hideTerminalMilestoneMark && isTerminalMilestone())
                        || showMilestoneMarks == false)
                    return;

                int hh = (height + 1) / 2;
                for (int i = 0;  i < hh;  i++) {
                    int d = (i + 1) / 2;
                    int l = height - i - 1;
                    leftEdge = xPos-d;
                    rightEdge = xPos+d;
                    g.setColor(Color.black);
                    g.drawLine(leftEdge, i, rightEdge, i);
                    g.drawLine(leftEdge, l, rightEdge, l);
                    if (d > 0) {
                        g.setColor(color);
                        g.drawLine(leftEdge+1, i, rightEdge-1, i);
                        g.drawLine(leftEdge+1, l, rightEdge-1, l);
                    }
                }

            }
        }
    }

    private class TeamMilestoneBar extends TeamMemberBar {

        private Map<Integer, Double> teamMilestoneEffort;

        public TeamMilestoneBar() {
            super(new TeamMember(getTeamName() + " (Balanced)", " Team ",
                    Color.darkGray, 0, teamList.getZeroDay()));
            effectivePastHours = 0;
            hideTerminalMilestoneMark = true;

            nameLabel.setIcon(showTeamMemberBars ? IconFactory.getMinusIcon()
                    : IconFactory.getPlusIcon());
            nameLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            nameLabel.addMouseListener(toggleMembers);

            hoursPerWeekLabel.setText("");
        }

        @Override
        protected Date getStartDate() {
            return new Date(teamStartTime);
        }

        @Override
        protected Date getDateForEffort(double hours) {
            return teamList.getDateForEffort(hours);
        }

        @Override
        protected Map<Integer, Double> getMilestoneEffort() {
            return teamMilestoneEffort;
        }

        @Override
        protected void recalcTotalHours(Map<Integer, Double> milestoneEffort,
                double cumMilestoneEffort) {
            // do nothing; our total hours value will be set elsewhere
        }

        public void reset() {
            teamMilestoneEffort = new HashMap<Integer, Double>();
        }

        public void recalc(double totalHours) {
            if (includeUnassigned && subteamFilter == null)
                addHoursToTeamMilestoneEffort(getUnassignedMilestoneTime());

            recalc();
            setTotalHours(totalHours);
        }

        private Map<Integer, Double> getUnassignedMilestoneTime() {
            WBSNode rootNode = dataModel.getWBSModel().getRoot();
            return (Map<Integer, Double>) rootNode.getAttribute(
                UnassignedTimeColumn.MILESTONE_UNASSIGNED_TIME_ATTR);
        }

        public void addHoursToTeamMilestoneEffort(Map<Integer, Double> hours) {
            if (hours != null && hours != teamMilestoneEffort) {
                for (Entry<Integer, Double> s : hours.entrySet()) {
                    Integer milestoneID = s.getKey();
                    Double newVal = s.getValue();
                    Double currVal = teamMilestoneEffort.get(milestoneID);
                    if (currVal == null) {
                        teamMilestoneEffort.put(milestoneID, newVal);
                    } else if (newVal != null) {
                        teamMilestoneEffort.put(milestoneID, currVal + newVal);
                    }
                }
            }
        }

    }

    private MouseAdapter toggleMembers = new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
            setShowTeamMemberBars(!isShowTeamMemberBars());
        }
    };

    private static final Icon SPACER_ICON = IconFactory.getEmptyIcon(9, 9);

    private DateFormat dateFormat = DateFormat.getDateInstance();

    private static final int COLORED_BAR_SIDE_INSET = 5;
    private static final int BALANCED_BAR_WIDTH = 7;
    private static final int BBHW = BALANCED_BAR_WIDTH / 2;
    private static final int PAD = 6;
    private static final Date A_LONG_TIME_AGO = new Date(0);

    private static final Border NAME_LABEL_BORDER = BorderFactory
            .createEmptyBorder(0, 0, 0, COLORED_BAR_SIDE_INSET);
    private static final Border COLORED_BAR_BORDER = BorderFactory.createCompoundBorder(
        BorderFactory.createEmptyBorder(0, COLORED_BAR_SIDE_INSET, 0, 0),
        BorderFactory.createBevelBorder(BevelBorder.LOWERED));
    private static final Color BAR_HIGHLIGHT_COLOR = new Color(160, 160, 160);

}
