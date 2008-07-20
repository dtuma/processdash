// Copyright (C) 2001-2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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


package net.sourceforge.processdash.process;


import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLDepthFirstIterator;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/** Automatically defines data for a process, based on its XML template.
 */
public class TemplateAutoData extends AutoData {


    private transient Element template;
    private String templateStr = null;
    private String templateID, usesRollupID, definesRollupID;
    private String dataFile;
    private Map definitions = null;


    TemplateAutoData(Element template, String templateID, String usesRollupID,
                     String definesRollupID, String dataFile) {
        this.template = template;
        this.templateID = templateID;
        this.usesRollupID = usesRollupID;
        this.definesRollupID = definesRollupID;
        this.dataFile = dataFile;
    }

    /** Create the default data definitions for the template
     *  represented by this AutoData object.
     */
    public Map getDefinitions(DataRepository data) {
        if (definitions == null)
            buildDefaultData(data);

        return definitions;
    }

    private void buildDefaultData(DataRepository data) {
        //System.out.println("buildDefaultData for " + templateID);
        Map dataDefinitions = new HashMap();
        StringBuffer globalDataHeader = new StringBuffer(1024);
        globalDataHeader.append("#define TEMPLATE_ID ")
            .append(esc(templateID)).append("\n");
        if (XMLUtils.hasValue(usesRollupID))
            globalDataHeader.append("#define USES_ROLLUP_ID ")
                .append(esc(usesRollupID)).append("\n");
        if (XMLUtils.hasValue(definesRollupID))
            globalDataHeader.append("#define DEFINE_ROLLUP_ID ")
                .append(esc(definesRollupID)).append("\n");

        // does the process capture size metrics?
        if (!"none".equals(template.getAttribute(SIZE_METRIC_ATTR)))
            globalDataHeader.append("#define PROCESS_HAS_SIZE\n");
        // does the process capture defect metrics?
        // FIXME: the following "if" statement is breaking some
        // dashboard processes (e.g. generic, psp3cycle?)
        //if ("true".equalsIgnoreCase(template.getAttribute(DEFECTLOG_ATTR)))
            globalDataHeader.append("#define PROCESS_HAS_DEFECTS\n");

        // Go get the data about the phases in this process.
        PhaseLister phases = new PhaseLister();
        phases.run(template);
        phases.commit();

        // Were any phases found in this template?
        if (phases.all.size() > 0) {
            globalDataHeader.append("#define PROCESS_HAS_PHASES\n");
            dataDefinitions.put(PHASE_LIST_ELEM, phases.all);

            // Were any failure phases defined?
            if (phases.failure.size() > 0) {
                globalDataHeader.append("#define PROCESS_HAS_FAILURE\n");
                dataDefinitions.put(FAIL_LIST_ELEM, phases.failure);
                String lastFailurePhase =
                    (String) phases.failure.get(phases.failure.size()-1);
                globalDataHeader.append("#define LAST_FAILURE_PHASE ")
                    .append(esc(lastFailurePhase)).append("\n");

                // Yield is only meaningful if failure phases have been
                // defined - otherwise, it would always be 100%.
                if (phases.yield.size() > 0) {
                    globalDataHeader.append("#define PROCESS_HAS_YIELD\n");
                    dataDefinitions.put(YIELD_LIST_ELEM, phases.yield);
                }
            }

            // Were any appraisal phases defined?
            if (phases.appraisal.size() > 0) {
                globalDataHeader.append("#define PROCESS_HAS_APPRAISAL\n");
                dataDefinitions.put(APPR_LIST_ELEM, phases.appraisal);
            }

            // Were any quality phases defined?
            if (phases.quality.size() > 0) {
                globalDataHeader.append("#define PROCESS_HAS_QUALITY\n");
                dataDefinitions.put(QUAL_LIST_ELEM, phases.quality);
            }

            // Were any development phases defined?
            if (phases.development.size() > 0)
                dataDefinitions.put(DEVEL_LIST_ELEM, phases.development);

            // Were any overhead phases defined?
            if (phases.overhead.size() > 0)
                dataDefinitions.put(OVERHEAD_LIST_ELEM, phases.overhead);
        }

        buildDefaultData(template, "", data, dataDefinitions,
                         globalDataHeader.length(), globalDataHeader);

        // Fill in data for the entire process.
        StringBuffer processData = globalDataHeader;
        defineIterMacro(processData, "FOR_EACH_PHASE", phases.all);
        defineIterMacro(processData, "FOR_EACH_APPR_PHASE", phases.appraisal);
        defineIterMacro(processData, "FOR_EACH_FAIL_PHASE", phases.failure);
        defineIterMacro(processData, "FOR_EACH_YIELD_PHASE", phases.yield);
        data.putDefineDeclarations(dataFile, processData.toString());

        processData.append("\n").append(PROCESS_DATA);
        parseDefinitions(data, processData.toString(), dataDefinitions);

        definitions = dataDefinitions;
    }

