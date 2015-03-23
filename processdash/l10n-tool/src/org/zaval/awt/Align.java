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
package org.zaval.awt;

import java.awt.Insets;

public class Align
implements AlignConstants
{
  private int     align    = CENTER;
  private String  alignStr = STR_CENTER;
  private Insets  code     = new Insets(0,0,0,0);

 /**
  * Constructs a new Align.
  */
  public Align() {
  }

 /**
  * Sets the arrangement type as int value.
  * @param a new int value of the align property.
  */
  public void setAlign(int a)
  {
    if (align == a) return;
    code     = align2insets(a);
    alignStr = align2str(a);
    align    = a;
  }

 /**
  * Returns the align type as int value.
  * @return int value of the align property.
  */
  public int getAlign() {
    return align;
  }

 /**
  * Sets the align type as Insets value.
  * @param i new java.awt.Insets value of the align property.
  */
  public void setAlignInsets(Insets i) {
    if (i == code) return;
    align  = insets2align(i);
  }

 /**
  * Returns the align type as Insets value.
  * @return  java.awt.Insets value of the align property.
  */
  public Insets getAlignInsets() {
    return code;
  }

 /**
  * Returns the align type as String value.
  * @return  String value of the align property.
  * </FONT>
  */
  public String getAlignString() {
    return alignStr;
  }

  public static Insets align2insets(int a)
  {
    Insets i = new Insets(-1,-1,-1,-1);
    i.top    = ((a & Align.TOP   )>0)?1:0;
    i.left   = ((a & Align.LEFT  )>0)?1:0;
    i.bottom = ((a & Align.BOTTOM)>0)?1:0;
    i.right  = ((a & Align.RIGHT )>0)?1:0;
    if (!check(i)) return null;
    return i;
  }

  public static String align2str(int a)
  {
     String r = null;
     switch (a)
     {
       case Align.TOP    : r = Align.STR_TOP   ; break;
       case Align.BOTTOM : r = Align.STR_BOTTOM; break;
       case Align.LEFT   : r = Align.STR_LEFT  ; break;
       case Align.RIGHT  : r = Align.STR_RIGHT ; break;
       case Align.TLEFT  : r = Align.STR_TLEFT ; break;
       case Align.TRIGHT : r = Align.STR_TRIGHT; break;
       case Align.BRIGHT : r = Align.STR_BRIGHT; break;
       case Align.BLEFT  : r = Align.STR_BLEFT ; break;
       case Align.CENTER : r = Align.STR_CENTER; break;
     }
     return r;
  }

  public static int insets2align(Insets i)
  {
    if (i == null || !check(i)) return -1;
    int a = 0;
    a |= ((i.top   >0)?Align.TOP   :0);
    a |= ((i.left  >0)?Align.LEFT  :0);
    a |= ((i.bottom>0)?Align.BOTTOM:0);
    a |= ((i.right >0)?Align.RIGHT :0);
    return a;
  }

  protected static boolean check(Insets i)
  {
    if ((i.top  > 0 && i.bottom > 0)||
        (i.left > 0 && i.right  > 0)  ) return false;

    if (i.top < 0  || i.bottom < 0||
        i.left < 0 || i.right  < 0  ) return false;
    return true;
  }
}
