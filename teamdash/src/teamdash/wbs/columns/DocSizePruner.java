package teamdash.wbs.columns;

import teamdash.wbs.WBSNode;

public class DocSizePruner extends LOCPruner {

    String type;

    public DocSizePruner(String type) {
        this.type = type;
    }

    public boolean shouldPrune(WBSNode node) {
        if (super.shouldPrune(node) == false) return false;
        if (type.equals(node.getType())) return false;
        return true;
    }

}
