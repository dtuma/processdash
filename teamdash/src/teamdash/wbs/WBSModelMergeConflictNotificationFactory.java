// Copyright (C) 2012 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.wbs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;

import teamdash.merge.AttributeMergeWarning;
import teamdash.merge.MergeWarning;
import teamdash.merge.ui.MergeConflictNotification;
import teamdash.merge.ui.MergeConflictNotification.ModelType;

public class WBSModelMergeConflictNotificationFactory {

    public static List<MergeConflictNotification> createAll(
            AbstractWBSModelMerger<? extends WBSModel> merger) {
        List<MergeConflictNotification> result = new ArrayList();

        ModelType modelType = merger.getModelType();
        Map<Integer, WBSNode> mainNodeMap = merger.main.getNodeMap();
        Map<Integer, WBSNode> incomingNodeMap = merger.incoming.getNodeMap();

        for (MergeWarning<Integer> warning : merger.getMergeWarnings()) {
            MergeConflictNotification notification = create(merger, modelType,
                mainNodeMap, incomingNodeMap, warning);
            if (notification != null)
                result.add(notification);
        }
        return result;
    }

    private static MergeConflictNotification create(
            AbstractWBSModelMerger merger, ModelType modelType,
            Map<Integer, WBSNode> mainNodeMap,
            Map<Integer, WBSNode> incomingNodeMap, MergeWarning<Integer> mw) {

        MergeConflictNotification result = new MergeConflictNotification(
                modelType, mw);

        // look up the main and incoming nodes, and store them as attributes
        WBSNode mainNode = mainNodeMap.get(mw.getMainNodeID());
        WBSNode incomingNode = incomingNodeMap.get(mw.getIncomingNodeID());
        result.putNodeAttributes(mainNode, incomingNode);

        if (mw instanceof AttributeMergeWarning) {
            // FIXME: need to handle attribute warnings

        } else {
            // for structural changes, record the parent nodes in case the
            // conflict message needs them
            WBSNode mainParent = merger.main.getParent(mainNode);
            result.putAttribute("mainParent", mainParent);
            WBSNode incomingParent = merger.incoming.getParent(incomingNode);
            result.putAttribute("incomingParent", incomingParent);

            // structural conflict messages are generic and shared by all of
            // the various WBS model types.  Thus, we switch to a plain
            // resource message key that is not prefixed by the model type.
            result.setMessageKey(mw.getKey());
        }

        result.addUserOption(MergeConflictNotification.ACCEPT, null);

        try {
            result.formatDescription();
            return result;
        } catch (MissingResourceException mre) {
            System.err.println("Unexpected merge conflict key for "
                    + modelType.name() + ": " + mre.getKey());
            return null;
        }
    }

}
