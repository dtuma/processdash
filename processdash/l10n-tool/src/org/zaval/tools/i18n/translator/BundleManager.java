/**
 *     Caption: Zaval Java Resource Editor
 *     $Revision$
 *     $Date$
 *
 *     @author:     Victor Krapivin
 *     @version:    1.0
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
import java.awt.*;
import java.util.*;

import org.zaval.util.SafeResourceBundle;

class BundleManager
implements TranslatorConstants
{
   private BundleSet set;

   BundleManager()
   {
      set = new BundleSet();
   }

   BundleManager(String baseFileName)
   throws IOException
   {
      set = new BundleSet();
      readResources(baseFileName);
   }

   BundleSet getBundle()
   {
    return set;
   }

   String dirName(String fn)
   {
      fn = replace(fn, "\\", "/");
      int ind = fn.lastIndexOf( '/' );
      return ind >= 0 ? fn.substring(0, ind + 1) : "./";
   }

   String baseName(String fn)
   {
      fn = replace(fn, "\\", "/");
      int ind = fn.lastIndexOf( '/' );
      fn = ind >= 0 ? fn.substring(ind + 1) : fn;
      ind = fn.lastIndexOf( '.' );
      return ind >= 0 ? fn.substring(0, ind) : fn;
   }

   private String extName(String fn)
   {
      fn = replace(fn, "\\", "/");
      int ind = fn.lastIndexOf( '.' );
      return ind >= 0 ? fn.substring(ind) : "";
   }

   private String purifyFileName(String fn)
   {
      fn = baseName( fn );
      int ind = fn.lastIndexOf( '_' );
      int in2 = ind > 0 ? fn.lastIndexOf( '_', ind - 1 ) : -1;
      if(in2<0  && ind>0) in2 = ind;
      return in2 >= 0 ? fn.substring(0, in2) : fn;
   }

   private Vector getResFiles( String dir, String baseFileName, String defExt )
   {
      File f = new File( dir );
      String bpn = purifyFileName( baseFileName );
      String[] fs = f.list();
      if (fs.length==0) return null;
      Vector res = new Vector();
      for ( int i = 0; i < fs.length; ++i ){
         if(!fs[i].startsWith(bpn)) continue;
         String bfn = purifyFileName(fs[i]);
         if(!bfn.equals(bpn)) continue;
         if(!extName(fs[i]).equals(defExt)) continue;
         File f2 = new File( dir + fs[i] );
         if ( !f2.isDirectory() ) res.addElement( fs[i] );
      }
      return res;
   }

   private String determineLanguage(String fn)
   {
      fn = baseName( fn );
      int ind = fn.lastIndexOf( '_' );
      int in2 = ind > 0 ? fn.lastIndexOf( '_', ind - 1 ) : -1;
      if(in2<0  && ind>0) in2 = ind;
      return in2 >= 0 ? fn.substring(in2 + 1) : "en";
   }

   private void readResources( String baseFileName )
   throws IOException
   {
      String dir  = dirName(baseFileName);
      String ext  = extName(baseFileName);
      baseFileName= baseName(baseFileName);

      Vector fileNames = getResFiles( dir, baseFileName, ext );
      for ( int i = 0; i < fileNames.size(); i++ ){
         String fn = (String) fileNames.elementAt( i );
         readResource( dir + fn, determineLanguage(fn));
      }
   }

   private void readResource( String fullName, String lang)
   throws IOException
   {
      Vector lines = getLines( fullName );
      String lastComment = null;
      for( int i = 0; i < lines.size(); i++ ){
         String line = (String) lines.elementAt( i );
         line = line.trim();
         if(line.length()==0) continue;
         if(line.startsWith("#")){
            lastComment = line.substring( 1 );
            continue;
         }
         int q = line.indexOf('#');

         StringTokenizer st = new StringTokenizer(line, "=", true); // key = value
         if(st.countTokens()<2) continue; // syntax error, ignored
         String dname = st.nextToken().trim();
         st.nextToken(); // '='
         String value = "";
         if(st.hasMoreTokens()) // is there a assigned value
           value = st.nextToken("");

         set.addLanguage(lang);
       //set.updateValue(DEFAULT_LANG_KEY, lang, set.getLanguage(lang).getLangDescription());
         set.getLanguage(lang).setLangFile(fullName);
         set.addKey(dname);
         set.updateValue(dname, lang, value);
         set.getItem(dname).setComment(lastComment);
         lastComment = null;
      }
   }

   void setComment(String key, String comment)
   {
      BundleItem bi = set.getItem(key);
      if(bi==null) return;
      bi.setComment(comment);
   }

   private Vector getLines( String fileName )
   throws IOException
   {
      Vector res = new Vector();
      if (fileName.endsWith( RES_EXTENSION)){
         DataInputStream in = new DataInputStream(new FileInputStream( fileName ));
         String line = null;
         while((line=in.readLine())!=null) {
             for(;;){
                 line = line.trim();
                 if(line.endsWith("\\")){
                    String line2 = in.readLine();
                    if(line2!=null) line = line.substring(0, line.length()-1) + line2;
                    else break;
                 }
                 else break;
             }
             res.addElement( fromEscape( line ) );
         }
      }
      else{
         RandomAccessFile in = new RandomAccessFile( fileName, "r" );
         StringBuffer sb = new StringBuffer();
         int factor1 = 1;
         int factor2 = 256;
         for(;;){
            int i = in.readUnsignedByte() * factor1 + in.readUnsignedByte() * factor2;
            if ( i == 0xFFFE ){
               factor1 = 256;
               factor2 = 1;
            }
            if ( i != 0x0D && i != 0xFFFE && i != 0xFEFF && i != 0xFFFF )
               if ( i != 0x0A ) sb.append( (char) i );
               else{
                  res.addElement( fromEscape( sb.toString() ) );
                  sb.setLength( 0 );
               }
         }
      }
      return res;
   }

   private static String toEscape( String s )
   {
      StringBuffer res = new StringBuffer();
      for ( int i = 0; i < s.length(); i++ ){
         char ch = s.charAt( i );
         int val = (int) ch;
         if ( ch == '\r' ) continue;
         if ( val >= 0 && val < 128 && ch != '\n' && ch != '\\' ) res.append( ch );
         else{
            res.append( "\\u" );
            String hex = Integer.toHexString( val );
            for( int j = 0; j < 4 - hex.length(); j++ ) res.append( "0" );
            res.append( hex );
         }
      }
      return res.toString();
   }
 
   private static String fromEscape( String s )
   {
      StringBuffer res = new StringBuffer();
      for ( int i = 0; i < s.length(); i++ ){
         char ch = s.charAt( i );
         if (ch == '\\' && i+1>=s.length()){
            res.append(ch);
            break;
         }
         if ( ch != '\\' || ch == '\\' && s.charAt( i+1 ) != 'u' ) res.append( ch );
         else{
            res.append( (char) Integer.parseInt( s.substring( i+2, i+6 ), 16 ) );
            i += 5;
         }
      }
      return res.toString();
   }

   String replace( String line, String from, String to )
   {
       StringBuffer res = new StringBuffer(line.length());
       String  tmpstr;
       int     ind = -1, lastind = 0;

       while( ( ind = line.indexOf( from, ind + 1 ) ) != -1 ){
           if(lastind<ind){
              tmpstr = line.substring( lastind, ind );
              res.append(tmpstr);
           }
           res.append(to);
           lastind = ind + from.length();
           ind += from.length() - 1;
       }
       if(lastind==0) return line;
       res.append(line.substring(lastind));
       return res.toString();
   }

   void store(String fileName)
   throws IOException
   {
      int j, k = set.getLangCount();
      for(j=0;j<k;++j){
         LangItem lang = set.getLanguage(j);
         store(lang.getLangId(), fileName);
      }
   }

   void store(String lng, String fn)
   throws IOException
   {
      LangItem lang = set.getLanguage(lng);
      if(fn==null) fn = lang.getLangFile();
      else{
         String tmpFn = fn;
         tmpFn = dirName(tmpFn) + purifyFileName(tmpFn);
         if(set.getLanguage(0)!=lang) tmpFn += "_" + lang.getLangId();
         tmpFn += RES_EXTENSION;
         fn = tmpFn;
         lang.setLangFile(fn);
      }

      if(fn==null){
         store(lng, "autosaved.properties");
         return;
      }

      Vector lines = set.store(lang.getLangId());
      if ( fn.endsWith( RES_EXTENSION ) ){
         PrintStream f = new PrintStream(new FileOutputStream(fn));
         for( int j = 0; j < lines.size(); j++ )
            f.print( toEscape( (String) lines.elementAt( j ) ) + System.getProperty("line.separator") );
         f.close();
      }
      else{
         FileOutputStream f = new FileOutputStream( fn );
         f.write( 0xFF );
         f.write( 0xFE );
         for( int j = 0; j < lines.size(); j++ ){
            String s = (String) lines.elementAt( j );
            s = replace( s, "\n", toEscape( "\n" ) );
            for ( int k = 0; k < s.length(); k++ ){
               char ch = s.charAt( k );
               f.write( ((int)ch) & 255 );
               f.write( ((int) ch) >> 8 );
            }
            f.write( 0x0D );
            f.write( 0x00 );
            f.write( 0x0A );
            f.write( 0x00 );
         }
         f.close();
      }
   }
}
