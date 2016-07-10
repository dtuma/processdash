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

package net.sourceforge.processdash.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import net.sourceforge.processdash.util.XMLUtils;

public class WorkflowInfo {

    public class Workflow {

        private String workflowName, workflowId;

        private List<Phase> phases;

        private Workflow(Element xml) {
            this.workflowName = xml.getAttribute("name");
            this.workflowId = xml.getAttribute("tid");
            this.phases = new ArrayList<WorkflowInfo.Phase>();
        }

        public String getWorkflowName() {
            return workflowName;
        }

        public String getWorkflowId() {
            return workflowId;
        }

        public List<Phase> getPhases() {
            return phases;
        }

    }


    public class Phase {

        private Workflow workflow;

        private String phaseName, phaseId, mcfPhase;

        private boolean isPspPhase;

        private Phase(Workflow workflow, Element xml, String phaseName,
                String pspPhase) {
            this.workflow = workflow;
            this.phaseName = phaseName;
            this.phaseId = xml.getAttribute("tid");
            String shortPhaseId = xml.getAttribute("id");

            if (pspPhase == null) {
                this.mcfPhase = xml.getAttribute("phaseName");
                this.isPspPhase = false;
            } else {
                this.mcfPhase = pspPhase;
                this.phaseName = this.phaseName + "/" + pspPhase;
                this.phaseId = this.phaseId + "/" + pspPhase;
                this.isPspPhase = true;
                shortPhaseId = shortPhaseId + "/" + pspPhase;
            }

            workflow.phases.add(this);
            phases.put(phaseId, this);
            phases.put(shortPhaseId, this);
        }

        public Workflow getWorkflow() {
            return workflow;
        }

        public String getPhaseName() {
            return phaseName;
        }

        public String getPhaseId() {
            return phaseId;
        }

        public String getMcfPhase() {
            return mcfPhase;
        }

        public boolean isPspPhase() {
            return isPspPhase;
        }

    }


    private List<Workflow> workflows;

    private Map<String, Phase> phases;


    public WorkflowInfo(Element xml) {
        this.workflows = new ArrayList<WorkflowInfo.Workflow>();
        this.phases = new HashMap<String, WorkflowInfo.Phase>();

        for (Element child : XMLUtils.getChildElements(xml))
            buildWorkflow(child);

        phases.remove(null);
        phases.remove("");
        this.phases = Collections.unmodifiableMap(phases);
        this.workflows = Collections.unmodifiableList(workflows);
    }

    public boolean isEmpty() {
        return workflows.isEmpty();
    }

    public List<Workflow> getWorkflows() {
        return workflows;
    }

    public Workflow getWorkflow(String name) {
        for (Workflow w : workflows)
            if (w.workflowName.equals(name))
                return w;
        return null;
    }

    public Map<String, Phase> getPhaseMap() {
        return phases;
    }

    public Phase getPhase(String phaseId) {
        return phases.get(phaseId);
    }

    public Phase getPhase(String workflowName, String phaseName) {
        Workflow workflow = getWorkflow(workflowName);
        if (workflow != null){
            for (Phase phase : workflow.getPhases()) {
                if (phase.getPhaseName().equalsIgnoreCase(phaseName))
                    return phase;
            }
        }

        return null;
    }

    private void buildWorkflow(Element xml) {
        if ("workflow".equals(xml.getTagName())) {
            Workflow workflow = new Workflow(xml);
            buildPhases(workflow, xml, null);
            if (!workflow.phases.isEmpty()) {
                workflow.phases = Collections.unmodifiableList(workflow.phases);
                workflows.add(workflow);
            }
        }
    }

    private void buildPhases(Workflow workflow, Element xml, String nodeName) {
        List<Element> children = XMLUtils.getChildElements(xml);
        if ("psp".equals(xml.getTagName())) {
            for (String phase : PSP_PHASES)
                new Phase(workflow, xml, nodeName, phase);

        } else if (children.isEmpty() && nodeName != null) {
            new Phase(workflow, xml, nodeName, null);

        } else {
            for (Element child : children) {
                String childName = child.getAttribute("name");
                String fullName = (nodeName == null ? childName : nodeName
                        + "/" + childName);
                buildPhases(workflow, child, fullName);
            }
        }
    }

    private static final String[] PSP_PHASES = { "Planning", "Design",
            "Design Review", "Code", "Code Review", "Compile", "Test",
            "Postmortem" };

}
