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

package teamdash.wbs;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.http.PDashServletUtils;
import net.sourceforge.processdash.util.StringUtils;

public class WorkflowMappingEditor extends HttpServlet {

    private static final String LIST_PARAM = "list";

    static final Resources resources = Resources
            .getDashBundle("WBSEditor.WorkflowMap");


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            showWorkflowMapListingPage(req, resp);

        } catch (WorkflowMappingManager.NotFound nf) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, nf.getMessage());
        } catch (HttpException he) {
            resp.sendError(he.statusCode, he.message);
        }

    }

    private void showWorkflowMapListingPage(HttpServletRequest req,
            HttpServletResponse resp) throws ServletException, IOException {

        // get the query object
        WorkflowMappingManager mgr = new WorkflowMappingManager(
                PDashServletUtils.getContext(req));

        // retrieve the workflow ID from the request
        String workflowID = requireParam(req, LIST_PARAM);
        if (!workflowID.startsWith("WF:"))
            throw new HttpException(HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid '" + LIST_PARAM + "' identifier");

        // look up information about the given workflow
        req.setAttribute("workflow", mgr.getWorkflow(workflowID));
        req.setAttribute("imported", mgr.getImportedWorkflows(workflowID));
        req.setAttribute("exported", mgr.getExportedWorkflows(workflowID));

        // display a page with the mappings for the given workflow
        showView(req, resp, "workflowMapList.jsp");
    }


    private String requireParam(HttpServletRequest req, String paramName) {
        String paramValue = req.getParameter(paramName);
        if (!StringUtils.hasValue(paramValue))
            throw new HttpException(HttpServletResponse.SC_BAD_REQUEST,
                    "Missing '" + paramName + "' parameter");
        return paramValue;
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
