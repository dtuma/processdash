package org.zaval.tools.i18n.translator;

import java.io.*;
import java.util.*;
import org.zaval.xml.*;

class XmlReader
{
    private XmlElement xml;

    public XmlReader(InputStream in)
    throws IOException, XmlParseException
    {
        xml = new XmlElement();
        xml.parse(new InputStreamReader(in));
    }

    public XmlReader(String body)
    throws IOException, XmlParseException
    {
        xml = new XmlElement();
        xml.parse(new StringReader(body));
    }

    public XmlElement getRootNode()
    {
        return xml;
    }

    public Hashtable getTable()
    {
        Hashtable ask = new Hashtable();
        Enumeration en = xml.enumerateChildren();
        while (en.hasMoreElements()) {
            XmlElement child = (XmlElement) en.nextElement();
            getTable(ask, child, "");
        }
        return ask;
    }

    private void getTable(Hashtable place, XmlElement root, String prefix)
    {
        String xmap = (String)root.getAttribute("lang");
//        if(xmap!=null) xmap = xmap;
        if(xmap==null) xmap = (String)root.getAttribute("name");
        if(xmap==null) xmap = root.getName();
        String name = prefix + xmap + "!";

        Enumeration en;
        /*
        en = root.enumerateAttributeNames();
        while (en.hasMoreElements()) {
            Object key = en.nextElement();
            Object val = root.getAttribute((String)key);
            place.put(name + key, val);
        }
        */
        if(root.getContent()!=null) 
            place.put(prefix + xmap, root.getContent());

        en = root.enumerateChildren();
        while (en.hasMoreElements()) {
            XmlElement child = (XmlElement) en.nextElement();
            getTable(place, child, name);
        }
    }
}

