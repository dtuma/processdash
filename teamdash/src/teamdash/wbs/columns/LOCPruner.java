package teamdash.wbs.columns;

import teamdash.wbs.WBSNode;

public class LOCPruner implements Pruner {

    public boolean shouldPrune(WBSNode node) {
        String nodeType = node.getType();
        if ("Software Component".equals(nodeType) ||
            "Project".equals(nodeType)) return false;

        return true;
    }

}
