
package teamdash.wbs;

import java.util.EventListener;

public interface WBSNodeListener extends EventListener {

    public void nodeChanged(WBSNodeEvent event);

}
