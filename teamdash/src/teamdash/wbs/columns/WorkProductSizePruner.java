package teamdash.wbs.columns;

import java.util.Collection;

import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSNode;

public class WorkProductSizePruner implements Pruner {

    private TeamProcess teamProcess;

    private Collection sizesToKeep;

    public WorkProductSizePruner(TeamProcess teamProcess, Collection sizesToKeep) {
        this.teamProcess = teamProcess;
        this.sizesToKeep = sizesToKeep;
    }

    public boolean shouldPrune(WBSNode node) {
        String nodeType = node.getType();
        Object size = teamProcess.getWorkProductSizeMap().get(nodeType);
        return size == null || !sizesToKeep.contains(size);
    }

}
