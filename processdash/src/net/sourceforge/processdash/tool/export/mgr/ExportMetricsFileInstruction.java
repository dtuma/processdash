// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2005 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.mgr;

import java.util.Vector;

import net.sourceforge.processdash.data.ListData;

import org.w3c.dom.Element;

public class ExportMetricsFileInstruction extends AbstractInstruction {

    public static final String FILE_SUFFIX = ".txt";

    private static final String TAG_NAME = "exportMetricsFile";

    private static final String FILE_ATTR = "file";

    private static final String PATHS_ATTR = "paths";

    public ExportMetricsFileInstruction() {
    }

    public ExportMetricsFileInstruction(Element e) {
        super(e);
    }

    public ExportMetricsFileInstruction(String file, Vector paths) {
        setFile(file);
        setPaths(paths);
    }

    public String getFile() {
        return getAttribute(FILE_ATTR);
    }

    public void setFile(String file) {
        if (file != null && !file.toLowerCase().endsWith(FILE_SUFFIX))
            file = file + FILE_SUFFIX;
        setAttribute(FILE_ATTR, file);
    }

    public Vector getPaths() {
        return stringToPaths(getAttribute(PATHS_ATTR));
    }

    public void setPaths(Vector path) {
        setAttribute(PATHS_ATTR, pathsToString(path));
    }

    public String getDescription() {
        String pathsStr = getPaths().toString();
        pathsStr = pathsStr.substring(1, pathsStr.length() - 1);
        return resource.format(
                "Wizard.Export.Export_Metrics_File.Task_Description_FMT",
                pathsStr, getFile());
    }

    public String getXmlTagName() {
        return TAG_NAME;
    }

    private static String pathsToString(Vector paths) {
        if (paths == null || paths.size() == 0)
            return "";

        ListData list = new ListData();
        for (int i = 0; i < paths.size(); i++) {
            list.add(paths.get(i));
        }
        return list.format();
    }

    private static Vector stringToPaths(String paths) {
        if (paths == null || paths.length() == 0)
            return new Vector();

        ListData list = new ListData(paths);
        Vector result = new Vector();
        for (int i = 0; i < list.size(); i++)
            result.add(list.get(i));
        return result;
    }

    public static boolean matches(Element e) {
        return TAG_NAME.equals(e.getTagName());
    }

    public Object dispatch(ExportInstructionDispatcher dispatcher) {
        return dispatcher.dispatch(this);
    }

}