    private void buildDefaultData(Element node, String path,
                                  DataRepository data, Map definitions,
                                  int globalDataLength,
                                  StringBuffer nodeDefinitions)
    {
        if (!isProcessNode(node)) return;
        if (noAutoDataNode(node)) return;

        // Check for a local redefinition of the Size metric.
        int localDataLength = globalDataLength;
        String sizeMetric = node.getAttribute(SIZE_METRIC_ATTR);
        if (XMLUtils.hasValue(sizeMetric)) {
            String localSizeDefn = "#define Size " + esc(sizeMetric) + "\n";
            nodeDefinitions.append(localSizeDefn);
            localDataLength += localSizeDefn.length();
        }

        // Iterate over the children of this element.
        NodeList children = node.getChildNodes();
        int numChildren = children.getLength();
        ListData childList = newEmptyList();
        Node c; Element child;
        for (int i=0;  i<numChildren;  i++) {
            c = children.item(i);
            if (c instanceof Element && isProcessNode((Element) c) &&
                !noAutoDataNode((Element) c)) {
                child = (Element) c;
                String childName =
                    pathConcat(path, child.getAttribute(NAME_ATTR));
                childList.add(childName);
                buildDefaultData(child, childName, data, definitions,
                                 localDataLength, nodeDefinitions);
            }
        }
        childList.setImmutable();

        // truncate the nodeDefinitions StringBuffer so it only
        // contains the global data definitions plus the local size
        // definition (probably unnecessary, but a wise and safe thing
        // to do).
        nodeDefinitions.setLength(localDataLength);

        if (XMLUtils.hasValue(path))
            nodeDefinitions.append("#define ").append(PATH_MACRO)
                .append(" ").append(esc(path)).append("\n");
        if (node.hasAttribute(DashHierarchy.IMAGINARY_NODE_ATTR))
            nodeDefinitions.append("#define IS_IMAGINARY_NODE\n");

        if (childList.size() > 0) {
            definitions.put(pathConcat(path, CHILD_LIST_ELEM), childList);
            nodeDefinitions.append(NODE_DATA);
        } else {
            // This is a phase.

            // #define various symbols based on the nature of this phase.
            String phaseType = node.getAttribute(PHASE_TYPE_ATTR);
            if (XMLUtils.hasValue(phaseType)) {
                phaseType = cleanupPhaseType(phaseType);
                nodeDefinitions.append("#define IS_")
                    .append(phaseType.toUpperCase()).append("_PHASE\n");
                if (isAppraisalPhaseType(phaseType))
                    nodeDefinitions.append("#define IS_APPRAISAL_PHASE\n")
                        .append("#define IS_QUALITY_PHASE\n");
                else if (isFailurePhaseType(phaseType))
                    nodeDefinitions.append("#define IS_FAILURE_PHASE\n")
                        .append("#define IS_QUALITY_PHASE\n");
                else if (isOverheadPhaseType(phaseType))
                    nodeDefinitions.append("#define IS_OVERHEAD_PHASE\n");
                else if (isDevelopmentPhaseType(phaseType))
                    nodeDefinitions.append("#define IS_DEVELOPMENT_PHASE\n");
            }

            nodeDefinitions.append(LEAF_DATA);
        }

        String finalData = nodeDefinitions.toString();
        if (!XMLUtils.hasValue(path))
            finalData = StringUtils.findAndReplace
                (finalData, PATH_MACRO + "/", "");
        /*
        System.out.println("For " + templateID+"->"+path + ", data is");
        System.out.println("------------------------------------------------");
        System.out.println(finalData);
        System.out.println("------------------------------------------------");
        */

        parseDefinitions(data, finalData, definitions);

        // truncate the nodeDefinitions StringBuffer so it only
        // contains the global data definitions.
        nodeDefinitions.setLength(globalDataLength);
    }

    private static void defineIterMacro(StringBuffer buf,
                                        String macroName,
                                        ListData list) {
        buf.append("\n#define ").append(macroName).append("(macro)");
        for (int i = 0;  i < list.size();  i++)
            buf.append(" macro(").append(list.get(i)).append(")");
        if (list.size() == 0)
            buf.append("/* */");
        buf.append("\n");
    }

    private static String cleanupPhaseType(String phaseType) {
        phaseType = StringUtils.findAndReplace(phaseType, " ", "");
        phaseType = StringUtils.findAndReplace(phaseType, "\t", "");
        return phaseType;
    }

