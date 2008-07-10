package teamdash;

import java.util.Comparator;
import java.util.List;

import javax.swing.Action;

/**
 * Class used to compare 2 Actions. The property that is used to compare the Actions is
 *  their category. The category is simply the value under the ACTION_CATEGORY key,
 *  in the Actions' key/value list.
 */
public class ActionCategoryComparator implements Comparator<Action> {

    /** The Action category key, used in the Action's key/value list */
    public static final String ACTION_CATEGORY = "actionCategory";

    /** A array that contains all categories in the order that the comparator
     * should sort them */
    private List<String> categoryOrder = null;

    public ActionCategoryComparator(List<String> categoryOrder) {
        this.categoryOrder = categoryOrder;
    }

    public int compare(Action a1, Action a2) {
        int categoryPos1 = categoryOrder.indexOf((String) a1.getValue(ACTION_CATEGORY));
        int categoryPos2 = categoryOrder.indexOf((String) a2.getValue(ACTION_CATEGORY));

        // If an Action's category is not in the list, it should be considered as
        //  "greater than" the other action.
        if (categoryPos1 == -1)
            categoryPos1 = Integer.MAX_VALUE;
        if (categoryPos2 == -1)
            categoryPos2 = Integer.MAX_VALUE;

        return categoryPos1 - categoryPos2;
    }

}
