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
import pspdash.data.ListFunction;
import pspdash.data.DoubleData;
import pspdash.data.StringData;
import pspdash.data.compiler.CompiledScript;

import java.io.IOException;
import java.io.Serializable;
import java.net.URLConnection;
import java.util.*;



/** Automatically defines a consolidator capable for rolling up data
 * definitions in some other process.
 */
public class RollupAutoData extends AutoData {

    private String definesRollupID;
    private String rollupDataFile;
    private String basedOnDataFile;
    private Map definitions = null, aliasDefs = null;


    /** Create a consolidator */
    RollupAutoData(String rollupID, String basedOnDataFile,
                   String rollupDataFile)
    {
        this.definesRollupID = rollupID;
        this.basedOnDataFile = basedOnDataFile;
        this.rollupDataFile = rollupDataFile;
    }

    public DefinitionFactory getAliasAutoData() {
        return new AliasAutoData();
    }

    private class AliasAutoData implements DefinitionFactory, Serializable {
        public AliasAutoData() {}

        /** Create the default data definitions for the template
         *  represented by this AutoData object.
         */
        public Map getDefinitions(DataRepository data) {
            if (aliasDefs == null)
                buildRollupData(data);

            return aliasDefs;
        }
    }

    /** Create the default data definitions for the template
     *  represented by this AutoData object.
     */
    public Map getDefinitions(DataRepository data) {
        if (definitions == null)
            buildRollupData(data);

        return definitions;
    }


    private void buildRollupData(DataRepository data) {
        //System.out.println("buildRollupData for " + definesRollupID);
        Map baseDefinitions = null;
        try {
            baseDefinitions =
                data.loadIncludedFileDefinitions(basedOnDataFile);

            // The very act of asking for the baseDefinitions may have
            // caused this method to be called recursively, and the
            // rollup data may therefore already be built.  If so, we
            // can exit immediately.
            if (definitions != null)
                return;
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        Map rollupDefinitions = new HashMap();

        StringBuffer definitionBuffer = new StringBuffer();
        definitionBuffer
            .append("#define ROLLUP_ID ").append(esc(definesRollupID));

        // Define macros for rolling up numbers and lists
        definitionBuffer.append("\n#define ROLLUP_NUMBER(Data) ");
        rollupDouble(definitionBuffer, "Data");
        definitionBuffer.append("\n#define ROLLUP_LIST(Data) ");
        rollupList(definitionBuffer, "Data");

        // Include the standard rollup data definitions
        definitionBuffer.append("\n").append(ROLLUP_DATA).append("\n");

        Map toDateAliasDefinitions = new HashMap();
        String toDatePrefix = "/To Date/" + definesRollupID + "/All";
        toDateAliasDefinitions.put(definesRollupID + " To Date Subset Prefix",
                                   StringData.create(toDatePrefix));
        StringBuffer toDateAliasBuffer = new StringBuffer();

        Iterator i = baseDefinitions.entrySet().iterator();
        Map.Entry definition;
        String name;
        Object value;
        while (i.hasNext()) {
            definition = (Map.Entry) i.next();
            name = (String) definition.getKey();
            value = definition.getValue();

            if (skipElement(name, value))
                // filter out elements which should not be rolled up.
                continue;

            else if (value instanceof DoubleData) {
                // roll up the values of any literal double data elements.
                rollupDouble(definitionBuffer, name);
                addAlias(toDateAliasBuffer, name, definesRollupID);

            } else if (value instanceof ListFunction)
                // roll up the values of any search()-generated lists.
                rollupList(definitionBuffer, name);

            else if (value != null) {
                if (value instanceof CompiledScript)
                    addAlias(toDateAliasBuffer, name, definesRollupID);

                // DO NOT roll up dates, strings, lists, tags,
                // compiled functions, or old-style definitions - just
                // copy them into the rollup set verbatim.
                rollupDefinitions.put(name, value);
            }

        }

        // Calculate the rollup aliases
        parseDefinitions(data, toDateAliasBuffer.toString(),
                         toDateAliasDefinitions);
        aliasDefs = toDateAliasDefinitions;


        // If the user specified a datafile for this rollup, include it.
        if (XMLUtils.hasValue(rollupDataFile)) try {
            URLConnection conn =
                TemplateLoader.resolveURLConnection(rollupDataFile);
            String contents = new String
                (TinyWebServer.slurpContents(conn.getInputStream(), true));
            definitionBuffer.append("\n").append(contents).append("\n");
        } catch (IOException ioe) {
            System.err.println("IOException: " + ioe);
            ioe.printStackTrace();
        }

        // Calculate the rollup definitions and return them.
        parseDefinitions(data, definitionBuffer.toString(), rollupDefinitions);
        definitions = rollupDefinitions;

        //debugPrint(rollupDefinitions);

        try {
            // Mount this rollup data set in the repository.
            data.mountPhantomData(toDatePrefix, rollupDefinitions);
        } catch (Exception e) {
            System.out.println("Caught Exception: " + e);
            e.printStackTrace();
        }
    }

    private boolean skipElement(String name, Object value) {
        // skip "To Date" data.
        if (name.endsWith(" To Date")) return true;

        // skip freeze flags.
        if (name.indexOf("FreezeFlag") != -1) return true;

        // skip renaming operations.
        if (value instanceof String &&
            (((String) value).startsWith
                 (DataRepository.SIMPLE_RENAME_PREFIX)  ||
             ((String) value).startsWith
                 (DataRepository.PATTERN_RENAME_PREFIX))) return true;

        // All other data is fit to roll up.
        return false;
    }

    private void rollupDouble(StringBuffer definitions, String name) {
        name = esc(name);
        definitions.append("[").append(name).append("] = sumFor(\"")
            .append(name).append("\", [" + ROLLUP_LIST_ELEM + "]);\n");
    }

    private void rollupList(StringBuffer definitions, String name) {
        name = esc(name);
        definitions.append("[").append(name).append("] = listFor(\"")
            .append(name).append("\", [" + ROLLUP_LIST_ELEM + "]);\n");
    }

    private void addAlias(StringBuffer definitions, String name, String rollID)
    {
        name = esc(name);
        rollID = esc(rollID);
        definitions.append("[").append(name).append(" To Date] = ")
            .append("indirect([").append(rollID)
            .append(" To Date Subset Prefix] &/ \"")
            .append(name).append("\");\n");
    }

    private void debugPrint(Map definitions) {
        System.out.println("Definitions for " + definesRollupID +
                           " Rollup ------------------------------");

        Iterator i = definitions.entrySet().iterator();
        Map.Entry e;
        while (i.hasNext()) {
            e = (Map.Entry) i.next();
            System.out.print("[");
            System.out.print(e.getKey());
            System.out.print("] = ");
            if (e.getValue() instanceof CompiledScript)
                System.out.print(((CompiledScript) e.getValue()).saveString());
            else
                System.out.print(e.getValue());
            System.out.println();
        }

        System.out.println("----End--------------------------------------");
    }


    private static final String ROLLUP_DATA=getFileContents("rollupData.txt");

    private static final String ROLLUP_LIST_ELEM = "Rollup_List";
}
