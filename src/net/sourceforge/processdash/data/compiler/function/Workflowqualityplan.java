// Copyright (C) 2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.data.compiler.function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.ResultSetData;
import net.sourceforge.processdash.data.compiler.AbstractFunction;
import net.sourceforge.processdash.data.compiler.ExpressionContext;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.log.defects.Defect;
import net.sourceforge.processdash.log.defects.DefectAnalyzer;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.tool.db.WorkflowEnactmentHelper;
import net.sourceforge.processdash.tool.db.WorkflowEnactmentHelper.TaskMapType;
import net.sourceforge.processdash.tool.db.WorkflowEnactmentHelper.TaskNodeType;

public class Workflowqualityplan extends AbstractFunction {

    public static final String DATA_NAME = "Quality Plan";

    /**
     * Perform a procedure call.
     * 
     * This method <b>must</b> be thread-safe.
     * 
     * Expected arguments: (ListData l)
     * 
     * Where l contains a single item, of type WorkflowEnactmentHelper.
     * 
     * This uses the given WorkflowEnactmentHelper to compute a full quality
     * plan for this component, and returns the resulting plan in a
     * {@link ResultSetData} object.
     */
    public Object call(List arguments, ExpressionContext context) {
        // get the WorkflowEnactmentHelper argument
        WorkflowEnactmentHelper workflow;
        try {
            ListData weh = (ListData) getArg(arguments, 0);
            workflow = (WorkflowEnactmentHelper) weh.get(0);
        } catch (Exception e) {
            return new ResultSetData(Collections.EMPTY_LIST, COLUMN_NAMES);
        }

        // get the dashboard hierarchy
        ListData hierItem = (ListData) context
                .get(DashHierarchy.DATA_REPOSITORY_NAME);
        DashHierarchy hier = (DashHierarchy) hierItem.get(0);

        // build a Phase object for each step in the workflow
        Map<String, String> phaseNames = workflow.getWorkflowPhaseNames();
        Map<String, Phase> phases = new LinkedHashMap();
        for (Entry<String, String> e : phaseNames.entrySet()) {
            Phase onePhase = new Phase(e.getValue(), e.getKey(), context);
            phases.put(onePhase.shortID, onePhase);
        }

        // scan the tasks in the workflow, and add their data to the phases
        DefectCounter defectCounter = new DefectCounter(phases);
        Map<String, String> tasks = workflow
                .getEnactmentTasks(TaskMapType.PhaseID);
        for (Entry<String, String> e : tasks.entrySet()) {
            // retrieve information about a single enactment task
            String taskPath = e.getKey();
            TaskNodeType nodeType = workflow.getNodeType(taskPath);
            String phaseID = e.getValue();
            String phaseNum = getNumericPhaseID(phaseID);

            // store time data for this task
            Phase phase = phases.get(phaseNum);
            if (phase != null) {
                boolean isLeaf = (nodeType == TaskNodeType.Leaf
                        || nodeType == TaskNodeType.PspPhase);
                phase.addTimeData(taskPath, isLeaf, context);
            }

            // find defects for this task, and store the actual counts
            if (nodeType != TaskNodeType.PspPhase) {
                DefectAnalyzer.run(hier, taskPath, false, defectCounter);
            }
        }

        // run the quality model calculations
        Phase runningDefects = new Phase("running");
        for (Phase phase : phases.values())
            phase.calcPhaseData(runningDefects, defectCounter.total);
        defectCounter.after.estDefRem = runningDefects.estDefInj;

        // build the result set from the list of phase objects
        List resultData = new ArrayList(phases.size() + 3);
        resultData.add(defectCounter.before.toDataArray());
        for (Phase phase : phases.values())
            resultData.add(phase.toDataArray());
        resultData.add(defectCounter.total.toDataArray());
        resultData.add(defectCounter.after.toDataArray());
        return new ResultSetData(resultData, COLUMN_NAMES);
    }



    private static class DefectCounter implements DefectAnalyzer.Task {

        Map<String, Phase> phases;

        Phase before, after, total;

        public DefectCounter(Map<String, Phase> phases) {
            this.phases = phases;
            this.before = new Phase("before");
            this.after = new Phase("after");
            this.total = new Phase("total");
        }

        @Override
        public void analyze(String path, Defect d) {
            Phase inj = getPhase(d.injected.phaseID);
            Phase rem = getPhase(d.removed.phaseID);

            if (inj != null) {
                inj.actDefInj += d.fix_count;
                total.actDefInj += d.fix_count;
            } else if (rem != null) {
                before.actDefInj += d.fix_count;
            }

            if (rem != null) {
                rem.actDefRem += d.fix_count;
                total.actDefRem += d.fix_count;
            } else if (inj != null) {
                after.actDefRem += d.fix_count;
            }
        }

