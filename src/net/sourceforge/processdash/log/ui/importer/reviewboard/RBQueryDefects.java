// Copyright (C) 2013-2016 Tuma Solutions, LLC
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.w3c.dom.Element;

import net.sourceforge.processdash.i18n.Resources;
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
            return convertDefects(issues, reviewRequestNumber);

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
    private List convertDefects(List<Map> rawIssueData,
            String reviewRequestNumber) {

        List result = new ArrayList();
        for (Map rawIssue : rawIssueData) {
            Map defect = new HashMap();

            Object defectId = rawIssue.get("id");
            String type = getTypeFlag(rawIssue);
            defect.put(ATTR_ID, "RB" + type + defectId);
            defect.put(ATTR_DESCRIPTION,
                getDefectDescription(rawIssue, reviewRequestNumber, type));
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

    private String getDefectDescription(Map issue, String reviewRequestNumber,
            String type) {
        StringBuilder result = new StringBuilder();
        Object text = issue.get("text");
        if (text instanceof String)
            result.append((String) text);

        String filename = getFilename(issue);
        String url = getReviewUrl(issue, reviewRequestNumber, type);

        if (result.length() > 0 && (filename != null || url != null))
            result.append("\n----------");
        if (filename != null)
            result.append("\n").append(filename);
        if (url != null)
            result.append("\n[").append(url).append("  ")
                    .append(map.getResource("Issue_URL_Title")).append("]");

        return result.toString();
    }

    private String getFilename(Map issue) {
        // most common case: extract filename from a file diff comment. If
        // this issue is not a file diff comment, exceptions will be thrown
        // and the next case will be tried
        try {
            String filename = getLinkAttr(issue, "filediff", "title");
            for (Pattern p : DIFF_FILENAME_PATTERNS) {
                Matcher m = p.matcher(filename);
                if (m.find()) {
                    filename = m.group(1);
                    break;
                }
            }
            int firstLine = parseInt(issue.get("first_line"));
            int numLines = parseInt(issue.get("num_lines"));
            int lastLine = firstLine + numLines - 1;
            return formatFilename(filename, firstLine, lastLine);
        } catch (Exception e) {
        }

        // next case: extract filename from a file attachment comment. If this
        // issue is not a file attachment comment, exceptions will be thrown
        try {
            String filename = getLinkAttr(issue, "file_attachment", "title");
            int firstLine, lastLine;
            try {
                Map extraData = (Map) issue.get("extra_data");
                firstLine = parseInt(extraData.get("beginLineNum"));
                lastLine = parseInt(extraData.get("endLineNum"));
            } catch (Exception e) {
                firstLine = lastLine = -1;
            }
            return formatFilename(filename, firstLine, lastLine);
        } catch (Exception e) {
        }

        // This is not a comment type that we recognize.
        return null;
    }

    private String getReviewUrl(Map issue, String reviewRequestNumber,
            String type) {
        String baseUrl;
        try {
            String href = getLinkAttr(issue, "self", "href");
            int pos = href.indexOf("/api/");
            baseUrl = href.substring(0, pos);
        } catch (Exception e) {
            return null;
        }

        String reviewUrl = (String) issue.get("review_url");
        if (StringUtils.hasValue(reviewUrl))
            return baseUrl + reviewUrl;

        Object commentId = issue.get("id");
        reviewUrl = baseUrl + "/r/" + reviewRequestNumber + "/#"
                + type.toLowerCase() + "comment" + commentId;
        return reviewUrl;
    }

    private String formatFilename(String filename, int firstLine, int lastLine) {
        Resources res = (Resources) map.getResources();
        if (firstLine < 0)
            return res.format("Filename.No_Lines_FMT", filename);
        else if (lastLine > firstLine)
            return res.format("Filename.Multiple_Lines_FMT", filename,
                firstLine, lastLine);
        else
            return res.format("Filename.One_Line_FMT", filename, firstLine);
    }

    private String getLinkAttr(Map issue, String linkName, String attrName)
            throws NullPointerException, ClassCastException {
        Map links = (Map) issue.get("links");
        Map link = (Map) links.get(linkName);
        return (String) link.get(attrName);
    }

    private int parseInt(Object val) {
        if (val instanceof Number)
            return ((Number) val).intValue();
        else if (val instanceof String)
            return Integer.parseInt((String) val);
        else
            return -1;
    }

    private Object parseDate(Object val) {
        Object result = RBRestClient.parseDate(val);
        return (result != null ? result : new Date());
    }


    private static final List<String> COMMENT_TYPES = Collections
            .unmodifiableList(Arrays.asList("diff_comments",
                "screenshot_comments", "file_attachment_comments"));

    private static final Pattern[] DIFF_FILENAME_PATTERNS = {
        Pattern.compile(" -> (.+) \\(")
    };

    private static final String ATTR_DATE = DefectDataBag.ATTRS[DefectDataBag.DATE];

    private static final String ATTR_DESCRIPTION = DefectDataBag.ATTRS[DefectDataBag.DESCRIPTION];

    private static final String ATTR_ID = DefectDataBag.ATTRS[DefectDataBag.ID];

}
