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

import java.io.*;
import java.util.*;

public class IniFile
{
    private Vector keys = new Vector();
    private Vector vals = new Vector();
    private boolean dirty = false;

    private File file = null;

    public IniFile(String name)
    throws IOException
    {
       file = new File(name);
       if(file.canRead()) loadFile();
    }

    private void saveFile()
    throws IOException
    {
       if(!dirty) return;
       dirty = false;
       PrintStream pr = new PrintStream(new FileOutputStream(file));
       for(int j=0;j<keys.size();++j){
          pr.print(keys.elementAt(j));
          pr.print("=");
          pr.println(vals.elementAt(j));
       }
       pr.close();
    }

    public void removeKey(String s)
    throws IOException
    {
      int i = keys.indexOf(s);
      if (i < 0) return;
      keys.removeElementAt(i);
      vals.removeElementAt(i);
      dirty = true;
      saveFile();
    }

    private void loadFile()
    throws IOException
    {
       char ch = ' ';
       String line = null;
       DataInputStream in = new DataInputStream(new FileInputStream(file));

       // [ \t]* ({symbol}+ '=' [ \t]* {symbol}*

       while((line=in.readLine())!=null){
          int j = 0, k = line.length(), i;
          if(k<=0) continue;
          for(;j<k;++j){
             ch = line.charAt(j);
             if(ch!='\t' && ch!=' ') break;
          }
          if(ch=='#' || ch=='\n' || ch=='\r') continue;
          for(i=j;j<k;++j){
             ch = line.charAt(j);
             if(ch=='\t' || ch==' ' || ch=='\n' || ch=='\r'
                || ch=='=' || ch=='#')
                   break;
          }
          if(j!=i) keys.addElement(line.substring(i,j));
          for(;j<k;++j){
             ch = line.charAt(j);
             if(ch=='=' || ch=='\n' || ch=='\r' || ch=='#') break;
          }
          if(ch=='\n' || ch=='\r' || ch=='#'){
             vals.addElement("");
             continue;
          }
          for(++j;j<k;++j){
             ch = line.charAt(j);
             if(ch!=' ' && ch!='\t') break;
          }
          if(ch=='\n' || ch=='\r' || ch=='#'){
             vals.addElement("");
             continue;
          }
          for(i=j;j<k;++j){
             ch = line.charAt(j);
             if(ch=='\n' || ch=='\r' || ch=='#') break;
          }
          vals.addElement(j!=i? line.substring(i,j).trim() : "");
       }
       in.close();
       in = null;
    }

    public synchronized String getString(String key)
    throws IOException
    {
      int j = keys.indexOf(key);
      if(j<0) return "UNDEFINED";
      return (String)vals.elementAt(j);
    }

    public int getInt(String key)
    throws IOException
    {
      return Integer.parseInt(getString(key));
    }

    public boolean getBoolean(String key)
    throws IOException
    {
        return getString(key).compareTo("True")==0;
    }

    public synchronized void putString(String key, String value)
    throws IOException
    {
      int j = keys.indexOf(key);
      if(j<0){
         keys.addElement(key);
         vals.addElement(value);
      }
      else vals.setElementAt(value,j);
      dirty = true;
      saveFile();
    }

    public void putInt(String key, int value)
    throws IOException
    {
      putString(key,Integer.toString(value));
    }

    public void putBoolean(String key, boolean value)
    throws IOException
    {
       putString(key,value?"True":"False");
    }

    public synchronized void close()
    throws IOException
    {
       saveFile();
    }

    public String toString()
    {
       StringBuffer sb = new StringBuffer();
       sb.append("[IniFile = "+file.toString()+"]={");
       for(int j=0;j<keys.size();++j){
          sb.append("\n\t"+keys.elementAt(j)+"="+vals.elementAt(j));
       }
       sb.append("}");
       return sb.toString();
    }

  public static String getValue (String iniName, String name)
  {
     IniFile ini = null;
     name  = name.trim();
     try {
       ini = new IniFile(iniName);

       String value = ini.getString(name);
       if (value==null || value.length()==0 || value.equals("UNDEFINED"))
         return null;
       else
         return value;
     }
     catch (IOException e) {
       System.out.println("Loader getValue():" + e);
       //e.printStackTrace();
       return null;
     }
     finally {
       try {
         ini.close();
       }
       catch (IOException ee) {
         System.out.println("Loader start():" + ee);
       }
     }
   }

   public static int setValue (String iniName, String name, String value)
   {
     IniFile ini = null;
     name  = name.trim();
     value = value.trim();
     try {
       ini = new IniFile(iniName);
       ini.putString(name, value);
     }
     catch (IOException e) {
       System.out.println("Loader setValue():" + e);
       //e.printStackTrace();
       return -1;
     }
     finally {
       try {
         ini.close();
       }
       catch (IOException ee) {
         System.out.println("Loader start():" + ee);
       }
     }
     return 0;
   }
}
