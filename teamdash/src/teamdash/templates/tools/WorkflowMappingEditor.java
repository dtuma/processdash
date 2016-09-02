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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.http.PDashServletUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

import teamdash.templates.tools.WorkflowMappingManager.Workflow;

public class WorkflowMappingEditor extends HttpServlet {

    private static final String LIST_PARAM = "list";

    private static final String SOURCE_PARAM = "source";

    private static final String TARGET_PARAM = "target";

    private static final String FOCUS_PARAM = "focus";

    private static final String ADD_PARAM = "add";

    private static final String ADDING_PARAM = "adding";

    private static final String EDIT_PARAM = "edit";

    static final Resources resources = Resources
            .getDashBundle("WBSEditor.WorkflowMap");


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            if (hasParam(req, SOURCE_PARAM))
                showWorkflowPhasesPage(req, resp);
            else if (hasParam(req, ADD_PARAM))
                showAddWorkflowMapPage(req, resp);
            else
                showWorkflowMapListingPage(req, resp);

        } catch (WorkflowMappingManager.NotFound nf) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, nf.getMessage());
        } catch (HttpException he) {
            resp.sendError(he.statusCode, he.message);
        }

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        maybeSaveMappingChanges(req, resp);
    }

    private void showWorkflowMapListingPage(HttpServletRequest req,
            HttpServletResponse resp) throws ServletException, IOException {

        // get the workflow mapping business object
        WorkflowMappingManager mgr = new WorkflowMappingManager(
                PDashServletUtils.getContext(req));

        // retrieve the workflow ID from the request
        String workflowID = requireWorkflowIdParam(req, LIST_PARAM);

        // look up information about the given workflow
        req.setAttribute("workflow", mgr.getWorkflow(workflowID));
        req.setAttribute("imported", mgr.getImportedWorkflows(workflowID));
        req.setAttribute("exported", mgr.getExportedWorkflows(workflowID));

        // display a page with the mappings for the given workflow
        showView(req, resp, "workflowMapList.jsp");
    }

    private void showAddWorkflowMapPage(HttpServletRequest req,
            HttpServletResponse resp) throws ServletException, IOException {

        // get the workflow mapping business object
        WorkflowMappingManager mgr = new WorkflowMappingManager(
                PDashServletUtils.getContext(req));

        // retrieve parameters from the request
        String workflowID = requireWorkflowIdParam(req, ADD_PARAM);
        String focus = requireParam(req, FOCUS_PARAM);
        boolean importing = TARGET_PARAM.equals(focus);

        // look up information we need to display the page
        req.setAttribute("workflow", mgr.getWorkflow(workflowID));
        req.setAttribute("allWorkflows",
            mgr.getAllWorkflowsExcept(workflowID, importing));
        req.setAttribute("importing", importing);

        // display a page allowing the user to select a workflow to add
        showView(req, resp, "workflowMapAdd.jsp");
    }

    private void showWorkflowPhasesPage(HttpServletRequest req,
            HttpServletResponse resp) throws ServletException, IOException {

        // get the workflow mapping business object
        WorkflowMappingManager mgr = new WorkflowMappingManager(
                PDashServletUtils.getContext(req));

        // retrieve the IDs of the workflows to map
        String sourceId = requireWorkflowIdParam(req, SOURCE_PARAM);
        String targetId = requireWorkflowIdParam(req, TARGET_PARAM);

        // look up information about the given workflows
        Workflow sourceWorkflow = mgr.getWorkflow(sourceId);
        Workflow targetWorkflow = mgr.getWorkflow(targetId);
        mgr.loadPhases(sourceWorkflow);
        mgr.loadPhases(targetWorkflow);
        mgr.loadPhaseMappings(sourceWorkflow, targetWorkflow);

        // save the workflows into the request
        req.setAttribute("sourceWorkflow", sourceWorkflow);
        req.setAttribute("targetWorkflow", targetWorkflow);

        // determine whether editing should be allowed/in effect
        if (mgr.canEditMappings(sourceId, targetId)) {
            if (hasParam(req, EDIT_PARAM))
                req.setAttribute("editing", Boolean.TRUE);
            else
                req.setAttribute("editingAllowed", Boolean.TRUE);
        }

        // display a page to edit mappings for the given workflow
        showView(req, resp, "workflowMapPhases.jsp");
    }

    private void maybeSaveMappingChanges(HttpServletRequest req,
            HttpServletResponse resp) throws ServletException, IOException {

        // save the changed phase data if requested
        if (hasParam(req, "save")) {
            Map<String, String> changes = getChangedPhaseMappings(req);
            if (changes != null) {
                // get the workflow mapping business object
                WorkflowMappingManager mgr = new WorkflowMappingManager(
                        PDashServletUtils.getContext(req));

                // retrieve the IDs of the workflows to map
                String sourceId = requireWorkflowIdParam(req, SOURCE_PARAM);
                String targetId = requireWorkflowIdParam(req, TARGET_PARAM);

                // look up the workflows in question
                Workflow workflow = mgr.getWorkflow(sourceId);
                Workflow target = mgr.getWorkflow(targetId);
                mgr.loadPhases(workflow);
                mgr.loadPhases(target);

                try {
                    // save the new mappings
                    mgr.saveChangedMappings(workflow, target, changes,
                        PDashServletUtils.buildEnvironment(req));

                } catch (WorkflowMappingException e) {
                    // on error, display a page to the user
                    String errorMessage = resources.format(
                        "Errors." + e.getErrorCode(), e.getArg());
                    String html = HTMLUtils.escapeEntities(errorMessage);
                    html = StringUtils.findAndReplace(html, "[[", "<pre>");
                    html = StringUtils.findAndReplace(html, "]]", "</pre>");
                    req.setAttribute("errorMessageHtml", html);
                    req.setAttribute("workflow", workflow);
                    showView(req, resp, "workflowMapEditError.jsp");
                    e.printStackTrace();
                    return;
                }
            }
        } else if (hasParam(req, ADDING_PARAM)) {
            StringBuffer url = req.getRequestURL();
            String focus = req.getParameter(FOCUS_PARAM);
            String workflowId = req.getParameter(focus);
            HTMLUtils.appendQuery(url, LIST_PARAM, workflowId);
            resp.sendRedirect(url.toString());
            return;
        }

        // redirect to the phase mapping view page for the two workflows
        redirectToPhasesPage(req, resp);
    }

    private Map<String, String> getChangedPhaseMappings(HttpServletRequest req) {
        Map<String, String> result = new HashMap<String, String>();
        boolean sawAtLeastOneChange = false;
        boolean sawAtLeastOneMapping = false;
        for (String i : req.getParameterValues("phase")) {
            // read POST-ed parameters for this phase
            String prefix = "phase" + i;
            String phaseID = requireParam(req, prefix + "id");
            String mapsToID = req.getParameter(prefix + "mapsTo");
            String origMapping = req.getParameter(prefix + "origMapsTo");

            // keep track of whether we've seen any changes, and any mappings
            if (!mapsToID.equals(origMapping))
                sawAtLeastOneChange = true;
            if (!mapsToID.isEmpty())
                sawAtLeastOneMapping = true;

            // record this mapping in our result set
            result.put(phaseID, mapsToID);
        }

        if (sawAtLeastOneChange == false)
            // if there were no changes, return null so the parent knows not to
            // bother with a save operation
            result = null;

        else if (sawAtLeastOneMapping == false)
            // if there used to be mappings, but the user cleared them all, make
            // a special note to delete all mappings between these workflows
            result.put(WorkflowMappingManager.DELETE_MAPPINGS, "true");

        return result;
    }

    private void redirectToPhasesPage(HttpServletRequest req,
            HttpServletResponse resp) throws IOException {

        // read the parameters from the request
        String source = req.getParameter(SOURCE_PARAM);
        String target = req.getParameter(TARGET_PARAM);
        String focus = req.getParameter(FOCUS_PARAM);

        // if the "reverse" parameter was present, swap the direction
        if (hasParam(req, "reverse")) {
            String tmp = source;
            source = target;
            target = tmp;
            focus = (SOURCE_PARAM.equals(focus) ? TARGET_PARAM : SOURCE_PARAM);
        }

        StringBuffer url = req.getRequestURL();
        HTMLUtils.appendQuery(url, SOURCE_PARAM, source);
        HTMLUtils.appendQuery(url, TARGET_PARAM, target);
        HTMLUtils.appendQuery(url, FOCUS_PARAM, focus);
        if (hasParam(req, EDIT_PARAM)) {
            HTMLUtils.appendQuery(url, EDIT_PARAM, "t");
            if (hasParam(req, ADDING_PARAM))
                HTMLUtils.appendQuery(url, ADDING_PARAM, "t");
        }
        resp.sendRedirect(url.toString());
    }


    private String requireWorkflowIdParam(HttpServletRequest req,
            String paramName) {
        String workflowID = requireParam(req, paramName);
        if (!workflowID.startsWith("WF:"))
            throw new HttpException(HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid '" + paramName + "' identifier");
        else
            return workflowID;
    }

    private String requireParam(HttpServletRequest req, String paramName) {
        String paramValue = req.getParameter(paramName);
        if (!StringUtils.hasValue(paramValue))
            throw new HttpException(HttpServletResponse.SC_BAD_REQUEST,
                    "Missing '" + paramName + "' parameter");
        return paramValue;
    }

    private boolean hasParam(HttpServletRequest req, String paramName) {
        return StringUtils.hasValue(req.getParameter(paramName));
    }


    private void showView(HttpServletRequest req, HttpServletResponse resp,
            String viewName) throws ServletException, IOException {
        req.setAttribute("resources", resources.asJSTLMap());
        RequestDispatcher disp = getServletContext().getRequestDispatcher(
            "/WEB-INF/jsp/" + viewName);
        disp.forward(req, resp);
    }



    private static class HttpException extends RuntimeException {
        private int statusCode;

        private String message;

        HttpException(int statusCode, String message) {
            this.statusCode = statusCode;
            this.message = message;
        }
    }

}
