// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.lib.binding;

import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sourceforge.processdash.util.ObservableMap;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;

public class BoundForm extends JPanel {

    public interface Disposable {
        void disposeBoundItem();
    }

    private static final Color DEFAULT_ERROR_COLOR = Color.RED.darker();

    private static final Color DEFAULT_WARNING_COLOR = Color.ORANGE.darker();

    private static final String RESOURCE_BUNDLE_KEY = "_RESOURCES_";

    private static final String ERROR_RESOURCE_PREFIX = "Errors.";

    private static final String DATA_MISSING = "Data_Missing";



    ObservableMap map;

    List elements;


    public BoundForm(Element xml, ResourceBundle resources) {
        map = new ObservableMap();
        if (resources != null)
            map.put(RESOURCE_BUNDLE_KEY, resources);

        elements = new ArrayList();

        setLayout(new GridLayout(0, 1, 5, 5));

        List elements = XMLUtils.getChildElements(xml);
        for (Iterator i = elements.iterator(); i.hasNext();) {
            Element e = (Element) i.next();
            addChildElement(e);
        }
    }

    public void disposeForm() {
        for (Iterator i = elements.iterator(); i.hasNext();) {
            Object element = (Object) i.next();
            if (element instanceof Disposable) {
                Disposable d = (Disposable) element;
                d.disposeBoundItem();
            }
        }
    }


    private void addChildElement(Element e) {
        String tagName = e.getTagName();

        Object formElement = null;

        if ("value".equals(tagName))
            addPlainValue(e);
        else if ("checkbox".equals(tagName))
            formElement = new BoundCheckBox(map, e);
        else if ("combobox".equals(tagName))
            formElement = new BoundComboBox(map, e);
        else if ("text".equals(tagName))
            formElement = new BoundTextField(map, e);
        else if ("password".equals(tagName))
            formElement = new BoundPasswordField(map, e);
        else if ("label".equals(tagName))
            formElement = new BoundLabel(map, e);
        else if ("sql-connection".equals(tagName))
            formElement = new BoundSqlConnection(map, e);
        else if ("sql-query".equals(tagName))
            formElement = new BoundSqlQuery(map, e);
        else
            System.out.println("Warning: element not recognized " + tagName);

        if (formElement != null)
            addFormElement(formElement, e);
    }


    private void addPlainValue(Element e) {
        String id = e.getAttribute("id");
        String value = e.getAttribute("value");
        if (!StringUtils.hasValue(value))
            value = XMLUtils.getTextContents(e);
        map.put(id, value);
    }


    private void addFormElement(Object element, Element e) {
        elements.add(element);

        if (element instanceof JComponent) {
            JComponent component = (JComponent) element;
            Box b = Box.createHorizontalBox();

            String label = e.getAttribute("label");
            if (StringUtils.hasValue(label)) {
                JLabel l = new JLabel(label);
                l.setLabelFor(component);
                b.add(l);
                b.add(Box.createHorizontalStrut(5));
            } else {
                b.add(Box.createHorizontalStrut(50));
            }

            b.add(component);
            b.add(Box.createHorizontalGlue());
            add(b);

            String tooltip = e.getAttribute("tooltip");
            if (StringUtils.hasValue(tooltip))
                component.setToolTipText(tooltip);
        }

        String id = e.getAttribute("id");

        String missingValuePrompt = e.getAttribute("missingError");
        if (StringUtils.hasValue(missingValuePrompt)) {
            String key = ERROR_RESOURCE_PREFIX + DATA_MISSING + "." + id;
            map.put(key, missingValuePrompt);
        }
    }


    public static ErrorData getErrorDataForAttr(Map map, String attrName) {
        Object value = map.get(attrName);

        if (value instanceof ErrorData) {
            ErrorData errorData = (ErrorData) value;
            int severity = errorData.getSeverity();
            String errorToken = errorData.getError();
            if (severity == ErrorData.NO_ERROR
                    || !StringUtils.hasValue(errorToken))
                return null;

            if (errorToken.indexOf(' ') != -1)
                return errorData;

            errorToken = getResource(map, ERROR_RESOURCE_PREFIX + errorToken);
            if (errorToken == null)
                return null;
            else
                return new ErrorValue(errorToken, severity);

        } else if (value == null) {
            String errorToken = getErrorForMissingAttr(map, attrName);
            if (errorToken == null)
                return null;
            else
                return new ErrorValue(errorToken,
                        ErrorTokens.MISSING_DATA_SEVERITY);

        } else
            return null;
    }

    public static String getErrorForAttr(Map map, String attrName) {
        Object value = map.get(attrName);

        if (value instanceof ErrorData) {
            ErrorData errorData = (ErrorData) value;
            if (errorData.getSeverity() == ErrorData.NO_ERROR)
                return null;

            String errorToken = errorData.getError();
            if (!StringUtils.hasValue(errorToken))
                return null;

            if (errorToken.indexOf(' ') != -1)
                return errorToken;

            return getResource(map, ERROR_RESOURCE_PREFIX + errorToken);

        } else if (value == null)
            return getErrorForMissingAttr(map, attrName);

        else
            return null;
    }

    public static String getErrorForMissingAttr(Map map, String attrName) {
        String key = ERROR_RESOURCE_PREFIX + DATA_MISSING + "." + attrName;
        return getResource(map, key);
    }

    public static String getResource(Map map, String key) {
        if (!StringUtils.hasValue(key))
            return null;

        if (map.containsKey(key))
            return StringUtils.asString(map.get(key));

        ResourceBundle resources = (ResourceBundle) map
                .get(RESOURCE_BUNDLE_KEY);
        if (resources == null)
            return null;

        try {
            return resources.getString(key);
        } catch (Exception e) {
            return null;
        }
    }

    public static Color getErrorColor(Map map, Object error) {
        return getErrorColor(map, error, ErrorData.NO_ERROR);
    }

    public static Color getErrorColor(Map map, Object error, int defaultSeverity) {
        int severity = defaultSeverity;
        if (error instanceof ErrorData)
            severity = ((ErrorData) error).getSeverity();
        return getErrorColor(map, severity);
    }

    public static Color getErrorColor(Map map, int errorSeverity) {
        switch (errorSeverity) {
        case ErrorData.NO_ERROR:
            return Color.BLACK;
        case ErrorData.INFORMATION:
            return Color.BLUE;
        case ErrorData.WARNING:
            return DEFAULT_WARNING_COLOR;
        case ErrorData.SEVERE:
            return DEFAULT_ERROR_COLOR;
        }
        return DEFAULT_ERROR_COLOR;
    }
}
