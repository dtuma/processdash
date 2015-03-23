/*
    This code based upon NanoXML 2.2 sources
*/

package org.zaval.xml;

public class XmlParseException
    extends RuntimeException
{
    public static final int NO_LINE = -1;
    private int lineNr;

    public XmlParseException(String name,
                             String message)
    {
        super("XML Parse Exception during parsing of "
              + ((name == null) ? "the XML definition"
                                : ("a " + name + " element"))
              + ": " + message);
        this.lineNr = NO_LINE;
    }

    public XmlParseException(String name,
                             int    lineNr,
                             String message)
    {
        super("XML Parse Exception during parsing of "
              + ((name == null) ? "the XML definition"
                                : ("a " + name + " element"))
              + " at line " + lineNr + ": " + message);
        this.lineNr = lineNr;
    }

    public int getLineNr()
    {
        return this.lineNr;
    }
}
