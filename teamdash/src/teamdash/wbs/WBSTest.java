
package teamdash.wbs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.w3c.dom.Document;
import pspdash.XMLUtils;
import teamdash.*;
import teamdash.TeamMember;
import teamdash.TeamMemberList;
import teamdash.wbs.columns.TeamMemberTimeColumn;
import teamdash.wbs.columns.TeamTimeColumn;


public class WBSTest implements WindowListener {

    public static void main(String args[]) {
        String filename = null;
        if (args.length > 0) filename = args[0];

        new WBSTest(filename);
        //new WBSTest(filename, true);
    }

    JTable table;
    WBSModel model;
    TeamMemberList teamList;
    TeamProcess teamProcess = new TeamProcess();

    private static final String[][] nodes = {
        { " A",  "Software Component" },
        { "  B", "Requirements Document" },
        { "  C", "Task" },
        { "D",   "Software Component" },
        { " E",  "Software Component" },
        { "  F", "Task" },
        { " G",  "High Level Design Document" },
        { " H",  "Detailed Design Document" },
        { "I",   "General Document" },
        { "  J", "Task" },
        { " K",  "General Document" },
        { " L",  "Task" },
        { "M",  "Software Component" },
        { "N", "Task" } };
    private static final String[] iconMenuItems = {
        "Software Component",
        "General Document",
        "Requirements Document",
        "High Level Design Document",
        "Detailed Design Document" };

    private void buildModel(String filename) {
        if (filename != null) try {
            Document doc = XMLUtils.parse(new FileInputStream(filename));
            model = new WBSModel(doc.getDocumentElement());
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }

        buildDefaultModel();
    }
    private void buildDefaultModel() {
        model = new WBSModel();
        for (int i = 0;   i < nodes.length;   i++)
            model.add(new WBSNode(model,
                                  nodes[i][0].trim(),
                                  nodes[i][1],
                                  1 + (nodes[i][0].length() -
                                       nodes[i][0].trim().length()),
                                  true));
    }
    private void loadTeam() {
        try {
            Document doc = XMLUtils.parse(new FileInputStream("team.xml"));
            teamList = new TeamMemberList(doc.getDocumentElement());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public WBSTest(String filename, boolean ignored) {
        buildModel(filename);
        Map iconMap = teamProcess.getIconMap(); //buildIconMap();
        table = new WBSJTable(model, iconMap);
        JScrollPane sp = new JScrollPane(table);

        JFrame frame = new JFrame("WBSTest");
        frame.getContentPane().add(sp);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.show();
    }

    public WBSTest(String filename) {
        buildModel(filename);
        loadTeam();
        DataTableModel data = new DataTableModel(model);

        WBSTabPanel table = new WBSTabPanel(model, data, teamProcess);


        table.addTab("Size",
                     new String[] { "Size", "Size-Units", "N&C-LOC", "N&C-Text Pages",
                                    "N&C-Reqts Pages", "N&C-HLD Pages", "N&C-DLD Lines" },
                     new String[] { "Size", "Units", "LOC","Text Pages",
                                    "Reqts Pages", "HLD Pages", "DLD Lines" });
        table.addTab("Size Accounting",
                     new String[] { "Size-Units", "Base", "Deleted", "Modified", "Added",
                                    "Reused", "N&C", "Total" },
                     new String[] { "Units",  "Base", "Deleted", "Modified", "Added",
                                    "Reused", "N&C", "Total" });

        List teamMembers = teamList.getTeamMembers();
        int teamSize = teamMembers.size();
        String[] teamColumnIDs = new String[teamSize+1];
        String[] teamColumnNames = new String[teamSize+1];
        DataTableModel dataModel = (DataTableModel) table.dataTable.getModel();
        dataModel.addDataColumn(new TeamTimeColumn(dataModel));
        for (int i = 0;   i < teamMembers.size();   i++) {
            TeamMember m = (TeamMember) teamMembers.get(i);
            TeamMemberTimeColumn col = new TeamMemberTimeColumn(dataModel, m);
            dataModel.addDataColumn(col);
            teamColumnIDs[i+1] = col.getColumnID();
            teamColumnNames[i+1] = m.getInitials();
        }
        teamColumnIDs[0] = "Time";
        teamColumnNames[0] = "Team";
        table.addTab("Time", teamColumnIDs, teamColumnNames);

        table.addTab("Time Calc",
                     new String[] { "Size", "Size-Units", "Rate", "Hrs/Indiv", "# People", "Time", "111-Time", "222-Time", "333-Time" },
                     new String[] { "Size", "Units", "Rate", "Hrs/Indiv", "# People",
                         "Time", "111", "222", "333" });

        String[] s = new String[] { "P", "O", "N", "M", "L", "K", "J", "I", "H", "G", "F" };
        table.addTab("Defects", s, s);


        TeamTimePanel teamTime = new TeamTimePanel(teamList, dataModel);

        //JScrollPane sp = new JScrollPane(table);

        JFrame frame = new JFrame("WBSTest");
        frame.getContentPane().add(table);
        frame.getContentPane().add(teamTime, BorderLayout.SOUTH);
        //frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(this);
        frame.pack();
        frame.show();
        /*
        long beg = System.currentTimeMillis();
        for (int j = 1000; j-- > 0;)
            table.paint();
        long end = System.currentTimeMillis();
        System.out.println("paint time (ms): "+ (end-beg));    */
    }

    private Map buildIconMap() {
        Map result = new HashMap();
        Color c = new Color(204, 204, 255);
        result.put("Project", IconFactory.getProjectIcon());
        result.put("Software Component",
                   IconFactory.getSoftwareComponentIcon());
        result.put("Requirements Document",
                   IconFactory.getDocumentIcon(new Color(204, 204, 0)));
        result.put("High Level Design Document",
                   IconFactory.getDocumentIcon(new Color(153, 153, 255)));
        result.put("Detailed Design Document",
                   IconFactory.getDocumentIcon(new Color(102, 255, 102)));
        result.put("General Document",
                   IconFactory.getDocumentIcon(Color.white));
        result.put("Task", IconFactory.getTaskIcon(c));
        result.put("PSP Task", IconFactory.getPSPTaskIcon(c));
        result.put(null, IconFactory.getTaskIcon(c));

        return result;
    }


    // implementation of java.awt.event.WindowListener interface

    public void windowOpened(WindowEvent param1) {}
    public void windowClosed(WindowEvent param1) {}
    public void windowDeiconified(WindowEvent param1) {}
    public void windowActivated(WindowEvent param1) {}
    public void windowDeactivated(WindowEvent param1) {}
    public void windowIconified(WindowEvent param1) {}
    public void windowClosing(WindowEvent param1) {
        try {
            FileWriter out = new FileWriter("out.xml");
            model.getAsXML(out);
            out.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        System.exit(0);
    }

}
