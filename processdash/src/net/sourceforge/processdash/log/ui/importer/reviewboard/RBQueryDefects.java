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

package net.sourceforge.processdash.log.ui.importer.reviewboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.w3c.dom.Element;

import net.sourceforge.processdash.log.defects.DefectDataBag;
import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.ui.lib.binding.ErrorDataValueException;
import net.sourceforge.processdash.util.JSONUtils;
import net.sourceforge.processdash.util.StringUtils;

public class RBQueryDefects extends RBAbstractQuery {

    public RBQueryDefects(BoundMap map, Element xml) {
        super(map, xml);

        addParameter(xml, "reviewRequestId", "review_request_id");

        recalc();
    }

    @Override
    protected Object executeQuery(RBRestClient client, Object[] parameterValues)
            throws ErrorDataValueException {

        try {
            // retrieve the review request number
            String reviewRequestNumber = parameterValues[0].toString();

            // retrieve all of the comments associated with this request
            JSONObject response = client.performGet("reviews",
                "{review_request_id}", reviewRequestNumber, //
                "expand", StringUtils.join(COMMENT_TYPES, ","), //
                "max-results", "9999");

            // find the comments that open an issue
            List<Map> issues = getIssues(response);

            // convert the issues into appropriate defect data
            return convertDefects(issues);

        } catch (Exception e) {
            e.printStackTrace();
            return RB_REST_ERROR_VALUE;
        }
    }

    /** Find the comments in the JSON response that open issues */
    private List getIssues(JSONObject response) {
        List issues = new ArrayList();
        for (String commentType : COMMENT_TYPES) {
            String commentPath = "reviews." + commentType;
            List<Map> comments = JSONUtils.lookup(response, commentPath, false);
            for (Map comment : comments)
                if (isIssue(comment)) {
                    comment.put("comment_type", commentType);
                    issues.add(comment);
                }
        }
        return issues;
    }

    private boolean isIssue(Map comment) {
        return (Boolean.TRUE.equals(comment.get("issue_opened")) //
        && !"dropped".equals(comment.get("issue_status")));
    }


    /** Convert RB issues into Defect data maps */
    private List convertDefects(List<Map> rawIssueData) {

        List result = new ArrayList();
        for (Map rawIssue : rawIssueData) {
            Map defect = new HashMap();

            Object defectId = rawIssue.get("id");
            String type = getTypeFlag(rawIssue);
            defect.put(ATTR_ID, "RB" + type + defectId);
            defect.put(ATTR_DESCRIPTION, rawIssue.get("text"));
            defect.put(ATTR_DATE, parseDate(rawIssue.get("timestamp")));

            result.add(defect);
        }
        return result;
    }

    private String getTypeFlag(Map issue) {
        String type = (String) issue.get("comment_type");
        String flag = type.substring(0,1).toUpperCase();
        if ("D".equals(flag))
            return "";
        else
            return flag;
    }

    private Object parseDate(Object val) {
        Object result = RBRestClient.parseDate(val);
        return (result != null ? result : new Date());
    }


    private static final List<String> COMMENT_TYPES = Collections
            .unmodifiableList(Arrays.asList("diff_comments",
                "screenshot_comments", "file_attachment_comments"));

    private static final String ATTR_DATE = DefectDataBag.ATTRS[DefectDataBag.DATE];

    private static final String ATTR_DESCRIPTION = DefectDataBag.ATTRS[DefectDataBag.DESCRIPTION];

    private static final String ATTR_ID = DefectDataBag.ATTRS[DefectDataBag.ID];

}
