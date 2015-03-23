/**
 *     Caption: Zaval Java Resource Editor
 *     $Revision$
 *     $Date$
 *
 *     @author:     Victor Krapivin
 *     @version:    1.3
 *
 * Zaval JRC Editor is a visual editor which allows you to manipulate 
 * localization strings for all Java based software with appropriate 
 * support embedded.
 * 
 * For more info on this product read Zaval Java Resource Editor User's Guide
 * (It comes within this package).
 * The latest product version is always available from the product's homepage:
 * http://www.zaval.org/products/jrc-editor/
 * and from the SourceForge:
 * http://sourceforge.net/projects/zaval0002/
 *
 * Contacts:
 *   Support : support@zaval.org
 *   Change Requests : change-request@zaval.org
 *   Feedback : feedback@zaval.org
 *   Other : info@zaval.org
 * 
 * Copyright (C) 2001-2002  Zaval Creative Engineering Group (http://www.zaval.org)
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * (version 2) as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */
package org.zaval.io;

import java.util.*;
import java.io.*;

 //========================================================================

 public class WinIniFile
 {
  public static final int     MAX_LEN_LINE = 256;
  public static final int     CREATE       = 1;
  public static final int     OPEN_EXISTS  = 2;
  public static final String  NO_VALUE     = "NOVALUE";

  private Hashtable    sections = null;
  private String       name     = null;
  private InputStream  isdata   = null;
  private char         buf[]    = new char[WinIniFile.MAX_LEN_LINE];
  private String       begSecSymb = "[";
  private String       endSecSymb = "]";
  private int          openFlag = OPEN_EXISTS;

 //========================================================================

  public WinIniFile(String name, int flag)
  throws IOException
  {
    this.name = name;
    openFlag  = flag;
    switch (flag)
    {
      case CREATE     :{
                         // if this file allready exist => clear it
                         FileOutputStream file = new FileOutputStream(name);
                         file.close();
                         sections = new Hashtable();
                       } break;
      case OPEN_EXISTS: init((InputStream)new FileInputStream(name));
                        break;
    }
  }

 //========================================================================

  public WinIniFile(String name)
  throws IOException
  {
    this.name = name;
    init((InputStream)new FileInputStream(name));
  }

 //========================================================================

  public WinIniFile(InputStream is)
  throws IOException
  {
    init(is);
  }

 //========================================================================

  public void init(InputStream is)
  throws IOException
  {
    String     data = new String();
    Hashtable  sect = null;
    sections = new Hashtable();
    isdata   = is;
    DataInputStream dis = new DataInputStream(isdata);
    for(;;)
    {
     data = readLine(is);
     if (data == null) break;
     if ((isEmpty(data))||
         (data.indexOf(";")== 0)  ) continue;
     if (data.equals("EOF")) break;
     int beg = data.indexOf(begSecSymb);
     if (beg == 0)
     {
      int end = data.indexOf(endSecSymb);
      if (end != (data.length()-1)) continue;
      sect = new Hashtable();
      sections.put(data.substring(beg+1, end).toLowerCase(), sect);
      continue;
     }
     if (sect == null) continue;
     StringTokenizer st = new StringTokenizer(data, "=");
     if (!st.hasMoreTokens()) continue;
     String vname = st.nextToken().toLowerCase();

     String value = null;
     if (!st.hasMoreTokens()) value = NO_VALUE;
     value = st.nextToken();

     if (isEmpty(vname) || isEmpty(value)) continue;
     sect.put(vname, value);
    }
    dis.close();
    dis = null;
  }

 //========================================================================

  private String readLine(InputStream is) throws IOException
  {
   int res = -1, i=0;
   while ((res = is.read()) > 0 &&  res != '\n' && i < buf.length)
     buf[i++] = (char)res;

   if (i == 0) return null;
   return new String(buf,0,i).trim();
  }

 //========================================================================

  private void writeLine(RandomAccessFile raf, String data) throws IOException
  {
   byte buf[] = new byte[data.length()];
   data.getBytes(0, data.length(), buf, 0);
   raf.write(buf);
   raf.write(0x0D);
   raf.write(0x0A);
  }

 //========================================================================

  public String  getValue(String section, String name)
  {
   Hashtable tab = (Hashtable) sections.get(section.toLowerCase());
   if (tab == null) return null;
   return (String) tab.get(name.toLowerCase());
  }

  //========================================================================

  public String  getValue(String name)
  {
    Hashtable tab = foundSection(name.toLowerCase());
    if (tab == null) return null;
    return (String)tab.get(name.toLowerCase());
  }

  //========================================================================

   public Hashtable  getSection(String name)
   {
    return (Hashtable)sections.get(name.toLowerCase());
   }

   public Hashtable  foundSection(String namevalue)
   {
    Enumeration el = sections.elements();
    while (el.hasMoreElements())
    {
     Hashtable tab = (Hashtable) el.nextElement();
     String    res = (String)    tab.get(namevalue.toLowerCase());
     if (res != null) return tab;
    }
    return null;
   }

  public int  size(String section)
  {
   Hashtable tab = getSection(section);
   if (tab == null) return -1;
   return tab.size();
  }

  public int  size()
  {
    Enumeration el    = sections.elements();
    int         count = 0;
    if ((el == null) || (!el.hasMoreElements())) return -1;
    while (el.hasMoreElements())
    {
     Hashtable tab = (Hashtable) el.nextElement();
     count += tab.size();
    }
    return count;
  }

  public int  sizeNumber(String section)
  {
   int count=0;
   Hashtable tab = (Hashtable)sections.get(section.toLowerCase());
   if (tab == null) return -1;
   String data = "";
   for (count=0; data != null ;count++)
    data = (String)tab.get(""+count);
   return count-1;
  }

  public int putValue(String section, String name, String value)
  {
   Hashtable tab = getSection(section.toLowerCase());
   if (tab == null) return -1;
   tab.put(name.toLowerCase(), value);
   return 0;
  }

  //========================================================================

  public int putValue(String name, String value)
  {
   Hashtable tab = foundSection(name.toLowerCase());
   if (tab == null) return -1;
   tab.put(name.toLowerCase(), value);
   return 0;
  }

  //========================================================================

  public int delSection(String name)
  {
   if (sections == null)  return -1;
   sections.remove(name.toLowerCase());
   return 0;
  }

 //========================================================================

  public int delValue (String section, String name)
  {
   Hashtable tab = getSection(section.toLowerCase());
   if (tab == null) return -1;
   tab.remove(name.toLowerCase());
   return 0;
  }

 //========================================================================

  public int putSection(String name)
  {
   if ((sections == null)||(sections.get(name.toLowerCase())!=null))  return -1;
   sections.put(name.toLowerCase(), new Hashtable());
   return 0;
  }

  //========================================================================

  public  int flush (String name) throws IOException
  {
   if (sections == null)  return -1;
   RandomAccessFile file = new RandomAccessFile(name, "rw");
   Enumeration      el   = sections.elements();
   Enumeration      sk   = sections.keys();
   while (el.hasMoreElements())
   {
     Hashtable tab = (Hashtable) el.nextElement();
     if (tab == null) break;
     Enumeration keys   = tab.keys();
     Enumeration values = tab.elements();
     writeLine(file, begSecSymb + (String)sk.nextElement() + endSecSymb);
     while (keys.hasMoreElements())
     {
        String key   = (String) keys.nextElement();
        String value = (String) values.nextElement();
        if (value.equals(NO_VALUE))
          writeLine(file, key);
        else
          writeLine(file, key + "=" + value);
     }
   }
   file.close();
   return 0;
  }

 //========================================================================

  public  int flush () throws IOException
  {
   if (this.name == null) return -1;
   return flush(name);
  }

 //========================================================================

  public String getName()
  {
     return name;
  }

 //========================================================================

  public void setSectionSymbols(String beg, String end)
  {
    begSecSymb = beg;
    endSecSymb = end;
  }

  private boolean isEmpty(String s)
  {
    return s == null || s.length() == 0 || s.trim().length() == 0;
  }
 }              
