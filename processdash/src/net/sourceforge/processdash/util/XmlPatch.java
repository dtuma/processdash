// Copyright (C) 2008 Tuma Solutions, LLC
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlPatch {

    public static final String PATCH_DIRECTIVE = "patch";

    public static final String TARGET_ATTR = "target";

    public static final String DELETE_TAG = "delete";

    public static final String REPLACE_TAG = "replace";

    public static final String SET_ATTR_TAG = "set";


    public static Document apply(InputStream doc, InputStream patch)
            throws SAXException, IOException {
        Document d = XMLUtils.parse(doc);
        Document p = XMLUtils.parse(patch);
        apply(d, p.getDocumentElement());
        return d;
    }

    public static void apply(Document doc, Element patch) {
        apply(doc.getDocumentElement(), patch);
    }

    public static void apply(Element root, Element patch) {
        List<Element> targets = findElements(root, patch);
        if (targets == null) {
            return;
        } else {
            for (Element oneTarget : targets) {
                applyPatchToOneTarget(oneTarget, patch);
            }
        }
    }

    private static void applyPatchToOneTarget(Element target, Element patch) {
        String tagName = patch.getTagName();
        if (PATCH_DIRECTIVE.equals(tagName)) {
            for (Element subPatch : XMLUtils.getChildElements(patch)) {
                apply(target, subPatch);
            }

        } else if (DELETE_TAG.equals(tagName)) {
            target.getParentNode().removeChild(target);

        } else if (REPLACE_TAG.equals(tagName)) {
            NodeList replacement = patch.getChildNodes();
            for (int i = 0; i < replacement.getLength(); i++) {
                Node oneReplaceElem = target.getOwnerDocument().importNode(
                    replacement.item(i), true);
                target.getParentNode().insertBefore(oneReplaceElem, target);
            }
            target.getParentNode().removeChild(target);

        } else if (SET_ATTR_TAG.equals(tagName)) {
            String attrName = patch.getAttribute("attr");
            String attrValue = patch.getAttribute("value");
            target.setAttribute(attrName, attrValue);
        }

    }

    private static List<Element> findElements(Element root, Element patch) {
        return findElements(root, patch.getAttribute(TARGET_ATTR));
    }

    private static List<Element> findElements(Element e, String condition) {
        if (!StringUtils.hasValue(condition))
            return Collections.singletonList(e);

        if (test(e, condition))
            return Collections.singletonList(e);

        List<Element> result = new ArrayList<Element>();
        for (Element c : XMLUtils.getChildElements(e)) {
            List<Element> oneResult = findElements(c, condition);
            if (oneResult != null)
                result.addAll(oneResult);
        }
        return (result.isEmpty() ? null : result);
    }

    private static boolean test(Element e, String condition) {
        if (condition.startsWith(".")) {
            String classTest = condition.substring(1);
            String elemClass = e.getAttribute("class");
            if (!StringUtils.hasValue(elemClass))
                return false;
            else
                return Arrays.asList(elemClass.split("\\s+")).contains(
                    classTest);
        }

        if (condition.startsWith("#")) {
            String idTest = condition.substring(1);
            String elemId = e.getAttribute("id");
            return idTest.equals(elemId);
        }

        return false;
    }
}
