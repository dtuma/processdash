package teamdash.wbs;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.w3c.dom.Element;
import teamdash.process.CustomProcess;


public class TeamProcess {

    private List phases;
    private Map phaseTypes;
    private Map iconMap;



    public TeamProcess(Element xml) {
        buildPhases(xml);
        buildIcons();
    }

    public List getPhases() {
        return phases;
    }

    public String getPhaseType(String phase) {
        String result =(String) phaseTypes.get(phase);
        if (result == null && phase.endsWith(" Task"))
            result = (String) phaseTypes.get
                (phase.substring(0, phase.length()-5));
        return result;
    }

    private final String[] SIZE_UNITS = new String[] {
            "LOC","Text Pages", "Reqts Pages", "HLD Pages", "DLD Lines" };

    public String getPhaseSizeMetric(String phase) {
        String type = getPhaseType(phase);
        if (type == null) return SIZE_UNITS[0];
        if (type.startsWith("DOC")) return SIZE_UNITS[1];
        if (type.startsWith("REQ")) return SIZE_UNITS[2];
        if (type.startsWith("HLD")) return SIZE_UNITS[3];
        if (type.startsWith("DLD")) return SIZE_UNITS[4];
        return SIZE_UNITS[0];
    }

    public Map getIconMap() {
        return iconMap;
    }

    public JMenu getNodeTypeMenu() {
        return buildMenu();
    }

    private void buildPhases(Element xml) {
        CustomProcess process = null;
        if (xml != null) try {
            process = new CustomProcess(xml);
        } catch (IOException ioe) {}
        if (process == null)
            process = new CustomProcess();

        phases = new ArrayList();
        phaseTypes = new HashMap();

        Iterator i = process.getPhaseIterator();
        while (i.hasNext()) {
            CustomProcess.CustomPhase phase =
                (CustomProcess.CustomPhase) i.next();
            phases.add(phase.name);
            phaseTypes.put(phase.name, phase.type);
        }
        phases = Collections.unmodifiableList(phases);
        phaseTypes = Collections.unmodifiableMap(phaseTypes);
    }

    private void buildIcons() {
        iconMap = new HashMap();

        // create a handful of icons that always exist.
        Color c = new Color(204, 204, 255);
        iconMap.put("Project", IconFactory.getProjectIcon());
        iconMap.put(SW_COMP, IconFactory.getSoftwareComponentIcon());
        iconMap.put(GEN_DOC, IconFactory.getDocumentIcon(Color.white));
        iconMap.put("PSP Task", IconFactory.getPSPTaskIcon(c));
        iconMap.put("Workflow", IconFactory.getWorkflowIcon());
        iconMap.put(null, IconFactory.getTaskIcon(c));

        int numPhases = phases.size();
        for (int phaseNum = 0; phaseNum < numPhases; phaseNum++) {
            String phase = (String) phases.get(phaseNum);
            String type = getPhaseType(phase);
            Color phaseColor = getPhaseColor(phaseNum, numPhases-1);
            iconMap.put(phase + " Task", IconFactory.getTaskIcon(phaseColor));

            if ("REQ".equals(type) && !iconMap.containsKey(REQ_DOC))
                iconMap.put(REQ_DOC, IconFactory.getDocumentIcon(phaseColor));
            if ("HLD".equals(type) && !iconMap.containsKey(HLD_DOC))
                iconMap.put(HLD_DOC, IconFactory.getDocumentIcon(phaseColor));
            if ("DLD".equals(type) && !iconMap.containsKey(DLD_DOC))
                iconMap.put(DLD_DOC, IconFactory.getDocumentIcon(phaseColor));
        }

        if (!iconMap.containsKey(REQ_DOC))
            iconMap.put(REQ_DOC,
                        IconFactory.getDocumentIcon(new Color(204, 204, 0)));
        if (!iconMap.containsKey(HLD_DOC))
            iconMap.put(HLD_DOC,
                        IconFactory.getDocumentIcon(new Color(153, 153, 255)));
        if (!iconMap.containsKey(DLD_DOC))
            iconMap.put(DLD_DOC,
                        IconFactory.getDocumentIcon(new Color(102, 255, 102)));

        iconMap = Collections.unmodifiableMap(iconMap);
    }

    private static final String SW_COMP = "Software Component";
    private static final String GEN_DOC = "General Document";
    private static final String REQ_DOC = "Requirements Document";
    private static final String HLD_DOC = "High Level Design Document";
    private static final String DLD_DOC = "Detailed Design Document";

    private static final boolean INCLUDE_PURPLE = false;

