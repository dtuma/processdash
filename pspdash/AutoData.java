// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import pspdash.data.DataRepository;
import pspdash.data.DefinitionFactory;
import pspdash.data.ListData;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import org.w3c.dom.Document;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/** Automatically defines data for a process, based on its XML template.
 */
public class AutoData implements DefinitionFactory {

    /** Create and register AutoData objects for all the templates in
     *the XML document.
     *
     * Note: This may intentionally modify the following features of the
     * XML document:
     * <UL>
     * <LI>Convert the ID attribute of template elements to canonical form.
     * <LI>Add a "dataFile" attribute to template elements if it is missing.
     * </UL>
     * Therefore, it should probably be called <b>before</b> you examine
     * these values.
     */
    public static void registerTemplates(Element e,
                                         DataRepository data) {
        NodeList templates = e.getElementsByTagName(TEMPLATE_NODE_NAME);
        for (int i = templates.getLength();  i-- > 0; )
            registerTemplate((Element) templates.item(i), data);
    }

    /** Create and register an AutoData object for a single XML template.
     */
    public static void registerTemplate(Element template, DataRepository data)
    {
        String dataFile = template.getAttribute(DATAFILE_ATTR);
        if (NO_DATAFILE.equalsIgnoreCase(dataFile) ||
            NO_DATAFILE.equalsIgnoreCase(template.getAttribute
                                         (DATA_EXTENT_ATTR)))
            // the user has requested that we do not create a datafile
            // for this template.
            return;

        String templateID = template.getAttribute(ID_ATTR);
        if (!XMLUtils.hasValue(templateID)) {
            templateID = template.getAttribute(NAME_ATTR);
            template.setAttribute(ID_ATTR, templateID);
        }

        // If the user did not specify a datafile name, invent an imaginary
        // datafile on their behalf.
        boolean isImaginary = !XMLUtils.hasValue(dataFile);
        if (isImaginary) {
            dataFile = templateID + "?dataFile.txt";
            template.setAttribute(DATAFILE_ATTR, dataFile);
        }

        // create an AutoData object and register it with the DataRepository.
        AutoData result = new AutoData(template, templateID, dataFile);
        data.registerDefaultData(result, dataFile, isImaginary);
    }


    private Element template;
    private String templateID;
    private String dataFile;
    private Map definitions = null;

    private AutoData(Element template, String templateID, String dataFile) {
        this.template = template;
        this.templateID = templateID;
        this.dataFile = dataFile;
    }

    /** Create the default data definitions for the template
     *  represented by this AutoData object.
     */
    public Map getDefinitions(DataRepository data) {
        if (definitions != null) return definitions;

        Map dataDefinitions = new HashMap();
        StringBuffer globalDataHeader = new StringBuffer(1024);
        globalDataHeader.append("#define TEMPLATE_ID ")
            .append(esc(templateID)).append("\n");
        // Eventually, we will look at the template and dynamically
        // determine whether or not to #define the next two tokens,
        // but for now we'll just do it.
        globalDataHeader.append("#define PROCESS_HAS_SIZE\n")
            .append("#define PROCESS_HAS_DEFECTS\n");

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
        return dataDefinitions;
    }

