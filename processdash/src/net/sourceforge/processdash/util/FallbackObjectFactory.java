// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net


package net.sourceforge.processdash.util;


import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


/**
 * This factory establishes a reusable pattern for running code that might not
 * be supported in a particular java runtime environment.
 * 
 * A case in point: our application makes use of certain APIs that are only
 * found in Java 1.6. However, it is crucial that our application continue to
 * run without errors in a Java 1.5 environment. Thus, any time we wish to use a
 * Java 1.6 API, we must approach our usage carefully, following these steps:
 * 
 * <ol>
 * 
 * <li>Collect the code that is using the Java 1.6 API, and place it in a
 * separate class. This class must have a no-arg constructor. That constructor
 * must perform various tests to ensure that the current runtime environment
 * meets its needs. If the runtime environment is missing any of the needed
 * APIs, the constructor <b>must</b> throw an exception.</li>
 * 
 * <li>Define an <tt>interface</tt> that you will use to interact with the
 * class above. This interface must compile cleanly against the Java 1.5 APIs.
 * Make the class above <tt>implement</tt> this interface.</li>
 * 
 * <li>If possible/applicable, define a class that runs under Java 1.5, and
 * performs some subset of the functionality provided by the class you created
 * in step 1. If the appropriate action to take in Java 1.5 is to "do nothing",
 * then it is not necessary to create this class.</li>
 * 
 * </ol>
 * 
 * Then, to interact with this functionality, use a code snippet like the
 * following:
 * 
 * <pre>MyInterface xyz =
 *     new FallbackObjectFactory&lt;MyInterface&gt;(MyInterface.class)
 *             .add("class.name.of.FirstChoiceImplementation")
 *             .add("class.name.of.SecondChoiceImplementation")
 *             .add("....") // as many of these as you need - only 1 is required
 *             .get();
 * // now invoke methods on xyz as necessary
 * xyz.doSomething(args);</pre>
 * 
 * This factory will attempt to create an object of the specified types.  If
 * none of the named classes could be supported in the current java runtime
 * environment, this factory will return an object which implements your
 * interface and does nothing.
 * 
 * Although our primary usage of this class will be graceful degradation in
 * Java 1.5, it can be used for other purposes as well.  For example, this
 * could be used to detect whether the user had JavaHelp installed, and
 * gracefully degrade to an alternative context-sensitive help implementation.
 * Any time we want to provide advanced functionality that gracefully degrades
 * in a less-capable runtime environment, this class is appropriate.
 * 
 * 
 * @param <I> the interface or class that will be implemented/extended by
 *    objects returned from this factory.
 * @see RuntimeUtils#assertMethod(Class, String)
 *
 * @author Tuma
 */
public class FallbackObjectFactory<I> {

    private Class<?> instanceType;

    private I result;


// Leaving this constructor undefined unless we determine that it is needed.
//
//    public FallbackObjectFactory() {
//        this.instanceType = null;
//        this.result = null;
//    }

    public FallbackObjectFactory(Class<?> instanceType) {
        if (instanceType == null)
            throw new NullPointerException("instanceType cannot be null");

        this.instanceType = instanceType;
        this.result = null;
    }


    /**
     * Add one alternative class to this factory.
     * 
     * The order classes are added to the factory is significant: the first
     * added class which can be successfully instantiated will be used.
     * 
     * @param nameOfClass
     *                the name of a class that implements/extends &lt;I&gt;,
     *                which could be instantiated and returned by this factory.
     *                The parameter can either be the fully qualified name of
     *                the class, or the terminal name if the class is in the
     *                same package as the interface.
     * @return this factory (useful for method chaining)
     */
    @SuppressWarnings("unchecked")
    public FallbackObjectFactory<I> add(String nameOfClass) {
        if (result == null) {
            String className = nameOfClass;
            if (className.indexOf('.') == -1 && instanceType != null) {
                String instanceClassName = instanceType.getName();
                int dotPos = instanceClassName.lastIndexOf('.');
                className = instanceClassName.substring(0, dotPos+1) + className;
            }

            try {
                Object o = Class.forName(className).newInstance();
                if (instanceType == null || instanceType.isInstance(o))
                    result = (I) o;
            } catch (Throwable t) {
            }
        }
        return this;
    }


    /**
     * Return an object implementing/extending &lt;I&gt;, or null if no such
     * object could be created.
     * 
     * This method is functionally equivalent to calling <tt>get(true)</tt>
     */
    public I get() {
        return get(true);
    }


    /**
     * Return an object implementing/extending &lt;I&gt;, or null if no such
     * object could be created.
     * 
     * First, this class will attempt to instantiate one of the classes
     * registered via the {@link #add(String)} method. If such an object can be
     * instantiated successfully, it will be returned.
     * 
     * If no such object could be created, and if &lt;I&gt; was an interface,
     * and the <tt>autoNoop</tt> parameter is true, a no-op object will be
     * returned which implements &lt;I&gt; and does nothing. If any of these
     * conditions is not true, this will return null.
     * 
     * Multiple calls to this method will return the same object (reference
     * equality).
     */
    @SuppressWarnings("unchecked")
    public I get(boolean autoNoop) {
        if (result != null)
            return result;

        if (autoNoop && instanceType != null && instanceType.isInterface()) {
            try {
                result = (I) Proxy.newProxyInstance(
                    instanceType.getClassLoader(),
                    new Class<?>[] { instanceType }, NOOP);
            } catch (Throwable t) {
            }
        }

        return result;
    }



    private static class NoopHandler implements InvocationHandler {

        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            Class<?> returnType = method.getReturnType();
            if (!returnType.isPrimitive() || returnType == Void.TYPE)
                return null;
            else if (returnType == Boolean.TYPE)
                return Boolean.FALSE;
            else if (returnType == Byte.TYPE)
                return new Byte((byte) 0);
            else if (returnType == Character.TYPE)
                return new Character((char) 0);
            else if (returnType == Double.TYPE)
                return new Double(0);
            else if (returnType == Float.TYPE)
                return new Float(0);
            else if (returnType == Integer.TYPE)
                return new Integer(0);
            else if (returnType == Long.TYPE)
                return new Long(0);
            else if (returnType == Short.TYPE)
                return new Short((short) 0);
            else
                return null;
        }

    }

    private static final InvocationHandler NOOP = new NoopHandler();
}