    /** Calculate the appropriate for displaying a particular phase. */
    private Color getPhaseColor(int phaseNum, int numPhases) {
        double a, r, g, b;

        a = norm(phaseNum, 0, numPhases);

        if (INCLUDE_PURPLE) {

            if (a > 0.8) {        // blend from dark blue to purple.
                r = 128 * norm(a, 0.8, 1.0);
                g = 63 * norm(a, 1.0, 0.8);
                b = 255;
            } else if (a > 0.6) { // blend from cyan to dark blue.
                r = 0;
                g = 255 - 192 * norm(a, 0.6, 0.8);
                b = 255;
            } else if (a > 0.40) { // blend from green to cyan.
                r = 0;
                g = 255;
                b = 255 * norm(a, 0.4, 0.6);
            } else if (a > 0.2) { // blend from yellow to green
                r = 255 * norm(a, 0.4, 0.2);
                g = 255;
                b = 0;
            } else { // blend from orange to yellow.
                r = 255;
                g = 255 - 128 * norm(a, 0.2, 0.0);
                b = 0;
            }

        } else {

            if (a > 0.75) { // blend from cyan to dark blue.
                r = 0;
                g = 255 - 192 * norm(a, 0.75, 1.0);
                b = 255;
            } else if (a > 0.50) { // blend from green to cyan.
                r = 0;
                g = 255;
                b = 255 * norm(a, 0.5, 0.75);
            } else if (a > 0.25) { // blend from yellow to green
                r = 255 * norm(a, 0.5, 0.25);
                g = 255;
                b = 0;
            } else { // blend from orange to yellow.
                r = 255;
                g = 255 - 128 * norm(a, 0.25, 0.0);
                b = 0;
            }

        }
        return new Color((int) r, (int) g, (int) b);
    }

    /** Perform a simple linear transform.  Calculate a linear transform
     * that maps a to 0 and b to 1; then return the mapping for w. */
    private static double norm(double w, double a, double b) {
        return (w - a) / (b - a);
    }

    private JMenu buildMenu() {
        JMenu taskSubmenu = new JMenu("Tasks");

        JMenu nodeTypeMenu = new JMenu();
        nodeTypeMenu.add(taskSubmenu);
        nodeTypeMenu.addSeparator();
        for (int j = 0; j < iconMenuItems.length; j++)
            nodeTypeMenu.add(new JMenuItem(iconMenuItems[j]));

        taskSubmenu.add(new JMenuItem("PSP Task"));
        Iterator i = phases.iterator();
        while (i.hasNext()) {
            if (taskSubmenu.getItemCount() >= 15) {
                JMenu moreTaskMenu = new JMenu("More...");
                taskSubmenu.add(moreTaskMenu, 0);
                taskSubmenu = moreTaskMenu;
            }
            taskSubmenu.add(new JMenuItem(i.next() + " Task"));
        }

        return nodeTypeMenu;
    }

    private static final String[] iconMenuItems =
        { SW_COMP, GEN_DOC, REQ_DOC, HLD_DOC, DLD_DOC };
/*
    public static String[][] defaultPhases = {
        { "Management and Miscellaneous", "Misc", "MGMT" },
        { "Launch and Strategy", "Strategy", "STRAT" },
        { "Planning", "Planning", "PLAN" },
        { "Requirements", "Reqts", "REQ" },
        { "System Test Plan", "Sys Test Plan", "STP" },
        { "Requirements Inspection", "Reqts Inspect", "REQINSP" },
        { "High-Level Design", "HLD", "HLD" },
        { "Integration Test Plan", "Int Test Plan", "ITP" },
        { "HLD Review", "HLD Review", "HLDRINSP" },
        { "HLD Inspection", "HLD Inspect", "HLDRINSP" },
        { "Detailed Design", "Design", "DLD" },
        { "Detailed Design Review", "Design Review", "DLDR" },
        { "Test Development", "Test Devel", "TD" },
        { "Detailed Design Inspection", "Design Inspect", "DLDINSP" },
        { "Code", "Code", "CODE" },
        { "Code Review", "Code Review", "CR" },
        { "Compile", "Compile", "COMP" },
        { "Code Inspection", "Code Inspect", "CODEINSP" },
        { "Unit Test", "Test", "UT" },
        { "Build and Integration Test", "Int Test", "IT" },
        { "System Test", "Sys Test", "ST" },
        { "Documentation", "Documentation", "DOC" },
        { "Acceptance Test", "Accept Test", "AT" },
        { "Postmortem", "Postmortem", "PM" },
        { "Product Life", "Product Life", "PL" }
    };
    */
}
