
package teamdash.wbs;

import java.util.*;
import javax.swing.*;


import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


public class WBSTest {

    public static void main(String args[]) {
        new WBSTest();
        //new WBSTest(args.length > 0 ? args[0] : "#ccccff");
    }

    JTable table;
    WBSModel model;

    private static final String[][] nodes = {
        { " A",  "Software Component" },
        { "  B", "Document" },
        { "  C", "Task" },
        { "D",   "Software Component" },
        { " E",   "Software Component" },
        { "  F",  "Task" },
        { " G",   "Document" },
        { " H",  "Document" },
        { "I", "Document" },
        { "  J", "Task" },
        { " K", "Document" },
        { " L", "Task" },
        { "M", "Software Component" },
        { "N", null } };

    public WBSTest(String colFmt) {
        model = new WBSModel();
        for (int i = 0;   i < nodes.length;   i++)
            model.add(new WBSNode(model,
                                  nodes[i][0].trim(),
                                  nodes[i][1],
                                  1 + (nodes[i][0].length() -
                                       nodes[i][0].trim().length()),
                                  true));
        Map iconMap = buildIconMap();
        table = new WBSJTable(model, iconMap);
        JScrollPane sp = new JScrollPane(table);

        JFrame frame = new JFrame("WBSTest");
        frame.getContentPane().add(sp);
        frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
        frame.pack();
        frame.show();
    }

    public WBSTest() {
        model = new WBSModel();
        for (int i = 0;   i < nodes.length;   i++)
            model.add(new WBSNode(model,
                                  nodes[i][0].trim(),
                                  nodes[i][1],
                                  1 + (nodes[i][0].length() -
                                       nodes[i][0].trim().length()),
                                  true));
        DataTableModel data = new DataTableModel(model);

        Map iconMap = buildIconMap();
        WBSTabPanel table = new WBSTabPanel(model, data, iconMap);
        table.addTab("Vowels", new String[] {"N&C LOC", "E", "I", "O", "U" });
        table.addTab("First", new String[] {
            "N&C LOC", "B", "C", "D", "E", "F", "G", "H", "I", "J" });
        table.addTab("Last", new String[] {
            "Z", "Y", "X", "W", "V", "U", "T", "S", "R", "Q" });

        //JScrollPane sp = new JScrollPane(table);

        JFrame frame = new JFrame("WBSTest");
        frame.getContentPane().add(table);
        frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
        frame.pack();
        frame.show();
    }

    private Map buildIconMap() {
        Map result = new HashMap();
        Color c = new Color(204, 204, 255);
        result.put("Project", IconFactory.getProjectIcon());
        result.put("Software Component", IconFactory.getSoftwareComponentIcon());
        result.put("Requirements Document", IconFactory.getDocumentIcon(Color.orange));
        result.put("High Level Design Document", IconFactory.getDocumentIcon(Color.blue));
        result.put("Detailed Design Document", IconFactory.getDocumentIcon(Color.green));
        result.put("General Document", IconFactory.getDocumentIcon(Color.white));
        result.put("Task", IconFactory.getTaskIcon(c));
        result.put(null, IconFactory.getTaskIcon(c));

        return result;
    }

}
