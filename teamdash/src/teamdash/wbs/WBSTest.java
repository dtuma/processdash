
package teamdash.wbs;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import javax.swing.*;


import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


import pspdash.XMLUtils;
import org.w3c.dom.Document;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;


public class WBSTest implements WindowListener {

    public static void main(String args[]) {
        String filename = null;
        if (args.length > 0) filename = args[0];

        new WBSTest(filename);
        //new WBSTest(filename, true);
    }

    JTable table;
    WBSModel model;

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
        { "N", null } };

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

    public WBSTest(String filename, boolean ignored) {
        buildModel(filename);
        Map iconMap = buildIconMap();
        table = new WBSJTable(model, iconMap);
        JScrollPane sp = new JScrollPane(table);

        JFrame frame = new JFrame("WBSTest");
        frame.getContentPane().add(sp);
        frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
        frame.pack();
        frame.show();
    }

    public WBSTest(String filename) {
        buildModel(filename);
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
        //frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
        frame.setDefaultCloseOperation(frame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(this);
        frame.pack();
        frame.show();
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
