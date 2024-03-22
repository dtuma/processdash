// Copyright (C) 2002-2024 Tuma Solutions, LLC
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
import java.awt.Component;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.w3c.dom.Element;

import net.sourceforge.processdash.hier.ui.icons.HierarchyIcons;
import net.sourceforge.processdash.team.mcf.CustomProcess;
import net.sourceforge.processdash.team.mcf.CustomProcess.Item;


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

    /** True if this process is compatible with PSP tasks */
    private boolean pspCompatible;

    /** True if we should hide the "Software Component" node type */
    private boolean hideSoftwareComponentNodeType;

    /** A map from size metricIDs to SizeMetric objects */
    private Map<String, SizeMetric> sizeMetrics, sizeMetricsReadOnly;

    /** An immutable map of phase names to phase types. */
    private Map phaseTypes;

    /** A map of colors used for phase icons */
    private Map<String, Color> phaseColors;

    /** An immutable map of node types to node icons. */
    private Map iconMap;

    /** Maps of work product node types to size metrics */
    private Map workProductSizes, workProductSizesReadOnly;

    /** Maps of the size metric for each phase type */
    private Map phaseSizeMetrics, phaseSizeMetricsReadOnly;

    /** An immutable map of the estimated yields for each phase */
    private Map<String, Double> phaseYields;

    /** An immutable map of the est defect injection rates for each phase */
    private Map<String, Double> phaseInjRates;


    /** Contruct a team process from information in the given XML element.
     */
    public TeamProcess(Element xml) {
        loadProcessData(xml);
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

    public boolean isPspCompatible() {
        return pspCompatible;
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
        String result = (String) phaseSizeMetricsReadOnly.get(phase);
        if (result == null)
            result = "LOC";
        return result;
    }

    /** For a task of a given phase, what is the default estimated yield?
     * This will return a number between 0.0 and 1.0
     */
    public double getPhaseEstYield(String phase) {
        if (phase != null && phase.endsWith(" Task"))
            phase = phase.substring(0, phase.length()-5);
        Double result = phaseYields.get(phase);
        return (result == null ? 0 : result);
    }

    /** For a task of a given phase, what is the default estimated number of
     * defects injected per hour?
     */
    public double getPhaseEstDefectInjectionRate(String phase) {
        if (phase != null && phase.endsWith(" Task"))
            phase = phase.substring(0, phase.length()-5);
        Double result = phaseInjRates.get(phase);
        return (result == null ? 0 : result);
    }

    /** @deprecated */
    public String[] getSizeMetrics() {
        // This method is obsolete - all clients must use getSizeMetricsMap for
        // proper operation going forward. This temporary stub implementation
        // will prevent those clients from breaking until they can be recoded.
        return new String[] { "LOC" };
    }

    /**
     * @return a map of the known size metrics. The keys in the map are
     *         metricIDs, and the values are metric names.
     */
    public Map<String, SizeMetric> getSizeMetricMap() {
        return sizeMetricsReadOnly;
    }

    void setSizeMetricMaps(Map<String, SizeMetric> newSizeMetrics,
            Map<String, String> newWorkProductSizeMap,
            Map<String, String> newPhaseSizeMap) {
        // load the new metrics into our own map (rather than replacing the
        // reference altogether) so clients don't have to re-fetch the map.
        sizeMetrics.clear();
        sizeMetrics.putAll(newSizeMetrics);

        // update the size maps for phases and work products
        updateProcessSizeMetricMap(workProductSizes, newWorkProductSizeMap);
        updateProcessSizeMetricMap(phaseSizeMetrics, newPhaseSizeMap);
    }

    private void updateProcessSizeMetricMap(Map<String, String> dest,
            Map<String, String> src) {
        for (Entry<String, String> e : dest.entrySet()) {
            String key = e.getKey();
            String newValue = src.get(key);
            e.setValue(newValue == null ? "LOC" : newValue);
        }
    }

    public Map getWorkProductSizeMap() {
        return workProductSizesReadOnly;
    }

    public Map getPhaseSizeMap() {
        return phaseSizeMetricsReadOnly;
    }


    /** Return the icon map for this process. */
    public Map getIconMap() {
        if (iconMap == null)
            buildIcons();
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

        this.pspCompatible = process.isPspCompatible();
        this.hideSoftwareComponentNodeType = process
                .getParamValue("hideSoftwareComponent") != null;

        buildSizeInfo(process);
        buildPhases(process);
    }

    private List<CustomProcess.Item> sizeMetricsItems;
    private boolean usingDefaultSizeMetrics;
    private boolean usesDLDLines;

    private void buildSizeInfo(CustomProcess process) {
        workProductSizes = new HashMap();
        workProductSizes.put(PROJECT_TYPE, "LOC");
        workProductSizes.put(COMPONENT_TYPE, "LOC");
        workProductSizes.put(SOFTWARE_COMPONENT_TYPE, "LOC");
        workProductSizes.put(PSP_TASK_TYPE, "LOC");
        workProductSizes.put(CODE_TASK_TYPE, "LOC");

        sizeMetrics = new LinkedHashMap<String, SizeMetric>();
        sizeMetrics.put("LOC", new SizeMetric("LOC", "LOC"));

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

        for (CustomProcess.Item metric : sizeMetricsItems) {
            String name = metric.getAttr(CustomProcess.NAME);
            String productName = metric.getAttr(PRODUCT_NAME);
            if (productName != null)
                workProductSizes.put(productName, name);
            sizeMetrics.put(name, new SizeMetric(name, name));
        }

        workProductSizesReadOnly = Collections.unmodifiableMap(workProductSizes);
        sizeMetricsReadOnly = Collections.unmodifiableMap(sizeMetrics);
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
        result.getAttributes().put(PRODUCT_NAME, productName);
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
        phaseColors = new HashMap();
        phaseSizeMetrics = new HashMap();
        phaseYields = new HashMap();
        phaseInjRates = new HashMap();
        Properties phaseDataDefaults = getPhaseDataDefaults();
        boolean usePhaseColors = shouldUsePhaseColorAttrs(process);

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
            // store the phase color if one was provided
            String phaseColor = phase.getAttr(CustomProcess.COLOR);
            if (usePhaseColors && phaseColor != null)
                phaseColors.put(phaseName, Color.decode(phaseColor));
            // add the size metric for the phase to our map
            String sizeMetric = phase.getAttr(CustomProcess.SIZE_METRIC);
            if (sizeMetric == null) {
                if (usingDefaultSizeMetrics)
                    sizeMetric = supplyDefaultSizeMetric(phaseType);
            } else if (sizeMetric.startsWith(INSPECTED_PREFIX))
                sizeMetric = sizeMetric.substring(INSPECTED_PREFIX.length());
            phaseSizeMetrics.put(phaseName, sizeMetric);
            // store quality metrics for this phase
            phaseYields.put(phaseName, getPhaseDoubleParam(phase,
                phaseDataDefaults, phaseType, CustomProcess.EST_YIELD));
            phaseInjRates.put(phaseName, getPhaseDoubleParam(phase,
                phaseDataDefaults, phaseType, CustomProcess.EST_INJ_RATE));
        }
        phaseTypes.put("PROBE", "PLAN");
        // make these items immutable.
        phases = Collections.unmodifiableList(phases);
        phaseTypes = Collections.unmodifiableMap(phaseTypes);
        phaseSizeMetricsReadOnly = Collections.unmodifiableMap(phaseSizeMetrics);
        phaseYields = Collections.unmodifiableMap(phaseYields);
        phaseInjRates = Collections.unmodifiableMap(phaseInjRates);
    }

    private Properties getPhaseDataDefaults() {
        Properties result = new Properties();
        try {
            InputStream in = TeamProcess.class
                    .getResourceAsStream("default-phase-data.txt");
            result.load(in);
            in.close();
        } catch (Exception e) {}
        return result;
    }

    private boolean shouldUsePhaseColorAttrs(CustomProcess process) {
        // see if this process has specified the use of color attributes
        for (Item param : process.getItemList(CustomProcess.PARAM_ITEM)) {
            String paramName = param.getAttr(CustomProcess.NAME);
            if (USE_PHASE_COLORS_PARAM.equals(paramName))
                return true;
        }

        // by default, do not use color attributes for the WBS
        return false;
    }

    private String supplyDefaultSizeMetric(String type) {
        if (type.startsWith("DOC")) return TEXT_UNITS;
        if (type.startsWith("REQ")) return REQ_UNITS;
        if (type.startsWith("HLD")) return HLD_UNITS;
        if (type.startsWith("DLD") && usesDLDLines) return DLD_UNITS;
        return null;
    }

    private Double getPhaseDoubleParam(CustomProcess.Item phase,
            Properties defaults, String phaseType, String attr) {
        String value = phase.getAttr(attr);
        if (value == null)
            value = defaults.getProperty(phaseType.toUpperCase() + "." + attr);

        try {
            if (value != null)
                return Double.valueOf(value);
        } catch (Exception e) {
        }
        return null;
    }


    /** Create icons for all the valid node types, and store them in the
     * icon map.
     */
    private void buildIcons() {
        iconMap = new HashMap();

        // create a handful of icons that always exist.
        iconMap.put(PROJECT_TYPE, HierarchyIcons.getProjectIcon());
        iconMap.put(COMPONENT_TYPE, HierarchyIcons.getComponentIcon());
        iconMap.put(SOFTWARE_COMPONENT_TYPE, HierarchyIcons.getSoftwareComponentIcon());
        iconMap.put(WORKFLOW_TYPE, IconFactory.getWorkflowIcon());
        iconMap.put(PSP_TASK_TYPE, HierarchyIcons.getPSPTaskIcon());
        iconMap.put(PSP_WORKFLOW_TASK_TYPE, HierarchyIcons.getPSPTaskIcon(Color.white));
        iconMap.put(PROBE_TASK_TYPE, HierarchyIcons.getProbeTaskIcon());
        iconMap.put(null, HierarchyIcons.getTaskIcon());

        Map defaultSizeIconColors = new HashMap();

        // create a spectrum of icons for each phase.
        int numPhases = phases.size();
        for (int phaseNum = 0; phaseNum < numPhases; phaseNum++) {
            String phase = (String) phases.get(phaseNum);
            Color phaseColor = phaseColors.get(phase);
            if (phaseColor == null) {
                phaseColor = getPhaseColor(phaseNum, numPhases);
                phaseColors.put(phase, phaseColor);
            }
            iconMap.put(phase + TASK_SUFFIX,
                HierarchyIcons.getTaskIcon(phaseColor));
            iconMap.put(phase + WORKFLOW_TASK_SUFFIX,
                HierarchyIcons.getWorkflowTaskIcon(phaseColor));

            // keep track of a likely color to use for each size metric icon.
            String sizeMetricName = getPhaseSizeMetric(phase);
            if (!defaultSizeIconColors.containsKey(sizeMetricName))
                defaultSizeIconColors.put(sizeMetricName, phaseColor);
        }

        // create icons for the various work products.
        for (CustomProcess.Item sizeMetric : sizeMetricsItems) {
            String productName = sizeMetric.getAttr(PRODUCT_NAME);
            if (productName != null)
                iconMap.put(productName,
                    getWorkProductIcon(defaultSizeIconColors, sizeMetric));
        }

        // register all icons for zoom-level awareness
        for (Object icon : iconMap.values()) {
            WBSZoom.icon(icon);
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
            return HierarchyIcons.getDocumentIcon(c);
        else
            return HierarchyIcons.getComponentIcon(c);
    }


    /** Calculate the appropriate color for displaying a particular phase. */
    public static Color getPhaseColor(int phaseNum, int numPhases) {
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
        JMenu taskSubmenu = new NodeTypeMenu("Tasks");

        JMenu nodeTypeMenu = new NodeTypeMenu();
        nodeTypeMenu.add(taskSubmenu);
        nodeTypeMenu.addSeparator();
        nodeTypeMenu.add(new JMenuItem(COMPONENT_TYPE));
        if (!hideSoftwareComponentNodeType)
            nodeTypeMenu.add(new JMenuItem(SOFTWARE_COMPONENT_TYPE));
        for (CustomProcess.Item item : sizeMetricsItems) {
            String productName = item.getAttr(PRODUCT_NAME);
            if (productName != null)
                nodeTypeMenu.add(new JMenuItem(productName));
        }

        if (pspCompatible)
            taskSubmenu.add(new JMenuItem(PSP_TASK_TYPE));
        Iterator i = phases.iterator();
        while (i.hasNext()) {
            if (taskSubmenu.getItemCount() >= 15) {
                JMenu moreTaskMenu = new NodeTypeMenu("More...");
                taskSubmenu.add(moreTaskMenu, 0);
                taskSubmenu = moreTaskMenu;
            }
            taskSubmenu.add(new JMenuItem(i.next() + " Task"));
        }

        WBSZoom.get().manage(nodeTypeMenu, "font");
        return nodeTypeMenu;
    }

    public static class NodeTypeMenu extends JMenu {
        
        public NodeTypeMenu() {}

        public NodeTypeMenu(String text) { super(text); }

        @Override
        public void setFont(Font font) {
            super.setFont(font);
            for (int i = getMenuComponentCount(); i-- > 0;) {
                Component c = getMenuComponent(i);
                if (c instanceof JMenu || c instanceof JMenuItem)
                    c.setFont(font);
            }
        }
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
        if (!hideSoftwareComponentNodeType)
            result.add(SOFTWARE_COMPONENT_TYPE);
        for (CustomProcess.Item item : sizeMetricsItems) {
            String productName = item.getAttr(PRODUCT_NAME);
            if (productName != null)
                result.add(productName);
        }

        if (pspCompatible)
            result.add(PSP_TASK_TYPE);
        Iterator i = phases.iterator();
        while (i.hasNext())
            result.add(i.next() + " Task");

        return result;
    }


    static final String PROJECT_TYPE = "Project";
    public static final String COMPONENT_TYPE = "Component";
    static final String SOFTWARE_COMPONENT_TYPE = "Software Component";
    static final String WORKFLOW_TYPE = "Workflow";
    static final String PROBE_TASK_TYPE = "PROBE Task";
    static final String PSP_TASK_TYPE = "PSP Task";
    static final String PSP_WORKFLOW_TASK_TYPE = "PSP Workflow Task";
    static final String CODE_TASK_TYPE = "Code Task";
    public static final String TASK_SUFFIX = " Task";
    static final String WORKFLOW_TASK_SUFFIX = " Workflow Task";

    private static final String PRODUCT_NAME = CustomProcess.PRODUCT_NAME;
    private static final String DLD_DOCUMENT_TYPE = "Detailed Design Document";

    private static final String INSPECTED_PREFIX = "Inspected ";
    private static final String USE_PHASE_COLORS_PARAM = "wbsUsesPhaseColors";

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
                && !type.equals(ProxyWBSModel.PROXY_TYPE)
                && !type.equals(ProxyWBSModel.BUCKET_TYPE)
                && !type.startsWith(SizeMetricsWBSModel.SIZE_METRIC_TYPE)
                && !type.endsWith("Milestone"));
    }
}
