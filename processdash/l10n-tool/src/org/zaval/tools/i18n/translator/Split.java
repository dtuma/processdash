/**
 *     Caption: Zaval Java Resource Editor
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
package org.zaval.tools.i18n.translator;

import java.io.*;
import java.util.*;

import org.zaval.util.SafeResourceBundle;

public class Split
{
    private BundleManager bundle = new BundleManager();
    
    private Split( String srcName )
    {
        try{
            readResources(srcName, false);
        }
        catch(Exception e){
        }
    }

    private SafeResourceBundle rcTable = null;
    private String RC (String key)
    {
       return rcTable.getString(key);
    }

    private void join(BundleManager bundle2, boolean part)
    {
       if(part){
           BundleSet set = bundle2.getBundle();
           for (Iterator iter = set.iterator(); iter.hasNext();) {
			  BundleItem bi = (BundleItem) iter.next();
              bundle.getBundle().addKey(bi.getId());
              Enumeration en= bi.getLanguages();
              while(en.hasMoreElements()){    
                  String lang = (String)en.nextElement();
                  bundle.getBundle().addLanguage(lang);
                  bundle.getBundle().updateValue(bi.getId(), lang, bi.getTranslation(lang));
              }
           }
       }
       else bundle = bundle2;
    }

    public void readResources( String fileName, boolean part )
    throws Exception
    {
       try{
          BundleManager bundle2 = new BundleManager(fileName);
          join(bundle2, part);
       }
       catch(Exception e){
          infoException(fileName, e);
          throw e;
       }
    }

    public void onSaveAs(String fileName)
    {
       String filename = fileName;
       if( filename != null )
          try{
             bundle.store( filename );
          }
          catch(Exception e){
             infoException(fileName, e);
          }
    }

    private void infoException(String fileName, Exception e)
    {
       System.err.println(fileName + ":" + e.getMessage());
    }

    private void onGenCode(String fileName)
    {
       try{
          String filename = fileName;
          if( filename != null ){
             SrcGenerator srcgen = new SrcGenerator(
                 bundle.replace(filename, "\\", "/"));
             srcgen.perform(bundle.getBundle());
          }
       }
       catch(Exception e){
          infoException(fileName, e);
       }
    }

    private void onParseCode(String fileName)
    throws Exception
    {
       try{
          String filename = fileName;
          if( fileName!=null ){
             filename = bundle.replace(filename, "\\", "/");
             JavaParser parser = new JavaParser(new FileInputStream(filename));
             Hashtable ask = parser.parse();

             bundle.getBundle().addLanguage("en");
             String rlng = bundle.getBundle().getLanguage(0).getLangId();

             Enumeration en = ask.keys();
             while(en.hasMoreElements()){
                 String key = (String)en.nextElement();
                 bundle.getBundle().addKey(key);
                 bundle.getBundle().updateValue(key, rlng, (String)ask.get(key));
             }
          }
       }
       catch(Exception e){
          //infoException(fileName, e);
          throw e;
       }
    }

    private String stretchPath(String name)
    {
       if(name.length()<60) return name;
       return name.substring(0,4) + "..." +
          name.substring(name.length() - Math.min(name.length() - 7, 60 - 7));
    }

    private String[] getLangSet(String options)
    {
       StringTokenizer st = new StringTokenizer(options, ";,");
       String[] ask = new String[st.countTokens()];
       for(int i = 0; i < ask.length; ++i)
          ask[i] = st.nextToken();
       return ask;
    }

    public void onSaveXml(String fileName, String[] parts)
    {
       String filename = fileName;
       if( filename != null )
          try{
             DataOutputStream out = new DataOutputStream(new FileOutputStream(filename));
             BundleSet set = bundle.getBundle();
             out.writeChar((char)0xFEFF);
             out.writeChars("<xml>\n");
             for (Iterator iter = set.iterator(); iter.hasNext();) {
				BundleItem bi = (BundleItem) iter.next();
                Enumeration en= bi.getLanguages();
                out.writeChars("\t<key name=\"" + bi.getId() + "\">\n");
                while(en.hasMoreElements()){    
                    String lang = (String)en.nextElement();
                    if(!inArray(parts, lang)) continue;
                    out.writeChars("\t\t<value lang=\""+lang+"\">"+bi.getTranslation(lang)+"</value>\n");
                }
                out.writeChars("\t</key>\n");
             }
             out.writeChars("</xml>\n");
             out.close();
          }
          catch(Exception e){
             infoException(fileName, e);
          }
    }

    public void onSaveUtf(String fileName, String[] parts)
    {
       String filename = fileName;
       if( filename != null )
          try{
             DataOutputStream out = new DataOutputStream(new FileOutputStream(filename));
             BundleSet set = bundle.getBundle();
             out.writeChar((char)0xFEFF);
             out.writeChars("#JRCE 1.3: do not modify this line\r\n\r\n");
 			 for (Iterator iter = set.iterator(); iter.hasNext();) {
			   BundleItem bi = (BundleItem) iter.next();
                Enumeration en= bi.getLanguages();
                out.writeChars("KEY=\"" + bi.getId() + "\":\r\n");
                while(en.hasMoreElements()){    
                    String lang = (String)en.nextElement();
                    if(!inArray(parts, lang)) continue;
                    out.writeChars("\t\""+lang+"\"=\""+bi.getTranslation(lang)+"\"\r\n");
                }
                out.writeChars("\r\n");
             }
             out.close();
          }
          catch(Exception e){
             infoException(fileName, e);
          }
    }

    private boolean inArray(String[] array, String lang)
    {
        for(int j=0;array!=null && j<array.length;++j)
            if(array[j]!=null && array[j].equalsIgnoreCase(lang)) return true;
        return false;
    }

    /*
        Reading unicode (UCS16) file stream into memory
    */
    private String getBody(String file)
    throws IOException
    {
        char ch;
        DataInputStream in = new DataInputStream(new FileInputStream(file));
        StringBuffer buf = new StringBuffer(in.available());

        try{
            in.readChar(); // skip UCS16 marker FEFF
            for(;;){
                ch=in.readChar();
                buf.append(ch);
            }
        }
        catch(EOFException eof){
        }
        return buf.toString();
    }

    private void fillTable(Hashtable tbl)
    {
        Enumeration en = tbl.keys();
        while(en.hasMoreElements()){
           String k = (String)en.nextElement();
           StringTokenizer st = new StringTokenizer(k, "!");
           String key = st.nextToken();
           if(!st.hasMoreTokens()) continue;
           String lang = st.nextToken();

           if(bundle.getBundle().getLanguage(lang)==null)
              bundle.getBundle().addLanguage(lang);

           bundle.getBundle().addKey(key);
           bundle.getBundle().updateValue(key, lang, (String)tbl.get(k));
        }
    }

    public void onLoadXml(String fileName)
    throws Exception
    {
       String filename = fileName;
       if( filename != null ){
          bundle.getBundle().addLanguage("en");

          try{
             XmlReader xml = new XmlReader(getBody(filename));
             Hashtable tbl = xml.getTable();
             fillTable(tbl);
          }
          catch(Exception e){
          // infoException(fileName, e);
             throw e;
          }
       }
    }

    public void onLoadUtf(String fileName)
    throws Exception
    {
       String filename = fileName;
       if( filename != null ){
          bundle.getBundle().addLanguage("en");
          try{
             UtfParser parser = new UtfParser(new StringReader(getBody(filename)));
             Hashtable tbl = parser.parse();
             fillTable(tbl);
          }
          catch(Exception e){
          // infoException(fileName, e);
             throw e;
          }
       }
    }

    private void infoError(String error)
    {
        System.err.println(error);
    }

    private void tryToLoad(String fileName)
    throws IOException
    {
        try{ onParseCode(fileName); return; }catch(Exception e){}
        try{ onLoadXml(fileName); return; }catch(Exception e){}
        try{ onLoadUtf(fileName); return; }catch(Exception e){}
        try{ readResources(fileName, true); return; }catch(Exception e){}
        throw new IOException(fileName + ": wrong file format or file unavailable");
    }

    public static void main(String[] args)
    {
        try{
            String command = args[0];
            String fileName= args[1];
            String[] options = new String[args.length - 2];
            for(int j=0;j<options.length;++j)
                options[j] = args[j+2];
            Split obj = new Split(fileName);
            if(command.equals("join")){
                for(int j=0;j<options.length;++j)
                    obj.tryToLoad(options[j]);
                obj.onSaveAs(fileName);
            }
            else if(command.equals("split")){
                String dstFile = options[0];
                options[0] = null;
                if(dstFile.endsWith(".txt")) obj.onSaveUtf(dstFile, options);
                else if(dstFile.endsWith(".xml")) obj.onSaveXml(dstFile, options);
                else if(dstFile.endsWith(".java")) obj.onGenCode(dstFile);
                else throw new IOException(dstFile + ": wrong file format or I/O error");
            }
            else throw new Exception();
        }
        catch(IOException eio){
            System.err.println(eio.getMessage());
        }
        catch(Exception e){
         // if(e.getMessage()!=null && e.getMessage().trim().length()>0) 
         //     System.err.println(e.getMessage());
            e.printStackTrace();
            System.out.println(
                "Usage:\n" + 
                "\tjrc-split join srcFile ... addFile\n" +
                "\tjrc-split split srcFile dstFile [lang ...]\n" +
                "Where:\n" +
                "\taddFile\t- XML, Java, other bundle set or UCS16 text file\n" +
                "\tsrcFile\t- a root file of properties bundle set\n" +
                "\tdstFile\t- XML, Java, other bundle set or UCS16 text file\n" + 
                "\tlang\t- locale abbreviation (suffix of slave properties files)\n"
            );
        }
    }
}
