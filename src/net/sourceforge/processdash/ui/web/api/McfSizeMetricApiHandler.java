// Copyright (C) 2013-2017 Tuma Solutions, LLC
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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;

import org.w3c.dom.Element;

import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.ui.UserNotificationManager;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

/**
 * This class responds to the SizeMetricApi on behalf of any standard metrics
 * collection framework, adding metrics to the Size Inventory Form.
 */
public class McfSizeMetricApiHandler implements SizeMetricApiHandler {


    private String notificationUri = "[PID]/sizeForm.class?showHighlightedRows=[TIME]";

    public void setConfigElement(Element xml, String attrName) {
        String uri = xml.getAttribute("notificationUri");
        if (StringUtils.hasValue(uri))
            this.notificationUri = uri;
    }



    /**
     * Process an incoming request from the size metric API.
     */
    public void handleSizeApiRequest(SizeMetricApiRequestData request)
            throws IOException {

        // get the data repository path where the size metric should be stored
        String dataPrefix = getTargetPrefix(request);
        if (dataPrefix == null)
            throw badRequest(UNSUPPORTED_TARGET_PATH,
                "Cannot store size data at the specified path '",
                request.targetPath, "'");

        // write the data into the repository
        validateAndStoreData(request, dataPrefix);

        // show a notification if requested
        maybeShowNotification(request, dataPrefix);

        // send a response back to the client
        writeResponse(request);
    }



    /**
     * Look in the data repository and validate that the targetPath is an
     * acceptable place to store size metrics data. If so, find an empty row in
     * the Size Inventory Form, and return the complete data repository prefix
     * for that empty row.
     */
    private String getTargetPrefix(SizeMetricApiRequestData request) {
        // make certain a task exists in the hierarchy at the specified path
        PropertyKey pathKey = request.ctx.getHierarchy().findExistingKey(
            request.targetPath);
        if (pathKey == null)
            return null;

        // make certain the task is an Indiv node within a team project
        String pathTemplateId = request.ctx.getHierarchy().getID(pathKey);
        if (!pathTemplateId.contains("/Indiv"))
            return null;

        // make certain the task is capable of storing size metrics
        if (!hasData(request, "Local_Sized_Object_List//All"))
            return null;

        // find the next unused number in the object list, and return a
        // data prefix for the corresponding row
        for (int rowNum = 0; true; rowNum++) {
            String rowPrefix = SIZED_OBJ_PREFIX + "/" + rowNum;

            boolean rowHasData = false;
            for (String oneElem : DATA_ELEMS)
                rowHasData = rowHasData
                        || hasData(request, rowPrefix + "/" + oneElem);

            if (!rowHasData)
                return request.targetPath + "/" + rowPrefix;
        }
    }



    /**
     * Retrieve the size data parameters from the incoming request and ensure
     * they represent valid size metric data. If so, write them into the data
     * repository.
     * 
     * @param request
     *            the request we are processing
     * @param dataPrefix
     *            the data repository prefix where metrics should be stored
     */
    private void validateAndStoreData(SizeMetricApiRequestData request,
            String dataPrefix) {
        // parse and validate the supplied parameter values
        StringData description = getDescription(request);
        StringData sizeUnits = getSizeUnits(request);
        DoubleData actSize = getActualSize(request);

        // store them into the data repository
        putData(request, dataPrefix + "/" + DESCRIPTION_ELEM, description);
        putData(request, dataPrefix + "/" + UNITS_ELEM, sizeUnits);
        putData(request, dataPrefix + "/" + ACT_SIZE_ELEM, actSize);
    }


    /** Retrieve the description from the input params, if one is present */
    private StringData getDescription(SizeMetricApiRequestData request) {
        String paramVal = (String) request.params.get("description");
        return (paramVal == null ? null : StringData.create(paramVal.trim()));
    }


    /**
     * Retrieve the size units from the input params.
     * 
     * @param request
     *            the request we are processing
     * @return the size metric
     */
    private StringData getSizeUnits(SizeMetricApiRequestData request) {
        String paramVal = (String) request.params.get("sizeUnits");
        if (!StringUtils.hasValue(paramVal) //
                || LOC.equalsIgnoreCase(paramVal) //
                || NC_LOC.equalsIgnoreCase(paramVal))
            return StringData.create(NC_LOC);
        if (DLD_LINES.equalsIgnoreCase(paramVal))
            return StringData.create(DLD_LINES);

        String processID = getStringData(request, "Process_ID");
        ListData allowedUnits = ListData.asListData(getData(request, //
            "/" + processID + "/Custom_Size_Metric_List"));
        if (allowedUnits == null)
            throw badRequest(UNSUPPORTED_TARGET_PATH, "Cannot store size ",
                "data; could not identify the process for task '",
                request.targetPath, "'");

        for (int i = 0; i < allowedUnits.size(); i++) {
            String oneUnit = allowedUnits.get(i).toString();
            if (paramVal.equalsIgnoreCase(oneUnit))
                return StringData.create(oneUnit);
        }

        throw badRequest(BAD_PARAM, "The sizeUnits parameter '", paramVal,
            "' does not name a valid size metric in the '", processID,
            "' process.").putAttr("param", "sizeUnits");
    }


