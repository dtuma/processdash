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

package net.sourceforge.processdash.data.compiler.function;

import java.util.LinkedList;
import java.util.List;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.compiler.AbstractFunction;
import net.sourceforge.processdash.data.compiler.ExpressionContext;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.TaskLabeler;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;

public class Indivautolabels extends AbstractFunction {

    private static final String COMPLETED_TASKS = "Completed_Tasks";

    private static final String COMPLETED_COMPONENTS = "Completed_Components";

    static final String[] AUTO_LABEL_NAMES = { COMPLETED_TASKS,
            COMPLETED_COMPONENTS };


    public Object call(List arguments, ExpressionContext context) {
        String prefix;
        if (!arguments.isEmpty())
            prefix = asStringVal(getArg(arguments, 0));
        else
            prefix = context.get(ExpressionContext.PREFIXVAR_NAME).format();

        if (prefix == null)
            return null;

        return new CompletionCalculator(context, prefix).getAsLabels();
    }


    private static class CompletionCalculator {

        private ExpressionContext context;

        private DashHierarchy hier;

        private int incompleteTaskCount;

        private List<String> completedTaskIDs;

        private List<String> completedComponentTaskIDs;

        private List<String> currentComponentContents;

        CompletionCalculator(ExpressionContext context, String prefix) {
            this.context = context;

            ListData hierItem = (ListData) context
                    .get(DashHierarchy.DATA_REPOSITORY_NAME);
            this.hier = (DashHierarchy) hierItem.get(0);

            incompleteTaskCount = 0;
            completedTaskIDs = new LinkedList<String>();
            completedComponentTaskIDs = new LinkedList<String>();

            PropertyKey key = hier.findExistingKey(prefix);
            if (key != null)
                scan(key, false);
        }

        ListData getAsLabels() {
            ListData result = new ListData();

            result.add(TaskLabeler.LABEL_PREFIX + COMPLETED_TASKS);
            result.add(TaskLabeler.LABEL_HIDDEN_MARKER);
            for (String oneItem : completedTaskIDs)
                result.add(oneItem);

            result.add(TaskLabeler.LABEL_PREFIX + COMPLETED_COMPONENTS);
            result.add(TaskLabeler.LABEL_HIDDEN_MARKER);
            for (String oneItem : completedComponentTaskIDs)
                result.add(oneItem);

            return result;
        }

        private void scan(PropertyKey key, boolean parentIsPsp) {
            Prop p = hier.pget(key);
            String path = key.path();
            int numChildren = hier.getNumChildren(key);
            boolean isComponent = isComponent(p.getID());
            boolean isPsp = !isComponent && isPspTask(p.getID());
            boolean isLeafTask = !isComponent && numChildren == 0;

            if (isLeafTask) {
                SimpleData completionFlag = get(path, "Completed");

                if (completionFlag == null || !completionFlag.test()) {
                    incompleteTaskCount++;
                    currentComponentContents = null;

                } else if (!parentIsPsp) {
                    addTaskID(path, completedTaskIDs, currentComponentContents);
                }

            } else {
                if (isComponent)
                    currentComponentContents = new LinkedList();
                int savedIncompleteTaskCount = incompleteTaskCount;

                // recurse over children
                for (int i = 0; i < numChildren; i++)
                    scan(hier.getChildKey(key, i), isPsp);

                if (isPsp && incompleteTaskCount == savedIncompleteTaskCount)
                    addTaskID(path, completedTaskIDs);

                if (currentComponentContents != null) {
                    addTaskID(path, currentComponentContents);

                    if (isComponent) {
                        completedComponentTaskIDs
                                .addAll(currentComponentContents);
                        currentComponentContents = null;
                    }
                }
            }
        }

        private boolean isComponent(String templateID) {
            return templateID != null && templateID.endsWith("ReadOnlyNode");
        }

        private boolean isPspTask(String templateID) {
            return templateID != null && templateID.startsWith("PSP");
        }

        private void addTaskID(String path, List... dest) {
            ListData ld = ListData.asListData(get(path, "EV_Task_IDs"));
            if (ld != null && ld.test()) {
                for (List l : dest)
                    if (l != null)
                        l.add(ld.get(0));
            }
        }

        private SimpleData get(String prefix, String name) {
            return context.get(DataRepository.createDataName(prefix, name));
        }

    }

}
