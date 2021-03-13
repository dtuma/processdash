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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.json.simple.JSONObject;
import org.w3c.dom.Element;

import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.ui.lib.binding.DynamicAttributeValue;
import net.sourceforge.processdash.util.JSONUtils;

public class CCQueryReviews extends CCAbstractQuery {

    private DynamicAttributeValue username;

    private MessageFormat displayFormat;

    public CCQueryReviews(BoundMap map, Element xml) {
        super(map, xml);

        username = getDynamicValue(xml, "username", NO_USERNAME);

        displayFormat = new MessageFormat(map.getResources().getString(
            "CodeCollab.Review_FMT"));

        recalc();
    }


    @Override
    protected Object executeQuery(CCJsonClient connection,
            Object[] parameterValues) throws Exception {
        // connect to the API and retrieve the action items for the user
        JSONObject apiResponse = connection
                .execute("UserService.getActionItems");
        checkJsonError(apiResponse);
        List<JSONObject> actionItems = JSONUtils.lookup(apiResponse,
            "result.actionItems", false);

        // iterate over the raw data and build a set of results
        List reviews = new ArrayList();
        for (JSONObject oneItem : actionItems) {
            Map oneReview = convertJsonReview(oneItem);
            if (oneReview != null)
                reviews.add(oneReview);
        }
        if (reviews.isEmpty())
            return null;

        // sort the results and return
        Collections.sort(reviews, new ReviewIdComparator());
        return reviews;
    }

    private JSONObject convertJsonReview(JSONObject oneItem) {
        // try to return only the reviews which the current user authored. Use a
        // lenient comparison strategy based on built-in Collaborator role names
        String role = (String) oneItem.get("roleText");
        if ("Moderator".equalsIgnoreCase(role)
                || "Reviewer".equalsIgnoreCase(role)
                || "Observer".equalsIgnoreCase(role))
            return null;

        // store extra values we need for display/handling
        Object reviewId = oneItem.get("reviewId");
        Object display = oneItem.get("reviewText");
        if (!(reviewId instanceof Number) || !(display instanceof String))
            return null;
        Integer id = Integer.valueOf(((Number) reviewId).intValue());
        oneItem.put("id", id);
        oneItem.put("VALUE", id);
        oneItem.put("DISPLAY", display);
        return oneItem;
    }


    @Override
    protected Object executeQuery(XmlRpcClient client, Object[] parameterValues)
            throws Exception {
        try {
            String username = this.username.getValue();
            Integer userId = (Integer) CCQuerySupport.lookupSingleValue(client,
                CCQuerySupport.USER_CLASS, "id", "login", username);

            Object[] reviews = CCQuerySupport.querySimple(client,
                CCQuerySupport.REVIEW_CLASS, 1000, "creatorId", userId);
            if (reviews == null || reviews.length == 0)
                return null;

            Arrays.sort(reviews, new ReviewIdComparator());
            return convertReviews(reviews);

        } catch (CCQuerySupport.SingleValueNotFoundException iae) {
            // this indicates that the user was not found.
            return null;
        }
    }

    private ArrayList convertReviews(Object[] rawReviewData) {
        ArrayList result = new ArrayList();
        for (Object o : rawReviewData) {
            Map review = (Map) o;
            Object reviewId = review.get("id");
            Object reviewTitle = review.get("title");
            Object reviewDate = review.get("creationDate");

            String display = displayFormat.format(new Object[] { reviewId,
                    reviewTitle, reviewDate });

            review.put("VALUE", reviewId);
            review.put("DISPLAY", display);

            result.add(review);
        }
        return result;
    }

    private static class ReviewIdComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            return getId(o2).compareTo(getId(o1));
        }

        private Integer getId(Object obj) {
            return (Integer) ((Map) obj).get("id");
        }
    }

}
