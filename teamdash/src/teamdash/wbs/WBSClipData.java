package teamdash.wbs;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import teamdash.XMLUtils;

/** Serializable container for a list of WBSNodes.
 */
public class WBSClipData implements Serializable {

    private static final String DOC_TAG = "ClipboardWBSData";

    public transient List wbsNodes;

    public String xmlData;

    public WBSClipData(List wbsNodes) {
        this.wbsNodes = wbsNodes;
        this.xmlData = getXML(wbsNodes);
    }

    /** Construct an XML representation of our node list */
    private String getXML(List wbsNodes) {
        try {
            StringWriter out = new StringWriter();
            out.write("<" + DOC_TAG + ">\n");
            for (Iterator i = wbsNodes.iterator(); i.hasNext();) {
                WBSNode node = (WBSNode) i.next();
                node.getAsXML(out);
            }
            out.write("</" + DOC_TAG + ">\n");
            return out.toString();
        } catch (IOException e) {
            // can't happen
            return null;
        }
    }

    public List getWBSNodes(WBSModel forModel) {
        if (wbsNodes != null)
            // this indicates that we haven't been serialized and deserialized.
            // just return the original list of nodes we are holding.  (This
            // behavior is important for the "Cut" logic.)
            return wbsNodes;

        try {
            // reconstruct our node list from the XML data.
            Element e = XMLUtils.parse(xmlData).getDocumentElement();
            if (!DOC_TAG.equals(e.getTagName()))
                return null;

            List result = new ArrayList();
            NodeList wbsElements = e.getChildNodes();
            int len = wbsElements.getLength();
            for (int i = 0; i < len; i++) {
                Node n = wbsElements.item(i);
                if (n instanceof Element
                        && WBSNode.ELEMENT_NAME.equals(((Element) n)
                                .getTagName()))
                    result.add(new WBSNode(forModel, (Element) n));
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }
}
