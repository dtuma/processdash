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

import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

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
        String imaginaryFilename = templateID + "?dataFile.txt";

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
        data.registerDefaultData(result, dataFile, imaginaryFilename);

        // If the user requested the definition of rollup data sets, create
        // a rollup AutoData object and register it with the DataRepository.
        if (definesRollup != null) {
            String rollupDataFile =
                template.getAttribute(ROLLUP_DATAFILE_ATTR);
            imaginaryFilename = data.getRollupDatafileName(definesRollup);

            RollupAutoData rollupResult = new RollupAutoData
                (definesRollup, dataFile, rollupDataFile);
            data.registerDefaultData(rollupResult, null, imaginaryFilename);

            imaginaryFilename = data.getAliasDatafileName(definesRollup);
            DefinitionFactory aliasResult = rollupResult.getAliasAutoData();
            data.registerDefaultData(aliasResult, null, imaginaryFilename);

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
            result.append(StringUtils.findAndReplace
                          (ROLLUP_TEMPLATE_XML, "RID",
                           XMLUtils.escapeAttribute(rollupID)));
        }
        result.append("</dashboard-process-template>");

        return result.toString();
    }
    private static final String ROLLUP_TEMPLATE_XML =
        "<template name='Rollup RID Data' ID='Rollup RID Data' "+
        "          dataFile='ROLLUP:RID' defineRollup='no'>" +
        "   <html ID='sum' title='RID Rollup Project Summary' " +
        "         href='dash/summary.shtm?rollup'/>" +
        "   <html ID='config' title='Edit Data Rollup Filter' " +
        "         href='dash/rollupFilter.shtm'/>" +
        "   <phase name='Analyze Rollup Data'/>" +
        "</template>";


    /** Get the contents of a file as a string.  The file is loaded from the
     * classpath and must be in the same package as this class.
     */
    protected static String getFileContents(String filename) {
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
        return EscapeString.escape(arg, '\\', "'\"[]");
    }


    /** Various string constants used by the XML scanning logic.
     */
    protected static final String TEMPLATE_NODE_NAME =
        PSPProperties.TEMPLATE_NODE_NAME;
    protected static final String DATAFILE_ATTR = PSPProperties.DATAFILE_ATTR;
    protected static final String NO_DATAFILE = PSPProperties.NO_DATAFILE;
    protected static final String DATA_EXTENT_ATTR = "autoData";
    protected static final String ID_ATTR   = PSPProperties.ID_ATTR;
    static final String NAME_ATTR = PSPProperties.NAME_ATTR;
    protected static final String DEFINE_ROLLUP_ATTR = "defineRollup";
    protected static final String USES_ROLLUP_ATTR = "usesRollup";
    protected static final String ROLLUP_DATAFILE_ATTR = "rollupDataFile";
    protected static final String SIZE_METRIC_ATTR = "size";

}
