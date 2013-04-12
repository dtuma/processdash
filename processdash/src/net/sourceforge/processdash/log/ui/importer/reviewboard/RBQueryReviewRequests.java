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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.ui.lib.binding.DynamicAttributeValue;
import net.sourceforge.processdash.ui.lib.binding.ErrorDataValueException;

public class RBQueryReviewRequests extends RBAbstractQuery {

    private DynamicAttributeValue username;

    private MessageFormat displayFormat;

    public RBQueryReviewRequests(BoundMap map, Element xml) {
        super(map, xml);

        username = getDynamicValue(xml, "username", NO_USERNAME);

        displayFormat = new MessageFormat(map.getResources().getString(
            "ReviewBoard.Review_Request_FMT"));

        recalc();
    }

    @Override
    protected Object executeQuery(RBRestClient client, Object[] parameterValues)
            throws ErrorDataValueException {

        try {
            String username = this.username.getValue();

            List reviews = client.performGet("review_requests", "from-user",
                username, "review_requests");
            List result = convertReviews(reviews);
            return result;

        } catch (ErrorDataValueException edve) {
            throw edve;

        } catch (Exception e) {
            e.printStackTrace();
            return RB_REST_ERROR_VALUE;
        }
    }

    private ArrayList convertReviews(List<Map> rawReviewData) {
        ArrayList result = new ArrayList();
        for (Map review : rawReviewData) {
            Object reviewId = review.get("id");
            Object reviewTitle = review.get("summary");

            String display = displayFormat.format(new Object[] { reviewId,
                    reviewTitle });

            review.put("VALUE", reviewId);
            review.put("DISPLAY", display);

            result.add(review);
        }
        return result;
    }

}