    private void buildDefaultData(Element node, String path,
                                  DataRepository data, Map definitions,
                                  int globalDataLength,
                                  StringBuffer nodeDefinitions)
    {
        if (!isProcessNode(node)) return;

        // Iterate over the children of this element.
        NodeList children = node.getChildNodes();
        ListData childList = newEmptyList();
        Node c; Element child;
        for (int i=0;  i<children.getLength();  i++) {
            c = children.item(i);
            if (c instanceof Element && isProcessNode((Element) c)) {
                child = (Element) c;
                String childName =
                    pathConcat(path, child.getAttribute(NAME_ATTR));
                childList.add(childName);
                buildDefaultData(child, childName, data, definitions,
                                 globalDataLength, nodeDefinitions);
            }
        }
        childList.setImmutable();

        // truncate the nodeDefinitions StringBuffer so it only
        // contains the global data definitions (probably unnecessary,
        // but a wise and safe thing to do).
        nodeDefinitions.setLength(globalDataLength);

        if (XMLUtils.hasValue(path))
            nodeDefinitions.append("#define ").append(PATH_MACRO)
                .append(" ").append(esc(path)).append("\n");
        if (node.hasAttribute(PSPProperties.IMAGINARY_NODE_ATTR))
            nodeDefinitions.append("#define IS_IMAGINARY_NODE\n");

        if (childList.size() > 0) {
            definitions.put(pathConcat(path, CHILD_LIST_ELEM), childList);
            nodeDefinitions.append(NODE_DATA);
        } else {
            // This is a phase.

            // #define various symbols based on the nature of this phase.
            String phaseType = node.getAttribute(AutoData.PHASE_TYPE_ATTR);
            if (XMLUtils.hasValue(phaseType)) {
                phaseType = cleanupPhaseType(phaseType);
                nodeDefinitions.append("#define IS_")
                    .append(phaseType.toUpperCase()).append("_PHASE\n");
                if (isAppraisal(phaseType))
                    nodeDefinitions.append("#define IS_APPRAISAL_PHASE\n")
                        .append("#define IS_QUALITY_PHASE\n");
                else if (isFailure(phaseType))
                    nodeDefinitions.append("#define IS_FAILURE_PHASE\n")
                        .append("#define IS_QUALITY_PHASE\n");
                else if (isOverhead(phaseType))
                    nodeDefinitions.append("#define IS_OVERHEAD_PHASE\n");
                else if (isDevelopment(phaseType))
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

    private void parseDefinitions(DataRepository data,
                                  String definitions, Map dest) {
        try {
            data.parseDatafile(definitions, dest);
        } catch (Exception e) {
            System.err.println("Exception when generating default data: " + e);
            System.err.println("Datafile BEG:-------------------------------");
            System.err.println(definitions);
            System.err.println("Datafile END:-------------------------------");
        }
    }

    private static void defineIterMacro(StringBuffer buf,
                                        String macroName,
                                        ListData list) {
        buf.append("#define ").append(macroName).append("(macro)");
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

    private static final String[] APPR_TYPES = {
        "appraisal","reqinsp","hldr","hldrinsp","dldr","dldinsp","cr","codeinsp" };
    private static final Set APPR_TYPE_SET =
        Collections.unmodifiableSet(new HashSet(Arrays.asList(APPR_TYPES)));

    static boolean isAppraisal(String type) {
        return APPR_TYPE_SET.contains(type.toLowerCase());
    }

    private static final String[] FAIL_TYPES = {
        "failure", "comp", "ut", "it", "st", "at" };
    private static final Set FAIL_TYPE_SET =
        Collections.unmodifiableSet(new HashSet(Arrays.asList(FAIL_TYPES)));

    static boolean isFailure(String type) {
        return FAIL_TYPE_SET.contains(type.toLowerCase());
    }

    private static final String[] OVER_TYPES = {
        "mgmt", "strat", "plan", "pm" };
    private static final Set OVER_TYPE_SET =
        Collections.unmodifiableSet(new HashSet(Arrays.asList(OVER_TYPES)));

    static boolean isOverhead(String type) {
        return OVER_TYPE_SET.contains(type.toLowerCase());
    }

    private static final String[] DEV_TYPES = {
        "req", "stp", "itp", "td", "hld", "dld", "code", "pl", "doc" };
    private static final Set DEV_TYPE_SET =
        Collections.unmodifiableSet(new HashSet(Arrays.asList(DEV_TYPES)));

    static boolean isDevelopment(String type) {
        return DEV_TYPE_SET.contains(type.toLowerCase());
    }



    static ListData newEmptyList() {
        ListData result = new ListData();
        result.setEditable(false);
        return result;
    }
    private static String pathConcat(String prefix, String node) {
        if (prefix == null || prefix.length() == 0) return node;
        return prefix + "/" + node;
    }


    private static String LEAF_DATA    = getFileContents("leafData.txt");
    private static String NODE_DATA    = getFileContents("nodeData.txt");
    private static String PROCESS_DATA = getFileContents("processData.txt");

    private static String getFileContents(String filename) {
        try {
            URL url = AutoData.class.getResource(filename);
            URLConnection conn = url.openConnection();
            return new String
                (TinyWebServer.slurpContents(conn.getInputStream(), true));
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static boolean isProcessNode(Element e) {
        String tagName = e.getTagName();
        return ("node".equals(tagName) ||
                "phase".equals(tagName) ||
                "template".equals(tagName));
    }


    private static final String PATH_MACRO = "PATH";
    private static final String CHILD_LIST_ELEM = "Child_List";
    private static final String YIELD_LIST_ELEM = "Yield_Phase_List";
    private static final String FAIL_LIST_ELEM  = "Failure_Phase_List";
    private static final String APPR_LIST_ELEM  = "Appraisal_Phase_List";
    private static final String PHASE_LIST_ELEM = "Phase_List";
    private static final String DATAFILE_ATTR = PSPProperties.DATAFILE_ATTR;
    private static final String NO_DATAFILE = "none";
    private static final String DATA_EXTENT_ATTR = "autoData";
    static final String PHASE_TYPE_ATTR = "type";
    private static final String ID_ATTR   = PSPProperties.ID_ATTR;
    static final String NAME_ATTR = PSPProperties.NAME_ATTR;

    private static final String TEMPLATE_NODE_NAME =
        PSPProperties.TEMPLATE_NODE_NAME;

    public static String esc(String arg) {
        return EscapeString.escape(arg, '\\', "'\"[]");
    }

    /** Analyze an XML template to find the various phases in the process.
     */
    private class PhaseLister extends XMLDepthFirstIterator {

        ListData all, yield, appraisal, failure, nodes;
        String lastNameSeen = null;

        public PhaseLister() {
            all = newEmptyList();
            yield = newEmptyList();
            appraisal = newEmptyList();
            failure = newEmptyList();
        }

        public void commit() {
            all.setImmutable();
            yield.setImmutable();
            appraisal.setImmutable();
            failure.setImmutable();
        }

        public int getOrdering() { return POST; }

        public String getPathAttributeName(Element e) {
            return (isProcessNode(e) ? NAME_ATTR : null);
        }

        public void caseElement(Element e, List path) {
            if (AutoData.isProcessNode(e)) {
                String nodeName = concatPath(path, false);
                if (lastNameSeen == null ||
                    lastNameSeen.startsWith(nodeName) == false) {

                    // add this phase to the complete list.
                    all.add(nodeName);

                    // Determine whether this phase should be added to the
                    // appraisal, failure, and/or yield lists.
                    String phaseType =
                        e.getAttribute(AutoData.PHASE_TYPE_ATTR);
                    if (AutoData.isFailure(phaseType))
                        failure.add(nodeName);
                    else if (AutoData.isAppraisal(phaseType))
                        appraisal.add(nodeName);

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
}
