package teamdash.wbs;

import java.util.EventObject;

public class WBSNodeEvent extends EventObject {

    public static final int NAME_CHANGE = 0;
    public static final int TYPE_CHANGE = 1;


    public WBSNodeEvent(WBSNode node, int id) {
        super(node);
        this.id = id;
    }


    private int id;
    public int getId() { return id; }

}
