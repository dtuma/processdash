package teamdash.wbs;

import org.w3c.dom.Element;

/** Tweak the behavior of a WBSModel for use in editing common workflows.
 */
public class WorkflowWBSModel extends WBSModel {

    public WorkflowWBSModel()            { super();     }
    public WorkflowWBSModel(String name) { super(name); }
    public WorkflowWBSModel(Element e)   { super(e);    }

    /** Nodes at indentation level 1 are defined workflows. */
    public String filterNodeType(WBSNode node) {
        if (node.getIndentLevel() == 1)
            return "Workflow";
        else
            return super.filterNodeType(node);
    }

    /** Workflows behave like software components for validation purposes. */
    public boolean isSoftwareComponent(String type) {
        return "Workflow".equals(type) || super.isSoftwareComponent(type);
    }

}
