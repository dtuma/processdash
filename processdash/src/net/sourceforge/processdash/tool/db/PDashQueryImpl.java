// Copyright (C) 2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.api.PDashQuery;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.tool.db.DatabaseMetadata.PlanItemEntityNames;
import net.sourceforge.processdash.tool.db.DatabaseMetadata.VersionedEntityNames;
import net.sourceforge.processdash.util.MockMap;
import net.sourceforge.processdash.util.StringUtils;

public class PDashQueryImpl extends MockMap<String, Object> implements
        PDashQuery {

    private DashboardContext ctx;

    private String prefix;

    private DatabasePlugin databasePlugin;

    private String lastQuery;

    public PDashQueryImpl(DashboardContext ctx, String prefix) {
        this.ctx = ctx;
        this.prefix = prefix;
        this.databasePlugin = QueryUtils.getDatabasePlugin(ctx.getData());
    }

    @Override
    public Object get(Object key) {
        if (key == null)
            return null;
        if ("lastHql".equals(key))
            return lastQuery;

        String query = key.toString();
        if (query.indexOf('?') == -1)
            return query(query);
        else
            return new DeferredQuery(query);
    }

    public String getLastHql() {
        return lastQuery;
    }

    public List query(String baseQuery, Object... baseArgs) {
        // if the database plugin is not available, abort.
        if (databasePlugin == null)
            return null;

        // prepare the query for execution
        StringBuilder query = new StringBuilder(baseQuery);
        List queryArgs = new ArrayList(Arrays.asList(baseArgs));
        maybeAddAutofilteredCriteria(query, queryArgs);

        // execute the query and return the results
        lastQuery = query.toString();
        List results = databasePlugin.getObject(QueryRunner.class).queryHql(
            lastQuery, queryArgs.toArray());
        return results;
    }

    private void maybeAddAutofilteredCriteria(StringBuilder query,
            List queryArgs) {
        // check and see if the user wants autofiltering disabled.
        boolean autofilteringDisabled = queryArgs.remove(NO_AUTOFILTER);

        if (autofilteringDisabled) {
            // if no autofiltering is being performed, wait for all project
            // data to be loaded.
            QueryUtils.waitForAllProjects(databasePlugin);

        } else {
            // Find any versioned entities in the query, and add "is current"
            // conditions to the WHERE clause
            addCriteriaForEntities(query, queryArgs, getVersionedEntityNames(),
                IS_CURRENT_CRITERIA);

            // Add criteria to limit the query to data from the current project
            addProjectSpecificCriteria(query, queryArgs);
        }
    }

    private void addCriteriaForEntities(StringBuilder query, List args,
            Collection<String> entityNames, List criteria) {
        if (entityNames != null && criteria != null && !criteria.isEmpty())
            for (String entity : entityNames)
                QueryUtils.addCriteriaToHqlIfEntityPresent(query, entity, args,
                    criteria);
    }

    private Collection<String> getVersionedEntityNames() {
        try {
            return databasePlugin.getObject(VersionedEntityNames.class);
        } catch (Exception e) {
            // if an older version of the database plugin is operating, return
            // the historically accurate list of known versioned entities
            return Arrays.asList("PersonAttrFact", "OrganizationAttrFact",
                "EvScheduleBridge", "DataBlockAttrFact", "SizeFact",
                "DefectLogFact", "TaskStatusFact", "ProjectAttrFact",
                "TeamAttrFact", "PlanItemNoteFact", "PlanItemAttrFact",
                "PlanItemHistory", "EvSchedulePeriodFact", "TimeLogFact",
                "TaskDateFact", "EvMetricNumberFact");
        }
    }

    private static final List IS_CURRENT_CRITERIA = Collections
            .singletonList(QueryUtils.IS_CURRENT_CRITERIA);

    private void addProjectSpecificCriteria(StringBuilder query, List args) {
        // if no prefix is in effect, do not limit the query to any project
        if (prefix == null || prefix.length() < 2)
            return;

        // get the criteria we should use for the project
        List projectCriteria = getProjectSpecificCriteria();

        // add these critiera to the standard plan item entities
        addCriteriaForEntities(query, args, getPlanItemEntityNames(),
            projectCriteria);

        // special handling when the query references PlanItem directly
        String alias = QueryUtils.addCriteriaToHqlIfEntityPresent(query,
            "PlanItem", args, projectCriteria);
        if (alias != null)
            StringUtils.findAndReplace(query, alias + ".planItem", alias);

        // special handling when the query references ProcessEnactment
        alias = QueryUtils.addCriteriaToHqlIfEntityPresent(query,
            "ProcessEnactment", args, projectCriteria);
        if (alias != null)
            StringUtils.findAndReplace(query, alias + ".planItem", //
                alias + ".rootItem");
    }

    private List getProjectSpecificCriteria() {
        List result = new ArrayList();
        result.add(QueryUtils.PROJECT_CRITERIA);

        ListData filter = ListData.asListData(ctx.getData()
                .getInheritableValue(this.prefix, "DB_Project_Keys"));
        if (filter != null)
            result.addAll(filter.asList());

        return result;
    }

    private Collection<String> getPlanItemEntityNames() {
        try {
            return databasePlugin.getObject(PlanItemEntityNames.class);
        } catch (Exception e) {
            // if an older version of the database plugin is operating, return
            // the historically accurate list of known planItem entities
            return Arrays.asList("TaskStatusFact", "PlanItemNoteFact",
                "PlanItemAttrFact", "PlanItemHistory", "TaskDateFact",
                "TimeLogFact", "SizeFact", "DefectLogFact");
        }
    }

    /**
     * This object collects query parameters via the Map interface, and executes
     * a query when all the parameters have been provided.
     */
    private class DeferredQuery extends MockMap {

        private String query;

        private List args;

        private int numArgsNeeded;

        public DeferredQuery(String query) {
            this.query = query;
            this.args = new ArrayList();
            this.numArgsNeeded = query.split("\\?", -1).length - 1;
        }

        @Override
        public Object get(Object key) {
            args.add(key);
            if (args.size() == numArgsNeeded)
                return query(query, args.toArray());
            else
                return this;
        }
    }

}
