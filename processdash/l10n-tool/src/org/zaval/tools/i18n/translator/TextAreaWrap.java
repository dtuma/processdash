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

import java.awt.*;
import org.zaval.awt.*;
import java.util.*;
import java.lang.*;
import java.awt.im.*;

public class TextAreaWrap
{
    private EmulatedTextArea tf1 = null;
    private TextArea tf2 = null;
    private int flavor;

    public static final int NATIVE = 1;
    public static final int PJAVA  = 2;

    static Character.Subset[] usedInputs={
        Character.UnicodeBlock.ARABIC                         ,
        Character.UnicodeBlock.ARMENIAN                       ,
        Character.UnicodeBlock.BASIC_LATIN                    ,
        Character.UnicodeBlock.BENGALI                        ,
        Character.UnicodeBlock.BOPOMOFO                       ,
        Character.UnicodeBlock.CJK_COMPATIBILITY              ,
        Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS   ,
        Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION    ,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS         ,
        Character.UnicodeBlock.CURRENCY_SYMBOLS               ,
        Character.UnicodeBlock.CYRILLIC                       ,
        Character.UnicodeBlock.DEVANAGARI                     ,
        Character.UnicodeBlock.DINGBATS                       ,
        Character.UnicodeBlock.ENCLOSED_ALPHANUMERICS         ,
        Character.UnicodeBlock.GENERAL_PUNCTUATION            ,
        Character.UnicodeBlock.GEORGIAN                       ,
        Character.UnicodeBlock.GREEK                          ,
        Character.UnicodeBlock.GREEK_EXTENDED                 ,
        Character.UnicodeBlock.GUJARATI                       ,
        Character.UnicodeBlock.GURMUKHI                       ,
        Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO      ,
        Character.UnicodeBlock.HANGUL_JAMO                    ,
        Character.UnicodeBlock.HANGUL_SYLLABLES               ,
        Character.UnicodeBlock.HEBREW                         ,
        Character.UnicodeBlock.HIRAGANA                       ,
        Character.UnicodeBlock.KANBUN                         ,
        Character.UnicodeBlock.KANNADA                        ,
        Character.UnicodeBlock.KATAKANA                       ,
        Character.UnicodeBlock.LAO                            ,
        Character.UnicodeBlock.LATIN_1_SUPPLEMENT             ,
        Character.UnicodeBlock.LATIN_EXTENDED_A               ,
        Character.UnicodeBlock.LATIN_EXTENDED_ADDITIONAL      ,
        Character.UnicodeBlock.LATIN_EXTENDED_B               ,
        Character.UnicodeBlock.MALAYALAM                      ,
        Character.UnicodeBlock.ORIYA                          ,
        Character.UnicodeBlock.TAMIL                          ,
        Character.UnicodeBlock.TELUGU                         ,
        Character.UnicodeBlock.THAI                           ,
        Character.UnicodeBlock.TIBETAN                        ,

        InputSubset.FULLWIDTH_DIGITS                          ,
        InputSubset.FULLWIDTH_LATIN                           ,
        InputSubset.HALFWIDTH_KATAKANA                        ,
        InputSubset.HANJA                                     ,
        InputSubset.KANJI                                     ,
        InputSubset.LATIN                                     ,
        InputSubset.LATIN_DIGITS                              ,
        InputSubset.SIMPLIFIED_HANZI                          ,
        InputSubset.TRADITIONAL_HANZI                          
    };

    public TextAreaWrap()
    {
        //flavor = PJAVA;
        flavor = NATIVE;
        String kind = System.getProperty("inputControls");
        if(kind!=null && kind.equals("java")) flavor = PJAVA;
        if(kind!=null && kind.equals("native")) flavor = NATIVE;
        initControls();
    }

    public TextAreaWrap(int flavor)
    {
        this.flavor = flavor;
        initControls();
    }

    private void initControls()
    {
        if(flavor==NATIVE) tf2 = new TextArea("", 9, 20, TextArea.SCROLLBARS_VERTICAL_ONLY);
        else tf1 = new EmulatedTextArea( true, false, 9, 20 );
    }

    public String getText()
    {
        return flavor == NATIVE ? tf2.getText() : tf1.getText();
    }

    public void setText(String text)
    {
        if(flavor == NATIVE ){ 
            tf2.setText(text);
        }
        else tf1.setText(text);
    }

    public void setVisible(boolean show)
    {
        if(flavor == NATIVE ) tf2.setVisible(show);
        else tf1.setVisible(show);
    }

    public void requestFocus()
    {
        if(flavor == NATIVE ) requestFocus();
        else tf1.requestFocus();
    }

    public Component getControl()
    {
        return flavor == NATIVE ? (Component)tf2 : (Component)tf1;
    }

    public void setLocale(Locale l)
    {   
        if(flavor == NATIVE){ 
            tf2.setLocale(l);
        }
        if(getControl().getInputContext()!=null)
            getControl().getInputContext().setCharacterSubsets(usedInputs);
    }

    public void setEditable(boolean editable) {
        if (flavor == NATIVE)
            tf2.setEditable(editable);
        else
            tf1.setEditable(editable);
    }
}
