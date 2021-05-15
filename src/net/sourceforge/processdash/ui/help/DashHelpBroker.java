// Copyright (C) 2001-2021 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net


package net.sourceforge.processdash.ui.help;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.w3c.dom.Element;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.net.http.DashboardHelpURLConnection;
import net.sourceforge.processdash.templates.DashPackage;
import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.util.StringUtils;



public class DashHelpBroker implements DashHelpProvider {

    private static final String ROOT_HELPSET_URL =
        DashboardHelpURLConnection.DASHHELP_PROTOCOL + ":/help/PSPDash.hs";
    private static final String HELPSET_TAG = "helpSet";
    private static final String HREF_ATTR = "href";
    private static final String BOOK_ATTR = "book";
    private static final String BOOKNAME_ATTR = "bookTitle";

    /** The classloader we should use to load javahelp classes */
    private static ClassLoader classloader = null;

    /** A javahelp broker object */
    private Object broker;

    /** The window displaying help content */
    private Window helpWindow;


    public DashHelpBroker() throws Exception {
        if (classloader == null)
            classloader = createClassLoader();
        broker = instantiateBroker();

        Object helpSet = createHelpSet();
        invoke(broker, "setHelpSet", helpSet);

        configureAppearance();
    }

    /** Reflectively create a new JavaHelp broker */
    private Object instantiateBroker() throws Exception {
        try {
            // configure the help viewer to use our custom content viewer UI.
            // that UI opens http URLs in a real web browser, and does a better
            // job of scrolling to named anchors within an HTML page.
            Class util = classloader.loadClass(UTILS_CLASS_NAME);
            util.getMethod("setContentViewerUI", String.class).invoke(null,
                CONTENT_VIEWER_CLASS_NAME);
        } catch (Throwable t) {
            // if the code above fails, continue with the standard viewer UI.
        }

        Class c = classloader.loadClass(BROKER_CLASS_NAME);
        return c.newInstance();
    }

    /** Reflectively create a help set pointing to the dashboard help */
    private Object createHelpSet() throws Exception {
        Class c = classloader.loadClass(HELPSET_CLASS_NAME);
        Class[] argTypes = new Class[] { ClassLoader.class, URL.class };
        Constructor cstr = c.getConstructor(argTypes);

        // construct the help set for the main process dashboard.
        URL hsURL = new URL(ROOT_HELPSET_URL);
        Object[] args = new Object[] { null, hsURL };
        Object rootHelpSet = cstr.newInstance(args);

        // find and append help sets contributed by add-ons
        List ce = ExtensionManager.getXmlConfigurationElements(HELPSET_TAG);
        for (Iterator i = ce.iterator(); i.hasNext();) {
            Element config = (Element) i.next();
            String href = config.getAttribute(HREF_ATTR);
            try {
                if (href.startsWith("/")) href = href.substring(1);
                String url = DashboardHelpURLConnection.DASHHELP_PROTOCOL
                        + ":/" + href;
                args[1] = new URL(url);
                Object subHelpSet = cstr.newInstance(args);
                invoke(rootHelpSet, "add", subHelpSet);
            } catch (Exception e) {
                System.out.println("Warning: couldn't load helpset '" +
                                   href + "'");
            }
        }

        return rootHelpSet;
    }


    /** Configure the appearance of the javahelp window */
    private void configureAppearance() throws Exception {
        invoke(broker, "initPresentation");

        try {
            Object windowPres = invoke(broker, "getWindowPresentation");
            helpWindow = (Window) invoke(windowPres, "getHelpWindow");
            DashboardIconFactory.setWindowIcon(helpWindow);
        } catch (Throwable t) {
            // an old version of JavaHelp in the system classpath will
            // cause this to fail.  It's no big deal - the window will
            // just have a different icon.  Life goes on.
            t.printStackTrace();
        }

        invoke(broker, "setSize", new Dimension(815,500));
    }



    // implementation of DashHelpProvider interface

    public void enableHelpKey(Component comp, String helpID) {
        try {
            invoke(broker, "enableHelpKey", comp, helpID, null);
        } catch (Exception e) {}
    }

    public void enableHelp(Component comp, String helpID) {
        try {
            invoke(broker, "enableHelp", comp, helpID, null);
        } catch (Exception e) {}
    }

    public void enableHelpOnButton(Component comp, String helpID) {
        try {
            invoke(CSH_CLASS_NAME, "setHelpIDString", comp, helpID);
            invoke(broker, "enableHelpOnButton", comp, helpID, null);
        } catch (Exception e) {}
    }

    public void displayHelpTopic(String helpID) {
        try {
            setActiveTab("TOC");
            invoke(broker, "setCurrentID", helpID);
            displayWindow();
        } catch (Exception e) {}
    }

    public void displaySearchTab() {
        try {
            setActiveTab("Search");
            displayWindow();
        } catch (Exception e) {}
    }

    private void displayWindow() throws Exception {
        if (helpWindow != null && !helpWindow.isVisible())
            DashController.setRelativeLocation(helpWindow, 100, 100);
        invoke(broker, "setDisplayed", Boolean.TRUE);
    }
    private void setActiveTab(String tab) {
        try {
            invoke(broker, "setCurrentView", tab);
        } catch (Exception e) {}
    }

    public String getHelpIDString(Component comp) {
        try {
            return (String) invoke(CSH_CLASS_NAME, "getHelpIDString", comp);
        } catch (Exception e) {
            return null;
        }
    }