    private static boolean isAppraisalPhaseType(String phaseType) {
        return PhaseUtil.isAppraisalPhaseType(phaseType); }
    private static boolean isFailurePhaseType(String phaseType) {
        return PhaseUtil.isFailurePhaseType(phaseType); }
    private static boolean isDevelopmentPhaseType(String phaseType) {
        return PhaseUtil.isDevelopmentPhaseType(phaseType); }
    private static boolean isOverheadPhaseType(String phaseType) {
        return PhaseUtil.isOverheadPhaseType(phaseType); }

    private static final String LEAF_DATA    = getFileContents("leafData.txt");
    private static final String NODE_DATA    = getFileContents("nodeData.txt");
    private static final String PROCESS_DATA =
        getFileContents("processData.txt");

    public static boolean isProcessNode(Element e) {
        String tagName = e.getTagName();
        return ("node".equals(tagName) ||
                "phase".equals(tagName) ||
                "template".equals(tagName));
    }
    public static boolean noAutoDataNode(Element e) {
        return "none".equals(e.getAttribute(DATA_EXTENT_ATTR));
    }


    private static final String PATH_MACRO = "PATH";
    private static final String CHILD_LIST_ELEM = "Child_List";
    private static final String YIELD_LIST_ELEM = "Yield_Phase_List";
    private static final String FAIL_LIST_ELEM  = "Failure_Phase_List";
    private static final String APPR_LIST_ELEM  = "Appraisal_Phase_List";
    private static final String OVERHEAD_LIST_ELEM  = "Overhead_Phase_List";
    private static final String DEVEL_LIST_ELEM  = "Development_Phase_List";
    private static final String QUAL_LIST_ELEM  = "Quality_Phase_List";
    private static final String PHASE_LIST_ELEM = "Phase_List";
    static final String PHASE_TYPE_ATTR = "type";



    /** Analyze an XML template to find the various phases in the process.
     */
    private class PhaseLister extends XMLDepthFirstIterator {

        ListData all, yield, appraisal, failure, quality, overhead, development, nodes;
        String lastNameSeen = null;

        public PhaseLister() {
            all = newEmptyList();
            yield = newEmptyList();
            appraisal = newEmptyList();
            failure = newEmptyList();
            quality = newEmptyList();
            overhead = newEmptyList();
            development = newEmptyList();
        }

        public void commit() {
            all.setImmutable();
            yield.setImmutable();
            appraisal.setImmutable();
            failure.setImmutable();
            quality.setImmutable();
            overhead.setImmutable();
            development.setImmutable();
        }

        public int getOrdering() { return POST; }

        public String getPathAttributeName(Element e) {
            return (isProcessNode(e) ? NAME_ATTR : null);
        }

        public void run(Node n, Stack path) {
            if (n instanceof Element && noAutoDataNode((Element) n))
                return;
            else
                super.run(n, path);
        }

        public void caseElement(Element e, List path) {
            if (isProcessNode(e)) {
                String nodeName = concatPath(path, false);
                if (nodeName.length() > 0 &&
                    (lastNameSeen == null ||
                     lastNameSeen.startsWith(nodeName+"/") == false)) {

                    // add this phase to the complete list.
                    all.add(nodeName);

                    // Determine whether this phase should be added to the
                    // appraisal, failure, and/or yield lists.
                    String phaseType =
                        e.getAttribute(PHASE_TYPE_ATTR);
                    if (isFailurePhaseType(phaseType)) {
                        failure.add(nodeName);
                        quality.add(nodeName);
                    } else if (isAppraisalPhaseType(phaseType)) {
                        appraisal.add(nodeName);
                        quality.add(nodeName);
                    } else if (isDevelopmentPhaseType(phaseType)) {
                        development.add(nodeName);
                    } else if (isOverheadPhaseType(phaseType)) {
                        overhead.add(nodeName);
                    }

                    if (failure.size() == 0) yield.add(nodeName);
                }
                lastNameSeen = nodeName;
            }
        }


        public String concatPath(List path, boolean includeFirst) {
            if (path.isEmpty()) return "";

            Iterator i = path.iterator();
            if (!includeFirst) i.next();

            StringBuffer buffer = new StringBuffer();
            String component;
            while (i.hasNext()) {
                component = (String) i.next();
                if (component != null && component.length() > 0)
                    buffer.append("/").append(component);
            }
            String result = buffer.toString();
            if (result.length() > 0)
                return result.substring(1);
            else
                return "";
        }
    }

    private void writeObject(java.io.ObjectOutputStream out)
        throws IOException
    {
        templateStr = XMLUtils.getAsText(template);
        out.defaultWriteObject();
    }
    private void readObject(java.io.ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        try {
            template = XMLUtils.parse(templateStr).getDocumentElement();
            templateStr = null;
        } catch (Exception e) {}
    }
}

