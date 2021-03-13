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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.json.simple.JSONObject;
import org.w3c.dom.Element;

import net.sourceforge.processdash.log.defects.DefectDataBag;
import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.util.JSONUtils;

public class CCQueryDefects extends CCAbstractQuery {

    public CCQueryDefects(BoundMap map, Element xml) {
        super(map, xml);

        addParameter(xml, "reviewNumberId", "review_id");

        recalc();
    }


    @Override
    protected Object executeQuery(CCJsonClient connection,
            Object[] parameterValues) throws Exception {
        // connect to the API and retrieve the defects for this review
        Integer reviewId = Integer.parseInt(parameterValues[0].toString());
        JSONObject apiResponse = connection.execute(
            "ReviewService.getDefects", "reviewId", reviewId);
        checkJsonError(apiResponse);
        List<JSONObject> defects = JSONUtils.lookup(apiResponse,
            "result.defects", true);
        if (defects == null)
            return null;
        else
            return convertJsonDefects(defects);
    }

    private Object convertJsonDefects(List<JSONObject> rawDefectData) {
        List result = new ArrayList();
        for (JSONObject rawDefect : rawDefectData) {
            Map defect = new HashMap();

            defect.put(ATTR_ID, "C" + rawDefect.get("defectId"));
            defect.put(ATTR_TYPE, getJsonDefectType(rawDefect));
            defect.put(ATTR_DESCRIPTION, getJsonDefectDescription(rawDefect));
            defect.put(ATTR_FIX_TIME, 0);
            defect.put(ATTR_DATE, getJsonDefectDate(rawDefect));

            result.add(defect);
        }
        return result;
    }

    private Object getJsonDefectType(JSONObject rawDefect) {
        return JSONUtils.lookup(rawDefect, "userDefinedFields.Type", true);
    }

    private String getJsonDefectDescription(JSONObject rawDefect) {
        StringBuilder result = new StringBuilder();
        Object text = rawDefect.get("text");
        if (text instanceof String)
            result.append((String) text);

        String location = (String) rawDefect.get("location");
        if (result.length() > 0 && location != null)
            result.append("\n----------");
        if (location != null)
            result.append("\n").append(location);

        return result.toString();
    }

    private Object getJsonDefectDate(JSONObject rawDefect) {
        return JSONUtils.parseDate((String) rawDefect.get("creationDate"));
    }


    @Override
    protected Object executeQuery(XmlRpcClient client, Object[] parameterValues)
            throws Exception {
        // we explicitly parse the number as an Integer because it might
        // arrive as a String or a Long, but we need an Integer value.
        Integer reviewNumber = Integer.parseInt(parameterValues[0].toString());

        Object[] defects = CCQuerySupport.querySimple(client,
            CCQuerySupport.DEFECT_CLASS, 10000, "reviewId", reviewNumber);
        if (defects == null)
            return null;
        else
            return convertDefects(client, defects);
    }

    private List convertDefects(XmlRpcClient client, Object[] rawDefectData) {
        CCDefectTypeLookup typeLookup = CCDefectTypeLookup
                .getTypeLookup(client);

        List result = new ArrayList();
        for (Object o : rawDefectData) {
            Map rawDefect = (Map) o;
            Map defect = new HashMap();

            Integer defectId = (Integer) rawDefect.get("id");
            defect.put(ATTR_ID, "C" + defectId);
            defect.put(ATTR_TYPE, typeLookup.getType(defectId));
            defect.put(ATTR_DESCRIPTION, rawDefect.get("text"));
            defect.put(ATTR_FIX_TIME, 0);
            defect.put(ATTR_DATE, rawDefect.get("createdOn"));

            result.add(defect);
        }
        return result;
    }


    private static final String ATTR_DATE = DefectDataBag.ATTRS[DefectDataBag.DATE];

    private static final String ATTR_FIX_TIME = DefectDataBag.ATTRS[DefectDataBag.FIX_TIME];

    private static final String ATTR_DESCRIPTION = DefectDataBag.ATTRS[DefectDataBag.DESCRIPTION];

    private static final String ATTR_TYPE = DefectDataBag.ATTRS[DefectDataBag.TYPE];

    private static final String ATTR_ID = DefectDataBag.ATTRS[DefectDataBag.ID];

}