        private Phase getPhase(String phaseID) {
            if (phaseID == null)
                return null;

            for (String oneID : phaseID.split(",")) {
                String oneShortID = getNumericPhaseID(oneID);
                Phase p = phases.get(oneShortID);
                if (p != null)
                    return p;
            }

            return null;
        }

    }



    private static class Phase {

        private String name, fullID, shortID;

        private double estInjRate, actInjRate;

        private double estYield, actYield;

        private double estTime, actTime;

        private double estDefInj, actDefInj, actPctInj;

        private double estDefRem, actDefRem, actPctRem;

        private double estRemRate, actRemRate;

        public Phase(String id) {
            this.name = this.fullID = this.shortID = id;
        }

        public Phase(String name, String fullID, ExpressionContext ctx) {
            this.name = name;
            this.fullID = fullID;
            this.shortID = getNumericPhaseID(fullID);

            String paramPrefix = TeamDataConstants.WORKFLOW_PARAM_PREFIX + "/"
                    + shortID + "/";
            this.estInjRate = asDouble(
                ctx.get(paramPrefix + "Estimated Defects Injected per Hour"));
            this.estYield = asDouble(
                ctx.get(paramPrefix + "Estimated % Phase Yield"));
        }

        public void addTimeData(String path, boolean isLeaf,
                ExpressionContext ctx) {
            if (isLeaf) {
                estTime += asDouble(ctx.get(path + "/Estimated Time"));
                actTime += asDouble(ctx.get(path + "/Time"));
            } else {
                actTime += asDouble(ctx.get(path + "/Orphaned Time"));
            }
        }

        /**
         * run the quality model calculations to estimate defect flows into and
         * out of this phase
         * 
         * @param running
         *            an object that tracks a running number of defects. On
         *            entry, the estDefInj field should contain the number of
         *            defects that we estimate have escaped the previous phases.
         *            The actDefInj field should contain the number of known
         *            defects that have actually escaped the previous phases.
         *            This method will update those fields based on defect
         *            injection/removal activities that occur in this phase. (No
         *            other fields in this object are used.)
         * @param total
         *            an object that tracks total defect counts. On entry, the
         *            actDefInj and actDefRem fields should already be populated
         *            with actual defect counts. On exit, the estDefInj and
         *            estDefRem fields will be updated based on the estimated
         *            defect injection/removal activities in this phase.
         */
        public void calcPhaseData(Phase running, Phase total) {
            // estimate the # defects that will be injected in this phase
            estDefInj = estInjRate * estTime / 60;
            running.estDefInj += estDefInj;
            total.estDefInj += estDefInj;

            // use yield to estimate the defects that will be removed
            estDefRem = running.estDefInj * estYield;
            running.estDefInj -= estDefRem;
            total.estDefRem += estDefRem;

            // calculate actual phase yield
            running.actDefInj += actDefInj;
            actYield = nanToZero(actDefRem / running.actDefInj);
            running.actDefInj -= actDefRem;

            // calculate injection/removal rates
            actInjRate = nanToZero(actDefInj * 60 / actTime);
            estRemRate = nanToZero(estDefRem * 60 / estTime);
            actRemRate = nanToZero(actDefRem * 60 / actTime);

            // calculate phase percentages
            actPctInj = nanToInf(actDefInj / total.actDefInj);
            actPctRem = nanToInf(actDefRem / total.actDefRem);
        }

        private double nanToZero(double d) {
            return (Double.isNaN(d) ? 0 : d);
        }

        private double nanToInf(double d) {
            return (Double.isNaN(d) ? Double.POSITIVE_INFINITY : d);
        }

        public Object[] toDataArray() {
            return new Object[] { fullID, shortID, name, //
                    estInjRate, actInjRate, //
                    estYield, actYield, //
                    estTime, actTime, //
                    estDefInj, actDefInj, actPctInj, //
                    estDefRem, actDefRem, actPctRem, //
                    estRemRate, actRemRate, //
            };
        }

    }

    private static String getNumericPhaseID(String fullID) {
        if (fullID == null)
            return null;
        int colonPos = fullID.lastIndexOf(':');
        return fullID.substring(colonPos + 1);
    }

    private static final String[] COLUMN_NAMES = { //
            "fullID", "id", "name", //
            "estInjRate", "actInjRate", //
            "estYield", "actYield", //
            "estTime", "actTime", //
            "estDefInj", "actDefInj", "actPctInj", //
            "estDefRem", "actDefRem", "actPctRem", //
            "estRemRate", "actRemRate", //
    };

}
