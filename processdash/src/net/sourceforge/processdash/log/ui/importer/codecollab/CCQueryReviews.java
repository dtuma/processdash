// Copyright (C) 2009 Tuma Solutions, LLC
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
import java.util.Comparator;
import java.util.Map;

import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.ui.lib.binding.DynamicAttributeValue;
import net.sourceforge.processdash.ui.lib.binding.ErrorDataValueException;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.w3c.dom.Element;

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
    protected Object executeQuery(XmlRpcClient client, Object[] parameterValues)
            throws ErrorDataValueException {

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

        } catch (ErrorDataValueException edve) {
            throw edve;

        } catch (CCQuerySupport.SingleValueNotFoundException iae) {
            // this indicates that the user was not found.
            return null;

        } catch (Exception e) {
            return XMLRPC_ERROR_VALUE;
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
