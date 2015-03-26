// Copyright (C) 2005-2009 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.mgr;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.tool.bridge.client.TeamServerSelector;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;

public class ExportMetricsFileInstruction extends AbstractInstruction implements
        CompletionStatus.Listener {

    private static final String TAG_NAME = "exportMetricsFile";

    private static final String FILE_ATTR = "file";

    private static final String URL_ATTR = "serverUrl";

    private static final String PATHS_TAG = "paths";

    private static final String PATH_TAG = "path";

    private static final String METRICS_FILTER_TAG = "metricsFilter";

    private static final String INCLUDE_TAG = "include";

    private static final String EXCLUDE_TAG = "exclude";

    private static final String ADD_ENTRY_TAG = "addEntry";



    private Vector paths = new Vector();

    private List metricsIncludes = new LinkedList();

    private List metricsExcludes = new LinkedList();

    private List additionalFiles = new LinkedList();

    public ExportMetricsFileInstruction() {
    }

    public ExportMetricsFileInstruction(String file, Vector paths) {
        setFile(file);
        setPaths(paths);
    }

    public ExportMetricsFileInstruction(Element e) {
        super(e);
        loadPathsFromLegacyAttr();
        loadChildrenFromXML(e);
    }

    public void mergeXML(Element e) {
        super.mergeXML(e);
        loadPathsFromLegacyAttr();
        loadChildrenFromXML(e);
    }

    private void loadPathsFromLegacyAttr() {
        String oldPathsAttr = getAttribute(PATHS_TAG);
        if (oldPathsAttr != null) {
            paths.addAll(stringToPaths(oldPathsAttr));
            setAttribute(PATHS_TAG, null);
        }
    }

    private void loadChildrenFromXML(Element e) {
        List children = XMLUtils.getChildElements(e);
        for (Iterator iter = children.iterator(); iter.hasNext();) {
            Element child = (Element) iter.next();
            if (PATHS_TAG.equals(child.getTagName()))
                loadListFromXML(child, paths, PATH_TAG);
            else if (METRICS_FILTER_TAG.equals(child.getTagName())) {
                loadListFromXML(child, metricsIncludes, INCLUDE_TAG);
                loadListFromXML(child, metricsExcludes, EXCLUDE_TAG);
            } else if (ADD_ENTRY_TAG.equals(child.getTagName())) {
                additionalFiles.add(new ExportFileEntry(child));
            }
        }
    }

    protected boolean hasChildXMLContent() {
        return !(paths.isEmpty() && metricsIncludes.isEmpty()
                && metricsExcludes.isEmpty());
    }

    protected void getChildXMLContent(StringBuffer out) {
        if (!paths.isEmpty())
            getListAsXML(out, paths, PATHS_TAG, PATH_TAG);

        if (!(metricsIncludes.isEmpty() && metricsExcludes.isEmpty())) {
            out.append("<").append(METRICS_FILTER_TAG).append(">");
            getListItemsAsXML(out, metricsIncludes, INCLUDE_TAG);
            getListItemsAsXML(out, metricsExcludes, EXCLUDE_TAG);
            out.append("</").append(METRICS_FILTER_TAG).append(">");
        }
    }

    public String getFile() {
        return getAttribute(FILE_ATTR);
    }

    public void setFile(String file) {
        setAttribute(FILE_ATTR, file);
    }

    public String getServerUrl() {
        return getAttribute(URL_ATTR);
    }

    public void setServerUrl(String url) {
        setAttribute(URL_ATTR, url);
    }

    public Vector getPaths() {
        return paths;
    }

    public void setPaths(Vector path) {
        this.paths = path;
    }

    public List getMetricsIncludes() {
        return metricsIncludes;
    }

    public void setMetricsIncludes(List includes) {
        this.metricsIncludes = includes;
    }

    public List getMetricsExcludes() {
        return metricsExcludes;
    }

    public void setMetricsExcludes(List excludes) {
        this.metricsExcludes = excludes;
    }

    public List getAdditionalFileEntries() {
        return this.additionalFiles;
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

    public void completionStatusReady(CompletionStatus.Event event) {
        CompletionStatus s = event.getSource().getCompletionStatus();
        if (s.getStatus() != CompletionStatus.SUCCESS)
            return;

        Object target = s.getTarget();
        if (target == null)
            return;

        String targetString = target.toString();
        if (TeamServerSelector.isUrlFormat(targetString))
            setServerUrl(targetString);
    }


    public boolean equals(Object obj) {
        if (super.equals(obj) == false)
            return false;

        ExportMetricsFileInstruction that = (ExportMetricsFileInstruction) obj;
        return this.paths.equals(that.paths)
                && this.metricsIncludes.equals(that.metricsIncludes)
                && this.metricsExcludes.equals(that.metricsExcludes);
    }

    public int hashCode() {
        return (super.hashCode() << 7) ^ paths.hashCode() ^ metricsIncludes.hashCode()
                ^ metricsExcludes.hashCode();
    }

    public String toString() {
        return super.toString()
                + " paths=" + paths
                + " includes=" + metricsIncludes
                + " excludes=" + metricsExcludes;
    }

    public Object clone() {
        ExportMetricsFileInstruction result = (ExportMetricsFileInstruction) super.clone();
        result.paths = new Vector(this.paths);
        result.metricsIncludes = new LinkedList(this.metricsIncludes);
        result.metricsExcludes = new LinkedList(this.metricsExcludes);
        return result;
    }

}
