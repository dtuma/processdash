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
import java.util.Iterator;

import net.sourceforge.processdash.util.glob.analysis.DepthFirstAdapter;
import net.sourceforge.processdash.util.glob.node.AAndExpression;
import net.sourceforge.processdash.util.glob.node.AExpression;
import net.sourceforge.processdash.util.glob.node.AGlobMatchTerm;
import net.sourceforge.processdash.util.glob.node.AUnaryNotUnaryExpr;
import net.sourceforge.processdash.util.glob.node.POrClause;
import net.sourceforge.processdash.util.glob.node.PUnaryExpr;

class GlobTestEvaluator extends DepthFirstAdapter {

    Collection words;
    boolean result = false;

    public GlobTestEvaluator(Collection words) {
        this.words = words;
    }

    public boolean getResult() {
        return result;
    }

    public void caseAGlobMatchTerm(AGlobMatchTerm node) {
        GlobPattern glob = new GlobPattern(node.getMatchTerm().getText());

        for (Iterator i = words.iterator(); i.hasNext();) {
            String word = (String) i.next();
            if (glob.test(word)) {
                result = true;
                return;
            }
        }

        result = false;
    }

    public void caseAUnaryNotUnaryExpr(AUnaryNotUnaryExpr node) {
        super.caseAUnaryNotUnaryExpr(node);
        result = !result;
    }

    public void caseAAndExpression(AAndExpression node) {
        Iterator terms = node.getUnaryExpr().iterator();

        // evaluate the first term in the AND clause.
        ((PUnaryExpr) terms.next()).apply(this);

        // evaluate each of the remaining terms in the AND clause.  If
        // any one evaluates false, stop.
        while (result && terms.hasNext()) {
            ((PUnaryExpr) terms.next()).apply(this);
        }
    }

    public void caseAExpression(AExpression node) {
        node.getAndExpression().apply(this);
        if (result)
            return;

        Iterator i = node.getOrClause().iterator();
        while (result == false && i.hasNext())
            ((POrClause) i.next()).apply(this);
    }

}
