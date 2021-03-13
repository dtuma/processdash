// Copyright (C) 2009-2021 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.ui.importer.codecollab;

import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.json.simple.JSONObject;
import org.w3c.dom.Element;

import net.sourceforge.processdash.ui.lib.binding.AbstractBoundQuery;
import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.ui.lib.binding.ErrorData;
import net.sourceforge.processdash.ui.lib.binding.ErrorDataValueException;
import net.sourceforge.processdash.ui.lib.binding.ErrorValue;

public abstract class CCAbstractQuery extends AbstractBoundQuery<CCClient> {

    public CCAbstractQuery(BoundMap map, Element xml) {
        super(map, xml, CCWebService.CC_DEFAULT_ID);
    }

    protected void addParameter(Element xml, String attrName, String defaultName) {
        String parameterName = getXmlAttr(xml, attrName, defaultName);
        addParameter(parameterName);
    }


    protected Object executeQuery(CCClient connection, Object[] parameterValues)
            throws ErrorDataValueException {
        try {
            // delegate to the right method based on the active connection type
            return connection.executeQuery(this, parameterValues);

        } catch (ErrorDataValueException edve) {
            throw edve;

        } catch (Exception e) {
            e.printStackTrace();
            return REST_ERROR_VALUE;
        }
    }

    protected abstract Object executeQuery(CCJsonClient connection,
            Object[] parameterValues) throws Exception;

    protected abstract Object executeQuery(XmlRpcClient connection,
            Object[] parameterValues) throws Exception;


    protected void checkJsonError(JSONObject apiResponse)
            throws ErrorDataValueException {
        String errorMessage = getJsonErrorMessage(apiResponse);
        if (errorMessage != null)
            throw new ErrorDataValueException(errorMessage, ErrorData.SEVERE);
    }

    protected String getJsonErrorMessage(JSONObject apiResponse) {
        Object errors = apiResponse.get("errors");
        if (errors instanceof List)
            return (String) ((List<Map>) errors).get(0).get("message");
        else
            return null;
    }


    protected static final ErrorValue REST_ERROR_VALUE = new ErrorValue(
            REST_ERROR, ErrorData.SEVERE);

}
