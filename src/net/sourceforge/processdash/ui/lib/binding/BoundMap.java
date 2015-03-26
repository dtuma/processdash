// Copyright (C) 2007-2013 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.lib.binding;

import java.awt.Color;
import java.beans.Introspector;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.util.Base64;
import net.sourceforge.processdash.util.ObservableMap;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;

public class BoundMap extends ObservableMap {

    public interface Disposable {
        void disposeBoundItem();
    }

    protected static final String RESOURCE_BUNDLE_KEY = "_RESOURCES_";

    protected static final String ERROR_RESOURCE_PREFIX = "Errors.";

    protected static final String DATA_MISSING = "Data_Missing";

    private static final Logger logger = Logger.getLogger(BoundMap.class
            .getName());



    protected Map elementTypes;

    protected List elements;


    public BoundMap() {
        elementTypes = new HashMap();
        elements = new ArrayList();
        intializeDefaultElementTypes();
    }


    public void setResources(ResourceBundle resources) {
        put(RESOURCE_BUNDLE_KEY, resources);
    }


    public ResourceBundle getResources() {
        return (ResourceBundle) get(RESOURCE_BUNDLE_KEY);
    }


    public void disposeMap() {
        for (Iterator i = elements.iterator(); i.hasNext();) {
            Object element = (Object) i.next();
            if (element instanceof Disposable) {
                Disposable d = (Disposable) element;
                d.disposeBoundItem();
            }
        }
    }


    protected void intializeDefaultElementTypes() {
        addElementType("value", BoundConstant.class);
        addElementType("checkbox", BoundCheckBox.class);
        addElementType("combobox", BoundComboBox.class);
        addElementType("textfield", BoundTextField.class);
        addElementType("textarea", BoundTextArea.class);
        addElementType("password", BoundPasswordField.class);
        addElementType("label", BoundLabel.class);
        addElementType("sql-connection", BoundSqlConnection.class);
        addElementType("sql-query", BoundSqlQuery.class);
        addElementType("xml-rpc-connection", BoundXmlRpcConnection.class);
        addElementType("grouping", BoundGrouping.class);
        addElementType("row", BoundRow.class);
        addElementType("radio", BoundRadioButtons.class);
    }

    protected void addElementType(String tagName, Class clazz)
            throws IllegalArgumentException {
        try {
            Constructor cstr = clazz.getConstructor(CONSTRUCTOR_PARAMS);
            elementTypes.put(tagName, cstr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Not a valid bound form class: "
                    + clazz);
        }
    }

    private static final Class[] CONSTRUCTOR_PARAMS = new Class[] {
            BoundMap.class, Element.class };



    public List addFormElements(Element xml) {
        List elements = XMLUtils.getChildElements(xml);
        List result = new ArrayList(elements.size());
        for (Iterator i = elements.iterator(); i.hasNext();) {
            Element e = (Element) i.next();
            Object formElement = addFormElement(e);
            if (formElement != null)
                result.add(formElement);
        }
        return result;
    }

    public Object addFormElement(Element xml) {
        if (shouldIgnoreElement(xml))
            return null;

        String tagName = xml.getTagName();
        return addFormElement(xml, tagName);
    }

    protected boolean shouldIgnoreElement(Element xml) {
        String ifProp = xml.getAttribute("ifPropIsSet");
        if (StringUtils.hasValue(ifProp) && !isPropSet(ifProp))
            return true;

        String unlessProp = xml.getAttribute("unlessPropIsSet");
        if (StringUtils.hasValue(unlessProp) && isPropSet(unlessProp))
            return true;

        return false;
    }

