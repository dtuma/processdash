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
package org.zaval.tools.i18n.translator;

import java.io.*;
import java.util.*;

class SrcGenerator
{
    private PrintStream out = null;
    private String filename = null;

    SrcGenerator(String filename)
    throws IOException
    {
        FileOutputStream fop = new FileOutputStream(filename);
        out = new PrintStream(fop);
        this.filename = filename;
    }

    void perform (BundleSet set)
    throws IOException
    {
        out.println("import java.util.*;\n\npublic class " +
            baseName(filename) + "\n{");
        for (Iterator iter = set.iterator(); iter.hasNext();) {
			BundleItem bi = (BundleItem) iter.next();
            out.println("\tprivate String " + makeVarName(bi) + ";");
        }
        out.println();
		for (Iterator iter = set.iterator(); iter.hasNext();) {
			BundleItem bi = (BundleItem) iter.next();
            out.println("\tpublic final String get" + makeFunName(bi) +
                "()\t{ return " + makeVarName(bi) + ";}");
        }
        out.println();
		for (Iterator iter = set.iterator(); iter.hasNext();) {
			BundleItem bi = (BundleItem) iter.next();
            out.println("\tpublic final void set" + makeFunName(bi) +
                "(String what)\t{ this." + makeVarName(bi) + " = what;}");
        }
        out.println();
        out.println("\tpublic void loadFromResource(ResourceBundle rs)\n\t{");        
		for (Iterator iter = set.iterator(); iter.hasNext();) {
			BundleItem bi = (BundleItem) iter.next();
            out.println(
                "\t\ttry{ set" + makeFunName(bi) + "(rs.getString(\"" + bi.getId() +
                "\")); } catch(Exception error){ reportNoRc(\"" + bi.getId() +
                "\", error); }");
        }
        out.println("\t}\n");
        out.println("\tprivate void reportNoRc(String what, Exception details)\n\t{\n" +
            "\t\tSystem.err.println(what + \": unknown resource\");\n" +
            "\t\tdetails.printStackTrace();\n\t}\n");
        out.println("}");
        out.close();
    }

    private String makeVarName(BundleItem bi)
    {
        String ask = bi.getTranslation("__var");
        if(ask!=null) return ask;
        ask = makeVarName(bi.getId());
        bi.setTranslation("__var", ask);
        return ask;
    }

    private String makeFunName(BundleItem bi)
    {
        String ask = bi.getTranslation("__varF");
        if(ask!=null) return ask;
        ask = capitalize(makeVarName(bi.getId()));
        bi.setTranslation("__varF", ask);
        return ask;
    }

    private String makeVarName(String key)
    {
        String s = key.toLowerCase();
        int j1 = s.lastIndexOf('.');
        if(j1<0) return s;
        int j2 = s.lastIndexOf('.', j1-1);
        if(j2<0) j2 = -1;
        s = s.substring(j2+1,j1) + capitalize(s.substring(j1+1));
        return s;
    }

    private String capitalize(String s)
    {
        if(s.length()<2) return s.toUpperCase();
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }

    private String baseName(String fn)
    {
        int ind = fn.lastIndexOf( '/' );
        fn = ind >= 0 ? fn.substring(ind + 1) : fn;
        ind = fn.lastIndexOf( '.' );
        return ind >= 0 ? fn.substring(0, ind) : fn;
    }
}

