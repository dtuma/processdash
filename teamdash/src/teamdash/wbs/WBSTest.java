
package teamdash.wbs;

import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JTable;

import org.w3c.dom.Document;
import pspdash.XMLUtils;
import teamdash.TeamMemberList;


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
    TeamProcess teamProcess = new TeamProcess(null);

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

/*
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
*/

    public WBSTest(String filename) {
        buildModel(filename);
        loadTeam();
        DataTableModel data = new DataTableModel(model, teamList, teamProcess);

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
        table.addTab("Time",
                     new String[] { "Time", WBSTabPanel.TEAM_MEMBER_TIMES_ID },
                     new String[] { "Team", "" });

        table.addTab("Time Calc",
                     new String[] { "Size", "Size-Units", "Rate", "Hrs/Indiv", "# People", "Time", "111-Time", "222-Time", "333-Time" },
                     new String[] { "Size", "Units", "Rate", "Hrs/Indiv", "# People",
                         "Time", "111", "222", "333" });

        String[] s = new String[] { "P", "O", "N", "M", "L", "K", "J", "I", "H", "G", "F" };
        table.addTab("Defects", s, s);


        TeamTimePanel teamTime = new TeamTimePanel(teamList, data);

        JFrame frame = new JFrame("WBSTest");
        frame.getContentPane().add(table);
        frame.getContentPane().add(teamTime, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(this);
        frame.pack();
        frame.show();
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
