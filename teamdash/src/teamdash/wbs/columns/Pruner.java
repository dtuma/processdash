package teamdash.wbs.columns;

import teamdash.wbs.WBSNode;

/** Interface used by {@link TopDownBottomUpColumn} to identify nodes
 * which should be excluded from bottom-up calculations.
 */
public interface Pruner {

    /** Returns true if the given node should not participate in the
     * bottom-up calculation. Nodes that are pruned from the calculation (and
     * all the children of pruned nodes) will have neither their "top down"
     * nor their "bottom up" attribute set.  Instead, they will have their
     * "inherited" attribute set to the value of their nearest ancestor.
     */
    public boolean shouldPrune(WBSNode node);

}
