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


/** Keep track of information the wbs editor needs to know about the
 * defined team process.
 */
public class TeamProcess {

    /** An immutable list of the names of the phases in this
     * process (Strings) */
    private List phases;

    /** An immutable map of phase names to phase types. */
    private Map phaseTypes;

    /** An immutable map of node types to node icons. */
    private Map iconMap;



    /** Contruct a team process from information in the given XML element.
     */
    public TeamProcess(Element xml) {
        buildPhases(xml);
        buildIcons();
    }


    /** Return a list of the phases in this process. */
    public List getPhases() {
        return phases;
    }


    /** Return the canonical "phase type" for a given phase. */
    public String getPhaseType(String phase) {
        String result = (String) phaseTypes.get(phase);
        if (result == null && phase.endsWith(" Task"))
            result = (String) phaseTypes.get
                (phase.substring(0, phase.length()-5));
        return result;
    }


    /** For a task of the given phase, which size metric is the most
     * appropriate to use for generating a time estimate?
     */
    public String getPhaseSizeMetric(String phase) {
        String type = getPhaseType(phase);
        if (type == null) return SIZE_UNITS[0];
        if (type.startsWith("DOC")) return SIZE_UNITS[1];
        if (type.startsWith("REQ")) return SIZE_UNITS[2];
        if (type.startsWith("HLD")) return SIZE_UNITS[3];
        if (type.startsWith("DLD")) return SIZE_UNITS[4];
        return SIZE_UNITS[0];
    }


    /** Return the icon map for this process. */
    public Map getIconMap() {
        return iconMap;
    }


    /** Build a menu containing valid node types. */
    public JMenu getNodeTypeMenu() {
        return buildMenu();
    }


    /** Extract information about the phases in this process from the
     * given XML stream.  It should contain a process definition that
     * can be recognized by the CustomProcess class.
     */
    private void buildPhases(Element xml) {
        // open the custom process.
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
            // add each phase name to our list.
            phases.add(phase.name);
            // add each phase type to our map.
            phaseTypes.put(phase.name, phase.type);
        }
        // make these items immutable.
        phases = Collections.unmodifiableList(phases);
        phaseTypes = Collections.unmodifiableMap(phaseTypes);
    }


    /** Create icons for all the valid node types, and store them in the
     * icon map.
     */
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

        // create a spectrum of icons for each phase.
        int numPhases = phases.size();
        for (int phaseNum = 0; phaseNum < numPhases; phaseNum++) {
            String phase = (String) phases.get(phaseNum);
            String type = getPhaseType(phase);
            Color phaseColor = getPhaseColor(phaseNum, numPhases);
            iconMap.put(phase + " Task", IconFactory.getTaskIcon(phaseColor));

            // create icons for the various documents - match the
            // document color to the corresponding phase.
            if ("REQ".equals(type) && !iconMap.containsKey(REQ_DOC))
                iconMap.put(REQ_DOC, IconFactory.getDocumentIcon(phaseColor));
            if ("HLD".equals(type) && !iconMap.containsKey(HLD_DOC))
                iconMap.put(HLD_DOC, IconFactory.getDocumentIcon(phaseColor));
            if ("DLD".equals(type) && !iconMap.containsKey(DLD_DOC))
                iconMap.put(DLD_DOC, IconFactory.getDocumentIcon(phaseColor));
        }

        // If we didn't find any phases corresponding to the various
        // document types, create document icons with default colors.
        if (!iconMap.containsKey(REQ_DOC))
            iconMap.put(REQ_DOC,
                        IconFactory.getDocumentIcon(new Color(204, 204, 0)));
        if (!iconMap.containsKey(HLD_DOC))
            iconMap.put(HLD_DOC,
                        IconFactory.getDocumentIcon(new Color(153, 153, 255)));
        if (!iconMap.containsKey(DLD_DOC))
            iconMap.put(DLD_DOC,
                        IconFactory.getDocumentIcon(new Color(102, 255, 102)));

        // make the icon map immutable.
        iconMap = Collections.unmodifiableMap(iconMap);
    }

    private static final String SW_COMP = "Software Component";
    private static final String GEN_DOC = "General Document";
    private static final String REQ_DOC = "Requirements Document";
    private static final String HLD_DOC = "High Level Design Document";
    private static final String DLD_DOC = "Detailed Design Document";


    /** Calculate the appropriate color for displaying a particular phase. */
    private Color getPhaseColor(int phaseNum, int numPhases) {
        float r = (phaseNum * (COLOR_SPECTRUM.length-1)) / (float) numPhases;
        int offset = (int) Math.floor(r);
        r -= offset;

        return IconFactory.mixColors
            (COLOR_SPECTRUM[offset+1], COLOR_SPECTRUM[offset], r);
    }

    private static final Color[] COLOR_SPECTRUM = {
        Color.orange,
        Color.yellow,
        Color.green,
        Color.cyan,
        new Color(  0, 63, 255),   // blue
        new Color(170, 85, 255) }; // purple



    /** Build a menu containing all the valid node types.
     */
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


    // A list of menu items to put in the main icon menu.
    private static final String[] iconMenuItems =
        { SW_COMP, GEN_DOC, REQ_DOC, HLD_DOC, DLD_DOC };

    // This is hardcoded for now, and must agree with the list in
    // teamdash.wbs.columns.SizeTypeColumn
    private final String[] SIZE_UNITS = new String[] {
            "LOC","Text Pages", "Reqts Pages", "HLD Pages", "DLD Lines" };

}
