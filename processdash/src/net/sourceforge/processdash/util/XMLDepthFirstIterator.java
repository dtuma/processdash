// Copyright (C) 2001-2003 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.util;

import java.util.List;
import java.util.Stack;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Entity;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Notation;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;


public class XMLDepthFirstIterator {

    public static final int PRE = 0;
    public static final int POST = 1;

    public void run(Node n) {
        run(n, new Stack());
    }

    public void run(Node n, Stack path) {
        path.push(getPathObj(n));
        if (getOrdering() == PRE) doNode(n, path);
        if (n.hasChildNodes()) {
            NodeList l = n.getChildNodes();
            int len = l.getLength();
            for (int i=0; i<len; i++)
                run(l.item(i), path);
        }
        if (getOrdering() == POST) doNode(n, path);
        path.pop();
    }

    private final void doNode(Node n, List path) {
        switch (n.getNodeType()) {
        case Node.ATTRIBUTE_NODE:
            caseAttr((Attr) n, path); break;
        case Node.CDATA_SECTION_NODE:
            caseCDATASection((CDATASection) n, path);
            caseText((Text) n, path);
            caseCharacterData((CharacterData) n, path); break;
        case Node.COMMENT_NODE:
            caseComment((Comment) n, path);
            caseCharacterData((CharacterData) n, path); break;
        case Node.DOCUMENT_FRAGMENT_NODE:
            caseDocumentFragment((DocumentFragment) n, path); break;
        case Node.DOCUMENT_NODE:
            caseDocument((Document) n, path); break;
        case Node.DOCUMENT_TYPE_NODE:
            caseDocumentType((DocumentType) n, path); break;
        case Node.ELEMENT_NODE:
            caseElement((Element) n, path); break;
        case Node.ENTITY_NODE:
            caseEntity((Entity) n, path); break;
        case Node.ENTITY_REFERENCE_NODE:
            caseEntityReference((EntityReference) n, path); break;
        case Node.NOTATION_NODE:
            caseNotation((Notation) n, path); break;
        case Node.PROCESSING_INSTRUCTION_NODE:
            caseProcessingInstruction((ProcessingInstruction) n,
                                      path); break;
        case Node.TEXT_NODE:
            caseText((Text) n, path);
            caseCharacterData((CharacterData) n, path); break;
        }
        caseNode(n, path);
    }

    public int getOrdering() { return PRE; }

    public String getPathAttributeName(Element n) { return null; }

    public Object getPathObj(Node n) {
        if (n instanceof Element) {
            String attrName = getPathAttributeName((Element) n);
            if (attrName == null)
                return null;
            else
                return ((Element) n).getAttribute(attrName);
        } else
            return null;
    }

    public void caseAttr(Attr a, List path) {}
    public void caseCDATASection(CDATASection a, List path) {}
    public void caseCharacterData(CharacterData a, List path) {}
    public void caseComment(Comment a, List path) {}
    public void caseDocument(Document a, List path) {}
    public void caseDocumentFragment(DocumentFragment a, List path) {}
    public void caseDocumentType(DocumentType a, List path) {}
    public void caseElement(Element e, List path) {}
    public void caseEntity(Entity a, List path) {}
    public void caseEntityReference(EntityReference a, List path) {}
    public void caseNode(Node n, List path) {}
    public void caseNotation(Notation a, List path) {}
    public void caseProcessingInstruction(ProcessingInstruction a,List path) {}
    public void caseText(Text a, List path) {}
}
