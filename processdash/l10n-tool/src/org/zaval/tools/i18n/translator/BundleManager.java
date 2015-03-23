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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.zaval.io.PropertiesFile;

class BundleManager
implements TranslatorConstants
{
   private static final String BASE_PREFIX = "Templates/resources/";
   private static final String TAG_FILENAME = "save-tags.txt";
   private static Random random = new Random();
   private BundleSet set;
   private Map prefixes = null;
   private Map zipTags = null;
   private String dontSaveLang = null;
   private String saveRefLang = null;
   private ActionListener saveListener = null;
   private BundleSet defaultTranslations = null;
   
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
   
   public void setBaseLang(String lang) {
       dontSaveLang = lang;
       saveRefLang = lang;
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
      Collections.sort(res);
      if (baseFileName.equals(Translator.BUNDLE_NAME))
          res.insertElementAt(baseFileName + RES_EXTENSION, 0);
          
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
       set.addLanguage(lang);
       set.getLanguage(lang).setLangFile(fullName);
       
       if (fullName.endsWith(RES_EXTENSION))
           readPropResource(fullName, lang);
       else if (fullName.endsWith(ZIP_EXTENSION) || fullName.endsWith(JAR_EXTENSION))
           readZipResources(fullName, lang);
       else
           oldreadResource(fullName, lang);
   }

   private void readPropResource( String fullName, String lang ) throws IOException {       
       readPropResource(new FileInputStream(fullName), lang, null);
   }
   
   private void readPropResource( InputStream inStream, String lang, String prefix )
      throws IOException
   {
       Properties p = new Properties();
       p.load(inStream);
       inStream.close();
       
       set.putProperties(lang, prefix, p);
   }

   private String pathToPrefix(String fullName) {
       String cleanedName = dirName(fullName) + purifyFileName(fullName);
       if (cleanedName.startsWith("/"))
           cleanedName = cleanedName.substring(1);
       if (cleanedName.startsWith(BASE_PREFIX))
           cleanedName = cleanedName.substring(BASE_PREFIX.length());
       cleanedName = cleanedName.replace
           ('/', TranslatorConstants.KEY_SEPARATOR);
       return cleanedName;
   }
   
   private void readZipResources(String fullName, String lang) throws IOException {
      ZipFile zipFile = new ZipFile(new File(fullName));
      Set excludedResources = getExcludedResources(zipFile);
      if (prefixes == null)
          prefixes = new TreeMap();
      if (zipTags == null)
          zipTags = new HashMap();
      
       ZipEntry file;
       String filename;
       for (Enumeration e = zipFile.entries();  e.hasMoreElements(); ) {
           file = (ZipEntry) e.nextElement();
           filename = file.getName();
           if (filename.equals(TAG_FILENAME))
               readZipTag(fullName, zipFile.getInputStream(file));
           else if (filename.toLowerCase().endsWith(RES_EXTENSION)
                    && !isExcluded(filename, excludedResources)) {
               String prefix = pathToPrefix(filename);
               prefixes.put(prefix, filename);
               String fileLang = determineLanguage(filename);
               readPropResource(zipFile.getInputStream(file), fileLang, prefix);
           }
       }
       zipFile.close();
   }

   private Set<String> getExcludedResources(ZipFile zipFile) throws IOException {
       ZipEntry e = zipFile.getEntry("meta-inf/l10n-ignore.txt");
       if (e == null)
           e = zipFile.getEntry("META-INF/l10n-ignore.txt");
       if (e == null)
           return Collections.EMPTY_SET;

       Set<String> result = new HashSet();
       BufferedReader in = new BufferedReader(new InputStreamReader(zipFile
                .getInputStream(e), "UTF-8"));
       String line;
       while ((line = in.readLine()) != null)
           result.add(line.trim());
       in.close();
       return result;
   }

   private boolean isExcluded(String filename, Set<String> exclusions) {
       if (exclusions == null || exclusions.isEmpty())
           return false;
       if (exclusions.contains(filename))
           return true;
       for (String s : exclusions) {
           if (s.endsWith("/") && filename.startsWith(s))
               return true;
       }
       return false;
   }

   private void readZipTag(String zipFileName, InputStream in) throws IOException {
       StringBuffer tag = new StringBuffer();
       BufferedReader lines = new BufferedReader(new InputStreamReader(in));
       String line;
       while ((line = lines.readLine()) != null)
           tag.append(line).append('\n');
       in.close();
       

       if (zipTags == null)
           zipTags = new HashMap();
       zipTags.put(zipFileName, tag.toString());
   }

   private void oldreadResource( String fullName, String lang)
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
//    initDefaultTranslations();
       
      int j, k = set.getLangCount();
      for(j=0;j<k;++j){
         LangItem lang = set.getLanguage(j);
         if (dontSaveLang != null && dontSaveLang.equals(lang.getLangId()))
             continue;
         
         store(lang.getLangId(), fileName);
      }
   }

   void store(String lng, String fn)
   throws IOException
   {
       store(lng, fn, null);
   }

   void store(String lng, String fn, String destDir)
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
         store(lng, "autosaved.properties", destDir);
         return;
      }

      if (destDir != null) {
          File f = new File(fn);
          if (!f.exists()) {
              f = new File(destDir, f.getName());
              fn = f.getPath();
              lang.setLangFile(fn);
          }
      }

      if ( fn.endsWith( RES_EXTENSION ) )
          storeProperties(fn, lang);
      else if (fn.endsWith( ZIP_EXTENSION ) || fn.endsWith(JAR_EXTENSION))
          storeZip(fn, lang);
      else
         storeOther(fn, lang);
      
      if (saveListener != null)
          saveListener.actionPerformed
              (new ActionEvent(this, ActionEvent.ACTION_PERFORMED, fn));
   }

   private void storeProperties(String fn, LangItem lang) throws IOException {
       Properties p = set.getProperties(lang.getLangId());
       FileOutputStream out = new FileOutputStream(fn);
       storeProperties(out, p);
       out.close();
   }

   
   private void storeProperties(OutputStream out, Properties p) throws IOException {
       PrintWriter w = new PrintWriter(out);
       w.println("# Java Resource Bundle");
       w.println("# Modified by Zaval JRC Editor (C) Zaval CE Group");
       w.println("# http://www.zaval.org/products/jrc-editor/");
       w.flush();
       
       p.store(out, " ");
   }

   private void storeZip(String fn, LangItem lang) throws IOException {
       Properties p = set.getProperties(lang.getLangId(), defaultTranslations);
       if (p.isEmpty()) {
           File f = new File (fn);
           f.delete();
           return;
       }
       
       ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(fn));
       storeLangInZip(lang, p, zipOut);
       if (saveRefLang != null && !saveRefLang.equals(lang.getLangId()))
           storeRefLangInZip(zipOut);
       storeTagInZip(fn, zipOut);
           
       zipOut.close();
   }

   private void storeRefLangInZip(ZipOutputStream zipOut) throws IOException {
       if (saveRefLang == null) return;
       Properties p = set.getProperties(saveRefLang);
       if (p.isEmpty()) return;
       
       zipOut.putNextEntry(new ZipEntry("ref.zip"));
       
       ZipOutputStream refZip = new ZipOutputStream(zipOut);
       storeLangInZip(saveRefLang, p, refZip);
       refZip.finish();
       
       zipOut.closeEntry();
   }
   
   private void storeTagInZip(String zipFileName, ZipOutputStream zipOut) throws IOException {
       zipOut.putNextEntry(new ZipEntry(TAG_FILENAME));
       String oldTag = null;
       if (zipTags != null)
           oldTag = (String) zipTags.get(zipFileName);
       Writer out = new OutputStreamWriter(zipOut);
       if (oldTag != null) 
           out.write(oldTag);
       else
           out.write("# This file facilitates revision tracking.\n");
       String newTag = System.currentTimeMillis() + "-" + 
           Math.abs(random.nextLong());
       out.write(newTag);
       out.write('\n');
       out.flush();
       zipOut.closeEntry();
   }

   private void storeLangInZip(LangItem lang, Properties p, ZipOutputStream zipOut) throws IOException {
       storeLangInZip(lang.getLangId(), p, zipOut);
   }
   
   private void storeLangInZip(String langID, Properties p, ZipOutputStream zipOut) throws IOException {
       Iterator i = prefixes.entrySet().iterator();
       while (i.hasNext()) {
          Map.Entry e = (Map.Entry) i.next();
          String prefix = (String) e.getKey();
          String filename = (String) e.getValue();
          Properties filt = filterProperties(p, prefix);
          if (filt.isEmpty()) continue;
          filename = dirName(filename) + purifyFileName(filename) + "_" + langID + RES_EXTENSION;
          if (filename.startsWith("/")) filename = filename.substring(1);
          zipOut.putNextEntry(new ZipEntry(filename));
          storeProperties(zipOut, filt);
          zipOut.closeEntry();
       }
   }

   void storeAllInZip(String zipFileName)
   throws IOException
   {   
       ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFileName));  

       int j, k = set.getLangCount();
       for(j=0;j<k;++j){
          LangItem lang = set.getLanguage(j);
          if (dontSaveLang != null && dontSaveLang.equals(lang.getLangId()))
              continue;
       
          Properties p = set.getProperties(lang.getLangId());
          storeLangInZip(lang, p, zipOut);
       }
       zipOut.close();
   }


    private Properties filterProperties(Properties p, String prefix) {
        prefix = prefix + TranslatorConstants.KEY_SEPARATOR;
        Properties result = new PropertiesFile();
        Iterator i = p.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry prop = (Map.Entry) i.next();
            String name = (String) prop.getKey();
            if (!name.startsWith(prefix))
                continue;
            name = name.substring(prefix.length());
            if (name.indexOf(TranslatorConstants.KEY_SEPARATOR) != -1)
                continue;            
            String value = (String) prop.getValue();
            if (value == null || value.trim().length() == 0) 
                continue;
            result.setProperty(name, value);
        }
        return result;
    }

    private void storeOther(String fn, LangItem lang) throws FileNotFoundException, IOException {
        Vector lines = set.store(lang.getLangId());
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

    public void setSaveListener(ActionListener saveListener) {
        this.saveListener = saveListener;
    }
    


    private void initDefaultTranslations() throws IOException {
        String baseName = set.getLanguage(0).getLangFile();
        if (baseName.endsWith(ZIP_EXTENSION) || baseName.endsWith(JAR_EXTENSION)) {
            BundleManager mgr = new BundleManager();
            mgr.readResource( baseName, "en");
            defaultTranslations = mgr.getBundle();
        } else
            defaultTranslations = null;
    }
    
    public Map getPrefixes() {
        return prefixes;
    }
    
    public void addPrefixes(Map prefixes) {
        this.prefixes.putAll(prefixes);
    }
    
}
