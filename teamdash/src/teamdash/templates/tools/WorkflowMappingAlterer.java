// Copyright (C) 2016 Tuma Solutions, LLC
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

package teamdash.templates.tools;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.lock.LockFailureException;

import teamdash.templates.tools.WorkflowMappingManager.Phase;
import teamdash.templates.tools.WorkflowMappingManager.Workflow;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WorkflowWBSModel;

public abstract class WorkflowMappingAlterer {

    public abstract void applyChanges(Workflow workflow, Workflow target,
            Map<String, String> changes) throws WorkflowMappingException;

    public abstract void startChanges() throws IOException,
            LockFailureException;

    public abstract WorkflowWBSModel loadWorkflows() throws IOException;

    public abstract void saveWorkflows(WorkflowWBSModel workflows)
            throws IOException, LockFailureException;

    public abstract void finishChanges() throws IOException,
            LockFailureException;



    protected void applyChangesImpl(Workflow workflow, Workflow target,
            Map<String, String> changes) throws IOException,
            LockFailureException {

        startChanges();
        try {
            WorkflowWBSModel model = loadWorkflows();
            applyChanges(model, workflow, target, changes);
            saveWorkflows(model);
        } finally {
            finishChanges();
        }
    }

    private void applyChanges(WorkflowWBSModel model, Workflow workflow,
            Workflow target, Map<String, String> changes) {

        // extract useful information from the various IDs
        String[] parts = workflow.getId().split(":", 3);
        String workflowProjectId = parts[1];
        Integer workflowUniqueId = Integer.valueOf(parts[2]);
        parts = target.getId().split(":", 3);
        String targetProjectId = parts[1];
        String targetUniqueId = parts[2];
        boolean sameProject = workflowProjectId.equals(targetProjectId);
        String targetRelativeId = (sameProject ? PROJECT_RELATIVE_PREFIX
                + targetUniqueId : target.getId());
        String attrName = PHASE_MAPPING_PREFIX + targetRelativeId;

        // collect useful information about the changes to be made
        Set<Integer> phaseUniqueIDsToChange = getPhaseUniqueIDs(changes);
        Set<String> targetPhaseFullIDs = getPhaseFullIDs(target, sameProject);

        // iterate over the workflow nodes, and update them
        for (WBSNode node : model.getWbsNodes()) {

            // only alter nodes for the set of phases we are changing
            if (!phaseUniqueIDsToChange.contains(node.getUniqueID()))
                continue;

            // remove any attributes from this node that specify obsolete
            // mappings to the target workflow
            deleteOldMappings(node, attrName, targetPhaseFullIDs);

            // store new mappings into this node as specified by the changes
            storeNewMappings(node, changes, attrName, sameProject);
        }

        // set/clear a flag on the workflow node to indicate whether the ETL
        // logic should disable mappings between these two workflows
        WBSNode workflowNode = model.getWorkflowNodeMap().get(workflowUniqueId);
        if (changes.containsKey(WorkflowMappingManager.DELETE_MAPPINGS))
            workflowNode.setAttribute(attrName, "*NONE*");
        else
            workflowNode.removeAttribute(attrName);
    }

    private Set<Integer> getPhaseUniqueIDs(Map<String, String> changes) {
        Set<Integer> result = new HashSet<Integer>();
        for (String phaseId : changes.keySet())
            result.add(uniqueId(phaseId));
        return result;
    }

    private Set<String> getPhaseFullIDs(Workflow w, boolean sameProject) {
        Set<String> result = new HashSet<String>();
        for (Phase phase : w.getPhases()) {
            result.add(phase.getId());
            if (sameProject)
                result.add(makeRelative(phase.getId()));
        }
        return result;
    }

    private void deleteOldMappings(WBSNode node, String attrName,
            Set<String> targetPhaseIDs) {
        // discard the named attribute if it is present.
        node.removeAttribute(attrName);

        // now, look over all the attributes in this node for others to purge
        Map<String, Object> attrs = node.getAttributeMap(true, true);
        for (Entry<String, Object> e : attrs.entrySet()) {
            String oneAttrName = e.getKey();
            if (oneAttrName.startsWith(attrName)) {
                // PSP phase mappings use the given name as a prefix. If this
                // attr looks like a PSP phase mapping for the given workflow,
                // delete it.
                node.removeAttribute(oneAttrName);

            } else if (oneAttrName.startsWith(PHASE_MAPPING_PREFIX)
                    && targetPhaseIDs.contains(e.getValue())) {
                // in rare cases, the ID of a workflow can change (due to
                // unusual user edits). If this attribute happens to map to
                // one of the phases in our target workflow, delete it.
                node.removeAttribute(oneAttrName);
            }
        }
    }

    private void storeNewMappings(WBSNode node, Map<String, String> changes,
            String attrName, boolean sameProject) {
        for (Entry<String, String> e : changes.entrySet()) {
            // for deleted mappings, there is nothing for us to save
            String mapsTo = e.getValue();
            if (!StringUtils.hasValue(mapsTo))
                continue;

            // look for changes that target the given node
            String changeFullId = e.getKey();
            Integer changeUniqueId = uniqueId(changeFullId);
            if (node.getUniqueID() != changeUniqueId)
                continue;

            // extract the phase suffix, if one is present
            String phaseSuffix = phaseSuffix(changeFullId);

            // save the new attribute into the node
            if (sameProject)
                mapsTo = makeRelative(mapsTo);
            node.setAttribute(attrName + phaseSuffix, mapsTo);
        }
    }


    /**
     * PSP phases use a trailing suffix (like "/Code" in the full IDs for each
     * workflow phase. If such a suffix is present, return it; otherwise, return
     * the empty string.
     */
    private String phaseSuffix(String fullId) {
        int slashPos = fullId.lastIndexOf('/');
        if (slashPos == -1)
            return "";
        else
            return fullId.substring(slashPos);
    }

    /**
     * The third colon-separated segment of a workflow/phase ID contains the
     * unique ID number of the node in question. Extract and parse that number.
     */
    private Integer uniqueId(String fullId) {
        int beg = fullId.lastIndexOf(':') + 1;
        int slashPos = fullId.indexOf('/', beg);
        try {
            if (slashPos == -1)
                return Integer.valueOf(fullId.substring(beg));
            else
                return Integer.valueOf(fullId.substring(beg, slashPos));
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    /**
     * When mapping between workflows in the same project, an abbreviated syntax
     * is used. This creates the abbreviated version of a full ID for use in
     * that scenario.
     */
    private String makeRelative(String fullId) {
        int colonPos = fullId.lastIndexOf(':');
        return PROJECT_RELATIVE_PREFIX + fullId.substring(colonPos + 1);
    }

    private static final String PHASE_MAPPING_PREFIX = "Phase Mapping ";

    private static final String PROJECT_RELATIVE_PREFIX = "WF:~:";

}
