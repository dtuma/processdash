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

package net.sourceforge.processdash.net.http;

import java.util.Map;

import org.eclipse.jetty.server.Request;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.api.PDashContext;
import net.sourceforge.processdash.api.PDashData;
import net.sourceforge.processdash.api.PDashQuery;
import net.sourceforge.processdash.data.repository.PDashDataImpl;
import net.sourceforge.processdash.tool.db.PDashQueryImpl;
import net.sourceforge.processdash.util.HTMLUtils;

public class PdashContextImpl implements PDashContext {

    private DashboardContext dashCtx;

    private String uriPrefix;

    private String projectPath;

    private boolean useFilters;

    private PDashData _data;

    private PDashQuery _query;

    protected PdashContextImpl(Request baseRequest, String uriPrefix) {
        this.dashCtx = (DashboardContext) ((Map) baseRequest.getConnection()
                .getServer().getAttribute(WebServer.DEFAULT_ENV_KEY))
                .get(TinyCGI.DASHBOARD_CONTEXT);

        this.uriPrefix = uriPrefix;

        if (uriPrefix.length() < 2) {
            this.projectPath = "";
            this.useFilters = false;
        } else {
            this.projectPath = HTMLUtils.urlDecode(uriPrefix.substring(0,
                uriPrefix.length() - 1));
            this.useFilters = PDashServletUtils.isSnippetRequest(baseRequest);
        }
    }

    public String getProjectPath() {
        return projectPath;
    }

    public String getUriPrefix() {
        return uriPrefix;
    }

    public PDashData getData() {
        if (_data == null && dashCtx != null)
            _data = new PDashDataImpl(dashCtx.getData(), projectPath);
        return _data;
    }

    public PDashQuery getQuery() {
        if (_query == null && dashCtx != null)
            _query = new PDashQueryImpl(dashCtx, projectPath, useFilters);
        return _query;
    }

}
