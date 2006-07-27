// Copyright (C) 2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.util.glob;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.sourceforge.processdash.util.glob.analysis.DepthFirstAdapter;
import net.sourceforge.processdash.util.glob.node.AAndExpression;
import net.sourceforge.processdash.util.glob.node.AExpression;
import net.sourceforge.processdash.util.glob.node.AGlobMatchTerm;
import net.sourceforge.processdash.util.glob.node.AUnaryNotUnaryExpr;
import net.sourceforge.processdash.util.glob.node.POrClause;
import net.sourceforge.processdash.util.glob.node.PUnaryExpr;

class GlobSearchEvaluator extends DepthFirstAdapter {

    Set allValues;
    Map taggedValues;

    Set result;

    public GlobSearchEvaluator(Map taggedData) {
        this.taggedValues = taggedData;

        allValues = new HashSet();
        for (Iterator i = taggedData.values().iterator(); i.hasNext();) {
            Collection c = (Collection) i.next();
            allValues.addAll(c);
        }

        result = allValues;
    }

    public Set getResult() {
        return result;
    }

    public void caseAGlobMatchTerm(AGlobMatchTerm node) {
        GlobPattern glob = new GlobPattern(node.getMatchTerm().getText());

        Set result = new HashSet();
        for (Iterator i = taggedValues.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String tag = (String) e.getKey();
            if (glob.test(tag))
                result.addAll((Set) e.getValue());
        }

        this.result = result;
    }

    public void caseAUnaryNotUnaryExpr(AUnaryNotUnaryExpr node) {
        super.caseAUnaryNotUnaryExpr(node);

        Set result = new HashSet(allValues);
        result.removeAll(this.result);
        this.result = result;
    }

    public void caseAAndExpression(AAndExpression node) {
        Iterator terms = node.getUnaryExpr().iterator();

        // evaluate the first term in the AND clause.
        ((PUnaryExpr) terms.next()).apply(this);

        // evaluate each of the remaining terms in the AND clause, and
        // perform the intersection of each with the final result.
        while (terms.hasNext()) {
            Set currentResult = this.result;
            ((PUnaryExpr) terms.next()).apply(this);
            this.result.retainAll(currentResult);
        }
    }

    public void caseAExpression(AExpression node) {
        node.getAndExpression().apply(this);

        for (Iterator i = node.getOrClause().iterator(); i.hasNext();) {
            Set currentResult = this.result;
            ((POrClause) i.next()).apply(this);
            this.result.addAll(currentResult);
        }
    }

}
