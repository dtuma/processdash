// Copyright (C) 2002-2018 Tuma Solutions, LLC
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


package net.sourceforge.processdash.ev;

import java.io.File;

import org.w3c.dom.Element;

import net.sourceforge.processdash.data.repository.DataRepository;


public class EVTaskListXML extends EVTaskListXMLAbstract {

    public static final String XMLID_FLAG = "#XMLID";
    static final String XMLID_ATTR = "tlid";

    private boolean reloadFromImports;
    private Element importedXml;
    private File importSourceFile;

    /** @deprecated */
    public EVTaskListXML(String taskListName, DataRepository data) {
        this(taskListName);
    }

    public EVTaskListXML(String taskListName) {
        super(taskListName, null, false);
        this.reloadFromImports = true;

        if (!openXMLForImportedTaskList())
            createErrorRootNode
                (cleanupName(taskListName),
                 resources.getString("TaskList.Missing_Error_Message")
                     + "\n#teamhelp/TeamAddSchedule "
                     + resources.getDlgString("Help"));
    }

    public EVTaskListXML(String displayName, Element xml) {
        super(displayName, null, false);
        this.reloadFromImports = false;

        try {
            openXML(xml, displayName, null);
        } catch (Exception e) {
            createErrorRootNode(displayName, resources
                    .getString("TaskList.Invalid_Schedule_Error_Message"));
        }
    }

    public void recalc() {
        if (reloadFromImports && !openXMLForImportedTaskList())
            createErrorRootNode
                (cleanupName(taskListName),
                 resources.getString("TaskList.Missing_Error_Message")
                     + "\n#teamhelp/TeamAddSchedule "
                     + resources.getDlgString("Help"));
        super.recalc();
    }

    /** @since 2.4.4 */
    public File getImportSourceFile() {
        return importSourceFile;
    }

    private boolean openXMLForImportedTaskList() {
        Element xmlDoc = ImportedEVManager.getInstance()
                .getImportedTaskListXml(taskListName);
        if (xmlDoc == null) return false;
        if (xmlDoc == importedXml) return true;

        try{
            openXML(xmlDoc, cleanupName(taskListName), null);
            importedXml = xmlDoc;
            importSourceFile = ImportedEVManager.getInstance()
                    .getSrcFile(taskListID);
            return true;

        } catch (Exception e) {
            System.err.println("Got exception: " +e);
            e.printStackTrace();
            return false;
        }
    }



    public static boolean validName(String taskListName) {
        return (taskListName != null &&
                taskListName.indexOf(MAIN_DATA_PREFIX) != -1);
    }

    public static boolean exists(String taskListName) {
        Element xmlDoc = ImportedEVManager.getInstance()
                .getImportedTaskListXml(taskListName);
        return xmlDoc != null;
    }

}
