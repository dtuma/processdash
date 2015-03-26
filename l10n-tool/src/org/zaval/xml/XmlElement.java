/*
    This code based upon NanoXML 2.2 sources
*/

package org.zaval.xml;

import java.io.*;
import java.util.*;

public class XmlElement
{
    static final long serialVersionUID = 6685035139346394777L;
    public static final int NANOXML_MAJOR_VERSION = 2;
    public static final int NANOXML_MINOR_VERSION = 2;

    private Hashtable attributes;
    private Vector children;
    private String name;
    private String contents;
    private Hashtable entities;

    private int lineNr;
    private boolean ignoreCase;
    private boolean ignoreWhitespace;
    private char charReadTooMuch;
    private Reader reader;
    private int parserLineNr;

    public XmlElement()
    {
        this(new Hashtable(), false, true, true);
    }
    
    public XmlElement(Hashtable entities)
    {
        this(entities, false, true, true);
    }

    public XmlElement(boolean skipLeadingWhitespace)
    {
        this(new Hashtable(), skipLeadingWhitespace, true, true);
    }

    public XmlElement(Hashtable entities,
                      boolean   skipLeadingWhitespace)
    {
        this(entities, skipLeadingWhitespace, true, true);
    }

    public XmlElement(Hashtable entities,
                      boolean   skipLeadingWhitespace,
                      boolean   ignoreCase)
    {
        this(entities, skipLeadingWhitespace, true, ignoreCase);
    }

    protected XmlElement(Hashtable entities,
                         boolean   skipLeadingWhitespace,
                         boolean   fillBasicConversionTable,
                         boolean   ignoreCase)
    {
        this.ignoreWhitespace = skipLeadingWhitespace;
        this.ignoreCase = ignoreCase;
        this.name = null;
        this.contents = "";
        this.attributes = new Hashtable();
        this.children = new Vector();
        this.entities = entities;
        this.lineNr = 0;
        Enumeration enum_ = this.entities.keys();
        while (enum_.hasMoreElements()) {
            Object key = enum_.nextElement();
            Object value = this.entities.get(key);
            if (value instanceof String) {
                value = ((String) value).toCharArray();
                this.entities.put(key, value);
            }
        }
        if (fillBasicConversionTable) {
            this.entities.put("amp", new char[] { '&' });
            this.entities.put("quot", new char[] { '"' });
            this.entities.put("apos", new char[] { '\'' });
            this.entities.put("lt", new char[] { '<' });
            this.entities.put("gt", new char[] { '>' });
        }
    }

    public void addChild(XmlElement child)
    {
        this.children.addElement(child);
    }

    public void setAttribute(String name,
                             Object value)
    {
        if (this.ignoreCase) {
            name = name.toLowerCase();
        }
        this.attributes.put(name, value.toString());
    }

    public int countChildren()
    {
        return this.children.size();
    }

    public Enumeration enumerateAttributeNames()
    {
        return this.attributes.keys();
    }

    public Enumeration enumerateChildren()
    {
        return this.children.elements();
    }

    public Vector getChildren()
    {
        try {
            return (Vector) this.children.clone();
        } catch (Exception e) {
            // this never happens, however, some Java compilers are so
            // braindead that they require this exception clause
            return null;
        }
    }

    public String getContents()
    {
        return this.getContent();
    }

    public String getContent()
    {
        return this.contents;
    }

    public int getLineNr()
    {
        return this.lineNr;
    }

    public Object getAttribute(String name)
    {
        return this.getAttribute(name, null);
    }