    /** Retrieve the actual size from the request parameters */
    private DoubleData getActualSize(SizeMetricApiRequestData request) {
        String paramVal = (String) request.params.get("actSize");
        if (!StringUtils.hasValue(paramVal))
            throw badRequest(MISSING_PARAM, "No value was provided for the ",
                "required parameter 'actSize'").putAttr("param", "actSize");

        try {
            double result = Double.parseDouble(paramVal.trim());
            return new DoubleData(result);
        } catch (NumberFormatException nfe) {
            throw badRequest(BAD_PARAM, "The 'actSize' parameter value '",
                paramVal, "' is not a valid number")
                    .putAttr("param", "actSize");
        }
    }



    /**
     * Register a user notification if requested by the client.
     */
    private void maybeShowNotification(SizeMetricApiRequestData request,
            String dataPrefix) {
        if ("true".equals(request.params.get("notify"))) {
            DateData timestamp = new DateData();
            writeHighlightFlags(request, dataPrefix, timestamp);
            addNotification(request, timestamp);
        }
    }

    /**
     * Write data elements into the repository to flag the target path and the
     * new size inventory row as "Added"
     */
    private void writeHighlightFlags(SizeMetricApiRequestData request,
            String dataPrefix, DateData timestamp) {
        putData(request, dataPrefix + HIGHLIGHT_FLAG_SUFFIX, timestamp);
        putData(request, request.targetPath + HIGHLIGHT_FLAG_SUFFIX, timestamp);
    }

    /** Register a notification with the UserNotificationManager */
    private void addNotification(SizeMetricApiRequestData request,
            DateData timestamp) {

        // find the root node of the team project, and retrieve the project ID
        StringBuffer projectPrefixBuf = new StringBuffer(request.targetPath);
        SaveableData projID = request.ctx.getData().getInheritableValue(
            projectPrefixBuf, "Project_ID");
        if (projID == null)
            return;
        String projectPrefix = projectPrefixBuf.toString();

        // construct an ID representing this user notification. We construct
        // an ID that will be the same for multiple API invocations against
        // this same team project; this way, the multiple invocations will
        // only spawn a single notification
        String notificationId = McfSizeMetricApiHandler.class.getName() + ":"
                + projID.getSimpleValue().format();

        // retrieve the message that should be displayed to the user
        String message = resources.format("Notification_Message_FMT",
            projectPrefix);

        // construct the URL that we will use to open the Size Inventory Form,
        // and make a Runnable action to open it.
        String processID = getStringData(request, projectPrefix + "/Process_ID");
        String notificationUri = this.notificationUri;
        notificationUri = StringUtils.findAndReplace(notificationUri, //
            "[PID]", processID);
        notificationUri = StringUtils.findAndReplace(notificationUri, //
            "[TIME]", Long.toString(timestamp.getValue().getTime()));
        if (notificationUri.startsWith("/"))
            notificationUri = notificationUri.substring(1);
        String notificationUrl = WebServer.urlEncodePath(projectPrefix) + "//"
                + notificationUri;
        notificationUrl = HTMLUtils.appendQuery(notificationUrl,
            "removeNotification", notificationId);

        // register this notification info with the UserNotificationManager
        UserNotificationManager.getInstance().addNotification(notificationId,
            message, notificationUrl);
    }



    /** Write an "OK" response to the client */
    private void writeResponse(SizeMetricApiRequestData request)
            throws IOException {
        Writer out = new OutputStreamWriter(request.outputStream, "UTF-8");
        out.write("Content-Type: application/json\r\n\r\n{ \"stat\":\"ok\" }");
        out.flush();
    }



    //
    // Utility methods
    //

    private boolean hasData(SizeMetricApiRequestData request,
            String dataElemName) {
        return getData(request, dataElemName) != null;
    }

    private String getStringData(SizeMetricApiRequestData request,
            String dataElemName) {
        SaveableData value = getData(request, dataElemName);

        SimpleData result = null;
        if (value != null)
            result = value.getSimpleValue();

        return (result == null ? null : result.format());
    }

    private SaveableData getData(SizeMetricApiRequestData request,
            String dataElemName) {
        String dataName = DataRepository.createDataName(request.targetPath,
            dataElemName);
        return request.ctx.getData().getValue(dataName);
    }

    private void putData(SizeMetricApiRequestData request, String dataElemName,
            SaveableData value) {
        String dataName = DataRepository.createDataName(request.targetPath,
            dataElemName);
        request.ctx.getData().putValue(dataName, value);
    }

    private WebApiException badRequest(String code, String... message) {
        return new WebApiException(code, 400, StringUtils.join(
            Arrays.asList(message), ""));
    }

    private static final String SIZED_OBJ_PREFIX = "Sized_Objects";

    private static final String DESCRIPTION_ELEM = "Description";

    private static final String UNITS_ELEM = "Sized_Object_Units";

    private static final String EST_SIZE_ELEM = "Estimated Size";

    private static final String ACT_SIZE_ELEM = "Size";

    private static final String[] DATA_ELEMS = { DESCRIPTION_ELEM, UNITS_ELEM,
            EST_SIZE_ELEM, ACT_SIZE_ELEM };

    private static final String LOC = "LOC";

    private static final String NC_LOC = "New & Changed LOC";

    private static final String DLD_LINES = "DLD Lines";

    private static final String HIGHLIGHT_FLAG_SUFFIX = "//Show_Highlight";

    private static final Resources resources = Resources
            .getDashBundle("Templates.SizeMetricApi");

}
