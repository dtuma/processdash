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
import java.awt.Dimension;
import java.awt.Rectangle;

public class AlignHelper
{
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

  protected static boolean check(Insets i)
  {
    if ((i.top  > 0 && i.bottom > 0)||
        (i.left > 0 && i.right  > 0)  ) return false;

    if (i.top < 0  || i.bottom < 0||
        i.left < 0 || i.right  < 0  ) return false;
    return true;
  }

  public static boolean isBelongArea(AlignArea a, int x, int y)
  {
     Rectangle r = a.getAlignRectangle();
     if (r == null) return false;

     switch (a.getMode())
     {
       case AlignArea.INSIDE : return  r.inside(x, y);
       case AlignArea.OUTSIDE: return !r.inside(x, y);
     }
     return false;
  }

  public static Insets getPointAlignInsets(AlignArea a, int x, int y)
  {
    if (!isBelongArea(a, x, y)) return null;

    if (a.getMode() == AlignArea.INSIDE) return a.getAlignInsets();
    Rectangle r = a.getAlignRectangle();
    int maxx = r.x + r.width;
    int maxy = r.y + r.height;
    Insets code = new Insets(0,0,0,0);

    if (x > maxx) code.right++;
    else
    {
      if (x < r.x) code.left++;
    }

    if (y > maxy) code.bottom++;
    else
    {
      if (y < r.y) code.top++;
    }

    return code;
  }
}

