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

public class TeamTimePanel extends JPanel implements TableModelListener {

    private TeamMemberList teamList;
    private DataTableModel dataModel;
    private GridBagLayout layout;
    private List teamMemberBars;
    private int teamColumnNum;
    private double maxNumWeeks;
    private double balancedWeeks = Double.NaN;
    private JPanel balancedBar;

    public TeamTimePanel(TeamMemberList teamList, DataTableModel dataModel) {
        this.teamList = teamList;
        this.dataModel = dataModel;
        this.teamMemberBars = new ArrayList();
        this.teamColumnNum = dataModel.findColumn("Time");

        setLayout(layout = new GridBagLayout());
        rebuildPanelContents();
        recalc();

        dataModel.addTableModelListener(this);
    }

    private void rebuildPanelContents() {
        removeAll();  // remove all components from this container.
        teamMemberBars.clear();

        balancedBar = new JPanel();
        balancedBar.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        balancedBar.setBackground(Color.darkGray);
        add(balancedBar);
        Dimension d = new Dimension(0, 0);
        balancedBar.setMaximumSize(d);
        balancedBar.setMinimumSize(d);
        balancedBar.setPreferredSize(d);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 2; c.gridy = 0;
        layout.setConstraints(balancedBar, c);

        List teamMembers = teamList.getTeamMembers();
        GridBagConstraints nc = new GridBagConstraints();
        nc.gridx = 0;
        nc.anchor = GridBagConstraints.WEST;
        nc.insets.left = nc.insets.right = 5;
        nc.insets.top = nc.insets.bottom = 0;
        GridBagConstraints bc = new GridBagConstraints();
        bc.gridx = 1;
        bc.fill = GridBagConstraints.BOTH;
        bc.insets.left = bc.insets.right = 5;
        bc.insets.top = bc.insets.bottom = 0;
        bc.weightx = bc.weighty = 1;
        for (int i = 0;   i < teamMembers.size();   i++) {
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

        if (balancedWeeks <= maxNumWeeks && teamMemberBars.size() > 0) {
            Rectangle r = ((TeamMemberBar) teamMemberBars.get(0)).getBounds();
            int pos = r.x + (int) (r.width * balancedWeeks / maxNumWeeks);
            balancedBar.setBounds(pos - BALANCED_BAR_WIDTH/2, 1,
                                  BALANCED_BAR_WIDTH, getHeight());
        }
    }



    public void recalc() {
        recalcIndividuals();
        recalcTeam();
        revalidate();
    }

    protected void recalcIndividuals() {
        maxNumWeeks = 0;
        Iterator i = teamMemberBars.iterator();
        while (i.hasNext())
            maxNumWeeks =
                Math.max(maxNumWeeks, ((TeamMemberBar) i.next()).recalc());
        i = teamMemberBars.iterator();
        while (i.hasNext())
            ((TeamMemberBar) i.next()).update();
    }

    protected void recalcTeam() {
        double teamHoursPerWeek = 0;
        Iterator i = teamMemberBars.iterator();
        while (i.hasNext())
            teamHoursPerWeek += ((TeamMemberBar) i.next()).getHoursPerWeek();

        NumericDataValue teamTotal =
            (NumericDataValue) dataModel.getValueAt(0, teamColumnNum);

        balancedWeeks = teamTotal.value / teamHoursPerWeek;
        balancedBar.setToolTipText("Balanced Team Duration - " +
                                   formatWeeks(balancedWeeks));
    }

    public void tableChanged(TableModelEvent e) {
        recalc();
        repaint();
    }

    private class TeamMemberBar extends JProgressBar {

        private TeamMember teamMember;
        private int columnNumber;
        private double numWeeks;

        public TeamMemberBar(TeamMember teamMember) {
            super(0, NORMALIZED_MAX);
            this.teamMember = teamMember;
            this.columnNumber =
                dataModel.findColumn(teamMember.getInitials()+"-Time");

            setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            setForeground(teamMember.getColor());
        }

        public double getHoursPerWeek() {
            return teamMember.getHoursPerWeek().doubleValue();
        }

        public double recalc() {
            NumericDataValue totalTime =
                (NumericDataValue) dataModel.getValueAt(0, columnNumber);
            numWeeks = totalTime.value / getHoursPerWeek();
            setToolTipText(teamMember.getName()+" - "+formatWeeks(numWeeks));
            return numWeeks;
        }

        public void update() {
            if (maxNumWeeks == 0)
                setValue(0);
            else
                setValue((int) (NORMALIZED_MAX * numWeeks / maxNumWeeks));
        }
    }

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
