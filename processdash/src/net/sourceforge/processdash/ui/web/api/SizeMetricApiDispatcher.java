// Copyright (C) 2013 Tuma Solutions, LLC
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

import static net.sourceforge.processdash.ui.web.api.SizeMetricApiHandler.UNSUPPORTED_TARGET_PATH;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.log.time.DashboardTimeLog;
import net.sourceforge.processdash.log.time.TimeLoggingModel;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.StringUtils;

import org.w3c.dom.Element;

public class SizeMetricApiDispatcher extends TinyCGIBase {

    public static final String REGISTRATION_DATA_ELEM = "Size_Metric_API_Handler";

    public static final String EXTENSION_POINT = "sizeMetricApi";

    private static final Logger log = Logger
            .getLogger(SizeMetricApiDispatcher.class.getName());

    @Override
    protected void doGet() throws IOException {
        throw new TinyCGIException(405,
                "Request method 'GET' is not supported by this API.");
    }

    @Override
    protected void doPost() throws IOException {
        WebApiUtils.checkRequestOrigin(env);
        parseFormData();

        try {
            String targetPath = getTargetPath();
            SizeMetricApiHandler handler = getHandler(targetPath);
            dispatchToHandler(targetPath, handler);
        } catch (WebApiException wae) {
            WebApiUtils.sendError(out, wae);
            if (wae.getHttpCode() / 100 == 5)
                log.log(Level.SEVERE,
                    "Unexpected error in SizeMetricApiDispatcher", wae);
        }
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

        return result;
    }



    /**
     * Get the handler that should be used to handle API requests for the given
     * target path.
     */
    private SizeMetricApiHandler getHandler(String targetPath) {
        String extensionPointId = getHandlerId(targetPath);
        Element configElement = getHandlerConfigElement(extensionPointId);
        return createHandler(extensionPointId, configElement);
    }

    protected String getHandlerId(String targetPath) {
        // look for a data element stating the ID of the extension point to use.
        SaveableData registration = getDataRepository().getInheritableValue(
            targetPath, REGISTRATION_DATA_ELEM);
        if (registration == null)
            throw new WebApiException(UNSUPPORTED_TARGET_PATH, 400,
                    "The path '" + targetPath + "' does not support the "
                            + "storage of size metrics through this API.");
        else
            return registration.getSimpleValue().format();
    }

    protected Element getHandlerConfigElement(String extensionPointId) {
        // look through the registered extension points for one with this ID.
        List elements = ExtensionManager
                .getXmlConfigurationElements(EXTENSION_POINT);
        for (Iterator i = elements.iterator(); i.hasNext();) {
            Element xml = (Element) i.next();
            String id = xml.getAttribute("id");
            if (extensionPointId.equals(id))
                return xml;
        }

        // no config element was found with the given ID.
        throw new WebApiException(UNSUPPORTED_TARGET_PATH, 500,
                "Bad internal configuration; no size metric API handler "
                        + "was found with ID " + extensionPointId);
    }

    private SizeMetricApiHandler createHandler(String extensionPointId,
            Element configElement) {
        try {
            Object result = ExtensionManager.getExecutableExtension(
                configElement, "class", getDashboardContext());
            return (SizeMetricApiHandler) result;
        } catch (Exception e) {
            throw new WebApiException(UNSUPPORTED_TARGET_PATH, 500,
                    "Unable to create SizeMetricApiHandler with ID "
                            + extensionPointId).causedBy(e);
        }
    }



    /**
     * Dispatch this size metric API request to the given handler
     */
    private void dispatchToHandler(String targetPath,
            SizeMetricApiHandler handler) throws IOException {
        SizeMetricApiRequestData request = new SizeMetricApiRequestData();
        request.ctx = getDashboardContext();
        request.env = this.env;
        request.params = this.parameters;
        request.targetPath = targetPath;
        request.outputStream = this.outStream;

        handler.handleSizeApiRequest(request);
    }

}
