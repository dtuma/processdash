// Copyright (C) 2002-2003 Tuma Solutions, LLC
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

package net.sourceforge.processdash.net.cache;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import net.sourceforge.processdash.util.*;

import org.w3c.dom.*;

public abstract class CachedObject {

    /** This interface allows an object cache to defer the loading of data.
     */
    public interface CachedDataProvider {
        byte[] getData(CachedObject c);
    }

    private ObjectCache objectCache = null;
    CachedDataProvider dataProvider = null;
    private int id;
    private String type;
    protected int challenge;
    protected Date refreshDate = null;
    protected HashMap localAttrs;
    protected byte[] data = null;

    protected String errorMessage = null;


    /** Create a new cached object of the given type, with the
     *  next available ID. */
    public CachedObject(ObjectCache c, String type) {
        this(c, type, c.getNextID());
    }

    /** Create a new cached object with the given type and id.
     */
    protected CachedObject(ObjectCache c, String type, int id) {
        if (c == null) throw new NullPointerException("cache cannot be null");

        this.objectCache = c;
        this.id = id;
        this.type = type;
        this.challenge = (new Random()).nextInt();
    }

    /** Deserialize a cached object from an XML stream.
     */
    public CachedObject(ObjectCache c, int id, Element xml,
                        CachedDataProvider dataProvider) {
        this.objectCache = c;
        this.dataProvider = dataProvider;
        this.id = id;
        type = xml.getAttribute("type");
        challenge = XMLUtils.getXMLInt(xml, "challenge");
        refreshDate = XMLUtils.getXMLDate(xml, "refreshDate");

        // read and restore the local attributes.
        NodeList attrs = xml.getElementsByTagName("localAttr");
        int len = attrs.getLength();
        for (int i=0;   i < len;   i++) {
            Element attr = (Element) attrs.item(i);
            setLocalAttrImpl(attr.getAttribute("name"),
                             XMLUtils.getTextContents(attr));
        }
    }

    /** Serialize a cached object as XML.
     */
    protected void getAsXML(StringBuffer buf) {
        buf.append("<cachedObject")
            .append(" type='").append(XMLUtils.escapeAttribute(type))
            .append("' challenge='").append(challenge)
            .append("' classname='")
            .append(XMLUtils.escapeAttribute(this.getClass().getName()));
        if (refreshDate != null)
            buf.append("' refreshDate='")
                .append(XMLUtils.saveDate(refreshDate));
        buf.append("'>\n");
        if (localAttrs != null) {
            Iterator i = localAttrs.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry e = (Map.Entry) i.next();
                if (e.getValue() instanceof String)
                    buf.append("  <localAttr type='String' name='")
                        .append(XMLUtils.escapeAttribute((String) e.getKey()))
                        .append("'>")
                        .append(XMLUtils.escapeAttribute
                                ((String) e.getValue()))
                        .append("</localAttr>\n");
            }
        }

        getXMLContent(buf);

        buf.append("</cachedObject>\n");
    }
    public void getXMLContent(StringBuffer buf) {}

    public ObjectCache getCache()   { return objectCache;      }
    public int getID()              { return id;               }
    public String getType()         { return type;             }
    public Date getDate()           { return refreshDate;      }
    public String getErrorMessage() { return errorMessage;     }

    public byte[] getBytes()        {
        if (data == null && dataProvider != null)
            synchronized (this) {
                if (dataProvider != null) {
                    data = dataProvider.getData(this);
                    dataProvider = null;
                }
            }
        return data;
    }
    public String getString()       { return getString(null);  }
    public String getString(String encoding) {
        try {
            getBytes();
            if (data == null) return null;
            if (encoding == null) return new String(data);
            return new String(data, encoding);
        } catch (UnsupportedEncodingException uee) { return null; }
    }
    public Object getLocalAttr(String name) {
        if (localAttrs == null) return null;
        return localAttrs.get(name);
    }
    public synchronized void setLocalAttr(String name, String val) {
        setLocalAttrImpl(name, val);
        store();
    }
    public void setLocalAttrImpl(String name, String val) {
        if (name == null) return;
        if (localAttrs == null) localAttrs = new HashMap();
        localAttrs.put(name, val);
    }


    public abstract boolean refresh();

    /** refresh the object if it is older than maxAge days.
     * @return true if the object was already recent enough, or if the
     *    refresh was successful.
     */
    public boolean refresh(double maxAge) {
        return !olderThanAge(maxAge) || refresh();
    }

    /** refresh the object if it is older than maxAge days, but try not to
     * take longer than maxWait milliseconds to do so.
     * @return true if the object was already recent enough, or if the
     *    refresh was successful.
     */
    public boolean refresh(double maxAge, long maxWait) {
        // simple, no-op implementation, to be intelligently
        // overridden in subclasses.
        return refresh(maxAge);
    }

    public void store(byte[] data) {
        synchronized (this) {
            this.data = data;
            this.dataProvider = null;
        }
        store();
    }

    public void store() {
        objectCache.storeCachedObject(this);
    }

    public boolean olderThanAge(double maxAge) {
        return olderThanAge(refreshDate, maxAge);
    }

    public static boolean olderThanAge(Date date, double maxAge) {
        if (maxAge < 0) return false;
        if (maxAge == 0 || date == null) return true;
        long ageMillis = System.currentTimeMillis() - date.getTime();
        double ageDays = ageMillis / (double) DAY_MILLIS;
        return ageDays > maxAge;
    }
    private static final long DAY_MILLIS =
        24L /*hours*/ * 60 /*minutes*/ * 60 /*seconds*/ * 1000 /*millis*/;

    public static CachedObject openXML(ObjectCache c, int id, Element xml,
                                       CachedDataProvider dataProvider) {
        try {
            // get the classname from the XML element.
            String classname = xml.getAttribute("classname");
            classname = updateClassname(classname);

            // look up the appropriate constructor for that class.
            Constructor constructor = getConstructor(classname);
            if (constructor == null) return null;

            // build up the arg list for the constructor
            Object[] args = new Object[4];
            args[0] = c;
            args[1] = new Integer(id);
            args[2] = xml;
            args[3] = dataProvider;

            // invoke the constructor and return the result.
            return (CachedObject) constructor.newInstance(args);

        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
        return null;
    }


    private static final String OLD_PACKAGE = "pspdash."; // legacy - OK
    private static final String PACKAGE =
        "net.sourceforge.processdash.net.cache.";

    private static String updateClassname(String classname) {
        if (classname.startsWith(OLD_PACKAGE))
            classname = PACKAGE + classname.substring(OLD_PACKAGE.length());
        return classname;
    }
    private static final Class[] CONSTRUCTOR_PARAMS = {
        ObjectCache.class, Integer.TYPE, Element.class,
        CachedDataProvider.class };
    private static HashMap constructors = new HashMap();

    private static Constructor getConstructor(String classname) {
        Constructor result = (Constructor) constructors.get(classname);
        if (result == null && !constructors.containsKey(classname)) try {
            // lookup the class
            Class c = Class.forName(classname);
            // find the appropriate constructor
            result = c.getConstructor(CONSTRUCTOR_PARAMS);
            // save it in the cache
            constructors.put(classname, result);
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
            constructors.put(classname, null);
        }

        return result;
    }
}
