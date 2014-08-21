// Copyright (C) 2012-2014 Tuma Solutions, LLC
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.PatternList;

import teamdash.merge.AttributeMergeWarning;
import teamdash.merge.MergeWarning;
import teamdash.merge.ui.DataModelSource;
import teamdash.merge.ui.MergeConflictHandler;
import teamdash.merge.ui.MergeConflictNotification;
import teamdash.merge.ui.MergeConflictNotification.ModelType;

public class WBSModelMergeConflictNotificationFactory {

    public static List<MergeConflictNotification> createAll(
            AbstractWBSModelMerger<? extends WBSModel> merger) {
        List<MergeConflictNotification> result = new ArrayList();

        ModelType modelType = merger.getModelType();
        Map<Integer, WBSNode> baseNodeMap = merger.base.getNodeMap();
        Map<Integer, WBSNode> mainNodeMap = merger.main.getNodeMap();
        Map<Integer, WBSNode> incomingNodeMap = merger.incoming.getNodeMap();

        for (MergeWarning<Integer> warning : merger.getMergeWarnings()) {
            MergeConflictNotification notification = create(merger, modelType,
                baseNodeMap, mainNodeMap, incomingNodeMap, warning);
            if (notification != null)
                result.add(notification);
        }
        return result;
    }

    private static MergeConflictNotification create(
            AbstractWBSModelMerger merger, ModelType modelType,
            Map<Integer, WBSNode> baseNodeMap,
            Map<Integer, WBSNode> mainNodeMap,
            Map<Integer, WBSNode> incomingNodeMap, MergeWarning<Integer> mw) {

        MergeConflictNotification result = new MergeConflictNotification(
                modelType, mw);

        // look up the main and incoming nodes, and store them as attributes
        WBSNode mainNode = mainNodeMap.get(mw.getMainNodeID());
        WBSNode incomingNode = incomingNodeMap.get(mw.getIncomingNodeID());
        result.putNodeAttributes(mainNode, incomingNode);

        if (mw instanceof AttributeMergeWarning) {
            if (mw.matches(AbstractWBSModelMerger.NODE_NAME)) {
                NODE_NAME_HANDLER.install(result);
            } else if (mw.matches(AbstractWBSModelMerger.NODE_TYPE)) {
                NODE_TYPE_HANDLER.install(result);
            } else {
                WBSNode baseNode = baseNodeMap.get(mw.getIncomingNodeID());
                result.putAttribute(MergeConflictNotification.BASE_NODE,
                    baseNode, false);
            }

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

            // structural conflicts can only be acknowledged and dismissed;
            // resolution options are not available at this time.
            result.addUserOption(MergeConflictNotification.DISMISS, null);
        }

        return result;
    }



    public static void refineAll(List<MergeConflictNotification> notifications,
            DataModelSource dataModelSource) {

        for (Iterator i = notifications.iterator(); i.hasNext();) {
            MergeConflictNotification n = (MergeConflictNotification) i.next();
            if (refine(n, dataModelSource) == false)
                i.remove();
        }
    }


    private static boolean refine(MergeConflictNotification mcn,
            DataModelSource dms) {

        // When faced with generic attribute conflicts, the "createAll" method
        // will have generated notifications that have no user options at all.
        // detect this pattern and attempt to refine these notifications.
        if (mcn.getUserOptions().isEmpty()) {
            if (!refineAttributeNotification(mcn, dms))
                return false;
        }

        // Now, check to make certain the notification has a registered
        // description.  If it doesn't, log an error and don't display the
        // conflict notification to the user.
        try {
            mcn.formatDescription();
            return true;
        } catch (MissingResourceException mre) {
            System.err.println("Unexpected merge conflict key for "
                    + mcn.getModelType().name() + ": " + mre.getKey());
            return false;
        }
    }

