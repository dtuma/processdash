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


import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.repository.DefinitionFactory;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.EscapeString;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/** Automatically defines various types of data.
 */
public abstract class AutoData implements DefinitionFactory, Serializable {

    static HashSet rollupsDefined = new HashSet();
    static HashSet rollupsUsed    = new HashSet();


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
        int len = templates.getLength();
        for (int i = 0;   i < len;   i++)
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

        // Construct an imaginary datafile name based upon the template ID.
        String imaginaryFilename = data.getImaginaryDatafileName(templateID);

        // If the user did not specify a datafile name, use the
        // imaginary datafile name.
        if (!XMLUtils.hasValue(dataFile))
            template.setAttribute(DATAFILE_ATTR, imaginaryFilename);

        // See if this template defines a rollup dataset.
        String definesRollup = template.getAttribute(DEFINE_ROLLUP_ATTR);
        String usesRollup = template.getAttribute(USES_ROLLUP_ATTR);

        if (!XMLUtils.hasValue(definesRollup))
            if (!XMLUtils.hasValue(usesRollup))
                // if both values are missing, assume the templateID for both.
                definesRollup = usesRollup = templateID;
            else
                // if usesRollup has a value but definesRollup does
                // not, assume the value "no" for definesRollup.
                definesRollup = null;

        else if ("no".equalsIgnoreCase(definesRollup))
            // "no" for definesRollup is a special value indicating
            // no rollup should be defined based upon this template.
            definesRollup = null;

        else
            if (!XMLUtils.hasValue(usesRollup))
                // if definesRollup has a value but usesRollup does not, assume
                // that the template uses the same rollup it defines.
                usesRollup = definesRollup;


        // create an AutoData object and register it with the DataRepository.
        TemplateAutoData result = new TemplateAutoData
            (template, templateID, usesRollup, definesRollup, dataFile);
        if (XMLUtils.hasValue(dataFile))
            data.registerDefaultData(result, dataFile, imaginaryFilename);
        else
            data.registerDefaultData(result, imaginaryFilename, null);

        // If the user requested the definition of rollup data sets, create
        // a rollup AutoData object and register it with the DataRepository.
        if (definesRollup != null) {
            String rollupBasedOn = dataFile;
            if (!XMLUtils.hasValue(rollupBasedOn))
                rollupBasedOn = imaginaryFilename;

            String rollupDataFile =
                template.getAttribute(ROLLUP_DATAFILE_ATTR);
            String imagRollupFilename =
                data.getRollupDatafileName(definesRollup);
            RollupAutoData rollupResult = new RollupAutoData
                (definesRollup, rollupBasedOn, rollupDataFile);
            data.registerDefaultData(rollupResult, null, imagRollupFilename);

            String imagAliasFilename =
                data.getAliasDatafileName(definesRollup);
            DefinitionFactory aliasResult = rollupResult.getAliasAutoData();
            data.registerDefaultData(aliasResult, null, imagAliasFilename);

            // Keep track of the rollups we have created.
            rollupsDefined.add(definesRollup);
        }

