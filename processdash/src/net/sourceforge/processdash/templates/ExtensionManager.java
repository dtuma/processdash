// Copyright (C) 2006-2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.templates;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ExtensionManager {

    private static class InvalidExtensionException extends Exception {
        public InvalidExtensionException(String m)    { super(m); }
        public InvalidExtensionException(Throwable t) { super(t); }
    }

    public static class DisabledExtensionException extends RuntimeException {}

    private static final Logger logger = Logger
            .getLogger(ExtensionManager.class.getName());

    private static Map extensionXmlDocs = new Hashtable();

    private static class ExtensionData {
        String filename;

        URL baseUrl;

        public ExtensionData(String filename, URL baseUrl) {
            this.filename = filename;
            this.baseUrl = baseUrl;
        }

        public String getDescription() {
            StringBuffer result = new StringBuffer().append("[");
            if (filename != null)
                result.append("filename=").append(filename);
            else if (baseUrl != null)
                result.append("baseUrl=").append(baseUrl);
            result.append("]");
            return result.toString();
        }
    }

    static void addXmlDoc(Document doc, String filename, URL baseUrl) {
        ExtensionData metaData = new ExtensionData(filename, baseUrl);
        extensionXmlDocs.put(doc, metaData);
    }

    /** Returns a list of all xml Elements with the given tag name found
     * in the <tt>*-template.xml</tt> files loaded by the dashboard.
     * 
     * This is used by various extensible dashboard features to discover
     * contributions made by add-ons.
     *
     * @param tagName the tag name of an xml document element
     * @return a list of all Elements with that tag name found in
     *     the <tt>*-template.xml</tt> files loaded by the dashboard.
     */
    public static List getXmlConfigurationElements(String tagName) {
        List result = new ArrayList();
        for (Iterator i = extensionXmlDocs.keySet().iterator(); i.hasNext();) {
            Document doc = (Document) i.next();
            NodeList configElements = doc.getElementsByTagName(tagName);
            int length = configElements.getLength();
            for (int j = 0; j < length; j++)
                result.add(configElements.item(j));
        }
        return result;
    }


    public static List getExecutableExtensions(String tagName,
            DashboardContext context) {
        return getExecutableExtensions(tagName, "class", "requires", context);
    }


    public static List getExecutableExtensions(String tagName,
            String classAttrName, DashboardContext context) {
        return getExecutableExtensions(tagName, classAttrName, "requires",
                context);
    }

    public static List getExecutableExtensions(String tagName,
            String classAttrName, String requiresAttrName,
            DashboardContext context) {
        return getExecutableExtensions(tagName, classAttrName, null,
            requiresAttrName, context);
    }

    public static List getExecutableExtensions(String tagName,
            String classAttrName, String defaultClass, String requiresAttrName,
            DashboardContext context) {

        List configElements = getXmlConfigurationElements(tagName);
        if (configElements.isEmpty())
            return configElements;

        List result = new ArrayList();
        for (Iterator i = configElements.iterator(); i.hasNext();) {
            Element configElem = (Element) i.next();

            if (requiresAttrName != null) {
                String requiresVal = configElem.getAttribute(requiresAttrName);
                if (!TemplateLoader.meetsPackageRequirement(requiresVal))
                    continue;
            }

            try {
                result.add(getExecutableExtension(configElem, classAttrName,
                        defaultClass, context));
            } catch (DisabledExtensionException dee) {
                // skip disabled extension - no action necessary
            } catch (Exception e) {
                logger.log(Level.WARNING,
                        "Unable to create executable extension", e);
            }
        }
        return result;
    }



    public static Object getExecutableExtension(Element configElement,
            String attrName, DashboardContext context) throws Exception {
        return getExecutableExtension(configElement, attrName, null, context);
    }

    public static Object getExecutableExtension(Element configElement,
            String attrName, String defaultClass, DashboardContext context)
            throws Exception {
        if (configElement == null)
            throw new NullPointerException("configElement must not be null");
        if (!XMLUtils.hasValue(attrName) && !XMLUtils.hasValue(defaultClass))
            throw new IllegalArgumentException(
                "Either attrName or defaultClass must have a value");

        Document configDoc = configElement.getOwnerDocument();
        ExtensionData metaData = (ExtensionData) extensionXmlDocs
                .get(configDoc);
        if (metaData == null)
            throw new IllegalArgumentException("configElement does not belong "
                    + "to any registered template.xml configuration document");


        String className = null;
        if (XMLUtils.hasValue(attrName))
            className = configElement.getAttribute(attrName);
        if (!XMLUtils.hasValue(className))
            className = defaultClass;
        if (!XMLUtils.hasValue(className))
            throw new IllegalArgumentException("Error in config file '"
                    + metaData.filename + "': no value was supplied for the '"
                    + attrName + "' attribute of the '"
                    + configElement.getTagName() + "' tag");

        try {
            ClassLoader loader = TemplateLoader
                    .getTemplateClassLoader(metaData.baseUrl);
            Class clazz = loader.loadClass(className);
            Object result = clazz.newInstance();
            initializeExecutableExtension(result, configElement, attrName,
                    context);
            return result;
        } catch (DisabledExtensionException dee) {
            throw dee;
        } catch (Exception e) {
            Exception re = new InvalidExtensionException(
                    "Error in config file '" + metaData.filename
                            + "': could not create object of type '"
                            + className + "' specified by the '" + attrName
                            + "' attribute of the '"
                            + configElement.getTagName() + "' tag");
            if (e instanceof InvalidExtensionException) {
                if (e.getCause() instanceof DisabledExtensionException)
                    throw (DisabledExtensionException) e.getCause();
                else
                    re.initCause(e.getCause());
            } else {
                re.initCause(e);
            }
            throw re;
        }
    }

    static ClassLoader getExtensionClassLoader(Element configElement) {
        if (configElement == null)
            throw new NullPointerException("configElement must not be null");

        Document configDoc = configElement.getOwnerDocument();
        ExtensionData metaData = (ExtensionData) extensionXmlDocs
                .get(configDoc);
        if (metaData == null)
            throw new IllegalArgumentException("configElement does not belong "
                    + "to any registered template.xml configuration document");

        ClassLoader loader = TemplateLoader
                .getTemplateClassLoader(metaData.baseUrl);
        return loader;
    }



    public static String getDebugDescriptionOfSource(Element configElement) {
        if (configElement == null) return null;
        Document configDoc = configElement.getOwnerDocument();
        ExtensionData metaData = (ExtensionData) extensionXmlDocs
                .get(configDoc);
        if (metaData == null)
            return null;
        else
            return metaData.getDescription();
    }

    private static void initializeExecutableExtension(Object obj, Element xml,
            String attrName, DashboardContext context)
            throws InvalidExtensionException {
        Class clz = obj.getClass();
        try {
            Method[] methods = clz.getMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals("setConfigElement"))
                    tryMethodCall(obj, methods[i], xml, attrName);
                else if (methods[i].getName().equals("setDashboardContext")
                        && context != null)
                    tryMethodCall(obj, methods[i], context);
            }
        } catch (InvalidExtensionException iee) {
            throw iee;
        } catch (Exception e) {
        }
    }

    private static void tryMethodCall(Object result, Method m, Object... args)
            throws InvalidExtensionException {
        try {
            m.invoke(result, args);

        } catch (InvocationTargetException ite) {
            // the method call was made, and it threw an exception.  This is
            // important information, indicating that the target object is not
            // in a usable state for some reason.
            throw new InvalidExtensionException(ite.getCause());

        } catch (Exception e) {
            // the method call could not be made.  Since we haven't been very
            // diligent in checking to see if the method matches our desired
            // signature, this is probably our error and can be ignored.
        }
    }
}