    public Object getAttribute(String name,
                               Object defaultValue)
    {
        if (this.ignoreCase) {
            name = name.toLowerCase();
        }
        Object value = this.attributes.get(name);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    public Object getAttribute(String    name,
                               Hashtable valueSet,
                               String    defaultKey,
                               boolean   allowLiterals)
    {
        if (this.ignoreCase) {
            name = name.toLowerCase();
        }
        Object key = this.attributes.get(name);
        Object result;
        if (key == null) {
            key = defaultKey;
        }
        result = valueSet.get(key);
        if (result == null) {
            if (allowLiterals) {
                result = key;
            } else {
                throw this.invalidValue(name, (String) key);
            }
        }
        return result;
    }

    public String getName()
    {
        return this.name;
    }

    public String getTagName()
    {
        return this.getName();
    }

    public void parse(Reader reader)
    throws IOException, XmlParseException
    {
        this.parse(reader, /*startingLineNr*/ 1);
    }

    public void parse(Reader reader,
                      int    startingLineNr)
        throws IOException, XmlParseException
    {
        this.charReadTooMuch = '\0';
        this.reader = reader;
        this.parserLineNr = startingLineNr;

        for (;;) {
            char ch = this.scanWhitespace();

            if (ch != '<') {
                throw this.expectedInput("<");
            }

            ch = this.readChar();

            if ((ch == '!') || (ch == '?')) {
                this.skipSpecialTag(0);
            } else {
                this.unreadChar(ch);
                this.scanElement(this);
                return;
            }
        }
    }

    public void parse(String string)
    throws XmlParseException
    {
        try {
            this.parse(new StringReader(string), 1);
        } catch (IOException e) {
        }
    }

    public void removeChild(XmlElement child)
    {
        this.children.removeElement(child);
    }

    public void removeAttribute(String name)
    {
        if (this.ignoreCase) {
            name = name.toLowerCase();
        }
        this.attributes.remove(name);
    }

    public void removeChild(String name)
    {
        this.removeAttribute(name);
    }

    protected XmlElement createAnotherElement()
    {
        return new XmlElement(this.entities,
                              this.ignoreWhitespace,
                              false,
                              this.ignoreCase);
    }

    public void setContent(String content)
    {
        this.contents = content;
    }

    public void setTagName(String name)
    {
        this.setName(name);
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String toString()
    {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            this.write(new PrintStream(out));
            out.flush();
            return new String(out.toByteArray(), 0);
        } catch (IOException e) {
            // Java exception handling suxx
            return super.toString();
        }
    }

    public void write(PrintStream writer)
        throws IOException
    {
        if (this.name == null) {
            this.writeEncoded(writer, this.contents);
            return;
        }
        writer.print('<');
        writer.print(this.name);
        if (! this.attributes.isEmpty()) {
            Enumeration enum_ = this.attributes.keys();
            while (enum_.hasMoreElements()) {
                writer.print(' ');
                String key = (String) enum_.nextElement();
                String value = (String) this.attributes.get(key);
                writer.print(key);
                writer.print('='); writer.write('"');
                this.writeEncoded(writer, value);
                writer.write('"');
            }
        }
        if ((this.contents != null) && (this.contents.length() > 0)) {
            writer.print('>');
            this.writeEncoded(writer, this.contents);
            writer.print('<'); writer.print('/');
            writer.print(this.name);
            writer.write('>');
        } else if (this.children.isEmpty()) {
            writer.print('/'); writer.print('>');
        } else {
            writer.print('>');
            Enumeration enum_ = this.enumerateChildren();
            while (enum_.hasMoreElements()) {
                XmlElement child = (XmlElement) enum_.nextElement();
                child.write(writer);
            }
            writer.print('<'); writer.print('/');
            writer.print(this.name);
            writer.print('>');
        }
    }

    protected void writeEncoded(PrintStream writer,
                                String str)
        throws IOException
    {
        for (int i = 0; i < str.length(); i += 1) {
            char ch = str.charAt(i);
            switch (ch) {
                case '<':
                    writer.write('&'); writer.write('l'); writer.write('t');
                    writer.write(';');
                    break;
                case '>':
                    writer.write('&'); writer.write('g'); writer.write('t');
                    writer.write(';');
                    break;
                case '&':
                    writer.write('&'); writer.write('a'); writer.write('m');
                    writer.write('p'); writer.write(';');
                    break;
                case '"':
                    writer.write('&'); writer.write('q'); writer.write('u');
                    writer.write('o'); writer.write('t'); writer.write(';');
                    break;
                case '\'':
                    writer.write('&'); writer.write('a'); writer.write('p');
                    writer.write('o'); writer.write('s'); writer.write(';');
                    break;
                default:
                    int unicode = (int) ch;
                    if ((unicode < 32) || (unicode > 126)) {
                        writer.write('&'); writer.write('#');
                        writer.write('x');
                        writer.print(Integer.toString(unicode, 16));
                        writer.write(';');
                    } else {
                        writer.write(ch);
                    }
            }
        }
    }

    protected void scanIdentifier(StringBuffer result)
        throws IOException
    {
        for (;;) {
            char ch = this.readChar();
            if (((ch < 'A') || (ch > 'Z')) && ((ch < 'a') || (ch > 'z'))
                && ((ch < '0') || (ch > '9')) && (ch != '_') && (ch != '.')
                && (ch != ':') && (ch != '-') && (ch <= '\u007E')) {
                this.unreadChar(ch);
                return;
            }
            result.append(ch);
        }
    }

    protected char scanWhitespace()
        throws IOException
    {
        for (;;) {
            char ch = this.readChar();
            switch (ch) {
                case ' ':
                case '\t':
                case '\n':
                case '\r':
                    break;
                default:
                    return ch;
            }
        }
    }

    protected char scanWhitespace(StringBuffer result)
        throws IOException
    {
        for (;;) {
            char ch = this.readChar();
            switch (ch) {
                case ' ':
                case '\t':
                case '\n':
                    result.append(ch);
                case '\r':
                    break;
                default:
                    return ch;
            }
        }
    }

    protected void scanString(StringBuffer string)
        throws IOException
    {
        char delimiter = this.readChar();
        if ((delimiter != '\'') && (delimiter != '"')) {
            throw this.expectedInput("' or \"");
        }
        for (;;) {
            char ch = this.readChar();
            if (ch == delimiter) {
                return;
            } else if (ch == '&') {
                this.resolveEntity(string);
            } else {
                string.append(ch);
            }
        }
    }

    protected void scanPCData(StringBuffer data)
        throws IOException
    {
        for (;;) {
            char ch = this.readChar();
            if (ch == '<') {
                ch = this.readChar();
                if (ch == '!') {
                    this.checkCDATA(data);
                } else {
                    this.unreadChar(ch);
                    return;
                }
            } else if (ch == '&') {
                this.resolveEntity(data);
            } else {
                data.append(ch);
            }
        }
    }

    protected boolean checkCDATA(StringBuffer buf)
        throws IOException
    {
        char ch = this.readChar();
        if (ch != '[') {
            this.unreadChar(ch);
            this.skipSpecialTag(0);
            return false;
        } else if (! this.checkLiteral("CDATA[")) {
            this.skipSpecialTag(1); // one [ has already been read
            return false;
        } else {
            int delimiterCharsSkipped = 0;
            while (delimiterCharsSkipped < 3) {
                ch = this.readChar();
                switch (ch) {
                    case ']':
                        if (delimiterCharsSkipped < 2) {
                            delimiterCharsSkipped += 1;
                        } else {
                            buf.append(']');
                            buf.append(']');
                            delimiterCharsSkipped = 0;
                        }
                        break;
                    case '>':
                        if (delimiterCharsSkipped < 2) {
                            for (int i = 0; i < delimiterCharsSkipped; i++) {
                                buf.append(']');
                            }
                            delimiterCharsSkipped = 0;
                            buf.append('>');
                        } else {
                            delimiterCharsSkipped = 3;
                        }
                        break;
                    default:
                        for (int i = 0; i < delimiterCharsSkipped; i += 1) {
                            buf.append(']');
                        }
                        buf.append(ch);
                        delimiterCharsSkipped = 0;
                }
            }
            return true;
        }
    }

    protected void skipComment()
        throws IOException
    {
        int dashesToRead = 2;
        while (dashesToRead > 0) {
            char ch = this.readChar();
            if (ch == '-') {
                dashesToRead -= 1;
            } else {
                dashesToRead = 2;
            }
        }
        if (this.readChar() != '>') {
            throw this.expectedInput(">");
        }
    }

    protected void skipSpecialTag(int bracketLevel)
        throws IOException
    {
        int tagLevel = 1; // <
        char stringDelimiter = '\0';
        if (bracketLevel == 0) {
            char ch = this.readChar();
            if (ch == '[') {
                bracketLevel += 1;
            } else if (ch == '-') {
                ch = this.readChar();
                if (ch == '[') {
                    bracketLevel += 1;
                } else if (ch == ']') {
                    bracketLevel -= 1;
                } else if (ch == '-') {
                    this.skipComment();
                    return;
                }
            }
        }
        while (tagLevel > 0) {
            char ch = this.readChar();
            if (stringDelimiter == '\0') {
                if ((ch == '"') || (ch == '\'')) {
                    stringDelimiter = ch;
                } else if (bracketLevel <= 0) {
                    if (ch == '<') {
                        tagLevel += 1;
                    } else if (ch == '>') {
                        tagLevel -= 1;
                    }
                }
                if (ch == '[') {
                    bracketLevel += 1;
                } else if (ch == ']') {
                    bracketLevel -= 1;
                }
            } else {
                if (ch == stringDelimiter) {
                    stringDelimiter = '\0';
                }
            }
        }
    }

    protected boolean checkLiteral(String literal)
        throws IOException
    {
        int length = literal.length();
        for (int i = 0; i < length; i += 1) {
            if (this.readChar() != literal.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    protected char readChar()
        throws IOException
    {
        if (this.charReadTooMuch != '\0') {
            char ch = this.charReadTooMuch;
            this.charReadTooMuch = '\0';
            return ch;
        } else {
            int i = this.reader.read();
            if (i < 0) {
                throw this.unexpectedEndOfData();
            } else if (i == 10) {
                this.parserLineNr += 1;
                return '\n';
            } else {
                return (char) i;
            }
        }
    }

    protected void scanElement(XmlElement elt)
        throws IOException
    {
        StringBuffer buf = new StringBuffer();
        this.scanIdentifier(buf);
        String name = buf.toString();
        elt.setName(name);
        char ch = this.scanWhitespace();
        while ((ch != '>') && (ch != '/')) {
            buf.setLength(0);
            this.unreadChar(ch);
            this.scanIdentifier(buf);
            String key = buf.toString();
            ch = this.scanWhitespace();
            if (ch != '=') {
                throw this.expectedInput("=");
            }
            this.unreadChar(this.scanWhitespace());
            buf.setLength(0);
            this.scanString(buf);
            elt.setAttribute(key, buf);
            ch = this.scanWhitespace();
        }
        if (ch == '/') {
            ch = this.readChar();
            if (ch != '>') {
                throw this.expectedInput(">");
            }
            return;
        }
        buf.setLength(0);
        ch = this.scanWhitespace(buf);
        if (ch != '<') {
            this.unreadChar(ch);
            this.scanPCData(buf);
        } else {
            for (;;) {
                ch = this.readChar();
                if (ch == '!') {
                    if (this.checkCDATA(buf)) {
                        this.scanPCData(buf);
                        break;
                    } else {
                        ch = this.scanWhitespace(buf);
                        if (ch != '<') {
                            this.unreadChar(ch);
                            this.scanPCData(buf);
                            break;
                        }
                    }
                } else {
                    buf.setLength(0);
                    break;
                }
            }
        }
        if (buf.length() == 0) {
            while (ch != '/') {
                if (ch == '!') {
                    ch = this.readChar();
                    if (ch != '-') {
                        throw this.expectedInput("Comment or Element");
                    }
                    ch = this.readChar();
                    if (ch != '-') {
                        throw this.expectedInput("Comment or Element");
                    }
                    this.skipComment();
                } else {
                    this.unreadChar(ch);
                    XmlElement child = this.createAnotherElement();
                    this.scanElement(child);
                    elt.addChild(child);
                }
                ch = this.scanWhitespace();
                if (ch != '<') {
                    throw this.expectedInput("<");
                }
                ch = this.readChar();
            }
            this.unreadChar(ch);
        } else {
            if (this.ignoreWhitespace) {
                elt.setContent(buf.toString().trim());
            } else {
                elt.setContent(buf.toString());
            }
        }
        ch = this.readChar();
        if (ch != '/') {
            throw this.expectedInput("/");
        }
        this.unreadChar(this.scanWhitespace());
        if (! this.checkLiteral(name)) {
            throw this.expectedInput(name);
        }
        if (this.scanWhitespace() != '>') {
            throw this.expectedInput(">");
        }
    }

    protected void resolveEntity(StringBuffer buf)
        throws IOException
    {
        char ch = '\0';
        StringBuffer keyBuf = new StringBuffer();
        for (;;) {
            ch = this.readChar();
            if (ch == ';') {
                break;
            }
            keyBuf.append(ch);
        }
        String key = keyBuf.toString();
        if (key.charAt(0) == '#') {
            try {
                if (key.charAt(1) == 'x') {
                    ch = (char) Integer.parseInt(key.substring(2), 16);
                } else {
                    ch = (char) Integer.parseInt(key.substring(1), 10);
                }
            } catch (NumberFormatException e) {
                throw this.unknownEntity(key);
            }
            buf.append(ch);
        } else {
            char[] value = (char[]) this.entities.get(key);
            if (value == null) {
                throw this.unknownEntity(key);
            }
            buf.append(value);
        }
    }

    protected void unreadChar(char ch)
    {
        this.charReadTooMuch = ch;
    }

    protected XmlParseException invalidValueSet(String name)
    {
        String msg = "Invalid value set (entity name = \"" + name + "\")";
        return new XmlParseException(this.getName(), this.parserLineNr, msg);
    }

    protected XmlParseException invalidValue(String name,
                                             String value)
    {
        String msg = "Attribute \"" + name + "\" does not contain a valid "
                   + "value (\"" + value + "\")";
        return new XmlParseException(this.getName(), this.parserLineNr, msg);
    }

    protected XmlParseException unexpectedEndOfData()
    {
        String msg = "Unexpected end of data reached";
        return new XmlParseException(this.getName(), this.parserLineNr, msg);
    }

    protected XmlParseException syntaxError(String context)
    {
        String msg = "Syntax error while parsing " + context;
        return new XmlParseException(this.getName(), this.parserLineNr, msg);
    }

    protected XmlParseException expectedInput(String charSet)
    {
        String msg = "Expected: " + charSet;
        return new XmlParseException(this.getName(), this.parserLineNr, msg);
    }

    protected XmlParseException unknownEntity(String name)
    {
        String msg = "Unknown or invalid entity: &" + name + ";";
        return new XmlParseException(this.getName(), this.parserLineNr, msg);
    }
}