    public static Map<String, String> getExternalPrintableManuals() {
        Map<String, String> result = new TreeMap<String, String>();
        List ce = ExtensionManager.getXmlConfigurationElements(HELPSET_TAG);
        for (Iterator i = ce.iterator(); i.hasNext();) {
            Element config = (Element) i.next();
            String title = config.getAttribute(BOOKNAME_ATTR);
            String url = config.getAttribute(BOOK_ATTR);
            if (StringUtils.hasValue(title) && StringUtils.hasValue(url))
                result.put(title, url);
        }
        return result;
    }


    /** Create a classloader for loading from the javahelp library */
    private static ClassLoader createClassLoader() throws Exception {
        // Try locating javahelp using our own classloader.  If that
        // succeeds, we can just return our classloader.
        try {
            ClassLoader cl = DashHelpBroker.class.getClassLoader();
            cl.loadClass(UTILS_CLASS_NAME);
            return cl;
        } catch (Throwable t) {}

        // Our classloader knows nothing about javahelp (the normal case).
        // Attempt to find javahelp within a dashboard template add-on file.
        DashPackage dashHelpPackage = TemplateLoader.getPackage("dashHelp");
        if (dashHelpPackage == null)
            throw new FileNotFoundException
                ("Could not locate javahelp jarfile");

        // We found javahelp.  Use the URL of the resource we found to
        // construct an appropriate classloader for loading javahelp classes.
        File dashHelpFile = new File(dashHelpPackage.filename);
        File dashHelpExt = new File(dashHelpFile.getParentFile(),
                "dashHelpExt.jar");
        URL[] classPath = new URL[] { dashHelpFile.toURI().toURL(),
                dashHelpExt.toURI().toURL() };
        return new URLClassLoader(classPath);
    }



    // Helper methods for reflective invocation

    private static Object invoke(Object target, String methodName) throws Exception {
        return invoke(target, methodName, NO_ARGS);
    }
    private static Object invoke(Object target, String methodName, Object arg1) throws Exception {
        return invoke(target, methodName, new Object[] { arg1 });
    }
    private static Object invoke(Object target, String methodName, Object arg1, Object arg2) throws Exception {
        return invoke(target, methodName, new Object[] { arg1, arg2 });
    }
    private static Object invoke(Object target, String methodName, Object arg1, Object arg2, Object arg3) throws Exception
    {
        return invoke(target, methodName, new Object[] { arg1, arg2, arg3 });
    }

    private static Object invoke(Object target,
                                 String methodName,
                                 Object[] args)
        throws Exception
    {
        try {
            Method m = getMethod(target, methodName);
            if (target instanceof String)
                return m.invoke(null, args);
            else
                return m.invoke(target, args);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private static final String UTILS_CLASS_NAME =
        "javax.help.SwingHelpUtilities";
    private static final String CONTENT_VIEWER_CLASS_NAME =
        "net.sourceforge.processdash.ui.help.DashHelpContentViewerUI";
    private static final String BROKER_CLASS_NAME =
        "javax.help.DefaultHelpBroker";
    private static final String HELPSET_CLASS_NAME = "javax.help.HelpSet";
    private static final String CSH_CLASS_NAME = "javax.help.CSH";

    private static final Object[] NO_ARGS = new Object[0];
    private static Map METHOD_ARGS = new Hashtable();
    static {
        Class component = Component.class;
        Class string = String.class;
        String helpset = HELPSET_CLASS_NAME;
        Object[] enableArgs = new Object[] { component, string, helpset };

        METHOD_ARGS.put("enableHelp", enableArgs);
        METHOD_ARGS.put("enableHelpKey", enableArgs);
        METHOD_ARGS.put("enableHelpOnButton", enableArgs);
        METHOD_ARGS.put("getHelpIDString", new Object[] { component });
        METHOD_ARGS.put("getHelpWindow", NO_ARGS);
        METHOD_ARGS.put("getWindowPresentation", NO_ARGS);
        METHOD_ARGS.put("initPresentation", NO_ARGS);
        METHOD_ARGS.put("setCurrentID", new Object[] { string });
        METHOD_ARGS.put("setCurrentView", new Object[] { string });
        METHOD_ARGS.put("setDisplayed", new Object[] { Boolean.TYPE });
        METHOD_ARGS.put("setHelpIDString", new Object[] { component, string });
        METHOD_ARGS.put("setHelpSet", new Object[] { helpset });
        METHOD_ARGS.put("setSize", new Object[] { Dimension.class });
        METHOD_ARGS.put("add", new Object[] { helpset });
    }


    private static Map METHODS = new Hashtable();

    private static Method getMethod(Object target, String methodName)
        throws Exception
    {
        Method result = (Method) METHODS.get(methodName);

        if (result == null) {
            Object[] argTypes = (Object[]) METHOD_ARGS.get(methodName);
            result = getMethod(target, methodName, argTypes);
            METHODS.put(methodName, result);
        }

        return result;
    }

    private static Method getMethod(Object target, String methodName,
                                    Object[] argTypes) throws Exception {
        Class c = resolveClass(target);

        Class[] paramTypes = new Class[argTypes.length];
        for (int i = 0; i < paramTypes.length; i++)
            paramTypes[i] = resolveClass(argTypes[i]);

        return c.getMethod(methodName, paramTypes);
    }

    private static Class resolveClass(Object obj) throws Exception {
        if (obj instanceof Class)
            return (Class) obj;
        else if (obj instanceof String)
            return classloader.loadClass((String) obj);
        else
            return obj.getClass();
    }
}
