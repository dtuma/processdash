// Copyright (C) 2010 Tuma Solutions, LLC
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

package teamdash.templates.setup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.process.ScriptID;
import net.sourceforge.processdash.process.ScriptSource;
import net.sourceforge.processdash.util.StringUtils;

import org.w3c.dom.Element;

public class WorkflowScriptSource implements ScriptSource {

    private DashboardContext context;

    private String dataName;

    public void setDashboardContext(DashboardContext context) {
        this.context = context;
    }

    public void setConfigElement(Element configElement, String attrName) {
        dataName = configElement.getAttribute("dataName");
    }

    public String getUniqueID() {
        return "WorkflowScriptSource/" + dataName;
    }

    public List<ScriptID> getScripts(String path) {
        // Get the list of workflow URL specs for the enclosing team project.
        DataRepository data = context.getData();
        StringBuffer projectPathBuf = new StringBuffer(path);
        ListData urlSpecList = ListData.asListData(data.getInheritableValue(
            projectPathBuf, dataName));

        // if no workflow URL spec was found, the current task is not part of
        // a team project that we handle.  If the workflow URL spec was empty,
        // this team project doesn't have any associated workflow URLs.
        if (urlSpecList == null || urlSpecList.test() == false)
            return null;

        // construct a list of path segments we should examine.  The first
        // segment is the path to the team project itself. Then we include
        // the name of each nested component or subtask within the project
        // on the path to the currently active task.
        String projectPath = projectPathBuf.toString();
        List<String> pathSegments = new ArrayList<String>();
        pathSegments.add(projectPath);
        if (path.length() > projectPath.length() + 1) {
            String relSubpath = path.substring(projectPath.length() + 1);
            pathSegments.addAll(Arrays.asList(relSubpath.split("/")));
        }

        // find the list of workflow scripts that are associated with the
        // currently active task and its ancestors.
        LinkedHashSet result = collectScripts(data, urlSpecList, pathSegments);
        if (result.isEmpty())
            return null;

        // for efficiency purposes, we built the list in backwards order.
        // reverse it so the URLs appear in the order the user wrote them.
        ArrayList<ScriptID> listResult = new ArrayList<ScriptID>(result);
        Collections.reverse(listResult);
        return listResult;
    }

    private LinkedHashSet collectScripts(DataContext data, ListData urlSpecList,
            List<String> pathSegments) {
        LinkedHashSet<ScriptID> result = new LinkedHashSet<ScriptID>();
        List<String> activePrefixes = new ArrayList<String>();
        String path = "";

        for (String oneSegment : pathSegments) {
            path = DataRepository.createDataName(path, oneSegment);

            // modify the list of active prefixes, appending the current path
            // segment to each one.
            extendPrefixes(activePrefixes, oneSegment);

            // retrieve the workflow source IDs for the current path, and
            // add them to the working list of workflow prefixes.
            addWorkflowSourceIds(data, activePrefixes, path);

            // look through the URL specs for ones that match the active list
            // of search prefixes.
            for (String onePrefix : activePrefixes)
                findMatchingScripts(urlSpecList, result, path, onePrefix);
        }

        return result;
    }

    /**
     * Modify each of the prefixes in a list, appending a new path segment to
     * each one.
     * 
     * @param prefixes a list of prefixes to modify
     * @param newSegment the new segment to append.
     */
    private void extendPrefixes(List<String> prefixes, String newSegment) {
        for (int i = 0; i < prefixes.size(); i++) {
            String oneOldPrefix = prefixes.get(i);
            String oneNewPrefix = oneOldPrefix + "/" + newSegment;
            prefixes.set(i, oneNewPrefix);
        }
    }

    /**
     * Look in the data repository for the node with a given path. If that node
     * has any associated workflow source IDs, add them to the list of prefixes.
     * 
     * @param data the data repository
     * @param prefixes the list to which prefixes should be added
     * @param path the path of a node within the repository
     */
    private void addWorkflowSourceIds(DataContext data, List<String> prefixes,
            String path) {
        String widDataName = DataRepository.createDataName(path,
            TeamDataConstants.WORKFLOW_ID_DATA_NAME);
        SimpleData wid = data.getSimpleValue(widDataName);
        if (wid != null && wid.test()) {
            for (String oneId : wid.format().split(","))
                if (StringUtils.hasValue(oneId))
                    prefixes.add(oneId);
        }
    }

    /**
     * Find workflow URL specifications that match a given workflow path,
     * construct ScriptID objects for the matching items, and add them to
     * a list.
     * 
     * @param urlSpecList a list of workflow URL specifications. Each entry
     *     in the list is of the form "workflow/path///URL list".  (That is,
     *     a workflow path, followed by a triple-slash, followed by a series
     *     of URLs.)
     * @param result the list that ScriptID objects should be added to
     * @param dataPath the dashboard hierarchy path that should be used when
     *     creating the ScriptID objects
     * @param workflowPathSpec either a workflow source ID (an integer), or
     *     a path of the form 12345/path/within/workflow.  (That is, a workflow
     *     source ID followed by the relative path of a step underneath the
     *     node with that workflow source ID.)
     */
    private void findMatchingScripts(ListData urlSpecList,
            Set<ScriptID> result, String dataPath, String workflowPathSpec) {
        String specPrefix = workflowPathSpec + "///";
        for (int i = 0; i < urlSpecList.size(); i++) {
            String oneScriptSpec = (String) urlSpecList.get(i);
            if (oneScriptSpec.startsWith(specPrefix)) {
                String oneUrlSpec = oneScriptSpec.substring(specPrefix.length());
                addScriptsFromUrlSpec(result, dataPath, oneUrlSpec);
            }
        }
    }

    /**
     * Parse a URL specification, construct ScriptIDs, and add them to a list.
     * 
     * @param result the list that ScriptID objects should be added to
     * @param dataPath the dashboard hierarchy path that should be used when
     *     creating the ScriptID objects
     * @param oneUrlSpec a String of the format
     *     <code>(http...[ display name])+</code>.  That is, a
     *     whitespace-separated list of HTTP URLs, each one optionally
     *     followed by whitespace and a free text display name.
     */
    private void addScriptsFromUrlSpec(Set<ScriptID> result, String dataPath,
            String oneUrlSpec) {
        while (oneUrlSpec.length() > 0) {
            int pos = oneUrlSpec.lastIndexOf("http");
            if (pos == -1)
                break;

            String oneUrl = oneUrlSpec.substring(pos).trim();
            String oneDisplayName = null;

            Matcher m = WHITESPACE_PAT.matcher(oneUrl);
            if (m.find()) {
                oneDisplayName = oneUrl.substring(m.end());
                oneUrl = oneUrl.substring(0, m.start());
            }

            result.add(new ScriptID(oneUrl, dataPath, oneDisplayName));

            oneUrlSpec = oneUrlSpec.substring(0, pos);
        }
    }

    private static final Pattern WHITESPACE_PAT = Pattern.compile("\\s+");

}
