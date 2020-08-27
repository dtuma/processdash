// Copyright (C) 2014-2020 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.ui;

import static net.sourceforge.processdash.team.TeamDataConstants.PROJECT_ID;
import static net.sourceforge.processdash.team.TeamDataConstants.RELAUNCH_SOURCE_WBS_ID;
import static net.sourceforge.processdash.team.TeamDataConstants.WBS_ID_DATA_NAME;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.TagData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.process.ScriptID;
import net.sourceforge.processdash.process.ScriptSource;
import net.sourceforge.processdash.templates.DashPackage;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.util.HTMLUtils;


public class WorkflowProbeScriptSource implements ScriptSource {

    private DashboardContext context;

    private String processID;

    public void setDashboardContext(DashboardContext context) {
        this.context = context;
    }

    public void setConfigElement(Element configElement, String attrName) {
        processID = configElement.getAttribute("processID");
    }

    public String getUniqueID() {
        return "WorkflowProbeScriptSource/" + processID;
    }

    public List<ScriptID> getScripts(String path) {
        // only return scripts for team projects that use this MCF.
        if (!isWithinMatchingTeamProject(path))
            return null;

        // find any PROBE tasks that are potentially within the same process
        // enactment as the current task. If none are found, abort.
        List<String> probeTasks = getProbeTaskPaths(path);
        if (probeTasks.isEmpty())
            return null;

        // Make certain the user has the required versions of dependent logic.
        // If not, show an error page.
        if (!checkVersionNumbers())
            return Collections.singletonList(new ScriptID(processID
                    + "/setup/probeUpgradeNeeded.shtm", path,
                    "Software Upgrade Needed"));

        List<ScriptID> result = new ArrayList<ScriptID>();
        for (String oneProbeTask : probeTasks)
            addScripts(result, oneProbeTask);
        return result;
    }

    private boolean isWithinMatchingTeamProject(String path) {
        return context.getData().getInheritableValue(path, //
            processID + " Tag") instanceof TagData;
    }

    private List<String> getProbeTaskPaths(String path) {
        String workflowRoot = getWorkflowParent(path);
        PropertyKey workflowRootKey = context.getHierarchy().findExistingKey(
            workflowRoot);
        if (workflowRootKey == null)
            return Collections.EMPTY_LIST;

        List<String> result = new ArrayList();
        findProbeTasks(result, workflowRootKey);
        findPriorProbeTasks(result, workflowRoot);
        return result;
    }

    private String getWorkflowParent(String path) {
        String result = null;
        while (path != null) {
            if (getValue(path, "Workflow_Source_ID") != null)
                result = path;
            path = DataRepository.chopPath(path);
        }
        return result;
    }

    private void findProbeTasks(List<String> result, PropertyKey node) {
        int numChildren = context.getHierarchy().getNumChildren(node);
        if (numChildren == 0) {
            String nodePath = node.path();
            if (getValue(nodePath, "PROBE Task") != null)
                result.add(nodePath);

        } else {
            for (int i = numChildren; i-- > 0;) {
                findProbeTasks(result,
                    context.getHierarchy().getChildKey(node, i));
            }
        }
    }

    private void findPriorProbeTasks(List<String> result, String workflowRoot) {
        // see if this component was relaunched from an earlier project
        String relaunchID = getString(workflowRoot, RELAUNCH_SOURCE_WBS_ID);
        if (relaunchID == null)
            return;

        // parse the relaunch ID
        String[] parts = relaunchID.split(":");
        if (parts.length != 2)
            return;
        String projectID = parts[0], wbsID = parts[1];

        // scan the hierarchy to find earlier instances of this component, and
        // look for PROBE tasks within
        scanForPriorProbeTasks(result, workflowRoot, relaunchID, projectID,
            wbsID, context.getHierarchy(), PropertyKey.ROOT, false, false);
    }

    private void scanForPriorProbeTasks(List<String> result,
            String workflowRoot, String relaunchID, String projectID,
            String wbsID, DashHierarchy hier, PropertyKey node,
            boolean projectMatch, boolean componentMatch) {

        // the original workflow root was already searched for PROBE tasks; so
        // when we encounter it again, skip that portion of the hierarchy.
        String path = node.path();
        if (path.equals(workflowRoot))
            return;

        if (componentMatch) {
            // when our ancestor is a pre-relaunch instance of the workflow
            // root, search for PROBE tasks
            if (getValue(path, "PROBE Task") != null)
                result.add(path);

        } else if (projectMatch) {
            // within the original project, look for the matching WBS node
            if (wbsID.equals(getString(path, WBS_ID_DATA_NAME)))
                componentMatch = true;

        } else if (projectID.equals(getString(path, PROJECT_ID))) {
            // if we find the root of the original project, set the flag
            projectMatch = true;

        } else if (relaunchID.equals(getString(path, RELAUNCH_SOURCE_WBS_ID))) {
            // in other projects, look for nodes with this relaunch ID
            componentMatch = true;
        }

        // recurse over hierarchy children
        for (int i = hier.getNumChildren(node); i-- > 0;) {
            PropertyKey child = hier.getChildKey(node, i);
            scanForPriorProbeTasks(result, workflowRoot, relaunchID, projectID,
                wbsID, hier, child, projectMatch, componentMatch);
        }
    }

    private boolean checkVersionNumbers() {
        for (String[] reqt : REQUIRED_VERSIONS) {
            if (!hasPackage(reqt[0], reqt[1]))
                return false;
        }
        return true;
    }

    private static final String[][] REQUIRED_VERSIONS = {
            { "pspdash", "2.0.9" }, //
            { "tpidw-embedded", "1.3.1" } };

    private boolean isPspAddOnInstalled() {
        return hasPackage("pspProc", "2.4");
    }

    private boolean hasPackage(String packageID, String minVersion) {
        String version = TemplateLoader.getPackageVersion(packageID);
        return (version != null && DashPackage.compareVersions(version,
            minVersion) >= 0);
    }

    private void addScripts(List<ScriptID> result, String path) {
        String rootPath = getString(path, "Workflow_Root_Path");
        String workflowName = getString(path, "Workflow_Name");
        if (rootPath == null || workflowName == null)
            return;

        result.add(new ScriptID(getUrl(path, "/setup/probeSummary"), //
                rootPath, workflowName + " - Plan Summary"));
        if (isPspAddOnInstalled())
            result.add(new ScriptID(getUrl(path, "/sizeest.class"), //
                rootPath, workflowName + " - Size Estimating Template"));
    }

    private String getUrl(String path, String href) {
        String uri = HTMLUtils.urlEncodePath(path) + "//" + processID + href;
        return Browser.mapURL(uri);
    }

    private String getString(String path, String name) {
        SimpleData sd = getValue(path, name);
        return (sd == null ? null : sd.format());
    }

    private SimpleData getValue(String path, String name) {
        String dataName = DataRepository.createDataName(path, name);
        return context.getData().getSimpleValue(dataName);
    }

}
