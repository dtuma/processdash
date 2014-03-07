// Copyright (C) 2002-2014 Tuma Solutions, LLC
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

    /** The name of the team process */
    private String processName;
    /** The version number of the team process */
    private String processVersion;
    /** The name/version ID of the team process */
    private String processID;

    /** An immutable list of the names of the phases in this
     * process (Strings) */
    private List phases;

    /** An immutable list of the names of the size metrics in this
     * process (Strings) */
    private List sizeMetrics;

    /** An immutable map of phase names to phase types. */
    private Map phaseTypes;

    /** An immutable map of node types to node icons. */
    private Map iconMap;

    /** An immutable map of work product node types to size metrics */
    private Map workProductSizes;

    /** An immutable map of the size metric for each phase type */
    private Map phaseSizeMetrics;


    /** Contruct a team process from information in the given XML element.
     */
    public TeamProcess(Element xml) {
        loadProcessData(xml);
        buildIcons();
    }


    public String getProcessName() {
        return processName;
    }


    public String getProcessVersion() {
        return processVersion;
    }

    public String getProcessID() {
        return processID;
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
        if (phase != null && phase.endsWith(" Task"))
            phase = phase.substring(0, phase.length()-5);
        String result = (String) phaseSizeMetrics.get(phase);
        if (result == null)
            result = "LOC";
        return result;
    }

    public String[] getSizeMetrics() {
        return (String[]) sizeMetrics.toArray(new String[0]);
    }

    public Map getWorkProductSizeMap() {
        return workProductSizes;
    }


    /** Return the icon map for this process. */
    public Map getIconMap() {
        return iconMap;
    }


    /** Build a menu containing valid node types. */
    public JMenu getNodeTypeMenu() {
        return buildMenu();
    }

    /** Extract information about the name of this process from the xml.
     */
    private void loadProcessData(Element xml) {
        this.processName = "Unknown";
        this.processVersion = "Unknown";

        // open the custom process.
        CustomProcess process = null;
        if (xml != null) try {
            process = new CustomProcess(xml);
            this.processName = process.getName();
            this.processVersion = process.getVersion();
            this.processID = process.getProcessID();
        } catch (IOException ioe) {}
        if (process == null)
            process = new CustomProcess();

        buildSizeInfo(process);
        buildPhases(process);
    }

    private List sizeMetricsItems;
    private boolean usingDefaultSizeMetrics;
    private boolean usesDLDLines;

    private void buildSizeInfo(CustomProcess process) {
        workProductSizes = new HashMap();
        workProductSizes.put(PROJECT_TYPE, "LOC");
        workProductSizes.put(COMPONENT_TYPE, "LOC");
        workProductSizes.put(SOFTWARE_COMPONENT_TYPE, "LOC");
        workProductSizes.put(PSP_TASK_TYPE, "LOC");
        workProductSizes.put(CODE_TASK_TYPE, "LOC");

        sizeMetrics = new ArrayList();
        sizeMetrics.add("LOC");

        usingDefaultSizeMetrics = false;
        sizeMetricsItems = process.getItemList(CustomProcess.SIZE_METRIC);
        if (sizeMetricsItems == null || sizeMetricsItems.isEmpty()) {
            sizeMetricsItems = generateDefaultSizeMetrics(process);
            usingDefaultSizeMetrics = true;
        }
        usesDLDLines = processUsesDLDLines(process, usingDefaultSizeMetrics);
        if (usesDLDLines)
            sizeMetricsItems.add(newMetric(process, DLD_UNITS,
                    DLD_DOCUMENT_TYPE, null));

        Iterator i = sizeMetricsItems.iterator();
        while (i.hasNext()) {
            CustomProcess.Item metric = (CustomProcess.Item) i.next();
            String name = metric.getAttr(CustomProcess.NAME);
            String productName = metric.getAttr("productName");
            if (productName == null) {
                productName = "Item (" + name + ")";
                metric.getAttributes().put("productName", productName);
            }

            workProductSizes.put(productName, name);
            sizeMetrics.add(name);
        }

        workProductSizes = Collections.unmodifiableMap(workProductSizes);
        sizeMetrics = Collections.unmodifiableList(sizeMetrics);
    }

    private List generateDefaultSizeMetrics(CustomProcess p) {
        List result = new ArrayList();
        for (int i = 0; i < DEFAULT_SIZE_METRIC_LIST.length; i++)
            result.add(newMetric(p, DEFAULT_SIZE_METRIC_LIST[i][0],
                    DEFAULT_SIZE_METRIC_LIST[i][1],
                    DEFAULT_SIZE_METRIC_LIST[i][2]));
        return result;
    }

    private CustomProcess.Item newMetric(CustomProcess process, String name,
            String productName, String iconColor) {
        CustomProcess.Item result = process.new Item("sizeMetric");
        result.getAttributes().put("name", name);
        result.getAttributes().put("productName", productName);
        result.getAttributes().put("iconStyle", "document");
        result.getAttributes().put("iconColor", iconColor);
        return result;
    }

    private boolean processUsesDLDLines(CustomProcess process,
            boolean mightBeLegacy) {
        boolean sawSizeMetric = false;
        Iterator i = process.getItemList(CustomProcess.PHASE_ITEM).iterator();
        while (i.hasNext()) {
            CustomProcess.Item phase =
                (CustomProcess.Item) i.next();
            String sizeMetric = phase.getAttr(CustomProcess.SIZE_METRIC);
            if (DLD_UNITS.equals(sizeMetric))
                return true;
            else if (sizeMetric != null && sizeMetric.length() > 0)
                sawSizeMetric = true;
        }

        if (sawSizeMetric == false && mightBeLegacy)
            // this process doesn't mention size metrics at all.  It's probably
            // an older process definition.  To preserve legacy operations,
            // return true.
            return true;

        else
            // this process definition does mention size metrics, but none
            // of them were DLD Lines.
            return false;
    }

    /** Extract information from the list of phases in this process.
     */
    private void buildPhases(CustomProcess process) {
        phases = new ArrayList();
        phaseTypes = new HashMap();
        phaseSizeMetrics = new HashMap();

        Iterator i = process.getItemList(CustomProcess.PHASE_ITEM).iterator();
        while (i.hasNext()) {
            CustomProcess.Item phase =
                (CustomProcess.Item) i.next();
            // add each phase name to our list.
            String phaseName = phase.getAttr(CustomProcess.NAME);
                        phases.add(phaseName);
            // add each phase type to our map.
            String phaseType = phase.getAttr(CustomProcess.TYPE);
            phaseTypes.put(phaseName, phaseType);
            // add the size metric for the phase to our map
            String sizeMetric = phase.getAttr(CustomProcess.SIZE_METRIC);
            if (sizeMetric == null) {
                if (usingDefaultSizeMetrics)
                    sizeMetric = supplyDefaultSizeMetric(phaseType);
            } else if (sizeMetric.startsWith(INSPECTED_PREFIX))
                sizeMetric = sizeMetric.substring(INSPECTED_PREFIX.length());
            phaseSizeMetrics.put(phaseName, sizeMetric);
        }
        phaseTypes.put("PROBE", "PLAN");
        // make these items immutable.
        phases = Collections.unmodifiableList(phases);
        phaseTypes = Collections.unmodifiableMap(phaseTypes);
        phaseSizeMetrics = Collections.unmodifiableMap(phaseSizeMetrics);
    }

    private String supplyDefaultSizeMetric(String type) {
        if (type.startsWith("DOC")) return TEXT_UNITS;
        if (type.startsWith("REQ")) return REQ_UNITS;
        if (type.startsWith("HLD")) return HLD_UNITS;
        if (type.startsWith("DLD") && usesDLDLines) return DLD_UNITS;
        return null;
    }


    /** Create icons for all the valid node types, and store them in the
     * icon map.
     */
    private void buildIcons() {
        iconMap = new HashMap();

        // create a handful of icons that always exist.
        Color c = new Color(204, 204, 255);
        iconMap.put(PROJECT_TYPE, IconFactory.getProjectIcon());
        iconMap.put(COMPONENT_TYPE, IconFactory.getComponentIcon());
        iconMap.put(SOFTWARE_COMPONENT_TYPE, IconFactory.getSoftwareComponentIcon());
        iconMap.put(WORKFLOW_TYPE, IconFactory.getWorkflowIcon());
        iconMap.put(PSP_TASK_TYPE, IconFactory.getPSPTaskIcon(c));
        iconMap.put(PROBE_TASK_TYPE, IconFactory.getProbeTaskIcon());
        iconMap.put(null, IconFactory.getTaskIcon(c));

        Map defaultSizeIconColors = new HashMap();

        // create a spectrum of icons for each phase.
        int numPhases = phases.size();
        for (int phaseNum = 0; phaseNum < numPhases; phaseNum++) {
            String phase = (String) phases.get(phaseNum);
            Color phaseColor = getPhaseColor(phaseNum, numPhases);
            iconMap.put(phase + TASK_SUFFIX,
                IconFactory.getTaskIcon(phaseColor));
            iconMap.put(phase + WORKFLOW_TASK_SUFFIX,
                IconFactory.getWorkflowTaskIcon(phaseColor));

            // keep track of a likely color to use for each size metric icon.
            String sizeMetricName = getPhaseSizeMetric(phase);
            if (!defaultSizeIconColors.containsKey(sizeMetricName))
                defaultSizeIconColors.put(sizeMetricName, phaseColor);
        }

        // create icons for the various work products.
        for (Iterator i = sizeMetricsItems.iterator(); i.hasNext();) {
            CustomProcess.Item sizeMetric = (CustomProcess.Item) i.next();
            String productName = sizeMetric.getAttr("productName");
            Object icon = getWorkProductIcon(defaultSizeIconColors, sizeMetric);
            iconMap.put(productName, icon);
        }

        // make the icon map immutable.
        iconMap = Collections.unmodifiableMap(iconMap);
    }

    private static final Object getWorkProductIcon(Map defaultSizeIconColors,
            CustomProcess.Item sizeMetric) {
        Color c = null;
        try {
            // if the process definition specified an icon color, use it.
            c = Color.decode(sizeMetric.getAttr("iconColor"));
        } catch (Exception e) {
            // otherwise, choose a color that corresponds to the first
            // phase that uses that size metric.
            String sizeMetricName = sizeMetric.getAttr(CustomProcess.NAME);
            c = (Color) defaultSizeIconColors.get(sizeMetricName);
        }
        if (c == null)
            // if all else fails, pick a color.
            c = Color.lightGray;

        // now, create the icon and save it in our map.
        if ("document".equalsIgnoreCase(sizeMetric.getAttr("iconStyle")))
            return IconFactory.getDocumentIcon(c);
        else
            return IconFactory.getComponentIcon(c);
    }


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
        nodeTypeMenu.add(new JMenuItem(COMPONENT_TYPE));
        nodeTypeMenu.add(new JMenuItem(SOFTWARE_COMPONENT_TYPE));
        for (Iterator i = sizeMetricsItems.iterator(); i.hasNext();) {
            CustomProcess.Item item = (CustomProcess.Item) i.next();
            nodeTypeMenu.add(new JMenuItem(item.getAttr("productName")));
        }

        taskSubmenu.add(new JMenuItem(PSP_TASK_TYPE));
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

    /** Return a list of node types that the user can choose from when selecting
     * the type of a node.
     * 
     * This mirrors the node types presented in the icon menu, but is returned
     * as a simple list of strings instead of a JMenu.
     */
    public List getChoosableNodeTypes() {
        List result = new ArrayList();

        result.add(COMPONENT_TYPE);
        result.add(SOFTWARE_COMPONENT_TYPE);
        for (Iterator i = sizeMetricsItems.iterator(); i.hasNext();) {
            CustomProcess.Item item = (CustomProcess.Item) i.next();
            result.add(item.getAttr("productName"));
        }

        result.add(PSP_TASK_TYPE);
        Iterator i = phases.iterator();
        while (i.hasNext())
            result.add(i.next() + " Task");

        return result;
    }


    static final String PROJECT_TYPE = "Project";
    static final String COMPONENT_TYPE = "Component";
    static final String SOFTWARE_COMPONENT_TYPE = "Software Component";
    static final String WORKFLOW_TYPE = "Workflow";
    static final String PROBE_TASK_TYPE = "PROBE Task";
    static final String PSP_TASK_TYPE = "PSP Task";
    static final String CODE_TASK_TYPE = "Code Task";
    static final String TASK_SUFFIX = " Task";
    static final String WORKFLOW_TASK_SUFFIX = " Workflow Task";

    private static final String DLD_DOCUMENT_TYPE = "Detailed Design Document";

    private static final String INSPECTED_PREFIX = "Inspected ";

    private static final String TEXT_UNITS = "Text Pages";
    private static final String REQ_UNITS = "Req Pages";
    private static final String HLD_UNITS = "HLD Pages";
    private static final String DLD_UNITS = "DLD Lines";

    private static final String[][] DEFAULT_SIZE_METRIC_LIST = {
        { TEXT_UNITS, "General Document", "#ffffff" },
        { REQ_UNITS, "Requirements Document", null },
        { HLD_UNITS, "High Level Design Document", null },
    };

    public static boolean isLOCNode(String type) {
        return SOFTWARE_COMPONENT_TYPE.equalsIgnoreCase(type)
                || COMPONENT_TYPE.equalsIgnoreCase(type)
                || PROJECT_TYPE.equalsIgnoreCase(type)
                || WORKFLOW_TYPE.equalsIgnoreCase(type)
                || PSP_TASK_TYPE.equalsIgnoreCase(type)
                || CODE_TASK_TYPE.equalsIgnoreCase(type);
    }

    public static boolean isProbeTask(String type) {
        return PROBE_TASK_TYPE.equalsIgnoreCase(type);
    }

    public static boolean isPSPTask(String type) {
        return PSP_TASK_TYPE.equalsIgnoreCase(type);
    }

    public static boolean isCodeTask(String type) {
        return CODE_TASK_TYPE.equalsIgnoreCase(type);
    }

    public static boolean isOtherSizeType(String type) {
        return (!isLOCNode(type) && !type.endsWith(" Task")
                && !type.endsWith("Milestone"));
    }
}
