// Copyright (C) 2015 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import org.json.simple.JSONObject;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.log.time.DashboardTimeLog;
import net.sourceforge.processdash.log.time.TimeLoggingModel;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.StringUtils;


public class TaskStatusApi extends TinyCGIBase {

    @Override
    public void service(InputStream in, OutputStream out, Map env)
            throws IOException {
        try {
            charset = "UTF-8";
            super.service(in, out, env);
        } catch (WebApiException wae) {
            WebApiUtils.sendError(this.out, wae);
        }
    }


    @Override
    protected void doGet() throws IOException {
        checkRequestValidity();

        String path = getTargetPath();

        DateData completionDate = getCompletionDate(path);
        JSONObject json = new JSONObject();
        json.put("stat", "ok");
        json.put("taskPath", path);
        json.put("completionDate", completionDate == null ? null
                : WebApiUtils.DATE_FMT.format(completionDate.getValue()));

        out.write("Content-Type: application/json\r\n\r\n");
        out.write(json.toString());
        out.flush();
    }


    @Override
    protected void doPost() throws IOException {
        checkRequestValidity();
        parseFormData();

        String path = getTargetPath();

        String newDateParam = getParameter("completionDate");
        if (newDateParam != null)
            storeCompletionDate(path, newDateParam);

        out.write("Content-Type: application/json\r\n\r\n");
        out.write("{\"stat\":\"ok\"}");
        out.flush();
    }


    private void checkRequestValidity() {
        WebApiUtils.checkRequestOrigin(env);
        if (!Settings.isPersonalMode())
            throw new WebApiException("personal-only", 400,
                    "Task status is only applicable for personal dashboards.");
    }


    /** Retrieve the target path from the incoming HTTP request */
    private String getTargetPath() {
        // use the prefix, if one was supplied in the request
        String result = getPrefix();

        // if the request did not supply a prefix, use the path of the currently
        // selected task
        if (!StringUtils.hasValue(result)) {
            DashboardContext ctx = getDashboardContext();
            DashboardTimeLog tl = (DashboardTimeLog) ctx.getTimeLog();
            TimeLoggingModel tlm = tl.getTimeLoggingModel();
            result = tlm.getActiveTaskModel().getPath();
        }

        // verify that the named path exists and is a leaf task
        DashHierarchy hier = getPSPProperties();
        PropertyKey key = hier.findExistingKey(result);
        if (key == null)
            throw new WebApiException("no-such-task", 404, "The task '"
                    + result + "' was not found.");
        else if (hier.getNumChildren(key) != 0)
            throw new WebApiException("not-leaf-task", 400, "The item '"
                    + result + "' is not a leaf task.");

        return result;
    }

    private DateData getCompletionDate(String path) {
        String dataName = path + "/Completed";
        SimpleData sd = getDataContext().getSimpleValue(dataName);
        if (sd instanceof DateData)
            return (DateData) sd;
        else
            return null;
    }

    private void storeCompletionDate(String path, String newDateParam) {
        DateData newDate = null;
        if (!"".equals(newDateParam) && !"null".equals(newDateParam)) {
            try {
                Date d;
                if ("now".equals(newDateParam))
                    d = new Date();
                else
                    d = WebApiUtils.DATE_FMT.parse(newDateParam);
                newDate = new DateData(d, true);
            } catch (ParseException e) {
                throw new WebApiException("parameter-invalid", 400,
                        "The 'completionDate' parameter value '" + newDateParam
                                + "' is not a valid date.").causedBy(e)
                        .putAttr("param", "completionDate");
            }
        }

        String dataName = path + "/Completed";
        SaveableData currentDate = getDataContext().getValue(dataName);
        if (currentDate != null && !(currentDate instanceof DateData))
            throw new WebApiException("date-not-editable", 400,
                    "The completion date is not editable for task '" + path
                            + "'.");

        getDataContext().putValue(dataName, newDate);
    }

}