    public Object addFormElement(Element xml, String type) {

        if ("object".equals(type)) {
            String className = xml.getAttribute("class");
            try {
                addElementType(className, Class.forName(className, true, Thread
                        .currentThread().getContextClassLoader()));
                type = className;
            } catch (Throwable t) {
                logger.log(Level.WARNING,
                    "Unable to load bound-item object class {0}", className);
                return null;
            }
        }

        Constructor cstr = (Constructor) elementTypes.get(type);
        if (cstr == null) {
            logger.log(Level.WARNING, "Unrecognized bound-item type {0}", type);
            return null;
        }

        try {
            Object[] params = new Object[] { this, xml };
            Object formElement = cstr.newInstance(params);
            addFormElement(formElement, xml);
            return formElement;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unable to create bound-item of type "
                    + type, e);
            return null;
        }
    }

    protected void addFormElement(Object element, Element xml) {
        elements.add(element);

        String id = xml.getAttribute("id");

        String missingValuePrompt = xml.getAttribute("missingError");
        if (StringUtils.hasValue(missingValuePrompt)) {
            String key = id + "." + DATA_MISSING;
            put(key, missingValuePrompt);
        }
    }



    public ErrorData getErrorDataForAttr(String attrName) {
        Object value = get(attrName);

        if (value instanceof ErrorData) {
            ErrorData errorData = (ErrorData) value;
            int severity = errorData.getSeverity();
            String errorToken = errorData.getError();
            if (severity == ErrorData.NO_ERROR
                    || !StringUtils.hasValue(errorToken))
                return null;

            if (errorToken.indexOf(' ') != -1)
                return errorData;

            errorToken = getResource(ERROR_RESOURCE_PREFIX + errorToken);
            if (errorToken == null)
                return null;
            else
                return new ErrorValue(errorToken, severity);

        } else if (value == null) {
            String errorToken = getErrorForMissingAttr(attrName);
            if (errorToken == null)
                return null;
            else
                return new ErrorValue(errorToken,
                        ErrorTokens.MISSING_DATA_SEVERITY);

        } else
            return null;
    }

    public String getErrorForAttr(String attrName) {
        Object value = get(attrName);

        if (value instanceof ErrorData) {
            ErrorData errorData = (ErrorData) value;
            if (errorData.getSeverity() == ErrorData.NO_ERROR)
                return null;

            String errorToken = errorData.getError();
            if (!StringUtils.hasValue(errorToken))
                return null;

            if (errorToken.indexOf(' ') != -1)
                return errorToken;

            return getResource(ERROR_RESOURCE_PREFIX + errorToken);

        } else if (value == null)
            return getErrorForMissingAttr(attrName);

        else
            return null;
    }

    public String getErrorForMissingAttr(String attrName) {
        String key = attrName + "." + DATA_MISSING;
        return getResource(key);
    }

    public String getAttrOrResource(Element xml, String defaultId,
            String resourceSuffix, String defaultValue) {
        String attrName = toCamelCase(resourceSuffix);
        String explicitValue = xml.getAttribute(attrName);
        if (StringUtils.hasValue(explicitValue))
            return explicitValue;

        String resourceId = getFirstNonEmptyValue( //
            xml.getAttribute(attrName + "Id"), //
            xml.getAttribute("resourceId"), //
            defaultId, //
            xml.getAttribute("id"), //
            xml.getTagName());

        String resourceKey = resourceId + "." + resourceSuffix;
        String resourceResult = getResource(resourceKey);
        if (StringUtils.hasValue(resourceResult))
            return resourceResult;

        return defaultValue;
    }

    private String toCamelCase(String withUnderscores) {
        StringBuffer result = new StringBuffer(Introspector
                .decapitalize(withUnderscores));
        StringUtils.findAndReplace(result, "_", "");
        return result.toString();
    }

    private String getFirstNonEmptyValue(String... values) {
        for (String s : values)
            if (StringUtils.hasValue(s))
                return s;
        return null;
    }

    public String getResource(String key) {
        if (!StringUtils.hasValue(key))
            return null;

        if (containsKey(key))
            return StringUtils.asString(get(key));

        ResourceBundle resources = getResources();
        if (resources == null)
            return null;

        try {
            return resources.getString(key);
        } catch (Exception e) {
            return null;
        }
    }

    public Color getErrorColor(Object error) {
        return getErrorColor(error, ErrorData.NO_ERROR);
    }

    public Color getErrorColor(Object error, int defaultSeverity) {
        int severity = defaultSeverity;
        if (error instanceof ErrorData)
            severity = ((ErrorData) error).getSeverity();
        return getErrorColor(severity);
    }

    public Color getErrorColor(int errorSeverity) {
        switch (errorSeverity) {
        case ErrorData.NO_ERROR:
            return DEFAULT_NO_ERROR_COLOR;
        case ErrorData.INFORMATION:
            return DEFAULT_INFORMATION_COLOR;
        case ErrorData.WARNING:
            return DEFAULT_WARNING_COLOR;
        case ErrorData.SEVERE:
            return DEFAULT_ERROR_COLOR;
        }
        return DEFAULT_ERROR_COLOR;
    }

    private static final Color DEFAULT_ERROR_COLOR = Color.RED.darker();

    private static final Color DEFAULT_WARNING_COLOR = Color.ORANGE.darker();

    private static final Color DEFAULT_INFORMATION_COLOR = Color.BLUE;

    private static final Color DEFAULT_NO_ERROR_COLOR = Color.BLACK;


    /**
     * Return true if a given key in the map has a non-null, non-empty-string
     * value.
     */
    public boolean isPropSet(String propName) {
        Object val = get(propName);
        return (val != null && !"".equals(val));
    }


    /** Decode a hashed value */
    public String unhashValue(String hash) {
        if (!StringUtils.hasValue(hash))
            return hash;

        byte[] bytes = Base64.decode(hash);
        if (bytes == null)
            // if the string wasn't a valid base 64 encoding, decode() will
            // return null. In that case, the value must not have been hashed.
            return hash;

        try {
            hashBytes(bytes);
            return new String(bytes, "UTF-8");
        } catch (Exception e) {
            // garbage input or some other problem? Return unchanged.
            return hash;
        }
    }

    /** Encode a hashed value */
    public String hashValue(String value) {
        if (!StringUtils.hasValue(value))
            return value;

        byte[] bytes;
        try {
            bytes = value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // can't happen
            return null;
        }

        hashBytes(bytes);
        return Base64.encodeBytes(bytes);
    }

    // VERY SIMPLISTIC algorithm. This isn't *any* sort of real protection -
    // it's just to prevent accidental misuse of a hashed value. In the event
    // that the hash and the value are stored in different places, and one
    // or the other isn't broadly accessible, this could provide some amount
    // of protection.
    private void hashBytes(byte[] bytes) {
        byte[] hb = null;
        try {
            hb = hashChars.getBytes("UTF-8");
        } catch (Exception e) {
        }
        if (hb == null || hb.length == 0)
            hb = new byte[] { 55 };
        int max = Math.max(bytes.length, hb.length);
        for (int i = 0; i < max; i++) {
            int j = i % bytes.length;
            int k = i % hb.length;
            bytes[j] = (byte) ((bytes[j] ^ hb[k]) & 0xff);
        }
    }

    protected String hashChars = "HASH-CHARS";

}