        // Keep track of the rollups that have been used by templates.
        String isRollupTemplate = data.isRollupDatafileName(dataFile);
        if (isRollupTemplate != null) rollupsUsed.add(isRollupTemplate);
    }


    /** This routine identifies "orphaned" data rollups, and creates
     *  XML templates that the user can use to instantiate those rollups.
     *  ("orphaned" data rollups are rollups that have been defined,
     *  but which are not referenced by any template.)
     *  @return null if there are no orphaned data rollups; otherwise
     *     returns XML text for the dynamically generated templates.
     */
    public static String generateRollupTemplateXML() {
        HashSet orphans = new HashSet(rollupsDefined);
        orphans.removeAll(rollupsUsed);

        if (orphans.isEmpty()) return null;

        StringBuffer result = new StringBuffer();
        result.append("<?xml version='1.0'?><dashboard-process-template>");

        Iterator i = orphans.iterator();
        while (i.hasNext()) {
            String rollupID = (String) i.next();
             //System.out.println("Generating rollup template: " + rollupID);
            StringBuffer xml = new StringBuffer(ROLLUP_TEMPLATE_XML);
            StringUtils.findAndReplace
                (xml, "RID", XMLUtils.escapeAttribute(rollupID));

            replaceRollupResource(xml, "Rollup_Template_Name", rollupID);
            replaceRollupResource(xml, "Rollup_Summary_Name", rollupID);
            replaceRollupResource(xml, "Edit_Filter_Name", rollupID);
            replaceRollupResource(xml, "Rollup_Phase_Name", rollupID);

            result.append(xml.toString());
        }
        result.append("</dashboard-process-template>");

        return result.toString();
    }
    private static Resources resources = null;
    private static void replaceRollupResource(StringBuffer buf, String key,
                                              String rollupID) {
        if (resources == null)
            resources = Resources.getDashBundle("Templates");

        String val = resources.format(key+"_FMT", rollupID);
        StringUtils.findAndReplace(buf, key, XMLUtils.escapeAttribute(val));
    }
    private static final String ROLLUP_TEMPLATE_XML =
        "<template name='Rollup_Template_Name' ID='Rollup RID Data' "+
        "          dataFile='ROLLUP:RID' defineRollup='no'>" +
        "   <html ID='sum' title='Rollup_Summary_Name' " +
        "         href='reports/analysis/index.htm'/>" +
        "   <html ID='config' title='Edit_Filter_Name' " +
        "         href='dash/rollupFilter.shtm'/>" +
        "   <phase name='Rollup_Phase_Name'/>" +
        "</template>";



    /** Get the contents of a file as a string.  The file is loaded from the
     * classpath and must be in the same package as this class.
     */
    protected static String getFileContents(String filename) {
        try {
            URL url = AutoData.class.getResource(filename);
            URLConnection conn = url.openConnection();
            return new String
                (FileUtils.slurpContents(conn.getInputStream(), true));
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }


    /** Ask the DataRepository to parse a string containing data definitions.
     */
    protected static void parseDefinitions(DataRepository data,
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


    /** Construct an empty list. */
    protected static ListData newEmptyList() {
        ListData result = new ListData();
        result.setEditable(false);
        return result;
    }


    /** Concatenate two portions of a hierarchy path. */
    protected static String pathConcat(String prefix, String node) {
        if (prefix == null || prefix.length() == 0) return node;
        return prefix + "/" + node;
    }


    /** Escape a string for use in a datafile either as a data element
     * name or as a string literal.
     */
    public static String esc(String arg) {
        // If you alter the next line, you must also update the code in
        // HTMLPreprocessor.applyEncodings()
        return EscapeString.escape(arg, '\\', "'\"[]");
    }


    /** Various string constants used by the XML scanning logic.
     */
    protected static final String TEMPLATE_NODE_NAME =
        DashHierarchy.TEMPLATE_NODE_NAME;
    protected static final String DATAFILE_ATTR = DashHierarchy.DATAFILE_ATTR;
    protected static final String NO_DATAFILE = DashHierarchy.NO_DATAFILE;
    protected static final String DATA_EXTENT_ATTR = "autoData";
    protected static final String ID_ATTR   = DashHierarchy.ID_ATTR;
    protected static final String NAME_ATTR = DashHierarchy.NAME_ATTR;
    protected static final String DEFINE_ROLLUP_ATTR = "defineRollup";
    protected static final String USES_ROLLUP_ATTR = "usesRollup";
    protected static final String ROLLUP_DATAFILE_ATTR = "rollupDataFile";
    protected static final String SIZE_METRIC_ATTR = "size";
    protected static final String DEFECTLOG_ATTR = DashHierarchy.DEFECTLOG_ATTR;

}