    private static boolean refineAttributeNotification(
            MergeConflictNotification mcn, DataModelSource dms) {

        if (!(mcn.getMergeWarning() instanceof AttributeMergeWarning))
            return false;

        ModelType modelType = mcn.getModelType();
        AttributeMergeWarning amw = (AttributeMergeWarning) mcn.getMergeWarning();
        DataTableModel dataModel = dms.getDataModel(modelType);
        ConflictCapableDataColumn column = findColumnForAttribute(dataModel,
            amw.getAttributeName());
        if (column == null)
            return false;

        String columnId = column.getColumnID().replace(' ', '_');
        mcn.putAttribute(COLUMN_ID, columnId);
        mcn.putAttribute(COLUMN_NAME, column.getColumnName());

        String explicitMessageKey = modelType.name() + ".Attribute." + columnId;

        String nullDisplay;
        try {
            nullDisplay = resources.getHTML(explicitMessageKey + ".Blank");
        } catch (MissingResourceException mre) {
            nullDisplay = resources.getHTML("WBSNode_Attribute.Blank");
        }

        WBSNode baseNode = mcn.getAttribute(MergeConflictNotification.BASE_NODE);
        String baseValue = (String) amw.getBaseValue();
        Object baseDisp = (baseNode == null ? baseValue //
                : column.getConflictDisplayValue(baseValue, baseNode));
        if (baseDisp == null) baseDisp = nullDisplay;

        WBSNode mainNode = mcn.getAttribute(MergeConflictNotification.MAIN_NODE);
        String mainValue = (String) amw.getMainValue();
        Object mainDisp = (mainNode == null ? mainValue : //
                column.getConflictDisplayValue(mainValue, mainNode));
        if (mainDisp == null) mainDisp = nullDisplay;

        WBSNode incNode = mcn.getAttribute(MergeConflictNotification.INCOMING_NODE);
        String incValue = (String) amw.getIncomingValue();
        Object incDisp = (incNode == null ? incValue : //
                column.getConflictDisplayValue(incValue, incNode));
        if (incDisp == null) incDisp = nullDisplay;

        mcn.putValueAttributes(baseDisp, mainDisp, incDisp);

        boolean supportsOverride;

        if (MergeConflictNotification.definesDescription(explicitMessageKey)) {
            // this column has explicitly defined a message it wants to
            // display, so we will use that message.
            mcn.setMessageKey(explicitMessageKey);
            // see if this column has defined a message for the
            // 'override' option.
            supportsOverride = mcn.definesMessageForUserOption(
                MergeConflictNotification.OVERRIDE);

        } else {
            // use a generic message.
            mcn.setMessageKey("WBSNode_Attribute");
            supportsOverride = true;
        }

        // Add "accept" as an option that is always present
        mcn.addUserOption(MergeConflictNotification.ACCEPT, null);
        // Add an "alter" option if it is supported
        if (supportsOverride)
            mcn.addUserOption(MergeConflictNotification.OVERRIDE,
                new WbsNodeAttributeHandler(dataModel, column));

        // give the column a final chance to tweak the configuration
        column.adjustConflictNotification(mcn);

        return true;
    }

    private static ConflictCapableDataColumn findColumnForAttribute(
            DataTableModel model, String attrName) {
        if (model == null)
            return null;

        for (int i = model.getColumnCount();  i-- > 0; ) {
            DataColumn column = model.getColumn(i);
            if (column instanceof ConflictCapableDataColumn) {
                ConflictCapableDataColumn ccdc = (ConflictCapableDataColumn) column;
                PatternList pattern = ccdc.getConflictAttributeNamePattern();
                if (pattern != null && pattern.matches(attrName))
                    return ccdc;
            }
        }

        return null;
    }



    private static abstract class WbsNodeHandler implements MergeConflictHandler {

        public void install(MergeConflictNotification notification) {
            // reset the merge key to a generic value
            String attrName = notification
                    .getAttribute(MergeConflictNotification.ATTR_NAME);
            notification.setMessageKey(attrName);

            // register handlers for accepting and overriding the change
            notification.addUserOption(MergeConflictNotification.ACCEPT, null);
            notification.addUserOption(MergeConflictNotification.OVERRIDE, this);
        }

        public void handle(MergeConflictNotification notification,
                TeamProject teamProject) {
            // retrieve the WBSModel that this notification is associated with
            ModelType modelType = notification.getModelType();
            WBSModel model = (WBSModel) modelType
                    .getAssociatedModel(teamProject);

            // retrieve the node from that model that was affected
            AttributeMergeWarning<Integer> amw =
                (AttributeMergeWarning) notification.getMergeWarning();
            Integer nodeId = amw.getIncomingNodeID();
            WBSNode node = model.getNodeMap().get(nodeId);
            if (node == null)
                return;

            // make the change
            alterNode(node, amw.getIncomingValue());

            // fire a table model event to trigger repaints
            int row = model.getRowForNode(node);
            fireEvent(model, node, row);
        }

        protected void fireEvent(WBSModel model, WBSNode node, int row) {
            if (row != -1)
                model.fireTableCellUpdated(row, 0);
        }

        protected abstract void alterNode(WBSNode node, Object value);

    }

    private static final WbsNodeHandler NODE_NAME_HANDLER = new WbsNodeHandler() {
        protected void alterNode(WBSNode node, Object value) {
            node.setName((String) value);
        }};

    private static final WbsNodeHandler NODE_TYPE_HANDLER = new WbsNodeHandler() {
        protected void alterNode(WBSNode node, Object value) {
            node.setType((String) value);
        }};


    private static class WbsNodeAttributeHandler extends WbsNodeHandler {
        private DataTableModel dataModel;
        private ConflictCapableDataColumn column;

        public WbsNodeAttributeHandler(DataTableModel dataModel,
                ConflictCapableDataColumn column) {
            this.dataModel = dataModel;
            this.column = column;
        }

        @Override
        protected void alterNode(WBSNode node, Object value) {
            column.storeConflictResolutionValue(value, node);
        }

        @Override
        protected void fireEvent(WBSModel model, WBSNode node, int row) {
            int colPos = dataModel.findIndexOfColumn(column);
            if (row != -1 && colPos != -1)
                dataModel.fireTableCellUpdated(row, colPos);
            dataModel.columnChanged(column);
        }

    }

    private static final String COLUMN_ID = "columnId";

    public static final String COLUMN_NAME = "columnName";

    private static final Resources resources = Resources
            .getDashBundle("WBSEditor.Merge");

}
