package teamdash.wbs;

import org.w3c.dom.Element;

public class WorkflowWBSModel extends WBSModel {

    public WorkflowWBSModel() {
        super();
    }

    public WorkflowWBSModel(String name) {
        super(name);
    }

    public WorkflowWBSModel(Element e) {
        super(e);
    }

    public String filterNodeType(WBSNode node) {
        if (node.getIndentLevel() == 1)
            return "Workflow";
        else
            return super.filterNodeType(node);
    }

    public boolean isSoftwareComponent(String type) {
        return "Workflow".equals(type) || super.isSoftwareComponent(type);
    }

}
